package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class ServerLocale extends ConstraintCommand {
    public ServerLocale(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = getChannel(event);

        if(ch != null) {
            String[] list = getContent(event).split(" ");

            if(list.length == 2) {
                if(StaticStore.isNumeric(list[1])) {
                    int lan = StaticStore.safeParseInt(list[1]) - 1;

                    if(lan >= 0 && lan <= StaticStore.langIndex.length - 1) {
                        int loc = StaticStore.langIndex[lan];

                        holder.config.lang = loc;

                        String locale;

                        switch (loc) {
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
}
