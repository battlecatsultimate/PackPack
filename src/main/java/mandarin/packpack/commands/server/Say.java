package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class Say extends ConstraintCommand {
    public Say(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        String[] contents = loader.getContent().split(" ", 3);

        if(contents.length < 2) {
            replyToMessageSafely(ch, "Please specify the channel! Format : `p!say [Channel] [Content]`", loader.getMessage(), a -> a);

            return;
        }

        if(contents.length < 3) {
            replyToMessageSafely(ch, "You didn't specify the message to be sent to that channel! Format : `p!say [Channel] [Content]`", loader.getMessage(), a -> a);

            return;
        }

        String channelID;

        if(StaticStore.isNumeric(contents[1])) {
            channelID = contents[1];
        } else if(contents[1].matches("<#\\d+>")) {
            channelID = contents[1].replace("<#", "").replace(">", "");
        } else {
            replyToMessageSafely(ch, "Bot couldn't detect chnnel from command. You have to pass channel via either ID or mention", loader.getMessage(), a -> a);

            return;
        }

        try {
            GuildChannel gc = g.getGuildChannelById(channelID);

            if(!(gc instanceof MessageChannel) || !((MessageChannel) gc).canTalk()) {
                replyToMessageSafely(ch, "Channel must be place where bot can talk! Check channel type and bot's permission", loader.getMessage(), a -> a);

                return;
            }

            if(Pattern.compile("(<@&\\d+>|@everyone|@here)").matcher(contents[2]).find()) {
                Message m = loader.getMessage();

                registerConfirmButtons(ch.sendMessage("This message may mention role or a lot of users, are you sure you want to send this message?"), lang).queue(target ->
                        StaticStore.putHolder(m.getAuthor().getId(), new ConfirmButtonHolder(m, target, ch.getId(), lang, () -> ((MessageChannel) gc).sendMessage(contents[2]).queue()))
                );

            } else {
                ((MessageChannel) gc).sendMessage(contents[2]).queue();
            }
        } catch (Exception ignored) {
            replyToMessageSafely(ch, "Failed to get channel from command...", loader.getMessage(), a -> a);
        }
    }
}
