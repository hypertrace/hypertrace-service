package org.hypertrace.core.bootstrapper;

import com.typesafe.config.Config;
import org.hypertrace.core.attribute.service.client.AttributeServiceClient;
import org.hypertrace.core.attribute.service.client.config.AttributeServiceClientConfig;
import org.hypertrace.entity.service.client.config.EntityServiceClientConfig;
import org.hypertrace.entity.type.service.client.EntityTypeServiceClient;

/** Config passed to every command. Encapsulates all the clients used by the bootstrapper */
public class BootstrapContext {
  private final AttributeServiceClient attributesServiceClient;
  private final EntityTypeServiceClient entityTypeServiceClient;

  private BootstrapContext(
      AttributeServiceClientConfig attributeServiceClientConfig,
      EntityServiceClientConfig entityServiceClientConfig) {
    this.attributesServiceClient = new AttributeServiceClient(attributeServiceClientConfig);
    this.entityTypeServiceClient = new EntityTypeServiceClient(entityServiceClientConfig);
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
}
