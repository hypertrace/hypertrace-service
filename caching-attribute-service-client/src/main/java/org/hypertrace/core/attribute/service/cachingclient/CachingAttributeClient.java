package org.hypertrace.core.attribute.service.cachingclient;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.grpcutils.client.RequestContextClientCallCredsProviderFactory;

public interface CachingAttributeClient {

  Single<AttributeMetadata> get(String scope, String key);

  Single<AttributeMetadata> get(String attributeId);

  Single<List<AttributeMetadata>> getAll();

  static Builder builder() {
    return new Builder();
  }

  final class Builder {
    private int maxCacheContexts = 100;
    private Duration cacheExpiration = Duration.of(15, ChronoUnit.MINUTES);
    private Channel channel;
    private CallCredentials callCredentials =
        RequestContextClientCallCredsProviderFactory.getClientCallCredsProvider().get();
    private AttributeMetadataFilter attributeFilter = AttributeMetadataFilter.getDefaultInstance();

    public CachingAttributeClient build() {
      Objects.requireNonNull(
          this.channel,
          "Channel must be defined with either CachingAttributeClientBuilder.withNewChannel or CachingAttributeClientBuilder.withExistingChannel");

      return new DefaultCachingAttributeClient(
          this.channel,
          this.callCredentials,
          this.maxCacheContexts,
          this.cacheExpiration,
          this.attributeFilter);
    }

    /**
     * Use a new channel for attribute queries created with the provided host and port. Replaces any
     * channel set.
     *
     * @param host
     * @param port
     * @return
     */
    public Builder withNewChannel(String host, int port) {
      this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
      return this;
    }

    /**
     * Use the provided channel to query attributes. Replaces any channel set.
     *
     * @param channel
     * @return
     */
    public Builder withExistingChannel(@Nonnull Channel channel) {
      this.channel = channel;
      return this;
    }

    /**
     * Limits the number unique contexts (i.e. tenants) to maintain in the cache at any one time.
     * Defaults to 100.
     *
     * @param maxCacheContexts
     * @return
     */
    public Builder withMaximumCacheContexts(int maxCacheContexts) {
      this.maxCacheContexts = maxCacheContexts;
      return this;
    }

    /**
     * Expires a cached context the provided duration after write. Defaults to 15 minutes.
     *
     * @param cacheExpiration
     * @return
     */
    public Builder withCacheExpiration(@Nonnull Duration cacheExpiration) {
      this.cacheExpiration = cacheExpiration;
      return this;
    }

    /**
     * Use the provided call credentials for propagating context. Defaults to the value provided by
     * {@link RequestContextClientCallCredsProviderFactory}
     *
     * @param callCredentials
     * @return
     */
    public Builder withCallCredentials(@Nonnull CallCredentials callCredentials) {
      this.callCredentials = callCredentials;
      return this;
    }

    /**
     * Filter all attributes fetched and cached with the provided filter. Useful for limiting a
     * client to use a particular scope, for example. Defaults to empty.
     *
     * @param attributeFilter
     * @return
     */
    public Builder withAttributeFilter(@Nonnull AttributeMetadataFilter attributeFilter) {
      this.attributeFilter = attributeFilter;
      return this;
    }
  }
}
