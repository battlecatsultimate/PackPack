package mandarin.packpack;

import mandarin.packpack.commands.*;
import mandarin.packpack.commands.bc.*;
import mandarin.packpack.commands.bot.*;
import mandarin.packpack.commands.data.*;
import mandarin.packpack.commands.server.*;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.InteractionHolder;
import mandarin.packpack.supporter.server.holder.MessageHolder;
import mandarin.packpack.supporter.server.slash.SlashBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unchecked")
public class AllEventAdapter extends ListenerAdapter {
    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        super.onGuildLeave(event);

        try {
            Guild g = event.getGuild();

            StaticStore.logger.uploadLog("Left server : "+g.getName()+ " ("+g.getId()+")");

            StaticStore.idHolder.remove(g.getId());

            StaticStore.saveServerInfo();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildLeave - Error happened");
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        super.onGuildJoin(event);

        try {
            Guild g = event.getGuild();

            StaticStore.logger.uploadLog("Joined server : "+g.getName()+" ("+g.getId()+")");

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

            if(idh.GET_ACCESS != null && idh.GET_ACCESS.equals(ch.getId()))
                idh.GET_ACCESS = null;

            if(idh.event != null && idh.event.equals(ch.getId()))
                idh.event = null;

            if(idh.logDM != null && idh.logDM.equals(ch.getId()))
                idh.logDM = null;

            StaticStore.idHolder.put(g.getId(), idh);

            if(StaticStore.scamLinkHandlers.servers.containsKey(g.getId())) {
                String channel = StaticStore.scamLinkHandlers.servers.get(g.getId()).channel;

                if(channel != null && channel.equals(ch.getId())) {
                    StaticStore.scamLinkHandlers.servers.remove(g.getId());
                }
            }
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
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            super.onMessageReceived(event);

            MessageChannel mc = event.getChannel();
            Message msg = event.getMessage();

            if(mc instanceof PrivateChannel) {
                String content = msg.getContentRaw();

                if(content.contains("http")) {
                    if(content.length() > 1000) {
                        content = content.substring(0, 997)+"...";
                    }

                    if(!StaticStore.optoutMembers.contains(msg.getAuthor().getId())) {
                        notifyModerators(event.getJDA(), msg.getAuthor(), content);
                    }
                }

                return;
            }

            Member m = event.getMember();
            User u = event.getAuthor();

            if(m == null)
                return;

            if(StaticStore.optoutMembers.contains(m.getId())) {
                return;
            }

            Guild g = event.getGuild();

            if(!u.isBot() && !StaticStore.optoutMembers.contains(m.getId()) && StaticStore.scamLinkHandlers.servers.containsKey(g.getId()) && ScamLinkHandler.validScammingUser(msg.getContentRaw())) {
                String link = ScamLinkHandler.getLinkFromMessage(msg.getContentRaw());

                if(link != null) {
                    link = link.replace("http://", "").replace("https://", "");
                }

                StaticStore.scamLinkHandlers.servers.get(g.getId()).takeAction(link, m, g);
                StaticStore.logger.uploadLog("I caught compromised user\nLINK : "+link+"\nGUILD : "+g.getName()+" ("+g.getId()+")\nMEMBER : "+m.getEffectiveName()+" ("+m.getId()+")");

                msg.delete().queue();
            }

            IDHolder idh = StaticStore.idHolder.get(g.getId());

            if(idh == null)
                return;

            boolean mandarin = m.getId().equals(StaticStore.MANDARIN_SMELL);
            boolean isMod = false;
            boolean canGo = false;

            if(idh.MOD != null) {
                isMod = StaticStore.rolesToString(m.getRoles()).contains(idh.MOD);
            }

            ArrayList<String> channels = idh.getAllAllowedChannels(m.getRoles());

            if(channels == null)
                canGo = true;
            else if(!channels.isEmpty()) {
                canGo = channels.contains(mc.getId());
            }

            String acc = idh.GET_ACCESS;

            canGo &= acc == null || !mc.getId().equals(acc);

            if(!mandarin && !isMod && !canGo)
                return;

            if(StaticStore.holderContainsKey(m.getId())) {
                Holder<? extends Event> holder = StaticStore.getHolder(m.getId());

                if(holder instanceof MessageHolder) {
                    MessageHolder<? extends GenericMessageEvent> messageHolder = (MessageHolder<? extends GenericMessageEvent>) holder;

                    if(messageHolder.canCastTo(MessageReceivedEvent.class)) {
                        MessageHolder<MessageReceivedEvent> h = (MessageHolder<MessageReceivedEvent>) messageHolder;

                        int result = h.handleEvent(event);

                        if(result == Holder.RESULT_FINISH) {
                            messageHolder.clean();
                            StaticStore.removeHolder(m.getId(), messageHolder);
                        } else if(result == Holder.RESULT_FAIL) {
                            StaticStore.logger.uploadLog("Error : Expired process tried to be handled : "+m.getId()+" | "+messageHolder.getClass().getName());
                            StaticStore.removeHolder(m.getId(), messageHolder);
                        }
                    }
                }
            }

            String prefix = StaticStore.getPrefix(m.getId());

            if(msg.getContentRaw().startsWith(idh.serverPrefix))
                prefix = idh.serverPrefix;

            if(msg.getContentRaw().startsWith(StaticStore.serverPrefix))
                prefix = StaticStore.serverPrefix;

            int lang = idh.serverLocale;
            ConfigHolder c;

            if(StaticStore.config.containsKey(m.getId())) {
                lang = StaticStore.config.get(m.getId()).lang;
                c = StaticStore.config.get(m.getId());
            } else {
                c = new ConfigHolder();
            }

            if(lang == -1)
                lang = idh.serverLocale;

            switch (StaticStore.getCommand(msg.getContentRaw(), prefix)) {
                case "checkbcu":
                    new CheckBCU(lang, idh).execute(event);
                    break;
                case "bcustat":
                    new BCUStat(lang, idh).execute(event);
                    break;
                case "analyze":
                    new Analyze(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "help":
                    new Help(lang, idh).execute(event);
                    break;
                case "prefix":
                    new Prefix(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "serverpre":
                    new ServerPrefix(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "save":
                    new Save(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "stimg":
                case "stimage":
                case "stageimg":
                case "stageimage":
                    new StageImage(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "stmimg":
                case "stmimage":
                case "stagemapimg":
                case "stagemapimage":
                    new StmImage(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "formstat":
                case "fs":
                    new FormStat(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
                    break;
                case "locale":
                case "loc":
                    new Locale(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "music":
                case "ms":
                    new Music(ConstraintCommand.ROLE.MEMBER, lang, idh, "music_").execute(event);
                    break;
                case "enemystat":
                case "es":
                    new EnemyStat(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
                    break;
                case "castle":
                case "cs":
                    new Castle(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "stageinfo":
                case "si":
                    new StageInfo(ConstraintCommand.ROLE.MEMBER, lang, idh, c,5000).execute(event);
                    break;
                case "memory":
                case "mm":
                    new Memory(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "formimage":
                case "formimg":
                case "fimage":
                case "fimg":
                    new FormImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
                    break;
                case "enemyimage":
                case "enemyimg":
                case "eimage":
                case "eimg":
                    new EnemyImage(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
                    break;
                case "background":
                case "bg":
                    new Background(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000).execute(event);
                    break;
                case "test":
                    new Test(ConstraintCommand.ROLE.MANDARIN, lang, idh, "test").execute(event);
                    break;
                case "formgif":
                case "fgif":
                case "fg":
                    new FormGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
                    break;
                case "enemygif":
                case "egif":
                case "eg":
                    new EnemyGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif").execute(event);
                    break;
                case "idset":
                    new IDSet(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "clearcache":
                    new ClearCache(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "aa":
                case "animanalyzer":
                    new AnimAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "channelpermission":
                case "channelperm":
                case "chpermission":
                case "chperm":
                case "chp":
                    new ChannelPermission(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "formsprite":
                case "fsprite":
                case "formsp":
                case "fsp":
                    new FormSprite(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.SECONDS.toMillis(10)).execute(event);
                    break;
                case "enemysprite":
                case "esprite":
                case "enemysp":
                case "esp":
                    new EnemySprite(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.SECONDS.toMillis(10)).execute(event);
                    break;
                case "medal":
                case "md":
                    new Medal(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "announcement":
                case "ann":
                    new Announcement(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "catcombo":
                case "combo":
                case "cc":
                    new CatCombo(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "serverjson":
                case "json":
                case "sj":
                    new ServerJson(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "findstage":
                case "findst":
                case "fstage":
                case "fst":
                    new FindStage(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 5000).execute(event);
                    break;
                case "suggest":
                    new Suggest(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.MINUTES.toMillis(60)).execute(event);
                    break;
                case "suggestban":
                case "sgb":
                    new SuggestBan(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "suggestunban":
                case "sgub":
                    new SuggestUnban(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "suggestresponse":
                case "sgr":
                    new SuggestResponse(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "alias":
                case "al":
                    new Alias(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "aliasadd":
                case "ala":
                    new AliasAdd(lang).execute(event);
                    break;
                case "aliasremove":
                case "alr":
                    new AliasRemove(lang).execute(event);
                    break;
                case "contributoradd":
                case "coa":
                    new ContributorAdd(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "contributorremove":
                case "cor":
                    new ContributorRemove(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "statistic":
                case "stat":
                    new Statistic(lang).execute(event);
                    break;
                case "serverlocale":
                case "serverloc":
                case "sloc":
                    new ServerLocale(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "news":
                    new News(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "publish":
                    new Publish(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "boosterrole":
                case "boosterr":
                case "brole":
                case "br":
                    new BoosterRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "boosterroleremove":
                case "brremove":
                case "boosterrolerem":
                case "brrem":
                case "brr":
                    new BoosterRoleRemove(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "boosteremoji":
                case "boostere":
                case "bemoji":
                case "be":
                    new BoosterEmoji(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "boosteremojiremove":
                case "beremove":
                case"boosteremojirem":
                case "berem":
                case "ber":
                    new BoosterEmojiRemove(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "registerlogging":
                case "rlogging":
                case "registerl":
                case "rl":
                    new RegisterLogging(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "unregisterlogging":
                case "urlogging":
                case "unregisterl":
                case "url":
                    new UnregisterLogging(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "setup":
                    new Setup(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "fixrole":
                case "fr":
                    new FixRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "registerfixing":
                case "rf":
                    new RegisterFixing(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "unregisterfixing":
                case "urf":
                    new UnregisterFixing(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "watchdm":
                case "wd":
                    new WatchDM(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "checkeventupdate":
                case "ceu":
                    new CheckEventUpdate(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "printstageevent":
                case "pse":
                    new PrintStageEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "subscribeevent":
                case "se":
                    new SubscribeEvent(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "printgachaevent":
                case "pge":
                    new PrintGachaEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "setbcversion":
                case "sbv":
                    new SetBCVersion(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "logout":
                    new LogOut(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "printitemevent":
                case "pie":
                    new PrintItemEvent(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "printevent":
                case "pe":
                    new PrintEvent(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "statanalyzer":
                case "sa":
                    new StatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "addscamlinkhelpingserver":
                case "aslhs":
                case "ashs":
                    new AddScamLinkHelpingServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "removescamlinkhelpingserver":
                case "rslhs":
                case "rshs":
                    new RemoveScamLinkHelpingServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "registerscamlink":
                case "rsl":
                    new RegisterScamLink(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "unregisterscamlink":
                case "usl":
                    new UnregisterScamLink(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "subscribescamlinkdetector":
                case "ssld":
                case "ssd":
                    new SubscribeScamLinkDetector(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "unsubscribescamlinkdetector":
                case "usld":
                case "usd":
                    new UnsubscribeScamLinkDetector(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                    break;
                case "timezone":
                case "tz":
                    new TimeZone(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "optout":
                    new OptOut(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                    break;
                case "config":
                    new Config(ConstraintCommand.ROLE.MEMBER, lang, idh, c).execute(event);
                    break;
                case "removecache":
                case "rc":
                    new RemoveCache(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "sendmessage":
                case "sm":
                    new SendMessage(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "analyzeserver":
                case "as":
                    new AnalyzeServer(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "cleartemp":
                case "ct":
                    new ClearTemp(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                    break;
                case "downloadapk":
                case "da":
                    new DownloadApk(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "trueformanalyzer":
                case "tfanalyzer":
                case "trueforma":
                case "tfa":
                    new TrueFormAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "enemystatanalyzer":
                case "estatanalyzer":
                case "enemysa":
                case "esa":
                    new EnemyStatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
                case "stagestatanalyzer":
                case "sstatanalyzer":
                case "stagesa":
                case "ssa":
                    new StageStatAnalyzer(ConstraintCommand.ROLE.TRUSTED, lang, idh).execute(event);
                    break;
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onMessageReceived - Error happened while doing something");
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        super.onMessageReactionAdd(event);

        try {
            Member m = event.getMember();

            if(m == null) {
                StaticStore.logger.uploadLog("W/AllEventAdapter::onMessageReactionAdd - Member is empty while trying to perform ReactionAddEvent");

                return;
            }

            if(StaticStore.holderContainsKey(m.getId())) {
                Holder<? extends Event> holder = StaticStore.getHolder(m.getId());

                if(!(holder instanceof MessageHolder))
                    return;

                MessageHolder<? extends GenericMessageEvent> messageHolder = (MessageHolder<? extends GenericMessageEvent>) holder;

                if(messageHolder.canCastTo(MessageReactionAddEvent.class)) {
                    MessageHolder<MessageReactionAddEvent> h = (MessageHolder<MessageReactionAddEvent>) messageHolder;

                    int result = h.handleEvent(event);

                    if(result == Holder.RESULT_FINISH) {
                        h.clean();
                        StaticStore.removeHolder(m.getId(), holder);
                    } else if(result == Holder.RESULT_FAIL) {
                        StaticStore.logger.uploadLog("W/AllEventAdapter::onMessageReactionAdd - Expired process tried to be handled : "+m.getId()+" | "+h.getClass().getName());
                        StaticStore.removeHolder(m.getId(), holder);
                    }
                }
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onMessageReactionAdd - Error happened");
        }
    }

    @Override
    public void onGenericInteractionCreate(@NotNull GenericInteractionCreateEvent event) {
        super.onGenericInteractionCreate(event);

        try {
            if(event instanceof GenericComponentInteractionCreateEvent) {
                GenericComponentInteractionCreateEvent c = (GenericComponentInteractionCreateEvent) event;

                if(c.getInteraction().getMember() == null)
                    return;

                Member m = c.getInteraction().getMember();

                if(StaticStore.holderContainsKey(m.getId())) {
                    Holder<? extends Event> holder = StaticStore.getHolder(m.getId());

                    if(!(holder instanceof InteractionHolder)) {
                        return;
                    }

                    InteractionHolder<? extends GenericComponentInteractionCreateEvent> interactionHolder = (InteractionHolder<? extends GenericComponentInteractionCreateEvent>) holder;

                    if(interactionHolder.canCastTo(ButtonInteractionEvent.class)) {
                        InteractionHolder<ButtonInteractionEvent> h = (InteractionHolder<ButtonInteractionEvent>) interactionHolder;

                        int result = h.handleEvent((ButtonInteractionEvent) event);

                        if(result == Holder.RESULT_FINISH || result == Holder.RESULT_FAIL) {
                            StaticStore.removeHolder(m.getId(), holder);
                        }

                        if(result == Holder.RESULT_FINISH)
                            h.performInteraction((ButtonInteractionEvent) event);
                    } else if(interactionHolder.canCastTo(GenericComponentInteractionCreateEvent.class)) {
                        InteractionHolder<GenericComponentInteractionCreateEvent> h = (InteractionHolder<GenericComponentInteractionCreateEvent>) interactionHolder;

                        int result = h.handleEvent((GenericComponentInteractionCreateEvent) event);

                        if(result == Holder.RESULT_FINISH)
                            h.performInteraction((GenericComponentInteractionCreateEvent) event);
                    }
                }
            } else if(event instanceof GenericCommandInteractionEvent) {
                GenericCommandInteractionEvent i = (GenericCommandInteractionEvent) event;

                if(event.getInteraction().getMember() == null) {
                    return;
                }

                SpamPrevent spam;

                Member m = event.getInteraction().getMember();

                if(StaticStore.spamData.containsKey(m.getId())) {
                    spam = StaticStore.spamData.get(m.getId());

                    String result = spam.isPrevented(event);

                    if(result != null) {
                        if (!result.isBlank()) {

                            i.deferReply().setContent(result).queue();
                        }

                        return;
                    }
                }

                switch (i.getName()) {
                    case "fs":
                        FormStat.performInteraction(i);
                        break;
                    case "es":
                        EnemyStat.performInteraction(i);
                        break;
                    case "si":
                        StageInfo.performInteraction(i);
                        break;
                }
            }
        } catch (Exception e) {
            String message = "E/AllEventAdapter::onGenericInteractionCreate - Error happened";

            Member m = event.getMember();

            if(m != null && StaticStore.holderContainsKey(m.getId())) {
                message += "\n\nTried to handle the holder : " + StaticStore.getHolder(m.getId()).getClass().getName();
            }

            StaticStore.logger.uploadErrorLog(e, message);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        super.onReady(event);

        JDA client = event.getJDA();

        EmojiStore.initialize(client);

        SlashBuilder.build(client);

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

        StaticStore.saveServerInfo();

        StaticStore.logger.uploadLog("Bot ready to be used!");
    }

    private static boolean isModerator(EnumSet<Permission> set) {
        return set.contains(Permission.MODERATE_MEMBERS) ||
                set.contains(Permission.ADMINISTRATOR) ||
                set.contains(Permission.BAN_MEMBERS) ||
                set.contains(Permission.VOICE_MUTE_OTHERS) ||
                set.contains(Permission.KICK_MEMBERS) ||
                set.contains(Permission.MANAGE_CHANNEL) ||
                set.contains(Permission.MANAGE_SERVER) ||
                set.contains(Permission.MANAGE_ROLES);
    }

    private static void handleInitialModRole(Guild g, IDHolder id, AtomicReference<Boolean> warned) {
        String modID = StaticStore.getRoleIDByName("PackPackMod", g);

        if(modID == null) {
            if (g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                if(g.getRoles().size() == 250) {
                    if(!warned.get()) {
                        Member owner = g.retrieveOwner().complete();

                        if(owner != null) {
                            owner.getUser().openPrivateChannel()
                                    .flatMap(pc -> pc.sendMessage(LangID.getStringByID("maxrole", id.serverLocale).replace("_", g.getName())))
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

                            Role role = null;

                            for(Role ro : g.getRoles()) {
                                if(!ro.isManaged() && !ro.isPublicRole() && isModerator(ro.getPermissions())) {
                                    role = ro;
                                    break;
                                }
                            }

                            String roleName = role != null ? role.getName() : null;
                            String roleID = role != null ? role.getId() : null;

                            g.retrieveOwner().queue(owner -> {
                                if(owner != null) {
                                    owner.getUser().openPrivateChannel()
                                            .flatMap(pc -> {
                                                String message;

                                                if(roleName != null) {
                                                    message = LangID.getStringByID("first_joinmod", id.serverLocale)
                                                            .replace("_III_", roleID)
                                                            .replace("_MMM_", roleName)
                                                            .replace("_SSS_", g.getName());
                                                } else {
                                                    message = LangID.getStringByID("first_join", id.serverLocale)
                                                            .replace("_SSS_", g.getName());
                                                }

                                                return pc.sendMessage(message);
                                            }).queue();
                                }
                            });
                        }, e -> StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::handleInitialModRole - Error happened while trying to create role"));
            } else {
                if(!warned.get()) {
                    Member owner = g.retrieveOwner().complete();

                    if(owner != null) {
                        owner.getUser().openPrivateChannel()
                                .flatMap(pc -> pc.sendMessage(LangID.getStringByID("needroleperm", id.serverLocale).replace("_", g.getName())))
                                .queue();
                    }

                    warned.set(true);
                }
            }

        } else {
            id.MOD = modID;

            Role role = null;

            for(Role ro : g.getRoles()) {
                if(!ro.isManaged() && !ro.isPublicRole() && isModerator(ro.getPermissions())) {
                    role = ro;
                    break;
                }
            }

            String roleName = role != null ? role.getName() : null;
            String roleID = role != null ? role.getId() : null;

            Member owner = g.retrieveOwner().complete();

            if(owner != null) {
                owner.getUser().openPrivateChannel()
                        .flatMap(pc -> {
                            String message;

                            if(roleName != null) {
                                message = LangID.getStringByID("first_joinmod", id.serverLocale)
                                        .replace("_III_", roleID)
                                        .replace("_MMM_", roleName)
                                        .replace("_SSS_", g.getName());
                            } else {
                                message = LangID.getStringByID("first_join", id.serverLocale)
                                        .replace("_SSS_", g.getName());
                            }

                            return pc.sendMessage(message);
                        }).queue();
            }
        }
    }

    private static void reassignTempModRole(Guild g, IDHolder holder, AtomicReference<Boolean> warned) {
        if (g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            if(g.getRoles().size() == 250) {
                if(!warned.get()) {
                    Member owner = g.retrieveOwner().complete();

                    if(owner != null) {
                        owner.getUser().openPrivateChannel()
                                .flatMap(pc -> pc.sendMessage(LangID.getStringByID("maxrole", holder.serverLocale).replace("_", g.getName())))
                                .queue();

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
                Member owner = g.retrieveOwner().complete();

                if(owner != null) {
                    owner.getUser().openPrivateChannel()
                            .flatMap(pc -> pc.sendMessage(LangID.getStringByID("needroleperm", holder.serverLocale).replace("_", g.getName())))
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

                if(StaticStore.isNumeric(holder.logDM)) {
                    GuildChannel ch = g.getGuildChannelById(holder.logDM);

                    if(ch != null) {
                        EmbedBuilder builder = new EmbedBuilder();

                        builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                                .setDescription(LangID.getStringByID("watdm_suslink", holder.serverLocale))
                                .setAuthor(m.getEffectiveName()+" ("+m.getId()+")", null, m.getAvatarUrl())
                                .addField(LangID.getStringByID("watdm_content", holder.serverLocale), content, true);

                        if(ch instanceof GuildMessageChannel) {
                            ((GuildMessageChannel) ch).sendMessageEmbeds(builder.build()).queue();
                        }
                    }
                }
            }
        }
    }
}
