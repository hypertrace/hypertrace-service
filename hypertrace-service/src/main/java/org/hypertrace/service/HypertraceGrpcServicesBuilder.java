package org.hypertrace.service;

import static org.hypertrace.service.HypertraceServiceUtils.getServiceConfig;

import com.typesafe.config.Config;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.hypertrace.core.attribute.service.AttributeServiceImpl;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.DatastoreProvider;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.query.service.QueryServiceFactory;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;
import org.hypertrace.entity.data.service.EntityDataServiceImpl;
import org.hypertrace.entity.query.service.EntityQueryServiceImpl;
import org.hypertrace.entity.service.EntityServiceConfig;
import org.hypertrace.entity.type.service.v2.EntityTypeServiceImpl;
import org.hypertrace.gateway.service.GatewayServiceImpl;
import org.hypertrace.gateway.service.entity.config.DomainObjectConfigs;
import org.hypertrace.gateway.service.entity.config.InteractionConfigs;
import org.hypertrace.service.all.services.HypertraceAllServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypertraceGrpcServicesBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceGrpcServicesBuilder.class);

  public static final String ATTRIBUTE_SERVICE_NAME = "attribute-service";
  public static final String ENTITY_SERVICE_NAME = "entity-service";
  public static final String GATEWAY_SERVICE_NAME = "gateway-service";
  public static final String QUERY_SERVICE_NAME = "query-service";
  public static final String ENTITY_SERVICE_ENTITY_SERVICE_CONFIG = "entity.service.config";
  public static final String QUERY_SERVICE_SERVICE_CONFIG = "service.config";

  private final ServerBuilder<?> serverBuilder;
  private final int service_port;
  private final ConfigClient configClient;

  public HypertraceGrpcServicesBuilder(int port, ConfigClient configClient) {
    this.serverBuilder = ServerBuilder.forPort(port);
    this.service_port = port;
    this.configClient = configClient;
  }

  public HypertraceGrpcServicesBuilder addEntityService() {
    final Config entityServiceAppConfig = getServiceConfig(ENTITY_SERVICE_NAME, configClient);
    System.out.println("XxxxxxxxxxxxxxxxxxxxxxxxxxxX");
    System.out.println(entityServiceAppConfig);
    final EntityServiceConfig entityServiceConfig = new EntityServiceConfig(
        entityServiceAppConfig.getConfig(ENTITY_SERVICE_ENTITY_SERVICE_CONFIG));
    final Config dataStoreConfig =
        entityServiceConfig.getDataStoreConfig(entityServiceConfig.getDataStoreType());
    final Datastore datastore =
        DatastoreProvider.getDatastore(entityServiceConfig.getDataStoreType(), dataStoreConfig);

    ManagedChannel localChannel =
        ManagedChannelBuilder.forAddress("localhost", service_port).usePlaintext().build();

    serverBuilder.addService(InterceptorUtil.wrapInterceptors(
        new org.hypertrace.entity.type.service.EntityTypeServiceImpl(datastore)))
        .addService(InterceptorUtil.wrapInterceptors(new EntityTypeServiceImpl(datastore)))
        .addService(InterceptorUtil.wrapInterceptors(new EntityDataServiceImpl(datastore, localChannel)))
        .addService(
            InterceptorUtil.wrapInterceptors(
                new EntityQueryServiceImpl(datastore, entityServiceAppConfig)));
    LOGGER.info("Added entity service to the list of services: {}", service_port);
    return this;
  }

  public HypertraceGrpcServicesBuilder addQueryService(PlatformServiceLifecycle lifecycle) {
    serverBuilder.addService(
        InterceptorUtil.wrapInterceptors(
            QueryServiceFactory.build(
                getServiceConfig(QUERY_SERVICE_NAME, configClient).getConfig(QUERY_SERVICE_SERVICE_CONFIG),
                lifecycle)));
    return this;
  }

  public HypertraceGrpcServicesBuilder addGatewayService() {
    final Config gatewayServiceAppConfig = getServiceConfig(GATEWAY_SERVICE_NAME, configClient);

    DomainObjectConfigs.init(gatewayServiceAppConfig);
    InteractionConfigs.init(gatewayServiceAppConfig);

    GatewayServiceImpl ht = new GatewayServiceImpl(gatewayServiceAppConfig);
    serverBuilder.addService(InterceptorUtil.wrapInterceptors(ht));
    return this;
  }

  public HypertraceGrpcServicesBuilder addAttributeService() {
    final Config attributeServiceAppConfig = getServiceConfig(ATTRIBUTE_SERVICE_NAME, configClient);
    serverBuilder.addService(
        InterceptorUtil.wrapInterceptors(new AttributeServiceImpl(attributeServiceAppConfig)));
    return this;
  }

  public Server build() {
    return serverBuilder.build();
  }
}