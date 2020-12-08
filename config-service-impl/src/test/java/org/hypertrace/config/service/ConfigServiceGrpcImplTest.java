package org.hypertrace.config.service;

import static org.hypertrace.config.service.TestUtils.CONTEXT1;
import static org.hypertrace.config.service.TestUtils.RESOURCE_NAME;
import static org.hypertrace.config.service.TestUtils.RESOURCE_NAMESPACE;
import static org.hypertrace.config.service.TestUtils.TENANT_ID;
import static org.hypertrace.config.service.TestUtils.getConfigResource;
import static org.hypertrace.config.service.TestUtils.getConfigValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Value;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.hypertrace.config.service.store.ConfigStore;
import org.hypertrace.config.service.v1.GetConfigRequest;
import org.hypertrace.config.service.v1.GetConfigResponse;
import org.hypertrace.config.service.v1.UpsertConfigRequest;
import org.hypertrace.config.service.v1.UpsertConfigResponse;
import org.hypertrace.core.grpcutils.client.GrpcClientRequestContextUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConfigServiceGrpcImplTest {

  private static ConfigServiceGrpcImpl configServiceGrpc;

  private static Value configValueWithContext;
  private static Value configValueWithoutContext;
  private static Value mergedConfigValue;

  @BeforeAll
  static void setUp() throws IOException {
    configValueWithoutContext = getConfigValue(Map.of("street", "101A, Street s1",
        "city", "city1"));
    configValueWithContext = getConfigValue(Map.of("street", "202B, Street s2",
        "state", "state2"));
    mergedConfigValue = getConfigValue(Map.of("street", "202B, Street s2",
        "city", "city1","state", "state2"));

    ConfigStore configStore = mock(ConfigStore.class);

    ConfigResource configResourceWithoutContext = getConfigResource();
    when(configStore.writeConfig(eq(configResourceWithoutContext), eq(""),
        eq(configValueWithoutContext))).thenReturn(7L, 8L);

    ConfigResource configResourceWithContext = getConfigResource(CONTEXT1);
    when(configStore.writeConfig(eq(configResourceWithContext), eq(""),
        eq(configValueWithContext))).thenReturn(2L);

    when(configStore.getConfig(eq(configResourceWithoutContext)))
        .thenReturn(configValueWithoutContext);

    when(configStore.getConfig(eq(configResourceWithContext)))
        .thenReturn(configValueWithContext);

    configServiceGrpc = new ConfigServiceGrpcImpl(configStore);
  }

  @Test
  void upsertConfig() {
    StreamObserver<UpsertConfigResponse> responseObserver = mock(StreamObserver.class);
    Runnable runnableWithoutContext = () -> configServiceGrpc.upsertConfig(
        getUpsertConfigRequest("", configValueWithoutContext), responseObserver);
    GrpcClientRequestContextUtil.executeInTenantContext(TENANT_ID, runnableWithoutContext);
    GrpcClientRequestContextUtil.executeInTenantContext(TENANT_ID, runnableWithoutContext);

    Runnable runnableWithContext = () -> configServiceGrpc.upsertConfig(
        getUpsertConfigRequest(CONTEXT1, configValueWithContext), responseObserver);
    GrpcClientRequestContextUtil.executeInTenantContext(TENANT_ID, runnableWithContext);

    ArgumentCaptor<UpsertConfigResponse> upsertConfigResponseCaptor =
        ArgumentCaptor.forClass(UpsertConfigResponse.class);
    verify(responseObserver, times(3))
        .onNext(upsertConfigResponseCaptor.capture());
    verify(responseObserver, times(3)).onCompleted();
    verify(responseObserver, never()).onError(any(Throwable.class));
    List<UpsertConfigResponse> actualResponseList = upsertConfigResponseCaptor.getAllValues();
    assertEquals(3, actualResponseList.size());
    assertEquals(UpsertConfigResponse.newBuilder().setConfigVersion(7).build(),
        actualResponseList.get(0));
    assertEquals(UpsertConfigResponse.newBuilder().setConfigVersion(8).build(),
        actualResponseList.get(1));
    assertEquals(UpsertConfigResponse.newBuilder().setConfigVersion(2).build(),
        actualResponseList.get(2));
  }

  @Test
  void getConfig() {
    StreamObserver<GetConfigResponse> responseObserver = mock(StreamObserver.class);
    Runnable runnableWithoutContext = () ->
        configServiceGrpc.getConfig(getGetConfigRequest(), responseObserver);
    GrpcClientRequestContextUtil.executeInTenantContext(TENANT_ID, runnableWithoutContext);

    Runnable runnableWithContext = () ->
        configServiceGrpc.getConfig(getGetConfigRequest(CONTEXT1), responseObserver);
    GrpcClientRequestContextUtil.executeInTenantContext(TENANT_ID, runnableWithContext);

    ArgumentCaptor<GetConfigResponse> getConfigResponseCaptor =
        ArgumentCaptor.forClass(GetConfigResponse.class);
    verify(responseObserver, times(2))
        .onNext(getConfigResponseCaptor.capture());
    verify(responseObserver, times(2)).onCompleted();
    verify(responseObserver, never()).onError(any(Throwable.class));

    List<GetConfigResponse> actualResponseList = getConfigResponseCaptor.getAllValues();
    assertEquals(2, actualResponseList.size());
    assertEquals(GetConfigResponse.newBuilder().setConfig(configValueWithoutContext).build(),
        actualResponseList.get(0));
    assertEquals(GetConfigResponse.newBuilder().setConfig(mergedConfigValue).build(),
        actualResponseList.get(1));
  }

  private UpsertConfigRequest getUpsertConfigRequest(String context, Value config) {
    return UpsertConfigRequest.newBuilder()
        .setResourceName(RESOURCE_NAME)
        .setResourceNamespace(RESOURCE_NAMESPACE)
        .setContext(context)
        .setConfig(config)
        .build();
  }

  private GetConfigRequest getGetConfigRequest(String... contexts) {
    return GetConfigRequest.newBuilder()
        .setResourceName(RESOURCE_NAME)
        .setResourceNamespace(RESOURCE_NAMESPACE)
        .addAllContexts(Arrays.asList(contexts))
        .build();
  }

}