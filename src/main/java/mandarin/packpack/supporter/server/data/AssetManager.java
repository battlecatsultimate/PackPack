package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import common.CommonStatic;
import common.pack.UserProfile;
import common.system.P;
import common.system.fake.FakeImage;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;
import kotlin.Pair;
import kotlin.Triple;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.RawPointGetter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class AssetManager {
    public static final int ICON_SIZE = 512;

    public static AssetManager fromJson(JsonElement element) {
        if (element instanceof JsonArray arr) {
            return fromJson(arr);
        } else if (element instanceof JsonObject obj) {
            return fromJson(obj);
        } else {
            throw new IllegalStateException("E/AssetManager::fromJson - Invalid asset manager format found : " + element.getClass());
        }
    }

    private static AssetManager fromJson(JsonObject obj) {
        AssetManager manager = new AssetManager();

        if (obj.has("assetCache")) {
            for (JsonElement e : obj.getAsJsonArray("assetCache")) {
                if (!(e instanceof JsonObject o)) {
                    continue;
                }

                String key = o.get("key").getAsString();

                JsonObject v = o.getAsJsonObject("val");

                long id = v.get("id").getAsLong();
                String link = v.get("link").getAsString();

                manager.assetCache.put(key, new Pair<>(id, link));
            }
        }

        if (obj.has("unitIcons")) {
            for (JsonElement e : obj.getAsJsonArray("unitIcons")) {
                if (!(e instanceof JsonObject o)) {
                    continue;
                }

                JsonObject key = o.getAsJsonObject("key");

                int formID = key.get("form").getAsInt();
                int unitID = key.get("unit").getAsInt();

                Unit unit = UserProfile.getBCData().units.get(unitID);

                if (unit == null)
                    continue;

                if (formID < 0 || formID >= unit.forms.length)
                    continue;

                Form form = unit.forms[formID];

                JsonObject v = o.getAsJsonObject("val");

                long id = v.get("id").getAsLong();
                String code = v.get("code").getAsString();
                String link = v.get("link").getAsString();

                manager.unitIcons.put(form, new Triple<>(id, code, link));
            }
        }

        if (obj.has("enemyIcons")) {
            for (JsonElement e : obj.getAsJsonArray("enemyIcons")) {
                if (!(e instanceof JsonObject o)) {
                    continue;
                }

                int enemyID = o.get("key").getAsInt();

                Enemy enemy = UserProfile.getBCData().enemies.get(enemyID);

                if (enemy == null)
                    continue;

                JsonObject v = o.getAsJsonObject("val");

                long id = v.get("id").getAsLong();
                String code = v.get("code").getAsString();
                String link = v.get("link").getAsString();

                manager.enemyIcons.put(enemy, new Triple<>(id, code, link));
            }
        }

        for (Map.Entry<String, Pair<Long, String>> entry : manager.assetCache.entrySet()) {
            if (entry.getKey().matches("UNIT-MODEL.+")) {
                String[] segments = entry.getKey().split("-");

                int unitID = StaticStore.safeParseInt(segments[3]);
                int formID = StaticStore.safeParseInt(segments[4]);

                Unit u = UserProfile.getBCData().units.get(unitID);

                if (u == null)
                    continue;

                if (formID < 0 || formID >= u.forms.length)
                    continue;

                Form f = u.forms[formID];

                manager.unitIcons.put(f, new Triple<>(entry.getValue().getFirst(), entry.getKey(), entry.getValue().getSecond()));
            } else if (entry.getKey().matches("ENEMY-MODEL.+")) {
                String[] segments = entry.getKey().split("-");

                int enemyID = StaticStore.safeParseInt(segments[3]);

                Enemy e = UserProfile.getBCData().enemies.get(enemyID);

                if (e == null || e.id == null)
                    continue;

                manager.enemyIcons.put(e, new Triple<>(entry.getValue().getFirst(), entry.getKey(), entry.getValue().getSecond()));
            }
        }

        manager.assetCache.entrySet().removeIf(e -> e.getKey().matches("UNIT-MODEL.+") || e.getKey().matches("ENEMY-MODEL.+"));

        return manager;
    }

    private static AssetManager fromJson(JsonArray arr) {
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

        for (Map.Entry<String, Pair<Long, String>> entry : manager.assetCache.entrySet()) {
            if (entry.getKey().matches("UNIT-MODEL.+")) {
                String[] segments = entry.getKey().split("-");

                int unitID = StaticStore.safeParseInt(segments[3]);
                int formID = StaticStore.safeParseInt(segments[4]);

                Unit u = UserProfile.getBCData().units.get(unitID);

                if (u == null)
                    continue;

                if (formID < 0 || formID >= u.forms.length)
                    continue;

                Form f = u.forms[formID];

                manager.unitIcons.put(f, new Triple<>(entry.getValue().getFirst(), entry.getKey(), entry.getValue().getSecond()));
            } else if (entry.getKey().matches("ENEMY-MODEL.+")) {
                String[] segments = entry.getKey().split("-");

                int enemyID = StaticStore.safeParseInt(segments[3]);

                Enemy e = UserProfile.getBCData().enemies.get(enemyID);

                if (e == null || e.id == null)
                    continue;

                manager.enemyIcons.put(e, new Triple<>(entry.getValue().getFirst(), entry.getKey(), entry.getValue().getSecond()));
            }
        }

        manager.assetCache.entrySet().removeIf(e -> e.getKey().matches("UNIT-MODEL.+") || e.getKey().matches("ENEMY-MODEL.+"));

        return manager;
    }

    private final Map<String, Pair<Long, String>> assetCache = new HashMap<>();
    private final Map<Form, Triple<Long, String, String>> unitIcons = new HashMap<>();
    private final Map<Enemy, Triple<Long, String, String>> enemyIcons = new HashMap<>();

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
        if (!assetCache.containsKey(id))
            return null;

        return assetCache.get(id).getSecond();
    }

    @Nullable
    public String getUnitIcon(@Nonnull Form f) {
        Triple<Long, String, String> triple = unitIcons.get(f);

        if (triple == null) {
            try {
                Unit u = f.unit;

                if (u.id == null)
                    return null;

                File folder = new File("./temp");

                if (!folder.exists() && !folder.mkdirs()) {
                    StaticStore.logger.uploadLog("W/AssetManager::getUnitIcon - Failed to create folder : " + folder.getAbsolutePath());

                    return null;
                }

                CommonStatic.getConfig().ref = false;

                File file = StaticStore.generateTempFile(folder, "icon", "png", false);

                if (file == null)
                    return null;

                f.anim.load();

                long hash = StaticStore.getHashOfVariables(f.anim.imgcut, new ArrayList<>()) +
                        StaticStore.getHashOfVariables(f.anim.mamodel, new ArrayList<>()) +
                        StaticStore.getHashOfVariables(f.anim.anims, new ArrayList<>());

                String hashCode = Long.toHexString(hash).toUpperCase(Locale.ENGLISH).replaceAll("^F+", "");

                if (hashCode.length() < 5) {
                    hashCode = "0".repeat(5 - hashCode.length()) + hashCode;
                } else {
                    hashCode = hashCode.substring(0, 5);
                }

                String id = StaticStore.UNIT_MODEL_ICON.formatted(Data.trio(u.id.id), Data.trio(f.fid), hashCode);

                if (StaticStore.assetManager.getAsset(id) != null) {
                    f.anim.unload();

                    return null;
                }

                EAnimD<?> anim = f.getEAnim(AnimU.UType.WALK);

                anim.setTime(0);

                Rectangle rect = new Rectangle();

                for(int i = 0; i < anim.getOrder().length; i++) {
                    if(anim.anim().parts((int) anim.getOrder()[i].getValRaw(2)) == null || anim.getOrder()[i].getValRaw(1) == -1)
                        continue;

                    FakeImage fi = anim.anim().parts((int) anim.getOrder()[i].getValRaw(2));

                    if(fi.getHeight() == 1 && fi.getWidth() == 1)
                        continue;

                    RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

                    getter.apply(anim.getOrder()[i], 1.0f, false);

                    int[][] result = getter.getRect();

                    if(Math.abs(result[1][0]-result[0][0]) >= 1000 || Math.abs(result[1][1] - result[2][1]) >= 1000)
                        continue;

                    int oldX = rect.x;
                    int oldY = rect.y;

                    rect.x = Math.min(minAmong(result[0][0], result[1][0], result[2][0], result[3][0]), rect.x);
                    rect.y = Math.min(minAmong(result[0][1], result[1][1], result[2][1], result[3][1]), rect.y);

                    if(oldX != rect.x) {
                        rect.width += oldX - rect.x;
                    }

                    if(oldY != rect.y) {
                        rect.height += oldY - rect.y;
                    }

                    rect.width = Math.round(Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width));
                    rect.height = Math.round(Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height));
                }

                if(rect.width == 0)
                    rect.width = 2;

                if(rect.height == 0)
                    rect.height = 2;

                if(rect.width % 2 == 1)
                    rect.width++;

                if(rect.height % 2 == 1)
                    rect.height++;

                CountDownLatch renderWaiter = new CountDownLatch(1);

                StaticStore.renderManager.createRenderer(AssetManager.ICON_SIZE, AssetManager.ICON_SIZE, folder, r -> {
                    r.queue(g -> {
                        float scale = AssetManager.ICON_SIZE * 1.0f / Math.max(rect.width, rect.height);

                        g.translate((AssetManager.ICON_SIZE - rect.width * scale) / 2.0f, (AssetManager.ICON_SIZE - rect.height * scale) / 2.0f);
                        g.scale(scale, scale);

                        anim.draw(g, new P(-rect.x, -rect.y), 1.0f);

                        return kotlin.Unit.INSTANCE;
                    });

                    return kotlin.Unit.INSTANCE;
                }, unused -> file, () -> {
                    renderWaiter.countDown();

                    return kotlin.Unit.INSTANCE;
                });

                renderWaiter.await();

                AtomicReference<String> l = new AtomicReference<>();
                AtomicReference<Long> i = new AtomicReference<>();

                CountDownLatch uploadWaiter = new CountDownLatch(1);

                channel.sendMessage(id)
                        .addFiles(FileUpload.fromData(file))
                        .queue(msg -> {
                            List<Message.Attachment> attachments = msg.getAttachments();

                            if (attachments.isEmpty()) {
                                StaticStore.logger.uploadLog("W/AssetManager::getUnitIcon - Failed to find asset from uploaded message");

                                l.set(null);
                            } else {
                                l.set(attachments.getFirst().getUrl());
                                i.set(msg.getIdLong());
                            }

                            uploadWaiter.countDown();
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/AssetManager::getUnitIcon - Error occurred while trying to upload asset");

                            uploadWaiter.countDown();
                        });

                uploadWaiter.await();

                StaticStore.deleteFile(file, true);

                f.anim.unload();

                unitIcons.put(f, new Triple<>(i.get(), id, l.get()));

                return l.get();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/AssetManager::getUnitIcon - Failed to generate unit icon cache");

                return null;
            }
        } else {
            return triple.getThird();
        }
    }

    public boolean uploadFormIconManual(Form f, String id, File file) {
        Triple<Long, String, String> triple = unitIcons.get(f);

        if (triple != null) {
            removeUnitIcon(f);
        }

        try {
            CountDownLatch countdown = new CountDownLatch(1);

            AtomicReference<String> l = new AtomicReference<>();
            AtomicReference<Long> i = new AtomicReference<>();

            channel.sendMessage(id)
                    .addFiles(FileUpload.fromData(file))
                    .queue(msg -> {
                        List<Message.Attachment> attachments = msg.getAttachments();

                        if (attachments.isEmpty()) {
                            StaticStore.logger.uploadLog("W/AssetManager::uploadFormIconManual - Failed to find asset from uploaded message");

                            l.set(null);
                        } else {
                            l.set(attachments.getFirst().getUrl());
                            i.set(msg.getIdLong());
                        }

                        countdown.countDown();
                    }, err -> {
                        StaticStore.logger.uploadErrorLog(err, "E/AssetManager::uploadFormIconManual - Error occurred while trying to upload asset");

                        countdown.countDown();
                    });

            countdown.await();

            unitIcons.put(f, new Triple<>(i.get(), id, l.get()));

            return true;
        } catch (Exception err) {
            StaticStore.logger.uploadErrorLog(err, "E/AssetManager::uploadFormIconManual - Failed to assign file as enemy icon");

            return false;
        }
    }

    @Nullable
    public String getUnitIconID(@Nonnull Form f) {
        Triple<Long, String, String> triple = unitIcons.get(f);

        if (triple == null)
            return null;

        return triple.getSecond();
    }

    @Nullable
    public String getEnemyIcon(@Nonnull Enemy e) {
        Triple<Long, String, String> triple = enemyIcons.get(e);

        if (triple == null) {
            try {
                if (e.id == null)
                    return null;

                File folder = new File("./temp");

                if (!folder.exists() && !folder.mkdirs()) {
                    StaticStore.logger.uploadLog("W/AssetManager::getEnemyIcon - Failed to create folder : " + folder.getAbsolutePath());

                    return null;
                }

                CommonStatic.getConfig().ref = false;

                File file = StaticStore.generateTempFile(folder, "icon", "png", false);

                if (file == null)
                    return null;

                e.anim.load();

                long hash = StaticStore.getHashOfVariables(e.anim.imgcut, new ArrayList<>()) +
                        StaticStore.getHashOfVariables(e.anim.mamodel, new ArrayList<>()) +
                        StaticStore.getHashOfVariables(e.anim.anims, new ArrayList<>());

                String hashCode = Long.toHexString(hash).toUpperCase(Locale.ENGLISH).replaceAll("^F+", "");

                if (hashCode.length() < 5) {
                    hashCode = "0".repeat(5 - hashCode.length()) + hashCode;
                } else {
                    hashCode = hashCode.substring(0, 5);
                }

                String id = StaticStore.ENEMY_MODEL_ICON.formatted(Data.trio(e.id.id), hashCode);

                EAnimD<?> anim = e.getEAnim(AnimU.UType.WALK);

                anim.setTime(0);

                Rectangle rect = new Rectangle();

                for(int i = 0; i < anim.getOrder().length; i++) {
                    if(anim.anim().parts((int) anim.getOrder()[i].getValRaw(2)) == null || anim.getOrder()[i].getValRaw(1) == -1)
                        continue;

                    FakeImage fi = anim.anim().parts((int) anim.getOrder()[i].getValRaw(2));

                    if(fi.getHeight() == 1 && fi.getWidth() == 1)
                        continue;

                    RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

                    getter.apply(anim.getOrder()[i], 1.0f, false);

                    int[][] result = getter.getRect();

                    if(Math.abs(result[1][0]-result[0][0]) >= 1000 || Math.abs(result[1][1] - result[2][1]) >= 1000)
                        continue;

                    int oldX = rect.x;
                    int oldY = rect.y;

                    rect.x = Math.min(minAmong(result[0][0], result[1][0], result[2][0], result[3][0]), rect.x);
                    rect.y = Math.min(minAmong(result[0][1], result[1][1], result[2][1], result[3][1]), rect.y);

                    if(oldX != rect.x) {
                        rect.width += oldX - rect.x;
                    }

                    if(oldY != rect.y) {
                        rect.height += oldY - rect.y;
                    }

                    rect.width = Math.round(Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width));
                    rect.height = Math.round(Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height));
                }

                if(rect.width == 0)
                    rect.width = 2;

                if(rect.height == 0)
                    rect.height = 2;

                if(rect.width % 2 == 1)
                    rect.width++;

                if(rect.height % 2 == 1)
                    rect.height++;

                CountDownLatch renderWaiter = new CountDownLatch(2);

                StaticStore.renderManager.createRenderer(AssetManager.ICON_SIZE, AssetManager.ICON_SIZE, folder, r -> {
                    r.queue(g -> {
                        float scale = AssetManager.ICON_SIZE * 1.0f / Math.max(rect.width, rect.height);

                        g.translate((AssetManager.ICON_SIZE - rect.width * scale) / 2.0f, (AssetManager.ICON_SIZE - rect.height * scale) / 2.0f);
                        g.scale(scale, scale);

                        anim.draw(g, new P(-rect.x, -rect.y), 1.0f);

                        return kotlin.Unit.INSTANCE;
                    });

                    return kotlin.Unit.INSTANCE;
                }, unused -> file, () -> {
                    renderWaiter.countDown();

                    return kotlin.Unit.INSTANCE;
                });

                renderWaiter.await();

                AtomicReference<String> l = new AtomicReference<>();
                AtomicReference<Long> i = new AtomicReference<>();

                CountDownLatch uploadWaiter = new CountDownLatch(1);

                channel.sendMessage(id)
                        .addFiles(FileUpload.fromData(file))
                        .queue(msg -> {
                            List<Message.Attachment> attachments = msg.getAttachments();

                            if (attachments.isEmpty()) {
                                StaticStore.logger.uploadLog("W/AssetManager::getEnemyIcon - Failed to find asset from uploaded message");

                                l.set(null);
                            } else {
                                l.set(attachments.getFirst().getUrl());
                                i.set(msg.getIdLong());
                            }

                            uploadWaiter.countDown();
                        }, err -> {
                            StaticStore.logger.uploadErrorLog(err, "E/AssetManager::getEnemyIcon - Error occurred while trying to upload asset");

                            uploadWaiter.countDown();
                        });

                uploadWaiter.await();

                StaticStore.deleteFile(file, true);

                e.anim.unload();

                enemyIcons.put(e, new Triple<>(i.get(), id, l.get()));

                return l.get();
            } catch (Exception err) {
                StaticStore.logger.uploadErrorLog(err, "E/AssetManager::getEnemyIcon - Failed to generate unit icon cache");

                return null;
            }
        } else {
            return triple.getThird();
        }
    }

    public boolean uploadEnemyIconManual(Enemy e, String id, File file) {
        Triple<Long, String, String> triple = enemyIcons.get(e);

        if (triple != null) {
            removeEnemyIcon(e);
        }

        try {
            CountDownLatch countdown = new CountDownLatch(1);

            AtomicReference<String> l = new AtomicReference<>();
            AtomicReference<Long> i = new AtomicReference<>();

            channel.sendMessage(id)
                    .addFiles(FileUpload.fromData(file))
                    .queue(msg -> {
                        List<Message.Attachment> attachments = msg.getAttachments();

                        if (attachments.isEmpty()) {
                            StaticStore.logger.uploadLog("W/AssetManager::uploadEnemyIconManual - Failed to find asset from uploaded message");

                            l.set(null);
                        } else {
                            l.set(attachments.getFirst().getUrl());
                            i.set(msg.getIdLong());
                        }

                        countdown.countDown();
                    }, err -> {
                        StaticStore.logger.uploadErrorLog(err, "E/AssetManager::uploadEnemyIconManual - Error occurred while trying to upload asset");

                        countdown.countDown();
                    });

            countdown.await();

            enemyIcons.put(e, new Triple<>(i.get(), id, l.get()));

            return true;
        } catch (Exception err) {
            StaticStore.logger.uploadErrorLog(err, "E/AssetManager::uploadEnemyIconManual - Failed to assign file as enemy icon");

            return false;
        }
    }

    @Nullable
    public String getEnemyIconID(@Nonnull Enemy e) {
        Triple<Long, String, String> triple = enemyIcons.get(e);

        if (triple == null)
            return null;

        return triple.getSecond();
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

    public void removeUnitIcon(@Nonnull Form f) {
        Triple<Long, String, String> triple = unitIcons.get(f);

        if (triple == null)
            return;

        if (triple.getFirst() != null) {
            long id = triple.getFirst();

            channel.deleteMessageById(id).queue();
        }

        unitIcons.remove(f);
    }

    public void removeEnemyIcon(@Nonnull Enemy e) {
        Triple<Long, String, String> triple = enemyIcons.get(e);

        if (triple == null)
            return;

        if (triple.getFirst() != null) {
            long id = triple.getFirst();

            channel.deleteMessageById(id).queue();
        }

        enemyIcons.remove(e);
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

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        JsonArray cacheArray = new JsonArray();

        for (Map.Entry<String, Pair<Long, String>> entry : assetCache.entrySet().stream().sorted((e1, e2) -> {
            if (e1 == null && e2 == null)
                return 0;

            if (e1 == null)
                return 1;

            if (e2 == null)
                return -1;

            return e1.getKey().compareTo(e2.getKey());
        }).toList()) {
            String key = entry.getKey();
            Pair<Long, String> pair = entry.getValue();

            JsonObject pairObject = new JsonObject();

            pairObject.addProperty("id", pair.getFirst());
            pairObject.addProperty("link", pair.getSecond());

            JsonObject v = new JsonObject();

            v.addProperty("key", key);
            v.add("val", pairObject);

            cacheArray.add(v);
        }

        obj.add("assetCache", cacheArray);

        JsonArray unitIconArray = new JsonArray();

        for (Map.Entry<Form, Triple<Long, String, String>> entry : unitIcons.entrySet().stream().sorted((e1, e2) -> {
            if (e1 == null && e2 == null)
                return 0;

            if (e1 == null)
                return 1;

            if (e2 == null)
                return -1;

            Form f1 = e1.getKey();
            Form f2 = e2.getKey();

            if (f1.unit.id == null && f2.unit.id == null)
                return 0;

            if (f1.unit.id == null)
                return 1;

            if (f2.unit.id == null)
                return -1;

            if (f1.unit.id.id != f2.unit.id.id) {
                return Integer.compare(f1.unit.id.id, f2.unit.id.id);
            } else {
                return Integer.compare(f1.fid, f2.fid);
            }
        }).toList()) {
            Form f = entry.getKey();

            if (f == null)
                continue;

            Unit u = f.unit;

            if (u.id == null)
                continue;

            JsonObject key = new JsonObject();

            key.addProperty("unit", u.id.id);
            key.addProperty("form", f.fid);

            JsonObject value = new JsonObject();

            value.addProperty("id", entry.getValue().getFirst());
            value.addProperty("code", entry.getValue().getSecond());
            value.addProperty("link", entry.getValue().getThird());

            JsonObject v = new JsonObject();

            v.add("key", key);
            v.add("val", value);

            unitIconArray.add(v);
        }

        obj.add("unitIcons", unitIconArray);

        JsonArray enemyIconArray = new JsonArray();

        for (Map.Entry<Enemy, Triple<Long, String, String>> entry : enemyIcons.entrySet().stream().sorted((e1, e2) -> {
            if (e1 == null && e2 == null)
                return 0;

            if (e1 == null)
                return 1;

            if (e2 == null)
                return -1;

            Enemy en1 = e1.getKey();
            Enemy en2 = e2.getKey();

            if (en1.id == null && en2.id == null)
                return 0;

            if (en1.id == null)
                return 1;

            if (en2.id == null)
                return -1;

            return Integer.compare(en1.id.id, en2.id.id);
        }).toList()) {
            Enemy e = entry.getKey();

            if (e == null || e.id == null)
                continue;

            int key = e.id.id;

            JsonObject value = new JsonObject();

            value.addProperty("id", entry.getValue().getFirst());
            value.addProperty("code", entry.getValue().getSecond());
            value.addProperty("link", entry.getValue().getThird());

            JsonObject v = new JsonObject();

            v.addProperty("key", key);
            v.add("val", value);

            enemyIconArray.add(v);
        }

        obj.add("enemyIcons", enemyIconArray);

        return obj;
    }

    private float maxAmong(float... values) {
        if(values.length == 1)
            return values[0];
        else if(values.length == 2) {
            return Math.max(values[0], values[1]);
        } else if(values.length >= 3) {
            float val = Math.max(values[0], values[1]);

            for(int i = 2; i < values.length; i++) {
                val = Math.max(values[i], val);
            }

            return val;
        } else {
            return  0;
        }
    }

    private int minAmong(int... values) {
        if(values.length == 1)
            return values[0];
        else if(values.length == 2) {
            return Math.min(values[0], values[1]);
        } else if(values.length >= 3) {
            int val = Math.min(values[0], values[1]);

            for(int i = 2; i < values.length; i++) {
                val = Math.min(values[i], val);
            }

            return val;
        } else {
            return  0;
        }
    }
}
