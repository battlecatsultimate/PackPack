package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class GetID extends ConstraintCommand {
    public GetID(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length != 3) {
            replyToMessageSafely(ch, "`p!gi -u|m/c/s [ID]`", getMessage(event), a -> a);

            return;
        }

        if(!StaticStore.isNumeric(contents[2])) {
            replyToMessageSafely(ch, "ID is not numeric", getMessage(event), a -> a);

            return;
        }

        JDA jda = ch.getJDA();

        try {
            switch (contents[1]) {
                case "-m", "-u" -> {
                    User u = jda.retrieveUserById(contents[2]).complete();
                    if (u == null)
                        return;
                    replyToMessageSafely(ch, "User : " + u.getEffectiveName() + " (" + u.getAsMention() + ")", getMessage(event), a -> a);
                }
                case "-c" -> {
                    GuildChannel c = jda.getGuildChannelById(contents[2]);
                    if (c == null)
                        return;
                    String type = switch (c) {
                        case TextChannel ignored2 -> "Text Channel";
                        case NewsChannel ignored1 -> "News Channel";
                        case MessageChannel ignored -> "Message Channel";
                        default -> "Channel";
                    };
                    replyToMessageSafely(ch, type + " : " + c.getName() + " (" + c.getAsMention() + ")", getMessage(event), a -> a);
                }
                case "-s" -> {
                    Guild g = jda.getGuildById(contents[2]);

                    if (g == null)
                        return;

                    replyToMessageSafely(ch, "Guild : " + g.getName() + " | Size : " + g.getMemberCount(), getMessage(event), a -> a);
                }
            }
        } catch (Exception ignore) {
            replyToMessageSafely(ch, "Failed", getMessage(event), a -> a);
        }
    }
}
