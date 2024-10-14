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

public class RegisterScamLink extends ConstraintCommand {
    public RegisterScamLink(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        if(!StaticStore.scamLink.servers.contains(g.getId())) {
            ch.sendMessage(LangID.getStringByID("scamLinkRegister.failed.noPermission", lang)).queue();

            return;
        }

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            ch.sendMessage(LangID.getStringByID("scamLinkRegister.failed.noLink", lang)).queue();

            return;
        }

        String link = contents[1].replaceAll("/$", "");

        if(!link.startsWith("http://") && !link.startsWith("https://")) {
            ch.sendMessage(LangID.getStringByID("scamLinkRegister.failed.invalidLink", lang)).queue();

            return;
        }

        if(StaticStore.scamLink.links.contains(link)) {
            ch.sendMessage(LangID.getStringByID("scamLinkRegister.failed.alreadyRegistered", lang)).queue();

            return;
        }

        StaticStore.scamLink.links.add(link);
        ch.sendMessage(LangID.getStringByID("scamLinkRegister.added", lang)).queue();
    }
}
