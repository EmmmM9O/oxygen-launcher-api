package oxygen.example

import java.io.*
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Supplier
import org.lwjgl.BufferUtils
import org.lwjgl.egl.EGL10
import org.lwjgl.egl.EGL12
import org.lwjgl.egl.EGL14
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES20
import org.lwjgl.system.*
import oxygen.api.*

fun Oprint(text: String) {
  LauncherBridge.logLauncher(text)
}

object Vars {
  lateinit var eglHelper: EGLHelper
  lateinit var renderer: Renderer
  @Volatile var running = true
  @Volatile var rendering = false
  @Volatile var glInit = false

  val taskQueue = ConcurrentLinkedQueue<Runnable>()
}

fun startRender(surface: Long) {
  Vars.apply {
    eglHelper = EGLHelper()
    eglHelper.init(surface)
    renderer = Renderer()
    renderer.init()
    rendering = true
  }
}

fun main() {
  LauncherBridge.setCallback(
      object : LauncherBridgeCallback {
        override fun onPause(): Unit {
          LauncherBridge.logLauncher("On Pause")
        }

        override fun onResume(): Unit {
          LauncherBridge.logLauncher("On Resume")
          LauncherBridge.setClipboardText("Test")
        }

        override fun onDestroy(): Unit {
          LauncherBridge.logLauncher("On Destroy")
          Vars.running = false
        }

        override fun onSurfaceCreated(surface: Long): Unit {
          LauncherBridge.logLauncher("On Surface Create: 0x%x".format(surface))
          Vars.taskQueue.offer { startRender(surface) }
        }

        override fun onSurfaceChanged(width: Int, height: Int): Unit {
          LauncherBridge.logLauncher("On Surface Change ${width}x${height}")
          Vars.taskQueue.offer {
            if (Vars.glInit) {
              GLES20.glViewport(0, 0, width, height)
              LauncherBridge.getTextInput(
                  "TextInput",
                  "",
                  "Test",
                  false,
                  true,
                  -1,
                  true,
                  { str -> LauncherBridge.logLauncher("TextInput $str") },
                  { LauncherBridge.logLauncher("On TextInput Cancel") },
              )
            }
          }
        }

        override fun onSurfaceDestroyed(): Unit {
          LauncherBridge.logLauncher("On Surface Destroy")
          Vars.rendering = false
          Vars.taskQueue.offer { Vars.eglHelper.destroy() }
        }
      }
  )

  OSLog.info("Hello Oxygen Launcher Os")
  LauncherBridge.logLauncher("Hello Oxygen Launcher File")
  LauncherBridge.logLauncher(System.getProperty("os.arch"))
  LauncherBridge.logLauncher(System.getProperty("java.vendor"))
  LauncherBridge.logLauncher(System.getProperty("java.vm.vendor"))
  LauncherBridge.logLauncher(System.getProperty("java.runtime.name"))
  System.setProperty("org.lwjgl.util.Debug", "true")
  System.setProperty("org.lwjgl.util.DebugLoader", "true")
  System.setProperty("org.lwjgl.util.DebugFunctions", "true")
  Configuration.DEBUG.set(true)
  Configuration.DEBUG_STREAM.set("oxygen.example.DebugStream")
  Configuration.EGL_LIBRARY_NAME.set("libEGL.so")
  Configuration.OPENGLES_LIBRARY_NAME.set("libGLESv2.so")
  Configuration.DISABLE_HASH_CHECKS.set(true)
  var frameCount = 0
  var lastTime = System.currentTimeMillis()
  LauncherBridge.logLauncher("Call createsurface()")
  LauncherBridge.createsurface()
  while (Vars.running) {
    Vars.taskQueue.poll()?.run()
    if (!Vars.rendering) continue
    Vars.renderer.render()
    Vars.eglHelper.swapBuffers()

    frameCount++
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastTime >= 1000) {
      Oprint("FPS: $frameCount")
      frameCount = 0
      lastTime = currentTime
    }
  }
  Vars.taskQueue.poll()?.run()
}

class DebugStream : Supplier<PrintStream> {
  override fun get(): PrintStream =
      object :
          PrintStream(
              object : OutputStream() {
                override fun write(b: Int) {}
              }
          ) {
        override fun print(x: String) {
          LauncherBridge.logLauncher(x)
        }

        override fun println(x: String) {
          LauncherBridge.logLauncher(x)
        }
      }
}

class EGLHelper {
  private var eglDisplay: Long = 0
  private var eglContext: Long = 0
  private var eglSurface: Long = 0

  fun init(surface: Long) {
    eglDisplay = EGL10.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
      throw RuntimeException("eglGetDisplay failed")
    }
    Oprint("EGL Display: 0x%x".format(eglDisplay))

    MemoryStack.stackPush().use { stack ->
      val major = stack.mallocInt(1)
      val minor = stack.mallocInt(1)
      if (!EGL10.eglInitialize(eglDisplay, major, minor)) {
        throw RuntimeException("eglInitialize failed: ${EGL10.eglGetError()}")
      }
      Oprint("EGL version: ${major[0]}.${minor[0]}")
    }

    // 2. 选择配置
    val configAttribs =
        intArrayOf(
            EGL12.EGL_RENDERABLE_TYPE,
            EGL14.EGL_OPENGL_ES2_BIT,
            EGL10.EGL_RED_SIZE,
            8,
            EGL10.EGL_GREEN_SIZE,
            8,
            EGL10.EGL_BLUE_SIZE,
            8,
            EGL10.EGL_DEPTH_SIZE,
            16,
            EGL10.EGL_SURFACE_TYPE,
            EGL10.EGL_WINDOW_BIT,
            EGL10.EGL_NONE,
        )

    val config = chooseConfig(configAttribs)
    Oprint("EGL Config: 0x%x".format(config))

    // 3. 创建Context
    MemoryStack.stackPush().use { stack ->
      val attribs = stack.mallocInt(3)
      attribs.put(EGL14.EGL_CONTEXT_CLIENT_VERSION).put(2).put(EGL10.EGL_NONE)
      attribs.flip()
      eglContext = EGL10.eglCreateContext(eglDisplay, config, EGL10.EGL_NO_CONTEXT, attribs)
    }
    Oprint("EGL Context: 0x%x".format(eglContext))

    // 4. 获取ANativeWindow并创建Surface
    val nativeWindow = surface
    Oprint("ANativeWindow pointer: 0x%x".format(nativeWindow))

    MemoryStack.stackPush().use { stack ->
      val attribs = stack.mallocInt(1)
      attribs.put(EGL10.EGL_NONE)
      attribs.flip()
      eglSurface = EGL10.eglCreateWindowSurface(eglDisplay, config, nativeWindow, attribs)
    }
    Oprint("EGL Surface: 0x%x".format(eglSurface))

    // 5. 绑定上下文
    if (!EGL10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
      throw RuntimeException("eglMakeCurrent failed: ${EGL10.eglGetError()}")
    }
    Oprint("EGL MakeCurrent success")
    GLES.createCapabilities()
    Vars.glInit = true
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

  fun swapBuffers() {
    EGL10.eglSwapBuffers(eglDisplay, eglSurface)
  }

  fun destroy() {
    EGL10.eglMakeCurrent(
        eglDisplay,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_SURFACE,
        EGL10.EGL_NO_CONTEXT,
    )
    EGL10.eglDestroySurface(eglDisplay, eglSurface)
    EGL10.eglDestroyContext(eglDisplay, eglContext)
    EGL10.eglTerminate(eglDisplay)
  }
}

class Renderer {
  private var program = 0

  fun init() {
    // 顶点着色器
    val vertexShader =
        """
        #version 100
        attribute vec4 aPosition;
        void main() {
            gl_Position = aPosition;
        }
        """
            .trimIndent()

    // 片段着色器
    val fragmentShader =
        """
        #version 100
        precision mediump float;
        void main() {
            gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        }
        """
            .trimIndent()

    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

    program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vs)
    GLES20.glAttachShader(program, fs)
    GLES20.glLinkProgram(program)

    // 检查链接状态
    if (GLES20.glGetProgrami(program, GLES20.GL_LINK_STATUS) == GLES20.GL_FALSE) {
      val log = GLES20.glGetProgramInfoLog(program)
      throw RuntimeException("Program link failed: $log")
    }

    GLES20.glDeleteShader(vs)
    GLES20.glDeleteShader(fs)

    Oprint("Shader program created: $program")
  }

  private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)

    if (GLES20.glGetShaderi(shader, GLES20.GL_COMPILE_STATUS) == GLES20.GL_FALSE) {
      val log = GLES20.glGetShaderInfoLog(shader)
      GLES20.glDeleteShader(shader)
      throw RuntimeException("Shader compile failed: $log")
    }

    return shader
  }

  fun render() {
    // 清屏绿色
    GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    // 使用着色器
    GLES20.glUseProgram(program)

    // 三角形顶点
    val vertices = floatArrayOf(0.0f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f)

    val vertexBuffer: FloatBuffer = BufferUtils.createFloatBuffer(vertices.size)
    vertexBuffer.put(vertices).flip()

    GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
    GLES20.glEnableVertexAttribArray(0)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
    GLES20.glDisableVertexAttribArray(0)
  }
}
