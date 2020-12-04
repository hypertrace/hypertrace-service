package org.hypertrace.config.service.store;

import com.google.protobuf.Value;
import com.typesafe.config.Config;
import org.hypertrace.config.service.Utils;
import org.hypertrace.config.service.v1.GetConfigResponse;
import org.hypertrace.core.documentstore.Collection;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.DatastoreProvider;
import org.hypertrace.core.documentstore.Document;
import org.hypertrace.core.documentstore.Filter;
import org.hypertrace.core.documentstore.Key;
import org.hypertrace.core.documentstore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import static org.hypertrace.config.service.store.ConfigDocument.CONTEXT_FIELD_NAME;
import static org.hypertrace.config.service.store.ConfigDocument.RESOURCE_FIELD_NAME;
import static org.hypertrace.config.service.store.ConfigDocument.RESOURCE_NAMESPACE_FIELD_NAME;
import static org.hypertrace.config.service.store.ConfigDocument.TENANT_ID_FIELD_NAME;
import static org.hypertrace.config.service.store.ConfigDocument.VERSION_FIELD_NAME;

public class DocumentConfigStore implements ConfigStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentConfigStore.class);
    private static final String DOC_STORE_CONFIG_KEY = "document.store";
    private static final String DATA_STORE_TYPE = "dataStoreType";

    private static final String CONFIGURATIONS_COLLECTION = "configurations";

    private Collection collection;

    @Override
    public void init(Config config) {
        Datastore store = initDataStore(config);
        this.collection = getOrCreateCollection(store);
    }

    private Datastore initDataStore(Config config) {
        Config docStoreConfig = config.getConfig(DOC_STORE_CONFIG_KEY);
        String dataStoreType = docStoreConfig.getString(DATA_STORE_TYPE);
        Config dataStoreConfig = docStoreConfig.getConfig(dataStoreType);
        return DatastoreProvider.getDatastore(dataStoreType, dataStoreConfig);
    }
    
    private Collection getOrCreateCollection(Datastore datastore) {
        if (!datastore.listCollections().contains(CONFIGURATIONS_COLLECTION)) {
            if (!datastore.createCollection(CONFIGURATIONS_COLLECTION, Collections.emptyMap())) {
                throw new RuntimeException("Failed to create collection:" + CONFIGURATIONS_COLLECTION + " in document store");
            }
        }
        return datastore.getCollection(CONFIGURATIONS_COLLECTION);
    }

    @Override
    public long writeConfig(ConfigResource configResource, String userId, Value config) throws IOException {
        // TODO: need to synchronize this method
        long configVersion = getLatestVersion(configResource) + 1;
        Key key = new ConfigDocumentKey(configResource, configVersion);
        Document document = new ConfigDocument(configResource.getResourceName(), configResource.getResourceNamespace(),
                configResource.getTenantId(), Utils.optionalContextToString(configResource.getContext()),
                configVersion, userId, config);
        collection.upsert(key, document);   // TODO: Retry if this returns false?
        return configVersion;
    }

    @Override
    public GetConfigResponse getConfig(ConfigResource configResource, Optional<Long> configVersion) {
        long version = configVersion.orElse(getLatestVersion(configResource));
        Filter filter = getConfigResourceFilter(configResource).and(Filter.eq(VERSION_FIELD_NAME, version));
        Query query = new Query();
        query.setFilter(filter);
        Iterator<Document> documentIterator = collection.search(query);
        
        GetConfigResponse.Builder responseBuilder = GetConfigResponse.newBuilder();
        if (documentIterator.hasNext()) {
            String documentString = documentIterator.next().toJson();
            try {
                ConfigDocument configDocument = ConfigDocument.fromJson(documentString);
                responseBuilder.setConfig(configDocument.getConfig());
            } catch (IOException e) {
                LOGGER.error("Exception while parsing document string : %s", documentString, e);
            }
        }
        return responseBuilder.build();
    }

    private long getLatestVersion(ConfigResource configResource) {
        Query query = new Query();
        query.setFilter(getConfigResourceFilter(configResource));
        return collection.total(query);
    }

    private Filter getConfigResourceFilter(ConfigResource configResource) {
        return Filter.eq(RESOURCE_FIELD_NAME, configResource.getResourceName())
                .and(Filter.eq(RESOURCE_NAMESPACE_FIELD_NAME, configResource.getResourceNamespace()))
                .and(Filter.eq(TENANT_ID_FIELD_NAME, configResource.getTenantId()))
                .and(Filter.eq(CONTEXT_FIELD_NAME, Utils.optionalContextToString(configResource.getContext())));
    }
}
