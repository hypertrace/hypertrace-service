plugins {
  java
  application
  jacoco
  id("org.hypertrace.docker-java-application-plugin")
  id("org.hypertrace.docker-publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  implementation("org.hypertrace.core.attribute.service:attribute-service")
  implementation("org.hypertrace.core.attribute.service:attribute-service-impl")
  implementation("org.hypertrace.entity.service:entity-service")
  implementation("org.hypertrace.entity.service:entity-service-impl")
  implementation("org.hypertrace.core.query.service:query-service")
  implementation("org.hypertrace.core.query.service:query-service-impl")
  implementation("org.hypertrace.gateway.service:gateway-service")
  implementation("org.hypertrace.gateway.service:gateway-service-impl")

  implementation("org.hypertrace.core.serviceframework:platform-service-framework:0.1.4")
  implementation("org.hypertrace.core.grpcutils:grpc-server-utils:0.1.3")
  implementation("org.hypertrace.core.documentstore:document-store:0.1.1")

  // Logging
  implementation("org.slf4j:slf4j-api:1.7.30")
  runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")

  // GRPC
  runtimeOnly("io.grpc:grpc-netty:1.30.2")

  // Config
  implementation("com.typesafe:config:1.4.0")
}

application {
  mainClassName = "org.hypertrace.core.serviceframework.PlatformServiceLauncher"
}

// Config for gw run to be able to run this locally. Just execute gw run here on Intellij or on the console.
tasks.run<JavaExec> {
  jvmArgs = listOf("-Dbootstrap.config.uri=file:${project.buildDir}/resources/main/configs", "-Dservice.name=${project.name}")
}

tasks.processResources {
  dependsOn("copyServiceConfigs");
  dependsOn("copyOverrideServiceConfigs")
}

tasks.register<Copy>("copyServiceConfigs") {
  with(
      createCopySpec("attribute-service", "attribute-service"),
      createCopySpec("entity-service", "entity-service"),
      createCopySpec("gateway-service", "gateway-service"),
      createCopySpec("query-service", "query-service")
  ).into("./build/resources/main/configs/")
}

fun createCopySpec(projectName: String, serviceName: String): CopySpec {
  return copySpec {
    from("../${projectName}/${serviceName}/src/main/resources/configs/common") {
      include("application.conf")
      into("${serviceName}")
    }
  }
}

tasks.register<Copy>("copyOverrideServiceConfigs") {
  with(
      createOverrideCopySpec("hypertrace-federated-service", "gateway-service")
  ).into("./build/resources/main/configs/")
}

fun createOverrideCopySpec(projectName: String, serviceName: String): CopySpec {
  return copySpec {
    from("./src/main/resources/configs/${projectName}/${serviceName}") {
      include("application.conf")
      into("${serviceName}/dev")
    }
  }
}
