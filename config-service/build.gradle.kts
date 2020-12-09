import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.bmuschko.gradle.docker.tasks.network.DockerCreateNetwork
import com.bmuschko.gradle.docker.tasks.network.DockerRemoveNetwork

plugins {
  java
  application
  jacoco
  id("org.hypertrace.docker-java-application-plugin")
  id("org.hypertrace.docker-publish-plugin")
  id("org.hypertrace.integration-test-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

tasks.register<DockerCreateNetwork>("createIntegrationTestNetwork") {
  networkName.set("cfg-svc-int-test")
}

tasks.register<DockerRemoveNetwork>("removeIntegrationTestNetwork") {
  networkId.set("cfg-svc-int-test")
}

tasks.register<DockerPullImage>("pullMongoImage") {
  image.set("mongo:4.2.6")
}

tasks.register<DockerCreateContainer>("createMongoContainer") {
  dependsOn("createIntegrationTestNetwork")
  dependsOn("pullMongoImage")
  targetImageId(tasks.getByName<DockerPullImage>("pullMongoImage").image)
  containerName.set("mongo-local")
  hostConfig.network.set(tasks.getByName<DockerCreateNetwork>("createIntegrationTestNetwork").networkId)
  hostConfig.portBindings.set(listOf("27017:27017"))
  hostConfig.autoRemove.set(true)
}

tasks.register<DockerStartContainer>("startMongoContainer") {
  dependsOn("createMongoContainer")
  targetContainerId(tasks.getByName<DockerCreateContainer>("createMongoContainer").containerId)
}

tasks.register<DockerStopContainer>("stopMongoContainer") {
  targetContainerId(tasks.getByName<DockerCreateContainer>("createMongoContainer").containerId)
  finalizedBy("removeIntegrationTestNetwork")
}

tasks.integrationTest {
  useJUnitPlatform()
  dependsOn("startMongoContainer")
  finalizedBy("stopMongoContainer")
}

dependencies {
  implementation(project(":config-service-impl"))

  constraints {
    implementation("com.google.guava:guava:30.0-jre") {
      because("https://snyk.io/vuln/SNYK-JAVA-COMGOOGLEGUAVA-1015415")
    }
    runtimeOnly("io.netty:netty-codec-http2:4.1.53.Final") {
      because("https://snyk.io/vuln/SNYK-JAVA-IONETTY-1020439")
    }
    runtimeOnly("io.netty:netty-handler-proxy:4.1.53.Final") {
      because("https://snyk.io/vuln/SNYK-JAVA-IONETTY-1020439")
    }
  }

  implementation("org.hypertrace.core.grpcutils:grpc-server-utils:0.3.2")
  implementation("org.hypertrace.core.serviceframework:platform-service-framework:0.1.18")

  runtimeOnly("io.grpc:grpc-netty:1.33.1")
  implementation("com.typesafe:config:1.4.0")

  implementation("org.slf4j:slf4j-api:1.7.30")
  runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")

  //Integration test dependencies
  integrationTestImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
  integrationTestImplementation("com.google.guava:guava:30.0-jre")
  integrationTestImplementation("org.yaml:snakeyaml:1.26")
  integrationTestImplementation(project(":config-service-impl"))
  integrationTestImplementation("org.hypertrace.core.serviceframework:integrationtest-service-framework:0.1.18")
  integrationTestImplementation("org.hypertrace.core.grpcutils:grpc-client-utils:0.3.2")
}

application {
  mainClass.set("org.hypertrace.core.serviceframework.PlatformServiceLauncher")
}

// Config for gw run to be able to run this locally. Just execute gw run here on Intellij or on the console.
tasks.run<JavaExec>  {
  jvmArgs = listOf("-Dservice.name=${project.name}")
}

tasks.jacocoIntegrationTestReport {
  sourceSets(project(":config-service-impl").sourceSets.getByName("main"))
}

hypertraceDocker {
  defaultImage {
    javaApplication {
      port.set(50101)
    }
  }
}