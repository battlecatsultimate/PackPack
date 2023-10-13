package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class UnregisterScamLink extends ConstraintCommand {
    public UnregisterScamLink(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        if(!StaticStore.scamLink.servers.contains(g.getId())) {
            ch.sendMessage(LangID.getStringByID("scamreg_noperm", lang)).queue();
            return;
        }

        String[] contents = loader.getContent().split(" ");

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
