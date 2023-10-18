package mandarin.packpack.supporter

import common.io.Backup
import common.io.PackLoader
import common.pack.Context
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Exception
import java.util.function.Consumer

class LwjglContext : Context {
    companion object {
        private val animationFileFormat = arrayOf(".imgcut", ".mamodel", ".maanim")
    }

    override fun confirmDelete(): Boolean {
        return true
    }

    override fun confirmDelete(f: File?): Boolean {
        return true
    }

    override fun getAssetFile(string: String): File {
        return File("./data/assets/$string")
    }

    override fun getAuxFile(string: String): File {
        return File(string)
    }

    override fun getLangFile(file: String): InputStream {
        return FileInputStream(File("./data/lang/$file"))
    }

    override fun getUserFile(string: String): File {
        return File("./user/$string")
    }

    override fun getWorkspaceFile(relativePath: String): File {
        return File("./workspace/$relativePath")
    }

    override fun getBackupFile(string: String): File {
        return File("./backups/$string")
    }

    override fun getBCUFolder(): File {
        return File("./")
    }

    override fun getAuthor(): String {
        return ""
    }

    override fun initProfile() {

    }

    override fun noticeErr(e: Exception, t: Context.ErrType, str: String) {
        printErr(t, str)
        e.printStackTrace(if (t == Context.ErrType.INFO) System.out else System.err)
    }

    override fun preload(desc: PackLoader.ZipDesc.FileDesc?): Boolean {
        return if (toString().endsWith("png"))
            false
        else
            !animationFileFormat.any { format -> desc?.path?.endsWith(format) ?: false }
    }

    override fun printErr(t: Context.ErrType, str: String) {
        (if (t == Context.ErrType.INFO) System.out else System.err).println(str)
    }

    override fun loadProg(str: String) {

    }

    override fun restore(b: Backup?, prog: Consumer<Double>?): Boolean {
        return false
    }
}