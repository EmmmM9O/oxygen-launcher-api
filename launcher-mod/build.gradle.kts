plugins { alias(libs.plugins.kotlin.jvm) }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

dependencies {
  compileOnly(kotlin("stdlib"))
  compileOnly(project(":mindustry"))
}

tasks {
  jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName = "oxygen-launcher-ui.jar"
    from(
        configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) }
    )
    from("assets/") { include("**") }
  }
}
