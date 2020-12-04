package org.hypertrace.config.service.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hypertrace.core.documentstore.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDocument implements Document {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigDocument.class);
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final String RESOURCE_FIELD_NAME = "resourceName";
    public static final String RESOURCE_NAMESPACE_FIELD_NAME = "resourceNamespace";
    public static final String TENANT_ID_FIELD_NAME = "tenantId";
    public static final String CONTEXT_FIELD_NAME = "context";
    public static final String VERSION_FIELD_NAME = "configVersion";
    public static final String USER_ID_FIELD_NAME = "userId";
    public static final String CONFIG_FIELD_NAME = "config";

    @JsonProperty(value = RESOURCE_FIELD_NAME)
    private String resourceName;

    @JsonProperty(value = RESOURCE_NAMESPACE_FIELD_NAME)
    private String resourceNamespace;

    @JsonProperty(value = TENANT_ID_FIELD_NAME)
    private String tenantId;

    @JsonProperty(value = CONTEXT_FIELD_NAME)
    private String context;

    @JsonProperty(value = VERSION_FIELD_NAME)
    private long configVersion;

    @JsonProperty(value = USER_ID_FIELD_NAME)
    private String userId;

    @JsonSerialize(using = ValueSerializer.class)
    @JsonProperty(value = CONFIG_FIELD_NAME)
    private Value config;

    public static ConfigDocument fromJson(String json) throws IOException {
        return OBJECT_MAPPER.readValue(json, ConfigDocument.class);
    }

    @Override
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            LOGGER.error("Error in converting {} to json", this);
            throw new RuntimeException("Error in converting ConfigDocument to json", ex);
        }
    }

    public static class ValueSerializer extends JsonSerializer<Value> {
        @Override
        public void serialize(Value value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeRawValue(JsonFormat.printer().print(value));
        }
    }
}
