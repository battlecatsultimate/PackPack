package mandarin.packpack.commands.data;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class Announcement extends ConstraintCommand {
    private static final int PARAM_EN = 2;
    private static final int PARAM_JP = 4;
    private static final int PARAM_KR = 8;
    private static final int PARAM_TW = 16;

    public Announcement(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        int param = checkParameter(getContent(event));

        String loc;
        String ver;

        switch (param) {
            case PARAM_EN:
                loc = "en";
                ver = EventFactor.currentGlobalVersion;
                break;
            case PARAM_JP:
                loc = "ja";
                ver = EventFactor.currentJapaneseVersion;
                break;
            case PARAM_KR:
                loc = "ko";
                ver = EventFactor.currentGlobalVersion;
                break;
            case PARAM_TW:
                loc = "tw";
                ver = EventFactor.currentGlobalVersion;
                break;
            default:
                switch (lang) {
                    case 1:
                        loc = "tw";
                        ver = EventFactor.currentGlobalVersion;
                        break;
                    case 2:
                        loc = "ko";
                        ver = EventFactor.currentGlobalVersion;
                        break;
                    case 3:
                        loc = "ja";
                        ver = EventFactor.currentJapaneseVersion;
                        break;
                    default:
                        loc = "en";
                        ver = EventFactor.currentGlobalVersion;
                }
        }

        Message msg = getMessage(event);

        String time = msg != null ? Long.toString(msg.getTimestamp().getEpochSecond()) : "0";

        String url = EventFactor.ANNOUNCEURL.replace("LL", loc).replace("VVVVVV", ver).replace("DDDDDDDDD", time);

        ch.createMessage(LangID.getStringByID("announce_limit", lang)+"\n"+url).subscribe();
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
