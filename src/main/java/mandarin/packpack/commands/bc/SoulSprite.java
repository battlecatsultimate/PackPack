package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.pack.Soul;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class SoulSprite extends TimedConstraintCommand {
    public SoulSprite(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, "soulsprite", false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        int id = findSoulID(loader.getContent());

        if(id == -1) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("soulImage.failed.noParameter", lang));

            disableTimer();

            return;
        }

        int soulLen = UserProfile.getBCData().souls.size();

        if(id >= soulLen) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("soulImage.failed.outOfRange", lang).formatted(soulLen - 1));

            disableTimer();

            return;
        }

        Soul s = UserProfile.getBCData().souls.get(id);

        if(s == null) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("soulImage.failed.noSoul", lang));

            disableTimer();

            return;
        }

        EntityHandler.generateSoulSprite(s, ch, loader.getMessage(), lang);
    }

    private int findSoulID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(StaticStore.isNumeric(contents[i]))
                return StaticStore.safeParseInt(contents[i]);
        }

        return -1;
    }
}
