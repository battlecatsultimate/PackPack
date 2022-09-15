package mandarin.packpack.commands;

import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class SendMessage extends ConstraintCommand {
    public SendMessage(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        JDA client = event.getJDA();

        String[] contents = getContent(event).split(" ", 4);

        if(contents.length != 4) {
            ch.sendMessage("`p!sm [Guild ID] [Channel ID] [Contents]`").queue();
            return;
        }

        Guild g = client.getGuildById(contents[1]);

        if(g != null) {
            GuildChannel c = g.getGuildChannelById(contents[2]);

            if(c != null) {
                if(c instanceof GuildMessageChannel) {
                    String co;

                    if(contents[3].startsWith("`"))
                        co = contents[3].substring(1, contents[3].length() - 1).replace("\\e", "");
                    else
                        co = contents[3].replace("\\e", "");

                    ((GuildMessageChannel) c).sendMessage(co).queue();

                    ch.sendMessage(co).queue();

                    System.out.println(co);
                } else {
                    ch.sendMessage("Channel isn't message channel").queue();
                }
            } else {
                ch.sendMessage("No such channel found").queue();
            }
        } else {
            ch.sendMessage("No such guild found").queue();
        }
    }
}
