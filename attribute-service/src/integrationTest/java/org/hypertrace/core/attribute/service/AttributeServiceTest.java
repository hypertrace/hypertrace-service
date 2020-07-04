package org.hypertrace.core.attribute.service;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hypertrace.core.attribute.service.client.AttributeServiceClient;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeSource;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadataDeleteRequest;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadataUpdateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeType;
import org.hypertrace.core.serviceframework.IntegrationTestServerUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration test for AttributeService */
public class AttributeServiceTest {
  private static final String SPAN_ID_ATTR = "EVENT.id";
  private static final String SPAN_NAME_ATTR = "EVENT.name";
  private static final String TRACE_METRIC_ATTR = "Trace.metrics.duration_millis";

  private static final String TEST_TENANT_ID = "test-tenant-id";

  private final AttributeMetadata spanNameAttr =
      AttributeMetadata.newBuilder()
          .setFqn(SPAN_NAME_ATTR)
          .setScope(AttributeScope.EVENT)
          .setKey("name")
          .setType(AttributeType.ATTRIBUTE)
          .setValueKind(AttributeKind.TYPE_STRING)
          .setDisplayName("Span Name")
          .addSources(AttributeSource.QS)
          .build();
  private final AttributeMetadata spanIdAttr =
      AttributeMetadata.newBuilder()
          .setFqn(SPAN_ID_ATTR)
          .setScope(AttributeScope.EVENT)
          .setKey("id")
          .setType(AttributeType.ATTRIBUTE)
          .setValueKind(AttributeKind.TYPE_STRING)
          .setDisplayName("Span Id")
          .addSources(AttributeSource.QS)
          .build();

  private final AttributeMetadata traceDurationMillis =
      AttributeMetadata.newBuilder()
          .setFqn(TRACE_METRIC_ATTR)
          .setScope(AttributeScope.TRACE)
          .setKey("duration")
          .setType(AttributeType.METRIC)
          .setValueKind(AttributeKind.TYPE_INT64)
          .setDisplayName("Duration")
          .addSources(AttributeSource.QS)
          .build();

  private final Map<String, String> requestHeaders = Map.of("x-tenant-id", TEST_TENANT_ID);

  private static AttributeServiceClient client;

  @BeforeAll
  public static void setup() {
    System.out.println("Testing the Attribute E2E Test");
    IntegrationTestServerUtil.startServices(new String[] {"attribute-service"});
    Channel channel = ManagedChannelBuilder.forAddress("localhost", 9012).usePlaintext().build();
    client = new AttributeServiceClient(channel);
  }

  @AfterAll
  public static void teardown() {
    IntegrationTestServerUtil.shutdownServices();
  }

  @BeforeEach
  public void setupMethod() {
    client.delete(requestHeaders, AttributeMetadataFilter.newBuilder().build());
  }

  @Test
  public void testCreateInvalidUseHeaders() {
    Assertions.assertThrows(RuntimeException.class, () -> testCreateInvalid(true));
  }

  @Test
  public void testCreateInvalidUseTenantId() {
    Assertions.assertThrows(RuntimeException.class, () -> testCreateInvalid(false));
  }

  private void testCreateInvalid(boolean useRequestHeaders) {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAttributes(
                AttributeMetadata.newBuilder()
                    .setFqn(SPAN_NAME_ATTR)
                    .setKey("name")
                    .setType(AttributeType.ATTRIBUTE)
                    .setValueKind(AttributeKind.TYPE_STRING)
                    .setDisplayName("Span Name")
                    .addSources(AttributeSource.QS)
                    .build())
            .build();
    if (useRequestHeaders) {
      client.create(requestHeaders, attributeCreateRequest);
    } else {
      client.create(TEST_TENANT_ID, attributeCreateRequest);
    }
  }

  @Test
  public void testCreateCallWithHeadersWithTenantIdHeader() {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAttributes(spanNameAttr)
            .addAttributes(spanIdAttr)
            .build();
    client.create(requestHeaders, attributeCreateRequest);

    Iterator<AttributeMetadata> attributeMetadataIterator =
        client.findAttributes(requestHeaders, AttributeMetadataFilter.newBuilder().build());
    List<String> attributeMetadataList =
        StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
            .map(AttributeMetadata::getFqn)
            .collect(Collectors.toList());
    Assertions.assertTrue(
        attributeMetadataList.containsAll(
            Arrays.asList(spanNameAttr.getFqn(), spanIdAttr.getFqn())));
  }

  @Test
  public void testCreateCallWithHeadersWithNoTenantIdHeader_shouldThrowException() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          Map<String, String> headers = Map.of("a1", "v1");
          AttributeCreateRequest attributeCreateRequest =
              AttributeCreateRequest.newBuilder()
                  .addAttributes(spanNameAttr)
                  .addAttributes(spanIdAttr)
                  .build();
          client.create(headers, attributeCreateRequest);
        });
  }

  @Test
  public void testUpdateAndDeleteSourceMetadataForAttributeUseHeaders() {
    testUpdateAndDeleteSourceMetadataForAttribute(true);
  }

  @Test
  public void testUpdateAndDeleteSourceMetadataForAttributeUseTenantId() {
    testUpdateAndDeleteSourceMetadataForAttribute(false);
  }

  private void testUpdateAndDeleteSourceMetadataForAttribute(boolean useRequestHeaders) {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAttributes(spanNameAttr)
            .addAttributes(spanIdAttr)
            .build();
    if (useRequestHeaders) {
      client.create(requestHeaders, attributeCreateRequest);
    } else {
      client.create(TEST_TENANT_ID, attributeCreateRequest);
    }

    AttributeSourceMetadataUpdateRequest request =
        AttributeSourceMetadataUpdateRequest.newBuilder()
            .setFqn(SPAN_NAME_ATTR)
            .setSource(AttributeSource.EDS)
            .putAllSourceMetadata(Map.of(SPAN_NAME_ATTR, "attributes.SPAN_NAME"))
            .build();

    Iterator<AttributeMetadata> attributeMetadataIterator;
    if (useRequestHeaders) {
      client.updateSourceMetadata(requestHeaders, request);
      attributeMetadataIterator =
          client.findAttributes(
              requestHeaders, AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR).build());
    } else {
      client.updateSourceMetadata(TEST_TENANT_ID, request);
      attributeMetadataIterator =
          client.findAttributes(
              TEST_TENANT_ID, AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR).build());
    }

    List<AttributeMetadata> attributeMetadataList =
        StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
            .collect(Collectors.toList());
    Assertions.assertEquals(1, attributeMetadataList.size());
    Map<String, AttributeSourceMetadata> attributeSourceMetadataMap =
        attributeMetadataList.get(0).getMetadataMap();
    Assertions.assertFalse(attributeSourceMetadataMap.isEmpty());
    Assertions.assertEquals(
        request.getSourceMetadataMap(),
        attributeSourceMetadataMap.get(AttributeSource.EDS.name()).getSourceMetadataMap());

    if (useRequestHeaders) {
      client.deleteSourceMetadata(
          requestHeaders,
          AttributeSourceMetadataDeleteRequest.newBuilder()
              .setFqn(SPAN_NAME_ATTR)
              .setSource(AttributeSource.EDS)
              .build());
      attributeMetadataIterator =
          client.findAttributes(
              requestHeaders, AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR).build());
    } else {
      client.deleteSourceMetadata(
          TEST_TENANT_ID,
          AttributeSourceMetadataDeleteRequest.newBuilder()
              .setFqn(SPAN_NAME_ATTR)
              .setSource(AttributeSource.EDS)
              .build());
      attributeMetadataIterator =
          client.findAttributes(
              TEST_TENANT_ID, AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR).build());
    }

    attributeMetadataList =
        StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
            .collect(Collectors.toList());
    Assertions.assertEquals(1, attributeMetadataList.size());
    attributeSourceMetadataMap = attributeMetadataList.get(0).getMetadataMap();
    Assertions.assertTrue(attributeSourceMetadataMap.isEmpty());
  }

  @Test
  public void testFindByFilterUseHeaders() {
    testFindByFilter(true);
  }

  @Test
  public void testFindByFilterUseTenantId() {
    testFindByFilter(false);
  }

  private void testFindByFilter(boolean useRequestHeaders) {
    createSampleAttributes(useRequestHeaders, spanNameAttr, spanIdAttr, traceDurationMillis);

    {
      Iterator<AttributeMetadata> attributeMetadataIterator =
          useRequestHeaders
              ? client.findAttributes(requestHeaders, AttributeMetadataFilter.newBuilder().build())
              : client.findAttributes(TEST_TENANT_ID, AttributeMetadataFilter.newBuilder().build());
      List<String> attributeMetadataList =
          StreamSupport.stream(
                  Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
              .map(AttributeMetadata::getFqn)
              .collect(Collectors.toList());
      Assertions.assertTrue(
          attributeMetadataList.containsAll(
              Arrays.asList(
                  spanNameAttr.getFqn(), spanIdAttr.getFqn(), traceDurationMillis.getFqn())));
    }

    {
      Iterator<AttributeMetadata> attributeMetadataIterator =
          useRequestHeaders
              ? client.findAttributes(
                  requestHeaders,
                  AttributeMetadataFilter.newBuilder().addFqn(spanNameAttr.getFqn()).build())
              : client.findAttributes(
                  TEST_TENANT_ID,
                  AttributeMetadataFilter.newBuilder().addFqn(spanNameAttr.getFqn()).build());
      List<String> attributeMetadataList =
          StreamSupport.stream(
                  Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
              .map(AttributeMetadata::getFqn)
              .collect(Collectors.toList());
      Assertions.assertEquals(1, attributeMetadataList.size());
      Assertions.assertEquals(spanNameAttr.getFqn(), attributeMetadataList.get(0));
    }

    {
      Iterator<AttributeMetadata> attributeMetadataIterator =
          useRequestHeaders
              ? client.findAttributes(
                  requestHeaders,
                  AttributeMetadataFilter.newBuilder()
                      .addScope(spanNameAttr.getScope())
                      .addKey(spanNameAttr.getKey())
                      .build())
              : client.findAttributes(
                  TEST_TENANT_ID,
                  AttributeMetadataFilter.newBuilder()
                      .addScope(spanNameAttr.getScope())
                      .addKey(spanNameAttr.getKey())
                      .build());
      List<String> attributeMetadataList =
          StreamSupport.stream(
                  Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
              .map(AttributeMetadata::getFqn)
              .collect(Collectors.toList());
      Assertions.assertEquals(1, attributeMetadataList.size());
      Assertions.assertEquals(spanNameAttr.getFqn(), attributeMetadataList.get(0));
    }
  }

  @Test
  public void testDeleteByFilterUseHeaders() {
    testDeleteByFilter(true);
  }

  @Test
  public void testDeleteByFilterUseTenantId() {
    testDeleteByFilter(false);
  }

  private void testDeleteByFilter(boolean useRequestHeaders) {
    createSampleAttributes(useRequestHeaders, spanNameAttr, spanIdAttr, traceDurationMillis);
    {
      Iterator<AttributeMetadata> attributeMetadataIterator;
      if (useRequestHeaders) {
        client.delete(requestHeaders, AttributeMetadataFilter.newBuilder().build());
        attributeMetadataIterator =
            client.findAttributes(requestHeaders, AttributeMetadataFilter.newBuilder().build());
      } else {
        client.delete(TEST_TENANT_ID, AttributeMetadataFilter.newBuilder().build());
        attributeMetadataIterator =
            client.findAttributes(TEST_TENANT_ID, AttributeMetadataFilter.newBuilder().build());
      }
      List<AttributeMetadata> attributeMetadataList =
          StreamSupport.stream(
                  Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
              .collect(Collectors.toList());
      Assertions.assertTrue(attributeMetadataList.isEmpty());
    }

    createSampleAttributes(useRequestHeaders, spanNameAttr, spanIdAttr, traceDurationMillis);
    {
      Iterator<AttributeMetadata> attributeMetadataIterator;
      if (useRequestHeaders) {
        client.delete(
            requestHeaders,
            AttributeMetadataFilter.newBuilder()
                .addScope(spanNameAttr.getScope())
                .addKey(spanNameAttr.getKey())
                .build());
        attributeMetadataIterator =
            client.findAttributes(requestHeaders, AttributeMetadataFilter.newBuilder().build());
      } else {
        client.delete(
            TEST_TENANT_ID,
            AttributeMetadataFilter.newBuilder()
                .addScope(spanNameAttr.getScope())
                .addKey(spanNameAttr.getKey())
                .build());
        attributeMetadataIterator =
            client.findAttributes(TEST_TENANT_ID, AttributeMetadataFilter.newBuilder().build());
      }
      List<String> attributeMetadataList =
          StreamSupport.stream(
                  Spliterators.spliteratorUnknownSize(attributeMetadataIterator, 0), false)
              .map(AttributeMetadata::getFqn)
              .collect(Collectors.toList());
      Assertions.assertEquals(2, attributeMetadataList.size());
      Assertions.assertTrue(
          attributeMetadataList.containsAll(
              Arrays.asList(spanIdAttr.getFqn(), traceDurationMillis.getFqn())));
    }
  }

  private void createSampleAttributes(
      boolean useRequestHeaders, AttributeMetadata... attributeMetadata) {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAllAttributes(Arrays.asList(attributeMetadata))
            .build();
    if (useRequestHeaders) {
      client.create(requestHeaders, attributeCreateRequest);
    } else {
      client.create(TEST_TENANT_ID, attributeCreateRequest);
    }
  }
}
