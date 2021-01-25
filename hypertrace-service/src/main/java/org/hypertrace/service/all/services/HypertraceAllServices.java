package org.hypertrace.service.all.services;

import static org.hypertrace.service.HypertraceServiceUtils.getServiceConfig;

import com.typesafe.config.Config;
import io.grpc.Server;
import java.io.IOException;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;
import org.hypertrace.service.HypertraceGrpcServicesBuilder;
import org.hypertrace.service.HypertraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypertraceAllServices implements HypertraceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceAllServices.class);

  private static final String PORT_PATH = "service.port";
  private static final String GRAPHQL_SERVICE_NAME = "hypertrace-graphql-service";

  //private final Server grpcServicesServer;
  private final HypertraceUIServer hypertraceUIServer;

  public HypertraceAllServices(ConfigClient configClient, PlatformServiceLifecycle serviceLifecycle) {
//    int port = configClient.getConfig().getInt(PORT_PATH);
//
//    // start Hypertrace Grpc services
//    this.grpcServicesServer = new HypertraceGrpcServicesBuilder(port, configClient)
//        .addEntityService()
//        .addQueryService(serviceLifecycle)
//        .addGatewayService()
//        .addAttributeService()
//        .build();

    // start Hypertrace GraphQl & UI service
    final Config graphQlServiceAppConfig = getServiceConfig(GRAPHQL_SERVICE_NAME, configClient);
    hypertraceUIServer = new HypertraceUIServer(configClient.getConfig(), graphQlServiceAppConfig);
  }

  public void start() {
//    LOGGER.info("Starting `all-services` server ");
//    Thread grpcThread = new Thread(() -> {
//      try {
//        try {
//          grpcServicesServer.start();
//        } catch (IOException e) {
//          LOGGER.error("Unable to start server");
//          throw new RuntimeException(e);
//        }
//        grpcServicesServer.awaitTermination();
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//        throw new RuntimeException(e);
//      }
//    });

    Thread uiThread = new Thread(hypertraceUIServer::startWithTimerTasks);

    //grpcThread.start();
    uiThread.start();
  }

  public void stop() {
//    LOGGER.info("Stopping `all-services` server");
//    grpcServicesServer.shutdownNow();
    hypertraceUIServer.stop();
  }
}
