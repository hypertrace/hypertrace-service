plugins {
  id("org.hypertrace.repository-plugin") version "0.2.0"
  id("org.hypertrace.ci-utils-plugin") version "0.1.1"
  id("org.hypertrace.jacoco-report-plugin") version "0.1.0" apply false
  id("org.hypertrace.docker-java-application-plugin") version "0.8.1" apply false
  id("org.hypertrace.docker-publish-plugin") version "0.8.1" apply false
}

subprojects {
  group = "org.hypertrace.graphql"

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }

  pluginManager.withPlugin("java-library") {
    dependencies {
      "api"(platform(project(":hypertrace-graphql-platform")))
      "annotationProcessor"(platform(project(":hypertrace-graphql-platform")))
      "testImplementation"(platform("org.hypertrace.core.graphql:hypertrace-core-graphql-test-platform"))
      "compileOnly"(platform(project(":hypertrace-graphql-platform")))
    }
  }
}
