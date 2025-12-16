package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class UnregisterScamLink extends ConstraintCommand {
    public UnregisterScamLink(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        if(!StaticStore.scamLink.servers.contains(g.getId())) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("scamLinkRegister.failed.noPermission", lang));

            return;
        }

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("scamLinkRegister.failed.noLink", lang));

            return;
        }

        String link = contents[1];

        if(!link.startsWith("http://") && !link.startsWith("https://")) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("scamLinkRegister.failed.invalidLink", lang));

            return;
        }

        if(!StaticStore.scamLink.links.contains(link)) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("scamLinkRegisterRemove.failed.notFound", lang));

            return;
        }

        StaticStore.scamLink.links.remove(link);

        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("scamLinkRegisterRemove.removed", lang));
    }
}
