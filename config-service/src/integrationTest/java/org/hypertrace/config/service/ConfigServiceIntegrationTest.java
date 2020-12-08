package org.hypertrace.config.service;

import static org.hypertrace.config.service.IntegrationTestUtils.getConfigValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.Value;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.hypertrace.config.service.v1.ConfigServiceGrpc;
import org.hypertrace.config.service.v1.GetConfigRequest;
import org.hypertrace.config.service.v1.GetConfigResponse;
import org.hypertrace.config.service.v1.UpsertConfigRequest;
import org.hypertrace.config.service.v1.UpsertConfigResponse;
import org.hypertrace.core.grpcutils.client.GrpcClientRequestContextUtil;
import org.hypertrace.core.grpcutils.client.RequestContextClientCallCredsProviderFactory;
import org.hypertrace.core.serviceframework.IntegrationTestServerUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration test for ConfigService
 */
public class ConfigServiceIntegrationTest {

  private static final String RESOURCE_NAME = "foo";
  private static final String RESOURCE_NAMESPACE = "bar";
  private static final String TENANT_1 = "tenant1";
  private static final String TENANT_2 = "tenant2";
  private static final String CONTEXT_1 = "ctx1";
  private static final String CONTEXT_2 = "ctx2";

  private static ConfigServiceGrpc.ConfigServiceBlockingStub configServiceBlockingStub;
  private static Value config1;
  private static Value config2;
  private static Value config3;

  @BeforeAll
  public static void setup() throws IOException {
    System.out.println("Starting Config Service E2E Test");
    IntegrationTestServerUtil.startServices(new String[]{"config-service"});

    Channel channel = ManagedChannelBuilder.forAddress("localhost", 50101)
        .usePlaintext()
        .build();

    configServiceBlockingStub = ConfigServiceGrpc.newBlockingStub(channel)
        .withCallCredentials(
            RequestContextClientCallCredsProviderFactory.getClientCallCredsProvider().get());

    config1 = getConfigValue("config1.yaml");
    config2 = getConfigValue("config2.yaml");
    config3 = getConfigValue("config3.yaml");
  }

  @AfterAll
  public static void teardown() {
    IntegrationTestServerUtil.shutdownServices();
  }

  @Test
  public void testUpsertAndGetConfig() {
    upsertConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1, Optional.empty(), config1);

    GetConfigResponse getConfigResponse = getConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1);
    assertEquals(config1, getConfigResponse.getConfig());

    // upsert second version of config
    upsertConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1, Optional.empty(), config2);

    // upsert config for second tenant
    upsertConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_2, Optional.empty(), config3);

    // get config should return latest config values for respective tenant
    getConfigResponse = getConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1);
    assertEquals(config2, getConfigResponse.getConfig());

    getConfigResponse = getConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_2);
    assertEquals(config3, getConfigResponse.getConfig());
  }

  @Test
  public void testUpsertAndGetConfigWithContext() {
    // upsert config with default or no context
    upsertConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1, Optional.empty(), config1);

    // upsert config with context
    upsertConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1, Optional.of(CONTEXT_1), config2);

    // get default config (without context)
    GetConfigResponse getDefaultConfigResponse =
        getConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1);
    assertEquals(config1, getDefaultConfigResponse.getConfig());

    // get config with context as CONTEXT_1 - should merge the first two configs(which is config3)
    GetConfigResponse getContext1ConfigResponse =
        getConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1, CONTEXT_1);
    assertEquals(config3, getContext1ConfigResponse.getConfig());

    // get config with context as CONTEXT_2 - should return the config with default context
    GetConfigResponse getContext2ConfigResponse =
        getConfig(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_1, CONTEXT_2);
    assertEquals(config1, getContext2ConfigResponse.getConfig());
  }

  private UpsertConfigResponse upsertConfig(String resourceName, String resourceNamespace,
      String tenantId, Optional<String> context, Value config) {
    UpsertConfigRequest.Builder builder = UpsertConfigRequest.newBuilder()
        .setResourceName(resourceName)
        .setResourceNamespace(resourceNamespace)
        .setConfig(config);
    if (context.isPresent()) {
      builder.setContext(context.get());
    }

    return GrpcClientRequestContextUtil.executeInTenantContext(
        tenantId, () -> configServiceBlockingStub.upsertConfig(builder.build()));
  }

  private GetConfigResponse getConfig(String resourceName, String resourceNamespace,
      String tenantId, String... contexts) {
    GetConfigRequest getConfigRequest = GetConfigRequest.newBuilder()
        .setResourceName(resourceName)
        .setResourceNamespace(resourceNamespace)
        .addAllContexts(Arrays.asList(contexts))
        .build();

    return GrpcClientRequestContextUtil.executeInTenantContext(
        tenantId, () -> configServiceBlockingStub.getConfig(getConfigRequest));
  }

}
