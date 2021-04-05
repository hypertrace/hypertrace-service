package org.hypertrace.service;

import com.typesafe.config.Config;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.hypertrace.config.service.ConfigServicesFactory;
import org.hypertrace.core.attribute.service.AttributeServiceImpl;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.DatastoreProvider;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigUtils;
import org.hypertrace.entity.data.service.EntityDataServiceImpl;
import org.hypertrace.entity.query.service.EntityQueryServiceImpl;
import org.hypertrace.entity.service.EntityServiceConfig;
import org.hypertrace.entity.type.service.v2.EntityTypeServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypertraceDataConfigService extends PlatformService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceDataConfigService.class);

  private static final String PORT_PATH = "service.port";

  private static final String CLUSTER_NAME = "cluster.name";
  private static final String POD_NAME = "pod.name";
  private static final String CONTAINER_NAME = "container.name";

  private static final String ENTITY_SERVICE_NAME = "entity-service";
  private static final String ATTRIBUTE_SERVICE_NAME = "attribute-service";
  private static final String CONFIG_SERVICE_NAME = "config-service";

  private static final String ENTITY_SERVICE_ENTITY_SERVICE_CONFIG = "entity.service.config";
  private Server server;

  public HypertraceDataConfigService(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    int port = getAppConfig().getInt(PORT_PATH);

    final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);

    // Attribute service
    final Config attributeServiceAppConfig = getServiceConfig(ATTRIBUTE_SERVICE_NAME);
    serverBuilder.addService(
        InterceptorUtil.wrapInterceptors(new AttributeServiceImpl(attributeServiceAppConfig)));

    // Entity service
    final Config entityServiceAppConfig = getServiceConfig(ENTITY_SERVICE_NAME);
    final EntityServiceConfig entityServiceConfig = new EntityServiceConfig(
        entityServiceAppConfig.getConfig(ENTITY_SERVICE_ENTITY_SERVICE_CONFIG));
    final Config dataStoreConfig =
        entityServiceConfig.getDataStoreConfig(entityServiceConfig.getDataStoreType());
    final Datastore datastore =
        DatastoreProvider.getDatastore(entityServiceConfig.getDataStoreType(), dataStoreConfig);

    GrpcChannelRegistry channelRegistry = new GrpcChannelRegistry();
    this.getLifecycle().shutdownComplete().thenRun(channelRegistry::shutdown);

    Channel localChannel = channelRegistry.forAddress("localhost", port);

    serverBuilder.addService(InterceptorUtil.wrapInterceptors(new org.hypertrace.entity.type.service.EntityTypeServiceImpl(datastore)))
        .addService(InterceptorUtil.wrapInterceptors(new EntityTypeServiceImpl(datastore)))
        .addService(InterceptorUtil.wrapInterceptors(new EntityDataServiceImpl(datastore, localChannel)))
        .addService(InterceptorUtil
            .wrapInterceptors(new EntityQueryServiceImpl(datastore, entityServiceAppConfig, channelRegistry)));

    // Config service
    ConfigServicesFactory.buildAllConfigServices(
        getServiceConfig(CONFIG_SERVICE_NAME), port, getLifecycle())
        .stream()
        .map(InterceptorUtil::wrapInterceptors)
        .forEach(serverBuilder::addService);

    this.server = serverBuilder.build();
  }

  private Config getServiceConfig(String serviceName) {
    return configClient.getConfig(
        serviceName,
        ConfigUtils.getEnvironmentProperty(CLUSTER_NAME),
        ConfigUtils.getEnvironmentProperty(POD_NAME),
        ConfigUtils.getEnvironmentProperty(CONTAINER_NAME));
  }

  @Override
  protected void doStart() {
    LOGGER.info("Starting Hypertrace Data Config Service");
    try {
      server.start();
      server.awaitTermination();
    } catch (IOException e) {
      LOGGER.error("Fail to start the server.");
      throw new RuntimeException(e);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ie);
    }
  }

  @Override
  protected void doStop() {
    LOGGER.info("Shutting down Hypertrace Data Config Service: {}", getServiceName());
    while (!server.isShutdown()) {
      server.shutdown();
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignore) {
      }
    }
  }

  @Override
  public boolean healthCheck() {
    return true;
  }
}
