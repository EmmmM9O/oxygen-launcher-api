package oxygen.launcher

import arc.files.*
import arc.util.*
import arc.util.serialization.*
import java.io.*
import java.net.*
import java.nio.charset.*
import java.nio.file.*
import java.util.*

fun isSimplifiedChineseLocale(): Boolean {
  val locale = Locale.getDefault()
  return locale.language == "zh" && locale.country == "CN"
}

data class Dependency(val definition: String, val repo: String) {
  val group: String
  val name: String
  val version: String
  val artifact: String
  val url: String
  val dir: String

  init {
    val parts = definition.split(":")
    require(parts.size >= 3) { "Invalid dependency definition: $definition" }
    group = parts[0]
    name = parts[1]
    version = parts[2]
    artifact = "$name-$version.jar"
    url =
        repo +
            Paths.get(group.replace(".", "/"), name, version, artifact)
                .toString()
                .replace("\\", "/")
    dir = Paths.get(group.replace(".", "/"), name).toString()
  }
}

data class InstallConfig(
    val mainClientClass: String,
    val mainServerClass: String,
    val fabricVersion: String,
    val providerVersion: String,
    val clientDependencies: Map<String, Dependency>,
    val serverDependencies: Map<String, Dependency>,
)

fun loadConfig(
    overrideConfigPath: String? = null,
    fetchLatestVersion: Boolean = false,
    mirror: String = "",
    resourceStreamProvider: (() -> InputStream?)? = null,
): InstallConfig {
  val rawJson: String =
      when {
        !overrideConfigPath.isNullOrBlank() -> {
          Fi(overrideConfigPath).readString()
        }
        fetchLatestVersion -> {
          val urlString =
              "${mirror}https://raw.githubusercontent.com/Qendolin/mindustry-fabric-loader/stable/installer/src/main/resources/fabric-dependencies.json"
          URI(urlString).toURL().readText(StandardCharsets.UTF_8)
        }
        else -> {
          val stream =
              resourceStreamProvider?.invoke()
                  ?: throw IOException(
                      "The installer is faulty: Unable to read fabric dependencies file."
                  )
          stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        }
      }

  return parseInstallConfig(rawJson)
}

fun parseInstallConfig(rawJson: String): InstallConfig {
  val VERSION = 1
  val json = Jval.read(rawJson)

  val version = json.getInt("version", 0)
  require(version <= VERSION) { "The installer version is too old" }
  require(version >= VERSION) { "The installer version is too new" }

  val mainClasses = json.get("mainClass")
  val mainClientClass = mainClasses.getString("client", "")
  val mainServerClass = mainClasses.getString("server", "")

  val clientDepMap = mutableMapOf<String, Dependency>()
  val serverDepMap = mutableMapOf<String, Dependency>()

  val libraries = json.get("libraries")
  val commonDeps = libraries.get("common")
  val clientDeps = libraries.get("client")
  val serverDeps = libraries.get("server")

  fun loadDeps(deps: Jval, targetMap: MutableMap<String, Dependency>) {
    if (deps.isArray) {
      val arr = deps.asArray()
      for (i in 0 until arr.size) {
        val obj = arr.get(i)
        val def = obj.getString("name", "")
        val repo = obj.getString("url", "")
        require(def.isNotEmpty() && repo.isNotEmpty()) {
          "Invalid dependency at index $i: missing name or url"
        }
        val dep = Dependency(def, repo)
        targetMap["${dep.group}:${dep.name}"] = dep
      }
    }
  }

  loadDeps(commonDeps, clientDepMap)
  loadDeps(clientDeps, clientDepMap)
  loadDeps(commonDeps, serverDepMap)
  loadDeps(serverDeps, serverDepMap)

  val serverFabricDep = serverDepMap["net.fabricmc:fabric-loader"]
  val clientFabricDep = clientDepMap["net.fabricmc:fabric-loader"]
  require(serverFabricDep == clientFabricDep) {
    "Client and Server fabric-loader dependency mismatch"
  }

  val serverProviderDep = serverDepMap["com.github.Qendolin:mindustry-fabric-loader"]
  val clientProviderDep = clientDepMap["com.github.Qendolin:mindustry-fabric-loader"]
  require(serverProviderDep == clientProviderDep) {
    "Client and Server mindustry-fabric-loader dependency mismatch"
  }

  return InstallConfig(
      mainClientClass = mainClientClass,
      mainServerClass = mainServerClass,
      fabricVersion = serverFabricDep?.version ?: error("Missing fabric-loader dependency"),
      providerVersion =
          serverProviderDep?.version ?: error("Missing mindustry-fabric-loader dependency"),
      clientDependencies = clientDepMap,
      serverDependencies = serverDepMap,
  )
}

fun installDependencies(
    appdataDir: Fi,
    dependencies: List<Dependency>,
    logger: (String) -> Unit,
): List<String> {
  val classpath = mutableListOf<String>()

  for (dep in dependencies) {
    logger("Downloading dependency ${dep.definition} from ${dep.url}")
    val libDir = appdataDir.child("libraries").child(dep.dir)
    libDir.mkdirs()
    val dest = libDir.child(dep.artifact)

    val url = URI(dep.url).toURL()
    dest.writeUrl(url)

    classpath.add(dest.absolutePath())
  }

  return classpath
}

private fun java.net.URL.readText(charset: Charset = StandardCharsets.UTF_8): String {
  return this.openStream().use { it.readBytes().toString(charset) }
}

private fun Fi.writeUrl(url: java.net.URL) {
  url.openStream().use { input -> this.write(input, false) }
}
