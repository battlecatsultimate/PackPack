package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import javax.annotation.Nonnull;

public class UnsubscribeScamLinkDetector extends ConstraintCommand {
    public UnsubscribeScamLinkDetector(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        Guild g = loader.getGuild();

        if(!StaticStore.scamLinkHandlers.servers.containsKey(g.getId())) {
            ch.sendMessage(LangID.getStringByID("unsubscribeScamDetector.failed.noDetector", lang)).queue();

            return;
        }

        StaticStore.scamLinkHandlers.servers.remove(g.getId());

        ch.sendMessage(LangID.getStringByID("unsubscribeScamDetector.unsubscribed", lang)).queue();
    }
}
