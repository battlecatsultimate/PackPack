package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.concurrent.atomic.AtomicReference;

public class WatchDM extends ConstraintCommand {
    public WatchDM(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event).block();

        if(ch == null || g == null)
            return;

        String channel = getChannelID(getContent(event));

        if(channel == null && holder.logDM != null) {
            holder.logDM = null;
            StaticStore.idHolder.put(g.getId().asString(), holder);
            createMessage(ch, m -> m.content(LangID.getStringByID("watdm_remove", lang)));
            return;
        } else if(channel == null) {
            createMessage(ch, m -> m.content(LangID.getStringByID("watdm_nochan", lang)));
            return;
        } else if(!StaticStore.isNumeric(channel)) {
            createMessage(ch, m -> m.content(LangID.getStringByID("watdm_nonum" ,lang)));
            return;
        } else if(!isValidChannel(g, channel)) {
            createMessage(ch, m -> m.content(LangID.getStringByID("watdm_invalid", lang)));
            return;
        }

        holder.logDM = channel;

        StaticStore.idHolder.put(g.getId().asString(), holder);

        createMessage(ch, m -> m.content(LangID.getStringByID("watdm_set", lang).replace("_", channel)));
    }

    private String getChannelID(String content) {
        String[] contents = content.split(" ");

        if(contents.length >= 2) {
            return contents[1].replace("<#", "").replace(">", "");
        }

        return null;
    }

    private boolean isValidChannel(Guild g, String id) {
        AtomicReference<Boolean> valid = new AtomicReference<>(false);

        g.getChannels().collectList().subscribe(l -> {
            for(GuildChannel gc : l) {
                if((gc.getType() == Channel.Type.GUILD_TEXT || gc.getType() == Channel.Type.GUILD_NEWS) && id.equals(gc.getId().asString())) {
                    valid.set(true);
                    return;
                }
            }
        });

        return valid.get();
    }
}
