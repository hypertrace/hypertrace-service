# Hypertrace Federated Service
###### org.hypertrace.federated.service

[![CircleCI](https://circleci.com/gh/hypertrace/hypertrace-federated-service.svg?style=svg)](https://circleci.com/gh/hypertrace/hypertrace-federated-service)

Hypertrace federated service combines [Hypertrace-UI](https://github.com/hypertrace/hypertrace-ui), [Hypertrace-GraphQL](https://github.com/hypertrace/hypertrace-graphql), [Gateway service](https://github.com/hypertrace/gateway-service), [Attribute service](https://github.com/hypertrace/attribute-service), [Query service](https://github.com/hypertrace/query-service), [Entity service](https://github.com/hypertrace/entity-service) into single service. This is used in hypertrace standalone deployment to make the deployment compact and use less resource.

## How Federated service works?

| ![space-1.jpg](https://hypertrace-docs.s3.amazonaws.com/federated-service.png) | 
|:--:| 
| *Hypertrace Federated Service* |

In Hypertrace Federated service, 
- GraphQL service is being used by UI and Attribute service fetches all relevant attributes to the scope of what is being shown.
- Gateway service provides single access point which routes queries to corresponding downstream service based on the source of attributes and Attribute service then does appropriate type conversion of data returned by downstream services. 
- The Query Service interfaces with Apache Pinot Data Store while entity-service provides CRUD operations for differently identified entities of observed applications.
