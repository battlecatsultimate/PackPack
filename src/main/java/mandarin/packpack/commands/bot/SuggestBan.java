package mandarin.packpack.commands.bot;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class SuggestBan extends ConstraintCommand {

    public SuggestBan(ROLE role, int lang, IDHolder id) {
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
            String reason = getReason(getContent(event));

            StaticStore.suggestBanned.put(contents[1], reason.isBlank() ? "None" : reason);

            ch.createMessage("Banned "+contents[1]).subscribe();
        }
    }

    private String getReason(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2)
            return "";

        StringBuilder result = new StringBuilder();

        boolean reasonStart = false;

        for(int i = 1; i < contents.length; i++) {
            if(contents[i].equals("-r") && !reasonStart) {
                reasonStart = true;
            } else if(reasonStart) {
                result.append(contents[i]);

                if(i < contents.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString();
    }
}
