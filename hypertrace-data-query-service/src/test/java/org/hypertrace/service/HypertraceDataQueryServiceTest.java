package org.hypertrace.service;

import com.typesafe.config.Config;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigClientFactory;
import org.hypertrace.core.serviceframework.config.ConfigUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HypertraceDataQueryServiceTest {

  @Test
  public void testServiceConfig() {
    System.setProperty("service.name", "hypertrace-data-query-service");
    System.setProperty("cluster.name", "default-cluster");
    ConfigClient configClient = ConfigClientFactory.getClient();
    Config appConfig = configClient.getConfig();
    Assertions.assertTrue(appConfig.hasPath("service.port"));
    Assertions.assertNotNull(configClient.getConfig(
        "query-service",
        ConfigUtils.getEnvironmentProperty("cluster.name"), null, null));
    Assertions.assertNotNull(configClient.getConfig(
        "gateway-service",
        ConfigUtils.getEnvironmentProperty("cluster.name"), null, null));
  }
}
