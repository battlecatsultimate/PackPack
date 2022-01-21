package mandarin.packpack.supporter.server;

import com.google.gson.JsonObject;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.Ban;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class ScamLinkHandler {
    public static boolean validScammingUser(String content) {
        for(String link : StaticStore.scamLink.links) {
            if(content.contains(link))
                return true;
        }

        return false;
    }

    public static String getLinkFromMessage(String content) {
        for(String link : StaticStore.scamLink.links) {
            if(content.contains(link))
                return link;
        }

        return null;
    }

    public enum ACTION {
        MUTE,
        KICK,
        BAN
    }

    public final String author;
    public final String server;
    public final String channel;
    @Nullable
    public final String mute;
    public final ACTION action;
    public final boolean noticeAll;

    public ScamLinkHandler(String author, String server, String channel, @Nullable String mute, ACTION action, boolean noticeAll) {
        this.author = author;
        this.server = server;
        this.action = action;
        this.channel = channel;
        this.mute = mute;
        this.noticeAll = noticeAll;
    }

    public void takeAction(GatewayDiscordClient gate, String link, Member m, Guild g) {
        GuildChannel ch = g.getChannelById(Snowflake.of(channel)).block();
        IDHolder holder = StaticStore.idHolder.get(g.getId().asString());

        if(holder == null)
            return;

        if(!(ch instanceof MessageChannel)) {
            g.getMemberById(Snowflake.of(author)).subscribe(me -> me.getPrivateChannel().subscribe(pch -> pch.createMessage(MessageCreateSpec.builder().content(LangID.getStringByID("scamhandle_nochannel", holder.serverLocale)).build())));
            return;
        }

        if(action == ACTION.MUTE) {
            if(mute == null) {
                StaticStore.logger.uploadLog("Something impossible happened for ScamLinkHandler\nServer ID : "+server+"\nACTION : "+action+"\nMute role ID : null\nReport Channel : "+channel);
            } else {
                Set<Snowflake> roleID = m.getRoleIds();

                for(Snowflake id : roleID) {
                    m.removeRole(id).subscribe();
                }

                g.getRoleById(Snowflake.of(mute)).subscribe(r -> m.addRole(r.getId()).subscribe());

                EmbedCreateSpec spec = Command.createEmbed(e -> {
                    e.title(LangID.getStringByID("scamhandle_title", holder.serverLocale));
                    e.author(m.getDisplayName() +" ("+m.getId().asString()+")", null, m.getAvatarUrl());
                    e.description(LangID.getStringByID("scamhandle_descmute", holder.serverLocale));
                });

                try {
                    Command.createMessage((MessageChannel) ch, msg -> msg.addEmbed(spec));
                } catch (Exception ignored) {}
            }
        } else if(action == ACTION.KICK) {
            m.kick(LangID.getStringByID("scamhandle_kickreason", holder.serverLocale)).subscribe();

            EmbedCreateSpec spec = Command.createEmbed(e -> {
                e.title(LangID.getStringByID("scamhandle_title", holder.serverLocale));
                e.author(m.getDisplayName() +" ("+m.getId().asString()+")", null, m.getAvatarUrl());
                e.description(LangID.getStringByID("scamhandle_desckick", holder.serverLocale));
            });

            try {
                Command.createMessage((MessageChannel) ch, msg -> msg.addEmbed(spec));
            } catch (Exception ignored) {}
        } else if(action == ACTION.BAN) {
            m.ban(BanQuerySpec.builder().reason(LangID.getStringByID("scamhandle_banreason", holder.serverLocale)).build()).subscribe();

            EmbedCreateSpec spec = Command.createEmbed(e -> {
                e.title(LangID.getStringByID("scamhandle_title", holder.serverLocale));
                e.author(m.getDisplayName() +" ("+m.getId().asString()+")", null, m.getAvatarUrl());
                e.description(LangID.getStringByID("scamhandle_descban", holder.serverLocale));
            });

            try {
                Command.createMessage((MessageChannel) ch, msg -> msg.addEmbed(spec));
            } catch (Exception ignored) {}
        }

        m.getPrivateChannel().subscribe(pc -> pc.createMessage(MessageCreateSpec.builder().content(LangID.getStringByID("scamhandle_dm", holder.serverLocale).replace("_NNN_", g.getName()).replace("_III_", g.getId().asString())).build()).subscribe());

        for(String guildID : StaticStore.idHolder.keySet()) {
            if(g.getId().asString().equals(guildID))
                continue;

            try {
                List<Ban> bans = g.getBans().collectList().block();

                if(bans != null) {
                    boolean banned = false;

                    for(Ban ban : bans) {
                        if(ban.getUser().getId().asString().equals(m.getId().asString())) {
                            banned = true;
                            break;
                        }
                    }

                    if(banned)
                        continue;
                }
            } catch (Exception ignored) {}

            if(!StaticStore.scamLinkHandlers.servers.containsKey(guildID))
                continue;

            IDHolder h = StaticStore.idHolder.get(guildID);
            ScamLinkHandler handler = StaticStore.scamLinkHandlers.servers.get(guildID);

            if(h == null || handler == null)
                continue;

            gate.getGuildById(Snowflake.of(guildID)).subscribe(gu -> {
                if(handler.noticeAll) {
                    gu.getMemberById(m.getId()).subscribe(me -> gu.getChannelById(Snowflake.of(handler.channel)).subscribe(cha -> {
                        if(cha instanceof MessageChannel) {
                            EmbedCreateSpec spec = Command.createEmbed(e -> {
                                e.title(LangID.getStringByID("scamhandle_report", h.serverLocale));
                                e.author(m.getDisplayName()+" ("+m.getId().asString()+")", null, m.getAvatarUrl());
                                e.description(LangID.getStringByID("scamhandle_reportdesc", h.serverLocale).replace("_", link));
                            });

                            Command.createMessage((MessageChannel) cha, msg -> msg.addEmbed(spec));
                        }
                    }, e -> {}), ex -> gu.getChannelById(Snowflake.of(handler.channel)).subscribe(cha -> {
                        if(cha instanceof MessageChannel) {
                            EmbedCreateSpec spec = Command.createEmbed(e -> {
                                e.title(LangID.getStringByID("scamhandle_report", h.serverLocale));
                                e.author(m.getDisplayName()+" ("+m.getId().asString()+")", null, m.getAvatarUrl());
                                e.description(LangID.getStringByID("scamhandle_reportdescall", h.serverLocale).replace("_", link));
                            });

                            Command.createMessage((MessageChannel) cha, msg -> msg.addEmbed(spec));
                        }
                    }, e -> {}));
                } else {
                    gu.getMemberById(m.getId()).subscribe(me -> gu.getChannelById(Snowflake.of(handler.channel)).subscribe(cha -> {
                        if(cha instanceof MessageChannel) {
                            EmbedCreateSpec spec = Command.createEmbed(e -> {
                                e.title(LangID.getStringByID("scamhandle_report", h.serverLocale));
                                e.author(m.getDisplayName()+" ("+m.getId().asString()+")", null, m.getAvatarUrl());
                                e.description(LangID.getStringByID("scamhandle_reportdesc", h.serverLocale).replace("_", link));
                            });

                            Command.createMessage((MessageChannel) cha, msg -> msg.addEmbed(spec));
                        }
                    }, e -> {}), e -> {});
                }
            }, e -> {});
        }
    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("author", author);
        obj.addProperty("server", server);
        obj.addProperty("channel", channel);
        obj.addProperty("noticeAll", noticeAll);

        if(mute != null) {
            obj.addProperty("mute", mute);
        }

        switch (action) {
            case MUTE:
                obj.addProperty("action", "mute");
                break;
            case KICK:
                obj.addProperty("action", "kick");
                break;
            case BAN:
                obj.addProperty("action", "ban");
                break;
        }

        return obj;
    }
}
