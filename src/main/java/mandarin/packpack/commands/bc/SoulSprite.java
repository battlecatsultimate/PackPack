package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.pack.Soul;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class SoulSprite extends TimedConstraintCommand {
    public SoulSprite(ConstraintCommand.ROLE role, int lang, IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, "soulsprite");
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        int id = findSoulID(getContent(event));

        if(id == -1) {
            createMessageWithNoPings(ch, LangID.getStringByID("soul_argu", lang));

            disableTimer();
        }

        int soulLen = UserProfile.getBCData().souls.size();

        if(id >= soulLen) {
            createMessageWithNoPings(ch, LangID.getStringByID("soul_range", lang).replace("_", (soulLen - 1) + ""));

            disableTimer();

            return;
        }

        Soul s = UserProfile.getBCData().souls.get(id);

        if(s == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("soul_nosoul", lang));

            disableTimer();

            return;
        }

        EntityHandler.getSoulSprite(s, ch, lang);
    }

    private int findSoulID(String content) {
        String[] contents = content.split(" ");

        boolean frame = false;

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-f") || contents[i].equals("-fr")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]) && !frame) {
                frame = true;

                i++;
            } else if(StaticStore.isNumeric(contents[i]))
                return StaticStore.safeParseInt(contents[i]);
        }

        return -1;
    }
}
