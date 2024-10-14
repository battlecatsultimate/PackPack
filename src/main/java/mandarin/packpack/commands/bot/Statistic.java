package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class Statistic extends Command {
    public Statistic(CommonStatic.Lang.Locale lang) {
        super(lang, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        createMessageWithNoPings(ch, LangID.getStringByID("stat.info", lang)
                .replace("_SSS_", String.valueOf(StaticStore.idHolder.size()))
                .replace("_CCC_", String.valueOf(StaticStore.executed))
                .replace("_MMM_", String.valueOf(StaticStore.spamData.size()))
        );
    }
}
