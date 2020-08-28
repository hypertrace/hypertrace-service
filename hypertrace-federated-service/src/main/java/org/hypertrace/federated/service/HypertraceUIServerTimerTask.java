package org.hypertrace.federated.service;

import static org.hypertrace.core.query.service.util.QueryRequestUtil.createTimeFilter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.grpcutils.client.GrpcClientRequestContextUtil;
import org.hypertrace.core.query.service.QueryServiceImplConfig;
import org.hypertrace.core.query.service.api.ColumnIdentifier;
import org.hypertrace.core.query.service.api.Expression;
import org.hypertrace.core.query.service.api.Filter;
import org.hypertrace.core.query.service.api.Operator;
import org.hypertrace.core.query.service.api.QueryRequest;
import org.hypertrace.core.query.service.api.QueryRequest.Builder;
import org.hypertrace.core.query.service.api.QueryServiceGrpc;
import org.hypertrace.core.query.service.api.ResultSetChunk;
import org.hypertrace.core.query.service.client.QueryServiceClient;
import org.hypertrace.core.query.service.client.QueryServiceConfig;
import org.hypertrace.gateway.service.GatewayServiceGrpc;
import org.hypertrace.gateway.service.GatewayServiceGrpc.GatewayServiceBlockingStub;
import org.hypertrace.gateway.service.v1.span.Spans;
import org.hypertrace.gateway.service.v1.span.SpansRequest;
import org.hypertrace.gateway.service.v1.span.SpansResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypertraceUIServerTimerTask extends TimerTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(HypertraceUIServerTimerTask.class);

  private int numTries;
  private int maxTries;
  private int timeout;
  private final HypertraceUIServer uiServer;
  //private final QueryServiceClient client;
  private final Map<String, String> contextMap;
  GatewayServiceBlockingStub client;

  public HypertraceUIServerTimerTask(int maxTries, int timeout, String defaultTenantId,
                                     HypertraceUIServer uiServer) {
    this.maxTries = maxTries;
    this.uiServer = uiServer;
    this.numTries = 0;
    this.timeout = timeout;
    this.contextMap = Map.of("x-tenant-id", defaultTenantId);

    /*Config config = ConfigFactory.parseMap(Map.of("host", "localhost",
            "port", "9001"));
    QueryServiceConfig queryServiceConfig = new QueryServiceConfig(config);
    client = new QueryServiceClient(queryServiceConfig);*/

    ManagedChannel managedChannel =
            ManagedChannelBuilder.forAddress(
                    "localhost", 9001)
                    .usePlaintext()
                    .build();
    client = GatewayServiceGrpc.newBlockingStub(managedChannel);
  }

  @Override
  public void run() {
    try {
      if (numTries >= maxTries || executeHealthCheck(buildSpanRequest(), contextMap, 2*1000)) {
        cancel();
        LOGGER.info(String.format("Starting UI server after [%s] attempts for config bootstrap", numTries));
        uiServer.start();
      }
    } catch (Exception ex) {
      LOGGER.info("Failed in checking attributes from query service, so it is not up");
    }
    numTries++;
    LOGGER.info(String.format("Checking for for bootstrapper job to finish, attempt:[%s]", numTries));
  }

  /*private boolean executeHealthCheck() {
    Iterator<ResultSetChunk> iterator = client.executeQuery(buildSimpleQuery(), contextMap, timeout*1000);
    if (iterator.hasNext()) {
      ResultSetChunk resultSetChunk = iterator.next();
      return resultSetChunk.getHasError();
    }
    return false;
  }

  private QueryRequest buildSimpleQuery() {
    Builder builder = QueryRequest.newBuilder();
    ColumnIdentifier spanId = ColumnIdentifier.newBuilder().setColumnName("EVENT.id").build();
    builder.addSelection(Expression.newBuilder().setColumnIdentifier(spanId).build());

    Filter startTimeFilter =
            createTimeFilter(
                    "EVENT.start_time_millis",
                    Operator.GT,
                    System.currentTimeMillis() - 1000 * 60);
    Filter endTimeFilter =
            createTimeFilter("EVENT.end_time_millis", Operator.LT, System.currentTimeMillis());

    Filter andFilter =
            Filter.newBuilder()
                    .setOperator(Operator.AND)
                    .addChildFilter(startTimeFilter)
                    .addChildFilter(endTimeFilter)
                    .build();
    builder.setFilter(andFilter);

    return builder.build();
  }*/

  private void buildSimpleSpansQuery() {
    ManagedChannel managedChannel =
            ManagedChannelBuilder.forAddress(
                    "localhost", 9001)
                    .usePlaintext()
                    .build();
    GatewayServiceBlockingStub blockingStub = GatewayServiceGrpc.newBlockingStub(managedChannel);

  }

  private boolean executeHealthCheck(
          SpansRequest request, Map<String, String> context, int timeoutMillis) {
    SpansResponse response = GrpcClientRequestContextUtil.executeWithHeadersContext(
            context,
            () -> client.withDeadline(Deadline.after(timeoutMillis, TimeUnit.MILLISECONDS))
                            .getSpans(request));
    if (response.getSpansCount() >= 0) { return true; }
    return false;
  }

  private SpansRequest buildSpanRequest() {
    return SpansRequest.newBuilder()
            .setStartTimeMillis(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10))
            .setEndTimeMillis(System.currentTimeMillis()).setLimit(1).build();
  }
}
