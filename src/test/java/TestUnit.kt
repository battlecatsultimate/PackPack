@file:Suppress("SameParameterValue")

import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.JsonArray
import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.server.data.BackupHolder
import java.io.File
import java.io.FileInputStream


@Suppress("unused")
class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val backupHolder = BackupHolder.fromJson(JsonArray())

            backupHolder.uploadBackup(Logger.BotInstance.PACK_PACK)
        }

        fun upload() {
            val credential = GoogleCredentials.fromStream(FileInputStream(File("./data/serviceKey.json"))).createScoped(DriveScopes.DRIVE)

            val request = HttpCredentialsAdapter(credential)
            val transport = NetHttpTransport()
            val factory = GsonFactory.getDefaultInstance()

            val service = Drive.Builder(transport, factory, request).setApplicationName("PackPack").build()

            val target = com.google.api.services.drive.model.File()
            val content = FileContent("image/png", File("./data/serverinfo.json"))

            target.name = "test-serverinfo.json"
            target.mimeType = content.type
            target.parents = listOf("1RR4eUCrqkBBV6TkNADVRzTn-68VLxm9Y")

            val result = service.files().create(target, content).execute()

            println(result.id + ", " + result.driveId)
        }
    }
}