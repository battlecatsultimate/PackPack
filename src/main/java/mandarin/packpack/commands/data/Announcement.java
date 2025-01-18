package mandarin.packpack.commands.data;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class Announcement extends ConstraintCommand {
    private static final int PARAM_EN = 2;
    private static final int PARAM_JP = 4;
    private static final int PARAM_KR = 8;
    private static final int PARAM_TW = 16;

    public Announcement(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        int param = checkParameter(loader.getContent());

        String loc;
        String ver;

        switch (param) {
            case PARAM_EN -> {
                loc = "en";
                ver = String.valueOf(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.EN, true));
            }
            case PARAM_JP -> {
                loc = "ja";
                ver = String.valueOf(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.JP, true));
            }
            case PARAM_KR -> {
                loc = "ko";
                ver = String.valueOf(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.KR, true));
            }
            case PARAM_TW -> {
                loc = "tw";
                ver = String.valueOf(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.ZH, true));
            }
            default -> {
                ver = String.valueOf(StaticStore.event.getVersionCode(lang, true));

                switch (lang) {
                    case ZH -> loc = "tw";
                    case KR -> loc = "ko";
                    case JP -> loc = "ja";
                    default -> loc = "en";
                }
            }
        }

        Message msg = loader.getMessage();

        String time = Long.toString(msg.getTimeCreated().toEpochSecond());

        String url = EventFactor.ANNOUNCEURL.replace("LL", loc).replace("VVVVVV", ver).replace("DDDDDDDDD", time);

        ch.sendMessage(LangID.getStringByID("announce.limit", lang)+"\n"+url).queue();
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        for (String content : contents) {
            switch (content) {
                case "-en" -> {
                    return PARAM_EN;
                }
                case "-jp" -> {
                    return PARAM_JP;
                }
                case "-kr" -> {
                    return PARAM_KR;
                }
                case "-tw" -> {
                    return PARAM_TW;
                }
            }
        }

        return 1;
    }
}
