package oxygen.launcher

import arc.*
import arc.math.*
import arc.math.geom.*
import arc.scene.*
import arc.scene.event.*
import arc.scene.style.*
import arc.scene.ui.*
import arc.scene.ui.layout.*
import arc.struct.*
import arc.util.*
import arc.util.io.*
import arc.util.serialization.*
import java.io.StringReader
import mindustry.*
import mindustry.gen.*
import mindustry.ui.*
import oxygen.api.*

class LauncherUI {
  val floatTable: FloatingTable
  val content: Table
  var current: Int = -1
  val bSize = 48f

  init {
    floatTable = FloatingTable()
    floatTable.reset()
    content = Table(Styles.black6).apply { visible = false }
    floatTable.table { main ->
      main
          .table { leftMain ->
            leftMain
                .table { buttons ->
                  buttons.add(floatTable.dragButton).size(bSize).row()
                  submenu(0, Icon.settings, buttons, settingsF())
                  submenu(1, Icon.info, buttons, infoF())
                }
                .width(bSize)
                .top()
                .row()
            leftMain.table().fill()
          }
          .width(bSize)
          .left()
          .uniformY()
      main.add(content).uniformY()
    }
    Core.scene.add(floatTable)
  }

  fun Table.buildSettings(json: Jval.JsonMap, callback: () -> Unit) {
    json.entries().forEach { en ->
      val name = en.key
      val value = en.value
      if (value.isObject()) {
        table { con ->
              con.table().width(8f).grow().left()
              con.table { tab ->
                    tab.table().width(8f).grow()
                    tab.add(Core.bundle.get("oxygenl.settings.$name", name)).left()
                    tab.table().grow().row()
                    tab.table().height(8f).grow().row()
                    tab.buildSettings(value.asObject(), callback)
                  }
                  .left()
                  .grow()
              con.table().grow()
            }
            .uniformX()
            .row()
      } else if (value.isBoolean()) {
        table().width(8f).grow()
        check(Core.bundle.get("oxygenl.settings.$name", name), value.asBool()) {
              json.put(name, Jval.valueOf(it))
              callback()
            }
            .left()
        table().grow().row()
      } else {
        table().width(8f).grow()
        add(Core.bundle.get("oxygenl.settings.$name", name)).left()
        table().width(8f).grow()
        lateinit var fie: TextField
        fie =
            field(value.toString()) {
                  try {
                    json.put(name, Jval.read(it))
                    callback()
                  } catch (err: Throwable) {
                    fie.setText(value.toString())
                  }
                }
                .get()
        row()
        if (!value.isString()) {
          fie.setValidator {
            try {
              Jval.read(it)
              return@setValidator true
            } catch (err: Throwable) {
              fie.setText(value.toString())
              return@setValidator false
            }
          }
        }
      }
    }
  }

  fun settingsF(): Table.() -> Unit = {
    table {
      val obj = Jval.read(LauncherBridge.getAllSettings())
      it.buildSettings(obj.asObject()) {
        LauncherBridge.setAllSettings(obj.toString(Jval.Jformat.formatted))
      }
    }
  }

  fun infoF(): Table.() -> Unit = {
    table {
          it.apply {
            buildInfo("app", LauncherBridge.appInfo())
            buildInfo("java", LauncherBridge.javaInfo())
          }
        }
        .grow()
  }

  val properties = ObjectMap<String, String>()

  fun Table.buildInfo(name: String, info: String) {
    table {
          it.apply {
            properties.clear()
            PropertiesUtils.load(properties, StringReader(info))
            table().width(8f).grow()
            add(Core.bundle.get("oxygenl.info.$name", name)).left()
            table().grow().row()
            table().height(8f).grow().row()
            properties.each { key, value ->
              add(Core.bundle.get("oxygenl.info.$key", key)).uniformY()
              add(if (value.length <= 20) value else value.substring(0, 20) + "...")
                  .uniformY()
                  .marginLeft(8f)
                  .left()
              table().uniformY().grow()
              row()
            }
          }
        }
        .marginLeft(16f)
        .marginRight(16f)
        .row()
  }

  fun resetPos(element: Element, ori: Vec2) {
    val pos = ori.sub(element.localToAscendantCoordinates(floatTable, Tmp.v2.set(0f, 0f)))
    floatTable.apply {
      setPosition(
          Mathf.clamp(pos.x, getPrefWidth() / 2f, parent.getWidth() - getPrefWidth() / 2f),
          Mathf.clamp(pos.y, getPrefHeight() / 2f, parent.getHeight() - getPrefHeight() / 2f),
      )
    }
  }

  fun submenu(id: Int, icon: Drawable, table: Table, rebuild: Table.() -> Unit) {
    val button = ImageButton(icon, Styles.cleari)
    button.changed {
      val pos = button.localToStageCoordinates(Tmp.v1.set(0f, 0f))
      content.reset()
      if (current == id) {
        current = -1
        content.visible = false
        floatTable.layout()
        resetPos(button, pos)
        return@changed
      }
      current = id
      content.visible = true
      rebuild(content)
      floatTable.layout()
      resetPos(button, pos)
    }
    table.add(button).size(bSize).row()
  }
}
