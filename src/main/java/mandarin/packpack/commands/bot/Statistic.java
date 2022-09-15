package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Statistic extends Command {
    public Statistic(int lang) {
        super(lang);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        createMessageWithNoPings(ch, LangID.getStringByID("stat_info", lang)
                .replace("_SSS_", StaticStore.idHolder.size()+"")
                .replace("_CCC_", StaticStore.executed+"")
                .replace("_MMM_", StaticStore.spamData.size()+"")
        );
    }
}
