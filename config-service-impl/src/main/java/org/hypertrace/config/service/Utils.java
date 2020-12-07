package org.hypertrace.config.service;

import com.google.common.base.Strings;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.LinkedHashMap;
import java.util.Map;

public class Utils {

  public static final String DEFAULT_CONTEXT = "DEFAULT-CONTEXT";

  private Utils() {
    // to prevent instantiation
  }

  public static Value merge(Value defaultConfig, Value overridingConfig) {
    if (defaultConfig == null) {
      return overridingConfig;
    } else if (overridingConfig == null) {
      return defaultConfig;
    }

    // Only if both - defaultConfig and overridingConfig are of kind Struct(Map), then merge
    // the common fields. Otherwise, just return the overridingConfig
    if (defaultConfig.getKindCase() == Value.KindCase.STRUCT_VALUE
        && overridingConfig.getKindCase() == Value.KindCase.STRUCT_VALUE) {
      Map<String, Value> defaultConfigMap = defaultConfig.getStructValue().getFieldsMap();
      Map<String, Value> overridingConfigMap = overridingConfig.getStructValue().getFieldsMap();

      Map<String, Value> resultConfigMap = new LinkedHashMap<>(defaultConfigMap);
      for (Map.Entry<String, Value> entry : overridingConfigMap.entrySet()) {
        resultConfigMap.put(entry.getKey(),
            merge(defaultConfigMap.get(entry.getKey()), entry.getValue()));
      }
      Struct struct = Struct.newBuilder().putAllFields(resultConfigMap).build();
      return Value.newBuilder().setStructValue(struct).build();
    } else {
      return overridingConfig;
    }
  }

  public static String getActualContext(String rawContext) {
    return Strings.isNullOrEmpty(rawContext) ? DEFAULT_CONTEXT : rawContext;
  }
}
