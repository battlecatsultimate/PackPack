package mandarin.packpack.commands.bot;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.IDHolder;

public class SuggestUnban extends ConstraintCommand {

    public SuggestUnban(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            ch.createMessage("This command requires user ID!").subscribe();
        } else {
            if(StaticStore.suggestBanned.containsKey(contents[1])) {
                StaticStore.suggestBanned.remove(contents[1]);

                ch.createMessage("Unbanned "+contents[1]).subscribe();
            } else {
                ch.createMessage("That user seems not suggest-banned yet").subscribe();
            }
        }
    }
}
