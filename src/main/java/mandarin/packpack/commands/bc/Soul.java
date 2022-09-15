package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.GlobalTimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Soul extends GlobalTimedConstraintCommand {
    private static final String NO_ID = "NO_ID";
    private static final String INVALID_RANGE = "INVALID_RANGE";

    private final int PARAM_DEBUG = 2;
    private final int PARAM_RAW = 4;
    private final int PARAM_GIF = 8;

    public Soul(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(30));
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    protected void doThing(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Member m = getMember(event);
        Guild g = getGuild(event);

        if(ch == null || m == null || g == null)
            return;

        int id = findSoulID(getContent(event));

        common.util.pack.Soul s = UserProfile.getBCData().souls.get(id);

        if(s == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("soul_nosoul", lang));

            return;
        }

        boolean isTrusted = StaticStore.contributors.contains(m.getId()) || m.getId().equals(StaticStore.MANDARIN_SMELL);

        int param = checkParameters(getContent(event));
        boolean debug = (param & PARAM_DEBUG) > 0;
        boolean raw = (param & PARAM_RAW) > 0;
        boolean gif = (param & PARAM_GIF) > 0;
        int frame = getFrame(getContent(event));

        if(raw && !isTrusted) {
            ch.sendMessage(LangID.getStringByID("gif_ignore", lang)).queue();
        }

        boolean result = EntityHandler.generateSoulAnim(s, ch, g.getBoostTier().getKey(), debug, frame, lang, raw && isTrusted, gif);

        if(raw && isTrusted) {
            changeTime(TimeUnit.MINUTES.toMillis(1));
        }

        if(!result) {
            disableTimer();
        }
    }

    @Override
    protected void setOptionalID(GenericMessageEvent event) {
        int id = findSoulID(getContent(event));

        if(id == -1) {
            optionalID = NO_ID;
        } else {
            int max = UserProfile.getBCData().souls.size();

            if(id >= max) {
                optionalID = INVALID_RANGE;
            }
        }
    }

    @Override
    protected void prepareAborts() {
        aborts.add(NO_ID);
        aborts.add(INVALID_RANGE);
    }

    @Override
    protected void onAbort(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        switch (optionalID) {
            case NO_ID:
                createMessageWithNoPings(ch, LangID.getStringByID("soul_argu", lang));

                return;
            case INVALID_RANGE:
                int soulLen = UserProfile.getBCData().souls.size() - 1;

                createMessageWithNoPings(ch, LangID.getStringByID("soul_range", lang).replace("_", soulLen + ""));
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for(int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-d":
                    case "-debug":
                        if((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                        break;
                    case "-r":
                    case "-raw":
                        if((result & PARAM_RAW) == 0) {
                            result |= PARAM_RAW;
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
                    case "-g":
                    case "-gif":
                        if((result & PARAM_GIF) == 0) {
                            result |= PARAM_GIF;
                        } else {
                            break label;
                        }
                        break;
                }
            }
        }

        return result;
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

        return -1;
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
