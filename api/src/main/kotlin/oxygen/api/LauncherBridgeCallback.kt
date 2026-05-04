package oxygen.api

interface LauncherBridgeCallback {
  fun onWindowFocusChanged(hasFocus: Boolean): Unit {}

  fun onPause(): Unit {}

  fun onResume(): Unit {}

  fun onDestroy(): Unit {}

  fun onConfigurationChanged(config: String): Unit {}

  fun onActivityResult(requestCode: Int, resultCode: Int, data: String): Unit {}

  fun onSurfaceCreated(surface: Long): Unit {}

  fun onSurfaceChanged(width: Int, height: Int): Unit {}

  fun onSurfaceDestroyed(): Unit {}

  fun handleTouch(/*MotionEvent*/ intData: IntArray, floatData: FloatArray): Boolean = true

  fun handleGenericMotion(
      /*MotionEvent*/ intData: IntArray,
      floatData: FloatArray,
  ): Boolean = true

  fun handleKey(keyCode: Int, /*KeyEvent*/ intData: IntArray): Boolean = true
}
