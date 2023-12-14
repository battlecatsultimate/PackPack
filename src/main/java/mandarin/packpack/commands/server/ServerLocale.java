package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class ServerLocale extends ConstraintCommand {
    public ServerLocale(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ");

        if(list.length == 2) {
            if(StaticStore.isNumeric(list[1])) {
                int lan = StaticStore.safeParseInt(list[1]) - 1;

                if(lan >= 0 && lan <= StaticStore.langIndex.length - 1) {
                    int loc = StaticStore.langIndex[lan];

                    holder.config.lang = loc;

                    String locale = switch (loc) {
                        case LangID.EN -> LangID.getStringByID("lang_en", loc);
                        case LangID.JP -> LangID.getStringByID("lang_jp", loc);
                        case LangID.KR -> LangID.getStringByID("lang_kr", loc);
                        case LangID.ZH -> LangID.getStringByID("lang_zh", loc);
                        case LangID.FR -> LangID.getStringByID("lang_fr", loc);
                        case LangID.IT -> LangID.getStringByID("lang_it", loc);
                        case LangID.ES -> LangID.getStringByID("lang_es", loc);
                        default -> LangID.getStringByID("lang_de", loc);
                    };

                    ch.sendMessage(LangID.getStringByID("serverlocale_set", lan).replace("_", locale)).queue();
                } else {
                    ch.sendMessage(LangID.getStringByID("locale_incorrect", lan)).queue();
                }
            } else {
                ch.sendMessage(LangID.getStringByID("locale_number", lang)).queue();
            }
        } else {
            ch.sendMessage(LangID.getStringByID("locale_argu", lang)).queue();
        }
    }
}
