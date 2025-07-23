package mandarin.packpack.commands.bot;

import common.CommonStatic;
import common.system.P;
import common.system.fake.FakeImage;
import common.util.Data;
import common.util.anim.EAnimD;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.bc.RawPointGetter;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.AssetManager;
import mandarin.packpack.supporter.server.data.IDHolder;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class ManualEnemyIconCache extends ConstraintCommand {
    public ManualEnemyIconCache(ROLE role, CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        File folder = new File("./temp");

        if (!folder.exists() && !folder.mkdirs()) {
            StaticStore.logger.uploadLog("W/CreateIconCache::doSomething - Failed to create folder : " + folder.getAbsolutePath());

            return;
        }

        String search = filterCommand(loader.getContent());

        if(search.isBlank()) {
            replyToMessageSafely(loader.getChannel(), LangID.getStringByID("enemyImage.fail.noParameter", lang), loader.getMessage(), a -> a);

            return;
        }

        ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(search, lang);

        CommonStatic.getConfig().ref = false;

        if (enemies.size() != 1) {
            replyToMessageSafely(loader.getChannel(), "Please be more accurate for enemy name", loader.getMessage(), a -> a);

            return;
        }

        int mode = getMode(loader.getContent());
        int frame = getFrame(loader.getContent());

        Enemy e = enemies.getFirst();

        if (e.id == null)
            return;

        e.anim.load();

        if(mode >= e.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = e.anim.getEAnim(ImageDrawing.getAnimType(mode, e.anim.anims.length));

        File file = StaticStore.generateTempFile(folder, "icon", "png", false);

        if (file == null)
            return;

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

        anim.setTime(frame);

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

        CountDownLatch countdown = new CountDownLatch(1);

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
            countdown.countDown();

            return kotlin.Unit.INSTANCE;
        });

        countdown.await();

        if (!StaticStore.assetManager.uploadEnemyIconManual(e, id, file)) {
            replyToMessageSafely(loader.getChannel(), "Failed to upload enemy icon file as cache...", loader.getMessage(), a -> a);
        } else {
            replyToMessageSafely(loader.getChannel(), "Successfully uploaded enemy icon file as cache!", loader.getMessage(), a -> a);
        }

        StaticStore.deleteFile(file, true);

        e.anim.unload();
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

    private int getMode(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-m") || msg[i].equals("-mode")) {
                if(i < msg.length - 1) {
                    if(LangID.getStringByID("data.animation.mode.walk", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 0;
                    else if(LangID.getStringByID("data.animation.mode.idle", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 1;
                    else if(LangID.getStringByID("data.animation.mode.attack", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 2;
                    else if(LangID.getStringByID("data.animation.mode.kb", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 3;
                    else if(LangID.getStringByID("data.animation.mode.enter", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowDown", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowMove", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 5;
                    else if(LangID.getStringByID("data.animation.mode.burrowUp", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 6;
                } else {
                    return 0;
                }
            }
        }

        return 0;
    }

    private int getFrame(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-f") || msg[i].equals("-fr")) {
                if(i < msg.length - 1 && StaticStore.isNumeric(msg[i+1])) {
                    return StaticStore.safeParseInt(msg[i+1]);
                }
            }
        }

        return 0;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean debug = false;
        boolean trans = false;

        boolean mode = false;
        boolean frame = false;

        for(int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-t" -> {
                    if (!trans) {
                        trans = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-d", "-debug" -> {
                    if (!debug) {
                        debug = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-m", "-mode" -> {
                    if (!mode && i < contents.length - 1) {
                        mode = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-f", "-fr" -> {
                    if (!frame && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                        frame = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                default -> {
                    result.append(contents[i]);
                    written = true;
                }
            }

            if(written && i < contents.length - 1)
                result.append(" ");
        }

        return result.toString().trim();
    }
}