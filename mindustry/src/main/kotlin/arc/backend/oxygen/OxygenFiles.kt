package arc.backend.oxygen

import arc.Files
import arc.Files.FileType
import arc.files.Fi
import arc.util.ArcRuntimeException
import arc.util.OS
import java.io.File

class OxygenFiles : Files {
  companion object {
    val externalPath: String = OS.userHome + File.separator
    val localPath: String = File("").absolutePath + File.separator
  }

  override fun get(fileName: String, type: FileType): Fi {
    return OxygenFi(fileName, type)
  }

  override fun getExternalStoragePath(): String = externalPath

  override fun isExternalStorageAvailable(): Boolean = true

  override fun getLocalStoragePath(): String = localPath

  override fun isLocalStorageAvailable(): Boolean = true

  class OxygenFi : Fi {
    constructor(fileName: String, type: FileType) : super(fileName, type)

    constructor(file: File, type: FileType) : super(file, type)

    override fun child(name: String): Fi {
      if (file.path.isEmpty()) return OxygenFi(File(name), type)
      return OxygenFi(File(file, name), type)
    }

    override fun sibling(name: String): Fi {
      if (file.path.isEmpty()) throw ArcRuntimeException("Cannot get the sibling of the root.")
      return OxygenFi(File(file.parent, name), type)
    }

    override fun file(): File {
      if (type == FileType.external) return File(externalPath, file.path)
      if (type == FileType.local) return File(localPath, file.path)
      return file
    }
  }
}
