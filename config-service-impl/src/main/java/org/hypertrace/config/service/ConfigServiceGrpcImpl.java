package org.hypertrace.config.service;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.stub.StreamObserver;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hypertrace.config.service.store.ConfigStore;
import org.hypertrace.config.service.v1.ConfigServiceGrpc;
import org.hypertrace.config.service.v1.GetConfigRequest;
import org.hypertrace.config.service.v1.GetConfigResponse;
import org.hypertrace.config.service.v1.UpsertConfigRequest;
import org.hypertrace.config.service.v1.UpsertConfigResponse;
import org.hypertrace.core.grpcutils.context.RequestContext;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class ConfigServiceGrpcImpl extends ConfigServiceGrpc.ConfigServiceImplBase {

  private static final String DEFAULT_CONTEXT = "DEFAULT-CONTEXT";

  private final ConfigStore configStore;

  public ConfigServiceGrpcImpl(ConfigStore configStore) {
    this.configStore = configStore;
  }

  @Override
  public void upsertConfig(UpsertConfigRequest request,
      StreamObserver<UpsertConfigResponse> responseObserver) {
    try {
      UpsertConfigResponse response = configStore
          .writeConfig(getConfigResource(request), getUserId(), request.getConfig());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Upsert failed for request:{}", request, e);
      responseObserver.onError(e);
    }
  }

  @Override
  public void getConfig(GetConfigRequest request,
      StreamObserver<GetConfigResponse> responseObserver) {
    try {
      GetConfigResponse response;
      if (request.getConfigVersion() != 0) {  // if configVersion is present
        String context = null;
        // get the config version corresponding to the most specific context(last element in the contexts list)
        if (!request.getContextsList().isEmpty()) {
          context = request.getContexts(request.getContextsCount() - 1);
        }
        response = configStore.getConfig(getConfigResource(request, context),
            Optional.of(request.getConfigVersion()));
      } else {
        GetConfigResponse defaultContextResponse =
            configStore.getConfig(getConfigResource(request, null), Optional.empty());
        Value config = defaultContextResponse.getConfig();

        // get the configs for the contexts mentioned in the request and merge them in the specified order
        for (String context : request.getContextsList()) {
          GetConfigResponse contextSpecificConfigResponse =
              configStore.getConfig(getConfigResource(request, context), Optional.empty());
          Value contextSpecificConfig = contextSpecificConfigResponse.getConfig();
          config = merge(config, contextSpecificConfig);
        }
        response = GetConfigResponse.newBuilder()
            .setConfig(config)
            .build();
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      log.error("Get config failed for request:{}", request, e);
      responseObserver.onError(e);
    }
  }

  private ConfigResource getConfigResource(UpsertConfigRequest upsertConfigRequest) {
    return new ConfigResource(upsertConfigRequest.getResourceName(),
        upsertConfigRequest.getResourceNamespace(),
        getTenantId(),
        getActualContext(upsertConfigRequest.getContext()));
  }

  private ConfigResource getConfigResource(GetConfigRequest getConfigRequest, String context) {
    return new ConfigResource(getConfigRequest.getResourceName(),
        getConfigRequest.getResourceNamespace(),
        getTenantId(),
        getActualContext(context));
  }

  private String getActualContext(String rawContext) {
    return StringUtils.isBlank(rawContext) ? DEFAULT_CONTEXT : rawContext;
  }

  private String getTenantId() {
    Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
    if (tenantId.isEmpty()) {
      throw new IllegalArgumentException("Tenant id is missing in the request.");
    }
    return tenantId.get();
  }

  private Value merge(Value defaultConfig, Value overridingConfig) {
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

  // TODO : get the userId from the context
  private String getUserId() {
    return null;
  }
}
