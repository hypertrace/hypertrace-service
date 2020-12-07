package org.hypertrace.config.service.store;

import com.google.protobuf.Value;
import com.typesafe.config.Config;
import org.hypertrace.config.service.ConfigResource;

import java.io.IOException;

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
   * @return
   */
  long writeConfig(ConfigResource configResource, String userId, Value config)
      throws IOException;

  /**
   * Get the config for the specified resource.
   *
   * @param configResource
   * @return
   */
  Value getConfig(ConfigResource configResource) throws IOException;
}
