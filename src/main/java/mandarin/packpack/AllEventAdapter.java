package mandarin.packpack;

import common.CommonStatic;
import common.util.Data;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import common.util.unit.Form;
import mandarin.packpack.commands.*;
import mandarin.packpack.commands.bc.*;
import mandarin.packpack.commands.bot.*;
import mandarin.packpack.commands.bot.manage.ClearCache;
import mandarin.packpack.commands.bot.manage.RegisterFixing;
import mandarin.packpack.commands.bot.manage.UnregisterFixing;
import mandarin.packpack.commands.data.*;
import mandarin.packpack.commands.math.*;
import mandarin.packpack.commands.server.*;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.*;
import mandarin.packpack.supporter.server.holder.HolderHub;
import mandarin.packpack.supporter.server.slash.SlashBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.GenericChannelUpdateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AllEventAdapter extends ListenerAdapter {
    private static boolean readyDone = false;

    @Override
    public void onGuildLeave(@Nonnull GuildLeaveEvent event) {
        super.onGuildLeave(event);

        try {
            Guild g = event.getGuild();

            StaticStore.logger.uploadLog("Left server : "+g.getName()+ " ("+g.getId()+")");

            StaticStore.idHolder.remove(g.getId());

            StaticStore.holders.values().forEach(hub -> hub.handleGuildDelete(g.getId()));

            StaticStore.saveServerInfo();

            StaticStore.updateStatus();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildLeave - Error happened");
        }
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
        super.onGuildJoin(event);

        try {
            Guild g = event.getGuild();

            StaticStore.logger.uploadLog("Joined server : "+g.getName()+" ("+g.getId()+")"+"\nSize : "+g.getMemberCount());

            IDHolder holder = StaticStore.idHolder.computeIfAbsent(g.getId(), k -> new IDHolder(g));

            StaticStore.saveServerInfo();

            StaticStore.updateStatus();

            findInviter(g).queue(m ->
                m.getUser().openPrivateChannel().queue(ch ->
                        ch.sendMessage(LangID.getStringByID("bot.directMessage.invitation", holder.config.lang).formatted(g.getName())).queue(null, e -> {
                            if (e instanceof ErrorResponseException err && err.getErrorCode() == 50007) {
                                return;
                            }

                            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildJoin - Failed to send message to inviter");
                        }),
                    e ->
                        StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildJoin - Failed to open private channel to inviter")
                )
            );
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildJoin - Error happened");
        }
    }

    @Override
    public void onRoleDelete(@Nonnull RoleDeleteEvent event) {
        super.onRoleDelete(event);

        try {
            Guild g = event.getGuild();
            String roleID = event.getRole().getId();

            IDHolder holder = StaticStore.idHolder.computeIfAbsent(g.getId(), k -> new IDHolder(g));

            if (roleID.equals(holder.moderator)) {
                holder.moderator = null;
            }

            if (roleID.equals(holder.member)) {
                holder.member = null;
            }

            if (roleID.equals(holder.booster)) {
                holder.booster = null;
            }

            holder.ID.entrySet().removeIf(entry -> roleID.equals(entry.getValue()));

            StaticStore.saveServerInfo();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onRoleDelete - Error happened");
        }
    }

    @Override
    public void onGenericChannelUpdate(@Nonnull GenericChannelUpdateEvent<?> event) {
        ChannelUnion channel = event.getChannel();

        if (channel instanceof MessageChannel mc && !mc.canTalk()) {
            StaticStore.holders.values().forEach(hub -> hub.handleChannelDelete(mc.getId()));
        }
    }

    @Override
    public void onChannelDelete(@Nonnull ChannelDeleteEvent event) {
        super.onChannelDelete(event);

        try {
            Guild g = event.getGuild();
            Channel ch = event.getChannel();

            IDHolder idh = StaticStore.idHolder.get(g.getId());

            if(idh == null)
                return;

            if(idh.announceChannel != null && idh.announceChannel.equals(ch.getId()))
                idh.announceChannel = null;

            for(CommonStatic.Lang.Locale key : idh.eventData.keySet()) {
                EventDataConfigHolder config = idh.eventData.get(key);

                if(config != null) {
                    if (config.channelID == ch.getIdLong()) {
                        config.channelID = -1L;
                    }

                    if (config.newVersionChannelID == ch.getIdLong()) {
                        config.newVersionChannelID = -1L;
                    }
                } else {
                    idh.eventData.remove(key);
                }
            }

            if(idh.logDM != null && idh.logDM.equals(ch.getId()))
                idh.logDM = null;

            idh.status.remove(ch.getId());

            StaticStore.idHolder.put(g.getId(), idh);

            if(StaticStore.scamLinkHandlers.servers.containsKey(g.getId())) {
                String channel = StaticStore.scamLinkHandlers.servers.get(g.getId()).getChannel();

                if(channel != null && channel.equals(ch.getId())) {
                    StaticStore.scamLinkHandlers.servers.remove(g.getId());
                }
            }

            idh.boosterPinChannel.remove(ch.getId());

            StaticStore.holders.values().forEach(hub -> hub.handleChannelDelete(ch.getId()));
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onChannelDelete - Error happened");
        }
    }

    @Override
    public void onGuildMemberUpdate(@Nonnull GuildMemberUpdateEvent event) {
        super.onGuildMemberUpdate(event);

        try {
            Member m = event.getMember();

            if(m.getUser().isBot())
                return;

            Guild g = event.getGuild();

            if(!StaticStore.idHolder.containsKey(g.getId()))
                return;

            IDHolder holder = StaticStore.idHolder.get(g.getId());

            if(holder.booster == null)
                return;

            if(!StaticStore.boosterData.containsKey(g.getId()))
                return;

            BoosterHolder booster = StaticStore.boosterData.get(g.getId());

            if(!booster.serverBooster.containsKey(m.getId()))
                return;

            BoosterData data = booster.serverBooster.get(m.getId());

            if(!StaticStore.rolesToString(m.getRoles()).contains(holder.booster)) {
                String role = data.getRole();
                String emoji = data.getEmoji();

                if(role != null) {
                    Role r = g.getRoleById(role);

                    if(r != null) {
                        r.delete().queue();
                    }
                }

                if(emoji != null) {
                    RichCustomEmoji e = g.getEmojiById(emoji);

                    if(e != null) {
                        e.delete().queue();
                    }
                }

                booster.serverBooster.remove(m.getId());
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildMemberUpdate - Error happened");
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        try {
            super.onMessageReceived(event);

            User u = event.getAuthor();

            if(u.getId().equals(event.getJDA().getSelfUser().getId()) || u.isBot())
                return;

            if(StaticStore.optoutMembers.contains(u.getId())) {
                return;
            }

            MessageChannel mc = event.getChannel();
            Message msg = event.getMessage();

            boolean mandarin = u.getId().equals(StaticStore.MANDARIN_SMELL);

            String prefix = StaticStore.getPrefix(u.getId());

            if(msg.getContentRaw().toLowerCase(java.util.Locale.ENGLISH).startsWith(StaticStore.globalPrefix))
                prefix = StaticStore.globalPrefix;

            CommonStatic.Lang.Locale lang = null;

            ConfigHolder c;

            if(StaticStore.config.containsKey(u.getId())) {
                lang = StaticStore.config.get(u.getId()).lang;
                c = StaticStore.config.get(u.getId());
            } else {
                c = null;
            }

            if(StaticStore.holderContainsKey(u.getId())) {
                HolderHub holder = StaticStore.getHolderHub(u.getId());

                holder.handleEvent(event);
            }

            if(mc instanceof PrivateChannel) {
                SelfUser self = event.getJDA().getSelfUser();

                if(event.getAuthor().getId().equals(self.getId())) {
                    return;
                }

                String content = msg.getContentRaw();

                if(content.contains("http")) {
                    if(content.length() > 1000) {
                        content = content.substring(0, 997)+"...";
                    }

                    if(!StaticStore.optoutMembers.contains(msg.getAuthor().getId())) {
                        notifyModerators(event.getJDA(), msg.getAuthor(), content);
                    }
                }

                if(lang == null)
                    lang = CommonStatic.Lang.Locale.EN;

                performCommand(event, msg, lang, prefix, null, c);
            } else if(mc instanceof GuildChannel) {
                Member m = event.getMember();

                if(m == null)
                    return;

                Guild g = event.getGuild();

                if(!u.isBot() && !StaticStore.optoutMembers.contains(u.getId()) && StaticStore.scamLinkHandlers.servers.containsKey(g.getId()) && ScamLinkHandler.validScammingUser(msg.getContentRaw())) {
                    String link = ScamLinkHandler.getLinkFromMessage(msg.getContentRaw());

                    if(link != null) {
                        link = link.replace("http://", "").replace("https://", "");
                    }

                    StaticStore.scamLinkHandlers.servers.get(g.getId()).takeAction(link, m, g);
                    StaticStore.logger.uploadLog("I caught compromised user\nLINK : "+link+"\nGUILD : "+g.getName()+" ("+g.getId()+")\nMEMBER : "+m.getEffectiveName()+" ("+u.getId()+")");

                    msg.delete().queue();
                }

                IDHolder idh = StaticStore.idHolder.computeIfAbsent(g.getId(), k -> new IDHolder(g));

                String userPrefix = StaticStore.getPrefix(u.getId()).toLowerCase(java.util.Locale.ENGLISH);

                if (idh.disableCustomPrefix && !prefix.equals(StaticStore.globalPrefix) && userPrefix.equals(prefix.toLowerCase(java.util.Locale.ENGLISH))) {
                    final CommonStatic.Lang.Locale finalLocale = lang;

                    m.getUser().openPrivateChannel().queue(pc ->
                            pc.sendMessage(LangID.getStringByID("bot.denied.reason.prefixBanned.all", finalLocale).formatted(g.getName(), StaticStore.globalPrefix)).queue(null, e ->
                                    StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onMessageReceived - Failed to send prefix banned DM to user")
                            ), e -> {}
                    );

                    return;
                } else if (idh.bannedPrefix.contains(prefix.toLowerCase(java.util.Locale.ENGLISH)) && userPrefix.equals(prefix.toLowerCase(java.util.Locale.ENGLISH))) {
                    final CommonStatic.Lang.Locale finalLocale = lang;
                    final String finalPrefix = prefix;

                    m.getUser().openPrivateChannel().queue(pc ->
                            pc.sendMessage(LangID.getStringByID("bot.denied.reason.prefixBanned.specific", finalLocale).formatted(g.getName(), finalPrefix, StaticStore.globalPrefix, StaticStore.globalPrefix)).queue(null, e ->
                                    StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onMessageReceived - Failed to send prefix banned DM to user")
                            ), e -> {}
                    );

                    return;
                }

                boolean isMod;

                String moderatorID = idh.moderator;
                List<Role> roles = m.getRoles();

                if (moderatorID != null) {
                    isMod = roles.stream().anyMatch(r -> r.getId().equals(moderatorID)) || m.isOwner();
                } else {
                    isMod = m.hasPermission(Permission.MANAGE_SERVER) || m.hasPermission(Permission.ADMINISTRATOR) || m.isOwner();
                }

                boolean channelPermitted = false;

                ArrayList<String> channels = idh.getAllAllowedChannels(m);

                if(channels == null)
                    channelPermitted = true;
                else if(!channels.isEmpty()) {
                    if (mc instanceof ThreadChannel tc) {
                        IThreadContainerUnion parent = tc.getParentChannel();

                        channelPermitted = channels.contains(tc.getId());

                        if (parent instanceof ForumChannel) {
                            channelPermitted |= channels.contains(parent.getId());
                        }
                    } else {
                        channelPermitted = channels.contains(mc.getId());
                    }
                }

                if(!mandarin && !isMod && !channelPermitted)
                    return;

                if(!mandarin && idh.banned.contains(u.getId()))
                    return;

                if(msg.getContentRaw().toLowerCase(java.util.Locale.ENGLISH).startsWith(idh.config.prefix))
                    prefix = idh.config.prefix;

                if(lang == null)
                    lang = idh.config.lang;

                performCommand(event, msg, lang, prefix, idh, c);
            }
        } catch (Exception e) {
            MessageChannel ch = event.getChannel();
            Message msg = event.getMessage();
            Member m = event.getMember();

            String data = "Command : " + msg.getContentRaw();

            if(ch instanceof GuildMessageChannel) {
                Guild g = event.getGuild();

                data += "\n\n" + "Guild : " + g.getName() + " (" + g.getId() + ")";
            }

            if(m != null) {
                data += "\n\nMember  : " + m.getEffectiveName() + " (" + m.getId() + ")";
            } else {
                User u = event.getAuthor();

                data += "\n\nUser : " + u.getName() + " (" + u.getId() + ")";
            }

            data += "\n\nChannel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

            StaticStore.logger.uploadErrorLog(e, "Failed to perform command : "+this.getClass()+"\n\n" + data);
        }
    }

    public void performCommand(MessageReceivedEvent event, Message msg, CommonStatic.Lang.Locale lang, String prefix, @Nullable IDHolder idh, @Nullable ConfigHolder c) {
        switch (StaticStore.getCommand(msg.getContentRaw(), prefix)) {
            case "serverstat", "ss" -> new ServerStat(lang, idh).execute(event);
            case "analyze" -> new Analyze(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "help" -> new Help(lang, idh).execute(event);
            case "prefix" -> new Prefix(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "serverpre" -> new ServerPrefix(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "save" -> new Save(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "stimg", "stimage", "stageimg", "stageimage" ->
                    new StageImage(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "stmimg", "stmimage", "stagemapimg", "stagemapimage" ->
                    new StmImage(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "formstat", "fs", "catstat", "cs", "unitstat", "us" ->
                    new FormStat(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "locale", "loc" -> new Locale(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "music", "ms" -> new Music(ConstraintCommand.ROLE.MEMBER, lang, idh, "music_").execute(event);
            case "enemystat", "es" -> new EnemyStat(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "castle", "cas" -> new Castle(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "stageinfo", "si" -> new StageInfo(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 5000).execute(event);
            case "memory", "mm" -> new Memory(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "formimage", "formimg", "fimage", "fimg", "catimage", "catimg", "cimage", "cimg", "unitimage", "unitimg", "uimage", "uimg" ->
                    new FormImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "enemyimage", "enemyimg", "eimage", "eimg" ->
                    new EnemyImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "background", "bg" -> new Background(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "test" -> new Test(ConstraintCommand.ROLE.MANDARIN, lang, idh, "test").execute(event);
            case "formgif", "fgif", "fg", "catgif", "cgif", "cg", "unitgif", "ugif", "ug" ->
                    new FormGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
            case "enemygif", "egif", "eg" ->
                    new EnemyGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
            case "idset" -> new IDSet(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "clearcache" -> new ClearCache(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "aa", "animanalyzer" -> new AnimAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "channelpermission", "channelperm", "chpermission", "chperm", "chp" ->
                    new ChannelPermission(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "formsprite", "fsprite", "formsp", "fsp", "catsprite", "csprite", "catsp", "csp", "unitsprite", "usprite", "unitsp", "usp" ->
                    new FormSprite(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.SECONDS.toMillis(10)).execute(event);
            case "enemysprite", "esprite", "enemysp", "esp" ->
                    new EnemySprite(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.SECONDS.toMillis(10)).execute(event);
            case "medal", "md" -> new Medal(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "announcement", "ann" -> new Announcement(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "catcombo", "combo", "cc" -> new CatCombo(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "serverjson", "json", "sj" -> new ServerJson(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "findstage", "findst", "fstage", "fst" ->
                    new FindStage(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 5000).execute(event);
            case "suggest" ->
                    new Suggest(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.MINUTES.toMillis(60)).execute(event);
            case "suggestban", "sgb" -> new SuggestBan(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "suggestunban", "sgub" -> new SuggestUnban(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "suggestresponse", "sgr" ->
                    new SuggestResponse(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "alias", "al" -> new Alias(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "aliasadd", "ala" -> new AliasAdd(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "aliasremove", "alr" -> new AliasRemove(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "contributoradd", "coa" ->
                    new ContributorAdd(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "contributorremove", "cor" ->
                    new ContributorRemove(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "statistic", "stat" -> new Statistic(lang).execute(event);
            case "serverlocale", "serverloc", "sloc" ->
                    new ServerLocale(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "news" -> new News(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "publish", "pub" -> new Publish(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "boosterrole", "boosterr", "brole", "br" ->
                    new BoosterRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "boosterroleremove", "brremove", "boosterrolerem", "brrem", "brr" ->
                    new BoosterRoleRemove(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "boosteremoji", "boostere", "bemoji", "be" ->
                    new BoosterEmoji(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "boosteremojiremove", "beremove", "boosteremojirem", "berem", "ber" ->
                    new BoosterEmojiRemove(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "registerlogging", "rlogging", "registerl", "rl" ->
                    new RegisterLogging(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "unregisterlogging", "urlogging", "unregisterl", "url" ->
                    new UnregisterLogging(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "setup" -> new Setup(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "registerfixing", "rf" ->
                    new RegisterFixing(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "unregisterfixing", "urf" ->
                    new UnregisterFixing(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "checkeventupdate", "ceu" ->
                    new CheckEventUpdate(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "printstageevent", "pse" ->
                    new PrintStageEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "subscribeevent", "se" -> new SubscribeEvent(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "printgachaevent", "pge" ->
                    new PrintGachaEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "bcversion", "bv" -> new BCVersion(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "logout", "lo" -> new LogOut(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "printitemevent", "pie" -> new PrintItemEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "printevent", "pe" -> new PrintEvent(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "statanalyzer", "sa" -> new StatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "addscamlinkhelpingserver", "aslhs", "ashs" ->
                    new AddScamLinkHelpingServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "removescamlinkhelpingserver", "rslhs", "rshs" ->
                    new RemoveScamLinkHelpingServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "registerscamlink", "rsl" ->
                    new RegisterScamLink(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "unregisterscamlink", "usl" ->
                    new UnregisterScamLink(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "subscribescamlinkdetector", "ssld", "ssd" ->
                    new SubscribeScamLinkDetector(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "unsubscribescamlinkdetector", "usld", "usd" ->
                    new UnsubscribeScamLinkDetector(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "timezone", "tz" -> new TimeZone(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "optout" -> new OptOut(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "config" -> new Config(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "removecache", "rc" -> new RemoveCache(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "sendmessage", "sm" -> new SendMessage(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "analyzeserver", "as" -> new AnalyzeServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "cleartemp", "ct" -> new ClearTemp(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "downloadapk", "da" -> new DownloadApk(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "trueformanalyzer", "tfanalyzer", "trueforma", "tfa" ->
                    new TrueFormAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "enemystatanalyzer", "estatanalyzer", "enemysa", "esa" ->
                    new EnemyStatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "stagestatanalyzer", "sstatanalyzer", "stagesa", "ssa" ->
                    new StageStatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "serverconfig", "sconfig", "serverc", "sc" -> {
                if (idh != null) {
                    new ServerConfig(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                }
            }
            case "talentinfo", "ti" -> new TalentInfo(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "soul", "sl" -> new Soul(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
            case "soulimage", "soulimg", "simage", "simg" ->
                    new SoulImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "soulsprite", "ssprite", "soulsp", "ssp" ->
                    new SoulSprite(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "sayhi", "hi" -> new SayHi(lang).execute(event);
            case "calculator", "calc", "c" -> new Calculator(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "assetbrowser", "abroswer", "assetb", "ab" ->
                    new AssetBrowser(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "garbagecollect", "gc" ->
                    new GarbageCollect(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "findreward", "freward", "findr", "fr" ->
                    new FindReward(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000, c).execute(event);
            case "eventdataarchive", "eventddataa", "earchive", "eda" ->
                    new EventDataArchive(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "talanetanalyzer", "tala", "ta" ->
                    new TalentAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "catcomboanalyzer", "comboanalyzer", "cca", "ca" ->
                    new ComboAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "say", "s" -> new Say(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "plot", "p" -> new Plot(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "tplot", "tp" -> new TPlot(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "solve", "sv" -> new Solve(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "differentiate", "diff", "dx" ->
                    new Differentiate(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "integrate", "int" -> new Integrate(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "plotrtheta", "plotrt", "prtheta", "prt", "rt" ->
                    new RTheta(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "getid", "gi" -> new GetID(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "donate", "donation", "don" -> new Donate(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "grablanguage", "gl" -> new GrabLanguage(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "reloadlanguage", "rel" ->
                    new ReloadLanguage(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "reactto", "ret" -> new ReactTo(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "react", "r" -> new React(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "treasure", "tr" -> new Treasure(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "boosterpin", "bp" -> new BoosterPin(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "switcheventgrabber", "seg" -> new SwitchEventGrabber(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "formdps", "catdps", "unitdps", "fdps", "cdps", "udps", "fd", "cd", "ud" -> new FormDPS(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 10000).execute(event);
            case "enemydps", "edps", "ed" -> new EnemyDPS(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "dumpheap", "dump", "heap", "dh" -> new DumpHeap(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "writelog", "wl" -> new WriteLog(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "zeroformanalyzer", "zfanalyzer", "zeroforma", "zfa" -> new ZeroFormAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "hasrole", "hr" -> new HasRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "optin", "oi" -> new OptIn(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "addmaintainer", "am" -> new AddMaintainer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "removemaintainer", "rm" -> new RemoveMaintainer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
        }
    }

    public void performCommand(GenericInteractionCreateEvent event, String command, CommonStatic.Lang.Locale lang, @Nullable IDHolder idh, @Nullable ConfigHolder c) {
        switch (command) {
            case "serverstat", "ss" -> new ServerStat(lang, idh).execute(event);
            case "analyze" -> new Analyze(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "help" -> new Help(lang, idh).execute(event);
            case "prefix" -> new Prefix(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "serverpre" -> new ServerPrefix(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "save" -> new Save(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "stimg", "stimage", "stageimg", "stageimage" ->
                    new StageImage(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "stmimg", "stmimage", "stagemapimg", "stagemapimage" ->
                    new StmImage(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "formstat", "fs", "catstat", "cs", "unitstat", "us" ->
                    new FormStat(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "locale", "loc" -> new Locale(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "music", "ms" -> new Music(ConstraintCommand.ROLE.MEMBER, lang, idh, "music_").execute(event);
            case "enemystat", "es" -> new EnemyStat(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "castle", "cas" -> new Castle(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "stageinfo", "si" -> new StageInfo(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 5000).execute(event);
            case "memory", "mm" -> new Memory(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "formimage", "formimg", "fimage", "fimg", "catimage", "catimg", "cimage", "cimg", "unitimage", "unitimg", "uimage", "uimg" ->
                    new FormImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "enemyimage", "enemyimg", "eimage", "eimg" ->
                    new EnemyImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "background", "bg" -> new Background(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "test" -> new Test(ConstraintCommand.ROLE.MANDARIN, lang, idh, "test").execute(event);
            case "formgif", "fgif", "fg", "catgif", "cgif", "cg", "unitgif", "ugif", "ug" ->
                    new FormGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
            case "enemygif", "egif", "eg" ->
                    new EnemyGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
            case "idset" -> new IDSet(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "clearcache" -> new ClearCache(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "aa", "animanalyzer" -> new AnimAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "channelpermission", "channelperm", "chpermission", "chperm", "chp" ->
                    new ChannelPermission(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "formsprite", "fsprite", "formsp", "fsp", "catsprite", "csprite", "catsp", "csp", "unitsprite", "usprite", "unitsp", "usp" ->
                    new FormSprite(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.SECONDS.toMillis(10)).execute(event);
            case "enemysprite", "esprite", "enemysp", "esp" ->
                    new EnemySprite(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.SECONDS.toMillis(10)).execute(event);
            case "medal", "md" -> new Medal(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "announcement", "ann" -> new Announcement(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "catcombo", "combo", "cc" -> new CatCombo(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "serverjson", "json", "sj" -> new ServerJson(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "findstage", "findst", "fstage", "fst" ->
                    new FindStage(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 5000).execute(event);
            case "suggest" ->
                    new Suggest(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.MINUTES.toMillis(60)).execute(event);
            case "suggestban", "sgb" -> new SuggestBan(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "suggestunban", "sgub" -> new SuggestUnban(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "suggestresponse", "sgr" ->
                    new SuggestResponse(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "alias", "al" -> new Alias(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "aliasadd", "ala" -> new AliasAdd(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "aliasremove", "alr" -> new AliasRemove(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "contributoradd", "coa" ->
                    new ContributorAdd(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "contributorremove", "cor" ->
                    new ContributorRemove(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "statistic", "stat" -> new Statistic(lang).execute(event);
            case "serverlocale", "serverloc", "sloc" ->
                    new ServerLocale(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "news" -> new News(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "publish", "pub" -> new Publish(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "boosterrole", "boosterr", "brole", "br" ->
                    new BoosterRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "boosterroleremove", "brremove", "boosterrolerem", "brrem", "brr" ->
                    new BoosterRoleRemove(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "boosteremoji", "boostere", "bemoji", "be" ->
                    new BoosterEmoji(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "boosteremojiremove", "beremove", "boosteremojirem", "berem", "ber" ->
                    new BoosterEmojiRemove(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "registerlogging", "rlogging", "registerl", "rl" ->
                    new RegisterLogging(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "unregisterlogging", "urlogging", "unregisterl", "url" ->
                    new UnregisterLogging(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "setup" -> new Setup(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "registerfixing", "rf" ->
                    new RegisterFixing(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "unregisterfixing", "urf" ->
                    new UnregisterFixing(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "checkeventupdate", "ceu" ->
                    new CheckEventUpdate(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "printstageevent", "pse" ->
                    new PrintStageEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "subscribeevent", "se" -> new SubscribeEvent(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "printgachaevent", "pge" ->
                    new PrintGachaEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "bcversion", "bv" -> new BCVersion(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "logout", "lo" -> new LogOut(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "printitemevent", "pie" -> new PrintItemEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "printevent", "pe" -> new PrintEvent(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "statanalyzer", "sa" -> new StatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "addscamlinkhelpingserver", "aslhs", "ashs" ->
                    new AddScamLinkHelpingServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "removescamlinkhelpingserver", "rslhs", "rshs" ->
                    new RemoveScamLinkHelpingServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "registerscamlink", "rsl" ->
                    new RegisterScamLink(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "unregisterscamlink", "usl" ->
                    new UnregisterScamLink(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "subscribescamlinkdetector", "ssld", "ssd" ->
                    new SubscribeScamLinkDetector(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "unsubscribescamlinkdetector", "usld", "usd" ->
                    new UnsubscribeScamLinkDetector(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "timezone", "tz" -> new TimeZone(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "optout" -> new OptOut(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "config" -> new Config(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "removecache", "rc" -> new RemoveCache(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "sendmessage", "sm" -> new SendMessage(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "analyzeserver", "as" -> new AnalyzeServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "cleartemp", "ct" -> new ClearTemp(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "downloadapk", "da" -> new DownloadApk(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "trueformanalyzer", "tfanalyzer", "trueforma", "tfa" ->
                    new TrueFormAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "enemystatanalyzer", "estatanalyzer", "enemysa", "esa" ->
                    new EnemyStatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "stagestatanalyzer", "sstatanalyzer", "stagesa", "ssa" ->
                    new StageStatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "serverconfig", "sconfig", "serverc", "sc" -> {
                if (idh != null) {
                    new ServerConfig(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                }
            }
            case "talentinfo", "ti" -> new TalentInfo(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
            case "soul", "sl" -> new Soul(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
            case "soulimage", "soulimg", "simage", "simg" ->
                    new SoulImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "soulsprite", "ssprite", "soulsp", "ssp" ->
                    new SoulSprite(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "sayhi", "hi" -> new SayHi(lang).execute(event);
            case "calculator", "calc", "c" -> new Calculator(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "assetbrowser", "abroswer", "assetb", "ab" ->
                    new AssetBrowser(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "garbagecollect", "gc" ->
                    new GarbageCollect(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "findreward", "freward", "findr", "fr" ->
                    new FindReward(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000, c).execute(event);
            case "eventdataarchive", "eventddataa", "earchive", "eda" ->
                    new EventDataArchive(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "talanetanalyzer", "tala", "ta" ->
                    new TalentAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "catcomboanalyzer", "comboanalyzer", "cca", "ca" ->
                    new ComboAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "say", "s" -> new Say(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "plot", "p" -> new Plot(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "tplot", "tp" -> new TPlot(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "solve", "sv" -> new Solve(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "differentiate", "diff", "dx" ->
                    new Differentiate(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "integrate", "int" -> new Integrate(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "plotrtheta", "plotrt", "prtheta", "prt", "rt" ->
                    new RTheta(ConstraintCommand.ROLE.MEMBER, lang, idh, 30000).execute(event);
            case "getid", "gi" -> new GetID(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "donate", "donation", "don" -> new Donate(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "grablanguage", "gl" -> new GrabLanguage(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "reloadlanguage", "rel" ->
                    new ReloadLanguage(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "reactto", "ret" -> new ReactTo(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "react", "r" -> new React(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "treasure", "tr" -> new Treasure(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "boosterpin", "bp" -> new BoosterPin(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
            case "switcheventgrabber", "seg" -> new SwitchEventGrabber(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "formdps", "catdps", "unitdps", "fdps", "cdps", "udps", "fd", "cd", "ud" -> new FormDPS(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 10000).execute(event);
            case "enemydps", "edps", "ed" -> new EnemyDPS(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
            case "dumpheap", "dump", "heap", "dh" -> new DumpHeap(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "writelog", "wl" -> new WriteLog(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "zeroformanalyzer", "zfanalyzer", "zeroforma", "zfa" -> new ZeroFormAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "hasrole", "hr" -> new HasRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "optin", "oi" -> new OptIn(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "addmaintainer", "am" -> new AddMaintainer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
            case "removemaintainer", "rm" -> new RemoveMaintainer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
        }
    }

    @Override
    public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
        super.onMessageDelete(event);

        String messageID = event.getMessageId();

        StaticStore.holders.values().forEach(hub -> hub.handleMessageDelete(messageID));
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        super.onMessageReactionAdd(event);

        try {
            User u = event.getUser();

            if(u == null)
                return;

            if(StaticStore.holderContainsKey(u.getId())) {
                HolderHub hub = StaticStore.getHolderHub(u.getId());

                hub.handleEvent(event);
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onMessageReactionAdd - Error happened");
        }
    }

    @Override
    public void onGenericInteractionCreate(@Nonnull GenericInteractionCreateEvent event) {
        super.onGenericInteractionCreate(event);

        User u = event.getUser();

        try {
            switch (event) {
                case GenericComponentInteractionCreateEvent c -> {
                    if (StaticStore.holderContainsKey(u.getId())) {
                        HolderHub holder = StaticStore.getHolderHub(u.getId());

                        holder.handleEvent(c);
                    }
                }
                case ModalInteractionEvent m -> {
                    if (StaticStore.holderContainsKey(u.getId())) {
                        HolderHub holder = StaticStore.getHolderHub(u.getId());

                        holder.handleEvent(m);
                    }
                }
                case GenericCommandInteractionEvent i -> {
                    if (StaticStore.spamData.containsKey(u.getId())) {
                        SpamPrevent spam = StaticStore.spamData.get(u.getId());

                        String result = spam.isPrevented(event);

                        if (result != null) {
                            if (!result.isBlank()) {
                                i.deferReply().setContent(result).queue();
                            }

                            return;
                        }
                    }

                    Guild g = event.getGuild();
                    MessageChannel mc = event.getMessageChannel();

                    IDHolder idh;
                    boolean mandarin = u.getId().equals(StaticStore.MANDARIN_SMELL);

                    if (g == null) {
                        idh = null;
                    } else {
                        idh = StaticStore.idHolder.computeIfAbsent(g.getId(), k -> new IDHolder(g));
                    }

                    boolean channelPermitted = false;

                    if (idh == null) {
                        channelPermitted = true;
                    } else {
                        Member m = event.getMember();

                        if (m != null) {
                            ArrayList<String> channels = idh.getAllAllowedChannels(m);

                            if(channels == null)
                                channelPermitted = true;
                            else if(!channels.isEmpty()) {
                                if (mc instanceof ThreadChannel tc) {
                                    IThreadContainerUnion parent = tc.getParentChannel();

                                    channelPermitted = channels.contains(tc.getId());

                                    if (parent instanceof ForumChannel) {
                                        channelPermitted |= channels.contains(parent.getId());
                                    }
                                } else {
                                    channelPermitted = channels.contains(mc.getId());
                                }
                            }
                        }
                    }

                    ConfigHolder c = StaticStore.config.get(event.getUser().getId());

                    CommonStatic.Lang.Locale lang;

                    if (c == null || c.lang == null) {
                        if (idh == null) {
                            lang = CommonStatic.Lang.Locale.EN;
                        } else {
                            lang = idh.config.lang;
                        }
                    } else {
                        lang = c.lang;
                    }

                    if (!mandarin && !channelPermitted) {
                        i.deferReply()
                                .setContent(LangID.getStringByID("bot.denied.reason.channel", lang))
                                .setEphemeral(true)
                                .queue();

                        return;
                    }

                    if (!mandarin && idh != null && idh.banned.contains(u.getId())) {
                        i.deferReply()
                                .setContent(LangID.getStringByID("bot.denied.reason.banned", lang))
                                .setEphemeral(true)
                                .queue();

                        return;
                    }

                    performCommand(event, i.getName(), lang, idh, c);
                }
                default -> {
                }
            }
        } catch (Exception e) {
            String message = "E/AllEventAdapter::onGenericInteractionCreate - Error happened";

            if(StaticStore.holderContainsKey(u.getId())) {
                message += "\n\nTried to handle the holder : " + StaticStore.getHolderHub(u.getId()).getClass().getName();

                HolderHub hub = StaticStore.getHolderHub(u.getId());

                if(hub.componentHolder != null) {
                    Message author = hub.componentHolder.getAuthorMessage();

                    MessageChannel ch = author.getChannel();

                    message += "\n\nCommand : " + author.getContentRaw() + "\n\n" +
                            "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                    if(ch instanceof GuildChannel) {
                        Guild g = author.getGuild();

                        message += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                    }
                }
            }

            StaticStore.logger.uploadErrorLog(e, message);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        super.onCommandAutoCompleteInteraction(event);

        String[] allowedCommands = {
                "edps", "es", "fdps", "fs", "si", "ti"
        };

        if (!ArrayUtils.contains(allowedCommands, event.getInteraction().getName())) {
            return;
        }

        Guild g = event.getGuild();

        IDHolder holder;

        if (g == null) {
            holder = null;
        } else {
            holder = StaticStore.idHolder.get(g.getId());
        }

        ConfigHolder config = StaticStore.config.get(event.getUser().getId());

        CommonStatic.Lang.Locale locale;

        if (config != null && config.lang != null) {
            locale = config.lang;
        } else {
            locale = holder == null ? CommonStatic.Lang.Locale.EN : holder.config.lang;
        }
        switch (event.getInteraction().getName()) {
            case "fdps", "fs", "ti" -> {
                String name = event.getOptions().stream().filter(o -> o.getName().equals("name") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");

                List<Form> forms = EntityFilter.findUnitWithName(name, event.getInteraction().getName().equals("ti" ) || (config != null && config.trueForm), locale);

                if (forms.isEmpty()) {
                    event.replyChoices().queue();
                } else {
                    List<Command.Choice> choices = new ArrayList<>();

                    for (int i = 0; i < Math.min(forms.size(), OptionData.MAX_CHOICES); i++) {
                        String formName = StaticStore.safeMultiLangGet(forms.get(i), locale);

                        if (formName == null || formName.isBlank()) {
                            formName = forms.get(i).names.toString();
                        }

                        if (formName.isBlank()) {
                            formName = Data.trio(forms.get(i).unit.id.id) + "-" + Data.trio(forms.get(i).fid);
                        }

                        choices.add(new Command.Choice(formName, Data.trio(forms.get(i).unit.id.id) + "-" + Data.trio(forms.get(i).fid)));
                    }

                    event.replyChoices(choices).queue();
                }
            }
            case "edps", "es" -> {
                String name = event.getOptions().stream().filter(o -> o.getName().equals("name") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");

                List<Enemy> enemies = EntityFilter.findEnemyWithName(name, locale);

                if (enemies.isEmpty()) {
                    event.replyChoices().queue();
                } else {
                    List<Command.Choice> choices = new ArrayList<>();

                    for (int i = 0; i < Math.min(enemies.size(), OptionData.MAX_CHOICES); i++) {
                        String enemyName = StaticStore.safeMultiLangGet(enemies.get(i), locale);

                        if (enemyName == null || enemyName.isBlank()) {
                            enemyName = enemies.get(i).names.toString();
                        }

                        if (enemyName.isBlank()) {
                            enemyName = Data.trio(enemies.get(i).id.id) ;
                        }

                        choices.add(new Command.Choice(enemyName, Data.trio(enemies.get(i).id.id)));
                    }

                    event.replyChoices(choices).queue();
                }
            }
            case "si" -> {
                switch(event.getInteraction().getFocusedOption().getName()) {
                    case "name" -> {
                        String[] names = new String[3];

                        names[0] = event.getOptions().stream().filter(o -> o.getName().equals("map_collection") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");
                        names[1] = event.getOptions().stream().filter(o -> o.getName().equals("stage_map") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");
                        names[2] = event.getOptions().stream().filter(o -> o.getName().equals("name") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");

                        List<Stage> stages = EntityFilter.findStageWithName(names, locale);

                        if (stages.isEmpty()) {
                            event.replyChoices().queue();
                        } else {
                            List<Command.Choice> choices = new ArrayList<>();

                            for (int i = 0; i < Math.min(stages.size(), OptionData.MAX_CHOICES); i++) {
                                Stage s = stages.get(i);

                                String stageName = StaticStore.safeMultiLangGet(s, locale);
                                String stageMapName = StaticStore.safeMultiLangGet(s.getCont(), locale);
                                String mapCollectionName = StaticStore.safeMultiLangGet(s.getCont().getCont(), locale);

                                if (stageName == null || stageName.isBlank()) {
                                    stageName = s.names.toString();
                                }

                                if (stageMapName == null || stageMapName.isBlank()) {
                                    stageMapName = s.getCont().names.toString();
                                }

                                if (mapCollectionName == null || mapCollectionName.isBlank()) {
                                    mapCollectionName = s.getCont().getCont().getSID();
                                }

                                if (stageName.isBlank() && s.id != null) {
                                    stageName = Data.trio(s.id.id);
                                }

                                if (stageMapName.isBlank() && s.getCont().id != null) {
                                    stageMapName = Data.trio(s.getCont().id.id);
                                }

                                String name = mapCollectionName + " - " + stageMapName + " - " + stageName;

                                if (name.length() > OptionData.MAX_CHOICE_NAME_LENGTH) {
                                    name = stageMapName + " - " + stageName;
                                }

                                if (name.length() > OptionData.MAX_CHOICE_NAME_LENGTH) {
                                    name = stageName;
                                }

                                if (name.length() > OptionData.MAX_CHOICE_NAME_LENGTH) {
                                    name = stageName.substring(0, OptionData.MAX_CHOICE_NAME_LENGTH - 3) + "...";
                                }

                                choices.add(new Command.Choice(name, DataToString.getStageCode(stages.get(i))));
                            }

                            event.replyChoices(choices).queue();
                        }
                    }
                    case "stage_map" -> {
                        String[] names = new String[2];

                        names[0] = event.getOptions().stream().filter(o -> o.getName().equals("map_collection") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");
                        names[1] = event.getOptions().stream().filter(o -> o.getName().equals("stage_map") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");

                        List<StageMap> maps = EntityFilter.findStageMapWithName(names, locale);

                        if (maps.isEmpty()) {
                            event.replyChoices().queue();
                        } else {
                            List<Command.Choice> choices = new ArrayList<>();

                            for (int i = 0; i < Math.min(maps.size(), OptionData.MAX_CHOICES); i++) {
                                StageMap s = maps.get(i);

                                String stageMapName = StaticStore.safeMultiLangGet(s, locale);
                                String mapCollectionName = StaticStore.safeMultiLangGet(s.getCont(), locale);

                                if (stageMapName == null || stageMapName.isBlank()) {
                                    stageMapName = s.names.toString();
                                }

                                if (mapCollectionName == null || mapCollectionName.isBlank()) {
                                    mapCollectionName = s.getCont().getSID();
                                }

                                if (stageMapName.isBlank() && s.id != null) {
                                    stageMapName = Data.trio(s.id.id);
                                }

                                String name = mapCollectionName + " - " + stageMapName;

                                if (name.length() > OptionData.MAX_CHOICE_NAME_LENGTH) {
                                    name = stageMapName;
                                }

                                if (name.length() > OptionData.MAX_CHOICE_NAME_LENGTH) {
                                    name = stageMapName.substring(0, OptionData.MAX_CHOICE_NAME_LENGTH - 3) + "...";
                                }

                                choices.add(new Command.Choice(name, stageMapName.substring(0, Math.min(stageMapName.length(), OptionData.MAX_CHOICE_VALUE_LENGTH))));
                            }

                            event.replyChoices(choices).queue();
                        }
                    }
                    case "map_collection" -> {
                        String name = event.getOptions().stream().filter(o -> o.getName().equals("map_collection") && o.getType() == OptionType.STRING).map(OptionMapping::getAsString).findAny().orElse("");

                        List<MapColc> mapCollections = EntityFilter.findMapCollectionWithName(name, locale);

                        if (mapCollections.isEmpty()) {
                            event.replyChoices().queue();
                        } else {
                            List<Command.Choice> choices = new ArrayList<>();

                            for (int i = 0; i < Math.min(mapCollections.size(), OptionData.MAX_CHOICES); i++) {
                                MapColc mc = mapCollections.get(i);

                                String mapCollectionName = StaticStore.safeMultiLangGet(mc, locale);

                                if (mapCollectionName == null || mapCollectionName.isBlank()) {
                                    mapCollectionName = mc.getSID();
                                }

                                String finalName = mapCollectionName;

                                if (finalName.length() > OptionData.MAX_CHOICE_NAME_LENGTH) {
                                    finalName = mapCollectionName.substring(0, OptionData.MAX_CHOICE_NAME_LENGTH - 3) + "...";
                                }

                                choices.add(new Command.Choice(finalName, mapCollectionName.substring(0, Math.min(mapCollectionName.length(), OptionData.MAX_CHOICE_VALUE_LENGTH))));
                            }

                            event.replyChoices(choices).queue();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        super.onReady(event);

        JDA client = event.getJDA();

        performClientReady(client);

        if (!readyDone) {
            performShardsReady(new ShardLoader(client.getShardManager()));
        }

        System.out.println("Sending online notification log...");

        StaticStore.logger.uploadLog("Bot ready to be used for shard " + client.getShardInfo().getShardString() + "!");
    }

    private static void notifyModerators(JDA client, User u, String content) {
        List<Guild> guilds = client.getGuilds();

        for(Guild g : guilds) {
            String gID = g.getId();

            Member m = g.getMemberById(u.getId());

            if(m != null) {
                IDHolder holder = StaticStore.idHolder.get(g.getId());

                if(holder == null) {
                    StaticStore.logger.uploadLog("W/AllEventAdapter::notifyModerators - No ID Holder found for guild ID : "+gID);

                    return;
                }

                if(holder.logDM != null && StaticStore.isNumeric(holder.logDM)) {
                    GuildChannel ch = g.getGuildChannelById(holder.logDM);

                    if(ch != null) {
                        EmbedBuilder builder = new EmbedBuilder();

                        builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                                .setDescription(LangID.getStringByID("watchDM.suspiciousLink", holder.config.lang))
                                .setAuthor(m.getEffectiveName()+" ("+m.getId()+")", null, m.getAvatarUrl())
                                .addField(LangID.getStringByID("watchDM.embed.content", holder.config.lang), content, true);

                        if(ch instanceof GuildMessageChannel) {
                            ((GuildMessageChannel) ch).sendMessageEmbeds(builder.build()).queue();
                        }
                    }
                }
            }
        }
    }

    private static RestAction<Member> findInviter(Guild g) {
        if (g.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            for(AuditLogEntry entry : g.retrieveAuditLogs()) {
                if(entry.getType() == ActionType.BOT_ADD) {
                    return g.retrieveMemberById(entry.getUserId());
                }
            }
        }

        return g.retrieveOwner();
    }

    /**
     * Perform on-ready tasks for this JDA shard<br>
     * <br>
     * This method must contain only JDA-level tasks
     *
     * @param client JDA shard
     */
    private static void performClientReady(JDA client) {
        System.out.println("Validating roles for shard " + client.getShardInfo() + "...");

        List<Guild> l = client.getGuilds().stream().filter(Objects::nonNull).toList();

        for (Guild guild : l) {
            IDHolder id = StaticStore.idHolder.computeIfAbsent(guild.getId(), k -> new IDHolder(guild));
            List<Role> roles = guild.getRoles();

            //Validate Role
            if (id.moderator != null && roles.stream().noneMatch(r -> r.getId().equals(id.moderator))) {
                id.moderator = null;
            }

            if (id.member != null && roles.stream().noneMatch(r -> r.getId().equals(id.member))) {
                id.member = null;
            }

            if (id.booster != null && roles.stream().noneMatch(r -> r.getId().equals(id.booster))) {
                id.booster = null;
            }

            id.ID.entrySet().removeIf(entry -> roles.stream().noneMatch(r -> r.getId().equals(entry.getValue())));
        }

        System.out.println("Sending online status for shard " + client.getShardInfo() + "...");

        for(String key : StaticStore.idHolder.keySet()) {
            try {
                IDHolder holder = StaticStore.idHolder.get(key);

                if(holder == null || holder.status.isEmpty())
                    continue;

                Guild g = client.getGuildById(key);

                if(g == null)
                    continue;

                for(int i = 0; i < holder.status.size(); i++) {
                    GuildChannel ch = g.getGuildChannelById(holder.status.get(i));

                    if(!(ch instanceof MessageChannel) || !((MessageChannel) ch).canTalk())
                        continue;

                    if(StaticStore.wasSafeClose) {
                        ((MessageChannel) ch).sendMessage(String.format(LangID.getStringByID("bot.status.online.normal", holder.config.lang), client.getSelfUser().getAsMention()))
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    } else {
                        ((MessageChannel) ch).sendMessage(String.format(LangID.getStringByID("bot.status.online.unknown", holder.config.lang), client.getSelfUser().getAsMention()))
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Performs global-level tasks<br>
     * <br>
     * This method must contain only tasks that don't rely on guilds
     *
     * @param loader Loaded content
     */
    private static void performShardsReady(ShardLoader loader) {
        if (!loader.fullyLoaded)
            return;

        System.out.println("Initializing emojis...");

        EmojiStore.initialize(loader);

        System.out.println("Initializing asset manager...");

        StaticStore.assetManager.initialize(loader);

        System.out.println("Building slash commands...");

        SlashBuilder.build(loader.supportServer.getJDA());

        System.out.println("Filtering out url format prefixes...");

        for(String key : StaticStore.config.keySet()) {
            ConfigHolder config = StaticStore.config.get(key);

            if(config == null)
                continue;

            if(config.prefix.matches("(.+)?http(s)?://(.+)?")) {
                config.prefix = StaticStore.globalPrefix;
            }
        }

        System.out.println("Initializing status message...");

        try {
            GuildChannel ch = loader.supportServer.getGuildChannelById("1100615571424419910");

            if(ch instanceof MessageChannel) {
                PackBot.statusMessage = ((MessageChannel) ch).retrieveMessageById("1100615782272090213");
            }
        } catch (Exception ignore) { }

        readyDone = true;
    }
}
