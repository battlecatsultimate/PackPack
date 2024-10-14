package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class ReactTo extends ConstraintCommand {
    public ReactTo(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ", 5);

        if(contents.length < 5) {
            replyToMessageSafely(ch, "Format : `p!ret [Guild ID] [Channel ID] [Message ID] [Emoji]`", loader.getMessage(), a -> a);

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, "Guild ID must be numeric", loader.getMessage(), a -> a);

            return;
        }

        try {
            ShardManager client = ch.getJDA().getShardManager();

            if (client == null)
                return;

            Guild g = client.getGuildById(contents[1]);

            if(g == null) {
                replyToMessageSafely(ch, "No such guild", loader.getMessage(), a -> a);

                return;
            }

            if(!StaticStore.isNumeric(contents[2])) {
                replyToMessageSafely(ch, "Channel ID must be numeric", loader.getMessage(), a -> a);

                return;
            }

            GuildChannel chan = g.getGuildChannelById(contents[2]);

            if(chan == null) {
                replyToMessageSafely(ch, "No such channel", loader.getMessage(), a -> a);

                return;
            }

            if(!(chan instanceof MessageChannel) || !g.getSelfMember().hasPermission(chan, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)) {
                replyToMessageSafely(ch, "Can't react message", loader.getMessage(), a -> a);

                return;
            }

            if(!StaticStore.isNumeric(contents[3])) {
                replyToMessageSafely(ch, "Message ID must be numeric", loader.getMessage(), a -> a);

                return;
            }

            ((MessageChannel) chan).retrieveMessageById(contents[3]).queue( m -> {
                if(m == null) {
                    replyToMessageSafely(ch, "No such message", loader.getMessage(), a -> a);

                    return;
                }

                EmojiUnion em = Emoji.fromFormatted(contents[4]);

                MessageReaction mr = m.getReaction(em);

                if(mr != null) {
                    if(mr.isSelf()) {
                        m.removeReaction(em).queue();
                    } else {
                        m.addReaction(em).queue();
                    }
                } else {
                    m.addReaction(em).queue();
                }
            });
        } catch (Exception ignored) { }
    }
}
