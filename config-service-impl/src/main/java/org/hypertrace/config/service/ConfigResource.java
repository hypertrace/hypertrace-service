package org.hypertrace.config.service;

import lombok.Value;

@Value
public class ConfigResource {

  String resourceName;
  String resourceNamespace;
  String tenantId;
  String context;
}
