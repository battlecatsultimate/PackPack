package mandarin.packpack.supporter.server.data;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.sharing.RequestedLinkAccessLevel;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.StaticStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class BackupHolder {
    private static final String CARD_DEALER_BACKUP_FOLDER = "Card Dealer Backup";
    private static final String PACKPACK_BACKUP_FOLDER = "PackPack Backup";
    private static final int MAX_BACKUP = 50;

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    static {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static BackupHolder fromJson(JsonArray arr) {
        BackupHolder holder = new BackupHolder();

        for (JsonElement e : arr) {
            if (!(e instanceof JsonObject obj)) {
                continue;
            }

            long timestamp = obj.get("key").getAsLong();
            String fileID = obj.get("val").getAsString();

            holder.backupList.put(timestamp, fileID);
        }

        return holder;
    }

    public Map<Long, String> backupList = new HashMap<>();
    private final DbxClientV2 client;

    private BackupHolder() {
        File accessToken = new File("./data/dropboxToken.txt");

        if (!accessToken.exists()) {
            StaticStore.logger.uploadLog("W/BackupHolder::init - Failed to find access token for dropbox API");

            client = null;

            return;
        }

        DbxClientV2 tempClient;

        try(BufferedReader reader = new BufferedReader(new FileReader("./data/dropboxToken.txt"))) {
            StringBuilder tokenBuilder = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                tokenBuilder.append(line).append("\n");
            }

            String token = tokenBuilder.toString().trim();

            DbxRequestConfig config = DbxRequestConfig.newBuilder("Discord Bot Backup").build();

            tempClient = new DbxClientV2(config, token);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/BackupHolder::init - Failed to initialize dropbox API service client");

            tempClient = null;
        }

        client = tempClient;
    }

    public String uploadBackup(Logger.BotInstance instance) throws Exception {
        if (client == null) {
            StaticStore.logger.uploadLog("W/BackupHolder::uploadBackup - Dropbox API client isn't initialized");

            return "";
        }

        String fileName;
        String parentFolder;

        switch (instance) {
            case PACK_PACK -> {
                fileName = "serverinfo.json";
                parentFolder = PACKPACK_BACKUP_FOLDER;
            }
            case CARD_DEALER -> {
                fileName = "cardSave.json";
                parentFolder = CARD_DEALER_BACKUP_FOLDER;
            }
            default -> throw new IllegalStateException("E/BackupHolder::uploadBackup - Invalid bot instance value");
        }

        long unixTime = Instant.now(Clock.systemUTC()).toEpochMilli();
        String date = format.format(new Date(unixTime));

        String fullFileName = "/" + parentFolder + "/" + date + " - " + fileName;

        try (
                UploadUploader uploader = client.files().upload(fullFileName);
                FileInputStream stream = new FileInputStream("./data/" + fileName)
        ) {
            FileMetadata result = uploader.uploadAndFinish(stream);

            backupList.put(unixTime, result.getName());
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/BackupHolder::uploadBackup - Failed to upload backup file for instance : " + instance);

            return "";
        }

        if (backupList.size() > MAX_BACKUP) {
            List<Long> timestamps = new ArrayList<>(backupList.keySet());

            timestamps.sort(null);
            Collections.reverse(timestamps);

            for (int i = MAX_BACKUP; i < timestamps.size(); i++) {
                String backupFileName = backupList.get(timestamps.get(i));

                if (backupFileName == null) {
                    backupList.remove(timestamps.get(i));

                    continue;
                }

                client.files().deleteV2("/" + parentFolder + "/" + backupFileName);
                backupList.remove(timestamps.get(i));
            }
        }

        SharedLinkSettings settings = SharedLinkSettings.newBuilder().withAccess(RequestedLinkAccessLevel.VIEWER).withAllowDownload(true).build();
        SharedLinkMetadata sharing = client.sharing().createSharedLinkWithSettings(fullFileName, settings);

        StaticStore.saveServerInfo();

        return sharing.getUrl();
    }

    public JsonArray toJson() {
        JsonArray arr = new JsonArray();

        for (Map.Entry<Long, String> entry : backupList.entrySet()) {
            JsonObject obj = new JsonObject();

            obj.addProperty("key", entry.getKey());
            obj.addProperty("val", entry.getValue());

            arr.add(obj);
        }

        return arr;
    }
}
