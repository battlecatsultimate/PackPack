package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class SuggestBan extends ConstraintCommand {

    public SuggestBan(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            ch.sendMessage("This command requires user ID!").queue();
        } else {
            String reason = getReason(loader.getContent());

            StaticStore.suggestBanned.put(contents[1], reason.isBlank() ? "None" : reason);

            ch.sendMessage("Banned "+contents[1]).queue();
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
