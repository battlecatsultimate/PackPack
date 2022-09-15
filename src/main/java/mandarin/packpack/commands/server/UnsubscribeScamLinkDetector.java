package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class UnsubscribeScamLinkDetector extends ConstraintCommand {
    public UnsubscribeScamLinkDetector(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Guild g = getGuild(event);

        if(g == null) {
            ch.sendMessage(LangID.getStringByID("subscam_noguild", lang)).queue();

            return;
        }

        if(!StaticStore.scamLinkHandlers.servers.containsKey(g.getId())) {
            ch.sendMessage(LangID.getStringByID("subscam_notreg", lang)).queue();

            return;
        }

        StaticStore.scamLinkHandlers.servers.remove(g.getId());

        ch.sendMessage(LangID.getStringByID("subscam_unsub", lang)).queue();
    }
}
