plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  implementation(project(":attribute-service-tenant-api"))

  implementation("commons-codec:commons-codec:1.13") {
    because("Information Exposure [Low Severity][https://snyk.io/vuln/SNYK-JAVA-COMMONSCODEC-561518] in commons-codec:commons-codec@1.11"
        + " introduced org.apache.httpcomponents:httpclient@4.5.12")
  }

  implementation("org.hypertrace.core.documentstore:document-store:0.1.1")
  implementation("org.hypertrace.core.grpcutils:grpc-context-utils:0.1.3")

  implementation("com.fasterxml.jackson.core:jackson-databind:2.9.5")
  implementation("com.typesafe:config:1.3.2")
  implementation("org.slf4j:slf4j-api:1.7.25")

  testImplementation("org.mockito:mockito-core:3.3.3")
  testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")
}

tasks.test {
  useJUnitPlatform()
}
