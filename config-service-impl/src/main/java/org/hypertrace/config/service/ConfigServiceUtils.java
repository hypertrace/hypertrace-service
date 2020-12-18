package org.hypertrace.config.service;

import com.google.common.base.Strings;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class contains utility methods.
 */
public class ConfigServiceUtils {

  public static final String DEFAULT_CONTEXT = "DEFAULT-CONTEXT";

  private ConfigServiceUtils() {
    // to prevent instantiation
  }

  /**
   * Deep merge the specified {@link Value} configs with overridingConfig taking precedence over
   * defaultConfig for the same keys.
   * @param defaultConfig
   * @param overridingConfig
   * @return the resulting config obtained after merging defaultConfig and overridingConfig
   */
  public static Value merge(Value defaultConfig, Value overridingConfig) {
    if (isNull(defaultConfig)) {
      return overridingConfig;
    } else if (isNull(overridingConfig)) {
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

  /**
   * Get the actual context from rawContext. Specifically, it handles the case where rawContext
   * can be null or empty which is equivalent to default context.
   * @param rawContext
   * @return
   */
  public static String getActualContext(String rawContext) {
    return Strings.isNullOrEmpty(rawContext) ? DEFAULT_CONTEXT : rawContext;
  }

  private static boolean isNull(Value value) {
    return value == null || value.getKindCase() == Value.KindCase.NULL_VALUE;
  }
}
