package org.hypertrace.service;

import com.typesafe.config.Config;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.query.service.QueryServiceFactory;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigUtils;
import org.hypertrace.gateway.service.GatewayServiceImpl;
import org.hypertrace.gateway.service.entity.config.InteractionConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypertraceDataQueryService extends PlatformService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceDataQueryService.class);

  private static final String PORT_PATH = "service.port";

  private static final String CLUSTER_NAME = "cluster.name";
  private static final String POD_NAME = "pod.name";
  private static final String CONTAINER_NAME = "container.name";

  private static final String GATEWAY_SERVICE_NAME = "gateway-service";
  private static final String QUERY_SERVICE_NAME = "query-service";

  private static final String QUERY_SERVICE_SERVICE_CONFIG = "service.config";

  private String serviceName;
  private Server server;

  public HypertraceDataQueryService(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    serviceName = getServiceName();
    int port = getAppConfig().getInt(PORT_PATH);

    final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);

    // Query service
    serverBuilder.addService(
        InterceptorUtil.wrapInterceptors(
            QueryServiceFactory.build(
                getServiceConfig(QUERY_SERVICE_NAME).getConfig(QUERY_SERVICE_SERVICE_CONFIG),
                getLifecycle())));

    // Gateway service
    final Config gatewayServiceAppConfig = getServiceConfig(GATEWAY_SERVICE_NAME);
    InteractionConfigs.init(gatewayServiceAppConfig);
    serverBuilder.addService(InterceptorUtil.wrapInterceptors(
        new GatewayServiceImpl(gatewayServiceAppConfig)));

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
    LOGGER.info("Starting Hypertrace Data Query Service");
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
    LOGGER.info("Shutting down Hypertrace Data Query Service: {}", serviceName);
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
