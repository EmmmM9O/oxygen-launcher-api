plugins {
  alias(libs.plugins.kotlin.jvm)
  `maven-publish`
}

group = "com.github.emmmm9o"

version = "1.0.0"

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
      artifactId = "launcher-api"
    }
  }
  repositories { mavenLocal() }
}
