package org.hypertrace.config.service;

import com.google.protobuf.Value;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.hypertrace.config.service.store.ConfigResource;
import org.hypertrace.config.service.store.ConfigStore;
import org.hypertrace.config.service.v1.ConfigServiceGrpc;
import org.hypertrace.config.service.v1.GetConfigRequest;
import org.hypertrace.config.service.v1.GetConfigResponse;
import org.hypertrace.config.service.v1.UpsertConfigRequest;
import org.hypertrace.config.service.v1.UpsertConfigResponse;
import org.hypertrace.core.grpcutils.context.RequestContext;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class ConfigServiceGrpcImpl extends ConfigServiceGrpc.ConfigServiceImplBase {

    private final ConfigStore configStore;

    public ConfigServiceGrpcImpl(ConfigStore configStore) {
        this.configStore = configStore;
    }

    // TODO: error handling
    @Override
    public void upsertConfig(UpsertConfigRequest request, StreamObserver<UpsertConfigResponse> responseObserver) {
        UpsertConfigResponse response;
        try {
            long configVersion = configStore.writeConfig(getConfigResource(request), getUserId(), request.getConfig());
            response = UpsertConfigResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Upsert successful")
                    .setConfigVersion(configVersion)
                    .build();
        } catch (IOException e) {
            response = getFailedResponse("Upsert failed with exception : " + e.getMessage());
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getConfig(GetConfigRequest request, StreamObserver<GetConfigResponse> responseObserver) {
        GetConfigResponse response;
        if (request.getConfigVersion() != 0) {  // if configVersion is present
            Optional<String> context = Optional.empty();
            // get the config version corresponding to the most specific context(last element in the contexts list)
            if (!request.getContextsList().isEmpty()) {
                context = Optional.of(request.getContexts(request.getContextsCount() - 1));
            }
            response = configStore.getConfig(getConfigResource(request, context), Optional.of(request.getConfigVersion()));
        } else {
            GetConfigResponse defaultContextResponse =
                    configStore.getConfig(getConfigResource(request, Optional.empty()), Optional.empty());
            Value config = defaultContextResponse.getConfig();

            // get the configs for the contexts mentioned in the request and merge them in the specified order
            for (String context : request.getContextsList()) {
                GetConfigResponse contextSpecificConfigResponse =
                        configStore.getConfig(getConfigResource(request, Optional.of(context)), Optional.empty());
                Value contextSpecificConfig = contextSpecificConfigResponse.getConfig();
                if (config == null) {
                    config = contextSpecificConfig;
                } else if (contextSpecificConfig != null) {
                    config = merge(config, contextSpecificConfig);
                }
            }
            response = GetConfigResponse.newBuilder()
                    .setConfig(config)
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private Optional<String> optionalOf(String context) {
        if (context == null || StringUtils.isEmpty(context)) {
            return Optional.empty();
        } else {
            return Optional.of(context);
        }
    }

    private UpsertConfigResponse getFailedResponse(String message) {
        return UpsertConfigResponse.newBuilder()
                .setSuccess(false)
                .setMessage(message)
                .build();
    }

    private ConfigResource getConfigResource(UpsertConfigRequest upsertConfigRequest) {
        return new ConfigResource(upsertConfigRequest.getResourceName(),
                upsertConfigRequest.getResourceNamespace(),
                getTenantId(),
                optionalOf(upsertConfigRequest.getContext()));
    }

    private ConfigResource getConfigResource(GetConfigRequest getConfigRequest, Optional<String> context) {
        return new ConfigResource(getConfigRequest.getResourceName(),
                getConfigRequest.getResourceNamespace(),
                getTenantId(),
                context);
    }

    private String getTenantId() {
        Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
        if (tenantId.isEmpty()) {
            return "DEFAULT-TENANT";
//            throw new IllegalArgumentException("Tenant id is missing in the request.");
        }
        return RequestContext.CURRENT.get().getTenantId().get();    // TODO: this doesn't seem to be working. Once resolved uncomment the above line
    }

    private Value merge(Value defaultConfig, Value overridingConfig) {
        // Only if both - defaultConfig and overridingConfig are of kind Struct(Map), then merge the common fields
        // Otherwise, just return the overridingConfig
        if (defaultConfig.getKindCase() == Value.KindCase.STRUCT_VALUE
                && overridingConfig.getKindCase() == Value.KindCase.STRUCT_VALUE) {
            Map<String, Value> defaultConfigMap = defaultConfig.getStructValue().getFieldsMap();
            Map<String, Value> overridingConfigMap = overridingConfig.getStructValue().getFieldsMap();
            for (Map.Entry<String, Value> entry : defaultConfigMap.entrySet()) {
                String configKey = entry.getKey();
                if (overridingConfigMap.containsKey(configKey)) {
                    overridingConfigMap.put(configKey, merge(entry.getValue(), overridingConfigMap.get(configKey)));
                } else {
                    overridingConfigMap.put(configKey, entry.getValue());
                }
            }
        }
        return overridingConfig;
    }

    // TODO : get the userId from the context
    private String getUserId() {
        return null;
    }
}
