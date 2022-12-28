package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.pack.Soul;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.io.File;
import java.util.Locale;

public class SoulImage extends TimedConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    public SoulImage(ConstraintCommand.ROLE role, int lang, IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, "soulimage", false);
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
            replyToMessageSafely(ch, LangID.getStringByID("soul_argu", lang), getMessage(event), a -> a);

            return;
        }

        int soulLen = UserProfile.getBCData().souls.size();

        if(id >= soulLen) {
            replyToMessageSafely(ch, LangID.getStringByID("soul_range", lang).replace("_", (soulLen - 1) + ""), getMessage(event), a -> a);

            disableTimer();

            return;
        }

        Soul s = UserProfile.getBCData().souls.get(id);

        if(s == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("soul_nosoul", lang));

            disableTimer();

            return;
        }

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/SoulImage::doSomething - Failed to create folder : " + temp.getAbsolutePath());

            disableTimer();

            return;
        }

        int param = checkParameters(getContent(event));
        int frame = getFrame(getContent(event));

        s.anim.load();

        EAnimD<?> anim = s.anim.getEAnim(AnimU.UType.SOUL);

        File img = ImageDrawing.drawAnimImage(anim, frame, 1.0, (param & PARAM_TRANSPARENT) > 0, (param & PARAM_DEBUG) > 0);

        s.anim.unload();

        if(img != null) {
            sendMessageWithFile(
                    ch,
                    LangID.getStringByID("soulimg_result", lang).replace("_", Data.trio(s.getID().id)).replace("-", frame + ""),
                    img,
                    getMessage(event)
            );
        } else {
            createMessageWithNoPings(ch, LangID.getStringByID("soulimg_fail", lang));

            disableTimer();
        }
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

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for(int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-t":
                        if((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
                        } else {
                            break label;
                        }
                        break;
                    case "-d":
                    case "-debug":
                        if((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                        break;
                    case "-f":
                    case "-fr":
                        if(i < pureMessage.length - 1 && StaticStore.isNumeric(pureMessage[i+1])) {
                            i++;
                        } else {
                            break label;
                        }
                        break;
                }
            }
        }

        return result;
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
