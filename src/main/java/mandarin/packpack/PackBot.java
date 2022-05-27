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
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.*;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.request.RouterOptions;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Image;
import mandarin.packpack.commands.Locale;
import mandarin.packpack.commands.*;
import mandarin.packpack.commands.TimeZone;
import mandarin.packpack.commands.bc.*;
import mandarin.packpack.commands.bot.*;
import mandarin.packpack.commands.data.*;
import mandarin.packpack.commands.server.*;
import mandarin.packpack.supporter.AssetDownloader;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.PackContext;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import mandarin.packpack.supporter.event.GachaSet;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.FormReactionMessageHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.MessageHolder;
import mandarin.packpack.supporter.server.slash.SlashBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Usage : java -jar JARNAME DISCORD_BOT_TOKEN IMGUR_API_ACCESS_TOKEN
 */
public class PackBot {
    public static int save = 0;
    public static int event = 0;
    public static int pfp = 0;
    public static boolean eventInit = false;
    public static boolean develop = false;

    public static final String normal = "p!help, but under Construction!";
    public static final String dev = "p!help, being developed, bot may not response";

    public static void main(String[] args) {
        initialize(args);

        final String TOKEN = args[0];

        DiscordClientBuilder<DiscordClient, RouterOptions> builder = DiscordClientBuilder.create(TOKEN);

        DiscordClient client = builder.build();
        GatewayDiscordClient gate = client.gateway().setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_EMOJIS, Intent.GUILD_INTEGRATIONS, Intent.GUILD_MESSAGES, Intent.GUILD_MESSAGE_REACTIONS, Intent.DIRECT_MESSAGES, Intent.DIRECT_MESSAGE_REACTIONS)).login().block();

        if(gate == null) {
            return;
        }

        StaticStore.logger = new Logger(gate);

        StaticStore.saver = new Timer();
        StaticStore.saver.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();

                if(save % 5 == 0) {
                    System.out.println("Save Process");
                    StaticStore.saveServerInfo();

                    EventFactor.currentYear = c.get(Calendar.YEAR);

                    save = 1;
                } else {
                    save++;
                }

                if(pfp % 60 == 0) {
                    try {
                        String fileName;

                        switch (c.get(Calendar.MONTH) + 1) {
                            case 12:
                                fileName = "BotDec.png";
                                break;
                            case 1:
                                fileName = "BotJan.png";
                                break;
                            case 2:
                                fileName = "BotFeb.png";
                                break;
                            case 3:
                                fileName = "BotMar.png";
                                break;
                            case 4:
                                fileName = "BotApr.png";
                                break;
                            default:
                                fileName = "Bot.png";
                                break;
                        }

                        File f = new File("./data/bot/", fileName);

                        if(f.exists()) {
                            FileInputStream fis = new FileInputStream(f);

                            gate.edit(UserEditSpec.builder().avatar(Image.ofRaw(fis.readAllBytes(), Image.Format.PNG)).build()).subscribe(null, e -> {});

                            fis.close();
                        }
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }

                    pfp = 1;
                } else {
                    pfp++;
                }

                if(event % 10 == 0) {
                    System.out.println("Checking event data");

                    try {
                        boolean[][] result = StaticStore.event.checkUpdates();

                        boolean doNotify = false;

                        for(int i = 0; i < result.length; i++) {
                            for(int j = 0; j < result[i].length; j++) {
                                if(result[i][j]) {
                                    doNotify = true;
                                    break;
                                }
                            }

                            if(doNotify)
                                break;
                        }

                        if(doNotify) {
                            StaticStore.saveServerInfo();

                            if(!eventInit)
                                eventInit = true;
                            else
                                notifyEvent(gate, result);
                        }
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "Error happened while trying to check event data");
                    }

                    event = 1;
                } else {
                    event++;
                }

                for(SpamPrevent spam : StaticStore.spamData.values()) {
                    if(spam.count > 0)
                        spam.count--;
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));

        SlashBuilder.build(gate);

        gate.updatePresence(ClientPresence.online(ClientActivity.playing(develop ? dev : normal))).subscribe();

        gate.getGuilds().collectList().subscribe(l -> {
            for (Guild guild : l) {
                if (guild != null) {
                    IDHolder id = StaticStore.idHolder.get(guild.getId().asString());

                    AtomicReference<Boolean> warned = new AtomicReference<>(false);

                    if (id == null) {
                        final IDHolder idh = new IDHolder();

                        String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                        if(modID == null) {
                            RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                            roleBuilder.name("PackPackMod");

                            guild.createRole(roleBuilder.build()).subscribe(r -> idh.MOD = r.getId().asString(), err -> {
                                if(!warned.get()) {
                                    guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", idh.serverLocale).replace("_", guild.getName())).subscribe()));
                                    warned.set(true);
                                }
                            });
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

                                guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString(), err -> {
                                    if(!warned.get()) {
                                        guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", id.serverLocale).replace("_", guild.getName())).subscribe()));
                                        warned.set(true);
                                    }
                                });
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

                                    guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString(), err -> {
                                        if(!warned.get()) {
                                            guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", id.serverLocale).replace("_", guild.getName())).subscribe()));
                                            warned.set(true);
                                        }
                                    });
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

            AtomicReference<Boolean> warned = new AtomicReference<>(false);

            if(holder != null) {
                String mod = holder.MOD;

                if(mod == null) {
                    String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                    if(modID == null) {
                        RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                        roleBuilder.name("PackPackMod");

                        guild.createRole(roleBuilder.build()).subscribe(r -> holder.MOD = r.getId().asString(), err -> {
                            if(!warned.get()) {
                                guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", holder.serverLocale).replace("_", guild.getName())).subscribe()));
                                warned.set(true);
                            }
                        });
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

                                guild.createRole(roleBuilder.build()).subscribe(ro -> holder.MOD = ro.getId().asString(), err -> {
                                    if(!warned.get()) {
                                        guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", holder.serverLocale).replace("_", guild.getName())).subscribe()));
                                        warned.set(true);
                                    }
                                });
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

                    guild.createRole(roleBuilder.build()).subscribe(r -> idh.MOD = r.getId().asString(), err -> {
                        if(!warned.get()) {
                            guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", idh.serverLocale).replace("_", guild.getName())).subscribe()));
                            warned.set(true);
                        }
                    });
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

                if(idh.event != null && idh.event.equals(e.getChannel().getId().asString()))
                    idh.event = null;

                if(idh.logDM != null && idh.logDM.equals(e.getChannel().getId().asString()))
                    idh.logDM = null;

                StaticStore.idHolder.put(g.getId().asString(), idh);

                if(StaticStore.scamLinkHandlers.servers.containsKey(g.getId().asString())) {
                    String channel = StaticStore.scamLinkHandlers.servers.get(g.getId().asString()).channel;

                    if(channel != null && channel.equals(e.getChannel().getId().asString()))
                        StaticStore.scamLinkHandlers.servers.remove(g.getId().asString());
                }
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

                if(idh.event != null && idh.event.equals(e.getChannel().getId().asString()))
                    idh.event = null;

                if(idh.logDM != null && idh.logDM.equals(e.getChannel().getId().asString()))
                    idh.logDM = null;

                StaticStore.idHolder.put(g.getId().asString(), idh);

                if(StaticStore.scamLinkHandlers.servers.containsKey(g.getId().asString())) {
                    String channel = StaticStore.scamLinkHandlers.servers.get(g.getId().asString()).channel;

                    if(channel != null && channel.equals(e.getChannel().getId().asString()))
                        StaticStore.scamLinkHandlers.servers.remove(g.getId().asString());
                }
            }
        });

        gate.on(GuildCreateEvent.class).subscribe(e -> {
            Guild guild = e.getGuild();

            IDHolder id = StaticStore.idHolder.get(guild.getId().asString());

            AtomicReference<Boolean> warned = new AtomicReference<>(false);

            if (id == null) {
                final IDHolder idh = new IDHolder();

                String modID = StaticStore.getRoleIDByName("PackPackMod", guild);

                if(modID == null) {
                    RoleCreateSpec.Builder roleBuilder = RoleCreateSpec.builder();

                    roleBuilder.name("PackPackMod");

                    guild.createRole(roleBuilder.build()).subscribe(r -> idh.MOD = r.getId().asString(), err -> {
                        if(!warned.get()) {
                            guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", idh.serverLocale).replace("_", guild.getName())).subscribe()));
                            warned.set(true);
                        }
                    });
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

                        guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString(), err -> {
                            if(!warned.get()) {
                                guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", id.serverLocale).replace("_", guild.getName())).subscribe()));
                                warned.set(true);
                            }
                        });
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

                            guild.createRole(roleBuilder.build()).subscribe(r -> id.MOD = r.getId().asString(), err -> {
                                if(!warned.get()) {
                                    guild.getOwner().subscribe(o -> o.getPrivateChannel().subscribe(po -> po.createMessage(LangID.getStringByID("needroleperm", id.serverLocale).replace("_", guild.getName())).subscribe()));
                                    warned.set(true);
                                }
                            });
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
                    AtomicReference<Boolean> isOptedOut = new AtomicReference<>(false);

                    event.getMember().ifPresent(m -> {
                        if(StaticStore.optoutMembers.contains(m.getId().asString())) {
                            isOptedOut.set(true);
                            return;
                        }

                        Guild g = event.getGuild().block();
                        Message msg = event.getMessage();

                        if(!m.isBot() && !StaticStore.optoutMembers.contains(m.getId().asString()) && g != null && StaticStore.scamLinkHandlers.servers.containsKey(g.getId().asString()) && ScamLinkHandler.validScammingUser(msg.getContent())) {
                            String link = ScamLinkHandler.getLinkFromMessage(msg.getContent());

                            if(link != null) {
                                link = link.replace("http://", "").replace("https://", "");
                            }

                            StaticStore.scamLinkHandlers.servers.get(g.getId().asString()).takeAction(gate, link, m, g);
                            StaticStore.logger.uploadLog("I caught compromised user\nLINK : "+link+"\nGUILD : "+g.getName()+" ("+g.getId().asString()+")\nMEMBER : "+m.getDisplayName()+" ("+m.getId().asString()+")");

                            msg.delete().subscribe();
                        }
                    });

                    if(isOptedOut.get())
                        return false;

                    try {
                        MessageChannel mc = event.getMessage().getChannel().block();

                        if(mc == null)
                            return true;
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
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "Something went wrong");

                        return false;
                    }
                }).subscribe(event -> {
                    Message msg = event.getMessage();

                    event.getMember().ifPresentOrElse(m -> {
                        MessageChannel ch = msg.getChannel().block();

                        if(ch != null && !(ch instanceof PrivateChannel)) {
                            Guild g = event.getGuild().block();
                            IDHolder ids;

                            if(g != null) {
                                ids = StaticStore.idHolder.get(g.getId().asString());
                            } else {
                                ids = new IDHolder();
                            }

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

                            if(StaticStore.config.containsKey(m.getId().asString())) {
                                lang = StaticStore.config.get(m.getId().asString()).lang;
                            }

                            if(idh == null)
                                idh = StaticStore.idHolder.get(StaticStore.BCU_SERVER);

                            if(lang == -1)
                                lang = idh.serverLocale;

                            ConfigHolder c;

                            if(StaticStore.config.containsKey(m.getId().asString()))
                                c = StaticStore.config.get(m.getId().asString());
                            else
                                c = new ConfigHolder();

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
                                    new Background(ConstraintCommand.ROLE.MEMBER, lang, idh, 10000, gate).execute(event);
                                    break;
                                case "test":
                                    new Test(ConstraintCommand.ROLE.MANDARIN, lang, idh, "test", gate).execute(event);
                                    break;
                                case "formgif":
                                case "fgif":
                                case "fg":
                                    new FormGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif", gate).execute(event);
                                    break;
                                case "enemygif":
                                case "egif":
                                case "eg":
                                    new EnemyGif(ConstraintCommand.ROLE.MEMBER, lang, idh, "gif", gate).execute(event);
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
                                    new FindStage(ConstraintCommand.ROLE.MEMBER, lang, idh, c, 5000).execute(event);
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
                                case "fixrole":
                                case "fr":
                                    new FixRole(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                                    break;
                                case "registerfixing":
                                case "rf":
                                    new RegisterFixing(ConstraintCommand.ROLE.MANDARIN, lang, idh, client).execute(event);
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
                                    new CheckEventUpdate(ConstraintCommand.ROLE.CONTRIBUTOR, lang, idh, gate).execute(event);
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
                                    new SetBCVersion(ConstraintCommand.ROLE.CONTRIBUTOR, lang, idh).execute(event);
                                    break;
                                case "logout":
                                    new LogOut(ConstraintCommand.ROLE.MANDARIN, lang, idh, gate).execute(event);
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
                                    new StatAnalyzer(ConstraintCommand.ROLE.CONTRIBUTOR, lang, idh).execute(event);
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
                            }
                        }
                    }, () -> {
                        MessageChannel ch = msg.getChannel().block();

                        if(ch instanceof PrivateChannel) {
                            String content = msg.getContent();

                            if(content.contains("http")) {
                                if(content.length() > 1000) {
                                    content = content.substring(0, 997)+"...";
                                }

                                String finalContent = content;

                                msg.getAuthor().ifPresent(a -> {
                                    if(!StaticStore.optoutMembers.contains(a.getId().asString()))
                                        notifyModerators(a, finalContent, gate);
                                });
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

            StaticStore.readServerInfo();

            if(arg.length >= 2) {
                StaticStore.imgur.registerClient(arg[1]);
            }

            LangID.initialize();

            AssetDownloader.checkAssetDownload();

            StaticStore.postReadServerInfo();

            DataToString.initialize();

            GachaSet.initialize();

            EventFactor.readMissionReward();

            try {
                EventFileGrabber.initialize();
                StaticStore.event.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(!StaticStore.contributors.contains(StaticStore.MANDARIN_SMELL)) {
                StaticStore.contributors.add(StaticStore.MANDARIN_SMELL);
            }

            StaticStore.initialized = true;
        }
    }

    public static void notifyModerators(User u, String content, GatewayDiscordClient gate) {
        List<Guild> guilds = gate.getGuilds().collectList().block();

        if(guilds == null)
            return;

        for(Guild g : guilds) {
            String gID = g.getId().asString();

            u.asMember(g.getId()).subscribe(m -> {
                IDHolder holder = StaticStore.idHolder.get(gID);

                if(holder == null) {
                    StaticStore.logger.uploadLog("No ID Holder found for guild ID : "+gID);
                    return;
                }

                if(StaticStore.isNumeric(holder.logDM)) {
                    Channel ch = gate.getChannelById(Snowflake.of(holder.logDM)).block();

                    if(ch instanceof MessageChannel) {
                        MessageCreateSpec.Builder builder = MessageCreateSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .color(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                                        .description(LangID.getStringByID("watdm_suslink", holder.serverLocale))
                                        .author(m.getDisplayName()+" ("+m.getId().asString()+")", null, m.getAvatarUrl())
                                        .addField(EmbedCreateFields.Field.of(LangID.getStringByID("watdm_content", holder.serverLocale), content, true))
                                        .build()
                                );

                        ((MessageChannel) ch).createMessage(builder.build()).subscribe();
                    }
                }
            }, (e) -> {});
        }
    }

    public static void notifyEvent(GatewayDiscordClient gate, boolean[][] r) {
        List<Guild> guilds = gate.getGuilds().collectList().block();

        if(guilds == null)
            return;

        boolean sent = false;

        for(Guild g : guilds) {
            String gID = g.getId().asString();

            IDHolder holder = StaticStore.idHolder.get(gID);

            if(holder == null) {
                StaticStore.logger.uploadLog("No ID Holder found for guild ID : "+gID);
                continue;
            }

            boolean done = false;

            for(int i = 0; i < r.length; i++) {
                boolean eventDone = false;

                for(int j = 0; j < r[i].length; j++) {
                    if(r[i][j] && holder.eventLocale.contains(i) && holder.event != null) {
                        try {
                            Channel ch = gate.getChannelById(Snowflake.of(holder.event)).block();

                            if(ch instanceof MessageChannel) {
                                if(j == EventFactor.SALE) {
                                    ArrayList<String> result = StaticStore.event.printStageEvent(i, holder.serverLocale, false, holder.eventRaw, false, 0);

                                    if(result.isEmpty())
                                        continue;

                                    boolean wasDone = done;

                                    done = true;

                                    if(!eventDone) {
                                        eventDone = true;
                                        ((MessageChannel) ch).createMessage(MessageCreateSpec.builder().content((wasDone ? "** **\n" : "") + LangID.getStringByID("event_loc"+i, holder.serverLocale)).build()).subscribe();
                                    }

                                    boolean goWithFile = false;

                                    for(int k = 0; k < result.size(); k++) {
                                        if(result.get(k).length() >= 1950) {
                                            goWithFile = true;
                                            break;
                                        }
                                    }

                                    if(goWithFile) {
                                        MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

                                        StringBuilder total = new StringBuilder(LangID.getStringByID("event_stage", holder.serverLocale).replace("**", "")).append("\n\n");

                                        for(int k = 0; k < result.size(); k++) {
                                            total.append(result.get(k).replace("```scss\n", "").replace("```", ""));

                                            if(k < result.size() - 1)
                                                total.append("\n");
                                        }

                                        File temp = new File("./temp");

                                        if(!temp.exists() && !temp.mkdirs()) {
                                            StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());
                                            return;
                                        }

                                        File res = new File(temp, StaticStore.findFileName(temp, "event", ".txt"));

                                        if(!res.exists() && !res.createNewFile()) {
                                            StaticStore.logger.uploadLog("Failed to create file : "+res.getAbsolutePath());
                                            return;
                                        }

                                        BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

                                        writer.write(total.toString());

                                        writer.close();

                                        FileInputStream fis = new FileInputStream(res);

                                        builder.content((wasDone ? "** **\n" : "") + LangID.getStringByID("printstage_toolong", holder.serverLocale))
                                                .addFile(MessageCreateFields.File.of("event.txt", fis));

                                        ((MessageChannel) ch).createMessage(builder.build()).subscribe(null, (e) -> {
                                            StaticStore.logger.uploadErrorLog(e, "Failed to perform uploading stage event data");

                                            try {
                                                fis.close();
                                            } catch (IOException ex) {
                                                StaticStore.logger.uploadErrorLog(ex, "Failed close stream while uploading stage event data");
                                            }

                                            if(res.exists() && !res.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                            }
                                        }, () -> {
                                            try {
                                                fis.close();
                                            } catch (IOException e) {
                                                StaticStore.logger.uploadErrorLog(e, "Failed close stream while uploading stage event data");
                                            }

                                            if(res.exists() && !res.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                            }
                                        });
                                    } else {
                                        for(int k = 0; k < result.size(); k++) {
                                            MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

                                            StringBuilder merge = new StringBuilder();

                                            if(k == 0) {
                                                if(wasDone) {
                                                    merge.append("** **\n");
                                                }

                                                merge.append(LangID.getStringByID("event_stage", holder.serverLocale)).append("\n\n");
                                            } else {
                                                merge.append("** **\n");
                                            }

                                            while(merge.length() < 2000) {
                                                if(k >= result.size())
                                                    break;

                                                if(result.get(k).length() + merge.length() >= 2000) {
                                                    k--;
                                                    break;
                                                }

                                                merge.append(result.get(k));

                                                if(k < result.size() - 1) {
                                                    merge.append("\n");
                                                }

                                                k++;
                                            }

                                            builder.content(merge.toString());
                                            builder.allowedMentions(AllowedMentions.builder().build());

                                            ((MessageChannel) ch).createMessage(builder.build()).subscribe();
                                        }
                                    }
                                } else {
                                    String result;

                                    if(j == EventFactor.GATYA)
                                        result = StaticStore.event.printGachaEvent(i, holder.serverLocale, false, holder.eventRaw, false, 0);
                                    else
                                        result = StaticStore.event.printItemEvent(i, holder.serverLocale, false, holder.eventRaw, false, 0);

                                    if(result.isBlank()) {
                                        continue;
                                    }

                                    boolean wasDone = done;

                                    done = true;

                                    if(!eventDone) {
                                        eventDone = true;
                                        ((MessageChannel) ch).createMessage(MessageCreateSpec.builder().content((wasDone ? "** **\n" : "") + LangID.getStringByID("event_loc"+i, holder.serverLocale)).build()).subscribe();
                                    }

                                    MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

                                    builder.allowedMentions(AllowedMentions.builder().build());

                                    if(result.length() >= 1980) {
                                        File temp = new File("./temp");

                                        if(!temp.exists() && !temp.mkdirs()) {
                                            StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());
                                            return;
                                        }

                                        File res = new File(temp, StaticStore.findFileName(temp, "event", ".txt"));

                                        if(!res.exists() && !res.createNewFile()) {
                                            StaticStore.logger.uploadLog("Failed to create file : "+res.getAbsolutePath());
                                            return;
                                        }

                                        BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

                                        writer.write(result);

                                        writer.close();

                                        FileInputStream fis = new FileInputStream(res);

                                        String lID;

                                        if(j == EventFactor.GATYA) {
                                            lID = "printgacha_toolong";
                                        } else {
                                            lID = "printitem_toolong";
                                        }

                                        builder.content((wasDone ? "** **\n" : "") + LangID.getStringByID(lID, holder.serverLocale))
                                                .addFile(MessageCreateFields.File.of("event.txt", fis));

                                        ((MessageChannel) ch).createMessage(builder.build()).subscribe(null, (e) -> {
                                            StaticStore.logger.uploadErrorLog(e, "Failed to perform uploading stage event data");

                                            try {
                                                fis.close();
                                            } catch (IOException ex) {
                                                StaticStore.logger.uploadErrorLog(ex, "Failed close stream while uploading stage event data");
                                            }

                                            if(res.exists() && !res.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                            }
                                        }, () -> {
                                            try {
                                                fis.close();
                                            } catch (IOException e) {
                                                StaticStore.logger.uploadErrorLog(e, "Failed close stream while uploading stage event data");
                                            }

                                            if(res.exists() && !res.delete()) {
                                                StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                            }
                                        });
                                    } else {
                                        builder.content((wasDone ? "** **\n" : "") + result);

                                        ((MessageChannel) ch).createMessage(builder.build()).subscribe();
                                    }
                                }
                            }
                        } catch(Exception ignored) {

                        }
                    }
                }
            }

            if(done && holder.event != null) {
                sent = true;

                Channel ch = gate.getChannelById(Snowflake.of(holder.event)).block();

                if(ch instanceof MessageChannel) {
                    ((MessageChannel) ch).createMessage(MessageCreateSpec.builder().content(LangID.getStringByID("event_warning", holder.serverLocale)).build()).subscribe();
                }
            }
        }

        if(sent) {
            StaticStore.logger.uploadLog("<@"+StaticStore.MANDARIN_SMELL+"> I caught new event data and successfully announced analyzed data to servers. Below is the updated list : \n\n"+parseResult(r));
        }
    }

    private static String parseResult(boolean[][] result) {
        StringBuilder r = new StringBuilder();

        for(int i = 0; i < result.length; i++) {
            for(int j = 0; j < result[i].length; j++) {
                if(result[i][j]) {
                    r.append(getLocale(i)).append(" : ").append(getFile(j)).append("\n");
                }
            }
        }

        String res = r.toString();

        if(!res.isBlank()) {
            return res.substring(0, res.length() - 1);
        } else {
            return "";
        }
    }

    private static String getLocale(int loc) {
        switch (loc) {
            case EventFactor.EN:
                return "en";
            case EventFactor.ZH:
                return "tw";
            case EventFactor.KR:
                return "kr";
            default:
                return "jp";
        }
    }

    private static String getFile(int f) {
        switch (f) {
            case EventFactor.GATYA:
                return "gatya";
            case EventFactor.ITEM:
                return "item";
            default:
                return "sale";
        }
    }
}
