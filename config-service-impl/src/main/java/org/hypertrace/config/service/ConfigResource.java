package org.hypertrace.config.service;

import lombok.Data;

@Data
public class ConfigResource {

  private final String resourceName;
  private final String resourceNamespace;
  private final String tenantId;
  private final String context;
}
