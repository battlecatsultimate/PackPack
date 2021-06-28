package mandarin.packpack.commands.server;

import common.CommonStatic;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.NewsChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class Publish extends ConstraintCommand {
    private final GatewayDiscordClient client;

    public Publish(ROLE role, int lang, IDHolder id, GatewayDiscordClient client) {
        super(role, lang, id);

        this.client = client;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        if(!StaticStore.announcements.containsKey(0)) {
            createMessageWithNoPings(ch, "You have to at least make announcement for English!");
            return;
        }

        for(String id : StaticStore.idHolder.keySet()) {
            if(id == null || id.isBlank())
                continue;

            IDHolder holder = StaticStore.idHolder.get(id);

            if(holder.ANNOUNCE == null)
                continue;

            Guild g = client.getGuildById(Snowflake.of(id)).block();

            if(g == null)
                continue;

            GuildChannel c = g.getChannelById(Snowflake.of(holder.ANNOUNCE)).block();

            if(c instanceof NewsChannel) {
                Message me = ((NewsChannel) c).createMessage(m -> {
                    m.setAllowedMentions(AllowedMentions.builder().build());

                    int[] pref = CommonStatic.Lang.pref[holder.serverLocale];

                    for(int p : pref) {
                        if(StaticStore.announcements.containsKey(p)) {
                            m.setContent(StaticStore.announcements.get(p));
                            break;
                        }
                    }
                }).block();

                if(me != null && holder.publish) {
                    me.publish().subscribe();
                }
            } else if(c instanceof TextChannel) {
                ((TextChannel) c).createMessage(m -> {
                    int[] pref = CommonStatic.Lang.pref[holder.serverLocale];

                    for(int p : pref) {
                        if(StaticStore.announcements.containsKey(p)) {
                            m.setContent(StaticStore.announcements.get(p));
                            m.setAllowedMentions(AllowedMentions.builder().build());
                            break;
                        }
                    }
                }).subscribe();
            }
        }

        StaticStore.announcements.clear();

        createMessageWithNoPings(ch, "Successfully announced");
    }
}
