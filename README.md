# Hypertrace Federated Service
###### org.hypertrace.federated.service

[![CircleCI](https://circleci.com/gh/hypertrace/hypertrace-federated-service.svg?style=svg)](https://circleci.com/gh/hypertrace/hypertrace-federated-service)

Combines the following hypertrace services into a single service. This is used in hypertrace standalone deployment to make the deployment compact and use less resource.
- **Attribute service:** Service to provide CRUD operations for the attributes in Hypertrace.
- **Entity service:** Service that provides CRUD operations for differently identified entities of observed applications.
- **Gateway service:** An entry service that acts as a single access point for querying data from other services like entity-service, query-service, etc
- **Query service:** Query service exposes APIs to query data from the underlying stores. 
 
