# Hypertrace Service
###### org.hypertrace.service

[![CircleCI](https://circleci.com/gh/hypertrace/hypertrace-service.svg?style=svg)](https://circleci.com/gh/hypertrace/hypertrace-service)

Hypertrace service combines [hypertrace-ui](https://github.com/hypertrace/hypertrace-ui), [hypertrace-graphql](https://github.com/hypertrace/hypertrace-graphql), [gateway-service](https://github.com/hypertrace/gateway-service), [attribute-service](https://github.com/hypertrace/attribute-service), [query-service](https://github.com/hypertrace/query-service), [entity-service](https://github.com/hypertrace/entity-service) into single service. This is used in Hypertrace standalone deployment to make the deployment compact and use less resource.

## How this works?

| ![space-1.jpg](https://hypertrace-docs.s3.amazonaws.com/federated-service-arch.png) | 
|:--:| 
| *Hypertrace Query Architecture* |

In Hypertrace service, 
- GraphQL service is being used by UI and attribute-service fetches all relevant attributes to the scope of what is being shown.
- The gateway-service provides a single access point that routes queries to corresponding downstream service based on the source of attributes and then does appropriate type conversion of data returned by upstream services. 
- The query-service interfaces with Apache Pinot Data Store while entity-service provides CRUD operations for differently identified entities of observed applications.

## Building locally
The `Hypertrace service` uses gradlew to compile/install/distribute. Gradle wrapper is already part of the source code. To build `Hypertrace Service`, run:

```
./gradlew clean build dockerBuildImages
```

## Docker Image Source:
- [DockerHub > Hypertrace service](https://hub.docker.com/r/hypertrace/hypertrace-service)
