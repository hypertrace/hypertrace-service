package org.hypertrace.core.bootstrapper;

import com.typesafe.config.Config;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.attribute.service.client.AttributeServiceClient;
import org.hypertrace.core.attribute.service.client.config.AttributeServiceClientConfig;
import org.hypertrace.entity.service.client.config.EntityServiceClientConfig;
import org.hypertrace.entity.type.service.client.EntityTypeServiceClient;

/**
 * Config passed to every command. Encapsulates all the clients used by the bootstrapper
 */
public class BootstrapContext {

  private final AttributeServiceClient attributesServiceClient;
  private final EntityTypeServiceClient entityTypeServiceClient;

  private final ManagedChannel attributeServiceChannel;
  private final ManagedChannel entityServiceChannel;
  private final List<ManagedChannel> managedChannels;

  private BootstrapContext(
      AttributeServiceClientConfig attributeServiceClientConfig,
      EntityServiceClientConfig entityServiceClientConfig) {

    this.attributeServiceChannel = ManagedChannelBuilder.forAddress(
        attributeServiceClientConfig.getHost(), attributeServiceClientConfig.getPort())
        .usePlaintext().build();
    this.attributesServiceClient = new AttributeServiceClient(attributeServiceChannel);

    this.entityServiceChannel = ManagedChannelBuilder.forAddress(
        entityServiceClientConfig.getHost(), entityServiceClientConfig.getPort())
        .usePlaintext().build();
    this.entityTypeServiceClient = new EntityTypeServiceClient(entityServiceChannel);

    managedChannels = List.of(attributeServiceChannel, entityServiceChannel);
  }

  public static BootstrapContext buildFrom(Config config) {
    return new BootstrapContext(
        AttributeServiceClientConfig.from(config), EntityServiceClientConfig.from(config));
  }

  public AttributeServiceClient getAttributesServiceClient() {
    return attributesServiceClient;
  }

  public EntityTypeServiceClient getEntityTypeServiceClient() {
    return entityTypeServiceClient;
  }

  public void close() {
    managedChannels.forEach((managedChannel) -> {
      if(!managedChannel.isShutdown()) {
        managedChannel.shutdown();
      }
    });
  }
}
