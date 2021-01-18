package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class Locale extends ConstraintCommand {
    public Locale(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch != null) {
            String[] list = getMessage(event).split(" ");

            if(list.length == 2) {
                if(StaticStore.isNumeric(list[1])) {
                    int lan = StaticStore.safeParseInt(list[1]) - 1;

                    if(lan >= 0 && lan <= 3) {
                        event.getMember().ifPresentOrElse(m -> {
                            StaticStore.locales.put(m.getId().asString(), lan);

                            String locale;

                            if(lan == LangID.EN) {
                                locale = LangID.getStringByID("lang_en", lan);
                            } else if(lan == LangID.JP) {
                                locale = LangID.getStringByID("lang_jp", lan);
                            } else if(lan == LangID.KR) {
                                locale = LangID.getStringByID("lang_kr", lan);
                            } else {
                                locale = LangID.getStringByID("lang_zh", lan);
                            }

                            ch.createMessage(LangID.getStringByID("locale_set", lan).replace("_", locale)).subscribe();
                        }, () -> ch.createMessage("Can't find member!").subscribe());
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
