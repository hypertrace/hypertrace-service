rootProject.name = "hypertrace-service"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://dl.bintray.com/hypertrace/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.1.2"
}

includeBuild("./attribute-service")
includeBuild("./entity-service")
includeBuild("./gateway-service")
includeBuild("./hypertrace-graphql")
includeBuild("./hypertrace-graphql/hypertrace-core-graphql")
includeBuild("./config-bootstrapper")
includeBuild("./config-service")

// query-service
include("query-service:query-service")
include("query-service:query-service-api")
include("query-service:query-service-client")
include("query-service:query-service-impl")

include(":hypertrace-service")
