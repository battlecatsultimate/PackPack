package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import javax.annotation.Nonnull;

public class SendMessage extends ConstraintCommand {
    public SendMessage(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        String[] contents = loader.getContent().split(" ", 4);

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
