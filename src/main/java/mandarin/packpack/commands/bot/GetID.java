package mandarin.packpack.commands.bot;

import common.CommonStatic;
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

import javax.annotation.Nonnull;

public class GetID extends ConstraintCommand {
    public GetID(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length != 3) {
            replyToMessageSafely(ch, loader.getMessage(), "`p!gi -u|m/c/s [ID]`");

            return;
        }

        if(!StaticStore.isNumeric(contents[2])) {
            replyToMessageSafely(ch, loader.getMessage(), "ID is not numeric");

            return;
        }

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        try {
            switch (contents[1]) {
                case "-m", "-u" -> client.retrieveUserById(contents[2]).queue(u -> replyToMessageSafely(ch, loader.getMessage(), "User : " + u.getEffectiveName() + " (" + u.getAsMention() + ")"));
                case "-c" -> {
                    GuildChannel c = client.getGuildChannelById(contents[2]);
                    if (c == null)
                        return;

                    String type = switch (c) {
                        case TextChannel ignored -> "Text Channel";
                        case NewsChannel ignored -> "News Channel";
                        case MessageChannel ignored -> "Message Channel";
                        default -> "Channel";
                    };

                    replyToMessageSafely(ch, loader.getMessage(), type + " : " + c.getName() + " (" + c.getAsMention() + ")");
                }
                case "-s" -> {
                    Guild g = client.getGuildById(contents[2]);

                    if (g == null)
                        return;

                    replyToMessageSafely(ch, loader.getMessage(), "Guild : " + g.getName() + " | Size : " + g.getMemberCount());
                }
            }
        } catch (Exception ignore) {
            replyToMessageSafely(ch, loader.getMessage(), "Failed");
        }
    }
}
