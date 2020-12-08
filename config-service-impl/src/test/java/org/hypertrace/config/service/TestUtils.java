package org.hypertrace.config.service;

import static org.hypertrace.config.service.ConfigServiceUtils.DEFAULT_CONTEXT;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.Map;

public class TestUtils {

  public static final String RESOURCE_NAME = "foo";
  public static final String RESOURCE_NAMESPACE = "bar";
  public static final String TENANT_ID = "tenant1";
  public static final String CONTEXT1 = "ctx1";

  public static ConfigResource getConfigResource() {
    return new ConfigResource(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_ID, DEFAULT_CONTEXT);
  }

  public static ConfigResource getConfigResource(String context) {
    return new ConfigResource(RESOURCE_NAME, RESOURCE_NAMESPACE, TENANT_ID, context);
  }

  public static Value getConfigValue() {
    return getConfigValue(Map.of("street", "101A, Street s1",
        "city", "city1","state", "state1"));
  }

  public static Value getConfigValue(Map<String, String> addressFields) {
    Struct.Builder addressBuilder = Struct.newBuilder();
    addressFields.forEach((k, v) ->
        addressBuilder.putFields(k, Value.newBuilder().setStringValue(v).build()));
    Value addressValue = Value.newBuilder().setStructValue(addressBuilder.build()).build();
    Struct billingInfo = Struct.newBuilder()
        .putFields("address", addressValue)
        .build();
    return Value.newBuilder().setStructValue(billingInfo).build();
  }

}
