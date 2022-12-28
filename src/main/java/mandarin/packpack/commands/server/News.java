package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class News extends ConstraintCommand {
    public News(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String content = getContent(event);

        String[] contents = content.split(" ", 3);

        if(contents.length != 3) {
            createMessageWithNoPings(ch, "Usage : [Locale] [Desc]\nEN : 0, ZH : 1, KR : 2, JP : 3, FR : 4, IT : 5, ES : 6, DE : 7");
        } else {
            if(StaticStore.isNumeric(contents[1])) {
                int loc = StaticStore.safeParseInt(contents[1]);

                if(loc >= 8) {
                    createMessageWithNoPings(ch, "Locale must be less than 8");
                    return;
                }

                if(contents[2].isBlank()) {
                    createMessageWithNoPings(ch, "Announcement content is empty!");
                    return;
                }

                StaticStore.announcements.put(StaticStore.langIndex[loc], contents[2]);

                String locale;

                switch (StaticStore.langIndex[loc]) {
                    case LangID.EN:
                        locale = LangID.getStringByID("lang_en", loc);
                        break;
                    case LangID.JP:
                        locale = LangID.getStringByID("lang_jp", loc);
                        break;
                    case LangID.KR:
                        locale = LangID.getStringByID("lang_kr", loc);
                        break;
                    case LangID.ZH:
                        locale = LangID.getStringByID("lang_zh", loc);
                        break;
                    case LangID.FR:
                        locale = LangID.getStringByID("lang_fr", loc);
                        break;
                    case LangID.IT:
                        locale = LangID.getStringByID("lang_it", loc);
                        break;
                    case LangID.ES:
                        locale = LangID.getStringByID("lang_es", loc);
                        break;
                    default:
                        locale = LangID.getStringByID("lang_de", loc);
                        break;
                }

                createMessageWithNoPings(ch, "Announcement added for "+locale);
            } else {
                createMessageWithNoPings(ch, "Locale must be number\nEN : 0, ZH : 1, KR : 2, JP : 3, FR : 4, IT : 5, ES : 6, DE : 7");
            }
        }
    }
}
