package org.hypertrace.federated.service;

import com.typesafe.config.Config;
import io.grpc.Deadline;
import io.grpc.ManagedChannelBuilder;
import java.time.Instant;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.grpcutils.client.GrpcClientRequestContextUtil;
import org.hypertrace.core.grpcutils.client.RequestContextClientCallCredsProviderFactory;
import org.hypertrace.gateway.service.GatewayServiceGrpc;
import org.hypertrace.gateway.service.GatewayServiceGrpc.GatewayServiceBlockingStub;
import org.hypertrace.gateway.service.common.util.QueryExpressionUtil;
import org.hypertrace.gateway.service.v1.span.SpansRequest;
import org.hypertrace.gateway.service.v1.span.SpansResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper TimerTask for checking health of dependency data services before starting UI server
 */
public class HypertraceUIServerTimerTask extends TimerTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceUIServerTimerTask.class);

  private static final String RETRIES_CONFIG = "hypertraceUI.init.waittime.retries";
  private static final int DEFAULT_RETRIES = 10;
  private static final String TIMEOUT_CONFIG = "hypertraceUI.init.waittime.timeout";
  private static final int DEFAULT_TIMEOUT = 2;
  private static final String INTERVAL = "hypertraceUI.init.waittime.interval";
  private static final int DEFAULT_INTERVAL = 5;
  private static final String START_PERIOD = "hypertraceUI.init.waittime.start_period";
  private static final int DEFAULT_START_PERIOD = 20;
  private final HypertraceUIServer uiServer;
  private final GatewayServiceBlockingStub client;
  private int numTries;
  private int maxTries;
  private int timeout;
  private long startTime;
  private long interval;
  private long startPeriod;
  private String defaultTenant;

  public HypertraceUIServerTimerTask(Config appConfig, HypertraceUIServer uiServer, String defaultTenant) {
    maxTries = appConfig.hasPath(RETRIES_CONFIG) ? appConfig.getInt(RETRIES_CONFIG) : DEFAULT_RETRIES;
    timeout = appConfig.hasPath(TIMEOUT_CONFIG) ? appConfig.getInt(TIMEOUT_CONFIG) : DEFAULT_TIMEOUT;
    interval = appConfig.hasPath(INTERVAL) ? appConfig.getInt(INTERVAL) : DEFAULT_INTERVAL;
    startPeriod = appConfig.hasPath(START_PERIOD) ? appConfig.getInt(START_PERIOD) : DEFAULT_START_PERIOD;

    this.uiServer = uiServer;
    this.numTries = 0;
    this.defaultTenant = defaultTenant;
    this.startTime = Instant.now().toEpochMilli();

    client = GatewayServiceGrpc.newBlockingStub(ManagedChannelBuilder.forAddress(
            "localhost", appConfig.getInt("service.port")).usePlaintext().build())
            .withCallCredentials(RequestContextClientCallCredsProviderFactory
                    .getClientCallCredsProvider().get());
  }

  public long getStartPeriod() {
    return startPeriod;
  }

  public long getInterval() {
    return interval;
  }

  @Override
  public void run() {
    try {
      if (numTries >= maxTries) {
        cancel();
        LOGGER.info(String.format("Max out attempt [%s] in checking bootstrapping. Manually check " +
                "the status of pinot, all-view-creator, and config-bootstrapper.", numTries));
        uiServer.start();
        return;
      }

      if (executeHealthCheck()) {
        cancel();
        LOGGER.info(String.format("Stack is up after [%s] attempts, and duration [%s] in millis.",
                numTries, Instant.now().toEpochMilli() - startTime));
        uiServer.start();
        return;
      }

    } catch (Exception ex) {
      LOGGER.warn("Failure in dependent service health check. Few data services like pinot is not yet up");
    }
    numTries++;
    LOGGER.info(String.format("Finished an attempt [%s] in checking for bootstrapping status, " +
            "will retry after [%s] seconds", numTries, interval));
  }

  private boolean executeHealthCheck() {
    SpansResponse response = GrpcClientRequestContextUtil.executeInTenantContext(defaultTenant,
            () -> client.withDeadline(Deadline.after(timeout, TimeUnit.SECONDS))
                    .getSpans(buildSpanRequest()));
    return response.getSpansCount() >= 0;
  }

  private SpansRequest buildSpanRequest() {
    return SpansRequest.newBuilder()
            .setStartTimeMillis(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10))
            .setEndTimeMillis(System.currentTimeMillis())
            .addSelection(QueryExpressionUtil.getColumnExpression("EVENT.id"))
            .setLimit(1)
            .build();
  }
}
