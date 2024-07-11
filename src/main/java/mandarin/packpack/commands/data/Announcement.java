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
import org.jetbrains.annotations.NotNull;

public class Announcement extends ConstraintCommand {
    private static final int PARAM_EN = 2;
    private static final int PARAM_JP = 4;
    private static final int PARAM_KR = 8;
    private static final int PARAM_TW = 16;

    public Announcement(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        int param = checkParameter(loader.getContent());

        String loc;
        String ver;

        switch (param) {
            case PARAM_EN -> {
                loc = "en";
                ver = StaticStore.englishVersion;
            }
            case PARAM_JP -> {
                loc = "ja";
                ver = StaticStore.japaneseVersion;
            }
            case PARAM_KR -> {
                loc = "ko";
                ver = StaticStore.koreanVersion;
            }
            case PARAM_TW -> {
                loc = "tw";
                ver = StaticStore.taiwaneseVersion;
            }
            default -> {
                switch (lang) {
                    case ZH -> {
                        loc = "tw";
                        ver = StaticStore.taiwaneseVersion;
                    }
                    case KR -> {
                        loc = "ko";
                        ver = StaticStore.koreanVersion;
                    }
                    case JP -> {
                        loc = "ja";
                        ver = StaticStore.japaneseVersion;
                    }
                    default -> {
                        loc = "en";
                        ver = StaticStore.englishVersion;
                    }
                }
            }
        }

        Message msg = loader.getMessage();

        String time = Long.toString(msg.getTimeCreated().toEpochSecond());

        String url = EventFactor.ANNOUNCEURL.replace("LL", loc).replace("VVVVVV", ver).replace("DDDDDDDDD", time);

        ch.sendMessage(LangID.getStringByID("announce_limit", lang)+"\n"+url).queue();
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
