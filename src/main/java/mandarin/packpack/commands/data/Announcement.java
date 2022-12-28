package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Announcement extends ConstraintCommand {
    private static final int PARAM_EN = 2;
    private static final int PARAM_JP = 4;
    private static final int PARAM_KR = 8;
    private static final int PARAM_TW = 16;

    public Announcement(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        int param = checkParameter(getContent(event));

        String loc;
        String ver;

        switch (param) {
            case PARAM_EN:
                loc = "en";
                ver = StaticStore.englishVersion;
                break;
            case PARAM_JP:
                loc = "ja";
                ver = StaticStore.japaneseVersion;
                break;
            case PARAM_KR:
                loc = "ko";
                ver = StaticStore.koreanVersion;
                break;
            case PARAM_TW:
                loc = "tw";
                ver = StaticStore.taiwaneseVersion;
                break;
            default:
                switch (lang) {
                    case 1:
                        loc = "tw";
                        ver = StaticStore.taiwaneseVersion;
                        break;
                    case 2:
                        loc = "ko";
                        ver = StaticStore.koreanVersion;
                        break;
                    case 3:
                        loc = "ja";
                        ver = StaticStore.japaneseVersion;
                        break;
                    default:
                        loc = "en";
                        ver = StaticStore.englishVersion;
                }
        }

        Message msg = getMessage(event);

        String time = msg != null ? Long.toString(msg.getTimeCreated().toEpochSecond()) : "0";

        String url = EventFactor.ANNOUNCEURL.replace("LL", loc).replace("VVVVVV", ver).replace("DDDDDDDDD", time);

        ch.sendMessage(LangID.getStringByID("announce_limit", lang)+"\n"+url).queue();
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        for (String content : contents) {
            switch (content) {
                case "-en":
                    return PARAM_EN;
                case "-jp":
                    return PARAM_JP;
                case "-kr":
                    return PARAM_KR;
                case "-tw":
                    return PARAM_TW;
            }
        }

        return 1;
    }
}
