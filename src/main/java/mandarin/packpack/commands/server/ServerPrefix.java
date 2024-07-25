package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class ServerPrefix extends ConstraintCommand {
    public ServerPrefix(ROLE role, CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(role, lang, holder, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ");

        if(list.length == 2) {
            if(list[1] == null || list[1].isBlank()) {
                ch.sendMessage(LangID.getStringByID("prefix.noWhiteSpace", lang)).queue();
                return;
            }

            holder.config.prefix = list[1];

            createMessageWithNoPings(ch, LangID.getStringByID("serverPrefix.set", lang).replace("_", holder.config.prefix));
        } else if(list.length == 1) {
            ch.sendMessage(LangID.getStringByID("prefix.fail.noParameter", lang)).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("prefix_tooag", lang)).queue();
        }
    }
}
