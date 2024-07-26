package mandarin.packpack.supporter.server;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public record ScamLinkHandler(String author, String server, String channel, @Nullable String mute, ACTION action, boolean noticeAll) {
    public static boolean validScammingUser(String content) {
        for (String link : StaticStore.scamLink.links) {
            if (content.contains(link) && !content.matches("(.+)?(unregisterscamlink|usl|registerscamlink|rsl) +" + link + "$"))
                return true;
        }

        return false;
    }

    public static String getLinkFromMessage(String content) {
        for (String link : StaticStore.scamLink.links) {
            if (content.contains(link))
                return link;
        }

        return null;
    }

    public enum ACTION {
        MUTE,
        KICK,
        BAN
    }

    public void takeAction(String link, Member m, Guild g) {
        if (m == null)
            return;

        GuildChannel ch = g.getGuildChannelById(channel);
        IDHolder holder = StaticStore.idHolder.get(g.getId());

        if (holder == null || ch == null)
            return;

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        if (!(ch instanceof MessageChannel)) {
            Member me = g.getMemberById(author);

            if (me == null)
                return;

            User u = me.getUser();

            u.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(LangID.getStringByID("bot.scamHandler.noChannel", holder.config.lang)))
                    .queue();

            return;
        }

        if (action == ACTION.MUTE) {
            if (mute == null) {
                StaticStore.logger.uploadLog("Something impossible happened for ScamLinkHandler\nServer ID : " + server + "\nACTION : " + action + "\nMute role ID : null\nReport Channel : " + channel);
            } else {
                List<Role> roleID = m.getRoles();
                int pos = StaticStore.getHighestRolePosition(g.getSelfMember());

                if (g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                    for (Role r : roleID) {
                        if (pos > r.getPosition()) {
                            g.removeRoleFromMember(UserSnowflake.fromId(m.getId()), r).queue();
                        }
                    }
                }

                Role muteRole = g.getRoleById(mute);

                if (muteRole != null && g.getSelfMember().hasPermission(Permission.MANAGE_ROLES) && StaticStore.getHighestRolePosition(g.getSelfMember()) > muteRole.getPosition()) {
                    g.addRoleToMember(UserSnowflake.fromId(m.getId()), muteRole).queue();
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setTitle(LangID.getStringByID("scamDetector.title", holder.config.lang))
                        .setAuthor(m.getEffectiveName() + " (" + m.getId() + ")", null, m.getAvatarUrl())
                        .setDescription(LangID.getStringByID("scamDetector.actionDone.mute", holder.config.lang))
                        .build();

                try {
                    ((MessageChannel) ch).sendMessageEmbeds(embed).queue();
                } catch (Exception ignored) {
                }
            }
        } else if (action == ACTION.KICK) {
            m.kick()
                    .reason(LangID.getStringByID("scamDetector.reason.kick", holder.config.lang))
                    .queue();

            MessageEmbed embed = new EmbedBuilder()
                    .setTitle(LangID.getStringByID("scamDetector.title", holder.config.lang))
                    .setAuthor(m.getEffectiveName() + " (" + m.getId() + ")", null, m.getAvatarUrl())
                    .setDescription(LangID.getStringByID("scamDetector.actionDone.kick", holder.config.lang))
                    .build();

            try {
                ((MessageChannel) ch).sendMessageEmbeds(embed).queue();
            } catch (Exception ignored) {
            }
        } else if (action == ACTION.BAN) {
            m.ban(0, TimeUnit.HOURS)
                    .reason(LangID.getStringByID("scamDetector.reason.kick", holder.config.lang))
                    .queue();

            MessageEmbed embed = new EmbedBuilder()
                    .setTitle(LangID.getStringByID("scamDetector.title", holder.config.lang))
                    .setAuthor(m.getEffectiveName() + " (" + m.getId() + ")", null, m.getAvatarUrl())
                    .setDescription(LangID.getStringByID("scamDetector.actionDone.ban", holder.config.lang))
                    .build();

            try {
                ((MessageChannel) ch).sendMessageEmbeds(embed).queue();
            } catch (Exception ignored) {
            }
        }

        m.getUser().openPrivateChannel()
                .flatMap(pc -> pc.sendMessage(LangID.getStringByID("scamDetector.directMessage", holder.config.lang).replace("_NNN_", g.getName()).replace("_III_", g.getId())))
                .queue();

        for (String guildID : StaticStore.idHolder.keySet()) {
            if (g.getId().equals(guildID))
                continue;

            try {
                AtomicReference<Boolean> banned = new AtomicReference<>(false);

                AtomicBoolean running = new AtomicBoolean(true);

                g.retrieveBanList().onSuccess(list -> {
                    for (int i = 0; i < list.size(); i++) {
                        Guild.Ban b = list.get(i);

                        if (b.getUser().getId().equals(m.getId())) {
                            banned.set(true);
                        }
                    }

                    running.set(false);
                }).queue();

                while (true) {
                    if (!running.get())
                        break;
                }

                if (banned.get())
                    return;
            } catch (Exception ignored) {
            }

            if (!StaticStore.scamLinkHandlers.servers.containsKey(guildID))
                continue;

            IDHolder h = StaticStore.idHolder.get(guildID);
            ScamLinkHandler handler = StaticStore.scamLinkHandlers.servers.get(guildID);

            if (h == null || handler == null)
                continue;

            Guild gu = client.getGuildById(guildID);

            if (gu == null)
                continue;

            Member me = gu.getMemberById(m.getId());

            if (handler.noticeAll) {
                GuildChannel cha = gu.getGuildChannelById(handler.channel);

                if (cha instanceof MessageChannel) {
                    MessageEmbed embed;

                    if (me != null) {
                        embed = new EmbedBuilder()
                                .setTitle(LangID.getStringByID("scamDetector.compromisedReport", h.config.lang))
                                .setAuthor(m.getEffectiveName() + " (" + m.getId() + ")", null, m.getAvatarUrl())
                                .setDescription(LangID.getStringByID("scamDetector.report.onlyMember", h.config.lang).replace("_", link))
                                .build();
                    } else {
                        embed = new EmbedBuilder()
                                .setTitle(LangID.getStringByID("scamDetector.compromisedReport", h.config.lang))
                                .setAuthor(m.getEffectiveName() + " (" + m.getId() + ")", null, m.getAvatarUrl())
                                .setDescription(LangID.getStringByID("scamDetector.report.allUsers", h.config.lang).replace("_", link))
                                .build();
                    }

                    ((MessageChannel) cha).sendMessageEmbeds(embed).queue();
                }
            } else {
                if (me != null) {
                    GuildChannel cha = gu.getGuildChannelById(handler.channel);

                    if (cha instanceof MessageChannel) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setTitle(LangID.getStringByID("scamDetector.compromisedReport", h.config.lang))
                                .setAuthor(m.getEffectiveName() + " (" + m.getId() + ")", null, m.getAvatarUrl())
                                .setDescription(LangID.getStringByID("scamDetector.report.onlyMember", h.config.lang).replace("_", link))
                                .build();

                        ((MessageChannel) cha).sendMessageEmbeds(embed).queue();
                    }
                }
            }
        }
    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("author", author);
        obj.addProperty("server", server);
        obj.addProperty("channel", channel);
        obj.addProperty("noticeAll", noticeAll);

        if (mute != null) {
            obj.addProperty("scamDetector.action.mute", mute);
        }

        switch (action) {
            case MUTE -> obj.addProperty("action", "scamDetector.action.mute");
            case KICK -> obj.addProperty("action", "scamDetector.action.kick");
            case BAN -> obj.addProperty("action", "scamDetector.action.ban");
        }

        return obj;
    }

    public String getChannel() {
        return channel;
    }
}
