package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class ServerPrefix extends ConstraintCommand {
    public ServerPrefix(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        String[] list = getContent(event).split(" ");

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
