package mandarin.packpack.supporter.server.data;

import com.google.gson.*;
import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * IDHolder is a class which contains server's preference data. Each IDHolder represents each server
 */
public class IDHolder implements Cloneable {
    public static IDHolder jsonToIDHolder(JsonObject obj) {
        IDHolder id = new IDHolder();

        if (obj.has("publish")) {
            id.publish = obj.get("publish").getAsBoolean();
        }

        if(obj.has("mod")) {
            JsonElement element = obj.get("mod");
            String moderator;

            if (element instanceof JsonNull) {
                moderator = null;
            } else {
                moderator = element.getAsString();

                if (moderator.equals("null")) {
                    moderator = null;
                }
            }

            id.moderator = moderator;
        }

        if(obj.has("mem")) {
            JsonElement element = obj.get("mem");
            String member;

            if (element instanceof JsonNull) {
                member = null;
            } else {
                member = element.getAsString();

                if (member.equals("null")) {
                    member = null;
                }
            }

            id.member = member;
        }

        if(obj.has("ann")) {
            JsonElement element = obj.get("ann");
            String announceChannel;

            if (element instanceof JsonNull) {
                announceChannel = null;
            } else {
                announceChannel = element.getAsString();

                if (announceChannel.equals("null")) {
                    announceChannel = null;
                }
            }

            id.announceChannel = announceChannel;
        }

        if(obj.has("status")) {
            JsonElement elem = obj.get("status");

            if(!elem.isJsonPrimitive()) {
                id.status = StaticStore.jsonToListString(elem.getAsJsonArray());
            }
        }

        if(obj.has("bo")) {
            JsonElement element = obj.get("bo");
            String booster;

            if (element instanceof JsonNull) {
                booster = null;
            } else {
                booster = element.getAsString();

                if (booster.equals("null")) {
                    booster = null;
                }
            }

            id.booster = booster;
        }

        if(obj.has("channel")) {
            id.channel = id.toMap(obj.getAsJsonObject("channel"));
        }

        if(obj.has("id")) {
            id.ID = id.toIDMap(obj.getAsJsonObject("id"));

            while(id.ID.size() > SelectMenu.OPTIONS_MAX_AMOUNT) {
                String[] keys = id.ID.keySet().toArray(new String[0]);

                id.ID.remove(keys[keys.length - 1]);
            }
        }

        if(obj.has("logDM")) {
            JsonElement element = obj.get("logDM");
            String logDM;

            if (element instanceof JsonNull) {
                logDM = null;
            } else {
                logDM = element.getAsString();

                if (logDM.equals("null")) {
                    logDM = null;
                }
            }

            id.logDM = logDM;
        }

        if(obj.has("config")) {
            id.config = ConfigHolder.parseJson(obj.getAsJsonObject("config"));
        }

        if(obj.has("banned")) {
            id.banned = id.jsonObjectToListString(obj.getAsJsonArray("banned"));
        }

        if(obj.has("channelException")) {
            id.channelException = id.toMap(obj.getAsJsonObject("channelException"));
        }

        if(obj.has("forceCompact")) {
            id.forceCompact = obj.get("forceCompact").getAsBoolean();
        }

        if(obj.has("forceFullTreasure")) {
            id.forceFullTreasure = obj.get("forceFullTreasure").getAsBoolean();
        }

        if(obj.has("announceMessage")) {
            JsonElement e = obj.get("announceMessage");

            if (e instanceof JsonNull) {
                id.announceMessage = "";
            } else {
                id.announceMessage = e.getAsString();
            }
        }

        // Handling old save structure
        if(obj.has("eventMap")) {
            Map<CommonStatic.Lang.Locale, String> eventMap = id.jsonObjectToMapLocaleString(obj.get("eventMap"));
            Map<CommonStatic.Lang.Locale, String> eventMessage;

            if (obj.has("eventMessage")) {
                eventMessage = id.jsonObjectToMapLocaleString(obj.getAsJsonArray("eventMessage").getAsJsonArray());
            } else {
                eventMessage = new HashMap<>();
            }

            for (Map.Entry<CommonStatic.Lang.Locale, String> entry : eventMap.entrySet()) {
                String value = entry.getValue();

                if (value == null || !StaticStore.isNumeric(value)) {
                    continue;
                }

                EventDataConfigHolder data = new EventDataConfigHolder();

                data.channelID = StaticStore.safeParseLong(value);
                data.eventMessage = eventMessage.getOrDefault(entry.getKey(), "");

                id.eventData.put(entry.getKey(), data);
            }
        } else if (obj.has("eventData")) {
            for (JsonElement e : obj.getAsJsonArray("eventData")) {
                if (!(e instanceof JsonObject o)) {
                    continue;
                }

                if (!o.has("key") && !o.has("val")) {
                    continue;
                }

                CommonStatic.Lang.Locale loc = CommonStatic.Lang.Locale.valueOf(o.get("key").getAsString());
                EventDataConfigHolder data = EventDataConfigHolder.fromJson(o.getAsJsonObject("val"));

                id.eventData.put(loc, data);
            }
        }

        if(obj.has("boosterPin")) {
            id.boosterPin = obj.get("boosterPin").getAsBoolean();
        }

        if (obj.has("boosterAll")) {
            id.boosterAll = obj.get("boosterAll").getAsBoolean();
        }

        if(obj.has("boosterPinChannel")) {
            id.boosterPinChannel = id.jsonObjectToListString(obj.getAsJsonArray("boosterPinChannel"));
        }

        if(id.config.lang == null)
            id.config.lang = CommonStatic.Lang.Locale.EN;

        return id;
    }

    /**
     * Moderator role ID, this must not be null value
     */
    @Nullable
    public String moderator;
    /**
     * Member role ID, nullable value. If this value is null, it means member will be
     * everyone
     */
    public String member;
    /**
     * Booster role ID, nullable value
     */
    public String booster;
    /**
     * Channel where bot will post announcements, nullable value
     */
    public String announceChannel;
    /**
     * Channel where bot will post random link posts in bot's DM from other user. This is
     *  for monitoring compromised accounts sending scam link to everyone
     */
    public String logDM = null;

    /**
     * Flag value whether bot will publish the announcement or not. This flag will be ignored
     *  if channel isn't announcement channel
     *
     * @see <a href="https://support.discord.com/hc/en-us/articles/360032008192-Announcement-Channels">Discord Announcement Channel</a>
     */
    public boolean publish = false;
    /**
     * Flag value whether bot will force users to use compact embed mode or not regardless of
     *  user's preferences
     */
    public boolean forceCompact = false;
    /**
     * Flag value whether bot will force users to use full treasure config regardless of user's
     *  preferences
     */
    public boolean forceFullTreasure = false;
    /**
     * Flag value whether bot will allow boosters to pin/unpin message from pinning command
     *
     * @see mandarin.packpack.commands.BoosterPin
     */
    public boolean boosterPin = false;
    /**
     * Flag value whether bot will allow boosters to pin/unpin message in all channels. If
     * {@link IDHolder#boosterPin} is disabled, this flag will be ignored
     */
    public boolean boosterAll = false;

    /**
     * {@link ConfigHolder} of this server. It will be used if user doesn't have personal
     *  preferences
     */
    public ConfigHolder config = new ConfigHolder();

    /**
     * List of channel ID where bot will post its status whenever it goes online or offline
     */
    public List<String> status = new ArrayList<>();
    /**
     * List of user ID who were banned from using any bot's commands in this server
     */
    public List<String> banned = new ArrayList<>();
    /**
     * List of channels that moderators allowed server boosters to pin/unpin messages
     */
    public List<String> boosterPinChannel = new ArrayList<>();

    /**
     * Custom assigned role data. Key is name of assigned role, and value is role ID
     */
    public Map<String, String> ID = new TreeMap<>();
    /**
     * Event data posting config. Key stands for locale, and value stands for Event data config for
     * each BC version
     */
    public Map<CommonStatic.Lang.Locale, EventDataConfigHolder> eventData = new HashMap<>();
    /**
     * Channel permission data. Key is assigned role ID, and value is list of allowed channels.
     * If channel list is null, it means this role allows users to use commands in all channels
     */
    public Map<String, List<String>> channel = new TreeMap<>();
    /**
     * Deactivated channel permission for each user. Key is user ID, and value is list of
     * deactivated channel permission for each role
     */
    public Map<String, List<String>> channelException = new HashMap<>();

    /**
     * Additional message that will be sent together whenever bot posts announcements
     */
    @Nonnull
    public String announceMessage = "";

    /**
     * IDHolder constructor if moderator, member, booster role is already known
     *
     * @param m Moderator role ID
     * @param me Member role ID
     * @param bo Booster roel ID
     */
    public IDHolder(@Nonnull String m, String me, String bo) {
        this.moderator = m;
        this.member = me;
        this.booster = bo;

        config.lang = CommonStatic.Lang.Locale.EN;
    }

    /**
     * Default IDHolder constructor for any server
     */
    public IDHolder(Guild guild) {
        switch (guild.getLocale()) {
            case CHINESE_TAIWAN, CHINESE_CHINA -> config.lang = CommonStatic.Lang.Locale.ZH;
            case KOREAN -> config.lang = CommonStatic.Lang.Locale.KR;
            case JAPANESE -> config.lang = CommonStatic.Lang.Locale.JP;
            case RUSSIAN ->  config.lang = CommonStatic.Lang.Locale.RU;
            case FRENCH -> config.lang = CommonStatic.Lang.Locale.FR;
            case SPANISH, SPANISH_LATAM -> config.lang = CommonStatic.Lang.Locale.ES;
            case ITALIAN -> config.lang = CommonStatic.Lang.Locale.IT;
            case GERMAN -> config.lang = CommonStatic.Lang.Locale.DE;
            case THAI -> config.lang = CommonStatic.Lang.Locale.TH;
            default -> config.lang = CommonStatic.Lang.Locale.EN;
        }
    }

    /**
     * Empty constructor used for reading files
     */
    private IDHolder() {

    }

    /**
     * Inject {@code holder} data into this IDHolder
     *
     * @param holder Other IDHolder that will be used for injecting data
     */
    public void inject(IDHolder holder) {
        publish = holder.publish;
        moderator = holder.moderator;
        member = holder.member;
        announceChannel = holder.announceChannel;
        status = holder.status;
        booster = holder.booster;
        channel = holder.channel;
        ID = holder.ID;
        logDM = holder.logDM;
        eventData = holder.eventData;
        config.inject(config);
        banned = holder.banned;
        channelException = holder.channelException;
        forceCompact = holder.forceCompact;
        forceFullTreasure = holder.forceFullTreasure;
        announceMessage = holder.announceMessage;
        boosterPin = holder.boosterPin;
        boosterPinChannel = holder.boosterPinChannel;
    }

    /**
     * Convert this IDHolder into json format
     *
     * @return {@link JsonObject} of this IDHolder
     */
    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("publish", publish);
        obj.addProperty("mod", moderator);
        obj.addProperty("mem", member);
        obj.addProperty("ann", announceChannel);
        obj.add("status", StaticStore.listToJsonString(status));
        obj.addProperty("bo", booster);
        obj.add("channel", jsonfyMap(channel));
        obj.add("id", jsonfyIDs());
        obj.addProperty("logDM", logDM);
        obj.add("eventData", mapLocaleEventData(eventData));
        obj.add("config", config.jsonfy());
        obj.add("banned", listStringToJsonObject(banned));
        obj.add("channelException", jsonfyMap(channelException));
        obj.addProperty("forceCompact", forceCompact);
        obj.addProperty("forceFullTreasure", forceFullTreasure);
        obj.addProperty("announceMessage", announceMessage);
        obj.addProperty("boosterPin", boosterPin);
        obj.addProperty("boosterAll", boosterAll);
        obj.add("boosterPinChannel", listStringToJsonObject(boosterPinChannel));

        return obj;
    }

    /**
     * Get allowed channels where this {@code member} can use bot's commands
     *
     * @param member Data of member who is in this {@link net.dv8tion.jda.api.entities.Guild}
     *
     * @return List of channel ID where {@code member} can use the commands
     */
    public ArrayList<String> getAllAllowedChannels(Member member) {
        List<Role> ids = member.getRoles();
        List<String> exceptions = channelException.get(member.getId());

        ArrayList<String> result = new ArrayList<>();

        if(this.member == null) {
            List<String> channels = channel.get("Member");

            if(channels == null)
                return null;

            result.addAll(channels);
        }

        for(Role role : ids) {
            if(isSetAsRole(role.getId()) && (exceptions == null || !exceptions.contains(role.getId()))) {
                List<String> channels = channel.get(role.getId());

                if(channels == null)
                    return null;

                result.addAll(channels);
            }
        }

        return result;
    }

    private boolean hasIDasRole(String id) {
        for(String i : ID.values()) {
            if(id.equals(i))
                return true;
        }

        return false;
    }

    private boolean isSetAsRole(String id) {
        return id.equals(moderator) || id.equals(member) || id.equals(booster) || hasIDasRole(id);
    }

    private JsonElement listStringToJsonObject(List<String> arr) {
        if(arr == null) {
            return JsonNull.INSTANCE;
        }

        JsonArray array = new JsonArray();

        for (String s : arr) {
            array.add(s);
        }

        return array;
    }

    private JsonElement mapLocaleEventData(Map<CommonStatic.Lang.Locale, EventDataConfigHolder> map) {
        JsonArray array = new JsonArray();

        for(CommonStatic.Lang.Locale key : map.keySet()) {
            if(map.get(key) == null || map.get(key) == null)
                continue;

            JsonObject obj = new JsonObject();

            obj.addProperty("key", key.name());
            obj.add("val", map.get(key).toJson());

            array.add(obj);
        }

        return array;
    }

    private List<String> jsonObjectToListString(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray ele = obj.getAsJsonArray();

            ArrayList<String> arr = new ArrayList<>();

            for(int i = 0; i < ele.size(); i++) {
                arr.add(ele.get(i).getAsString());
            }

            return arr;
        }

        return null;
    }

    private Map<CommonStatic.Lang.Locale, String> jsonObjectToMapLocaleString(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray();

            Map<CommonStatic.Lang.Locale, String> result = new TreeMap<>();

            for(int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();

                if(o.has("key") && o.has("val")) {
                    JsonElement key = o.get("key");

                    if (key instanceof JsonPrimitive primitive && primitive.isNumber()) {
                        result.put(getLocale(primitive.getAsInt()), o.get("val").getAsString());
                    } else {
                        String code = key.getAsString();

                        if (code.equals("tw"))
                            code = "zh";

                        result.put(CommonStatic.Lang.Locale.valueOf(code.toUpperCase(Locale.US)), o.get("val").getAsString());
                    }
                }
            }

            return result;
        }

        return new TreeMap<>();
    }

    private JsonObject jsonfyMap(Map<String, List<String>> map) {
        JsonObject obj = new JsonObject();

        Set<String> keys = map.keySet();

        int i = 0;

        for(String key : keys) {
            List<String> arr = map.get(key);

            if(arr == null)
                continue;

            JsonObject container = new JsonObject();

            container.addProperty("key", key);
            container.add("val" , listStringToJsonObject(arr));

            obj.add(Integer.toString(i), container);

            i++;
        }

        return obj;
    }

    private JsonObject jsonfyIDs() {
        JsonObject obj = new JsonObject();

        Set<String> keys = ID.keySet();

        int i = 0;

        for(String key : keys) {
            String id = ID.get(key);

            if(id == null)
                continue;

            JsonObject container = new JsonObject();

            container.addProperty("key", key);
            container.addProperty("val", id);

            obj.add(Integer.toString(i), container);

            i++;
        }

        return obj;
    }

    private TreeMap<String, List<String>> toMap(JsonObject obj) {
        TreeMap<String, List<String>> map = new TreeMap<>();

        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject container = obj.getAsJsonObject(Integer.toString(i));

                String key = container.get("key").getAsString();
                List<String> arr = jsonObjectToListString(container.get("val"));

                map.put(key, arr);

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    private TreeMap<String, String> toIDMap(JsonObject obj) {
        TreeMap<String, String> map = new TreeMap<>();

        int i = 0;

        while(true) {
            if (obj.has(Integer.toString(i))) {
                JsonObject container = obj.getAsJsonObject(Integer.toString(i));

                String key = container.get("key").getAsString();
                String val = container.get("val").getAsString();

                map.put(key, val);

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    @Override
    public IDHolder clone() {
        try {
            IDHolder id = (IDHolder) super.clone();

            id.moderator = moderator;
            id.member = member;
            id.booster = booster;

            id.announceChannel = announceChannel;
            id.logDM = logDM;

            id.publish = publish;
            id.forceCompact = forceCompact;
            id.forceFullTreasure = forceFullTreasure;
            id.boosterPin = boosterPin;

            id.config = config.clone();

            id.status = new ArrayList<>(status);
            id.banned = new ArrayList<>(banned);
            id.boosterPinChannel = new ArrayList<>(boosterPinChannel);

            id.ID = new HashMap<>(ID);
            id.eventData = new HashMap<>();

            for(String key : channel.keySet()) {
                id.channel.put(key, new ArrayList<>(channel.get(key)));
            }

            for(String key : channelException.keySet()) {
                id.channelException.put(key, new ArrayList<>(channelException.get(key)));
            }

            for (CommonStatic.Lang.Locale key : eventData.keySet()) {
                EventDataConfigHolder data = eventData.get(key);

                if (data == null)
                    continue;

                id.eventData.put(key, data.clone());
            }

            return id;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private CommonStatic.Lang.Locale getLocale(int ordinal) {
        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.Locale.values()) {
            if (locale.ordinal() == ordinal)
                return locale;
        }

        return CommonStatic.Lang.Locale.EN;
    }
}