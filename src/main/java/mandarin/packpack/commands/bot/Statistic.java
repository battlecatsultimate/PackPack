package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class Statistic extends Command {
    public Statistic(int lang) {
        super(lang, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        createMessageWithNoPings(ch, LangID.getStringByID("stat_info", lang)
                .replace("_SSS_", String.valueOf(StaticStore.idHolder.size()))
                .replace("_CCC_", String.valueOf(StaticStore.executed))
                .replace("_MMM_", String.valueOf(StaticStore.spamData.size()))
        );
    }
}
