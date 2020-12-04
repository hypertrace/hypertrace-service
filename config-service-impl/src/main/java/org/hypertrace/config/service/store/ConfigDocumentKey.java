package org.hypertrace.config.service.store;

import org.hypertrace.config.service.Utils;
import org.hypertrace.core.documentstore.Key;

/**
 * Key for the Config Document
 */
public class ConfigDocumentKey implements Key {

    private static final String SEPARATOR = ":";

    private final ConfigResource configResource;
    private final long configVersion;

    public ConfigDocumentKey(ConfigResource configResource, long configVersion) {
        this.configResource = configResource;
        this.configVersion = configVersion;
    }

    @Override
    public String toString() {
        return String.join(SEPARATOR, configResource.getResourceName(), configResource.getResourceNamespace(),
                configResource.getTenantId(), Utils.optionalContextToString(configResource.getContext()),
                String.valueOf(configVersion));
    }
}
