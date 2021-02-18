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

        gate.on(MessageCreateEvent.class)
                .filter(event -> {
                    MessageChannel mc = event.getMessage().getChannel().block();

                    if(mc == null)
                        return false;
                    else {
                        AtomicReference<Boolean> mandarin = new AtomicReference<>(false);

                        event.getMember().ifPresent(m -> mandarin.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL)));

                        Guild g = event.getGuild().block();

                        IDHolder ids;

                        if(g != null) {
                            ids = StaticStore.holder.get(g.getId().asString());
                        } else {
                            ids = StaticStore.holder.get(StaticStore.BCU_SERVER);
                        }

                        if(ids == null)
                            ids = StaticStore.holder.get(StaticStore.BCU_SERVER);

                        return mc.getId().asString().equals(ids.BOT_COMMAND) || mandarin.get();
                    }
                }).subscribe(event -> {
                    Message msg = event.getMessage();

                    MessageChannel ch = msg.getChannel().block();

                    event.getMember().ifPresent(m -> {
                        String prefix = StaticStore.getPrefix(m.getId().asString());

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

                        if(ch != null) {
                            Guild g = event.getGuild().block();

                            IDHolder ids;

                            if(g != null) {
                                ids = StaticStore.holder.get(g.getId().asString());
                            } else {
                                ids = StaticStore.holder.get(StaticStore.BCU_SERVER);
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

                            if(ids == null)
                                ids = StaticStore.holder.get(StaticStore.BCU_SERVER);

                            switch (StaticStore.getCommand(msg.getContent(), prefix)) {
                                case "checkbcu":
                                    new CheckBCU(lang, ids).execute(event);
                                    break;
                                case "bcustat":
                                    new BCUStat(lang, ids).execute(event);
                                    break;
                                case "analyze":
                                    new Analyze(ConstraintCommand.ROLE.MANDARIN, lang, ids).execute(event);
                                    break;
                                case "help":
                                    new Help(lang).execute(event);
                                    break;
                                case "prefix":
                                    new Prefix(ConstraintCommand.ROLE.MEMBER, lang, ids).execute(event);
                                    break;
                                case "serverpre":
                                    new ServerPrefix(ConstraintCommand.ROLE.MOD, lang, ids).execute(event);
                                    break;
                                case "save":
                                    new Save(ConstraintCommand.ROLE.MANDARIN, lang, ids).execute(event);
                                    break;
                                case "stimg":
                                case "stimage":
                                case "stageimg":
                                case "stageimage":
                                    new StageImage(ConstraintCommand.ROLE.MEMBER, lang, ids).execute(event);
                                    break;
                                case "stmimg":
                                case "stmimage":
                                case "stagemapimg":
                                case "stagemapimage":
                                    new StmImage(ConstraintCommand.ROLE.MEMBER, lang, ids).execute(event);
                                    break;
                                case "formstat":
                                case "fs":
                                    new FormStat(ConstraintCommand.ROLE.MEMBER, lang, ids).execute(event);
                                    break;
                                case "locale":
                                    new Locale(ConstraintCommand.ROLE.MEMBER, lang, ids).execute(event);
                                    break;
                                case "music":
                                case "ms":
                                    new Music(ConstraintCommand.ROLE.MEMBER, lang, ids, "music_").execute(event);
                                    break;
                                case "enemystat":
                                case "es":
                                    new EnemyStat(ConstraintCommand.ROLE.MEMBER, lang, ids).execute(event);
                                    break;
                                case "castle":
                                case "cs":
                                    new Castle(ConstraintCommand.ROLE.MEMBER, lang, ids).execute(event);
                                    break;
                                case "helpbc":
                                case "hbc":
                                    new HelpBC(lang).execute(event);
                                    break;
                                case "stageinfo":
                                case "si":
                                    new StageInfo(TimedConstraintCommand.ROLE.MEMBER, lang, ids, 5000).execute(event);
                                    break;
                                case "memory":
                                case "mm":
                                    new Memory(ConstraintCommand.ROLE.DEV, lang, ids).execute(event);
                                    break;
                                case "formimage":
                                case "formimg":
                                case "fimage":
                                case "fimg":
                                    new FormImage(TimedConstraintCommand.ROLE.MEMBER, lang, ids, 10000).execute(event);
                                    break;
                                case "enemyimage":
                                case "enemyimg":
                                case "eimage":
                                case "eimg":
                                    new EnemyImage(TimedConstraintCommand.ROLE.MEMBER, lang, ids, 10000).execute(event);
                                    break;
                                case "background":
                                case "bg":
                                    new Background(TimedConstraintCommand.ROLE.MEMBER, lang, ids, 10000).execute(event);
                                    break;
                                case "test":
                                    new Test(ConstraintCommand.ROLE.MANDARIN, lang, ids).execute(event);
                                    break;
                                case "formgif":
                                case "fgif":
                                case "fg":
                                    new FormGif(ConstraintCommand.ROLE.MEMBER, lang, ids, "gif").execute(event);
                                    break;
                                case "enemygif":
                                case "egif":
                                case "eg":
                                    new EnemyGif(ConstraintCommand.ROLE.MEMBER, lang, ids, "gif").execute(event);
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
                     "490941233963728896", "563745009912774687",
                    "632835571655507968", "490940081738350592",
                    "490940151501946880", "787391428916543488",
                    "508042127352266755", "632836623931015185"
            ));

            StaticStore.holder.put("679858366389944409", new IDHolder(
                    "679871555794108416", "679870747694596121",
                    "679869691656667157", "743808872376041553",
                    "679870744561188919", "800632019418742824",
                    "679862700284575744", "689333420794707984"
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
