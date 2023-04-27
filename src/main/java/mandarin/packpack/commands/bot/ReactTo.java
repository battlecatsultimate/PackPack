package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class ReactTo extends ConstraintCommand {
    public ReactTo(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ", 5);

        if(contents.length < 5) {
            replyToMessageSafely(ch, "Format : `p!ret [Guild ID] [Channel ID] [Message ID] [Emoji]`", getMessage(event), a -> a);

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, "Guild ID must be numeric", getMessage(event), a -> a);

            return;
        }

        try {
            Guild g = ch.getJDA().getGuildById(contents[1]);

            if(g == null) {
                replyToMessageSafely(ch, "No such guild", getMessage(event), a -> a);

                return;
            }

            if(!StaticStore.isNumeric(contents[2])) {
                replyToMessageSafely(ch, "Channel ID must be numeric", getMessage(event), a -> a);

                return;
            }

            GuildChannel chan = g.getGuildChannelById(contents[2]);

            if(chan == null) {
                replyToMessageSafely(ch, "No such channel", getMessage(event), a -> a);

                return;
            }

            if(!(chan instanceof MessageChannel) || !g.getSelfMember().hasPermission(chan, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)) {
                replyToMessageSafely(ch, "Can't react message", getMessage(event), a -> a);

                return;
            }

            if(!StaticStore.isNumeric(contents[3])) {
                replyToMessageSafely(ch, "Message ID must be numeric", getMessage(event), a -> a);

                return;
            }

            Message m = ((MessageChannel) chan).getHistory().getMessageById(contents[3]);

            if(m == null) {
                replyToMessageSafely(ch, "No such message", getMessage(event), a -> a);

                return;
            }

            EmojiUnion em = Emoji.fromFormatted(contents[4]);

            m.addReaction(em).queue();
        } catch (Exception ignored) { }
    }
}
