# attribute-service

Service to provide CRUD operations for the attributes in Hypertrace.

### What are Attributes?
An attribute represents a piece of data that's present in Hypertrace
that's available for querying. Attribute metadata gives the type of the
data, kind of aggregations allowed on it, services which serve that attribute, etc.
See `attribute-service-api/src/main/proto/org/hypertrace/core/attribute/service/v1/attribute_metadata.proto`
for the structure of an attribute.

### How are attributes created?
An initial list of attributes needed by Hypertrace are seeded from `helm/configs` but they
can also be dynamically registered and queried using the APIs of AttributeService.
