package mandarin.packpack.commands.bot;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class CreateIconCache extends ConstraintCommand {
    public CreateIconCache(ROLE role, CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        File folder = new File("./temp");

        if (!folder.exists() && !folder.mkdirs()) {
            StaticStore.logger.uploadLog("W/CreateIconCache::doSomething - Failed to create folder : " + folder.getAbsolutePath());

            return;
        }

        CommonStatic.getConfig().ref = false;

        String progress = "**Progress**\n\nUnit : %d / %d \n Enemy : %d / %d";

        AtomicReference<Message> message = new AtomicReference<>();
        CountDownLatch countdown = new CountDownLatch(1);

        replyToMessageSafely(loader.getChannel(), progress.formatted(0, UserProfile.getBCData().units.size(), 0, UserProfile.getBCData().enemies.size()), loader.getMessage(), a -> a, msg -> {
            message.set(msg);

            countdown.countDown();
        });

        countdown.await();

        Message msg = message.get();

        if (msg == null)
            return;

        int unitIndex = 0;
        int enemyIndex = 0;

        long currentTime = System.currentTimeMillis();

        for (Unit u : UserProfile.getBCData().units) {
            if (System.currentTimeMillis() - currentTime >= 1500) {
                msg.editMessage(progress.formatted(unitIndex, UserProfile.getBCData().units.size(), enemyIndex, UserProfile.getBCData().enemies.size())).queue();

                currentTime = System.currentTimeMillis();
            }

            if (u.id == null)
                continue;

            for (Form f : u.forms) {
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
                String oldId = StaticStore.assetManager.getUnitIconID(f);

                f.anim.unload();

                if (!id.equals(oldId)) {
                    StaticStore.assetManager.removeUnitIcon(f);
                    StaticStore.assetManager.getUnitIcon(f);
                }
            }

            unitIndex++;
        }

        for (Enemy e : UserProfile.getBCData().enemies) {
            if (System.currentTimeMillis() - currentTime >= 1500) {
                msg.editMessage(progress.formatted(unitIndex, UserProfile.getBCData().units.size(), enemyIndex, UserProfile.getBCData().enemies.size())).queue();

                currentTime = System.currentTimeMillis();
            }

            if (e.id == null)
                continue;

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
            String oldId = StaticStore.assetManager.getEnemyIconID(e);

            e.anim.unload();

            if (!id.equals(oldId)) {
                StaticStore.assetManager.removeEnemyIcon(e);
                StaticStore.assetManager.getEnemyIcon(e);
            }

            enemyIndex++;
        }

        msg.editMessage(progress.formatted(unitIndex, UserProfile.getBCData().units.size(), enemyIndex, UserProfile.getBCData().enemies.size()) + "\n\n**Done!**").queue();
    }
}
