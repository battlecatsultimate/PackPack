package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class TimeZone extends ConstraintCommand {
    public TimeZone(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            createMessage(ch, m -> m.content(LangID.getStringByID("timezone_noval", lang)));
            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            createMessage(ch, m -> m.content(LangID.getStringByID("timezone_notnum", lang)));
            return;
        }

        int timeZone = Math.min(12, Math.max(-12, StaticStore.safeParseInt(contents[1])));

        getMember(event).ifPresentOrElse(m -> {
            StaticStore.timeZones.put(m.getId().asString(), timeZone);

            if(timeZone >= 0) {
                createMessage(ch, me -> me.content(LangID.getStringByID("timezone_done", lang).replace("_", "+" + timeZone)));
            } else {
                createMessage(ch, me -> me.content(LangID.getStringByID("timezone_done", lang).replace("_", timeZone + "")));
            }
        }, () -> createMessage(ch, m -> m.content(LangID.getStringByID("timezone_nomem", lang))));
    }
}
