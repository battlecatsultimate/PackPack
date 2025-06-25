package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlin.Pair;
import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class AssetManager {
    public static AssetManager fromJson(JsonArray arr) {
        AssetManager manager = new AssetManager();

        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);

            if (!(e instanceof JsonObject obj)) {
                continue;
            }

            String key = obj.get("key").getAsString();

            JsonObject v = obj.getAsJsonObject("val");

            long id = v.get("id").getAsLong();
            String link = v.get("link").getAsString();

            manager.assetCache.put(key, new Pair<>(id, link));
        }

        return manager;
    }

    private final Map<String, Pair<Long, String>> assetCache = new HashMap<>();

    private MessageChannel channel = null;

    public void initialize(ShardLoader loader) {
        if (channel != null) {
            return;
        }

        if (loader.supportServer == null)
            return;

        TextChannel ch = loader.supportServer.getTextChannelById(StaticStore.ASSETARCHIVE);

        if (ch == null) {
            StaticStore.logger.uploadLog("W/AssetManager::initialize - Failed to get asset archive channel information from shard manager");

            return;
        }

        channel = ch;
    }

    @Nullable
    public String getAsset(@Nonnull String id) {
        return assetCache.get(id).getSecond();
    }

    public void removeAsset(@Nonnull String id) {
        if (channel == null) {
            throw new IllegalStateException("E/AssetManager::removeAsset - Manager hasn't been initialized yet");
        }

        Pair<Long, String> pair = assetCache.get(id);

        if (pair == null)
            return;

        if (pair.getFirst() != null) {
            long messageID = pair.getFirst();

            channel.deleteMessageById(messageID).queue();
        }

        assetCache.remove(id);
    }

    public void removeAssetRegex(@Nonnull String regex) {
        if (channel == null) {
            throw new IllegalStateException("E/AssetManager::removeAssetRegex - Manager hasn't been initialized yet");
        }

        List<String> keyList = new ArrayList<>();

        for (String key : assetCache.keySet()) {
            if (key.matches(regex)) {
                keyList.add(key);
            }
        }

        for (int i = 0; i < keyList.size(); i++) {
            Pair<Long, String> pair = assetCache.get(keyList.get(i));

            if (pair == null)
                continue;

            if (pair.getFirst() != null) {
                long id = pair.getFirst();

                channel.deleteMessageById(id).queue();
            }

            assetCache.remove(keyList.get(i));
        }
    }

    @Nullable
    public String uploadIf(@Nonnull String id, @Nonnull File file) {
        Pair<Long, String> pair = assetCache.get(id);

        if (pair == null || pair.getFirst() == null || pair.getSecond() == null) {
            removeAsset(id);

            return uploadAsset(id, file);
        } else {
            return pair.getSecond();
        }
    }

    @Nullable
    private String uploadAsset(@Nonnull String id, @Nonnull File file) {
        if (channel == null) {
            throw new IllegalStateException("E/AssetManager::uploadAsset - Manager hasn't been initialized yet");
        }

        Pair<Long, String> pair = assetCache.get(id);

        if (pair != null) {
            throw new IllegalStateException("E/AssetManager::uploadAsset - Tried to assign existing asset id");
        }

        try {
            AtomicReference<String> l = new AtomicReference<>();
            AtomicReference<Long> i = new AtomicReference<>();

            CountDownLatch countdown = new CountDownLatch(1);

            channel.sendMessage(id)
                    .addFiles(FileUpload.fromData(file))
                    .queue(msg -> {
                        List<Message.Attachment> attachments = msg.getAttachments();

                        if (attachments.isEmpty()) {
                            StaticStore.logger.uploadLog("W/AssetManager::uploadAsset - Failed to find asset from uploaded message");

                            l.set(null);
                        } else {
                            l.set(attachments.getFirst().getUrl());
                            i.set(msg.getIdLong());
                        }

                        countdown.countDown();
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/AssetManager::uploadAsset - Error occurred while trying to upload asset");

                        countdown.countDown();
                    });

            countdown.await();

            String link = l.get();

            if (link != null) {
                assetCache.put(id, new Pair<>(i.get(), l.get()));
            }

            return link;
        } catch (InterruptedException e) {
            StaticStore.logger.uploadErrorLog(e, "E/AssetManager::uploadAsset - Failed to perform waiting");

            return null;
        }
    }

    public JsonArray toJson() {
        JsonArray arr = new JsonArray();

        for (Map.Entry<String, Pair<Long, String>> entry : assetCache.entrySet()) {
            String key = entry.getKey();
            Pair<Long, String> pair = entry.getValue();

            JsonObject pairObject = new JsonObject();

            pairObject.addProperty("id", pair.getFirst());
            pairObject.addProperty("link", pair.getSecond());

            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.add("val", pairObject);

            arr.add(obj);
        }

        return arr;
    }
}
