@file:Suppress("SameParameterValue")

import com.google.gson.JsonArray
import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.server.data.BackupHolder


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