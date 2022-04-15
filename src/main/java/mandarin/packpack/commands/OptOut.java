package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class OptOut extends ConstraintCommand {
    public OptOut(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        getMember(event).ifPresentOrElse(me -> {
            String id = me.getId().asString();

            if(StaticStore.optoutMembers.contains(id)) {
                StaticStore.optoutMembers.remove(id);

                createMessage(ch, m -> m.content(LangID.getStringByID("optout_in", lang)));
            } else {
                StaticStore.optoutMembers.add(id);

                createMessage(ch, m -> m.content(LangID.getStringByID("optout_out", lang)));
            }
        }, () -> createMessage(ch, m -> m.content(LangID.getStringByID("optout_nomem", lang))));
    }
}
