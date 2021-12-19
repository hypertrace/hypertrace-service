plugins {
  java
  application
  jacoco
  id("org.hypertrace.docker-java-application-plugin")
  id("org.hypertrace.docker-publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  implementation("org.hypertrace.core.query.service:query-service")
  implementation("org.hypertrace.core.query.service:query-service-impl")
  implementation("org.hypertrace.gateway.service:gateway-service")
  implementation("org.hypertrace.gateway.service:gateway-service-impl")

  implementation("org.eclipse.jetty:jetty-server:9.4.42.v20210604")
  implementation("org.eclipse.jetty:jetty-servlet:9.4.42.v20210604")
  implementation("org.eclipse.jetty:jetty-rewrite:9.4.42.v20210604")

  implementation("org.hypertrace.core.serviceframework:platform-service-framework:0.1.29")
  implementation("org.hypertrace.core.grpcutils:grpc-server-utils:0.6.1")
  implementation("org.hypertrace.core.grpcutils:grpc-client-utils:0.6.1")
  implementation("org.hypertrace.core.documentstore:document-store:0.6.0")

  // Config
  implementation("com.typesafe:config:1.4.1")

  // Logging
  implementation("org.slf4j:slf4j-api:1.7.30")
  runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")

  // GRPC
  runtimeOnly("io.grpc:grpc-netty:1.40.0")
  constraints {
    runtimeOnly("io.netty:netty-codec-http2:4.1.71.Final")
    runtimeOnly("io.netty:netty-handler-proxy:4.1.71.Final")
  }

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

application {
  mainClass.set("org.hypertrace.core.serviceframework.PlatformServiceLauncher")
}

hypertraceDocker {
  defaultImage {
    javaApplication {
      envVars.put("CLUSTER_NAME", "default-cluster")
      envVars.put("POD_NAME", "default-pod")
    }
  }
}

// Config for gw run to be able to run this locally. Just execute gw run here on Intellij or on the console.
tasks.run<JavaExec> {
  jvmArgs = listOf("-Dservice.name=${project.name}", "-Dcluster.name=default-cluster")
}

tasks.processResources {
  dependsOn("copyServiceConfigs");
}

tasks.register<Copy>("copyServiceConfigs") {
  with(
      createCopySpec("gateway-service", "gateway-service"),
      createCopySpec("query-service", "query-service")
  ).into("./build/resources/main/configs/")
}

fun createCopySpec(projectName: String, serviceName: String): CopySpec {
  return copySpec {
    from("../${projectName}/${serviceName}/src/main/resources/configs/common") {
      include("application.conf")
      into(serviceName)
    }
  }
}

tasks.test {
  useJUnitPlatform()
}
