package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class SuggestUnban extends ConstraintCommand {

    public SuggestUnban(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            ch.sendMessage("This command requires user ID!").queue();
        } else {
            if(StaticStore.suggestBanned.containsKey(contents[1])) {
                StaticStore.suggestBanned.remove(contents[1]);

                ch.sendMessage("Unbanned "+contents[1]).queue();
            } else {
                ch.sendMessage("That user seems not suggest-banned yet").queue();
            }
        }
    }
}
