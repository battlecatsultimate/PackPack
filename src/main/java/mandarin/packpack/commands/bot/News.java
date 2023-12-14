package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class News extends ConstraintCommand {
    public News(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String content = loader.getContent();

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

                String locale = switch (StaticStore.langIndex[loc]) {
                    case LangID.EN -> LangID.getStringByID("lang_en", loc);
                    case LangID.JP -> LangID.getStringByID("lang_jp", loc);
                    case LangID.KR -> LangID.getStringByID("lang_kr", loc);
                    case LangID.ZH -> LangID.getStringByID("lang_zh", loc);
                    case LangID.FR -> LangID.getStringByID("lang_fr", loc);
                    case LangID.IT -> LangID.getStringByID("lang_it", loc);
                    case LangID.ES -> LangID.getStringByID("lang_es", loc);
                    default -> LangID.getStringByID("lang_de", loc);
                };

                createMessageWithNoPings(ch, "Announcement added for "+locale);
            } else {
                createMessageWithNoPings(ch, "Locale must be number\nEN : 0, ZH : 1, KR : 2, JP : 3, FR : 4, IT : 5, ES : 6, DE : 7");
            }
        }
    }
}
