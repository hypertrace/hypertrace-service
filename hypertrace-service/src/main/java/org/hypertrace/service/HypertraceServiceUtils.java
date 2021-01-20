package org.hypertrace.service;

import com.typesafe.config.Config;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigUtils;

public class HypertraceServiceUtils {

  public static final String DEFAULT_CLUSTER_NAME = "default-cluster";

  public static final String CLUSTER_NAME = "cluster.name";
  public static final String POD_NAME = "pod.name";
  public static final String CONTAINER_NAME = "container.name";

  public static Config getServiceConfig(String serviceName, ConfigClient configClient) {
    String clusterName = ConfigUtils.getEnvironmentProperty(CLUSTER_NAME);
    if (clusterName == null) {
      clusterName = DEFAULT_CLUSTER_NAME;
    }

    return configClient.getConfig(
        serviceName,
        clusterName,
        ConfigUtils.getEnvironmentProperty(POD_NAME),
        ConfigUtils.getEnvironmentProperty(CONTAINER_NAME));
  }
}
