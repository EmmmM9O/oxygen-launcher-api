package arc.backend.oxygen

import arc.*
import arc.Graphics.*
import arc.Graphics.Cursor.*
import arc.graphics.*
import arc.graphics.GL20
import arc.graphics.GL30
import arc.graphics.gl.*
import arc.struct.*
import arc.util.*
import java.nio.*
import org.lwjgl.*
import org.lwjgl.egl.*
import org.lwjgl.opengles.*
import org.lwjgl.system.*

class OxygenGraphics(val app: OxygenApplication) : Graphics() {
  val EGL_COVERAGE_SAMPLES_NV = 0x30E1
  private var gl20: GL20? = null
  private var gl30: GL30? = null
  private var glVersion: GLVersion? = null

  private var lastFrameTime: Long = -1
  private var deltaTime: Float = 0f
  private var frameId: Long = 0
  private var frameCounterStart: Long = 0
  private var frames: Int = 0
  private var fps: Int = 0

  private var eglDisplay: Long = 0
  private var eglContext: Long = 0
  private var eglSurface: Long = 0

  private var bufferFormat = BufferFormat(8, 8, 8, 0, 16, 0, 0, false)

  @JvmField var width = 0
  @JvmField var height = 0

  var resumed = false
  var running = false

  var firstResume = true

  val extensions: String by lazy { Gl.getString(GLES20.GL_EXTENSIONS) }

  init {}


  fun initEGL(surface: Long) {
    if (eglDisplay == EGL10.EGL_NO_DISPLAY || eglContext == EGL10.EGL_NO_CONTEXT) {
      fullInit(surface)
      return
    }
    Log.debug("ANativeWindow pointer: 0x%x".format(surface))

    val configA = app.config
    val configAttribs =
        mutableListOf(
                EGL12.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE,
                configA.r,
                EGL10.EGL_GREEN_SIZE,
                configA.g,
                EGL10.EGL_BLUE_SIZE,
                configA.b,
                EGL10.EGL_ALPHA_SIZE,
                configA.a,
                EGL10.EGL_DEPTH_SIZE,
                configA.depth,
                EGL10.EGL_STENCIL_SIZE,
                configA.stencil,
                EGL10.EGL_SURFACE_TYPE,
                EGL10.EGL_WINDOW_BIT,
            )
            .apply {
              if (configA.samples > 0) {
                add(EGL10.EGL_SAMPLE_BUFFERS)
                add(1)
                add(EGL10.EGL_SAMPLES)
                add(configA.samples)
              }
              add(EGL10.EGL_NONE)
            }
            .toIntArray()
    val config = chooseConfig(configAttribs)
    Log.debug("EGL Config: 0x%x".format(config))
    logConfig(config)
    MemoryStack.stackPush().use { stack ->
      val attribs = stack.mallocInt(1)
      attribs.put(EGL10.EGL_NONE)
      attribs.flip()
      eglSurface = EGL10.eglCreateWindowSurface(eglDisplay, config, surface, attribs)
    }
    Log.debug("EGL Surface: 0x%x".format(eglSurface))

    if (!EGL10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
      throw RuntimeException("eglMakeCurrent failed: ${EGL10.eglGetError()}")
    }
    Log.debug("EGL MakeCurrent success")
  }
  fun fullInit(surface: Long) {
    eglDisplay = EGL10.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
      throw RuntimeException("eglGetDisplay failed")
    }
    Log.debug("EGL Display: 0x%x".format(eglDisplay))

    MemoryStack.stackPush().use { stack ->
      val major = stack.mallocInt(1)
      val minor = stack.mallocInt(1)
      if (!EGL10.eglInitialize(eglDisplay, major, minor)) {
        throw RuntimeException("eglInitialize failed: ${EGL10.eglGetError()}")
      }
      Log.debug("EGL version: ${major[0]}.${minor[0]}")
    }

    val configA = app.config
    val configAttribs =
        mutableListOf(
                EGL12.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE,
                configA.r,
                EGL10.EGL_GREEN_SIZE,
                configA.g,
                EGL10.EGL_BLUE_SIZE,
                configA.b,
                EGL10.EGL_ALPHA_SIZE,
                configA.a,
                EGL10.EGL_DEPTH_SIZE,
                configA.depth,
                EGL10.EGL_STENCIL_SIZE,
                configA.stencil,
                EGL10.EGL_SURFACE_TYPE,
                EGL10.EGL_WINDOW_BIT,
            )
            .apply {
              if (configA.samples > 0) {
                add(EGL10.EGL_SAMPLE_BUFFERS)
                add(1)
                add(EGL10.EGL_SAMPLES)
                add(configA.samples)
              }
              add(EGL10.EGL_NONE)
            }
            .toIntArray()
    val config = chooseConfig(configAttribs)
    Log.debug("EGL Config: 0x%x".format(config))
    logConfig(config)

    MemoryStack.stackPush().use { stack ->
      val attribs = stack.mallocInt(3)
      attribs.put(EGL14.EGL_CONTEXT_CLIENT_VERSION).put(2).put(EGL10.EGL_NONE)
      attribs.flip()
      eglContext = EGL10.eglCreateContext(eglDisplay, config, EGL10.EGL_NO_CONTEXT, attribs)
    }
    Log.debug("EGL Context: 0x%x".format(eglContext))

    Log.debug("ANativeWindow pointer: 0x%x".format(surface))

    MemoryStack.stackPush().use { stack ->
      val attribs = stack.mallocInt(1)
      attribs.put(EGL10.EGL_NONE)
      attribs.flip()
      eglSurface = EGL10.eglCreateWindowSurface(eglDisplay, config, surface, attribs)
    }
    Log.debug("EGL Surface: 0x%x".format(eglSurface))

    if (!EGL10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
      throw RuntimeException("eglMakeCurrent failed: ${EGL10.eglGetError()}")
    }
    Log.debug("EGL MakeCurrent success")
  }

  fun onCreated(surface: Long) {
    initEGL(surface)
    setupGL()
    app.setup = true
    running = true
  }

  private fun chooseConfig(attribs: IntArray): Long {
    MemoryStack.stackPush().use { stack ->
      val attribBuffer = stack.mallocInt(attribs.size)
      attribBuffer.put(attribs).flip()
      val configs = stack.mallocPointer(1)
      val numConfigs = stack.mallocInt(1)
      if (!EGL10.eglChooseConfig(eglDisplay, attribBuffer, configs, numConfigs)) {
        throw RuntimeException("eglChooseConfig failed: ${EGL10.eglGetError()}")
      }
      return configs[0]
    }
  }

  protected fun setupGL() {
    val caps = GLES.createCapabilities()
    Core.gl = OxygenGL20().also { gl20 = it }
    Core.gl20 = gl20 as GL20

    val versionString = gl20!!.glGetString(GL20.GL_VERSION)
    val vendorString = gl20!!.glGetString(GL20.GL_VENDOR)
    val rendererString = gl20!!.glGetString(GL20.GL_RENDERER)

    glVersion =
        GLVersion(Application.ApplicationType.android, versionString, vendorString, rendererString)

    if (glVersion!!.atLeast(3, 0) && app.config.useGL30) {
      Core.gl =
          OxygenGL30().also {
            gl20 = it
            gl30 = it
          }
      Core.gl20 = gl20 as GL20
      Core.gl30 = gl30 as GL30
    }
  }

  private fun logConfig(config: Long) {
    val r = getAttrib(config, EGL10.EGL_RED_SIZE, 0)
    val g = getAttrib(config, EGL10.EGL_GREEN_SIZE, 0)
    val b = getAttrib(config, EGL10.EGL_BLUE_SIZE, 0)
    val a = getAttrib(config, EGL10.EGL_ALPHA_SIZE, 0)
    val d = getAttrib(config, EGL10.EGL_DEPTH_SIZE, 0)
    val s = getAttrib(config, EGL10.EGL_STENCIL_SIZE, 0)
    val samples =
        maxOf(
            getAttrib(config, EGL10.EGL_SAMPLES, 0),
            getAttrib(config, EGL_COVERAGE_SAMPLES_NV, 0),
        )
    val coverageSample = getAttrib(config, EGL_COVERAGE_SAMPLES_NV, 0) != 0

    Log.debug("framebuffer: ($r, $g, $b, $a)")
    Log.debug("depthbuffer: ($d)")
    Log.debug("stencilbuffer: ($s)")
    Log.debug("samples: ($samples)")
    Log.debug("coverage sampling: ($coverageSample)")

    bufferFormat = BufferFormat(r, g, b, a, d, s, samples, coverageSample)
  }

  private fun getAttrib(config: Long, attrib: Int, defValue: Int): Int {
    MemoryStack.stackPush().use { stack ->
      val value = stack.mallocInt(1)
      if (EGL10.eglGetConfigAttrib(eglDisplay, config, attrib, value)) {
        return value[0]
      }
    }
    return defValue
  }

  fun onResize(width: Int, height: Int) {
    this.width = width
    this.height = height
    GLES20.glViewport(0, 0, this.width, this.height)

    app.listeners.each { it.resize(this.width, this.height) }
  }

  fun onDestroyed() {
    EGL10.eglMakeCurrent(
        eglDisplay,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_CONTEXT,
    )
    EGL10.eglDestroySurface(eglDisplay, eglSurface)
    //setPreserveEGLContextOnPause
    //EGL10.eglDestroyContext(eglDisplay, eglContext)
    //EGL10.eglTerminate(eglDisplay)
  }

  fun resume() {
    if (!firstResume) {
      firstResume = true
      return
    }
    running = true
    resumed = true
    Gl.reset()
    app.listeners.each(ApplicationListener::resume)
    Gl.reset()
    Log.info("[resume]")
  }

  fun pause() {
    app.listeners.each(ApplicationListener::pause)
    Log.info("[pause]")
    running = false
  }

  fun destroy() {
    running = false
    app.listeners.each(ApplicationListener::pause)
    app.listeners.each {
      try {
        it.exit()
        it.dispose()
      } catch (e: Exception) {
        // suppress dispose errors
        Log.err(e)
      }
    }
    app.dispose()
    Log.info("[destroy]")
    System.exit(0)
  }

  fun update() {
    val time = System.nanoTime()
    if (lastFrameTime == -1L) {
      lastFrameTime = time
    }
    deltaTime = (time - lastFrameTime) / 1000000000.0f
    lastFrameTime = time
    if (resumed) {
      deltaTime = 0f
      resumed = false
    }

    if (time - frameCounterStart >= 1000000000) {
      fps = frames
      frames = 0
      frameCounterStart = time
    }
    frames++
    frameId++
  }

  fun swapBuffers() {
    EGL10.eglSwapBuffers(eglDisplay, eglSurface)
  }

  override fun isGL30Available(): Boolean {
    return gl30 != null
  }

  override fun getGL20(): GL20 {
    return gl20!!
  }

  override fun setGL20(gl20: GL20) {
    this.gl20 = gl20
    Core.gl = gl20
    Core.gl20 = gl20
  }

  override fun getGL30(): GL30? {
    return gl30
  }

  override fun setGL30(gl30: GL30) {
    this.gl20 = gl30
    this.gl30 = gl30
    Core.gl = gl30
    Core.gl20 = gl30
  }

  override fun getWidth(): Int = width

  override fun getHeight(): Int = height

  override fun getBackBufferWidth(): Int = width

  override fun getBackBufferHeight(): Int = height

  override fun getFrameId(): Long = frameId

  override fun getDeltaTime(): Float = deltaTime

  override fun getFramesPerSecond(): Int = fps

  override fun getGLVersion(): GLVersion = glVersion!!

  override fun getPpiX(): Float = 0f

  override fun getPpiY(): Float = 0f

  override fun getPpcX(): Float = 0f

  override fun getPpcY(): Float = 0f

  override fun getDensity(): Float = 0f

  override fun setTitle(title: String) {}

  override fun setVSync(vsync: Boolean) {}

  override fun getBufferFormat(): BufferFormat {
    return bufferFormat
  }

  override fun supportsExtension(extension: String): Boolean = extensions.contains(extension)

  override fun isContinuousRendering(): Boolean = true

  override fun setContinuousRendering(isContinuous: Boolean) {}

  override fun requestRendering() {}

  override fun isFullscreen(): Boolean = true

  override fun newCursor(pixmap: Pixmap, xHotspot: Int, yHotspot: Int): Cursor? = null

  override fun setCursor(cursor: Cursor) {}

  override fun setSystemCursor(cursor: SystemCursor) {}
}
