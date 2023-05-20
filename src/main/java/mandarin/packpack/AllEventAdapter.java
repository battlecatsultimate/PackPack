package mandarin.packpack;

import mandarin.packpack.commands.*;
import mandarin.packpack.commands.bc.*;
import mandarin.packpack.commands.bot.*;
import mandarin.packpack.commands.data.*;
import mandarin.packpack.commands.math.*;
import mandarin.packpack.commands.server.*;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.KoreanSeparater;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.HolderHub;
import mandarin.packpack.supporter.server.slash.SlashBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AllEventAdapter extends ListenerAdapter {
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

            IDHolder id = StaticStore.idHolder.get(g.getId());

            AtomicReference<Boolean> warned = new AtomicReference<>(false);

            if(id == null) {
                final IDHolder idh = new IDHolder();

                handleInitialModRole(g, idh, warned);

                StaticStore.idHolder.put(g.getId(), idh);
            } else {
                String mod = id.MOD;

                if (mod != null) {
                    List<Role> roles = g.getRoles();

                    for (Role r : roles) {
                        if (r.getId().equals(mod))
                            return;
                    }
                }

                handleInitialModRole(g, id, warned);
            }

            StaticStore.updateStatus();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildJoin - Error happened");
        }
    }

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        super.onRoleDelete(event);

        try {
            Guild g = event.getGuild();

            IDHolder holder = StaticStore.idHolder.get(g.getId());

            AtomicReference<Boolean> warned = new AtomicReference<>(false);

            if(holder != null) {
                String mod = holder.MOD;

                if(mod == null) {
                    reassignTempModRole(g, holder, warned);
                } else {
                    if(event.getRole().getId().equals(mod)) {
                        String modID = StaticStore.getRoleIDByName("PackPackMod", g);

                        if(modID == null) {
                            reassignTempModRole(g, holder, warned);
                        } else {
                            holder.MOD = modID;
                        }
                    }
                }
            } else {
                final IDHolder idh = new IDHolder();

                String modID = StaticStore.getRoleIDByName("PackPackMod", g);

                if(modID == null) {
                    reassignTempModRole(g, idh, warned);
                } else {
                    idh.MOD = modID;
                }

                StaticStore.idHolder.put(g.getId(), idh);
            }

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

            if(idh.ANNOUNCE != null && idh.ANNOUNCE.equals(ch.getId()))
                idh.ANNOUNCE = null;

            for(int key : idh.eventMap.keySet()) {
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
                String channel = StaticStore.scamLinkHandlers.servers.get(g.getId()).channel;

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

            if(holder.BOOSTER == null)
                return;

            if(!StaticStore.boosterData.containsKey(g.getId()))
                return;

            BoosterHolder booster = StaticStore.boosterData.get(g.getId());

            if(!booster.serverBooster.containsKey(m.getId()))
                return;

            BoosterData data = booster.serverBooster.get(m.getId());

            if(!StaticStore.rolesToString(m.getRoles()).contains(holder.BOOSTER)) {
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
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        super.onGuildMemberJoin(event);

        Guild g = event.getGuild();

        if(g.getId().equals(StaticStore.BCU_SERVER)) {
            Member m = event.getMember();

            String memberName = m.getNickname();
            String userName = m.getUser().getName();

            boolean recommend = memberName != null && KoreanSeparater.containKorean(memberName);

            if(KoreanSeparater.containKorean(userName)) {
                recommend = true;
            }

            if(recommend) {
                m.getUser().openPrivateChannel().queue(ch -> ch.sendMessage(LangID.getStringByID("korean_recommend", 0)).queue(), e -> {});
            }
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

            int lang = -1;

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

                if(lang == -1)
                    lang = LangID.EN;

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

                IDHolder idh = StaticStore.idHolder.get(g.getId());

                if(idh == null)
                    return;

                boolean isMod = false;
                boolean canGo = false;

                if(idh.MOD != null) {
                    isMod = StaticStore.rolesToString(m.getRoles()).contains(idh.MOD);
                }

                ArrayList<String> channels = idh.getAllAllowedChannels(m);

                if(channels == null)
                    canGo = true;
                else if(!channels.isEmpty()) {
                    canGo = channels.contains(mc.getId());
                }

                if(!mandarin && !isMod && !canGo)
                    return;

                if(!mandarin && idh.banned.contains(u.getId()))
                    return;

                if(msg.getContentRaw().toLowerCase(java.util.Locale.ENGLISH).startsWith(idh.serverPrefix))
                    prefix = idh.serverPrefix;

                if(lang == -1)
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

    public void performCommand(MessageReceivedEvent event, Message msg, int lang, String prefix, @Nullable IDHolder idh, @Nullable ConfigHolder c) {
        switch (StaticStore.getCommand(msg.getContentRaw(), prefix)) {
            case "checkbcu" -> new CheckBCU(lang, idh).execute(event);
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
            case "serverjson", "json", "sj" -> new ServerJson(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
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
            case "logout", "lo" -> new LogOut(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
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
            case "config" -> new Config(ConstraintCommand.ROLE.MEMBER, lang, idh, c, false).execute(event);
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
                    new Config(ConstraintCommand.ROLE.MOD, lang, idh, idh.config, true).execute(event);
                }
            }
            case "commandban", "cb" -> new CommandBan(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "commandunban", "cub" -> new CommandUnban(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "ignorechannelpermission", "ichannelpermission", "ignorechp", "ichp" ->
                    new IgnoreChannelPermission(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "allowchannelpermission", "achannelpermission", "allowchp", "achp" ->
                    new AllowChannelPermission(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
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
            case "announcemessage", "am" -> new AnnounceMessage(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "talanetanalyzer", "tala", "ta" ->
                    new TalentAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "catcomboanalyzer", "comboanalyzer", "cca", "ca" ->
                    new ComboAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
            case "addstatuschannel", "asc" ->
                    new AddStatusChannel(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
            case "removestatuschannel", "rsc" ->
                    new RemoveStatusChannel(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
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

        System.out.println("Initializing emojis...");

        EmojiStore.initialize(client);

        System.out.println("Building slash commands...");

        SlashBuilder.build(client);

        System.out.println("Validating roles...");

        List<Guild> l = client.getGuilds();

        for (Guild guild : l) {
            if (guild != null) {
                IDHolder id = StaticStore.idHolder.get(guild.getId());

                AtomicReference<Boolean> warned = new AtomicReference<>(false);

                if (id == null) {
                    final IDHolder idh = new IDHolder();

                    String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                    if(modID == null) {
                        reassignTempModRole(guild, idh, warned);
                    } else {
                        idh.MOD = modID;
                    }

                    StaticStore.idHolder.put(guild.getId(), idh);
                } else {
                    //Validate Role
                    String mod = id.MOD;

                    if(mod == null) {
                        String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                        if(modID == null) {
                            reassignTempModRole(guild, id, warned);
                        } else {
                            id.MOD = modID;
                        }
                    } else {
                        List<Role> roles = guild.getRoles();

                        boolean found = false;

                        for(Role r : roles) {
                            if(r.getId().equals(mod)) {
                                found = true;
                                break;
                            }
                        }

                        if(found)
                            continue;

                        String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                        if(modID == null) {
                            reassignTempModRole(guild, id, warned);
                        } else {
                            id.MOD = modID;
                        }
                    }
                }
            }
        }

        System.out.println("Filtering out unreachable guilds...");

        List<String> unreachableGuilds = new ArrayList<>();

        for(String key : StaticStore.idHolder.keySet()) {
            Guild g = client.getGuildById(key);

            if(g == null) {
                unreachableGuilds.add(key);
            }
        }

        for(String key : unreachableGuilds) {
            StaticStore.idHolder.remove(key);
        }

        StaticStore.saveServerInfo();

        System.out.println("Sending online status...");

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

                    if(StaticStore.safeClose) {
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

        System.out.println("Initializing status message...");

        try {
            Guild g = client.getGuildById("964054872649515048");

            if(g != null) {
                GuildChannel ch = g.getGuildChannelById("1100615571424419910");

                if(ch instanceof MessageChannel) {
                    PackBot.statusMessage = ((MessageChannel) ch).retrieveMessageById("1100615782272090213").complete();
                }
            }
        } catch (Exception ignore) { }

        System.out.println("Filtering out url format prefixes...");

        for(String key : StaticStore.prefix.keySet()) {
            String prefix = StaticStore.prefix.get(key);

            if(prefix == null)
                continue;

            if(prefix.matches("(.+)?http(s)?://(.+)?")) {
                StaticStore.prefix.remove(key);
            }
        }

        System.out.println("Sending online notification log...");

        StaticStore.safeClose = false;

        StaticStore.logger.uploadLog("Bot ready to be used!");
    }

    private static void handleInitialModRole(Guild g, IDHolder id, AtomicReference<Boolean> warned) {
        String modID = StaticStore.getRoleIDByName("PackPackMod", g);

        Member inviter = findInviter(g);

        if(modID == null) {
            if (g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                if(g.getRoles().size() == 250) {
                    if(!warned.get()) {
                        if(inviter != null) {
                            inviter.getUser().openPrivateChannel()
                                    .flatMap(pc -> pc.sendMessage(LangID.getStringByID("maxrole", id.config.lang).replace("_", g.getName())))
                                    .queue();

                            warned.set(true);
                        }
                    }

                    return;
                }

                g.createRole()
                        .setName("PackPackMod")
                        .queue(r -> {
                            id.MOD = r.getId();

                            if(inviter != null) {
                                inviter.getUser().openPrivateChannel()
                                        .flatMap(pc -> {
                                            String message = LangID.getStringByID("first_join", id.config.lang)
                                                    .replace("_SSS_", g.getName());

                                            return pc.sendMessage(message);
                                        }).queue();
                            }
                        }, e -> StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::handleInitialModRole - Error happened while trying to create role"));
            } else {
                if(!warned.get()) {
                    if(inviter != null) {
                        inviter.getUser().openPrivateChannel()
                                .flatMap(pc -> pc.sendMessage(LangID.getStringByID("needroleperm", id.config.lang).replace("_", g.getName())))
                                .queue();
                    }

                    warned.set(true);
                }
            }

        } else {
            id.MOD = modID;

            if(inviter != null) {
                inviter.getUser().openPrivateChannel()
                        .flatMap(pc -> {
                            String message = LangID.getStringByID("first_join", id.config.lang)
                                    .replace("_SSS_", g.getName());

                            return pc.sendMessage(message);
                        }).queue();
            }
        }
    }

    private static void reassignTempModRole(Guild g, IDHolder holder, AtomicReference<Boolean> warned) {
        if (g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            if(g.getRoles().size() == 250) {
                if(!warned.get()) {
                    Member inviter = findInviter(g);

                    if(inviter != null) {
                        inviter.getUser().openPrivateChannel()
                                .flatMap(pc -> pc.sendMessage(LangID.getStringByID("maxrole", holder.config.lang).replace("_", g.getName())))
                                .queue(null, e -> { });

                        warned.set(true);
                    }
                }

                return;
            }

            g.createRole()
                    .setName("PackPackMod")
                    .queue(r -> holder.MOD = r.getId(), e -> StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::reassignTempModRole - Error happened while trying to create role"));
        } else {
            if(!warned.get()) {
                Member inviter = findInviter(g);

                if(inviter != null) {
                    inviter.getUser().openPrivateChannel()
                            .flatMap(pc -> pc.sendMessage(LangID.getStringByID("needroleperm", holder.config.lang).replace("_", g.getName())))
                            .queue();

                    warned.set(true);
                }
            }
        }
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

    private static Member findInviter(Guild g) {
        if (g.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            for(AuditLogEntry entry : g.retrieveAuditLogs()) {
                if(entry.getType() == ActionType.BOT_ADD) {
                    return g.retrieveMemberById(entry.getUserId()).complete();
                }
            }

            return null;
        } else {
            return g.getOwner();
        }
    }
}
