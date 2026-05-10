package oxygen.api

object LauncherBridge {
  // Implement in android jni
  external fun logLauncher(message: String): Unit

  external fun logOS(prio: Int, message: String): Unit

  external fun isAndroid(): Boolean

  external fun getVersion(): Int

  external fun getNativeHeap(): Long

  external fun openURI(uri: String): Boolean

  external fun openFolder(path: String): Boolean

  external fun setClipboardText(text: String): Unit

  external fun getClipboardText(): String

  external fun showFileChooser(
      open: Boolean,
      title: String,
      cons: StrCons,
      error: StrCons2,
      extensions: Array<String>,
  )

  external fun haveExternalPermission(): Boolean

  external fun getExternalPermission(code: Int): Unit

  external fun hide(): Unit

  external fun beginForceLandscape(): Unit

  external fun endForceLandscape(): Unit

  external fun postCacheFile(uri: String): Unit

  external fun setCallback(callback: LauncherBridgeCallback): Unit

  external fun createsurface(): Unit

  external fun isFinishing(): Boolean

  external fun getTextInput(
      title: String,
      message: String,
      text: String,
      numeric: Boolean,
      multiline: Boolean,
      maxLength: Int,
      allowEmpty: Boolean,
      onAccepted: StrCons,
      onCanceled: VoidFunc,
  ): Unit

  external fun isShowingTextInput(): Boolean

  external fun setOnscreenKeyboardVisible(visible: Boolean): Unit

  external fun vibrate(milliseconds: Int): Unit

  external fun vibrate(pattern: LongArray, repeat: Int): Unit

  external fun cancelVibrate(): Unit

  init {
    val dir = System.getProperty("oxygenlauncher.nativedir")
    System.load("$dir/libc++_shared.so")
    System.load("$dir/liboxygen.so")
  }
}

fun interface VoidFunc : () -> Unit {
  override fun invoke(): Unit
}

fun interface StrCons : (String) -> Unit {
  override fun invoke(text: String): Unit
}

fun interface StrCons2 : (String, String) -> Unit {
  override fun invoke(str1: String, str2: String): Unit
}

object OSLog {
  const val UNKNOWN = 0
  const val DEFAULT = 1
  const val VERBOSE = 2
  const val DEBUG = 3
  const val INFO = 4
  const val WARN = 5
  const val ERROR = 6
  const val FATAL = 7
  const val SILENT = 8

  fun log(prio: Int, message: String): Unit {
    LauncherBridge.logOS(prio, message)
  }

  fun debug(message: String) {
    log(DEBUG, message)
  }

  fun info(message: String) {
    log(INFO, message)
  }

  fun warn(message: String) {
    log(WARN, message)
  }

  fun error(message: String) {
    log(ERROR, message)
  }
}
