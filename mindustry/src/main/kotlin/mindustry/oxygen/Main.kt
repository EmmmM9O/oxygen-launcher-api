package mindustry.oxygen

import arc.*
import arc.Files.*
import arc.backend.oxygen.*
import arc.files.*
import arc.func.*
import arc.struct.*
import arc.util.*
import arc.util.Log.*
import java.io.*
import java.lang.Thread.*
import java.util.*
import mindustry.*
import mindustry.net.*
import mindustry.ui.dialogs.*
import oxygen.api.*
import oxygen.util.*

fun main() {
  Log.level = Log.LogLevel.debug
  try {
    val lau = OxygenLauncher()
    object : OxygenApplication(lau as ApplicationListener, object : OxygenConfig() {}) {
      override fun onRequestPermissionsResult(
          requestCode: Int,
          permissions: Array<String>,
          grantResults: IntArray,
      ) {
        if (requestCode == lau.PERMISSION_REQUEST_CODE) {
          for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) return
          }
          lau.chooser?.let { Core.app.post(it::show) }
          lau.permCallback?.let { Core.app.post(it) }
          lau.permCallback = null
        }
      }
    }
  } catch (error: Throwable) {
    Log.err(error)
    CrashHandler.log(error)
  }
}

class OxygenLauncher : ClientLauncher() {
  val PERMISSION_REQUEST_CODE = 1
  var chooser: FileChooser? = null
  var permCallback: (() -> Unit)? = null

  override fun hide() {
    LauncherBridge.hide()
  }

  override fun shareFile(file: Fi) {}

  override fun showFileChooser(open: Boolean, title: String, extension: String, cons: Cons<Fi>) {
    showFileChooser(open, title, cons, extension)
  }

  override fun showMultiFileChooser(cons: Cons<Fi>, vararg extensions: String) {
    showFileChooser(true, "@open", cons, *extensions)
  }

  fun showFileChooser(
      open: Boolean,
      title: String,
      cons: Cons<Fi>,
      vararg extensions: String,
  ) {
    try {
      val extension = extensions[0]
      val version = LauncherBridge.getVersion()
      if (version >= Build.VERSION_CODES.Q) {
        LauncherBridge.showFileChooser(
            open,
            title,
            StrCons {
              val file = Fi(it)
              cons.get(file)
              Time.run(20f){
                if(file.exists()){
                  if(!open) LauncherBridge.postCacheFile(file.absolutePath())
                  file.delete()
                }
              }
            },
            StrCons2 { str1, str2 -> Vars.ui.showErrorMessage("$str1\n======\n$str2") },
            arrayOf(*extensions),
        )
      } else if (version >= Build.VERSION_CODES.M && !LauncherBridge.haveExternalPermission()) {
        chooser =
            FileChooser(
                title,
                { file -> file.extension().lowercase(Locale.getDefault()) in extensions },
                open,
            ) { file ->
              if (!open) {
                cons.get(file.parent().child(file.nameWithoutExtension() + "." + extension))
              } else {
                cons.get(file)
              }
            }
        LauncherBridge.getExternalPermission(PERMISSION_REQUEST_CODE)
      } else {
        if (open) {
          FileChooser(
                  title,
                  { file -> file.extension().lowercase(Locale.getDefault()) in extensions },
                  true,
                  cons,
              )
              .show()
        } else {
          super.showFileChooser(open, "@open", extension, cons)
        }
      }
    } catch (error: Throwable) {
      Core.app.post { Vars.ui.showException(error) }
    }
  }

  override fun beginForceLandscape() {
    LauncherBridge.beginForceLandscape()
  }

  override fun endForceLandscape() {
    LauncherBridge.endForceLandscape()
  }
}
