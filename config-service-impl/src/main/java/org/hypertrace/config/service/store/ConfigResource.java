package org.hypertrace.config.service.store;

import lombok.Data;

import java.util.Optional;

@Data
public class ConfigResource {
    private final String resourceName;
    private final String resourceNamespace;
    private final String tenantId;
    private final Optional<String> context;
}
