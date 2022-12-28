package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Locale extends ConstraintCommand {

    public Locale(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch != null) {
            String[] list = getContent(event).split(" ");

            if(list.length == 2) {
                if(StaticStore.isNumeric(list[1])) {
                    int lan = StaticStore.safeParseInt(list[1]) - 1;

                    if(lan >= 0 && lan <= StaticStore.langIndex.length - 1) {
                        int loc = StaticStore.langIndex[lan];

                        User u = getUser(event);
                        
                        if(u != null) {
                            ConfigHolder holder;

                            if(StaticStore.config.containsKey(u.getId()))
                                holder = StaticStore.config.get(u.getId());
                            else
                                holder = new ConfigHolder();

                            holder.lang = loc;

                            StaticStore.config.put(u.getId(), holder);

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
                                case LangID.DE:
                                    locale = LangID.getStringByID("lang_de", loc);
                                    break;
                                default:
                                    locale = LangID.getStringByID("lang_th", loc);
                                    break;
                            }

                            replyToMessageSafely(ch, LangID.getStringByID("locale_set", lan).replace("_", locale), getMessage(event), a -> a);
                        } else {
                            replyToMessageSafely(ch, "Can't find member!", getMessage(event), a -> a);
                        }
                    } else if(lan == -1) {
                        User u = getUser(event);
                        
                        if(u != null) {
                            if(StaticStore.config.containsKey(u.getId())) {
                                ConfigHolder holder = StaticStore.config.get(u.getId());

                                holder.lang = -1;

                                StaticStore.config.put(u.getId(), holder);
                            }
                        }

                        Guild g = getGuild(event);

                        if(g != null) {
                            IDHolder holder = StaticStore.idHolder.get(g.getId());

                            if(holder != null) {
                                replyToMessageSafely(ch, LangID.getStringByID("locale_auto", holder.config.lang), getMessage(event), a -> a);
                            } else {
                                replyToMessageSafely(ch, LangID.getStringByID("locale_auto", lang), getMessage(event), a -> a);
                            }
                        } else {
                            replyToMessageSafely(ch, LangID.getStringByID("locale_auto", lang), getMessage(event), a -> a);
                        }
                    } else {
                        replyToMessageSafely(ch, LangID.getStringByID("locale_incorrect", lan), getMessage(event), a -> a);
                    }
                } else {
                    replyToMessageSafely(ch, LangID.getStringByID("locale_number", lang), getMessage(event), a -> a);
                }
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("locale_argu", lang), getMessage(event), a -> a);
            }
        }
    }
}
