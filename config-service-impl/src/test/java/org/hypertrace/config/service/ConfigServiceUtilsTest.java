package org.hypertrace.config.service;

import static org.hypertrace.config.service.ConfigServiceUtils.DEFAULT_CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.junit.jupiter.api.Test;

class ConfigServiceUtilsTest {

  @Test
  void merge() {
    assertEquals(getExpectedMergedConfigValue(),
        ConfigServiceUtils.merge(getConfig1Value(), getConfig2Value()));
  }

  @Test
  void getActualContext() {
    assertEquals(DEFAULT_CONTEXT, ConfigServiceUtils.getActualContext(null));
    assertEquals(DEFAULT_CONTEXT, ConfigServiceUtils.getActualContext(""));
    assertEquals("ctx1", ConfigServiceUtils.getActualContext("ctx1"));
  }

  /**
   * This method returns the {@link Value} corresponding to the below JSON
   * { "k1": 10, "k2":"v2", "k3": [ { "k31": "v31" }, { "k32": "v32" } ] }
   */
  private Value getConfig1Value() {
    ListValue listValueForK3 = ListValue.newBuilder()
        .addValues(Value.newBuilder().setStructValue(getStructForKeyValue("k31", "v31")).build())
        .addValues(Value.newBuilder().setStructValue(getStructForKeyValue("k32", "v32")).build())
        .build();
    Struct struct = Struct.newBuilder()
        .putFields("k1", Value.newBuilder().setNumberValue(10).build())
        .putFields("k2", Value.newBuilder().setStringValue("v2").build())
        .putFields("k3", Value.newBuilder().setListValue(listValueForK3).build())
        .build();

    return Value.newBuilder().setStructValue(struct).build();
  }

  /**
   * This method returns the {@link Value} corresponding to the below JSON
   * { "k1": 20, "k3":[ { "k33": "v33" } ], "k4": "v4" }
   */
  private Value getConfig2Value() {
    ListValue listValueForK3 = ListValue.newBuilder()
        .addValues(Value.newBuilder().setStructValue(getStructForKeyValue("k33", "v33")).build())
        .build();
    Struct struct = Struct.newBuilder()
        .putFields("k1", Value.newBuilder().setNumberValue(20).build())
        .putFields("k3", Value.newBuilder().setListValue(listValueForK3).build())
        .putFields("k4", Value.newBuilder().setStringValue("v4").build())
        .build();

    return Value.newBuilder().setStructValue(struct).build();
  }

  /**
   * This method returns the {@link Value} corresponding to the below JSON (obtained by
   * merging config1 and config2) 
   * { "k1": 20, "k2": "v2", "k3": [ { "k33": "v33" } ], "k4": "v4" }
   */
  private Value getExpectedMergedConfigValue() {
    ListValue listValueForK3 = ListValue.newBuilder()
        .addValues(Value.newBuilder().setStructValue(getStructForKeyValue("k33", "v33")).build())
        .build();
    Struct struct = Struct.newBuilder()
        .putFields("k1", Value.newBuilder().setNumberValue(20).build())
        .putFields("k2", Value.newBuilder().setStringValue("v2").build())
        .putFields("k3", Value.newBuilder().setListValue(listValueForK3).build())
        .putFields("k4", Value.newBuilder().setStringValue("v4").build())
        .build();

    return Value.newBuilder().setStructValue(struct).build();
  }

  private Struct getStructForKeyValue(String key, String value) {
    return Struct.newBuilder()
        .putFields(key, Value.newBuilder().setStringValue(value).build())
        .build();
  }
}
