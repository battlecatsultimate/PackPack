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
import mandarin.packpack.supporter.server.slash.SlashBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    public static void main(String[] args) throws LoginException {
        initialize(args);

        final String TOKEN = args[0];

        JDABuilder builder = JDABuilder.createDefault(TOKEN);

        builder.setEnabledIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS);
        builder.disableCache(CacheFlag.VOICE_STATE);
        builder.setActivity(Activity.playing(develop ? dev : normal));
        builder.addEventListeners(new AllEventAdapter());

        JDA client = builder.build();

        StaticStore.logger = new Logger(client);

        EmoteStore.initialize(client);

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
                            client.getSelfUser().getManager().setAvatar(Icon.from(f)).queue();
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
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));

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

                        for(Role r : roles) {
                            if(r.getId().equals(mod))
                                return;
                        }

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

            boolean done = false;

            for(int i = 0; i < r.length; i++) {
                boolean eventDone = false;

                for(int j = 0; j < r[i].length; j++) {
                    if(r[i][j] && holder.eventLocale.contains(i) && holder.event != null) {
                        try {
                            GuildChannel ch = client.getGuildChannelById(holder.event);

                            if(ch instanceof GuildMessageChannel) {
                                if(j == EventFactor.SALE) {
                                    ArrayList<String> result = StaticStore.event.printStageEvent(i, holder.serverLocale, false, holder.eventRaw, false, 0);

                                    if(result.isEmpty())
                                        continue;

                                    boolean wasDone = done;

                                    done = true;

                                    if(!eventDone) {
                                        eventDone = true;
                                        ((MessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID("event_loc"+i, holder.serverLocale)).queue();
                                    }

                                    boolean goWithFile = false;

                                    for(int k = 0; k < result.size(); k++) {
                                        if(result.get(k).length() >= 1950) {
                                            goWithFile = true;
                                            break;
                                        }
                                    }

                                    if(goWithFile) {
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

                                        ((GuildMessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID("printstage_toolong", holder.serverLocale))
                                                .addFile(res, "event.txt")
                                                .queue(m -> {
                                                    if(res.exists() && !res.delete()) {
                                                        StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                                    }
                                                }, e -> {
                                                    StaticStore.logger.uploadErrorLog(e, "E/PackBot::notifyEvent - Failed to perform uploading stage event data");

                                                    if(res.exists() && !res.delete()) {
                                                        StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                                    }
                                                });
                                    } else {
                                        for(int k = 0; k < result.size(); k++) {
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

                                            ((GuildMessageChannel) ch).sendMessage(merge.toString())
                                                    .allowedMentions(new ArrayList<>())
                                                    .queue();
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

                                        ((MessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID("event_loc"+i, holder.serverLocale)).queue();
                                    }

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

                                        String lID;

                                        if(j == EventFactor.GATYA) {
                                            lID = "printgacha_toolong";
                                        } else {
                                            lID = "printitem_toolong";
                                        }

                                        ((GuildMessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + LangID.getStringByID(lID, holder.serverLocale))
                                                .allowedMentions(new ArrayList<>())
                                                .addFile(res, "event.txt")
                                                .queue(m -> {
                                                    if(res.exists() && !res.delete()) {
                                                        StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                                    }
                                                }, e -> {
                                                    StaticStore.logger.uploadErrorLog(e, "E/PackBot::notifyEvent - Failed to perform uploading stage event data");

                                                    if(res.exists() && !res.delete()) {
                                                        StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                                                    }
                                                });
                                    } else {
                                        ((MessageChannel) ch).sendMessage((wasDone ? "** **\n" : "") + result).queue();
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

                GuildChannel ch = client.getGuildChannelById(holder.event);

                if(ch instanceof GuildMessageChannel) {
                    ((GuildMessageChannel) ch).sendMessage(LangID.getStringByID("event_warning", holder.serverLocale)).queue();
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

    private static void reassignTempModRole(Guild g, IDHolder holder, AtomicReference<Boolean> warned) {
        if (g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            g.createRole()
                    .setName("PackPackMod")
                    .queue(r -> holder.MOD = r.getId(), e -> StaticStore.logger.uploadErrorLog(e, "E/PackBot::reassignTempModRole - Error happened while trying to create role"));
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
}
