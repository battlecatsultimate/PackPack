package mandarin.packpack;

import common.CommonStatic;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.channel.NewsChannelDeleteEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.request.RouterOptions;
import mandarin.packpack.commands.*;
import mandarin.packpack.commands.bc.*;
import mandarin.packpack.commands.bot.*;
import mandarin.packpack.commands.data.AnimAnalyzer;
import mandarin.packpack.commands.data.Announcement;
import mandarin.packpack.commands.data.StageImage;
import mandarin.packpack.commands.data.StmImage;
import mandarin.packpack.commands.server.*;
import mandarin.packpack.supporter.AssetDownloader;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.PackContext;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.FormReactionMessageHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.MessageHolder;
import mandarin.packpack.supporter.server.slash.SlashBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Usage : java -jar JARNAME DISCORD_BOT_TOKEN IMGUR_API_ACCESS_TOKEN
 */
public class PackBot {
    public static int save = 0;

    public static void main(String[] args) {
        initialize(args);

        final String TOKEN = args[0];

        DiscordClientBuilder<DiscordClient, RouterOptions> builder = DiscordClientBuilder.create(TOKEN);

        DiscordClient client = builder.build();
        GatewayDiscordClient gate = client.gateway().setEnabledIntents(IntentSet.all()).login().block();

        if(gate == null) {
            return;
        }

        StaticStore.logger = new Logger(gate);

        SlashBuilder.build(gate);

        gate.updatePresence(ClientPresence.online(ClientActivity.playing("p!help, but under Construction!"))).subscribe();

        gate.getGuilds().collectList().subscribe(l -> {
            for (Guild guild : l) {
                if (guild != null) {
                    IDHolder id = StaticStore.idHolder.get(guild.getId().asString());

                    if (id == null) {
                        final IDHolder idh = new IDHolder();

                        String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                        if(modID == null) {
                            RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                            roleBuilder.name("PackPackMod");

                            guild.createRole(roleBuilder.build()).subscribe(r -> idh.MOD = r.getId().asString());
                        } else {
                            idh.MOD = modID;
                        }

                        StaticStore.idHolder.put(guild.getId().asString(), idh);
                    } else {
                        //Validate Role
                        String mod = id.MOD;

                        if(mod == null) {
                            String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                            if(modID == null) {
                                RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                                roleBuilder.name("PackPackMod");

                                guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString());
                            } else {
                                id.MOD = modID;
                            }
                        } else {
                            guild.getRoles().collectList().subscribe(ro -> {
                                for(Role r : ro) {
                                    if(r.getId().asString().equals(mod))
                                        return;
                                }

                                String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                                if(modID == null) {
                                    RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                                    roleBuilder.name("PackPackMod");

                                    guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString());
                                } else {
                                    id.MOD = modID;
                                }
                            });
                        }
                    }
                }
            }

            StaticStore.saveServerInfo();
        });

        gate.on(GuildDeleteEvent.class).subscribe(e -> {
            e.getGuild().ifPresent(g -> StaticStore.idHolder.remove(g.getId().asString()));

            StaticStore.saveServerInfo();
        });

        gate.on(RoleDeleteEvent.class).subscribe(e -> {
            Guild guild = e.getGuild().block();

            if(guild == null)
                return;

            IDHolder holder = StaticStore.idHolder.get(guild.getId().asString());

            if(holder != null) {
                String mod = holder.MOD;

                if(mod == null) {
                    String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                    if(modID == null) {
                        RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                        roleBuilder.name("PackPackMod");

                        guild.createRole(roleBuilder.build()).subscribe(r -> holder.MOD = r.getId().asString());
                    } else {
                        holder.MOD = modID;
                    }
                } else {
                    e.getRole().ifPresent(r -> {
                        if(r.getId().asString().equals(mod)) {
                            String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                            if(modID == null) {
                                RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                                roleBuilder.name("PackPackMod");

                                guild.createRole(roleBuilder.build()).subscribe(ro -> holder.MOD = r.getId().asString());
                            } else {
                                holder.MOD = modID;
                            }
                        }
                    });
                }
            } else {
                final IDHolder idh = new IDHolder();

                String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                if(modID == null) {
                    RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                    roleBuilder.name("PackPackMod");

                    guild.createRole(roleBuilder.build()).subscribe(r -> idh.MOD = r.getId().asString());
                } else {
                    idh.MOD = modID;
                }

                StaticStore.idHolder.put(guild.getId().asString(), idh);
            }

            StaticStore.saveServerInfo();
        });

        gate.on(NewsChannelDeleteEvent.class).subscribe(e -> {
            Guild g = e.getChannel().getGuild().block();

            if(g != null) {
                IDHolder idh = StaticStore.idHolder.get(g.getId().asString());

                if(idh == null)
                    idh = new IDHolder();

                if(idh.ANNOUNCE != null && idh.ANNOUNCE.equals(e.getChannel().getId().asString()))
                    idh.ANNOUNCE = null;

                if(idh.GET_ACCESS != null && idh.GET_ACCESS.equals(e.getChannel().getId().asString()))
                    idh.GET_ACCESS = null;

                StaticStore.idHolder.put(g.getId().asString(), idh);
            }
        });

        gate.on(TextChannelDeleteEvent.class).subscribe(e -> {
            Guild g = e.getChannel().getGuild().block();

            if(g != null) {
                IDHolder idh = StaticStore.idHolder.get(g.getId().asString());

                if(idh == null)
                    idh = new IDHolder();

                if(idh.ANNOUNCE != null && idh.ANNOUNCE.equals(e.getChannel().getId().asString()))
                    idh.ANNOUNCE = null;

                if(idh.GET_ACCESS != null && idh.GET_ACCESS.equals(e.getChannel().getId().asString()))
                    idh.GET_ACCESS = null;

                StaticStore.idHolder.put(g.getId().asString(), idh);
            }
        });

        gate.on(GuildCreateEvent.class).subscribe(e -> {
            Guild guild = e.getGuild();

            IDHolder id = StaticStore.idHolder.get(guild.getId().asString());

            if (id == null) {
                final IDHolder idh = new IDHolder();

                String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                if(modID == null) {
                    RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                    roleBuilder.name("PackPackMod");

                    guild.createRole(roleBuilder.build()).subscribe(r -> idh.MOD = r.getId().asString());
                } else {
                    idh.MOD = modID;
                }

                StaticStore.idHolder.put(guild.getId().asString(), idh);
            } else {
                //Validate Role
                String mod = id.MOD;

                if(mod == null) {
                    String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                    if(modID == null) {
                        RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                        roleBuilder.name("PackPackMod");

                        guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString());
                    } else {
                        id.MOD = modID;
                    }
                } else {
                    guild.getRoles().collectList().subscribe(ro -> {
                        for(Role r : ro) {
                            if(r.getId().asString().equals(mod))
                                return;
                        }

                        String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                        if(modID == null) {
                            RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                            roleBuilder.name("PackPackMod");

                            guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString());
                        } else {
                            id.MOD = modID;
                        }
                    });
                }
            }
        });

        gate.on(ReactionAddEvent.class)
                .filter(event -> {
                    MessageChannel mc = event.getChannel().block();

                    if(mc == null)
                        return false;
                    else {
                        AtomicReference<Boolean> mandarin = new AtomicReference<>(false);
                        AtomicReference<Boolean> isMod = new AtomicReference<>(false);
                        AtomicReference<Boolean> canGo = new AtomicReference<>(true);

                        Guild g = event.getGuild().block();

                        IDHolder ids;

                        if(g != null) {
                            ids = StaticStore.idHolder.get(g.getId().asString());
                        } else {
                            return true;
                        }

                        event.getMember().ifPresent(m -> {
                            mandarin.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));

                            if(ids.MOD != null) {
                                isMod.set(StaticStore.rolesToString(m.getRoleIds()).contains(ids.MOD));
                            }

                            ArrayList<String> channels = ids.getAllAllowedChannels(m.getRoleIds());

                            if(channels == null)
                                return;

                            if(channels.isEmpty())
                                canGo.set(false);
                            else {
                                MessageChannel channel = event.getChannel().block();

                                if(channel == null)
                                    return;

                                canGo.set(channels.contains(channel.getId().asString()));
                            }
                        });

                        String acc = ids.GET_ACCESS;

                        return ((acc == null || !mc.getId().asString().equals(ids.GET_ACCESS)) && canGo.get()) || mandarin.get() || isMod.get();
                    }
                }).subscribe(event -> {
            Message msg = event.getMessage().block();

            if(msg == null) {
                StaticStore.logger.uploadLog("Message is null while trying to perform ReactionAddEvent");
                return;
            }

            if(event.getMember().isEmpty()) {
                StaticStore.logger.uploadLog("Member is empty while trying to perform ReactionAddEvent");
            }

            event.getMember().ifPresent(m -> {
                if (StaticStore.holderContainsKey(m.getId().asString())) {
                    Holder<? extends Event> holder = StaticStore.getHolder(m.getId().asString());

                    if(!(holder instanceof MessageHolder))
                        return;

                    MessageHolder<? extends MessageEvent> messageHolder = (MessageHolder<? extends MessageEvent>) holder;

                    if(messageHolder instanceof FormReactionMessageHolder) {
                        StaticStore.logger.uploadLog("Trying to perform FormReactionHolder : "+ messageHolder.canCastTo(ReactionAddEvent.class));
                    }

                    if (messageHolder.canCastTo(ReactionAddEvent.class)) {
                        @SuppressWarnings("unchecked")
                        MessageHolder<ReactionAddEvent> h = (MessageHolder<ReactionAddEvent>) messageHolder;

                        int result = h.handleEvent(event);

                        if(messageHolder instanceof FormReactionMessageHolder) {
                            StaticStore.logger.uploadLog("Result is : "+result);
                        }

                        if (result == Holder.RESULT_FINISH) {
                            messageHolder.clean();
                            StaticStore.removeHolder(m.getId().asString(), messageHolder);
                        } else if (result == Holder.RESULT_FAIL) {
                            StaticStore.logger.uploadLog("W/ReactionEventHolder | Expired process tried to be handled : " + m.getId().asString() + " / " + messageHolder.getClass().getName());
                            System.out.println("ERROR : Expired process tried to be handled : " + m.getId().asString() + " | " + messageHolder.getClass().getName());
                            StaticStore.removeHolder(m.getId().asString(), messageHolder);
                        }
                    }
                }
            });
        });

        gate.on(MemberUpdateEvent.class)
                .filter(event -> {
                    AtomicReference<Boolean> isBot = new AtomicReference<>(false);

                    Member m = event.getMember().block();

                    if(m != null) {
                        isBot.set(m.isBot());
                    }

                    return !isBot.get();
                }).subscribe(event -> {
                    Guild g = event.getGuild().block();

                    if(g == null)
                        return;

                    if(!StaticStore.idHolder.containsKey(g.getId().asString()))
                        return;

                    IDHolder holder = StaticStore.idHolder.get(g.getId().asString());

                    if(holder.BOOSTER == null)
                        return;

                    if(!StaticStore.boosterData.containsKey(g.getId().asString()))
                        return;

                    BoosterHolder booster = StaticStore.boosterData.get(g.getId().asString());

                    event.getMember().subscribe(m -> {
                        if(!booster.serverBooster.containsKey(m.getId().asString()))
                            return;

                        BoosterData data = booster.serverBooster.get(m.getId().asString());

                        if(!m.getRoleIds().contains(Snowflake.of(holder.BOOSTER))) {
                            String role = data.getRole();
                            String emoji = data.getEmoji();

                            if(role != null) {
                                g.getRoleById(Snowflake.of(role)).subscribe(r -> r.delete().subscribe());
                            }

                            if(emoji != null) {
                                g.getGuildEmojiById(Snowflake.of(emoji)).subscribe(e -> e.delete().subscribe());
                            }

                            booster.serverBooster.remove(m.getId().asString());
                        }
                    });
                });

        gate.on(MessageCreateEvent.class)
                .filter(event -> {
                    MessageChannel mc = event.getMessage().getChannel().block();

                    if(mc == null)
                        return false;
                    else {
                        AtomicReference<Boolean> mandarin = new AtomicReference<>(false);
                        AtomicReference<Boolean> isMod = new AtomicReference<>(false);
                        AtomicReference<Boolean> canGo = new AtomicReference<>(true);

                        Guild g = event.getGuild().block();

                        IDHolder ids;

                        if(g != null) {
                            ids = StaticStore.idHolder.get(g.getId().asString());
                        } else {
                            return true;
                        }

                        event.getMember().ifPresent(m -> {
                            mandarin.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));

                            if(ids.MOD != null) {
                                isMod.set(StaticStore.rolesToString(m.getRoleIds()).contains(ids.MOD));
                            }

                            ArrayList<String> channels = ids.getAllAllowedChannels(m.getRoleIds());

                            if(channels == null)
                                return;

                            if(channels.isEmpty())
                                canGo.set(false);
                            else {
                                MessageChannel channel = event.getMessage().getChannel().block();

                                if(channel == null)
                                    return;

                                canGo.set(channels.contains(channel.getId().asString()));
                            }
                        });

                        String acc = ids.GET_ACCESS;

                        return ((acc == null || !mc.getId().asString().equals(ids.GET_ACCESS)) && canGo.get()) || mandarin.get() || isMod.get();
                    }
                }).subscribe(event -> {
                    Guild g = event.getGuild().block();
                    IDHolder ids;

                    if(g != null) {
                        ids = StaticStore.idHolder.get(g.getId().asString());
                    } else {
                        ids = new IDHolder();
                    }

                    Message msg = event.getMessage();

                    MessageChannel ch = msg.getChannel().block();

                    event.getMember().ifPresent(m -> {
                        String prefix = StaticStore.getPrefix(m.getId().asString());

                        if(msg.getContent().startsWith(ids.serverPrefix))
                            prefix = ids.serverPrefix;

                        if(msg.getContent().startsWith(StaticStore.serverPrefix))
                            prefix = StaticStore.serverPrefix;

                        if(StaticStore.holderContainsKey(m.getId().asString())) {
                            Holder<? extends Event> holder = StaticStore.getHolder(m.getId().asString());

                            if(holder instanceof MessageHolder) {
                                MessageHolder<? extends MessageEvent> messageHolder = (MessageHolder<? extends MessageEvent>) holder;

                                if(messageHolder.canCastTo(MessageCreateEvent.class)) {
                                    @SuppressWarnings("unchecked")
                                    MessageHolder<MessageCreateEvent> h = (MessageHolder<MessageCreateEvent>) messageHolder;

                                    int result = h.handleEvent(event);

                                    if(result == Holder.RESULT_FINISH) {
                                        messageHolder.clean();
                                        StaticStore.removeHolder(m.getId().asString(), messageHolder);
                                    } else if(result == Holder.RESULT_FAIL) {
                                        System.out.println("ERROR : Expired process tried to be handled : "+m.getId().asString() + " | "+ messageHolder.getClass().getName());
                                        StaticStore.removeHolder(m.getId().asString(), messageHolder);
                                    }
                                }
                            }
                        }

                        if(ch != null) {
                            IDHolder idh;

                            if(g != null) {
                                idh = StaticStore.idHolder.get(g.getId().asString());
                            } else {
                                idh = new IDHolder();
                            }

                            int lang;

                            if(idh != null) {
                                lang = idh.serverLocale;
                            } else {
                                lang = LangID.EN;
                            }

                            if(StaticStore.locales.containsKey(m.getId().asString())) {
                                lang = StaticStore.locales.get(m.getId().asString());
                            }

                            if(idh == null)
                                idh = StaticStore.idHolder.get(StaticStore.BCU_SERVER);

                            switch (StaticStore.getCommand(msg.getContent(), prefix)) {
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
                                    new Save(ConstraintCommand.ROLE.CONTRIBUTOR, lang, idh).execute(event);
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
                                    new FormStat(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
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
                                    new EnemyStat(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                                    break;
                                case "castle":
                                case "cs":
                                    new Castle(ConstraintCommand.ROLE.MEMBER, lang, idh).execute(event);
                                    break;
                                case "stageinfo":
                                case "si":
                                    new StageInfo(ConstraintCommand.ROLE.MEMBER, lang, idh, 5000).execute(event);
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
                                    new AnimAnalyzer(ConstraintCommand.ROLE.CONTRIBUTOR, lang, idh).execute(event);
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
                                    new ServerJson(ConstraintCommand.ROLE.CONTRIBUTOR, lang, idh).execute(event);
                                    break;
                                case "findstage":
                                case "findst":
                                case "fstage":
                                case "fst":
                                    new FindStage(ConstraintCommand.ROLE.MEMBER, lang, idh, 5000).execute(event);
                                    break;
                                case "suggest":
                                    new Suggest(ConstraintCommand.ROLE.MEMBER, lang, idh, TimeUnit.MINUTES.toMillis(60), gate).execute(event);
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
                                    new SuggestResponse(ConstraintCommand.ROLE.MANDARIN, lang, idh, gate).execute(event);
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
                                    new ContributorAdd(ConstraintCommand.ROLE.MANDARIN, lang, idh, gate).execute(event);
                                    break;
                                case "contributorremove":
                                case "cor":
                                    new ContributorRemove(ConstraintCommand.ROLE.MANDARIN, lang, idh, gate).execute(event);
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
                                    new Publish(ConstraintCommand.ROLE.MANDARIN, lang, idh, gate).execute(event);
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
                            }
                        }
                    });
        });

        gate.onDisconnect().block();
    }

    public static void initialize(String... arg) {
        if(!StaticStore.initialized) {
            CommonStatic.ctx = new PackContext();
            CommonStatic.getConfig().ref = false;

            if(arg.length >= 2) {
                StaticStore.imgur.registerClient(arg[1]);
            }

            StaticStore.readServerInfo();

            AssetDownloader.checkAssetDownload();

            StaticStore.postReadServerInfo();

            LangID.initialize();

            DataToString.initialize();

            try {
                StaticStore.event.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(!StaticStore.contributors.contains(StaticStore.MANDARIN_SMELL)) {
                StaticStore.contributors.add(StaticStore.MANDARIN_SMELL);
            }

            StaticStore.saver = new Timer();
            StaticStore.saver.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(save % 5 == 0) {
                        System.out.println("Save Process");
                        StaticStore.saveServerInfo();

                        Calendar c = Calendar.getInstance();

                        EventFactor.currentYear = c.get(Calendar.YEAR);

                        save = 1;
                    } else {
                        save++;
                    }

                    for(SpamPrevent spam : StaticStore.spamData.values()) {
                        if(spam.count > 0)
                            spam.count--;
                    }
                }
            }, 0, TimeUnit.MINUTES.toMillis(1));

            StaticStore.initialized = true;
        }
    }
}
