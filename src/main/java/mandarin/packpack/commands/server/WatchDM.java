package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.List;

public class WatchDM extends ConstraintCommand {
    public WatchDM(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        String channel = getChannelID(getContent(event));

        if(channel == null && holder.logDM != null) {
            holder.logDM = null;
            StaticStore.idHolder.put(g.getId(), holder);

            ch.sendMessage(LangID.getStringByID("watdm_remove", lang)).queue();

            return;
        } else if(channel == null) {
            ch.sendMessage(LangID.getStringByID("watdm_nochan", lang)).queue();

            return;
        } else if(!StaticStore.isNumeric(channel)) {
            ch.sendMessage(LangID.getStringByID("watdm_nonum" ,lang)).queue();

            return;
        } else if(!isValidChannel(g, channel)) {
            ch.sendMessage(LangID.getStringByID("watdm_invalid", lang)).queue();

            return;
        }

        holder.logDM = channel;

        StaticStore.idHolder.put(g.getId(), holder);

        ch.sendMessage(LangID.getStringByID("watdm_set", lang).replace("_", channel)).queue();
    }

    private String getChannelID(String content) {
        String[] contents = content.split(" ");

        if(contents.length >= 2) {
            return contents[1].replace("<#", "").replace(">", "");
        }

        return null;
    }

    private boolean isValidChannel(Guild g, String id) {
        List<GuildChannel> channels = g.getChannels();

        for(GuildChannel gc : channels) {
            if((gc.getType() == ChannelType.TEXT || gc.getType() == ChannelType.NEWS) && id.equals(gc.getId())) {
                return true;
            }
        }

        return false;
    }
}
