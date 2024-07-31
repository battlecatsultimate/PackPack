package mandarin.packpack;

import common.CommonStatic;
import mandarin.packpack.supporter.*;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import mandarin.packpack.supporter.event.GachaSet;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lwjgl.LwjglContext;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Usage : java -jar JARNAME DISCORD_BOT_TOKEN IMGUR_API_ACCESS_TOKEN
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class PackBot {
    public static boolean test = false;

    public static int save = 0;
    public static int event = 0;
    public static int pfp = 0;
    public static int udp = 0;
    public static int log = 0;
    public static int backup = 0;
    public static int updateStatus = 0;

    public static final String normal = "p!help for command info!";

    public static RestAction<Message> statusMessage = null;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Logger.writeLog(Logger.BotInstance.PACK_PACK)));
        Thread.currentThread().setUncaughtExceptionHandler((t, e) ->
                StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Uncaught exception found : " + t.getName())
        );

        RestActionImpl.setDefaultFailure(e -> StaticStore.logger.uploadErrorLog(e, "E/Unknown - Failed to perform the task"));

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--test") && i < args.length - 1) {
                if (args[i + 1].equals("true")) {
                    test = true;
                } else if(args[i + 1].equals("false")) {
                    test = false;
                }
            }
        }

        initialize(args);

        final String TOKEN = args[0];

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(TOKEN);

        builder.setEnabledIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.SCHEDULED_EVENTS);
        builder.disableCache(CacheFlag.VOICE_STATE);
        builder.setActivity(Activity.playing(normal));
        builder.addEventListeners(new AllEventAdapter());

        ShardManager client = builder.build();

        StaticStore.logger.assignClient(client);

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

                if(backup % 360 == 0) {
                    System.out.println("Backup save file");

                    if (!test) {
                        client.retrieveUserById(StaticStore.MANDARIN_SMELL).queue(user -> user.openPrivateChannel().queue(pv -> pv.sendMessage("Sending backup")
                                .addFiles(FileUpload.fromData(new File("./data/serverinfo.json")))
                                .queue()));

                        client.retrieveUserById(195682910269865984L).queue(user -> user.openPrivateChannel().queue(pv -> pv.sendMessage("Sending backup")
                                .addFiles(FileUpload.fromData(new File("./data/serverinfo.json")))
                                .queue()));
                    }

                    backup = 1;
                } else {
                    backup++;
                }

                if(udp % 30 == 0) {
                    System.out.println("Fetch UDP");

                    try {
                        StaticStore.fetchUDPData();
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to fetch UDP data");
                    }

                    udp = 1;
                } else {
                    udp++;
                }

                StaticStore.updateStatus();

                if(pfp % 60 == 0) {
                    try {
                        String fileName;

                        switch (c.get(Calendar.MONTH) + 1) {
                            case 1 -> fileName = "BotJan.png";
                            case 2 -> fileName = "BotFeb.png";
                            case 3 -> fileName = "BotMar.png";
                            case 4 -> {
                                if (c.get(Calendar.DAY_OF_MONTH) == 1) {
                                    fileName = "BotDoge.png";
                                } else {
                                    fileName = "BotApr.png";
                                }
                            }
                            case 6 -> fileName = "BotJun.png";
                            case 7 -> fileName = "BotJul.png";
                            case 10 -> fileName = "BotOct.png";
                            case 11 -> fileName = "BotNov.png";
                            case 12 -> fileName = "BotDec.png";
                            default -> fileName = "Bot.png";
                        }

                        File f = new File("./data/bot/", fileName);

                        if(f.exists()) {
                            JDA house = client.getShardCache().getElementById(0);

                            if (house != null) {
                                house.getSelfUser().getManager().setAvatar(Icon.from(f)).queue();
                            }
                        }
                    } catch (IOException exception) {
                        StaticStore.logger.uploadErrorLog(exception, "E/PackBot::main - Failed to change profile image");
                    }

                    pfp = 1;
                } else {
                    pfp++;
                }

                if(event % 30 == 0) {
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

                            notifyEvent(client, result);
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

                RecordableThread.handleExpiration();

                if (StaticStore.previousExecuted < StaticStore.executed) {
                    Logger.addLog("Executed commands " + (StaticStore.executed - StaticStore.previousExecuted) + " time(s)");

                    StaticStore.previousExecuted = StaticStore.executed;
                }

                if (log == 60) {
                    log = 0;

                    Logger.writeLog(Logger.BotInstance.PACK_PACK);
                } else {
                    log++;
                }

                if (!test) {
                    if (updateStatus == 5) {
                        updateStatus = 0;

                        BotListPlatformHandler.handleUpdatingBotStatus(client);
                    } else {
                        updateStatus++;
                    }
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));
    }

    public static void initialize(String... arg) {
        if(!StaticStore.initialized) {
            CommonStatic.ctx = new LwjglContext();
            CommonStatic.getConfig().ref = false;

            StaticStore.readServerInfo();

            if(arg.length >= 2) {
                StaticStore.imgur.registerClient(arg[1]);
            }

            LangID.initialize();

            Initializer.checkAssetDownload(true);

            StaticStore.postReadServerInfo();

            DataToString.initialize();

            GachaSet.initialize();

            EventFactor.readMissionReward();

            BotListPlatformHandler.initialize();

            try {
                EventFileGrabber.initialize();
                StaticStore.event.initialize();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to initialize event data handler");
            }

            if(!StaticStore.contributors.contains(StaticStore.MANDARIN_SMELL)) {
                StaticStore.contributors.add(StaticStore.MANDARIN_SMELL);
            }

            try {
                StaticStore.fetchUDPData();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to fetch UDP data");
            }

            StaticStore.initialized = true;
        }
    }

    public static void notifyEvent(ShardManager client, boolean[][] r) {
        List<Guild> guilds = client.getGuilds();

        boolean sent = false;

        for(Guild g : guilds) {
            String gID = g.getId();

            IDHolder holder = StaticStore.idHolder.get(gID);

            if(holder == null) {
                StaticStore.logger.uploadLog("No ID Holder found for guild ID : "+gID);
                continue;
            }

            boolean[] done = new boolean[EventFactor.supportedVersions.length];
            boolean[] gachaChange = new boolean[4];
            int[] dataFound = new int[4];

            for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
                boolean eventDone = false;
                int index = ArrayUtils.indexOf(EventFactor.supportedVersions, locale);

                if (index == -1)
                    continue;

                for(int j = 0; j < r[index].length; j++) {
                    if(j == EventFactor.GATYA) {
                        gachaChange[index] = r[index][j];
                    }

                    if(r[index][j] && holder.eventMap.containsKey(locale) && holder.eventMap.get(locale) != null) {
                        try {
                            GuildChannel ch = client.getGuildChannelById(holder.eventMap.get(locale));

                            if(ch instanceof GuildMessageChannel && ((GuildMessageChannel) ch).canTalk()) {
                                if(j == EventFactor.SALE) {
                                    Map<EventFactor.SCHEDULE, List<String>> result = StaticStore.event.printStageEvent(locale, holder.config.lang, false, holder.eventRaw, false, 0);

                                    if(result.isEmpty())
                                        continue;

                                    boolean wasDone = done[index];

                                    done[index] = true;

                                    if(!eventDone) {
                                        eventDone = true;

                                        ((MessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID("event.title." + locale.code, holder.config.lang)).queue();
                                    }

                                    boolean started = false;

                                    for(EventFactor.SCHEDULE type : result.keySet()) {
                                        List<String> data = result.get(type);

                                        if(data == null || data.isEmpty())
                                            continue;

                                        boolean initial = false;

                                        while(!data.isEmpty()) {
                                            StringBuilder builder = new StringBuilder();

                                            if(!started) {
                                                started = true;

                                                if(wasDone) {
                                                    builder.append("** **\n");
                                                }

                                                builder.append(LangID.getStringByID("event.section.stage", holder.config.lang)).append("\n\n");
                                            }

                                            if(!initial) {
                                                initial = true;

                                                builder.append(builder.isEmpty() ? "** **\n" : "");

                                                switch (type) {
                                                    case DAILY ->
                                                            builder.append(LangID.getStringByID("event.permanentSchedule.daily", holder.config.lang)).append("\n\n```ansi\n");
                                                    case WEEKLY ->
                                                            builder.append(LangID.getStringByID("event.permanentSchedule.weekly", holder.config.lang)).append("\n\n```ansi\n");
                                                    case MONTHLY ->
                                                            builder.append(LangID.getStringByID("event.permanentSchedule.monthly", holder.config.lang)).append("\n\n```ansi\n");
                                                    case YEARLY ->
                                                            builder.append(LangID.getStringByID("event.permanentSchedule.yearly", holder.config.lang)).append("\n\n```ansi\n");
                                                    case MISSION ->
                                                            builder.append(LangID.getStringByID("event.section.mission", holder.config.lang)).append("\n\n```ansi\n");
                                                    default -> builder.append("```ansi\n");
                                                }
                                            } else {
                                                builder.append("```ansi\n");
                                            }

                                            while(builder.length() < 1980 && !data.isEmpty()) {
                                                String line = data.getFirst();

                                                if(line.length() > 1950) {
                                                    data.removeFirst();

                                                    continue;
                                                }

                                                if(builder.length() + line.length() > 1980)
                                                    break;

                                                builder.append(line).append("\n");

                                                if(type == EventFactor.SCHEDULE.MISSION)
                                                    builder.append("\n");

                                                data.removeFirst();
                                                dataFound[index] += 1;
                                            }

                                            builder.append("```");

                                            ((GuildMessageChannel) ch).sendMessage(builder.toString())
                                                    .setAllowedMentions(new ArrayList<>())
                                                    .queue();
                                        }
                                    }
                                } else {
                                    List<String> result;

                                    if(j == EventFactor.GATYA)
                                        result = StaticStore.event.printGachaEvent(locale, holder.config.lang, false, holder.eventRaw, false, 0);
                                    else
                                        result = StaticStore.event.printItemEvent(locale, holder.config.lang, false, holder.eventRaw, false, 0);

                                    if(result.isEmpty())
                                        continue;

                                    boolean wasDone = done[index];

                                    done[index] = true;

                                    if(!eventDone) {
                                        eventDone = true;

                                        ((MessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID("event.title." + locale.code, holder.config.lang)).queue();
                                    }

                                    boolean started = false;

                                    while(!result.isEmpty()) {
                                        StringBuilder builder = new StringBuilder();

                                        if(!started) {
                                            started = true;

                                            if(wasDone) {
                                                builder.append("** **\n");
                                            }

                                            if(j == EventFactor.GATYA) {
                                                builder.append(LangID.getStringByID("event.section.gacha", holder.config.lang)).append("\n\n");
                                            } else {
                                                builder.append(LangID.getStringByID("event.section.item", holder.config.lang)).append("\n\n");
                                            }
                                        }

                                        builder.append("```ansi\n");

                                        while(builder.length() < (j == EventFactor.GATYA ? 1800 : 1950) && !result.isEmpty()) {
                                            String line = result.getFirst();

                                            if(line.length() > 1950) {
                                                result.removeFirst();

                                                continue;
                                            }

                                            if(builder.length() + line.length() > (j == EventFactor.GATYA ? 1800 : 1950))
                                                break;

                                            builder.append(line).append("\n");

                                            result.removeFirst();
                                            dataFound[index] += 1;
                                        }

                                        if(result.isEmpty() && j == EventFactor.GATYA) {
                                            builder.append("\n")
                                                    .append(LangID.getStringByID("event.gachaCode.guaranteed.code", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("event.gachaCode.guaranteed.fullName", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("event.gachaCode.stepUp.code", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("event.gachaCode.stepUp.fullName", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("event.gachaCode.luckyTicket.code", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("event.gachaCode.luckyTicket.fullName", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("event.gachaCode.platinumShard.code", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("event.gachaCode.platinumShard.fullName", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("event.gachaCode.nenekoGang.code", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("event.gachaCode.nenekoGang.fullName", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("event.gachaCode.grandon.code", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("event.gachaCode.grandon.fullName", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("event.gachaCode.reinforcement.code", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("event.gachaCode.reinforcement.fullName", holder.config.lang))
                                                    .append("\n```");
                                        } else {
                                            builder.append("```");
                                        }

                                        ((GuildMessageChannel) ch).sendMessage(builder.toString())
                                                .setAllowedMentions(new ArrayList<>())
                                                .queue();
                                    }
                                }
                            }
                        } catch(Exception ignored) {

                        }
                    }
                }
            }

            List<String> sentChannels = new ArrayList<>();

            for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
                int index = ArrayUtils.indexOf(EventFactor.supportedVersions, locale);

                if(done[index] && holder.eventMap.containsKey(locale)) {
                    GuildChannel ch = client.getGuildChannelById(holder.eventMap.get(locale));

                    if(ch instanceof GuildMessageChannel) {
                        if (!sentChannels.contains(holder.eventMap.get(locale))) {
                            sent = true;
                            sentChannels.add(holder.eventMap.get(locale));

                            ((GuildMessageChannel) ch).sendMessage(LangID.getStringByID("event.warning", holder.config.lang)).queue();
                        }

                        if(!holder.eventMessage.isEmpty()) {
                            Pattern p = Pattern.compile("(<@(&)?\\d+>|@everyone|@here)");

                            if(holder.eventMessage.containsKey(locale)) {
                                if(!p.matcher(holder.eventMessage.get(locale)).find() || (gachaChange[index] || dataFound[index] >= 5)) {
                                    ((GuildMessageChannel) ch).sendMessage(holder.eventMessage.get(locale)).queue();
                                }
                            }
                        }
                    }
                }
            }
        }

        if(sent) {
            StaticStore.logger.uploadLogWithPing("<@"+StaticStore.MANDARIN_SMELL+"> I caught new event data and successfully announced analyzed data to servers. Below is the updated list : \n\n"+parseResult(r));
        }
    }

    private static String parseResult(boolean[][] result) {
        StringBuilder r = new StringBuilder();

        for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
            int index = ArrayUtils.indexOf(EventFactor.supportedVersions, locale);

            if (index == -1)
                continue;

            for(int j = 0; j < result[index].length; j++) {
                if(result[index][j]) {
                    r.append(locale.code).append(" : ").append(getFile(j)).append("\n");
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

    private static String getFile(int f) {
        return switch (f) {
            case EventFactor.GATYA -> "gatya";
            case EventFactor.ITEM -> "item";
            default -> "sale";
        };
    }
}
