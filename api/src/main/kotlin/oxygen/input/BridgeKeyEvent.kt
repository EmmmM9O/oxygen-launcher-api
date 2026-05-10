package oxygen.input

/**
 * IntArray 布局： [0] action, [1] keyCode, [2] repeatCount, [3] metaState, [4] flags, [5] scanCode,
 * [6] deviceId, [7] source, [8] downTime, [9] eventTime, [10] unicodeChar
 */
class BridgeKeyEvent(
    val action: Int,
    val keyCode: Int,
    val repeatCount: Int,
    val metaState: Int,
    val flags: Int,
    val scanCode: Int,
    val deviceId: Int,
    val source: Int,
    val downTime: Long,
    val eventTime: Long,
    val unicodeChar: Int,
    val characters: String = "",
) {

  fun isDown(): Boolean = action == ACTION_DOWN

  fun isUp(): Boolean = action == ACTION_UP

  fun isMultiple(): Boolean = action == ACTION_MULTIPLE

  fun isLongPress(): Boolean = repeatCount > 0 && action == ACTION_DOWN

  /** 是否按下了 Ctrl 键 */
  fun isCtrlPressed(): Boolean = (metaState and META_CTRL_ON) != 0

  /** 是否按下了 Shift 键 */
  fun isShiftPressed(): Boolean = (metaState and META_SHIFT_ON) != 0

  /** 是否按下了 Alt 键 */
  fun isAltPressed(): Boolean = (metaState and META_ALT_ON) != 0

  /** 是否按下了 Meta/Win 键 */
  fun isMetaPressed(): Boolean = (metaState and META_META_ON) != 0

  /** 获取可打印字符（如果 unicodeChar 有效） */
  fun getUnicodeChar(): Char? = if (unicodeChar != 0) unicodeChar.toChar() else null

  /** 获取按键标签字符串（需要外部映射表） */
  fun keyCodeToString(): String = KEYCODE_NAMES[keyCode] ?: "KEYCODE_$keyCode"

  companion object {
    // ========== 动作常量 ==========
    const val ACTION_DOWN = 0
    const val ACTION_UP = 1
    const val ACTION_MULTIPLE = 2

    // ========== 修饰键常量 ==========
    const val META_SHIFT_ON = 1
    const val META_ALT_ON = 2
    const val META_CTRL_ON = 0x1000
    const val META_META_ON = 0x10000
    const val META_CAPS_LOCK_ON = 0x100000
    const val META_NUM_LOCK_ON = 0x200000

    // ========== 常用按键码 ==========
    const val KEYCODE_UNKNOWN = 0
    const val KEYCODE_HOME = 3
    const val KEYCODE_BACK = 4
    const val KEYCODE_CALL = 5
    const val KEYCODE_VOLUME_UP = 24
    const val KEYCODE_VOLUME_DOWN = 25
    const val KEYCODE_POWER = 26
    const val KEYCODE_CAMERA = 27
    const val KEYCODE_ENTER = 66
    const val KEYCODE_DEL = 67
    const val KEYCODE_TAB = 61
    const val KEYCODE_SPACE = 62
    const val KEYCODE_SHIFT_LEFT = 59
    const val KEYCODE_SHIFT_RIGHT = 60
    const val KEYCODE_ALT_LEFT = 57
    const val KEYCODE_ALT_RIGHT = 58
    const val KEYCODE_CTRL_LEFT = 113
    const val KEYCODE_CTRL_RIGHT = 114
    const val KEYCODE_META_LEFT = 117
    const val KEYCODE_META_RIGHT = 118
    const val KEYCODE_ESCAPE = 111
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_DPAD_DOWN = 20
    const val KEYCODE_DPAD_LEFT = 21
    const val KEYCODE_DPAD_RIGHT = 22
    const val KEYCODE_DPAD_CENTER = 23
    const val KEYCODE_PAGE_UP = 92
    const val KEYCODE_PAGE_DOWN = 93
    const val KEYCODE_MOVE_HOME = 122
    const val KEYCODE_MOVE_END = 123
    const val KEYCODE_INSERT = 124
    const val KEYCODE_F1 = 131
    const val KEYCODE_F2 = 132
    const val KEYCODE_F3 = 133
    const val KEYCODE_F4 = 134
    const val KEYCODE_F5 = 135
    const val KEYCODE_F6 = 136
    const val KEYCODE_F7 = 137
    const val KEYCODE_F8 = 138
    const val KEYCODE_F9 = 139
    const val KEYCODE_F10 = 140
    const val KEYCODE_F11 = 141
    const val KEYCODE_F12 = 142

    const val KEYCODE_0 = 7
    const val KEYCODE_1 = 8
    const val KEYCODE_2 = 9
    const val KEYCODE_3 = 10
    const val KEYCODE_4 = 11
    const val KEYCODE_5 = 12
    const val KEYCODE_6 = 13
    const val KEYCODE_7 = 14
    const val KEYCODE_8 = 15
    const val KEYCODE_9 = 16

    const val KEYCODE_A = 29
    const val KEYCODE_B = 30
    const val KEYCODE_C = 31
    const val KEYCODE_D = 32
    const val KEYCODE_E = 33
    const val KEYCODE_F = 34
    const val KEYCODE_G = 35
    const val KEYCODE_H = 36
    const val KEYCODE_I = 37
    const val KEYCODE_J = 38
    const val KEYCODE_K = 39
    const val KEYCODE_L = 40
    const val KEYCODE_M = 41
    const val KEYCODE_N = 42
    const val KEYCODE_O = 43
    const val KEYCODE_P = 44
    const val KEYCODE_Q = 45
    const val KEYCODE_R = 46
    const val KEYCODE_S = 47
    const val KEYCODE_T = 48
    const val KEYCODE_U = 49
    const val KEYCODE_V = 50
    const val KEYCODE_W = 51
    const val KEYCODE_X = 52
    const val KEYCODE_Y = 53
    const val KEYCODE_Z = 54

    private val KEYCODE_NAMES =
        mapOf(
            KEYCODE_UNKNOWN to "KEYCODE_UNKNOWN",
            KEYCODE_HOME to "KEYCODE_HOME",
            KEYCODE_BACK to "KEYCODE_BACK",
            KEYCODE_ENTER to "KEYCODE_ENTER",
            KEYCODE_DEL to "KEYCODE_DEL",
            KEYCODE_TAB to "KEYCODE_TAB",
            KEYCODE_SPACE to "KEYCODE_SPACE",
            KEYCODE_SHIFT_LEFT to "KEYCODE_SHIFT_LEFT",
            KEYCODE_SHIFT_RIGHT to "KEYCODE_SHIFT_RIGHT",
            KEYCODE_ALT_LEFT to "KEYCODE_ALT_LEFT",
            KEYCODE_ALT_RIGHT to "KEYCODE_ALT_RIGHT",
            KEYCODE_CTRL_LEFT to "KEYCODE_CTRL_LEFT",
            KEYCODE_CTRL_RIGHT to "KEYCODE_CTRL_RIGHT",
            KEYCODE_ESCAPE to "KEYCODE_ESCAPE",
            KEYCODE_DPAD_UP to "KEYCODE_DPAD_UP",
            KEYCODE_DPAD_DOWN to "KEYCODE_DPAD_DOWN",
            KEYCODE_DPAD_LEFT to "KEYCODE_DPAD_LEFT",
            KEYCODE_DPAD_RIGHT to "KEYCODE_DPAD_RIGHT",
            KEYCODE_DPAD_CENTER to "KEYCODE_DPAD_CENTER",
            KEYCODE_PAGE_UP to "KEYCODE_PAGE_UP",
            KEYCODE_PAGE_DOWN to "KEYCODE_PAGE_DOWN",
            KEYCODE_VOLUME_UP to "KEYCODE_VOLUME_UP",
            KEYCODE_VOLUME_DOWN to "KEYCODE_VOLUME_DOWN",
            KEYCODE_POWER to "KEYCODE_POWER",
            KEYCODE_F1 to "KEYCODE_F1",
            KEYCODE_F2 to "KEYCODE_F2",
            KEYCODE_F3 to "KEYCODE_F3",
            KEYCODE_F4 to "KEYCODE_F4",
            KEYCODE_F5 to "KEYCODE_F5",
            KEYCODE_F6 to "KEYCODE_F6",
            KEYCODE_F7 to "KEYCODE_F7",
            KEYCODE_F8 to "KEYCODE_F8",
            KEYCODE_F9 to "KEYCODE_F9",
            KEYCODE_F10 to "KEYCODE_F10",
            KEYCODE_F11 to "KEYCODE_F11",
            KEYCODE_F12 to "KEYCODE_F12",
            KEYCODE_A to "A",
            KEYCODE_B to "B",
            KEYCODE_C to "C",
            KEYCODE_D to "D",
            KEYCODE_E to "E",
            KEYCODE_F to "F",
            KEYCODE_G to "G",
            KEYCODE_H to "H",
            KEYCODE_I to "I",
            KEYCODE_J to "J",
            KEYCODE_K to "K",
            KEYCODE_L to "L",
            KEYCODE_M to "M",
            KEYCODE_N to "N",
            KEYCODE_O to "O",
            KEYCODE_P to "P",
            KEYCODE_Q to "Q",
            KEYCODE_R to "R",
            KEYCODE_S to "S",
            KEYCODE_T to "T",
            KEYCODE_U to "U",
            KEYCODE_V to "V",
            KEYCODE_W to "W",
            KEYCODE_X to "X",
            KEYCODE_Y to "Y",
            KEYCODE_Z to "Z",
            KEYCODE_0 to "0",
            KEYCODE_1 to "1",
            KEYCODE_2 to "2",
            KEYCODE_3 to "3",
            KEYCODE_4 to "4",
            KEYCODE_5 to "5",
            KEYCODE_6 to "6",
            KEYCODE_7 to "7",
            KEYCODE_8 to "8",
            KEYCODE_9 to "9",
        )

    private const val INT_DATA_SIZE = 11

    fun fromArray(intData: IntArray, characters: String = ""): BridgeKeyEvent {
      var pos = 0

      val action = intData[pos++]
      val keyCode = intData[pos++]
      val repeatCount = intData[pos++]
      val metaState = intData[pos++]
      val flags = intData[pos++]
      val scanCode = intData[pos++]
      val deviceId = intData[pos++]
      val source = intData[pos++]
      val downTime = intData[pos++].toLong()
      val eventTime = intData[pos++].toLong()
      val unicodeChar = intData[pos++]

      return BridgeKeyEvent(
          action = action,
          keyCode = keyCode,
          repeatCount = repeatCount,
          metaState = metaState,
          flags = flags,
          scanCode = scanCode,
          deviceId = deviceId,
          source = source,
          downTime = downTime,
          eventTime = eventTime,
          unicodeChar = unicodeChar,
          characters = characters,
      )
    }

    fun toArray(event: BridgeKeyEvent): IntArray {
      return intArrayOf(
          event.action,
          event.keyCode,
          event.repeatCount,
          event.metaState,
          event.flags,
          event.scanCode,
          event.deviceId,
          event.source,
          event.downTime.toInt(),
          event.eventTime.toInt(),
          event.unicodeChar,
      )
    }
  }

  override fun toString(): String {
    val actionName =
        when (action) {
          ACTION_DOWN -> "DOWN"
          ACTION_UP -> "UP"
          ACTION_MULTIPLE -> "MULTIPLE"
          else -> "UNKNOWN"
        }
    val char = getUnicodeChar()
    val charStr = if (char != null) " char='$char'" else ""
    return "BridgeKeyEvent(action=$actionName, key=${keyCodeToString()}, repeat=$repeatCount$charStr)"
  }
}
