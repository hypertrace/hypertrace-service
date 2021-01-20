package org.hypertrace.service;

import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.service.all.services.HypertraceAllServices;
import org.hypertrace.service.dataquery.services.HypertraceDataQueryServices;

public class HypertraceServiceEntry extends PlatformService {

  private static final String SERVICE_NAME_CONFIG = "service.name";
  private static final String SERVICE_ROLE = "service.role";
  private static final String ALL_SERVICES_ROLE = "all-service";
  private static final String DATA_QUERY_SERVICES_ROLE = "data-query-services";
  
  private final ConfigClient configClient;
  
  private String serviceName;
  private HypertraceService hypertraceService;

  public HypertraceServiceEntry(ConfigClient configClient) {
    super(configClient);
    this.configClient = configClient;
  }


  @Override
  protected void doInit() {
    serviceName = getAppConfig().getString(SERVICE_NAME_CONFIG);
    if (getAppConfig().hasPath(SERVICE_ROLE) 
        && getAppConfig().getString(SERVICE_ROLE).equals(DATA_QUERY_SERVICES_ROLE)) {
      hypertraceService = new HypertraceDataQueryServices(configClient, getLifecycle());
    } else {
      // fallback to all-service
      hypertraceService = new HypertraceAllServices(configClient, getLifecycle());
    }
  }

  @Override
  protected void doStart() {
    hypertraceService.start();
  }

  @Override
  protected void doStop() {
    hypertraceService.stop();
  }

  @Override
  public boolean healthCheck() {
    return true;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }
}
