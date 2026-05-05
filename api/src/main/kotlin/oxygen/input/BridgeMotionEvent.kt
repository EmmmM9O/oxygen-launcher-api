package oxygen.input
/**
 * IntArray 布局： [0] actionMasked, [1] actionIndex, [2] pointerCount, [3] historySize, [4] source,
 * [5] flags, [6] edgeFlags, [7] metaState, [8] buttonState, [9] deviceId, [10] downTime, [11]
 * eventTime [12,13] pointer0: id, toolType [14,15] pointer1: id, toolType ...
 *
 * FloatArray 布局： [0,1,2,3] pointer0: x, y, pressure, touchMajor [4,5,6,7] pointer1: x, y, pressure,
 * touchMajor ...
 */
class BridgeMotionEvent(
    val actionMasked: Int,
    val actionIndex: Int,
    val action: Int, // 组合后的完整 action = actionMasked | (actionIndex << 8)
    val pointerCount: Int,
    val historySize: Int,
    val source: Int,
    val flags: Int,
    val edgeFlags: Int,
    val metaState: Int,
    val buttonState: Int,
    val deviceId: Int,
    val downTime: Long,
    val eventTime: Long,
    val pointers: List<Pointer>,
    val historical: List<List<Pointer>> = emptyList(),
) {

  data class Pointer(
      val id: Int,
      val toolType: Int,
      val x: Float,
      val y: Float,
      val pressure: Float,
      val touchMajor: Float,
  )

  val x: Float
    get() = if (pointerCount > 0) pointers[0].x else 0f

  /** 获取主指针的 y */
  val y: Float
    get() = if (pointerCount > 0) pointers[0].y else 0f

  fun getX(pointerIndex: Int): Float =
      if (pointerIndex in 0 until pointerCount) pointers[pointerIndex].x else 0f

  fun getY(pointerIndex: Int): Float =
      if (pointerIndex in 0 until pointerCount) pointers[pointerIndex].y else 0f

  fun getHistoricalX(historyIndex: Int, pointerIndex: Int): Float =
      if (historyIndex in historical.indices && pointerIndex in 0 until pointerCount)
          historical[historyIndex][pointerIndex].x
      else 0f

  fun getHistoricalY(historyIndex: Int, pointerIndex: Int): Float =
      if (historyIndex in historical.indices && pointerIndex in 0 until pointerCount)
          historical[historyIndex][pointerIndex].y
      else 0f

  fun getPointerId(index: Int): Int = if (index in 0 until pointerCount) pointers[index].id else -1

  fun getPressure(index: Int): Float =
      if (index in 0 until pointerCount) pointers[index].pressure else 0f

  fun isDown(): Boolean = actionMasked == ACTION_DOWN

  fun isUp(): Boolean = actionMasked == ACTION_UP

  fun isMove(): Boolean = actionMasked == ACTION_MOVE

  fun isCancel(): Boolean = actionMasked == ACTION_CANCEL

  fun isPointerDown(): Boolean = actionMasked == ACTION_POINTER_DOWN

  fun isPointerUp(): Boolean = actionMasked == ACTION_POINTER_UP

  companion object {
    const val ACTION_MASK = 0xff
    const val ACTION_POINTER_INDEX_MASK = 0xff00
    const val ACTION_POINTER_INDEX_SHIFT = 8

    // ========== 触摸动作常量 ==========
    const val ACTION_DOWN = 0 // 单点按下
    const val ACTION_UP = 1 // 单点抬起
    const val ACTION_MOVE = 2 // 移动
    const val ACTION_CANCEL = 3 // 取消
    const val ACTION_OUTSIDE = 4 // 超出边界
    const val ACTION_POINTER_DOWN = 5 // 非主指针按下
    const val ACTION_POINTER_UP = 6 // 非主指针抬起

    // ========== 非触摸事件（鼠标/触控笔悬浮等） ==========
    const val ACTION_HOVER_MOVE = 7 // 指针悬浮移动
    const val ACTION_SCROLL = 8 // 滚动（鼠标滚轮等）
    const val ACTION_HOVER_ENTER = 9 // 指针进入窗口边界
    const val ACTION_HOVER_EXIT = 10 // 指针离开窗口边界

    // ========== 按钮事件 ==========
    const val ACTION_BUTTON_PRESS = 11 // 按钮按下
    const val ACTION_BUTTON_RELEASE = 12 // 按钮释放

    fun fromArrays(intData: IntArray, floatData: FloatArray): BridgeMotionEvent {
      var pos = 0

      val actionMasked = intData[pos++]
      val actionIndex = intData[pos++]
      val pointerCount = intData[pos++]
      val historySize = intData[pos++]
      val source = intData[pos++]
      val flags = intData[pos++]
      val edgeFlags = intData[pos++]
      val metaState = intData[pos++]
      val buttonState = intData[pos++]
      val deviceId = intData[pos++]
      val downTime = intData[pos++].toLong()
      val eventTime = intData[pos++].toLong()

      val pointerIds = IntArray(pointerCount)
      val pointerToolTypes = IntArray(pointerCount)
      for (i in 0 until pointerCount) {
        pointerIds[i] = intData[pos++]
        pointerToolTypes[i] = intData[pos++]
      }

      val pointers =
          (0 until pointerCount).map { p ->
            val offset = p * 4
            Pointer(
                id = pointerIds[p],
                toolType = pointerToolTypes[p],
                x = floatData[offset],
                y = floatData[offset + 1],
                pressure = floatData[offset + 2],
                touchMajor = floatData[offset + 3],
            )
          }

      val action = actionMasked or (actionIndex shl ACTION_POINTER_INDEX_SHIFT)
      return BridgeMotionEvent(
          actionMasked = actionMasked,
          actionIndex = actionIndex,
          action = action,
          pointerCount = pointerCount,
          historySize = historySize,
          source = source,
          flags = flags,
          edgeFlags = edgeFlags,
          metaState = metaState,
          buttonState = buttonState,
          deviceId = deviceId,
          downTime = downTime,
          eventTime = eventTime,
          pointers = pointers,
      )
    }

    fun fromArraysWithHistory(intData: IntArray, floatData: FloatArray): BridgeMotionEvent {
      var pos = 0

      val actionMasked = intData[pos++]
      val actionIndex = intData[pos++]
      val pointerCount = intData[pos++]
      val historySize = intData[pos++]
      val source = intData[pos++]
      val flags = intData[pos++]
      val edgeFlags = intData[pos++]
      val metaState = intData[pos++]
      val buttonState = intData[pos++]
      val deviceId = intData[pos++]
      val downTime = intData[pos++].toLong()
      val eventTime = intData[pos++].toLong()

      val pointerIds = IntArray(pointerCount)
      val pointerToolTypes = IntArray(pointerCount)
      for (i in 0 until pointerCount) {
        pointerIds[i] = intData[pos++]
        pointerToolTypes[i] = intData[pos++]
      }

      val totalFrames = historySize + 1
      val allFrames =
          (0 until totalFrames).map { frame ->
            (0 until pointerCount).map { p ->
              val offset = (frame * pointerCount + p) * 4
              Pointer(
                  id = pointerIds[p],
                  toolType = pointerToolTypes[p],
                  x = floatData[offset],
                  y = floatData[offset + 1],
                  pressure = floatData[offset + 2],
                  touchMajor = floatData[offset + 3],
              )
            }
          }

      val currentPointers = allFrames.last()
      val historicalFrames = allFrames.dropLast(1)
      val action = actionMasked or (actionIndex shl ACTION_POINTER_INDEX_SHIFT)

      return BridgeMotionEvent(
          actionMasked = actionMasked,
          actionIndex = actionIndex,
          action = action,
          pointerCount = pointerCount,
          historySize = historySize,
          source = source,
          flags = flags,
          edgeFlags = edgeFlags,
          metaState = metaState,
          buttonState = buttonState,
          deviceId = deviceId,
          downTime = downTime,
          eventTime = eventTime,
          pointers = currentPointers,
          historical = historicalFrames,
      )
    }
  }

  override fun toString(): String {
    return "BridgeMotionEvent(action=$actionMasked, index=$actionIndex, pointers=$pointerCount, history=$historySize, x=$x, y=$y)"
  }
}

class BridgeGenericMotionEvent(
    val actionMasked: Int,
    val actionIndex: Int,
    val action: Int,
    val source: Int,
    val flags: Int,
    val edgeFlags: Int,
    val metaState: Int,
    val buttonState: Int,
    val deviceId: Int,
    val downTime: Long,
    val eventTime: Long,
    val x: Float,
    val y: Float,
    @JvmField val axisValues: FloatArray,
) {
  fun getAxisValue(axis: Int): Float {
    return if (axis in 0 until axisValues.size) axisValues[axis] else 0f
  }

  companion object {
    const val ACTION_MASK = 0xff
    const val ACTION_POINTER_INDEX_MASK = 0xff00
    const val ACTION_POINTER_INDEX_SHIFT = 8

    const val ACTION_HOVER_MOVE = 7
    const val ACTION_SCROLL = 8
    const val ACTION_HOVER_ENTER = 9
    const val ACTION_HOVER_EXIT = 10
    const val ACTION_BUTTON_PRESS = 11
    const val ACTION_BUTTON_RELEASE = 12

    // 常用轴
    const val AXIS_X = 0
    const val AXIS_Y = 1
    const val AXIS_PRESSURE = 2
    const val AXIS_SIZE = 3
    const val AXIS_TOUCH_MAJOR = 4
    const val AXIS_TOUCH_MINOR = 5
    const val AXIS_TOOL_MAJOR = 6
    const val AXIS_TOOL_MINOR = 7
    const val AXIS_ORIENTATION = 8
    const val AXIS_VSCROLL = 9
    const val AXIS_HSCROLL = 10
    const val AXIS_Z = 11
    const val AXIS_RX = 12
    const val AXIS_RY = 13
    const val AXIS_RZ = 14
    const val AXIS_HAT_X = 15
    const val AXIS_HAT_Y = 16
    const val AXIS_LTRIGGER = 17
    const val AXIS_RTRIGGER = 18
    const val AXIS_THROTTLE = 19
    const val AXIS_RUDDER = 20
    const val AXIS_WHEEL = 21
    const val AXIS_GAS = 22
    const val AXIS_BRAKE = 23
    const val AXIS_DISTANCE = 24
    const val AXIS_TILT = 25
    const val AXIS_SCROLL = 26
    const val AXIS_RELATIVE_X = 27
    const val AXIS_RELATIVE_Y = 28
    const val AXIS_GENERIC_1 = 32
    const val AXIS_GENERIC_2 = 33
    const val AXIS_GENERIC_3 = 34
    const val AXIS_GENERIC_4 = 35
    const val AXIS_GENERIC_5 = 36
    const val AXIS_GENERIC_6 = 37
    const val AXIS_GENERIC_7 = 38
    const val AXIS_GENERIC_8 = 39
    const val AXIS_GENERIC_9 = 40
    const val AXIS_GENERIC_10 = 41
    const val AXIS_GENERIC_11 = 42
    const val AXIS_GENERIC_12 = 43
    const val AXIS_GENERIC_13 = 44
    const val AXIS_GENERIC_14 = 45
    const val AXIS_GENERIC_15 = 46
    const val AXIS_GENERIC_16 = 47

    private const val AXIS_COUNT = 48

    fun fromArrays(intData: IntArray, floatData: FloatArray): BridgeGenericMotionEvent {
      var pos = 0

      val actionMasked = intData[pos++]
      val actionIndex = intData[pos++]
      pos++ // pointerCount，通用事件大多为 0
      pos++ // historySize
      val source = intData[pos++]
      val flags = intData[pos++]
      val edgeFlags = intData[pos++]
      val metaState = intData[pos++]
      val buttonState = intData[pos++]
      val deviceId = intData[pos++]
      val downTime = intData[pos++].toLong()
      val eventTime = intData[pos++].toLong()

      val x = floatData[0]
      val y = floatData[1]
      val axisValues = FloatArray(AXIS_COUNT)
      // floatData 直接是各轴的值，长度应与 AXIS_COUNT 或实际传递的轴数一致
      for (i in 2 until floatData.size) {
        val axisIndex = i - 2
        if (axisIndex < AXIS_COUNT) axisValues[axisIndex] = floatData[i]
      }

      val action = actionMasked or (actionIndex shl ACTION_POINTER_INDEX_SHIFT)

      return BridgeGenericMotionEvent(
          actionMasked,
          actionIndex,
          action,
          source,
          flags,
          edgeFlags,
          metaState,
          buttonState,
          deviceId,
          downTime,
          eventTime,
          x,
          y,
          axisValues,
      )
    }
  }
}
