package org.hypertrace.federated.service;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URL;
import java.util.TimerTask;
import org.hypertrace.core.bootstrapper.BootstrapArgs;
import org.hypertrace.core.bootstrapper.BootstrapRunner;
import org.hypertrace.core.bootstrapper.ConfigBootstrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bootstrap task for initializing required attributes of Hypertrace. It will retry for max
 * attempts based on a fixed delay.
 */
public class BootstrapTimerTask extends TimerTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapTimerTask.class);

  private static final String RETRIES_CONFIG = "hypertraceUI.bootstrap.task.retries";
  private static final int DEFAULT_RETRIES = 3;
  private static final String INTERVAL = "hypertraceUI.bootstrap.task.interval";
  private static final int DEFAULT_INTERVAL = 5;
  private static final String SHOULD_EXIT = "hypertraceUI.bootstrap.task.should_exit";
  private static final boolean DEFAULT_SHOULD_EXIT = false;

  private final BootstrapArgs bootstrapArgs;
  private final BootstrapRunner bootstrapRunner;
  private int numRetries;
  private int maxRetries;
  private boolean shouldExit;
  private int interval;
  private boolean isDone;

  public BootstrapTimerTask(Config appConfig) {
    this.numRetries = 0;
    this.isDone = false;

    maxRetries = appConfig.hasPath(RETRIES_CONFIG) ? appConfig.getInt(RETRIES_CONFIG) : DEFAULT_RETRIES;
    interval = appConfig.hasPath(INTERVAL) ? appConfig.getInt(INTERVAL) : DEFAULT_INTERVAL;
    shouldExit = appConfig.hasPath(SHOULD_EXIT) ? appConfig.getBoolean(SHOULD_EXIT) : DEFAULT_SHOULD_EXIT;

    String basePath = getBasePath();
    String applicationConf = basePath + "application.conf";
    String[] args = new String[] { "-c", applicationConf, "-C", basePath, "--upgrade"};

    bootstrapArgs = BootstrapArgs.from(args);
    bootstrapRunner = ConfigBootstrapper.bootstrapper(bootstrapArgs);
  }

  public int getInterval() {
    return interval;
  }

  @Override
  public void run() {
    if(isDone) {
      cancel();
      LOGGER.info("Already finished bootstrapping attributes");
      return;
    }

    if (numRetries >= maxRetries) {
      cancel();
      if (shouldExit) {
        LOGGER.info(String.format("Max out attempts [%s] in bootstrapping attributes. Stopping the service...!!", numRetries));
        System.exit(1);
      }
      LOGGER.info(String.format("Max out attempts [%s] in bootstrapping attributes. " +
              "Pl try manually running config-bootstrapper job!!", numRetries));
      return;
    }

    try {
      LOGGER.info(String.format("Starting an attempt [%s] for bootrapping attributes", numRetries));
      bootstrapRunner.execute(bootstrapArgs);
      LOGGER.info("Successfully finished bootstrapping of attributes!!");
      isDone = true;
      cancel();
    } catch (Exception ex) {
      LOGGER.error(String.format("Failure in bootsrapping attributes, will try after [%s]", getInterval()));
    } finally {
      numRetries++;
    }
  }

  private String getBasePath() {
    try {
      URL url = BootstrapTimerTask.class.getResource("/configs/config-bootstrapper/application.conf");
      if (url == null) {
        throw new RuntimeException("Failed to find config-bootstapper resource");
      }
      URI baseURI = url.toURI().resolve("./");
      LOGGER.info("Base URI for config-bootstrapper resource:" + baseURI);
      return baseURI.getPath();
    } catch (Exception ex) {
      throw new RuntimeException("Failure in finding config-bootstapper resource", ex);
    }
  }
}
