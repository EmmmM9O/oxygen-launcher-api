package oxygen.launcher

import arc.*
import arc.util.*
import mindustry.*
import mindustry.game.*
import mindustry.mod.*

class LauncherMain : Mod() {
  lateinit var launcherUI: LauncherUI

  init {
    Events.run(EventType.ClientLoadEvent::class.java) {
      launcherUI = LauncherUI()
      Time.run(10f) {
        launcherUI.floatTable.setPosition(0f, Core.graphics.height.toFloat() / 1.5f, Align.topLeft)
      }
    }
  }

  override fun init() {}

  override fun loadContent() {}
}
