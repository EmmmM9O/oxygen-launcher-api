package oxygen.api

object LauncherBridge {
  // Implement in android jni
  external fun logLauncher(message: String): Unit

  external fun logOS(message: String): Unit

  external fun isAndroid(): Boolean

  external fun getVersion(): Int

  external fun getNativeHeap(): Long

  external fun openURI(uri: String): Boolean

  external fun openFolder(path: String): Boolean

  external fun setClipboardText(text: String): Unit

  external fun getClipboardText(): String

  external fun setCallback(callback: LauncherBridgeCallback): Unit

  external fun createsurface(): Unit

  init {
    val dir = System.getProperty("oxygenlauncher.nativedir")
    System.load("$dir/libc++_shared.so")
    System.load("$dir/liboxygen.so")
  }
}
