package org.hypertrace.federated.service;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URL;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.hypertrace.graphql.service.GraphQlServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves both hypertrace-ui and graphql
 */
public class HypertraceUIServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceUIServer.class);

  private static final int PORT = 2020;

  private Server server;
  private GraphQlServiceImpl graphQlService;

  public HypertraceUIServer(Config appConfig) {
    server = new Server(PORT);
    graphQlService = new GraphQlServiceImpl(appConfig);

    ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setBaseResource(getBaseResource());

    ContextHandler baseContextHandler = new ContextHandler();
    baseContextHandler.setContextPath("/");
    baseContextHandler.setHandler(resourceHandler);

    ContextHandler homeContextHandler = new ContextHandler();
    homeContextHandler.setContextPath("/home");
    homeContextHandler.setHandler(resourceHandler);

    ContextHandler applicationFlowContextHandler = new ContextHandler();
    applicationFlowContextHandler.setContextPath("/application-flow");
    applicationFlowContextHandler.setHandler(resourceHandler);

    ContextHandler servicesContextHandler = new ContextHandler();
    servicesContextHandler.setContextPath("/services");
    servicesContextHandler.setHandler(resourceHandler);

    ContextHandler backendsContextHandler = new ContextHandler();
    backendsContextHandler.setContextPath("/backends");
    backendsContextHandler.setHandler(resourceHandler);

    ContextHandler explorerContextHandler = new ContextHandler();
    explorerContextHandler.setContextPath("/explorer");
    explorerContextHandler.setHandler(resourceHandler);

    ContextHandlerCollection contexts = new ContextHandlerCollection();
    contexts.setHandlers(new Handler[] {
            baseContextHandler,
            homeContextHandler,
            applicationFlowContextHandler,
            servicesContextHandler,
            backendsContextHandler,
            explorerContextHandler,
            graphQlService.getContextHandler()});

//    HandlerList handlers = new HandlerList();
//    handlers.setHandlers(new Handler[]{resourceHandler, graphQlService.getContextHandler(),
//            new DefaultHandler()});

    server.setHandler(contexts);
    server.setStopAtShutdown(true);
  }

  private Resource getBaseResource() {
    try {
      URL url = HypertraceUIServer.class.getResource("/hypertrace-ui/index.html");
      if (url == null) {
        throw new RuntimeException("Failed to find hypertrace-ui resource");
      }
      URI baseURI = url.toURI().resolve("./");
      LOGGER.info("base URI for static resource:" + baseURI);
      return Resource.newResource(baseURI);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void start() {
    try {
      this.server.start();
      LOGGER.info("Started HypertraceUI service on port: {}.", PORT);
      this.server.join();
    } catch (Exception var4) {
      LOGGER.error("Failed to start HypertraceUI servlet.");
    }
  }

  public void stop() {
    try {
      graphQlService.shutdown();
      server.stop();
    } catch (Exception e) {
      LOGGER.error("Error stopping HypertraceUI server");
    }
  }
}
