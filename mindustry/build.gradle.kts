plugins {
  alias(libs.plugins.kotlin.jvm)
  id("java-library")
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

val extractedDir = layout.buildDirectory.dir("extracted")

tasks.register<Copy>("copyNatives") {
  from(
      arrayOf("libs/natives-android.jar", "libs/natives-freetype-android.jar").map {
        zipTree(file(it))
      }
  ) {
    include("**/*.so")
  }
  into(extractedDir)

  eachFile {
    val path = this.path
    val regex = """^(.*)/(lib.+?)\.so$""".toRegex()
    val matchResult = regex.find(path)

    if (matchResult != null) {
      val abiPath = matchResult.groupValues[1]
      val libName = matchResult.groupValues[2]

      val archSuffix =
          when {
            abiPath.contains("arm64", ignoreCase = true) -> "arm64"
            abiPath.contains("arm", ignoreCase = true) -> "arm"
            abiPath.contains("x86_64", ignoreCase = true) -> "64"
            abiPath.contains("x86", ignoreCase = true) -> ""
            else -> ""
          }
      this.path = "${libName}${archSuffix}.so"
    }
  }
}

tasks.register<Jar>("trimNatives") {
  archiveFileName.set("native-trimmed.jar")
  destinationDirectory.set(layout.buildDirectory.dir("libs"))

  from(extractedDir)
  include("**/*.so")

  dependsOn("copyNatives")
}

dependencies {
  api(project(":api"))
  implementation(project(":lwjgl-natives"))
  api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
  api("org.lwjgl", "lwjgl-opengles")
  api("org.lwjgl", "lwjgl-egl")

  val jar1 = file("libs/Mindustry.jar")
  if (!jar1.exists()) {
    println("libs/Mindustry.jar not exists")
  } else {
    api(files(tasks.named("trimJar").map { it.outputs.files.singleFile }))
  }

  val jar2 = file("libs/natives-android.jar")
  val jar3 = file("libs/natives-freetype-android.jar")
  if (!jar2.exists() || !jar3.exists()) {
    println("libs/natives-android.jar or natives-freetype-android.jar not exists")
  } else {
    implementation(files(tasks.named("trimNatives").map { it.outputs.files.singleFile }))
  }

  implementation(
      fileTree("libs") {
        include("*.jar")
        exclude("Mindustry.jar")
        exclude("natives-android.jar")
        exclude("natives-freetype-android.jar")
      }
  )
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
