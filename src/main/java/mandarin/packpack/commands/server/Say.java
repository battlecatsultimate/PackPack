package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class Say extends ConstraintCommand {
    public Say(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        String[] contents = getContent(event).split(" ", 3);

        if(contents.length < 2) {
            replyToMessageSafely(ch, "Please specify the channel! Format : `p!say [Channel] [Content]`", getMessage(event), a -> a);

            return;
        }

        if(contents.length < 3) {
            replyToMessageSafely(ch, "You didn't specify the message to be sent to that channel! Format : `p!say [Channel] [Content]`", getMessage(event), a -> a);

            return;
        }

        String channelID;

        if(StaticStore.isNumeric(contents[1])) {
            channelID = contents[1];
        } else if(contents[1].matches("<#\\d+>")) {
            channelID = contents[1].replace("<#", "").replace(">", "");
        } else {
            replyToMessageSafely(ch, "Bot couldn't detect chnnel from command. You have to pass channel via either ID or mention", getMessage(event), a -> a);

            return;
        }

        try {
            GuildChannel gc = g.getGuildChannelById(channelID);

            if(!(gc instanceof MessageChannel) || !((MessageChannel) gc).canTalk()) {
                replyToMessageSafely(ch, "Channel must be place where bot can talk! Check channel type and bot's permission", getMessage(event), a -> a);

                return;
            }

            if(Pattern.compile("(<@&\\d+>|@everyone|@here)").matcher(contents[2]).find()) {
                Message m = getMessage(event);

                if(m == null)
                    return;

                Message target = registerConfirmButtons(ch.sendMessage("This message may mention role or a lot of users, are you sure you want to send this message?"), lang).complete();

                StaticStore.putHolder(m.getAuthor().getId(), new ConfirmButtonHolder(m, target, ch.getId(), () -> ((MessageChannel) gc).sendMessage(contents[2]).queue(), lang));
            } else {
                ((MessageChannel) gc).sendMessage(contents[2]).queue();
            }
        } catch (Exception ignored) {
            replyToMessageSafely(ch, "Failed to get channel from command...", getMessage(event), a -> a);
        }
    }
}
