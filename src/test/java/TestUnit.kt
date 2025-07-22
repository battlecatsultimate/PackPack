@file:Suppress("SameParameterValue")

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.sharing.RequestedLinkAccessLevel
import com.dropbox.core.v2.sharing.SharedLinkSettings
import com.google.gson.JsonArray
import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.server.data.BackupHolder
import okhttp3.internal.UTC
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileReader
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.util.*


@Suppress("unused")
class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val holder = BackupHolder.fromJson(JsonArray())

            holder.uploadBackup(Logger.BotInstance.PACK_PACK)
        }

        fun upload() {

        }
    }
}