package arc.backend.oxygen

import arc.*
import arc.input.*
import arc.math.*
import arc.math.geom.*
import arc.scene.ui.*
import arc.struct.*
import arc.util.*
import arc.util.pooling.*
import java.util.*
import oxygen.api.*
import oxygen.input.*

class OxygenInput(val app: OxygenApplication) : Input() {
  companion object {
    const val maxTouches = 20
  }

  val hasMultitouch: Boolean
  var keyEvents = ArrayList<KeyEvent>()
  var touchEvents = ArrayList<TouchEvent>()
  var touchX = IntArray(maxTouches)
  var touchY = IntArray(maxTouches)
  var deltaX = IntArray(maxTouches)
  var deltaY = IntArray(maxTouches)
  var touched = BooleanArray(maxTouches)
  var button = IntArray(maxTouches)
  var realId = IntArray(maxTouches) { -1 }
  var pressure = FloatArray(maxTouches)
  var keyboardAvailable = false
  private var currentEventTimeStamp = System.nanoTime()
  private var mouseLastX = 0
  private var mouseLastY = 0
  private var _justTouched = false

  private val usedKeyEvents =
      object : Pool<KeyEvent>(16, 1000) {
        override fun newObject(): KeyEvent = KeyEvent()
      }

  private val usedTouchEvents =
      object : Pool<TouchEvent>(16, 1000) {
        override fun newObject(): TouchEvent = TouchEvent()
      }

  init {
    hasMultitouch = true
    // TODO I hope so but Platfor but Platformm
  }

  override fun mouseX(): Int {
    synchronized(this) {
      return touchX[0]
    }
  }

  override fun mouseY(): Int {
    synchronized(this) {
      return touchY[0]
    }
  }

  override fun mouseX(pointer: Int): Int {
    synchronized(this) {
      return touchX[pointer]
    }
  }

  override fun mouseY(pointer: Int): Int {
    synchronized(this) {
      return touchY[pointer]
    }
  }

  override fun isTouched(pointer: Int): Boolean {
    synchronized(this) {
      return touched[pointer]
    }
  }

  override fun getPressure(): Float = getPressure(0)

  override fun getPressure(pointer: Int): Float = pressure[pointer]

  override fun isTouched(): Boolean {
    synchronized(this) {
      if (hasMultitouch) {
        for (pointer in 0 until maxTouches) {
          if (touched[pointer]) {
            return true
          }
        }
      }
      return touched[0]
    }
  }

  fun processDevices() {
    keyboard.postUpdate()
  }

  fun processEvents() {
    synchronized(this) {
      _justTouched = false

      val processor = this.inputMultiplexer

      var len = keyEvents.size
      for (i in 0 until len) {
        val e = keyEvents[i]
        currentEventTimeStamp = e.timeStamp
        when (e.type) {
          KeyEvent.KEY_DOWN -> processor.keyDown(e.keyCode)
          KeyEvent.KEY_UP -> processor.keyUp(e.keyCode)
          KeyEvent.KEY_TYPED -> processor.keyTyped(e.keyChar)
        }
        usedKeyEvents.free(e)
      }

      len = touchEvents.size
      for (i in 0 until len) {
        val e = touchEvents[i]
        currentEventTimeStamp = e.timeStamp
        when (e.type) {
          TouchEvent.TOUCH_DOWN -> {
            processor.touchDown(e.x, e.y, e.pointer, e.button)
            _justTouched = true
          }
          TouchEvent.TOUCH_UP -> processor.touchUp(e.x, e.y, e.pointer, e.button)
          TouchEvent.TOUCH_DRAGGED -> processor.touchDragged(e.x, e.y, e.pointer)
          TouchEvent.TOUCH_MOVED -> processor.mouseMoved(e.x, e.y)
          TouchEvent.TOUCH_SCROLLED ->
              processor.scrolled(e.scrollAmountX.toFloat(), e.scrollAmountY.toFloat())
        }
        usedTouchEvents.free(e)
      }

      if (touchEvents.isEmpty()) {
        for (i in deltaX.indices) {
          deltaX[0] = 0
          deltaY[0] = 0
        }
      }

      keyEvents.clear()
      touchEvents.clear()
    }
  }

  fun handleTouch(event: BridgeMotionEvent) {
    val action = event.action and BridgeMotionEvent.ACTION_MASK
    var pointerIndex =
        (event.action and BridgeMotionEvent.ACTION_POINTER_INDEX_MASK) shr
            BridgeMotionEvent.ACTION_POINTER_INDEX_SHIFT
    var pointerId = event.getPointerId(pointerIndex)

    var x: Int
    var y: Int
    var realPointerIndex: Int
    var button: KeyCode

    val timeStamp = System.nanoTime()
    synchronized(this) {
      when (action) {
        BridgeMotionEvent.ACTION_DOWN,
        BridgeMotionEvent.ACTION_POINTER_DOWN -> {
          realPointerIndex = this.getFreePointerIndex()
          if (realPointerIndex >= maxTouches) return
          this.realId[realPointerIndex] = pointerId
          x = event.getX(pointerIndex).toInt()
          y = event.getY(pointerIndex).toInt()
          button = toButton(event.buttonState)
          if (button != KeyCode.unknown)
              postTouchEvent(this, TouchEvent.TOUCH_DOWN, x, y, realPointerIndex, button, timeStamp)
          this.touchX[realPointerIndex] = x
          this.touchY[realPointerIndex] = Core.graphics.height - 1 - y
          this.deltaX[realPointerIndex] = 0
          this.deltaY[realPointerIndex] = 0
          this.touched[realPointerIndex] = (button != KeyCode.unknown)
          this.button[realPointerIndex] = button.ordinal
          this.pressure[realPointerIndex] = event.getPressure(pointerIndex)
        }

        BridgeMotionEvent.ACTION_UP,
        BridgeMotionEvent.ACTION_POINTER_UP,
        BridgeMotionEvent.ACTION_OUTSIDE -> {
          realPointerIndex = this.lookUpPointerIndex(pointerId)
          if (realPointerIndex == -1) return
          if (realPointerIndex >= maxTouches) return
          this.realId[realPointerIndex] = -1
          x = event.getX(pointerIndex).toInt()
          y = event.getY(pointerIndex).toInt()
          button = KeyCode.byOrdinal(this.button[realPointerIndex])
          if (button != KeyCode.unknown)
              postTouchEvent(this, TouchEvent.TOUCH_UP, x, y, realPointerIndex, button, timeStamp)
          this.touchX[realPointerIndex] = x
          this.touchY[realPointerIndex] = Core.graphics.height - 1 - y
          this.deltaX[realPointerIndex] = 0
          this.deltaY[realPointerIndex] = 0
          this.touched[realPointerIndex] = false
          this.button[realPointerIndex] = 0
          this.pressure[realPointerIndex] = 0f
        }

        BridgeMotionEvent.ACTION_CANCEL -> {
          for (i in this.realId.indices) {
            this.realId[i] = -1
            this.touchX[i] = 0
            this.touchY[i] = 0
            this.deltaX[i] = 0
            this.deltaY[i] = 0
            this.touched[i] = false
            this.button[i] = 0
            this.pressure[i] = 0f
          }
        }

        BridgeMotionEvent.ACTION_MOVE -> {
          val pointerCount = event.pointerCount
          for (i in 0 until pointerCount) {
            pointerIndex = i
            pointerId = event.getPointerId(pointerIndex)
            x = event.getX(pointerIndex).toInt()
            y = event.getY(pointerIndex).toInt()
            realPointerIndex = this.lookUpPointerIndex(pointerId)
            if (realPointerIndex == -1) continue
            if (realPointerIndex >= maxTouches) break
            button = KeyCode.byOrdinal(this.button[realPointerIndex])
            if (button != KeyCode.unknown)
                postTouchEvent(
                    this,
                    TouchEvent.TOUCH_DRAGGED,
                    x,
                    y,
                    realPointerIndex,
                    button,
                    timeStamp,
                )
            else
                postTouchEvent(
                    this,
                    TouchEvent.TOUCH_MOVED,
                    x,
                    y,
                    realPointerIndex,
                    KeyCode.mouseLeft,
                    timeStamp,
                )
            this.deltaX[realPointerIndex] = x - this.touchX[realPointerIndex]
            this.deltaY[realPointerIndex] = -(y - this.touchY[realPointerIndex])
            this.touchX[realPointerIndex] = x
            this.touchY[realPointerIndex] = Core.graphics.height - 1 - y
            this.pressure[realPointerIndex] = event.getPressure(pointerIndex)
          }
        }
      }
    }
    Core.graphics.requestRendering()
  }

  private fun toButton(button: Int): KeyCode {
    if (button == 0 || button == 1) return KeyCode.mouseLeft
    if (button == 2) return KeyCode.mouseRight
    if (button == 4) return KeyCode.mouseMiddle
    if (button == 8) return KeyCode.mouseBack
    if (button == 16) return KeyCode.mouseForward
    return KeyCode.unknown
  }

  private fun postTouchEvent(
      input: OxygenInput,
      type: Int,
      x: Int,
      y: Int,
      pointer: Int,
      button: KeyCode,
      timeStamp: Long,
  ) {
    val event = input.usedTouchEvents.obtain()
    event.timeStamp = timeStamp
    event.pointer = pointer
    event.x = x
    event.y = Core.graphics.height - y - 1
    event.type = type
    event.button = button
    input.touchEvents.add(event)
  }

  fun onTap(x: Int, y: Int) {
    postTap(x, y)
  }

  fun onDrop(x: Int, y: Int) {
    postTap(x, y)
  }

  protected fun postTap(x: Int, y: Int) {
    synchronized(this) {
      var event = usedTouchEvents.obtain()
      event.timeStamp = System.nanoTime()
      event.pointer = 0
      event.x = x
      event.y = app.graphics.height - y - 1
      event.type = TouchEvent.TOUCH_DOWN
      touchEvents.add(event)

      event = usedTouchEvents.obtain()
      event.timeStamp = System.nanoTime()
      event.pointer = 0
      event.x = x
      event.y = app.graphics.height - y - 1
      event.type = TouchEvent.TOUCH_UP
      touchEvents.add(event)
    }
    Core.graphics.requestRendering()
  }

  fun handleKey(keyCode: Int, e: BridgeKeyEvent): Boolean {
    if (e.action == BridgeKeyEvent.ACTION_DOWN && e.repeatCount > 0)
        return caughtKeys.contains(keyCode)

    synchronized(this) {
      if (
          e.keyCode == BridgeKeyEvent.KEYCODE_UNKNOWN && e.action == BridgeKeyEvent.ACTION_MULTIPLE
      ) {
        val chars = e.characters
        for (i in 0 until chars.length) {
          var event = usedKeyEvents.obtain()
          event.timeStamp = System.nanoTime()
          event.keyCode = KeyCode.unknown
          event.keyChar = chars[i]
          event.type = KeyEvent.KEY_TYPED
          keyEvents.add(event)
        }
        return false
      }

      var character = e.unicodeChar.toChar()
      if (keyCode == 67) character = '\b'
      if (e.keyCode < 0) {
        return false
      }

      val code = OxygenInputMap.getKeyCode(e.keyCode)

      when (e.action) {
        BridgeKeyEvent.ACTION_DOWN -> {
          var event = usedKeyEvents.obtain()
          event.timeStamp = System.nanoTime()
          event.keyChar = 0.toChar()
          event.keyCode = code
          event.type = KeyEvent.KEY_DOWN

          if (keyCode == BridgeKeyEvent.KEYCODE_BACK && e.isAltPressed()) {
            event.keyCode = KeyCode.buttonCircle
          }

          keyEvents.add(event)
        }

        BridgeKeyEvent.ACTION_UP -> {
          val timeStamp = System.nanoTime()
          var event = usedKeyEvents.obtain()
          event.timeStamp = timeStamp
          event.keyChar = 0.toChar()
          event.keyCode = code
          event.type = KeyEvent.KEY_UP
          if (keyCode == BridgeKeyEvent.KEYCODE_BACK && e.isAltPressed()) {
            event.keyCode = KeyCode.buttonCircle
          }
          keyEvents.add(event)

          event = usedKeyEvents.obtain()
          event.timeStamp = timeStamp
          event.keyChar = character
          event.keyCode = KeyCode.unknown
          event.type = KeyEvent.KEY_TYPED
          keyEvents.add(event)
        }
      }
      Core.graphics.requestRendering()
    }

    if (keyCode == 255) return true
    return caughtKeys.contains(OxygenInputMap.getKeyCode(keyCode).ordinal)
  }

  override fun getTextInput(input: Input.TextInput): Unit {
    LauncherBridge.getTextInput(
        input.title,
        input.message,
        input.text,
        input.numeric,
        input.multiline,
        input.maxLength,
        input.allowEmpty,
        { str -> input.accepted.get(str) },
        { input.canceled.run() },
    )
  }

  override fun isShowingTextInput(): Boolean = LauncherBridge.isShowingTextInput()

  override fun setOnscreenKeyboardVisible(visible: Boolean): Unit {
    LauncherBridge.setOnscreenKeyboardVisible(visible)
  }

  override fun vibrate(milliseconds: Int): Unit {
    LauncherBridge.vibrate(milliseconds)
  }

  override fun vibrate(pattern: LongArray, repeat: Int): Unit =
      LauncherBridge.vibrate(pattern, repeat)

  override fun cancelVibrate(): Unit {
    LauncherBridge.cancelVibrate()
  }

  override fun justTouched(): Boolean = _justTouched

  fun getFreePointerIndex(): Int {
    val len = realId.size
    for (i in 0 until len) {
      if (realId[i] == -1) return i
    }

    realId = resize(realId)
    touchX = resize(touchX)
    touchY = resize(touchY)
    deltaX = resize(deltaX)
    deltaY = resize(deltaY)
    touched = resize(touched)
    button = resize(button)

    return len
  }

  private fun resize(orig: IntArray): IntArray {
    val tmp = IntArray(orig.size + 2)
    System.arraycopy(orig, 0, tmp, 0, orig.size)
    return tmp
  }

  private fun resize(orig: FloatArray): FloatArray {
    val tmp = FloatArray(orig.size + 2)
    System.arraycopy(orig, 0, tmp, 0, orig.size)
    return tmp
  }

  private fun resize(orig: BooleanArray): BooleanArray {
    val tmp = BooleanArray(orig.size + 2)
    System.arraycopy(orig, 0, tmp, 0, orig.size)
    return tmp
  }

  fun lookUpPointerIndex(pointerId: Int): Int {
    val len = realId.size
    for (i in 0 until len) {
      if (realId[i] == pointerId) return i
    }

    val sb = StringBuilder()
    for (i in 0 until len) {
      sb.append(i).append(":").append(realId[i]).append(" ")
    }
    Log.err("AndroidInput: Pointer ID lookup failed: $pointerId, $sb")
    return -1
  }

  override fun deltaX(): Int = deltaX[0]

  override fun deltaX(pointer: Int): Int = deltaX[pointer]

  override fun deltaY(): Int = deltaY[0]

  override fun deltaY(pointer: Int): Int = deltaY[pointer]

  override fun getCurrentEventTime(): Long = currentEventTimeStamp

  fun onPause() {
    Arrays.fill(realId, -1)
    Arrays.fill(touched, false)
  }

  fun onResume() {}

  class KeyEvent {
    companion object {
      const val KEY_DOWN = 0
      const val KEY_UP = 1
      const val KEY_TYPED = 2
    }

    var timeStamp: Long = 0
    var type: Int = 0
    var keyCode: KeyCode = KeyCode.unknown
    var keyChar: Char = 0.toChar()
  }

  class TouchEvent {
    companion object {
      const val TOUCH_DOWN = 0
      const val TOUCH_UP = 1
      const val TOUCH_DRAGGED = 2
      const val TOUCH_SCROLLED = 3
      const val TOUCH_MOVED = 4
    }

    var timeStamp: Long = 0
    var type: Int = 0
    var x: Int = 0
    var y: Int = 0
    var scrollAmountX: Int = 0
    var scrollAmountY: Int = 0
    var button: KeyCode = KeyCode.unknown
    var pointer: Int = 0
  }

  fun handleGenericMotion(event: BridgeGenericMotionEvent): Boolean {
    val action = event.action and BridgeGenericMotionEvent.ACTION_MASK
    var x: Int
    var y: Int
    var scrollAmountX: Int
    var scrollAmountY: Int
    val pointer = 0

    val timeStamp = System.nanoTime()
    synchronized(this) {
      when (action) {
        BridgeGenericMotionEvent.ACTION_HOVER_MOVE -> {
          x = event.x.toInt()
          y = event.y.toInt()
          if ((x != mouseLastX) || (y != mouseLastY)) {
            postTouchEvent(TouchEvent.TOUCH_MOVED, x, y, 0, 0, timeStamp)

            touchX[pointer] = x
            touchY[pointer] = Core.graphics.height - 1 - y
            deltaX[pointer] = x - mouseLastX
            deltaY[pointer] = -(y - mouseLastY)

            mouseLastX = x
            mouseLastY = y
          }
        }

        BridgeGenericMotionEvent.ACTION_SCROLL -> {
          scrollAmountY =
              (-Math.signum(event.getAxisValue(BridgeGenericMotionEvent.AXIS_VSCROLL))).toInt()
          scrollAmountX =
              (-Math.signum(event.getAxisValue(BridgeGenericMotionEvent.AXIS_HSCROLL))).toInt()
          postTouchEvent(TouchEvent.TOUCH_SCROLLED, 0, 0, scrollAmountX, scrollAmountY, timeStamp)
        }
      }
    }
    Core.graphics.requestRendering()
    return true
  }

  private fun postTouchEvent(
      type: Int,
      x: Int,
      y: Int,
      scrollAmountX: Int,
      scrollAmountY: Int,
      timeStamp: Long,
  ) {
    val event = usedTouchEvents.obtain()
    event.timeStamp = timeStamp
    event.x = x
    event.y = Core.graphics.height - y - 1
    event.type = type
    event.scrollAmountX = scrollAmountX
    event.scrollAmountY = scrollAmountY
    touchEvents.add(event)
  }
}

object OxygenInputMap {
  fun getKeyCode(key: Int): KeyCode =
      when (key) {
        -1 -> KeyCode.anyKey
        7 -> KeyCode.num0
        8 -> KeyCode.num1
        9 -> KeyCode.num2
        10 -> KeyCode.num3
        11 -> KeyCode.num4
        12 -> KeyCode.num5
        13 -> KeyCode.num6
        14 -> KeyCode.num7
        15 -> KeyCode.num8
        16 -> KeyCode.num9
        29 -> KeyCode.a
        57 -> KeyCode.altLeft
        58 -> KeyCode.altRight
        75 -> KeyCode.apostrophe
        77 -> KeyCode.at
        30 -> KeyCode.b
        4 -> KeyCode.back
        73 -> KeyCode.backslash
        31 -> KeyCode.c
        5 -> KeyCode.call
        27 -> KeyCode.camera
        28 -> KeyCode.clear
        55 -> KeyCode.comma
        32 -> KeyCode.d
        67 -> KeyCode.backspace
        112 -> KeyCode.forwardDel
        23 -> KeyCode.center
        20 -> KeyCode.down
        21 -> KeyCode.left
        22 -> KeyCode.right
        19 -> KeyCode.up
        33 -> KeyCode.e
        6 -> KeyCode.endcall
        66 -> KeyCode.enter
        65 -> KeyCode.envelope
        70 -> KeyCode.equals
        34 -> KeyCode.f
        80 -> KeyCode.focus
        35 -> KeyCode.g
        68 -> KeyCode.backtick
        36 -> KeyCode.h
        79 -> KeyCode.headsetHook
        3 -> KeyCode.home
        37 -> KeyCode.i
        38 -> KeyCode.j
        39 -> KeyCode.k
        40 -> KeyCode.l
        71 -> KeyCode.leftBracket
        41 -> KeyCode.m
        90 -> KeyCode.mediaFastForward
        87 -> KeyCode.mediaNext
        85 -> KeyCode.mediaPlayPause
        88 -> KeyCode.mediaPrevious
        89 -> KeyCode.mediaRewind
        86 -> KeyCode.mediaStop
        82 -> KeyCode.menu
        69 -> KeyCode.minus
        91 -> KeyCode.mute
        42 -> KeyCode.n
        83 -> KeyCode.notification
        78 -> KeyCode.num
        43 -> KeyCode.o
        44 -> KeyCode.p
        56 -> KeyCode.period
        81 -> KeyCode.plus
        18 -> KeyCode.pound
        26 -> KeyCode.power
        45 -> KeyCode.q
        46 -> KeyCode.r
        72 -> KeyCode.rightBracket
        47 -> KeyCode.s
        84 -> KeyCode.search
        74 -> KeyCode.semicolon
        59 -> KeyCode.shiftLeft
        60 -> KeyCode.shiftRight
        76 -> KeyCode.slash
        1 -> KeyCode.softLeft
        2 -> KeyCode.softRight
        62 -> KeyCode.space
        17 -> KeyCode.star
        63 -> KeyCode.sym
        48 -> KeyCode.t
        61 -> KeyCode.tab
        49 -> KeyCode.u
        0 -> KeyCode.unknown
        50 -> KeyCode.v
        25 -> KeyCode.volumeDown
        24 -> KeyCode.volumeUp
        51 -> KeyCode.w
        52 -> KeyCode.x
        53 -> KeyCode.y
        54 -> KeyCode.z
        64 -> KeyCode.metaShiftLeftOn
        128 -> KeyCode.metaShiftRightOn
        129 -> KeyCode.controlLeft
        130 -> KeyCode.controlRight
        111 -> KeyCode.escape
        123 -> KeyCode.end
        124 -> KeyCode.insert
        92 -> KeyCode.pageUp
        93 -> KeyCode.pageDown
        94 -> KeyCode.pictSymbols
        95 -> KeyCode.switchCharset
        255 -> KeyCode.buttonC
        96 -> KeyCode.buttonA
        97 -> KeyCode.buttonB
        98 -> KeyCode.buttonC
        99 -> KeyCode.buttonX
        100 -> KeyCode.buttonY
        101 -> KeyCode.buttonZ
        102 -> KeyCode.buttonL1
        103 -> KeyCode.buttonL1
        104 -> KeyCode.buttonL2
        105 -> KeyCode.buttonL2
        106 -> KeyCode.buttonThumbL
        107 -> KeyCode.buttonThumbR
        108 -> KeyCode.buttonStart
        109 -> KeyCode.buttonSelect
        110 -> KeyCode.buttonMode
        144 -> KeyCode.numpad0
        145 -> KeyCode.numpad1
        146 -> KeyCode.numpad2
        147 -> KeyCode.numpad3
        148 -> KeyCode.numpad4
        149 -> KeyCode.numpad5
        150 -> KeyCode.numpad6
        151 -> KeyCode.numpad7
        152 -> KeyCode.numpad8
        153 -> KeyCode.numpad9
        243 -> KeyCode.colon
        131 -> KeyCode.f1
        132 -> KeyCode.f2
        133 -> KeyCode.f3
        134 -> KeyCode.f4
        135 -> KeyCode.f5
        136 -> KeyCode.f6
        137 -> KeyCode.f7
        138 -> KeyCode.f8
        139 -> KeyCode.f9
        140 -> KeyCode.f10
        141 -> KeyCode.f11
        142 -> KeyCode.f12
        else -> KeyCode.unknown
      }
}
