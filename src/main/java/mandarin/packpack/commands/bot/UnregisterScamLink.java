package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class UnregisterScamLink extends ConstraintCommand {
    public UnregisterScamLink(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null) {
            return;
        }

        if(!StaticStore.scamLink.servers.contains(g.getId())) {
            ch.sendMessage(LangID.getStringByID("scamreg_noperm", lang)).queue();
            return;
        }

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            ch.sendMessage(LangID.getStringByID("scamreg_nolink", lang)).queue();

            return;
        }

        String link = contents[1];

        if(!link.startsWith("http://") && !link.startsWith("https://")) {
            ch.sendMessage(LangID.getStringByID("scamreg_invlink", lang)).queue();
            return;
        }

        if(!StaticStore.scamLink.links.contains(link)) {
            ch.sendMessage(LangID.getStringByID("scamreg_notfound", lang)).queue();
            return;
        }

        StaticStore.scamLink.links.remove(link);
        ch.sendMessage(LangID.getStringByID("scamreg_removed", lang)).queue();
    }
}
