plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(project(":config-service-api"))
  implementation("com.google.guava:guava:30.0-jre")
  testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

tasks.test {
  useJUnitPlatform()
}
