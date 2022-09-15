package mandarin.packpack.commands;

import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test extends GlobalTimedConstraintCommand {
    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected void doThing(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 3) {
            ch.sendMessage("`p!test -s/c/m [ID]`").queue();

            return;
        }

        JDA gate = event.getJDA();

        switch (contents[1]) {
            case "-m":
                User u = gate.getUserById(contents[2]);

                if(u != null) {
                    ch.sendMessage("User Name : "+u.getName()+u.getDiscriminator()).queue();
                }
                break;
            case "-c":
                GuildChannel c = gate.getGuildChannelById(contents[2]);

                if(c != null) {
                    ch.sendMessage("Channel type : "+c.getType()).queue();
                    ch.sendMessage("Channel Name : "+ c.getName()).queue();
                    ch.sendMessage("Server name : "+ c.getGuild().getName()).queue();
                }

                break;
            case "-s":
                Guild g = gate.getGuildById(contents[2]);

                if(g != null) {
                    ch.sendMessage("Guild Name : "+g.getName()).queue();
                }

                break;
        }
    }

    @Override
    protected void setOptionalID(GenericMessageEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}