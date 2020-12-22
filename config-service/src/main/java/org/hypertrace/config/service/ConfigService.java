package org.hypertrace.config.service;

import com.typesafe.config.Config;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.hypertrace.config.service.store.ConfigStore;
import org.hypertrace.config.service.store.DocumentConfigStore;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ConfigService extends PlatformService {

  private static final String SERVICE_NAME_CONFIG = "service.name";
  private static final String SERVICE_PORT_CONFIG = "service.port";
  private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);
  private String serviceName;
  private int serverPort;
  private Server configServer;
  private ConfigStore configStore;

  public ConfigService(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    Config config = getAppConfig();
    serviceName = config.getString(SERVICE_NAME_CONFIG);
    serverPort = config.getInt(SERVICE_PORT_CONFIG);
    LOG.info("Creating {} on port {}", serviceName, serverPort);

    configStore = getConfigStore(config);
    ConfigServiceGrpcImpl configServiceGrpcImpl = new ConfigServiceGrpcImpl(configStore);
    configServer = ServerBuilder.forPort(serverPort)
        .addService(InterceptorUtil.wrapInterceptors(configServiceGrpcImpl))
        .build();
  }

  @Override
  protected void doStart() {
    LOG.info("Attempting to start {} on port {}", serviceName, serverPort);
    try {
      configServer.start();
      LOG.info("Started Config Service on port {}", serverPort);
    } catch (IOException e) {
      LOG.error("Unable to start Config Service");
      throw new RuntimeException(e);
    }

    try {
      configServer.awaitTermination();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void doStop() {
    LOG.info("Shutting down service: {}", serviceName);
    while (!configServer.isShutdown()) {
      configServer.shutdown();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        LOG.warn("Interrupted!", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public boolean healthCheck() {
    if (configStore != null) {
      return configStore.healthCheck();
    }
    return false;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  private ConfigStore getConfigStore(Config config) {
    try {
      ConfigStore configStore = new DocumentConfigStore();
      configStore.init(config);
      return configStore;
    } catch (Exception e) {
      throw new RuntimeException("Error in getting or initializing config store", e);
    }
  }
}