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
            replyToMessageSafely(ch, loader.getMessage(), "Format : `p!ret [Guild ID] [Channel ID] [Message ID] [Emoji]`");

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, loader.getMessage(), "Guild ID must be numeric");

            return;
        }

        try {
            ShardManager client = loader.getClient().getShardManager();

            if (client == null)
                return;

            Guild g = client.getGuildById(contents[1]);

            if(g == null) {
                replyToMessageSafely(ch, loader.getMessage(), "No such guild");

                return;
            }

            if(!StaticStore.isNumeric(contents[2])) {
                replyToMessageSafely(ch, loader.getMessage(), "Channel ID must be numeric");

                return;
            }

            GuildChannel chan = g.getGuildChannelById(contents[2]);

            if(chan == null) {
                replyToMessageSafely(ch, loader.getMessage(), "No such channel");

                return;
            }

            if(!(chan instanceof MessageChannel) || !g.getSelfMember().hasPermission(chan, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)) {
                replyToMessageSafely(ch, loader.getMessage(), "Can't react message");

                return;
            }

            if(!StaticStore.isNumeric(contents[3])) {
                replyToMessageSafely(ch, loader.getMessage(), "Message ID must be numeric");

                return;
            }

            ((MessageChannel) chan).retrieveMessageById(contents[3]).queue( m -> {
                if(m == null) {
                    replyToMessageSafely(ch, loader.getMessage(), "No such message");

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
