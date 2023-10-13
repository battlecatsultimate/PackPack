package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class ServerPrefix extends ConstraintCommand {
    public ServerPrefix(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder, true);
    }

    @Override
    public void doSomething(CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ");

        if(list.length == 2) {
            if(list[1] == null || list[1].isBlank()) {
                ch.sendMessage(LangID.getStringByID("prefix_space", lang)).queue();
                return;
            }

            holder.serverPrefix = list[1];

            createMessageWithNoPings(ch, LangID.getStringByID("serverpre_set", lang).replace("_", holder.serverPrefix));
        } else if(list.length == 1) {
            ch.sendMessage(LangID.getStringByID("prefix_argu", lang)).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("prefix_tooag", lang)).queue();
        }
    }
}
