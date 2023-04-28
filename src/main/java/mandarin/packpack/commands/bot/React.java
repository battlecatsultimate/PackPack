package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class React extends ConstraintCommand {
    public React(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        String[] contents = getContent(event).replaceAll("\\s+", " ").split(" ", 4);

        if(contents.length < 4) {
            replyToMessageSafely(ch, "Format : `p!r [Channel ID] [Message ID] [Emoji]`", getMessage(event), a -> a);

            return;
        }

        try {
            String channel = getChannel(contents[1]);

            if(!StaticStore.isNumeric(channel)) {
                replyToMessageSafely(ch, "Channel ID must be numeric", getMessage(event), a -> a);

                return;
            }

            GuildChannel chan = g.getGuildChannelById(channel);

            if(chan == null) {
                replyToMessageSafely(ch, "No such channel", getMessage(event), a -> a);

                return;
            }

            if(!(chan instanceof MessageChannel) || !g.getSelfMember().hasPermission(chan, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)) {
                replyToMessageSafely(ch, "Can't react message", getMessage(event), a -> a);

                return;
            }

            if(!StaticStore.isNumeric(contents[2])) {
                replyToMessageSafely(ch, "Message ID must be numeric", getMessage(event), a -> a);

                return;
            }

            Message m = ((MessageChannel) chan).retrieveMessageById(contents[2]).complete();

            if(m == null) {
                replyToMessageSafely(ch, "No such message", getMessage(event), a -> a);

                return;
            }

            try {
                EmojiUnion em = Emoji.fromFormatted(contents[3]);

                MessageReaction mr = m.getReaction(em);

                if(mr != null) {
                    if(mr.isSelf()) {
                        m.removeReaction(em).queue();

                        replyToMessageSafely(ch, "Removed emoji : " + em.getFormatted(), getMessage(event), a -> a);
                    } else {
                        m.addReaction(em).queue();

                        replyToMessageSafely(ch, "Added emoji : " + em.getFormatted(), getMessage(event), a -> a);
                    }
                } else {
                    m.addReaction(em).queue();

                    replyToMessageSafely(ch, "Added emoji : " + em.getFormatted(), getMessage(event), a -> a);
                }
            } catch (IllegalStateException ignored) {
                replyToMessageSafely(ch, "Failed to get emoji data", getMessage(event), a -> a);
            }
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
