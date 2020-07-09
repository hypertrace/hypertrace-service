rootProject.name = "hypertrace-federated-service"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://dl.bintray.com/hypertrace/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.1.1"
}

includeBuild("./attribute-service")
includeBuild("./entity-service")
includeBuild("./gateway-service")
includeBuild("./query-service")

include(":hypertrace-federated-service")
