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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

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

        for (Unit u : UserProfile.getBCData().units) {
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
        }

        for (Enemy e : UserProfile.getBCData().enemies) {
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
        }
    }
}
