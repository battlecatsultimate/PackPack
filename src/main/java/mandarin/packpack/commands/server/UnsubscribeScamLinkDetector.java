package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class UnsubscribeScamLinkDetector extends ConstraintCommand {
    public UnsubscribeScamLinkDetector(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Guild g = getGuild(event).block();

        if(g == null) {
            createMessage(ch, m -> m.content(LangID.getStringByID("subscam_noguild", lang)));
            return;
        }

        if(!StaticStore.scamLinkHandlers.servers.containsKey(g.getId().asString())) {
            createMessage(ch, m -> m.content(LangID.getStringByID("subscam_notreg", lang)));
            return;
        }

        StaticStore.scamLinkHandlers.servers.remove(g.getId().asString());

        createMessage(ch, m -> m.content(LangID.getStringByID("subscam_unsub", lang)));
    }
}
