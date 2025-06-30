package mandarin.packpack.supporter.server.data;

import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.StaticStore;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class BackupHolder {
    private static final String CARD_DEALER_BACKUP_FOLDER_ID = "1FlXSURFasPuI0NuGqZ8hmneLQGBXezii";
    private static final String PACKPACK_BACKUP_FOLDER_ID = "1RR4eUCrqkBBV6TkNADVRzTn-68VLxm9Y";
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
    private final Drive service;

    private BackupHolder() {
        File serviceKey = new File("./data/serviceKey.json");

        if (!serviceKey.exists()) {
            StaticStore.logger.uploadLog("W/BackupHolder::init - Failed to find key json file for google service account");

            service = null;

            return;
        }

        Drive tempService;

        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(serviceKey)).createScoped(DriveScopes.DRIVE);
            HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(credentials);
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory factory = GsonFactory.getDefaultInstance();

            tempService = new Drive.Builder(transport, factory, adapter).setApplicationName("PackPack").build();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/BackupHolder::init - Failed to initialize google drive service");

            tempService = null;
        }

        service = tempService;
    }

    public String uploadBackup(Logger.BotInstance instance) throws Exception {
        if (service == null) {
            StaticStore.logger.uploadLog("W/BackupHolder::uploadBackup - Google drive service hasn't been initialized");

            return "";
        }

        String fileName;
        String parentFolder;

        switch (instance) {
            case PACK_PACK -> {
                fileName = "serverinfo.json";
                parentFolder = PACKPACK_BACKUP_FOLDER_ID;
            }
            case CARD_DEALER -> {
                fileName = "cardSave.json";
                parentFolder = CARD_DEALER_BACKUP_FOLDER_ID;
            }
            default -> throw new IllegalStateException("E/BackupHolder::uploadBackup - Invalid bot instance value");
        }

        com.google.api.services.drive.model.File target = new com.google.api.services.drive.model.File();
        FileContent content = new FileContent("application/json", new File("./data/" + fileName));

        long unixTime = Instant.now(Clock.systemUTC()).toEpochMilli();
        String date = format.format(new Date(unixTime));

        target.setName(date + " - " + fileName);
        target.setMimeType(content.getType());
        target.setParents(List.of(parentFolder));

        com.google.api.services.drive.model.File result = service.files().create(target, content).execute();

        backupList.put(unixTime, result.getId());

        if (backupList.size() > MAX_BACKUP) {
            List<Long> timestamps = new ArrayList<>(backupList.keySet());

            timestamps.sort(null);
            Collections.reverse(timestamps);

            for (int i = MAX_BACKUP; i < timestamps.size(); i++) {
                String fileID = backupList.get(timestamps.get(i));

                if (fileID == null) {
                    backupList.remove(timestamps.get(i));

                    continue;
                }

                service.files().delete(fileID).execute();
                backupList.remove(timestamps.get(i));
            }
        }

        StaticStore.saveServerInfo();

        return "https://drive.google.com/file/d/" + result.getId() + "/view";
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
