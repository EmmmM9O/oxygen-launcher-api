package arc.backend.oxygen

import arc.*
import arc.Application.ApplicationType
import arc.audio.*
import arc.files.*
import arc.func.*
import arc.graphics.*
import arc.math.geom.*
import arc.scene.ui.*
import arc.struct.*
import arc.util.*
import arc.util.Log.*
import arc.util.serialization.Json
import arc.util.serialization.JsonWriter
import java.io.*
import java.net.*
import java.nio.*
import java.util.*
import java.util.function.Supplier
import org.lwjgl.*
import org.lwjgl.egl.*
import org.lwjgl.opengles.*
import org.lwjgl.system.*
import oxygen.api.*
import oxygen.input.*

class DebugStream : Supplier<PrintStream> {
  override fun get(): PrintStream =
      object :
          PrintStream(
              object : OutputStream() {
                override fun write(b: Int) {}
              }
          ) {
        override fun print(x: String) {
          Log.debug(x)
        }

        override fun println(x: String) {
          Log.debug(x)
        }
      }
}

open class OxygenApplication(
    listener: ApplicationListener,
    configDef: OxygenConfig,
) : Application, LauncherBridgeCallback {
  private val listeners = Seq<ApplicationListener>()
  private val runnables = TaskQueue()

  val graphics: OxygenGraphics
  val input: OxygenInput

  val mainThread_: Thread

  var running = true
  var setup = false
  var window = 0L
  var context = 0L
  val config: OxygenConfig
  val json = Json(JsonWriter.OutputType.json).apply { setUsePrototypes(false) }

  init {
    Log.logger = OxygenApplicationLogger()
    LauncherBridge.setCallback(this)
    LauncherBridge.setGameDefault(json.toJson(configDef))
    config = json.fromJson(OxygenConfig::class.java, LauncherBridge.getGameSettings())
    Log.debug("Config: ${LauncherBridge.getGameSettings()}\n---\n${json.toJson(config)}")
    this.listeners.add(listener)

    mainThread_ = Thread.currentThread()

    init()

    Core.app = this
    Core.files = OxygenFiles()

    this.graphics = OxygenGraphics(this)
    Core.graphics = this.graphics
    initGraphics()
    this.input = OxygenInput(this)
    Core.input = this.input
    Core.settings = Settings()
    Core.settings.setDataDirectory(Fi(System.getProperty("oxygen.home")))

    Core.audio = Audio(!config.disableAudio)

    try {
      loopRun()
      listen(ApplicationListener::exit)
    } finally {
      try {
        cleanup()
      } catch (error: Throwable) {
        val msg =
            StringWriter().let {
              PrintWriter(it).let(error::printStackTrace)
              it.toString()
            }
        OSLog.error(msg)
        LauncherBridge.logLauncher("[Mindustry Crash]:\n$msg")
      }
      System.exit(0)
    }
  }

  fun getArch(): String =
      when {
        OS.isARM && OS.is64Bit -> "linux/arm64"
        OS.isARM && !OS.is64Bit -> "linux/arm32"
	else -> ""
      }

  fun getAngleLib(lib: String): String =
      if (config.angle) "${getArch()}/lib${lib}_angle.so" else "lib$lib.so"

  private fun init() {
    OS.isAndroid = false
    OS.isWindows = false
    OS.isLinux = true
    OS.isMac = false
    OS.is64Bit =
        OS.propNoNull("os.arch").contains("64") || OS.propNoNull("os.arch").startsWith("armv8")
    if (config.debug) {
      System.setProperty("org.lwjgl.util.Debug", "true")
      System.setProperty("org.lwjgl.util.DebugLoader", "true")
      System.setProperty("org.lwjgl.util.DebugFunctions", "true")
      Configuration.DEBUG.set(true)
      Configuration.DEBUG_STREAM.set("arc.backend.oxygen.DebugStream")
    }
    Configuration.EGL_LIBRARY_NAME.set(getAngleLib("EGL"))
    Configuration.OPENGLES_LIBRARY_NAME.set(getAngleLib("GLESv2"))
    Configuration.DISABLE_HASH_CHECKS.set(true)
    Configuration.OPENGLES_EXPLICIT_INIT.set(true)
    GLES.create()

    ArcNativesLoader.load()
  }

  private fun initGraphics() {
    LauncherBridge.createsurface()
    // Wait util call back
    while (!setup) {
      runnables.run()
    }
    LauncherBridge.setupInput()
  }

  private fun loopRun() {
    listen(ApplicationListener::init)
    while (running) {
      graphics.update()
      runnables.run()
      if (!graphics.running) continue
      input.processEvents()
      defaultUpdate()

      listen(ApplicationListener::update)

      graphics.swapBuffers()
    }
  }

  private fun listen(cons: Cons<ApplicationListener>) {
    synchronized(listeners) {
      for (l in listeners) {
        cons.get(l)
      }
    }
  }

  private fun cleanup() {
    listen { l ->
      l.pause()
      try {
        l.dispose()
      } catch (t: Throwable) {
        t.printStackTrace()
      }
    }
    dispose()

    graphics.onDestroyed()
  }

  override fun getMainThread(): Thread {
    return mainThread_
  }

  override fun openFolder(file: String): Boolean = LauncherBridge.openFolder(file)

  override fun openURI(url: String): Boolean = LauncherBridge.openURI(url)

  override fun getListeners(): Seq<ApplicationListener> {
    return listeners
  }

  override fun getType(): ApplicationType {
    return ApplicationType.android
  }

  override fun getClipboardText(): String = LauncherBridge.getClipboardText()

  override fun setClipboardText(text: String) {
    LauncherBridge.setClipboardText(text)
  }

  override fun post(runnable: Runnable) {
    runnables.post(runnable)
  }

  override fun exit() {
    running = false
  }

  override fun getVersion(): Int = LauncherBridge.getVersion()

  override fun getNativeHeap(): Long = LauncherBridge.getNativeHeap()

  override fun onPause(): Unit {
    if (!setup) return
    graphics.running = false
    post {
      input.onPause()

      if (LauncherBridge.isFinishing()) {
        graphics.destroy()
      } else {
        graphics.pause()
      }
    }
  }

  override fun onResume(): Unit {
    if (!setup) return
    graphics.running = true
    post {
      input.onResume()
      graphics.resume()
    }
  }

  override fun onDestroy(): Unit {
    post { exit() }
  }

  override fun onSurfaceCreated(surface: Long): Unit {
    post { graphics.onCreated(surface) }
  }

  override fun onSurfaceChanged(width: Int, height: Int): Unit {
    post { graphics.onResize(width, height) }
  }

  override fun onSurfaceDestroyed(): Unit {
    post { graphics.onDestroyed() }
  }

  override fun handleTouch(
      /*MotionEvent*/ intData: IntArray,
      floatData: FloatArray,
  ): Boolean {
    input.handleTouch(BridgeMotionEvent.fromArrays(intData, floatData))
    return true
  }

  override fun handleGenericMotion(
      /*MotionEvent*/ intData: IntArray,
      floatData: FloatArray,
  ): Boolean = input.handleGenericMotion(BridgeGenericMotionEvent.fromArrays(intData, floatData))

  override fun handleKey(
      keyCode: Int, /*KeyEvent*/
      intData: IntArray,
      characters: String,
  ): Boolean = input.handleKey(keyCode, BridgeKeyEvent.fromArray(intData, characters))

  override fun onExit() {
    exit()
  }
}

open class OxygenConfig {
  @JvmField var r: Int = 8
  @JvmField var g: Int = 8
  @JvmField var b: Int = 8
  @JvmField var a: Int = 0
  @JvmField var depth: Int = 16
  @JvmField var stencil: Int = 0
  @JvmField var samples: Int = 0
  @JvmField var disableAudio: Boolean = false
  @JvmField var useGL30: Boolean = true
  @JvmField var debug: Boolean = true
  @JvmField var angle: Boolean = true
}

open class OxygenApplicationLogger : LogHandler {
  override fun log(level: LogLevel, text: String) {
    (when (level) {
      LogLevel.info -> OSLog::info
      LogLevel.warn -> OSLog::warn
      LogLevel.err -> OSLog::error
      LogLevel.debug -> OSLog::debug
      else -> { str -> }
    })(text)
  }
}
