package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class ClearCache extends ConstraintCommand {
    public ClearCache(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        StaticStore.imgur.clear();

        ch.createMessage(LangID.getStringByID("clearcache_cleared", lang)).subscribe();
    }
}
