package mandarin.packpack;

import common.CommonStatic;
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
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AllEventAdapter extends ListenerAdapter {
    private static boolean readyDone = false;

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        super.onGuildLeave(event);

        try {
            Guild g = event.getGuild();

            StaticStore.logger.uploadLog("Left server : "+g.getName()+ " ("+g.getId()+")");

            StaticStore.idHolder.remove(g.getId());

            StaticStore.saveServerInfo();

            StaticStore.updateStatus();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildLeave - Error happened");
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        super.onGuildJoin(event);

        try {
            Guild g = event.getGuild();

            StaticStore.logger.uploadLog("Joined server : "+g.getName()+" ("+g.getId()+")"+"\nSize : "+g.getMemberCount());

            IDHolder holder = StaticStore.idHolder.computeIfAbsent(g.getId(), k -> new IDHolder(g));

            StaticStore.saveServerInfo();

            StaticStore.updateStatus();

            findInviter(g).queue(m ->
                m.getUser().openPrivateChannel().queue(ch ->
                        ch.sendMessage(LangID.getStringByID("first_join", holder.config.lang).formatted(g.getName())).queue(),
                    e ->
                        StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildJoin - Failed to open private channel to inviter")
                )
            );
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildJoin - Error happened");
        }
    }

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
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
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        super.onChannelDelete(event);

        try {
            Guild g = event.getGuild();
            Channel ch = event.getChannel();

            IDHolder idh = StaticStore.idHolder.get(g.getId());

            if(idh == null)
                return;

            if(idh.announceChannel != null && idh.announceChannel.equals(ch.getId()))
                idh.announceChannel = null;

            for(CommonStatic.Lang.Locale key : idh.eventMap.keySet()) {
                String channel = idh.eventMap.get(key);

                if(channel == null || channel.isBlank() || channel.equals(ch.getId())) {
                    idh.eventMap.remove(key);
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
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onChannelDelete - Error happened");
        }
    }

    @Override
    public void onGuildMemberUpdate(@NotNull GuildMemberUpdateEvent event) {
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
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
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

                boolean isMod;

                String moderatorID = idh.moderator;
                List<Role> roles = m.getRoles();

                if (moderatorID != null) {
                    isMod = roles.stream().anyMatch(r -> r.getId().equals(moderatorID));
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
            case "memory", "mm" -> new Memory(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
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
            case "fixrole", "fir" -> new FixRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
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
            case "setbcversion", "sbv" -> new SetBCVersion(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
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
            case "eventmessage", "em" -> new EventMessage(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
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
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
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
    public void onGenericInteractionCreate(@NotNull GenericInteractionCreateEvent event) {
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

                    switch (i.getName()) {
                        case "fs" -> FormStat.performInteraction(i);
                        case "es" -> EnemyStat.performInteraction(i);
                        case "si" -> StageInfo.performInteraction(i);
                    }
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
    public void onReady(@NotNull ReadyEvent event) {
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
                                .setDescription(LangID.getStringByID("watdm_suslink", holder.config.lang))
                                .setAuthor(m.getEffectiveName()+" ("+m.getId()+")", null, m.getAvatarUrl())
                                .addField(LangID.getStringByID("watdm_content", holder.config.lang), content, true);

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
            if (roles.stream().noneMatch(r -> r.getId().equals(id.moderator))) {
                id.moderator = null;
            }

            if (roles.stream().noneMatch(r -> r.getId().equals(id.member))) {
                id.moderator = null;
            }

            if (roles.stream().noneMatch(r -> r.getId().equals(id.booster))) {
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
                        ((MessageChannel) ch).sendMessage(String.format(LangID.getStringByID("bot_online", holder.config.lang), client.getSelfUser().getAsMention()))
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    } else {
                        ((MessageChannel) ch).sendMessage(String.format(LangID.getStringByID("bot_issue", holder.config.lang), client.getSelfUser().getAsMention()))
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
