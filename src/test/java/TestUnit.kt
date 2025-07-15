@file:Suppress("SameParameterValue")

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.sharing.RequestedLinkAccessLevel
import com.dropbox.core.v2.sharing.SharedLinkSettings
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
            val token = BufferedReader(FileReader("./data/dropboxToken.txt")).use { r -> r.readLines().joinToString { l -> "\n$l" } }.trim()

            println(token)

            val config = DbxRequestConfig.newBuilder("PackPack Backup").build()
            val client = DbxClientV2(config, token)

            val format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
            format.timeZone = UTC
            val unixTime = Instant.now(Clock.systemUTC()).toEpochMilli()
            val date = format.format(Date(unixTime))

            val fileName = "/PackPack Backup/$date - serverinfo.json"

            val uploader = client.files().upload(fileName)

            val inputStream = FileInputStream("./data/serverinfo.json")

            val result = uploader.uploadAndFinish(inputStream)

            inputStream.close()
            uploader.close()

            println(result.name + " : " + result.id + " -> " + result.isDownloadable.toString())

            val shareConfig = SharedLinkSettings.newBuilder().withAccess(RequestedLinkAccessLevel.VIEWER).withAllowDownload(true).build()

            val sharing = client.sharing().createSharedLinkWithSettings(fileName, shareConfig)

            println(sharing.url)

            val folders = client.files().listFolder("/PackPack Backup")

            println(folders.entries.map { f -> f.name })

            val files = client.files().searchV2("/PackPack Backup").matches

            println(files.map { f -> f.metadata.metadataValue.name })
        }

        fun upload() {

        }
    }
}