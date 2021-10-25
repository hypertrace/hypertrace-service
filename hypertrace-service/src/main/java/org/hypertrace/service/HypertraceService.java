package org.hypertrace.service;

import com.typesafe.config.Config;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.time.Clock;
import org.hypertrace.entity.service.change.event.api.EntityChangeEventGenerator;
import org.hypertrace.entity.service.change.event.impl.EntityChangeEventGeneratorFactory;
import org.hypertrace.config.service.ConfigServicesFactory;
import org.hypertrace.core.attribute.service.AttributeServiceImpl;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.DatastoreProvider;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.query.service.QueryServiceFactory;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigUtils;
import org.hypertrace.entity.data.service.EntityDataServiceImpl;
import org.hypertrace.entity.query.service.EntityQueryServiceImpl;
import org.hypertrace.entity.service.EntityServiceConfig;
import org.hypertrace.entity.type.service.v2.EntityTypeServiceImpl;
import org.hypertrace.gateway.service.GatewayServiceImpl;
import org.hypertrace.gateway.service.entity.config.InteractionConfigs;
import org.hypertrace.gateway.service.entity.config.TimestampConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypertraceService extends PlatformService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceService.class);

  private static final String SERVICE_NAME_CONFIG = "service.name";
  private static final String PORT_PATH = "service.port";

  private static final String CLUSTER_NAME = "cluster.name";
  private static final String POD_NAME = "pod.name";
  private static final String CONTAINER_NAME = "container.name";

  private static final String ATTRIBUTE_SERVICE_NAME = "attribute-service";
  private static final String ENTITY_SERVICE_NAME = "entity-service";
  private static final String GATEWAY_SERVICE_NAME = "gateway-service";
  private static final String QUERY_SERVICE_NAME = "query-service";
  private static final String GRAPHQL_SERVICE_NAME = "hypertrace-graphql-service";
  private static final String CONFIG_SERVICE_NAME = "config-service";

  private static final String ENTITY_SERVICE_ENTITY_SERVICE_CONFIG = "entity.service.config";
  private static final String QUERY_SERVICE_SERVICE_CONFIG = "service.config";

  private static final String DEFAULT_CLUSTER_NAME = "default-cluster";

  private String serviceName;
  private Server server;
  private HypertraceUIServer hypertraceUIServer;

  public HypertraceService(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    serviceName = getAppConfig().getString(SERVICE_NAME_CONFIG);
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
    EntityChangeEventGenerator entityChangeEventGenerator =
        EntityChangeEventGeneratorFactory.getInstance()
            .createEntityChangeEventGenerator(getAppConfig(), Clock.systemUTC());

    serverBuilder
        .addService(
            InterceptorUtil.wrapInterceptors(
                new org.hypertrace.entity.type.service.EntityTypeServiceImpl(datastore)))
        .addService(InterceptorUtil.wrapInterceptors(new EntityTypeServiceImpl(datastore)))
        .addService(
            InterceptorUtil.wrapInterceptors(new EntityDataServiceImpl(datastore, localChannel, entityChangeEventGenerator)))
        .addService(
            InterceptorUtil.wrapInterceptors(
                new EntityQueryServiceImpl(datastore, entityServiceAppConfig, channelRegistry)));

    // Query service
    serverBuilder.addService(
        InterceptorUtil.wrapInterceptors(
            QueryServiceFactory.build(
                getServiceConfig(QUERY_SERVICE_NAME).getConfig(QUERY_SERVICE_SERVICE_CONFIG),
                getLifecycle())));

    // Gateway service
    final Config gatewayServiceAppConfig = getServiceConfig(GATEWAY_SERVICE_NAME);

    InteractionConfigs.init(gatewayServiceAppConfig);
    TimestampConfigs.init(gatewayServiceAppConfig);

    GatewayServiceImpl ht = new GatewayServiceImpl(gatewayServiceAppConfig);
    serverBuilder.addService(InterceptorUtil.wrapInterceptors(ht));

    // Config service
    ConfigServicesFactory.buildAllConfigServices(
            getServiceConfig(CONFIG_SERVICE_NAME), port, getLifecycle())
        .stream()
        .map(InterceptorUtil::wrapInterceptors)
        .forEach(serverBuilder::addService);

    this.server = serverBuilder.build();

    // start Hypertrace UI
    final Config graphQlServiceAppConfig = getServiceConfig(GRAPHQL_SERVICE_NAME);
    hypertraceUIServer = new HypertraceUIServer(getAppConfig(), graphQlServiceAppConfig);
  }

  private Config getServiceConfig(String serviceName) {
    String clusterName = ConfigUtils.getEnvironmentProperty(CLUSTER_NAME);
    if (clusterName == null) {
      clusterName = DEFAULT_CLUSTER_NAME;
    }

    return configClient.getConfig(
        serviceName,
        clusterName,
        ConfigUtils.getEnvironmentProperty(POD_NAME),
        ConfigUtils.getEnvironmentProperty(CONTAINER_NAME));
  }

  @Override
  protected void doStart() {
    Thread grpcThread = new Thread(() -> {
      try {
        try {
          server.start();
        } catch (IOException e) {
          LOGGER.error("Unable to start server");
          throw new RuntimeException(e);
        }
        server.awaitTermination();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    });

    Thread uiThread = new Thread(() -> hypertraceUIServer.startWithTimerTasks());

    grpcThread.start();
    uiThread.start();
  }

  @Override
  protected void doStop() {
    server.shutdownNow();
    hypertraceUIServer.stop();
  }

  @Override
  public boolean healthCheck() {
    return true;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }
}
