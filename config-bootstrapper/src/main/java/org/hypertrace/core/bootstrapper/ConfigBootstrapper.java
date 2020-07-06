package org.hypertrace.core.bootstrapper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.hypertrace.core.bootstrapper.dao.ConfigBootstrapStatusDao;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.DatastoreProvider;
import org.hypertrace.core.documentstore.DocumentStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigBootstrapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigBootstrapper.class);

  public static void main(String[] args) {
    updateRuntime();
    bootstrapper(args);
  }

  private static void bootstrapper(String[] args) {
    BootstrapArgs bootstrapArgs = BootstrapArgs.from(args);
    Config config = ConfigFactory.parseFile(new File(bootstrapArgs.getConfigFile()));
    String dataStoreType = config.getString(DocumentStoreConfig.DATASTORE_TYPE_CONFIG_KEY);
    Datastore datastore =
        DatastoreProvider.getDatastore(dataStoreType, config.getConfig(dataStoreType));
    new BootstrapRunner(new ConfigBootstrapStatusDao(datastore)).execute(bootstrapArgs);
  }

  private static void updateRuntime() {
    Runtime.getRuntime().addShutdownHook(new Thread(ConfigBootstrapper::finalizeBootstrapper));
  }

  private static void finalizeBootstrapper() {
    String istioPilotQuitEndpoint = "http://127.0.0.1:15020/quitquitquit";
    HttpClient httpclient = HttpClients.createDefault();
    HttpPost httppost = new HttpPost(istioPilotQuitEndpoint);
    try {
      httpclient.execute(httppost);
      LOGGER.info("Request to pilot succeeded");
    } catch (IOException e) {
      LOGGER.error("Error while calling quitquitquit", e);
    }
  }
}
