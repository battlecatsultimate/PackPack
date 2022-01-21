package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class RegisterScamLink extends ConstraintCommand {
    public RegisterScamLink(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event).block();

        if(ch == null || g == null) {
            return;
        }

        if(!StaticStore.scamLink.servers.contains(g.getId().asString())) {
            createMessage(ch, m -> m.content(LangID.getStringByID("scamreg_noperm", lang)));
            return;
        }

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            createMessage(ch, m -> m.content(LangID.getStringByID("scamreg_nolink", lang)));
            return;
        }

        String link = contents[1];

        if(!link.startsWith("http://") && !link.startsWith("https://")) {
            createMessage(ch, m -> m.content(LangID.getStringByID("scamreg_invlink", lang)));
            return;
        }

        if(StaticStore.scamLink.links.contains(link)) {
            createMessage(ch, m -> m.content(LangID.getStringByID("scamreg_already", lang)));
            return;
        }

        StaticStore.scamLink.links.add(link);
        createMessage(ch, m -> m.content(LangID.getStringByID("scamreg_added", lang)));
    }
}
