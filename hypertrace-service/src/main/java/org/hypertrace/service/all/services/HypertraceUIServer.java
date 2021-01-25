package org.hypertrace.service.all.services;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URL;
import java.util.Timer;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.hypertrace.graphql.service.GraphQlServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves both the Hypertrace UI and GraphQL APIs used by it.
 */
public class HypertraceUIServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceUIServer.class);

  private static final String PORT_CONFIG = "hypertraceUI.port";
  private static final int DEFAULT_PORT = 2020;
  private static final String DEFAULT_TENANT_ID_CONFIG = "defaultTenantId";

  //private final Server server;
  //private final GraphQlServiceImpl graphQlService;
  private final Config appConfig;
  private final Config graphQlServiceAppConfig;
  private final int port;

  public HypertraceUIServer(Config appConfig, Config graphQlServiceAppConfig) {
    this.appConfig = appConfig;
    this.graphQlServiceAppConfig = graphQlServiceAppConfig;
    this.port = appConfig.hasPath(PORT_CONFIG) ? appConfig.getInt(PORT_CONFIG) : DEFAULT_PORT;

//    server = new Server(port);
//    graphQlService = new GraphQlServiceImpl(graphQlServiceAppConfig);
//
//    ServletContextHandler servletContextHandler = graphQlService.getContextHandler();
//    servletContextHandler.setBaseResource(getBaseResource());
//    servletContextHandler.setWelcomeFiles(new String[] {"index.html"});
//    servletContextHandler.addServlet(DefaultServlet.class, "/");
//
//    RewriteHandler rewriteHandler = new RewriteHandler();
//    rewriteHandler.setRewriteRequestURI(true);
//    rewriteHandler.setRewritePathInfo(false);
//    rewriteHandler.setOriginalPathAttribute("requestedPath");
//
//    String graphqlPath = this.graphQlService.getGraphQlServiceConfig().getGraphqlUrlPath();
//
//    // Doesn't start with the graphql root, /assets/, or end with .png, .css,  or .js
//    String matchRegex = "^(?!" + graphqlPath + "|/assets/).*(?<!\\.png|\\.css|\\.js)$";
//    rewriteHandler.addRule(new RewriteRegexRule(matchRegex, "/index.html"));
//
//    rewriteHandler.setHandler(servletContextHandler);
//    server.setHandler(rewriteHandler);
//    server.setStopAtShutdown(true);
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

  public void startWithTimerTasks() {
    BootstrapTimerTask bootstrapTimerTask = new BootstrapTimerTask(appConfig);
    new Timer().scheduleAtFixedRate(bootstrapTimerTask, 0, bootstrapTimerTask.getInterval() * 1000);

//    String defaultTenant = graphQlServiceAppConfig.getString(DEFAULT_TENANT_ID_CONFIG);
//    HypertraceUIServerTimerTask timerTask = new HypertraceUIServerTimerTask(appConfig,
//            this, defaultTenant);
//    LOGGER.info(String.format("Starting a timer task for checking health for bootstrapping process, " +
//            "will try first attempt after [%s] seconds", timerTask.getStartPeriod()));
//    new Timer().scheduleAtFixedRate(timerTask, timerTask.getStartPeriod() * 1000,
//            timerTask.getInterval() * 1000);
  }

  public void start() {
//    try {
//      this.server.start();
//      LOGGER.info("Started Hypertrace UI service on port: {}.", port);
//      this.server.join();
//    } catch (Exception var4) {
//      LOGGER.error("Failed to start HypertraceUI servlet.");
//    }
  }

  public void stop() {
//    try {
//      graphQlService.shutdown();
//      server.stop();
//    } catch (Exception e) {
//      LOGGER.error("Error stopping HypertraceUI server");
//    }
  }
}
