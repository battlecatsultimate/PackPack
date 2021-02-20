package mandarin.packpack;

import common.CommonStatic;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.request.RouterOptions;
import mandarin.packpack.commands.*;
import mandarin.packpack.commands.bc.*;
import mandarin.packpack.commands.data.AnimAnalyzer;
import mandarin.packpack.commands.data.StageImage;
import mandarin.packpack.commands.data.StmImage;
import mandarin.packpack.commands.server.*;
import mandarin.packpack.supporter.AssetDownloader;
import mandarin.packpack.supporter.PackContext;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Usage : java -jar JARNAME DISCORD_BOT_TOKEN IMGUR_API_ACCESS_TOKEN
 */
public class PackBot {
    public static void main(String[] args) {
        initialize(args);

        final String TOKEN = args[0];

        DiscordClientBuilder<DiscordClient, RouterOptions> builder = DiscordClientBuilder.create(TOKEN);

        DiscordClient client = builder.build();

        GatewayDiscordClient gate = client.gateway().login().block();

        if(gate == null) {
            return;
        }

        gate.updatePresence(Presence.online(Activity.playing("p!help, but under Construction!"))).subscribe();

        gate.getGuilds().collectList().subscribe(l -> {
            for (Guild guild : l) {
                if (guild != null) {
                    IDHolder id = StaticStore.holder.get(guild.getId().asString());

                    if (id == null) {
                        final IDHolder idh = new IDHolder();

                        guild.createRole(r -> r.setName("PackPackMod")).subscribe(r -> idh.MOD = r.getId().asString());

                        StaticStore.holder.put(guild.getId().asString(), idh);
                    }
                }
            }

            StaticStore.saveServerInfo();
        });

        gate.on(MessageCreateEvent.class)
                .filter(event -> {
                    MessageChannel mc = event.getMessage().getChannel().block();

                    if(mc == null)
                        return false;
                    else {
                        AtomicReference<Boolean> mandarin = new AtomicReference<>(false);
                        AtomicReference<Boolean> isMod = new AtomicReference<>(false);

                        Guild g = event.getGuild().block();

                        IDHolder ids;

                        if(g != null) {
                            ids = StaticStore.holder.get(g.getId().asString());
                        } else {
                            ids = new IDHolder();
                        }

                        event.getMember().ifPresent(m -> {
                            mandarin.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));

                            if(ids.MOD != null) {
                                isMod.set(StaticStore.rolesToString(m.getRoleIds()).contains(ids.MOD));
                            }
                        });

                        String acc = ids.GET_ACCESS;

                        return (acc == null || !mc.getId().asString().equals(ids.GET_ACCESS)) || mandarin.get() || isMod.get();
                    }
                }).subscribe(event -> {
                    Guild g = event.getGuild().block();
                    IDHolder ids;

                    if(g != null) {
                        ids = StaticStore.holder.get(g.getId().asString());
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

                        if(StaticStore.formHolder.containsKey(m.getId().asString())) {
                            FormStatHolder holder = StaticStore.formHolder.get(m.getId().asString());

                            int result = holder.handleEvent(event);

                            if(result == FormStatHolder.RESULT_FINISH) {
                                holder.clean();
                                StaticStore.formHolder.remove(m.getId().asString());
                            } else if(result == FormStatHolder.RESULT_FAIL) {
                                System.out.println("ERROR : Expired process tried to be handled : "+m.getId().asString());
                                StaticStore.formHolder.remove(m.getId().asString());
                            }
                        }

                        if(StaticStore.enemyHolder.containsKey(m.getId().asString())) {
                            EnemyStatHolder holder = StaticStore.enemyHolder.get(m.getId().asString());

                            int result = holder.handleEvent(event);

                            if(result == EnemyStatHolder.RESULT_FINISH) {
                                holder.clean();
                                StaticStore.formHolder.remove(m.getId().asString());
                            } else if(result == EnemyStatHolder.RESULT_FAIL) {
                                System.out.println("ERROR : Expired process tried to be handled : "+m.getId().asString()+"|"+m.getNickname().orElse(m.getUsername()));
                                StaticStore.enemyHolder.remove(m.getId().asString());
                            }
                        }

                        if(StaticStore.stageHolder.containsKey(m.getId().asString())) {
                            StageInfoHolder holder = StaticStore.stageHolder.get(m.getId().asString());

                            int result = holder.handleEvent(event);

                            if(result == StageInfoHolder.RESULT_FINISH) {
                                holder.clean();
                                StaticStore.stageHolder.remove(m.getId().asString());
                            } else if(result == StageInfoHolder.RESULT_FAIL) {
                                System.out.println("ERROR : Expired process tried to be handled : "+m.getId().asString()+"|"+m.getNickname().orElse(m.getUsername()));
                                StaticStore.stageHolder.remove(m.getId().asString());
                            }
                        }

                        if(StaticStore.formAnimHolder.containsKey(m.getId().asString())) {
                            FormAnimHolder holder = StaticStore.formAnimHolder.get(m.getId().asString());

                            int result = holder.handleEvent(event);

                            if(result == FormAnimHolder.RESULT_FINISH) {
                                holder.clean();
                                StaticStore.formAnimHolder.remove(m.getId().asString());
                            } else if(result == FormAnimHolder.RESULT_FAIL) {
                                System.out.println("ERROR : Expired process tried to be handled : "+m.getId().asString()+"|"+m.getNickname().orElse(m.getUsername()));
                                StaticStore.formAnimHolder.remove(m.getId().asString());
                            }
                        }

                        if(StaticStore.enemyAnimHolder.containsKey(m.getId().asString())) {
                            EnemyAnimHolder holder = StaticStore.enemyAnimHolder.get(m.getId().asString());

                            int result = holder.handleEvent(event);

                            if(result == EnemyAnimHolder.RESULT_FINISH) {
                                holder.clean();
                                StaticStore.enemyAnimHolder.remove(m.getId().asString());
                            } else if(result == EnemyAnimHolder.RESULT_FAIL) {
                                System.out.println("ERROR : Expired process tried to be handled : "+m.getId().asString()+"|"+m.getNickname().orElse(m.getUsername()));
                                StaticStore.enemyAnimHolder.remove(m.getId().asString());
                            }
                        }

                        if(StaticStore.animHolder.containsKey(m.getId().asString())) {
                            AnimHolder holder = StaticStore.animHolder.get(m.getId().asString());

                            try {
                                int result = holder.handleEvent(event);

                                if(result == EnemyAnimHolder.RESULT_FINISH) {
                                    StaticStore.animHolder.remove(m.getId().asString());
                                }else if(result == AnimHolder.RESULT_FAIL) {
                                    System.out.println("ERROR : Expired process tried to be handled : "+m.getId().asString()+"|"+m.getNickname().orElse(m.getUsername()));
                                    StaticStore.animHolder.remove(m.getId().asString());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if(ch != null) {
                            IDHolder idh;

                            if(g != null) {
                                idh = StaticStore.holder.get(g.getId().asString());
                            } else {
                                idh = new IDHolder();
                            }

                            int lang;

                            if(g != null) {
                                if(g.getId().asString().equals(StaticStore.BCU_SERVER))
                                    lang = LangID.EN;
                                else if(g.getId().asString().equals(StaticStore.BCU_KR_SERVER))
                                    lang = LangID.KR;
                                else
                                    lang = LangID.EN;
                            } else {
                                lang = LangID.EN;
                            }

                            if(StaticStore.locales.containsKey(m.getId().asString())) {
                                lang = StaticStore.locales.get(m.getId().asString());
                            }

                            if(idh == null)
                                idh = StaticStore.holder.get(StaticStore.BCU_SERVER);

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
                                    new Save(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
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
                                case "helpbc":
                                case "hbc":
                                    new HelpBC(lang, idh).execute(event);
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
                                    new IDSet(ConstraintCommand.ROLE.MANDARIN, lang, idh).execute(event);
                                    break;
                                case "clearcache":
                                    new ClearCache(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
                                    break;
                                case "aa":
                                case "animanalyzer":
                                    new AnimAnalyzer(ConstraintCommand.ROLE.MOD, lang, idh).execute(event);
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
            StaticStore.readServerInfo();

            if(arg.length >= 2) {
                StaticStore.imgur.registerClient(arg[1]);
            }

            AssetDownloader.checkAssetDownload();

            LangID.initialize();

            DataToString.initialize();

            StaticStore.holder.put("490262537527623692", new IDHolder(
                    "563745009912774687",
                    "632835571655507968", "490940081738350592",
                    "490940151501946880", "787391428916543488",
                    "632836623931015185", "632836623931015185"
            ));

            StaticStore.holder.put("679858366389944409", new IDHolder(
                    "679870747694596121",
                    "679869691656667157", "743808872376041553",
                    "679870744561188919", "800632019418742824",
                    null, "689333420794707984"
            ));

            StaticStore.saver = new Timer();
            StaticStore.saver.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("Save Process");
                    StaticStore.saveServerInfo();
                }
            }, 0, TimeUnit.MINUTES.toMillis(5));

            StaticStore.initialized = true;
        }
    }
}
