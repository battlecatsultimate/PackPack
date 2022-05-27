package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;

public class Locale extends ConstraintCommand {

    public Locale(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(MessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch != null) {
            String[] list = getContent(event).split(" ");

            if(list.length == 2) {
                if(StaticStore.isNumeric(list[1])) {
                    int lan = StaticStore.safeParseInt(list[1]) - 1;

                    if(lan >= 0 && lan <= StaticStore.langIndex.length - 1) {
                        int loc = StaticStore.langIndex[lan];

                        getMember(event).ifPresentOrElse(m -> {
                            ConfigHolder holder;

                            if(StaticStore.config.containsKey(m.getId().asString()))
                                holder = StaticStore.config.get(m.getId().asString());
                            else
                                holder = new ConfigHolder();

                            holder.lang = loc;

                            StaticStore.config.put(m.getId().asString(), holder);

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

                            ch.createMessage(LangID.getStringByID("locale_set", lan).replace("_", locale)).subscribe();
                        }, () -> ch.createMessage("Can't find member!").subscribe());
                    } else if(lan == -1) {
                        getMember(event).ifPresent(m -> {
                            if(StaticStore.config.containsKey(m.getId().asString())) {
                                ConfigHolder holder = StaticStore.config.get(m.getId().asString());

                                holder.lang = -1;

                                StaticStore.config.put(m.getId().asString(), holder);
                            }
                        });

                        Guild g = getGuild(event).block();

                        if(g != null) {
                            IDHolder holder = StaticStore.idHolder.get(g.getId().asString());

                            if(holder != null) {
                                ch.createMessage(LangID.getStringByID("locale_auto", holder.serverLocale)).subscribe();
                            } else {
                                ch.createMessage(LangID.getStringByID("locale_auto", lang)).subscribe();
                            }
                        } else {
                            ch.createMessage(LangID.getStringByID("locale_auto", lang)).subscribe();
                        }
                    } else {
                        ch.createMessage(LangID.getStringByID("locale_incorrect", lan)).subscribe();
                    }
                } else {
                    ch.createMessage(LangID.getStringByID("locale_number", lang)).subscribe();
                }
            } else {
                ch.createMessage(LangID.getStringByID("locale_argu", lang)).subscribe();
            }
        }
    }
}
