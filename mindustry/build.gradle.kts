plugins {
  alias(libs.plugins.kotlin.jvm)
  id("application")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

val MAIN = "mindustry.oxygen.MainKt"

application { mainClass.set(MAIN) }

val lwjglVersion = "3.4.1"

tasks.register<Jar>("trimJar") {
  from(zipTree(file("libs/Mindustry.jar"))) {
    include("**")
    exclude("*.dll")
    exclude("*.dylib")
    exclude("*.so")
    exclude("org/lwjgl/**")
    exclude("windows/**")
    exclude("windows32/**")
    exclude("windows64/**")
    exclude("linux/**")
    exclude("linuxarm64/**")
    exclude("linux64/**")
    exclude("macosxarm64/**")
    exclude("macosx64/**")
    exclude("freebsd/**")
    exclude("arc/backend/**")
    exclude("arc/discord/**")
    exclude("arc/filedialogs/**")
    exclude("com/codedisaster/steamworks/**")
    exclude("mindustry/desktop/**")
  }
  archiveFileName.set("Mindustry-trimmed.jar")
  destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

dependencies {
  implementation(project(":api"))
  implementation(project(":lwjgl-natives"))
  implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
  implementation("org.lwjgl", "lwjgl-opengles")
  implementation("org.lwjgl", "lwjgl-egl")
  val localJar = file("libs/Mindustry.jar")
  if (!localJar.exists()) {
    println("libs/Mindustry.jar not exists")
  }else{
    implementation(files(tasks.named("trimJar").map { it.outputs.files.singleFile }))
  }
  implementation(fileTree("libs") {
    include("*.jar")
    exclude("Mindustry.jar")
  })
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
