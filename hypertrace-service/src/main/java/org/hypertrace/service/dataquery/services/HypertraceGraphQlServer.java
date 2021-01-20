package org.hypertrace.service.dataquery.services;

import com.typesafe.config.Config;
import org.eclipse.jetty.server.Server;
import org.hypertrace.graphql.service.GraphQlServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypertraceGraphQlServer {

  private static final Logger LOG = LoggerFactory.getLogger(HypertraceGraphQlServer.class);

  private final Server server;
  private final GraphQlServiceImpl graphQlService;

  public HypertraceGraphQlServer(Config graphQlServiceAppConfig) {
    graphQlService = new GraphQlServiceImpl(graphQlServiceAppConfig);
    server = new Server(graphQlService.getGraphQlServiceConfig().getServicePort());
    server.setHandler(graphQlService.getContextHandler());
    server.setStopAtShutdown(true);
  }

  protected void doStart() {
    LOG.info("Starting GraphQl server");
    try {
      server.start();
    } catch (Exception e) {
      LOG.error("Failed to start GraphQl server");
      throw new RuntimeException(e);
    }

    try {
      server.join();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ie);
    }
  }

  protected void doStop() {
    LOG.info("Shutting down GraphQl service");

    graphQlService.shutdown();

    while (!server.isStopped()) {
      try {
        server.stop();
      } catch (Exception e) {
        LOG.error("Failed to shutdown GraphQl service");
        throw new RuntimeException(e);
      }
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException ignore) {
    }
  }
}
