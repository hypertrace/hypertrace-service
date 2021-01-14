package org.hypertrace.config.service;

import static org.hypertrace.core.grpcutils.server.InterceptorUtil.wrapInterceptors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.hypertrace.config.service.store.ConfigStore;
import org.hypertrace.config.service.store.DocumentConfigStore;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.space.config.service.SpacesConfigServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigService extends PlatformService {

  private static final String SERVICE_PORT_CONFIG = "service.port";
  private static final String GENERIC_CONFIG_SERVICE_CONFIG = "generic.config.service";
  private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);
  private int serverPort;
  private Server configServer;
  private ConfigStore configStore;

  public ConfigService(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    serverPort = getAppConfig().getInt(SERVICE_PORT_CONFIG);
    LOG.info("Creating {} on port {}", getServiceName(), serverPort);

    configStore = getConfigStore();
    ManagedChannel configChannel =
        ManagedChannelBuilder.forAddress("localhost", serverPort).usePlaintext().build();
    this.getLifecycle().shutdownComplete().thenRun(configChannel::shutdown);

    configServer =
        ServerBuilder.forPort(serverPort)
            .addService(wrapInterceptors(new ConfigServiceGrpcImpl(configStore)))
            .addService(wrapInterceptors(new SpacesConfigServiceImpl(configChannel)))
            .build();
  }

  @Override
  protected void doStart() {
    LOG.info("Attempting to start {} on port {}", getServiceName(), serverPort);
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
    LOG.info("Shutting down service: {}", getServiceName());
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
    return configStore.healthCheck();
  }

  private ConfigStore getConfigStore() {
    try {
      ConfigStore configStore = new DocumentConfigStore();
      configStore.init(getAppConfig().getConfig(GENERIC_CONFIG_SERVICE_CONFIG));
      return configStore;
    } catch (Exception e) {
      throw new RuntimeException("Error in getting or initializing config store", e);
    }
  }
}
