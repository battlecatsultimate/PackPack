package mandarin.packpack.supporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.*;
import common.CommonStatic;
import common.io.WebFileIO;
import common.io.assets.UpdateCheck;
import common.io.json.JsonDecoder;
import common.io.json.JsonEncoder;
import common.pack.Identifier;
import common.util.lang.MultiLangCont;
import common.util.pack.Background;
import mandarin.packpack.PackBot;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import mandarin.packpack.supporter.event.EventHolder;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lwjgl.opengl.renderer.Renderer;
import mandarin.packpack.supporter.server.FixedScheduleHandler;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.TimeBoolean;
import mandarin.packpack.supporter.server.data.*;
import mandarin.packpack.supporter.server.holder.Conflictable;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.HolderHub;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.message.MessageHolder;
import mandarin.packpack.supporter.server.holder.modal.ModalHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

public class StaticStore {
    public static final Logger logger = new Logger();

    public static String ratingChannel = "";

    public static boolean initialized = false;

    public static long executed = 0;
    public static long previousExecuted = 0;

    public static String globalPrefix = "p!";

    public static boolean apkDownloading = false;

    public static boolean safeClose = false;
    public static boolean wasSafeClose = false;
    public static boolean logCommand = false;

    public static final String COMMAND_BG_ID = "bg";
    public static final String COMMAND_COMBO_ID = "combo";
    public static final String COMMAND_ENEMYIMAGE_ID = "eimage";
    public static final String COMMAND_ENEMYSPRITE_ID = "esprite";
    public static final String COMMAND_FINDSTAGE_ID = "fstage";
    public static final String COMMAND_FORMIMAGE_ID = "fimage";
    public static final String COMMAND_FORMSPRITE_ID = "fsprite";
    public static final String COMMAND_STAGEINFO_ID = "stageinfo";
    public static final String COMMAND_SUGGEST_ID = "suggest";
    public static final String COMMAND_FINDREWARD_ID = "freward";
    public static final String COMMNAD_PLOT_ID = "plot";
    public static final String COMMAND_TPLOT_ID = "tplot";
    public static final String COMMAND_SOLVE_ID = "solve";
    public static final String COMMAND_INTEGRATE_ID = "integrate";
    public static final String COMMAND_RTHETA_ID = "rtheta";
    public static final String COMMAND_FORMDPS_ID = "fdps";
    public static final String COMMAND_ENEMYDPS_ID = "edps";

    public static final int MAX_STAGE_IMAGE_LENGTH = 128;

    public static final ConfigHolder defaultConfig = new ConfigHolder();

    public static BackupHolder backup = null;

    public static Map<String, String> langs = new HashMap<>();
    public static Map<String, String> musics = new HashMap<>();
    public static final Map<String, Integer> timeZones = new HashMap<>();
    public static Map<String, ConfigHolder> config = new HashMap<>();
    public static Map<String, TreasureHolder> treasure = new HashMap<>();

    public static final Map<CommonStatic.Lang.Locale, String> announcements = new HashMap<>();

    public static Map<String, String> suggestBanned = new HashMap<>();

    public static ArrayList<String> contributors = new ArrayList<>();
    public static ArrayList<String> maintainers = new ArrayList<>();

    public static ImgurDataHolder imgur = new ImgurDataHolder(null);

    public static final Map<String, TimeBoolean> canDo = new HashMap<>();

    public static Map<String, SpamPrevent> spamData = new HashMap<>();

    public static Map<String, BoosterHolder> boosterData = new HashMap<>();

    public static ArrayList<String> needFixing = new ArrayList<>();

    public static final EventHolder event = new EventHolder();

    public static ArrayList<String> optoutMembers = new ArrayList<>();

    public static final ScamLinkHolder scamLink = new ScamLinkHolder();
    public static final ScamLinkHandlerHolder scamLinkHandlers = new ScamLinkHandlerHolder();

    public static final MultiLangCont<Integer, String> MEDNAME = new MultiLangCont<>();
    public static final MultiLangCont<Integer, String> MEDEXP = new MultiLangCont<>();
    public static final MultiLangCont<Integer, String> GACHANAME = new MultiLangCont<>();
    public static final MultiLangCont<Integer, String> EXTRAGACHA = new MultiLangCont<>();
    public static final MultiLangCont<Integer, String> NORMALGACHA = new MultiLangCont<>();
    public static final MultiLangCont<Integer, String> MISSIONNAME = new MultiLangCont<>();

    public static int medalNumber = 0;
    public static JsonElement medalData;
    public static final List<Integer> existingRewards = new ArrayList<>();

    public static Map<Identifier<Background>, Integer> backgroundStageLength = new HashMap<>();

    public static final Map<String, Map<String, Long>> timeLimit = new HashMap<>();

    public static Timer saver = null;
    public static Renderer renderManager = null;
    public static final FixedScheduleHandler executorHandler = new FixedScheduleHandler(5);

    public static final Map<String, HolderHub> holders = new HashMap<>();
    private static final List<String> queuedFileNames = new ArrayList<>();

    public static final String[] langUnicode = {
            "🇺🇸",
            "🇹🇼",
            "🇰🇷",
            "🇯🇵",
            "🇷🇺",
            "🇩🇪",
            "🇫🇷",
            "🇪🇸",
            "🇮🇹",
            "🇹🇭"
    };
    public static final CommonStatic.Lang.Locale[] supportedLanguages = {
            CommonStatic.Lang.Locale.EN,
            CommonStatic.Lang.Locale.ZH,
            CommonStatic.Lang.Locale.JP,
            CommonStatic.Lang.Locale.KR,
            CommonStatic.Lang.Locale.RU,
            CommonStatic.Lang.Locale.DE,
            CommonStatic.Lang.Locale.FR,
            CommonStatic.Lang.Locale.ES,
            CommonStatic.Lang.Locale.IT,
            CommonStatic.Lang.Locale.TH
    };

    public static final int[] rainbow = {rgb(217, 65, 68), rgb(217, 128, 65), rgb(224, 213, 85)
    , rgb(118, 224, 85), rgb(85, 169, 224), rgb(185, 85, 224)};

    public static final int[] grade = {
        rgb(204,124,84),
        rgb(206,209,210),
        rgb(213,171,98),
        rgb(218,232,240)
    };

    public final static String UNITARCHIVE = "1327548891847069728";
    public final static String ENEMYARCHIVE = "1327548910465581057";
    public final static String MISCARCHIVE = "964536641526067310";
    public final static String ASSETARCHIVE = "1386599052291997766";

    public static AssetManager assetManager = new AssetManager();

    /** UNIT-ICON-001-002 **/
    public static final String UNIT_ICON = "UNIT-ICON-%s-%s";
    /** ENEMY-ICON-000 **/
    public static final String ENEMY_ICON = "ENEMY-ICON-%s";
    /** UNIT-EVOLVE-TRUE-050-EN **/
    public static final String UNIT_EVOLVE_TRUE = "UNIT-EVOLVE-TRUE-%s-%s";
    /** UNIT-EVOLVE-ULTRA-050-EN **/
    public static final String UNIT_EVOLVE_ULTRA = "UNIT-EVOLVE-ULTRA-%s-%s";
    /** UNIT-DPS-GRAPH-000-002-JP-ACD85-TREASURE **/
    public static final String DPS_GRAPH_UNIT = "UNIT-DPS-GRAPH-%s-%s-%s-%s-%s";
    /** ENEMY-DPS-GRAPH-000-KR-DE8B5 **/
    public static final String DPS_GRAPH_ENEMY = "ENEMY-DPS-GRAPH-%s-%s-%s";
    /** STAGE-SCHEME-NA-000-000-LV3-ZH-AB32C **/
    public static final String STAGE_SCHEME = "STAGE-SCHEME-%s-%s-%s-LV%d-%s-%s";
    /** STAGE-LINEUP-SR-000-000-E8FC6 **/
    public static final String STAGE_LINEUP = "STAGE-LINEUP-%s-%s-%s-%s";
    /** MEDAL-ICON-018-EN **/
    public static final String MEDAL_ICON = "MEDAL-ICON-%s-%s";
    /** UNIT-COMBO-051-ABD4C **/
    public static final String COMBO_IMAGE = "UNIT-COMBO-%s-%s";

    public final static String SUPPORT_SERVER = "964054872649515048";

    public static final String[] EMOJI_ARCHIVES = {
            "1192008484926005288",
            "1010103237958959194",
            "1010126565545357443",
            "1012995850739388416",
            "1096841217528053882",
            "964054872649515048",
            "1246002716480639086"
    };

    public static final Random random = new Random();

    public static final BigInteger intMax = new BigInteger(Integer.toString(Integer.MAX_VALUE));
    public static final BigInteger intMin = new BigInteger(Integer.toString(Integer.MIN_VALUE));

    public static final BigInteger longMax = new BigInteger(Long.toString(Long.MAX_VALUE));
    public static final BigInteger longMin = new BigInteger(Long.toString(Long.MIN_VALUE));

    public static final BigDecimal doubleMax = new BigDecimal(Double.toString(Double.MAX_VALUE));
    public static final BigDecimal doubleMin = new BigDecimal(Double.toString(Double.MIN_VALUE));

    public static final String MANDARIN_SMELL = "460409259021172781";

    public static final String UDP_LINK = "https://api.github.com/repos/ThanksFeanor/UDP-Data/contents";

    public static final String PAYPAL = "https://www.paypal.com/paypalme/GidGalG2";
    public static final String CASHAPP = "https://cash.app/$Gideon9787";

    public static final String downPack = "./pack/download";
    public static final String tempPack = "./pack/download";

    public static final String ERROR_MSG = "`INTERNAL_ERROR`";

    public static Map<String, IDHolder> idHolder = new HashMap<>();

    public static String loggingChannel = "";

    public static final List<Integer> availableUDP = new ArrayList<>();

    public static List<String> cultist = new ArrayList<>();

    public static final Map<String, String> conflictedAnimation = new HashMap<>();

    public static BannerHolder bannerHolder = new BannerHolder();

    static {
        if (CommonStatic.Lang.Locale.values().length != langUnicode.length) {
            throw new IllegalStateException("E/StaticStore - Number of supported language and unicode size are desycned!");
        }
    }

    public static String rolesToString(List<Role> roles) {
        StringBuilder builder = new StringBuilder();

        for(Role role : roles) {
            builder.append(role.getId()).append(", ");
        }

        return builder.toString();
    }

    public static File getDownPackFile() {
        int index = 0;

        while(true) {
            File f = new File(downPack+(index == 0 ? "" : Integer.toString(index))+".pack.bcuzip");

            if(f.exists())
                index++;
            else
                return f;
        }
    }

    public static File getTempPackFile() {
        int index = 0;

        while(true) {
            File f = new File(tempPack+(index == 0 ? "" : Integer.toString(index))+".tmp");

            if(f.exists())
                index++;
            else
                return f;
        }
    }

    public static String getPrefix(String id) {
        ConfigHolder holder = config.get(id);

        String pre = holder == null ? null : holder.prefix;

        return Objects.requireNonNullElse(pre, globalPrefix);
    }

    public static String getCommand(String message, String prefix) {
        if(message.isBlank() || prefix.isBlank())
            return "";

        String[] list = message.split(" ");

        if(list[0].toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH)))
            return list[0].toLowerCase(Locale.ENGLISH).replaceFirst(Pattern.quote(prefix.toLowerCase(Locale.ENGLISH)), "");
        else
            return "";
    }

    public static JsonArray mapToJsonIDHolder(Map<String, IDHolder> map) {
        JsonArray arr = new JsonArray();

        for(String key : map.keySet()) {
            IDHolder value = map.get(key);

            if(value == null) {
                System.out.println("Warning! : Key "+key+" returns null!");
                continue;
            }

            JsonObject id = new JsonObject();

            id.add("val", value.jsonfy());
            id.addProperty("key", key);

            arr.add(id);
        }

        return arr;
    }

    public static JsonArray mapToJsonTreasureHolder(Map<String, TreasureHolder> map) {
        JsonArray arr = new JsonArray();

        for(String key : map.keySet()) {
            TreasureHolder value = map.get(key);

            if(value == null)
                continue;

            JsonObject set = new JsonObject();

            set.addProperty("key", key);
            set.add("val", value.toJson());

            arr.add(set);
        }

        return arr;
    }

    public static JsonArray mapToJsonString(Map<String, String> map) {
        JsonArray arr = new JsonArray();

        for(String key : map.keySet()) {
            String value = map.get(key);

            if(value == null) {
                System.out.println("Warning! : Key "+key+" returns null!");
                continue;
            }

            JsonObject set = new JsonObject();

            set.addProperty("key", key);
            set.addProperty("val", value);

            arr.add(set);
        }

        return arr;
    }

    public static JsonArray mapToJsonBoosterHolder(Map<String, BoosterHolder> map) {
        JsonArray arr = new JsonArray();

        for(String key : map.keySet()) {
            BoosterHolder holder = map.get(key);

            if(holder == null)
                continue;

            JsonObject set = new JsonObject();

            set.addProperty("key", key);
            set.add("val", holder.jsonfy());

            arr.add(set);
        }

        return arr;
    }

    public static JsonArray mapToJsonConfigHolder(Map<String, ConfigHolder> map) {
        JsonArray arr = new JsonArray();

        for(String key : map.keySet()) {
            ConfigHolder holder = map.get(key);

            if(holder == null)
                continue;

            JsonObject set = new JsonObject();

            set.addProperty("key", key);
            set.add("val", holder.jsonfy());

            arr.add(set);
        }

        return arr;
    }

    public static JsonArray listToJsonString(List<String> list) {
        JsonArray arr = new JsonArray();

        for(String str : list) {
            arr.add(str);
        }

        return arr;
    }

    public static JsonArray listToJsonInteger(List<Integer> list) {
        JsonArray arr = new JsonArray();

        for(int i : list) {
            arr.add(i);
        }

        return arr;
    }

    public static JsonArray mapToJsonLocaleList(Map<CommonStatic.Lang.Locale, List<Integer>> map) {
        JsonArray arr = new JsonArray();

        for(CommonStatic.Lang.Locale key : map.keySet()) {
            List<Integer> list = map.get(key);

            if(list == null || list.isEmpty())
                continue;

            JsonObject set = new JsonObject();

            set.addProperty("key", key.name());
            set.add("val", listToJsonInteger(list));

            arr.add(set);
        }

        return arr;
    }

    public static JsonArray mapToJsonLocaleBoolean(Map<CommonStatic.Lang.Locale, Boolean> map) {
        JsonArray arr = new JsonArray();

        for(CommonStatic.Lang.Locale key : map.keySet()) {
            boolean b = map.get(key);

            JsonObject set = new JsonObject();

            set.addProperty("key", key.name());
            set.addProperty("val", b);

            arr.add(set);
        }

        return arr;
    }

    public static JsonArray mapToJsonBackgroundInteger(Map<Identifier<Background>, Integer> map) {
        JsonArray arr = new JsonArray();

        for (Identifier<Background> key : map.keySet()) {
            int length = map.get(key);

            JsonObject set = new JsonObject();

            set.add("key", JsonEncoder.encode(key));
            set.addProperty("val", length);

            arr.add(set);
        }

        return arr;
    }

    public static Map<String, IDHolder> jsonToMapIDHolder(JsonArray arr) {
        Map<String, IDHolder> map = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();

            if(obj.has("val") && obj.has("key")) {
                JsonObject val = obj.getAsJsonObject("val");
                String key = obj.get("key").getAsString();

                IDHolder holder = IDHolder.jsonToIDHolder(val);

                map.put(key, holder);
            }
        }

        return map;
    }

    public static Map<String, String> jsonToMapString(JsonArray arr) {
        Map<String, String> map = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();

            if(obj.has("key") && obj.has("val")) {
                map.put(obj.get("key").getAsString(), obj.get("val").getAsString());
            }
        }

        return map;
    }

    public static Map<String, BoosterHolder> jsonToMapBoosterHolder(JsonArray arr) {
        Map<String, BoosterHolder> map = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if(set.has("key") && set.has("val")) {
                String key = set.get("key").getAsString();
                BoosterHolder val = BoosterHolder.parseJson(set.get("val").getAsJsonArray());

                map.put(key, val);
            }
        }

        return map;
    }

    public static Map<String, ConfigHolder> jsonToMapConfigHolder(JsonArray arr) {
        Map<String, ConfigHolder> map = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if(set.has("key") && set.has("val")) {
                String key = set.get("key").getAsString();
                ConfigHolder val = ConfigHolder.parseJson(set.get("val").getAsJsonObject());

                map.put(key, val);
            }
        }

        return map;
    }

    public static Map<String, TreasureHolder> jsonToMapTreasureHolder(JsonArray arr) {
        Map<String, TreasureHolder> map = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if(set.has("key") && set.has("val")) {
                map.put(set.get("key").getAsString(), TreasureHolder.toData(set.getAsJsonObject("val")));
            }
        }

        return map;
    }

    public static Map<CommonStatic.Lang.Locale, List<Integer>> jsonToMapLocaleList(JsonArray arr) {
        Map<CommonStatic.Lang.Locale, List<Integer>> map = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if(set.has("key") && set.has("val")) {
                JsonElement k = set.get("key");
                List<Integer> val = jsonToListInteger(set.get("val").getAsJsonArray());

                CommonStatic.Lang.Locale locale;

                if (k instanceof JsonPrimitive primitive && primitive.isNumber()) {
                    locale = EventFactor.supportedVersions[primitive.getAsInt()];
                } else {
                    locale = CommonStatic.Lang.Locale.valueOf(k.getAsString());
                }

                map.put(locale, val);
            }
        }

        return map;
    }

    public static ArrayList<String> jsonToListString(JsonArray arr) {
        ArrayList<String> result = new ArrayList<>();

        for(int i = 0; i < arr.size(); i++) {
            result.add(arr.get(i).getAsString());
        }

        return result;
    }

    public static List<Integer> jsonToListInteger(JsonArray arr) {
        ArrayList<Integer> result = new ArrayList<>();

        for(int i = 0; i < arr.size(); i++) {
            result.add(arr.get(i).getAsInt());
        }

        return result;
    }

    public static Map<CommonStatic.Lang.Locale, Boolean> jsonToMapLocaleBoolean(JsonArray arr) {
        Map<CommonStatic.Lang.Locale, Boolean> map = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if(set.has("key") && set.has("val")) {
                JsonElement k = set.get("key");
                boolean val = set.get("val").getAsBoolean();

                CommonStatic.Lang.Locale locale;

                if (k instanceof JsonPrimitive primitive && primitive.isNumber()) {
                    locale = EventFactor.supportedVersions[primitive.getAsInt()];
                } else {
                    locale = CommonStatic.Lang.Locale.valueOf(k.getAsString());
                }

                map.put(locale, val);
            }
        }

        return map;
    }

    @SuppressWarnings({"unchecked", "CastCanBeRemovedNarrowingVariableType"})
    public static Map<Identifier<Background>, Integer> jsonToMapBackgroundInteger(JsonArray arr) {
        Map<Identifier<Background>, Integer> map = new HashMap<>();

        for (int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if (set.has("key") && set.has("val")) {
                Identifier<?> anyID = JsonDecoder.decode(set.getAsJsonObject("key"), Identifier.class);

                if (anyID.cls != Background.class)
                    continue;

                Background bg = Identifier.get((Identifier<Background>) anyID);

                if (bg == null)
                    continue;

                int length = set.get("val").getAsInt();

                map.put(bg.id, length);
            }
        }

        return map;
    }

    public static JsonObject getJsonFile(String name) {
        File f = new File("./data/"+name+".json");

        if(!f.exists())
            return null;

        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            JsonElement obj = JsonParser.parseReader(br);

            br.close();

            return obj == null ? null : obj.isJsonObject() ? obj.getAsJsonObject() : null;
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to read json file", f);

            return null;
        }
    }

    public static void saveServerInfo() {
        JsonObject obj = new JsonObject();

        obj.addProperty("safeClose", safeClose);
        obj.addProperty("rating", ratingChannel);
        obj.addProperty("serverpre", globalPrefix);
        obj.addProperty("executed", executed);
        obj.addProperty("bcenVersionCode", event.bcenVersionCode);
        obj.addProperty("bctwVersionCode", event.bctwVersionCode);
        obj.addProperty("bckrVersionCode", event.bckrVersionCode);
        obj.addProperty("bcjpVersionCode", event.bcjpVersionCode);
        obj.add("lang", mapToJsonString(langs));
        obj.add("music", mapToJsonString(musics));
        obj.add("config", mapToJsonConfigHolder(config));
        obj.add("treasure", mapToJsonTreasureHolder(treasure));
        obj.add("imgur", imgur.getData());
        obj.add("idholder", mapToJsonIDHolder(idHolder));
        obj.add("suggestBanned", mapToJsonString(suggestBanned));
        obj.add("alias", AliasHolder.jsonfy());
        obj.add("contributor", listToJsonString(contributors));
        obj.add("maintainers", listToJsonString(maintainers));
        obj.add("spam", SpamPrevent.jsonfyMap());
        obj.add("booster", mapToJsonBoosterHolder(boosterData));
        obj.addProperty("logging", loggingChannel);
        obj.add("needFixing", listToJsonString(needFixing));
        obj.add("gachaCache", mapToJsonLocaleList(event.gachaCache));
        obj.add("itemCache", mapToJsonLocaleList(event.itemCache));
        obj.add("stageCache", mapToJsonLocaleList(event.stageCache));
        obj.add("scamLink", scamLink.jsonfy());
        obj.add("scamLinkHandlers", scamLinkHandlers.jsonfy());
        obj.add("optoutMembers", listToJsonString(optoutMembers));
        obj.add("cultist", listToJsonString(cultist));
        obj.add("eventNewWay", mapToJsonLocaleBoolean(EventFileGrabber.newWay));
        obj.add("backgroundStageLength", mapToJsonBackgroundInteger(backgroundStageLength));
        obj.add("bannerHolder", bannerHolder.toJson());
        obj.add("assetManager", assetManager.toJson());
        obj.add("backup", backup.toJson());

        if (EventFileGrabber.accountCode != null && EventFileGrabber.password != null && EventFileGrabber.passwordRefreshToken != null) {
            obj.addProperty("accountCode", EventFileGrabber.accountCode);
            obj.addProperty("password", EventFileGrabber.password);
            obj.addProperty("passwordRefreshToken", EventFileGrabber.passwordRefreshToken);
        }

        if (EventFileGrabber.jwtToken != null) {
            obj.addProperty("jwtToken", EventFileGrabber.jwtToken);
            obj.addProperty("tokenCreatedAt", EventFileGrabber.tokenCreatedAt);
        }

        try {
            File folder = new File("./data/");

            if(!folder.exists()) {
                boolean res = folder.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder " + folder.getAbsolutePath());
                    return;
                }
            }

            String fileName = PackBot.test ? "testserverinfo.json" : "serverinfo.json";

            File file = new File(folder.getAbsolutePath(), fileName);

            if(!file.exists()) {
                boolean res = file.createNewFile();

                if(!res) {
                    System.out.println("Can't create file " + file.getAbsolutePath());
                    return;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            String json = obj.toString();
            JsonNode tree = mapper.readTree(json);

            FileWriter writer = new FileWriter(file);

            writer.append(mapper.writeValueAsString(tree));
            writer.close();
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to save server info");
        }
    }

    public static void postReadServerInfo() {
        String fileName = PackBot.test ? "testserverinfo" : "serverinfo";

        JsonObject obj = getJsonFile(fileName);

        if(obj != null) {
            if(obj.has("alias")) {
                AliasHolder.parseJson(obj.getAsJsonObject("alias"));
            }

            if (obj.has("backgroundStageLength")) {
                backgroundStageLength = jsonToMapBackgroundInteger(obj.getAsJsonArray("backgroundStageLength"));
            }
        }
    }

    public static void readServerInfo() {
        String fileName = PackBot.test ? "testserverinfo" : "serverinfo";

        JsonObject obj = getJsonFile(fileName);

        if(obj != null) {
            if(obj.has("safeClose")) {
                safeClose = obj.get("safeClose").getAsBoolean();
            }

            if(obj.has("rating")) {
                ratingChannel = obj.get("rating").getAsString();
            }

            if (obj.has("serverpre")) {
                globalPrefix = obj.get("serverpre").getAsString();
            }

            if(obj.has("executed")) {
                executed = obj.get("executed").getAsLong();
                previousExecuted = executed;
            }

            if(obj.has("lang")) {
                langs = jsonToMapString(obj.get("lang").getAsJsonArray());
            }

            if(obj.has("music")) {
                musics = jsonToMapString(obj.get("music").getAsJsonArray());
            }

            if(obj.has("config")) {
                config = jsonToMapConfigHolder(obj.getAsJsonArray("config"));
            }

            if(obj.has("prefix")) {
                Map<String, String> map = jsonToMapString(obj.get("prefix").getAsJsonArray());

                for (String id : map.keySet()) {
                    String prefix = map.get(id);

                    if (prefix == null)
                        continue;

                    ConfigHolder holder = config.computeIfAbsent(id, k -> new ConfigHolder());

                    holder.prefix = prefix;
                }
            }

            if(obj.has("treasure")) {
                treasure = jsonToMapTreasureHolder(obj.getAsJsonArray("treasure"));
            }

            if(obj.has("imgur")) {
                imgur = new ImgurDataHolder(obj.getAsJsonObject("imgur"));
            }

            if(obj.has("idholder")) {
                idHolder = jsonToMapIDHolder(obj.getAsJsonArray("idholder"));
            }

            if(obj.has("suggestBanned")) {
                suggestBanned = jsonToMapString(obj.getAsJsonArray("suggestBanned"));
            }

            if(obj.has("contributor")) {
                contributors = jsonToListString(obj.getAsJsonArray("contributor"));
            }

            if (obj.has("maintainers")) {
                maintainers = jsonToListString(obj.getAsJsonArray("maintainers"));
            }

            if(obj.has("spam")) {
                spamData = SpamPrevent.parseJsonMap(obj.getAsJsonArray("spam"));
            }

            if(obj.has("booster")) {
                boosterData = jsonToMapBoosterHolder(obj.getAsJsonArray("booster"));
            }

            if(obj.has("logging")) {
                loggingChannel = obj.get("logging").getAsString();
            }

            if(obj.has("needFixing")) {
                needFixing = jsonToListString(obj.getAsJsonArray("needFixing"));
            }

            if(obj.has("bcenVersionCode")) {
                event.bcenVersionCode = obj.get("bcenVersionCode").getAsLong();
            }

            if(obj.has("bctwVersionCode")) {
                event.bctwVersionCode = obj.get("bctwVersionCode").getAsLong();
            }

            if(obj.has("bckrVersionCode")) {
                event.bckrVersionCode = obj.get("bckrVersionCode").getAsLong();
            }

            if(obj.has("bcjpVersionCode")) {
                event.bcjpVersionCode = obj.get("bcjpVersionCode").getAsLong();
            }

            if(obj.has("gachaCache")) {
                event.gachaCache = jsonToMapLocaleList(obj.getAsJsonArray("gachaCache"));
            }

            if(obj.has("itemCache")) {
                event.itemCache = jsonToMapLocaleList(obj.getAsJsonArray("itemCache"));
            }

            if(obj.has("stageCache")) {
                event.stageCache = jsonToMapLocaleList(obj.getAsJsonArray("stageCache"));
            }

            if(obj.has("scamLink")) {
                scamLink.readData(obj.getAsJsonObject("scamLink"));
            }

            if(obj.has("scamLinkHandlers")) {
                scamLinkHandlers.readData(obj.getAsJsonArray("scamLinkHandlers"));
            }

            if(obj.has("optoutMembers")) {
                optoutMembers = jsonToListString(obj.getAsJsonArray("optoutMembers"));
            }

            if(obj.has("cultist")) {
                cultist = jsonToListString(obj.getAsJsonArray("cultist"));
            }

            if(obj.has("eventNewWay")) {
                EventFileGrabber.newWay = jsonToMapLocaleBoolean(obj.getAsJsonArray("eventNewWay"));
            }

            if(obj.has("accountCode")) {
                EventFileGrabber.accountCode = obj.get("accountCode").getAsString();
            }

            if(obj.has("password")) {
                EventFileGrabber.password = obj.get("password").getAsString();
            }

            if(obj.has("passwordRefreshToken")) {
                EventFileGrabber.passwordRefreshToken = obj.get("passwordRefreshToken").getAsString();
            }

            if(obj.has("jwtToken")) {
                EventFileGrabber.jwtToken = obj.get("jwtToken").getAsString();
            }

            if(obj.has("tokenCreatedAt")) {
                EventFileGrabber.tokenCreatedAt = obj.get("tokenCreatedAt").getAsLong();
            }

            if (obj.has("bannerHolder")) {
                BannerHolder.fromJson(bannerHolder, obj.getAsJsonObject("bannerHolder"));
            }

            if (obj.has("assetManager")) {
                assetManager = AssetManager.fromJson(obj.getAsJsonArray("assetManager"));
            }

            if (obj.has("backup")) {
                backup = BackupHolder.fromJson(obj.getAsJsonArray("backup"));
            } else {
                backup = BackupHolder.fromJson(new JsonArray());
            }

            // If any of these are null, bot can't check event data properly, resetting
            if (EventFileGrabber.accountCode == null || EventFileGrabber.password == null || EventFileGrabber.passwordRefreshToken == null) {
                EventFileGrabber.accountCode = null;
                EventFileGrabber.password = null;
                EventFileGrabber.passwordRefreshToken = null;

                EventFileGrabber.jwtToken = null;
            }

            wasSafeClose = safeClose;
            safeClose = false;
        }
    }

    public static void deleteFile(File f, boolean selfDelete) {
        if(f.isFile()) {
            try {
                boolean res = Files.deleteIfExists(f.toPath());

                if(!res) {
                    logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.uploadErrorLog(e, "E/StaticStore::deleteFile : Failed to delete file/folders\nWritable : "+f.canWrite()+"\nReadable : "+f.canRead());
            }
        } else if(f.isDirectory()) {
            File[] files = f.listFiles();

            if(files != null) {
                for(File g : files) {
                    if(g.isFile()) {
                       try {
                           boolean res = Files.deleteIfExists(g.toPath());

                           if(!res) {
                               logger.uploadLog("Failed to delete file : "+g.getAbsolutePath());
                           }
                       } catch (Exception e) {
                           logger.uploadErrorLog(e, "E/StaticStore::deleteFile : Failed to delete file/folders\nWritable : "+g.canWrite()+"\nReadable : "+g.canRead());
                       }
                    } else if(g.isDirectory()) {
                        deleteFile(g, true);
                    }
                }
            }

            if(selfDelete) {
                try {
                    boolean res = Files.deleteIfExists(f.toPath());

                    if(!res) {
                        logger.uploadLog("Failed to delete folder : "+f.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.uploadErrorLog(e, "E/StaticStore::deleteFile : Failed to delete file/folders");
                }
            }
        }
    }

    public static synchronized File generateTempFile(File folder, String name, String extension, boolean isDirectory) {
        int n = 0;

        if(!extension.isBlank() && !extension.startsWith("."))
            extension = "." + extension.strip();

        while(true) {
            String fileName;

            if(n == 0)
                fileName = name+extension;
            else
                fileName = name+"_"+n+extension;

            if(queuedFileNames.contains(fileName)) {
                n++;
                continue;
            }

            queuedFileNames.add(fileName);

            File test = new File(folder, fileName);

            if(!Files.exists(test.toPath())) {
                try {
                    if(isDirectory) {
                        Files.createDirectory(test.toPath());
                    } else {
                        Files.createFile(test.toPath());
                    }

                    return test;
                } catch (Exception e) {
                    logger.uploadErrorLog(e, "Error happened while trying to create new temp file/folder : "+test.getAbsolutePath());

                    return null;
                } finally {
                    queuedFileNames.remove(fileName);
                }
            } else {
                queuedFileNames.remove(fileName);

                n++;
            }
        }
    }

    public static boolean isNumeric(String name) {
        name = name.trim();

        boolean decimalStart = false;
        boolean eNotationStart = false;
        boolean numberContained = false;

        for(int i = 0; i < name.length(); i++) {
            if(!Character.isDigit(name.charAt(i))) {
                if (!decimalStart && !eNotationStart && name.charAt(i) == '.')
                    decimalStart = true;
                else if (!eNotationStart && name.charAt(i) == 'E')
                    eNotationStart = true;
                else if (i != 0 || name.charAt(i) != '-')
                    return false;
            } else {
                numberContained = true;
            }
        }

        return numberContained;
    }

    public static int safeParseInt(String value) {
        if(isNumeric(value)) {
            BigInteger big = new BigDecimal(value.trim()).toBigInteger();

            if(big.compareTo(intMax) > 0) {
                return Integer.MAX_VALUE;
            } else if(big.compareTo(intMin) < 0) {
                return Integer.MIN_VALUE;
            } else {
                return big.intValue();
            }
        } else {
            throw new IllegalStateException("Value isn't numeric! : "+value);
        }
    }

    public static long safeParseLong(String value) {
        if(isNumeric(value)) {
            BigInteger big = new BigDecimal(value.trim()).toBigInteger();

            if(big.compareTo(longMax) > 0) {
                return Long.MAX_VALUE;
            } else if(big.compareTo(longMin) < 0) {
                return Long.MIN_VALUE;
            } else {
                return big.longValue();
            }
        } else {
            throw new IllegalStateException("Value isn't numeric! : "+value);
        }
    }

    public static double safeParseDouble(String value) {
        if(isNumeric(value)) {
            BigDecimal big = new BigDecimal(value.trim());

            if(big.compareTo(doubleMax) > 0) {
                return Double.MAX_VALUE;
            } else if(big.compareTo(doubleMin) < 0) {
                return Double.MIN_VALUE;
            } else {
                return big.doubleValue();
            }
        } else {
            throw new IllegalStateException("Value isn't numeric! : "+value);
        }
    }

    public static String fileToMD5(File f) {
        if(!f.exists() || f.isDirectory())
            return null;

        try {
            FileInputStream fis = new FileInputStream(f);

            byte[] buffer = new byte[65536];

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            int n;

            while((n = fis.read(buffer)) != -1) {
                md5.update(buffer, 0, n);
            }

            fis.close();

            byte[] result = md5.digest();

            BigInteger big = new BigInteger(1, result);

            String str = big.toString(16);

            return String.format("%32s", str).replace(' ', '0');
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.uploadErrorLog(e, "Failed to parse file to md5 : "+f.getAbsolutePath());
        }

        return null;
    }

    public static String safeMultiLangGet(Object any, CommonStatic.Lang.Locale lang) {
        String res = MultiLangCont.get(any, lang);

        if(res != null && lang != CommonStatic.Lang.Locale.JP) {
            res = res.replaceAll("[’|‘]" , "'");
        }

        return res;
    }

    public static UpdateCheck.Downloader getDownloader(Message.Attachment att, File container) {
        if(!container.exists() || container.isFile())
            return null;

        String url = att.getUrl();

        File target = new File(container, att.getFileName());
        File temp = new File(container, att.getFileName()+".tmp");

        return new UpdateCheck.Downloader(target, temp, "", false, url);
    }

    synchronized public static void putHolder(String id, Holder holder) {
        if (holder instanceof Conflictable c) {
            handleConflict(c);
        }

        HolderHub hub = holders.getOrDefault(id, new HolderHub());

        if(holder instanceof MessageHolder messageHolder) {
            if (hub.messageHolder == messageHolder)
                return;

            if(hub.messageHolder != null) {
                hub.messageHolder.expire();
            }

            hub.messageHolder = messageHolder;
        } else if(holder instanceof ComponentHolder componentHolder) {
            if (hub.componentHolder == componentHolder)
                return;

            if(hub.componentHolder != null) {
                hub.componentHolder.expire();
            }

            hub.componentHolder = componentHolder;
        } else if(holder instanceof ModalHolder modalHolder) {
            if (hub.modalHolder == modalHolder)
                return;

            hub.modalHolder = modalHolder;
        }

        if(!holders.containsKey(id)) {
            holders.put(id, hub);
        }
    }

    synchronized public static void removeHolder(String id, Holder holder) {
        if(holders.containsKey(id)) {
            HolderHub hub = holders.get(id);

            if(holder instanceof MessageHolder messageHolder && messageHolder == hub.messageHolder) {
                hub.messageHolder = null;
            } else if(holder instanceof ComponentHolder componentHolder && componentHolder == hub.componentHolder) {
                hub.componentHolder = null;
            } else if(holder instanceof ModalHolder modalHolder && modalHolder == hub.modalHolder) {
                hub.modalHolder = null;
            }
        }
    }

    synchronized public static HolderHub getHolderHub(String id) {
        return holders.get(id);
    }

    synchronized private static void handleConflict(Conflictable holder) {
        for(HolderHub hub : holders.values()) {
            if(holder instanceof MessageHolder m && hub.messageHolder instanceof Conflictable c && c.isConflicted(m)) {
                c.onConflict();
            } else if (holder instanceof ComponentHolder c && hub.componentHolder instanceof Conflictable co && co.isConflicted(c)) {
                co.onConflict();
            } else if (holder instanceof ModalHolder m && hub.modalHolder instanceof Conflictable c && c.isConflicted(m)) {
                c.onConflict();
            }
        }
    }

    public static boolean holderContainsKey(String id) {
        return holders.containsKey(id);
    }

    public static String extractFileName(String rawName) {
        String[] names = rawName.split("\\.");

        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < names.length - 1; i++) {
            builder.append(names[i]);

            if(i < names.length - 2)
                builder.append(".");
        }

        return builder.toString();
    }

    public static String beautifyFileSize(File f) {
        String[] unit = {"B", "KB", "MB"};

        double size = f.length();

        for (String s : unit) {
            if (size < 1024) {
                return DataToString.df.format(size) + s;
            } else {
                size /= 1024.0;
            }
        }

        return DataToString.df.format(size)+unit[2];
    }

    public static int getHighestRolePosition(Member m) {
        List<Role> roles = m.getRoles();

        int pos = -1;

        for(Role r : roles) {
            pos = Math.max(r.getPosition(), pos);
        }

        return pos;
    }

    public static void fetchUDPData() throws Exception {
        JsonElement data = WebFileIO.directRead(UDP_LINK);

        if(!(data instanceof JsonArray arr))
            return;

        availableUDP.clear();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();

            if(obj.has("name")) {
                String name = obj.get("name").getAsString();

                if(name.startsWith("UDPData_") && name.endsWith(".txt")) {
                    String idName = name.replace("UDPData_", "").replace(".txt", "");

                    if(isNumeric(idName)) {
                        availableUDP.add(safeParseInt(idName));
                    }
                }
            }
        }

        System.out.println("Successfully fetched UDP data");
    }

    private static int rgb(int r, int g, int b) {
        return new Color(r, g, b).getRGB();
    }

    public static void updateStatus() {
        if(PackBot.statusMessage != null) {
            try {
                long f = Runtime.getRuntime().freeMemory();
                long t = Runtime.getRuntime().totalMemory();
                long m = Runtime.getRuntime().maxMemory();

                double per = 100.0 * (t - f) / m;

                PackBot.statusMessage.queue(msg -> {
                    if (msg.getJDA().getSelfUser().getId().equals(msg.getAuthor().getId())) {
                        msg.editMessage(LangID.getStringByID("stat.info", CommonStatic.Lang.Locale.EN)
                                .replace("_SSS_", String.valueOf(StaticStore.idHolder.size()))
                                .replace("_CCC_", String.valueOf(StaticStore.executed))
                                .replace("_MMM_", String.valueOf(StaticStore.spamData.size())) + "\n\nNumber of Threads\n\n" +
                                "- In Group : " + Thread.activeCount() + "\n" +
                                "- In All : " + ManagementFactory.getThreadMXBean().getThreadCount() + "\n\n" +
                                "Memory Used : " + (t - f >> 20) + " MB / " + (m >> 20) + " MB, " + (int) per + "%").queue(null, e -> {
                        });
                    }
                });
            } catch (Exception ignored) { }
        }
    }

    public static String cutOffText(String text, int limit) {
        if (text.length() > limit) {
            return text.substring(0, limit - 3) + "...";
        } else {
            return text;
        }
    }

    public static boolean hasAllTag(JsonObject obj, String tag, String... tags) {
        if (!obj.has(tag))
            return false;

        for (int i = 0; i < tags.length; i++) {
            if (!obj.has(tags[i]))
                return false;
        }

        return true;
    }
}
