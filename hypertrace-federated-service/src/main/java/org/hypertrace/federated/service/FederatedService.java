package org.hypertrace.federated.service;

import com.typesafe.config.Config;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.hypertrace.core.attribute.service.AttributeServiceImpl;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.DatastoreProvider;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.query.service.QueryServiceImpl;
import org.hypertrace.core.query.service.QueryServiceImplConfig;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigUtils;
import org.hypertrace.entity.data.service.EntityDataServiceImpl;
import org.hypertrace.entity.query.service.EntityQueryServiceImpl;
import org.hypertrace.entity.service.EntityServiceConfig;
import org.hypertrace.entity.type.service.EntityTypeServiceImpl;
import org.hypertrace.gateway.service.GatewayServiceImpl;
import org.hypertrace.gateway.service.entity.config.DomainObjectConfigs;
import org.hypertrace.gateway.service.entity.config.InteractionConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FederatedService extends PlatformService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FederatedService.class);

  private static final String SERVICE_NAME_CONFIG = "service.name";
  private static final String PORT_PATH = "service.port";

  private static final String CLUSTER_NAME = "cluster.name";
  private static final String POD_NAME = "pod.name";
  private static final String CONTAINER_NAME = "container.name";

  private static final String ATTRIBUTE_SERVICE_NAME = "attribute-service";
  private static final String ENTITY_SERVICE_NAME = "entity-service";
  private static final String GATEWAY_SERVICE_NAME = "gateway-service";
  private static final String QUERY_SERVICE_NAME = "query-service";

  private static final String ENTITY_SERVICE_ENTITY_SERVICE_CONFIG = "entity.service.config";
  private static final String GATEWAY_SERVICE_QUERY_SERVICE_CONFIG = "query.service.config";
  private static final String QUERY_SERVICE_SERVICE_CONFIG = "service.config";

  private static final String DEFAULT_CLUSTER_NAME = "dev";

  private String serviceName;
  private Server server;

  public FederatedService(ConfigClient configClient) {
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

    serverBuilder.addService(InterceptorUtil.wrapInterceptors(new EntityTypeServiceImpl(datastore)))
        .addService(InterceptorUtil.wrapInterceptors(new EntityDataServiceImpl(datastore)))
        .addService(InterceptorUtil
            .wrapInterceptors(new EntityQueryServiceImpl(datastore, entityServiceAppConfig)));

    // Query service
    final Config queryServiceAppConfig = getServiceConfig(QUERY_SERVICE_NAME);
    final QueryServiceImplConfig queryServiceImplConfig =
        QueryServiceImplConfig.parse(queryServiceAppConfig.getConfig(QUERY_SERVICE_SERVICE_CONFIG));

    serverBuilder
        .addService(InterceptorUtil.wrapInterceptors(new QueryServiceImpl(queryServiceImplConfig)));

    // Gateway service
    final Config gatewayServiceAppConfig = getServiceConfig(GATEWAY_SERVICE_NAME);

    DomainObjectConfigs.init(gatewayServiceAppConfig);
    InteractionConfigs.init(gatewayServiceAppConfig);

    GatewayServiceImpl ht = new GatewayServiceImpl(
        gatewayServiceAppConfig,
        gatewayServiceAppConfig.getConfig(GATEWAY_SERVICE_QUERY_SERVICE_CONFIG));

    serverBuilder.addService(InterceptorUtil.wrapInterceptors(ht));

    this.server = serverBuilder.build();
  }

  private Config getServiceConfig(String serviceName) {
    String clusterName = ConfigUtils.getEnvironmentProperty(CLUSTER_NAME);
    if (clusterName == null) { clusterName = DEFAULT_CLUSTER_NAME; }

    return configClient.getConfig(serviceName,
        clusterName,
        ConfigUtils.getEnvironmentProperty(POD_NAME),
        ConfigUtils.getEnvironmentProperty(CONTAINER_NAME)
    );
  }

  @Override
  protected void doStart() {
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
  }

  @Override
  protected void doStop() {
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