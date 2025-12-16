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
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class React extends ConstraintCommand {
    public React(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        String[] contents = loader.getContent().replaceAll("\\s+", " ").split(" ", 4);

        if(contents.length < 4) {
            replyToMessageSafely(ch, loader.getMessage(), "Format : `p!r [Channel ID] [Message ID] [Emoji]`");

            return;
        }

        try {
            String channel = getChannel(contents[1]);

            if(!StaticStore.isNumeric(channel)) {
                replyToMessageSafely(ch, loader.getMessage(), "Channel ID must be numeric");

                return;
            }

            GuildChannel chan = g.getGuildChannelById(channel);

            if(chan == null) {
                replyToMessageSafely(ch, loader.getMessage(), "No such channel");

                return;
            }

            if(!(chan instanceof MessageChannel) || !g.getSelfMember().hasPermission(chan, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)) {
                replyToMessageSafely(ch, loader.getMessage(), "Can't react message");

                return;
            }

            if(!StaticStore.isNumeric(contents[2])) {
                replyToMessageSafely(ch, loader.getMessage(), "Message ID must be numeric");

                return;
            }

            ((MessageChannel) chan).retrieveMessageById(contents[2]).queue( m -> {
                if(m == null) {
                    replyToMessageSafely(ch, loader.getMessage(), "No such message");

                    return;
                }

                try {
                    EmojiUnion em = Emoji.fromFormatted(contents[3]);

                    MessageReaction mr = m.getReaction(em);

                    if(mr != null) {
                        if(mr.isSelf()) {
                            m.removeReaction(em).queue();

                            replyToMessageSafely(ch, loader.getMessage(), "Removed emoji : " + em.getFormatted());
                        } else {
                            m.addReaction(em).queue();

                            replyToMessageSafely(ch, loader.getMessage(), "Added emoji : " + em.getFormatted());
                        }
                    } else {
                        m.addReaction(em).queue();

                        replyToMessageSafely(ch, loader.getMessage(), "Added emoji : " + em.getFormatted());
                    }
                } catch (IllegalStateException ignored) {
                    replyToMessageSafely(ch, loader.getMessage(), "Failed to get emoji data");
                }
            });


        } catch (Exception ignored) { }
    }

    private String getChannel(String text) {
        if(StaticStore.isNumeric(text))
            return text;
        else if(text.matches("<#\\d+>"))
            return text.replaceAll("(<#|>)", "");
        else
            return text;
    }
}
