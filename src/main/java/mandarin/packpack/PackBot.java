package mandarin.packpack;

import common.CommonStatic;
import mandarin.packpack.supporter.*;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import mandarin.packpack.supporter.event.GachaSet;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
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
    public static int save = 0;
    public static int event = 0;
    public static int pfp = 0;
    public static int udp = 0;
    public static int status = 0;
    public static int log = 0;
    public static int backup = 0;

    public static boolean develop = false;

    public static final String normal = "p!help for command info!";
    public static final String dev = "p!help, being developed, bot may not respond";

    public static RestAction<Message> statusMessage = null;

    public static void main(String[] args) throws LoginException {
        Runtime.getRuntime().addShutdownHook(new Thread(Logger::writeLog));

        initialize(args);

        final String TOKEN = args[0];

        JDABuilder builder = JDABuilder.createDefault(TOKEN);

        builder.setEnabledIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.SCHEDULED_EVENTS);
        builder.disableCache(CacheFlag.VOICE_STATE);
        builder.setActivity(Activity.playing(develop ? dev : normal));
        builder.addEventListeners(new AllEventAdapter());

        JDA client = builder.build();

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

                    client.retrieveUserById(StaticStore.MANDARIN_SMELL).queue(user -> user.openPrivateChannel().queue(pv -> pv.sendMessage("Sending backup")
                            .addFiles(FileUpload.fromData(new File("./data/serverinfo.json")))
                            .queue()));

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
                            client.getSelfUser().getManager().setAvatar(Icon.from(f)).queue();
                        }
                    } catch (IOException exception) {
                        exception.printStackTrace();
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

                    Logger.writeLog();
                } else {
                    log++;
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

            try {
                EventFileGrabber.initialize();
                StaticStore.event.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(!StaticStore.contributors.contains(StaticStore.MANDARIN_SMELL)) {
                StaticStore.contributors.add(StaticStore.MANDARIN_SMELL);
            }

            try {
                StaticStore.fetchUDPData();
            } catch (Exception e) {
                e.printStackTrace();
            }

            StaticStore.initialized = true;
        }
    }

    public static void notifyEvent(JDA client, boolean[][] r) {
        List<Guild> guilds = client.getGuilds();

        boolean sent = false;

        for(Guild g : guilds) {
            String gID = g.getId();

            IDHolder holder = StaticStore.idHolder.get(gID);

            if(holder == null) {
                StaticStore.logger.uploadLog("No ID Holder found for guild ID : "+gID);
                continue;
            }

            boolean[] done = { false, false, false, false };
            boolean[] gachaChange = new boolean[4];
            int[] dataFound = new int[4];

            for(int i = 0; i < r.length; i++) {
                boolean eventDone = false;

                for(int j = 0; j < r[i].length; j++) {
                    if(j == EventFactor.GATYA) {
                        gachaChange[i] = r[i][j];
                    }

                    if(r[i][j] && holder.eventMap.containsKey(i) && holder.eventMap.get(i) != null) {
                        try {
                            GuildChannel ch = client.getGuildChannelById(holder.eventMap.get(i));

                            if(ch instanceof GuildMessageChannel && ((GuildMessageChannel) ch).canTalk()) {
                                if(j == EventFactor.SALE) {
                                    Map<EventFactor.SCHEDULE, List<String>> result = StaticStore.event.printStageEvent(i, holder.config.lang, false, holder.eventRaw, false, 0);

                                    if(result.isEmpty())
                                        continue;

                                    boolean wasDone = done[i];

                                    done[i] = true;

                                    if(!eventDone) {
                                        eventDone = true;

                                        ((MessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID("event_loc"+i, holder.config.lang)).queue();
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

                                                builder.append(LangID.getStringByID("event_stage", holder.config.lang)).append("\n\n");
                                            }

                                            if(!initial) {
                                                initial = true;

                                                builder.append(builder.length() == 0 ? "** **\n" : "");

                                                switch (type) {
                                                    case DAILY ->
                                                            builder.append(LangID.getStringByID("printstage_daily", holder.config.lang)).append("\n\n```ansi\n");
                                                    case WEEKLY ->
                                                            builder.append(LangID.getStringByID("printstage_weekly", holder.config.lang)).append("\n\n```ansi\n");
                                                    case MONTHLY ->
                                                            builder.append(LangID.getStringByID("printstage_monthly", holder.config.lang)).append("\n\n```ansi\n");
                                                    case YEARLY ->
                                                            builder.append(LangID.getStringByID("printstage_yearly", holder.config.lang)).append("\n\n```ansi\n");
                                                    case MISSION ->
                                                            builder.append(LangID.getStringByID("event_mission", holder.config.lang)).append("\n\n```ansi\n");
                                                    default -> builder.append("```ansi\n");
                                                }
                                            } else {
                                                builder.append("```ansi\n");
                                            }

                                            while(builder.length() < 1980 && !data.isEmpty()) {
                                                String line = data.get(0);

                                                if(line.length() > 1950) {
                                                    data.remove(0);

                                                    continue;
                                                }

                                                if(builder.length() + line.length() > 1980)
                                                    break;

                                                builder.append(line).append("\n");

                                                if(type == EventFactor.SCHEDULE.MISSION)
                                                    builder.append("\n");

                                                data.remove(0);
                                                dataFound[i] += 1;
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
                                        result = StaticStore.event.printGachaEvent(i, holder.config.lang, false, holder.eventRaw, false, 0);
                                    else
                                        result = StaticStore.event.printItemEvent(i, holder.config.lang, false, holder.eventRaw, false, 0);

                                    if(result.isEmpty())
                                        continue;

                                    boolean wasDone = done[i];

                                    done[i] = true;

                                    if(!eventDone) {
                                        eventDone = true;

                                        ((MessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID("event_loc"+i, holder.config.lang)).queue();
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
                                                builder.append(LangID.getStringByID("event_gacha", holder.config.lang)).append("\n\n");
                                            } else {
                                                builder.append(LangID.getStringByID("event_item", holder.config.lang)).append("\n\n");
                                            }
                                        }

                                        builder.append("```ansi\n");

                                        while(builder.length() < (j == EventFactor.GATYA ? 1800 : 1950) && !result.isEmpty()) {
                                            String line = result.get(0);

                                            if(line.length() > 1950) {
                                                result.remove(0);

                                                continue;
                                            }

                                            if(builder.length() + line.length() > (j == EventFactor.GATYA ? 1800 : 1950))
                                                break;

                                            builder.append(line).append("\n");

                                            result.remove(0);
                                            dataFound[i] += 1;
                                        }

                                        if(result.isEmpty() && j == EventFactor.GATYA) {
                                            builder.append("\n")
                                                    .append(LangID.getStringByID("printgacha_g", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("printgacha_gua", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("printgacha_s", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("printgacha_step", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("printgacha_l", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("printgacha_lucky", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("printgacha_p", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("printgacha_plat", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("printgacha_n", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("printgacha_neneko", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("printgacha_gr", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("printgacha_gran", holder.config.lang))
                                                    .append(" | ")
                                                    .append(LangID.getStringByID("printgacha_r", holder.config.lang))
                                                    .append(" : ")
                                                    .append(LangID.getStringByID("printgacha_rein", holder.config.lang))
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

            for(int i = 0; i < done.length; i++) {
                if(done[i] && holder.eventMap.containsKey(i)) {
                    GuildChannel ch = client.getGuildChannelById(holder.eventMap.get(i));

                    if(ch instanceof GuildMessageChannel) {
                        if (!sentChannels.contains(holder.eventMap.get(i))) {
                            sent = true;
                            sentChannels.add(holder.eventMap.get(i));

                            ((GuildMessageChannel) ch).sendMessage(LangID.getStringByID("event_warning", holder.config.lang)).queue();
                        }

                        if(!holder.eventMessage.isEmpty()) {
                            Pattern p = Pattern.compile("(<@(&)?\\d+>|@everyone|@here)");

                            if(holder.eventMessage.containsKey(getLocale(i))) {
                                if(!p.matcher(holder.eventMessage.get(getLocale(i))).find() || (gachaChange[i] || dataFound[i] >= 5)) {
                                    ((GuildMessageChannel) ch).sendMessage(holder.eventMessage.get(getLocale(i))).queue();
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
        return switch (loc) {
            case EventFactor.EN -> "en";
            case EventFactor.ZH -> "tw";
            case EventFactor.KR -> "kr";
            default -> "jp";
        };
    }

    private static String getFile(int f) {
        return switch (f) {
            case EventFactor.GATYA -> "gatya";
            case EventFactor.ITEM -> "item";
            default -> "sale";
        };
    }
}
