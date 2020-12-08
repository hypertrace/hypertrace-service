package org.hypertrace.config.service.store;

import com.google.protobuf.Value;
import com.typesafe.config.Config;
import org.hypertrace.config.service.ConfigResource;

import java.io.IOException;

/**
 * Abstraction for the backend which stores and serves the configuration data for multiple
 * resources.
 */
public interface ConfigStore {

  /**
   * Initialize the config store
   *
   * @param config
   */
  void init(Config config);

  /**
   * Write the config value associated with the specified config resource to the store.
   *
   * @param configResource
   * @param userId
   * @param config
   * @return the version allocated to the newly inserted configuration
   */
  long writeConfig(ConfigResource configResource, String userId, Value config)
      throws IOException;

  /**
   * Get the config with the latest version for the specified resource.
   *
   * @param configResource
   * @return
   */
  Value getConfig(ConfigResource configResource) throws IOException;
}
