plugins {
  id("org.hypertrace.repository-plugin") version "0.2.3"
  id("org.hypertrace.ci-utils-plugin") version "0.2.0"
  id("org.hypertrace.jacoco-report-plugin") version "0.1.3" apply false
  id("org.hypertrace.publish-plugin") version "0.3.3" apply false
  id("org.hypertrace.docker-java-application-plugin") version "0.8.0" apply false
  id("org.hypertrace.docker-publish-plugin") version "0.8.0" apply false
  id("org.hypertrace.integration-test-plugin") version "0.1.3" apply false
}

subprojects {
  group = "org.hypertrace.config.service"
  pluginManager.withPlugin("org.hypertrace.publish-plugin") {
    configure<org.hypertrace.gradle.publishing.HypertracePublishExtension> {
      license.set(org.hypertrace.gradle.publishing.License.TRACEABLE_COMMUNITY)
    }
  }

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }
}
