package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;

public class GetID extends ConstraintCommand {
    public GetID(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length != 3) {
            replyToMessageSafely(ch, "`p!gi -u|m/c/s [ID]`", loader.getMessage(), a -> a);

            return;
        }

        if(!StaticStore.isNumeric(contents[2])) {
            replyToMessageSafely(ch, "ID is not numeric", loader.getMessage(), a -> a);

            return;
        }

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        try {
            switch (contents[1]) {
                case "-m", "-u" -> client.retrieveUserById(contents[2]).queue(u -> replyToMessageSafely(ch, "User : " + u.getEffectiveName() + " (" + u.getAsMention() + ")", loader.getMessage(), a -> a));
                case "-c" -> {
                    GuildChannel c = client.getGuildChannelById(contents[2]);
                    if (c == null)
                        return;

                    String type;

                    if (c instanceof TextChannel) {
                        type = "Text Channel";
                    } else if (c instanceof NewsChannel) {
                        type = "News Channel";
                    } else if (c instanceof MessageChannel) {
                        type = "Message Channel";
                    } else {
                        type = "Channel";
                    }

                    replyToMessageSafely(ch, type + " : " + c.getName() + " (" + c.getAsMention() + ")", loader.getMessage(), a -> a);
                }
                case "-s" -> {
                    Guild g = client.getGuildById(contents[2]);

                    if (g == null)
                        return;

                    replyToMessageSafely(ch, "Guild : " + g.getName() + " | Size : " + g.getMemberCount(), loader.getMessage(), a -> a);
                }
            }
        } catch (Exception ignore) {
            replyToMessageSafely(ch, "Failed", loader.getMessage(), a -> a);
        }
    }
}
