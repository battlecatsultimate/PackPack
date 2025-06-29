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
import mandarin.packpack.supporter.server.data.BannerHolder;
import mandarin.packpack.supporter.server.data.EventDataConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.managers.AccountManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
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
    public static final int SAVE_TERM = 5;
    public static final int BACKUP_TERM = 360;
    public static final int UDP_FETCH_TERM = 30;
    public static final int PFP_UPDATE_TERM = 60;
    public static final int BANNER_UPDATE_TERM = 1440;
    public static final int EVENT_UPDATE_TERM = 5;
    public static final int LOG_WRITE_TERM = 60;
    public static final int STATUS_UPDATE_TERM = 5;

    public static boolean test = false;

    public static int save = Math.max(0, SAVE_TERM - 1);
    public static int backup = Math.max(0, BACKUP_TERM - 1);
    public static int udp = Math.max(0, UDP_FETCH_TERM - 1);
    public static int pfp = Math.max(0, PFP_UPDATE_TERM - 1);
    public static int banner = Math.max(0, BANNER_UPDATE_TERM - 1);
    public static int event = Math.max(0, EVENT_UPDATE_TERM - 1);
    public static int log = Math.max(0, LOG_WRITE_TERM - 1);
    public static int updateStatus = Math.max(0, STATUS_UPDATE_TERM - 1);

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

        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_EXPRESSIONS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.SCHEDULED_EVENTS);
        builder.disableCache(CacheFlag.VOICE_STATE);
        builder.addEventListeners(new AllEventAdapter());

        ShardManager client = builder.build();

        if (StaticStore.bannerHolder.pickedBanner != null) {
            String status = "p!help for command | Banner by " + StaticStore.bannerHolder.pickedBanner.author();

            client.setPresence(OnlineStatus.ONLINE, Activity.customStatus(status.substring(0, Math.min(status.length(), Activity.MAX_ACTIVITY_STATE_LENGTH))));
        } else {
            client.setPresence(OnlineStatus.ONLINE, Activity.customStatus("p!help for command!"));
        }

        StaticStore.logger.assignClient(client);

        StaticStore.saver = new Timer();
        StaticStore.saver.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();

                if(save % SAVE_TERM == 0) {
                    try {
                        System.out.println("Save Process");
                        StaticStore.saveServerInfo();

                        EventFactor.currentYear = c.get(Calendar.YEAR);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to save file");
                    }

                    save = 1;
                } else {
                    save++;
                }

                if(backup % BACKUP_TERM == 0) {
                    try {
                        System.out.println("Backup save file");

                        if (!test && StaticStore.backup != null) {
                            String link = StaticStore.backup.uploadBackup();

                            if (!link.isBlank()) {
                                client.retrieveUserById(StaticStore.MANDARIN_SMELL).queue(user -> user.openPrivateChannel().queue(pv -> pv.sendMessage("Sending backup : " + link)
                                        .queue()));

                                for (int i = 0; i < StaticStore.maintainers.size(); i++) {
                                    client.retrieveUserById(StaticStore.maintainers.get(i)).queue(user -> user.openPrivateChannel().queue(pv -> pv.sendMessage("Sending backup : " + link)
                                            .queue()));
                                }
                            }

                        }
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to send backup file");
                    }

                    backup = 1;
                } else {
                    backup++;
                }

                if(udp % UDP_FETCH_TERM == 0) {
                    try {
                        System.out.println("Fetch UDP");

                        StaticStore.fetchUDPData();
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to fetch UDP data");
                    }

                    udp = 1;
                } else {
                    udp++;
                }

                StaticStore.updateStatus();

                AccountManager manager;
                boolean managerChanged = false;

                JDA house = client.getShardCache().getElementById(0);

                if (house != null) {
                    manager = house.getSelfUser().getManager();
                } else {
                    manager = null;
                }

                if(pfp % PFP_UPDATE_TERM == 0) {
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

                        if(f.exists() && manager != null) {
                            manager = manager.setAvatar(Icon.from(f));
                            managerChanged = true;
                        }
                    } catch (IOException exception) {
                        StaticStore.logger.uploadErrorLog(exception, "E/PackBot::main - Failed to change profile image");
                    }

                    pfp = 1;
                } else {
                    pfp++;
                }

                if ((banner % BANNER_UPDATE_TERM == 0 || banner >= BANNER_UPDATE_TERM) && System.currentTimeMillis() - StaticStore.bannerHolder.lastUpdated >= TimeUnit.DAYS.toMillis(1)) {
                    BannerHolder.BannerData pickedBanner = StaticStore.bannerHolder.pickBanner();

                    if (pickedBanner != null && manager != null) {
                        try {
                            manager = manager.setBanner(Icon.from(pickedBanner.bannerFile()));
                            managerChanged = true;

                            String status = "p!help for command | Banner by " + pickedBanner.author();

                            client.setPresence(OnlineStatus.ONLINE, Activity.customStatus(status.substring(0, Math.min(status.length(), Activity.MAX_ACTIVITY_STATE_LENGTH))));
                        } catch (IOException e) {
                            StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to change profile image");
                        }
                    }

                    banner = 1;
                } else {
                    banner++;
                }

                if (managerChanged) {
                    manager.queue();
                }

                if(event % EVENT_UPDATE_TERM == 0) {
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

                        boolean[] versionResult = StaticStore.event.checkBCVersion();

                        for(int i = 0; i < versionResult.length; i++) {
                            if (versionResult[i]) {
                                doNotify = true;

                                break;
                            }
                        }

                        if (doNotify) {
                            StaticStore.saveServerInfo();

                            notifyNewVersion(client, versionResult);
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

                if (log == LOG_WRITE_TERM) {
                    log = 0;

                    Logger.writeLog(Logger.BotInstance.PACK_PACK);
                } else {
                    log++;
                }

                if (!test) {
                    if (updateStatus == STATUS_UPDATE_TERM) {
                        try {
                            BotListPlatformHandler.handleUpdatingBotStatus(client);

                            Set<String> guilds = new HashSet<>();

                            for(JDA shard : client.getShards()) {
                                guilds.addAll(shard.getGuilds().stream().map(ISnowflake::getId).toList());
                            }

                            StaticStore.holders.entrySet().removeIf(e -> guilds.contains(e.getKey()));
                        } catch (Exception e) {
                            StaticStore.logger.uploadErrorLog(e, "E/PackBot::main - Failed to update status of bot to bot list sites");
                        }

                        updateStatus = 0;
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

            CommonStatic.getConfig().deadOpa = 0;
            CommonStatic.getConfig().fullOpa = 100;

            BannerHolder.initializeBannerData("banner", "bannerData");

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

            IDHolder holder = StaticStore.idHolder.computeIfAbsent(gID, k -> new IDHolder(g));

            boolean[] done = new boolean[EventFactor.supportedVersions.length];
            boolean[] gachaChange = new boolean[EventFactor.supportedVersions.length];
            int[] dataFound = new int[EventFactor.supportedVersions.length];

            for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
                boolean eventDone = false;
                int index = ArrayUtils.indexOf(EventFactor.supportedVersions, locale);

                if (index == -1)
                    continue;

                if (!holder.eventData.containsKey(locale))
                    continue;

                EventDataConfigHolder config = holder.eventData.get(locale);

                if (config == null || config.channelID == -1L)
                    continue;

                for(int j = 0; j < r[index].length; j++) {
                    if(j == EventFactor.GATYA) {
                        gachaChange[index] = r[index][j];
                    }

                    if (!r[index][j])
                        continue;

                    try {
                        GuildChannel ch = client.getGuildChannelById(config.channelID);

                        if(ch instanceof GuildMessageChannel && ((GuildMessageChannel) ch).canTalk()) {
                            if(j == EventFactor.SALE) {
                                Map<EventFactor.SCHEDULE, List<String>> result = StaticStore.event.printStageEvent(locale, holder.config.lang, false, config.eventRaw, false, 0);

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
                                    result = StaticStore.event.printGachaEvent(locale, holder.config.lang, false, config.eventRaw, false, 0);
                                else
                                    result = StaticStore.event.printItemEvent(locale, holder.config.lang, false, config.eventRaw, false, 0);

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
                                        builder.append("\n").append(EventFactor.getGachaCodeExplanation(holder.config.lang)).append("\n```");
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

            List<Long> sentChannels = new ArrayList<>();

            for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
                int index = ArrayUtils.indexOf(EventFactor.supportedVersions, locale);

                if(done[index] && holder.eventData.containsKey(locale)) {
                    EventDataConfigHolder config = holder.eventData.get(locale);

                    if (config == null || config.channelID == -1L)
                        continue;

                    GuildChannel gc = client.getGuildChannelById(config.channelID);

                    if(!(gc instanceof GuildMessageChannel ch)) {
                        continue;
                    }

                    if (!sentChannels.contains(config.channelID)) {
                        sent = true;

                        sentChannels.add(config.channelID);

                        ch.sendMessage(LangID.getStringByID("event.warning", holder.config.lang)).queue();
                    }

                    if(!config.eventMessage.isEmpty()) {
                        Pattern p = Pattern.compile("(<@(&)?\\d+>|@everyone|@here)");

                        if(!p.matcher(config.eventMessage).find() || (gachaChange[index] || dataFound[index] >= 5)) {
                            ch.sendMessage(config.eventMessage).queue();
                        }
                    }
                }
            }
        }

        if(sent) {
            StaticStore.logger.uploadLogWithPing("<@"+StaticStore.MANDARIN_SMELL+"> I caught new event data and successfully announced analyzed data to servers. Below is the updated list : \n\n"+parseResult(r));
        }
    }

    public static void notifyNewVersion(ShardManager client, boolean[] r) {
        List<Guild> guilds = client.getGuilds();

        for (Guild g : guilds) {
            IDHolder holder = StaticStore.idHolder.computeIfAbsent(g.getId(), k -> new IDHolder(g));

            for (CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
                int index = ArrayUtils.indexOf(EventFactor.supportedVersions, locale);

                if (index == -1)
                    continue;

                if (!r[index])
                    continue;

                EventDataConfigHolder config = holder.eventData.get(locale);

                if (config == null || config.newVersionChannelID == -1L)
                    continue;

                GuildChannel gc = g.getGuildChannelById(config.newVersionChannelID);

                if (!(gc instanceof GuildMessageChannel ch))
                    continue;

                String gameName = switch (locale) {
                    case JP -> LangID.getStringByID("event.gameName.jp", holder.config.lang);
                    case KR -> LangID.getStringByID("event.gameName.kr", holder.config.lang);
                    case ZH -> LangID.getStringByID("event.gameName.tw", holder.config.lang);
                    default -> LangID.getStringByID("event.gameName.en", holder.config.lang);
                };

                ch.sendMessage(LangID.getStringByID("event.newVersion", holder.config.lang).formatted(gameName, beautifyVersionName(StaticStore.event.getVersionCode(locale, false)))).queue();

                if (!config.newVersionMessage.isBlank()) {
                    ch.sendMessage(config.newVersionMessage).queue();
                }
            }
        }
    }

    private static String beautifyVersionName(long version) {
        long main = version / 100000;

        version -= main * 100000;

        long major = version / 1000;

        version -= major * 1000;

        long minor = version / 10;

        long subMinor = version % 10;

        if (subMinor != 0) {
            return main + "." + major + "." + subMinor;
        } else {
            return main + "." + major + "." + minor;
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
