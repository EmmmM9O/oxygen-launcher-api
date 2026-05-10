plugins {
  alias(libs.plugins.kotlin.jvm)
  id("application")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

val MAIN = "oxygen.example.MainKt"

application { mainClass.set(MAIN) }

val lwjglVersion = "3.4.1"

dependencies {
  implementation(project(":api"))
  implementation(project(":lwjgl-natives"))
  implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
  implementation("org.lwjgl", "lwjgl-opengles")
  implementation("org.lwjgl", "lwjgl-egl")
}

tasks.register<Jar>("dist") {
  archiveClassifier.set("dist")

  from(sourceSets.main.get().output)

  dependsOn(configurations.runtimeClasspath)
  from({
    configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
  })

  manifest { attributes["Main-Class"] = MAIN }

  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
