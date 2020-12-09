package org.hypertrace.config.service;

import static org.hypertrace.config.service.ConfigServiceUtils.DEFAULT_CONTEXT;
import static org.hypertrace.config.service.ConfigServiceUtils.getActualContext;
import static org.hypertrace.config.service.ConfigServiceUtils.merge;

import com.google.protobuf.Value;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.config.service.store.ConfigStore;
import org.hypertrace.config.service.v1.ConfigServiceGrpc;
import org.hypertrace.config.service.v1.GetConfigRequest;
import org.hypertrace.config.service.v1.GetConfigResponse;
import org.hypertrace.config.service.v1.UpsertConfigRequest;
import org.hypertrace.config.service.v1.UpsertConfigResponse;
import org.hypertrace.core.grpcutils.context.RequestContext;

import java.util.Optional;

/**
 * Implementation for the gRPC service.
 */
@Slf4j
public class ConfigServiceGrpcImpl extends ConfigServiceGrpc.ConfigServiceImplBase {

  private final ConfigStore configStore;

  public ConfigServiceGrpcImpl(ConfigStore configStore) {
    this.configStore = configStore;
  }

  @Override
  public void upsertConfig(UpsertConfigRequest request,
      StreamObserver<UpsertConfigResponse> responseObserver) {
    try {
      long configVersion =
          configStore.writeConfig(getConfigResource(request), getUserId(), request.getConfig());
      responseObserver.onNext(UpsertConfigResponse.newBuilder()
          .setConfigVersion(configVersion)
          .build());
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
      Value config = configStore.getConfig(getConfigResource(request));

      // get the configs for the contexts mentioned in the request and merge them in the specified order
      for (String context : request.getContextsList()) {
        Value contextSpecificConfig = configStore.getConfig(getConfigResource(request, context));
        config = merge(config, contextSpecificConfig);
      }
      responseObserver.onNext(GetConfigResponse.newBuilder()
          .setConfig(config)
          .build());
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

  private ConfigResource getConfigResource(GetConfigRequest getConfigRequest) {
    return new ConfigResource(getConfigRequest.getResourceName(),
        getConfigRequest.getResourceNamespace(),
        getTenantId(),
        DEFAULT_CONTEXT);
  }

  private ConfigResource getConfigResource(GetConfigRequest getConfigRequest, String context) {
    return new ConfigResource(getConfigRequest.getResourceName(),
        getConfigRequest.getResourceNamespace(),
        getTenantId(),
        getActualContext(context));
  }

  private String getTenantId() {
    Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
    if (tenantId.isEmpty()) {
      throw new IllegalArgumentException("Tenant Id is missing in the request.");
    }
    return tenantId.get();
  }

  // TODO : get the userId from the context
  private String getUserId() {
    return "";
  }
}
