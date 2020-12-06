package org.hypertrace.config.service.store;

import com.google.protobuf.Value;
import com.typesafe.config.Config;
import org.hypertrace.config.service.ConfigResource;
import org.hypertrace.config.service.v1.GetConfigResponse;

import java.io.IOException;
import java.util.Optional;
import org.hypertrace.config.service.v1.UpsertConfigResponse;

public interface ConfigStore {

    /**
     * Initialize the config store
     * @param config
     */
    void init(Config config);

    /**
     * Write the config value associated with the specified config resource to the store.
     * @param configResource
     * @param userId
     * @param config
     * @return
     */
    UpsertConfigResponse writeConfig(ConfigResource configResource, String userId, Value config)
        throws IOException;

    /**
     * Get the config for the specified resource with the specified version(optional).
     * If configVersion is empty, get the config for the latest version.
     * @param configResource
     * @param configVersion
     * @return
     */
    GetConfigResponse getConfig(ConfigResource configResource, Optional<Long> configVersion)
        throws IOException;
}
