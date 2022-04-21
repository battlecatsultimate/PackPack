package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;

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

            Message m = createMessage(ch, c -> {
                c.content(LangID.getStringByID("optout_warn", lang));
                registerConfirmButtons(c, lang);
            });

            StaticStore.putHolder(id, new ConfirmButtonHolder(m, getMessage(event), ch.getId().asString(), id, () -> {
                StaticStore.optoutMembers.add(id);

                StaticStore.spamData.remove(id);
                StaticStore.prefix.remove(id);
                StaticStore.timeZones.remove(id);

                createMessage(ch, co -> co.content(LangID.getStringByID("optout_success", lang)));
            }, lang));
        }, () -> createMessage(ch, m -> m.content(LangID.getStringByID("optout_nomem", lang))));
    }
}
