package mandarin.packpack.supporter.server.data;

import com.google.gson.*;
import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.components.selections.SelectMenu;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * IDHolder is a class which contains server's preference data. Each IDHolder represents each server
 */
public class IDHolder implements Cloneable {
    public static final long MEMBER_INDICATOR = -2L;

    public static IDHolder jsonToIDHolder(JsonObject obj) {
        IDHolder id = new IDHolder();

        if (obj.has("publish")) {
            id.publish = obj.get("publish").getAsBoolean();
        }

        if(obj.has("mod")) {
            JsonElement element = obj.get("mod");

            long moderator;

            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isNumber())
                    moderator = primitive.getAsLong();
                else {
                    String channelID = primitive.getAsString();

                    if (channelID.equals("null"))
                        moderator = -1L;
                    else
                        moderator = StaticStore.safeParseLong(primitive.getAsString());
                }
            } else {
                moderator = -1L;
            }

            id.moderator = moderator;
        }

        if(obj.has("mem")) {
            JsonElement element = obj.get("mem");

            long member;

            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isNumber())
                    member = primitive.getAsLong();
                else {
                    String channelID = primitive.getAsString();

                    if (channelID.equals("null"))
                        member = -1L;
                    else
                        member = StaticStore.safeParseLong(primitive.getAsString());
                }
            } else {
                member = -1L;
            }

            id.member = member;
        }

        if(obj.has("ann")) {
            JsonElement element = obj.get("ann");

            long announceChannel;

            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isNumber())
                    announceChannel = primitive.getAsLong();
                else {
                    String channelID = primitive.getAsString();

                    if (channelID.equals("null"))
                        announceChannel = -1L;
                    else
                        announceChannel = StaticStore.safeParseLong(primitive.getAsString());
                }
            } else {
                announceChannel = -1L;
            }

            id.announceChannel = announceChannel;
        }

        if(obj.has("status")) {
            JsonElement elem = obj.get("status");

            if(!elem.isJsonPrimitive()) {
                id.status = StaticStore.jsonToListLong(elem.getAsJsonArray());
            }
        }

        if(obj.has("bo")) {
            JsonElement element = obj.get("bo");

            long booster;

            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isNumber())
                    booster = primitive.getAsLong();
                else
                    booster = StaticStore.safeParseLong(primitive.getAsString());
            } else {
                booster = -1L;
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
            long logDM;

            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isNumber())
                    logDM = primitive.getAsLong();
                else
                    logDM = StaticStore.safeParseLong(primitive.getAsString());
            } else {
                logDM = -1L;
            }

            id.logDM = logDM;
        }

        if(obj.has("config")) {
            id.config = ConfigHolder.fromJson(obj.getAsJsonObject("config"));
        }

        if(obj.has("banned")) {
            id.banned = id.jsonObjectToListLong(obj.getAsJsonArray("banned"));
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
            Map<CommonStatic.Lang.Locale, Long> eventMap = id.jsonObjectToMapLocaleLong(obj.get("eventMap"));
            Map<CommonStatic.Lang.Locale, String> eventMessage;

            if (obj.has("eventMessage")) {
                eventMessage = id.jsonObjectToMapLocaleString(obj.getAsJsonArray("eventMessage").getAsJsonArray());
            } else {
                eventMessage = new HashMap<>();
            }

            for (Map.Entry<CommonStatic.Lang.Locale, Long> entry : eventMap.entrySet()) {
                Long value = entry.getValue();

                if (value == null) {
                    continue;
                }

                EventDataConfigHolder data = new EventDataConfigHolder();

                data.channelID = value;
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
            id.boosterPinChannel = id.jsonObjectToListLong(obj.getAsJsonArray("boosterPinChannel"));
        }

        if (obj.has("bannedPrefix")) {
            id.bannedPrefix = id.jsonObjectToListString(obj.getAsJsonArray("bannedPrefix"));

            id.bannedPrefix.removeIf(prefix -> prefix.toLowerCase(Locale.ENGLISH).equals(StaticStore.globalPrefix));
        }

        if (obj.has("disableCustomPrefix")) {
            id.disableCustomPrefix = obj.get("disableCustomPrefix").getAsBoolean();
        }

        if(id.config.lang == null)
            id.config.lang = CommonStatic.Lang.Locale.EN;

        return id;
    }

    /**
     * Moderator role ID
     */
    public long moderator;
    /**
     * Member role ID, nullable value. If this value is null, it means member will be
     * everyone
     */
    public long member;
    /**
     * Booster role ID, nullable value
     */
    public long booster;
    /**
     * Channel where bot will post announcements, nullable value
     */
    public long announceChannel;
    /**
     * Channel where bot will post random link posts in bot's DM from other user. This is
     *  for monitoring compromised accounts sending scam link to everyone
     */
    public long logDM = -1L;

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
    public List<Long> status = new ArrayList<>();
    /**
     * List of user ID who were banned from using any bot's commands in this server
     */
    public List<Long> banned = new ArrayList<>();
    /**
     * List of channels that moderators allowed server boosters to pin/unpin messages
     */
    public List<Long> boosterPinChannel = new ArrayList<>();

    /**
     * Custom assigned role data. Key is name of assigned role, and value is role ID
     */
    public Map<String, Long> ID = new TreeMap<>();
    /**
     * Event data posting config. Key stands for locale, and value stands for Event data config for
     * each BC version
     */
    public Map<CommonStatic.Lang.Locale, EventDataConfigHolder> eventData = new HashMap<>();
    /**
     * Channel permission data. Key is assigned role ID, and value is list of allowed channels.
     * If channel list is null, it means this role allows users to use commands in all channels
     */
    public Map<Long, List<Long>> channel = new TreeMap<>();
    /**
     * Deactivated channel permission for each user. Key is user ID, and value is list of
     * deactivated channel permission for each role
     */
    public Map<Long, List<Long>> channelException = new HashMap<>();

    /**
     * Prefix that will be ignored in this server
     */
    public List<String> bannedPrefix = new ArrayList<>();
    /**
     * Prevent users from using their custom prefix as whole
     */
    public boolean disableCustomPrefix = false;

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
     * @param bo Booster role ID
     */
    public IDHolder(long m, long me, long bo) {
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
        bannedPrefix = holder.bannedPrefix;
        disableCustomPrefix = holder.disableCustomPrefix;
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
        obj.add("status", StaticStore.listToJsonLong(status));
        obj.addProperty("bo", booster);
        obj.add("channel", jsonfyMap(channel));
        obj.add("id", jsonfyIDs());
        obj.addProperty("logDM", logDM);
        obj.add("eventData", mapLocaleEventData(eventData));
        obj.add("config", config.toJson());
        obj.add("banned", listLongToJsonObject(banned));
        obj.add("channelException", jsonfyMap(channelException));
        obj.addProperty("forceCompact", forceCompact);
        obj.addProperty("forceFullTreasure", forceFullTreasure);
        obj.addProperty("announceMessage", announceMessage);
        obj.addProperty("boosterPin", boosterPin);
        obj.addProperty("boosterAll", boosterAll);
        obj.add("boosterPinChannel", listLongToJsonObject(boosterPinChannel));
        obj.add("bannedPrefix", StaticStore.listToJsonString(bannedPrefix));
        obj.addProperty("disableCustomPrefix", disableCustomPrefix);

        return obj;
    }

    /**
     * Get allowed channels where this {@code member} can use bot's commands
     *
     * @param member Data of member who is in this {@link net.dv8tion.jda.api.entities.Guild}
     *
     * @return List of channel ID where {@code member} can use the commands
     */
    public ArrayList<Long> getAllAllowedChannels(Member member) {
        List<Role> ids = member.getRoles();
        List<Long> exceptions = channelException.get(member.getIdLong());

        ArrayList<Long> result = new ArrayList<>();

        if(this.member == -1L) {
            List<Long> channels = channel.get(MEMBER_INDICATOR);

            if(channels == null)
                return null;

            result.addAll(channels);
        }

        for(Role role : ids) {
            if(isSetAsRole(role.getIdLong()) && (exceptions == null || !exceptions.contains(role.getIdLong()))) {
                List<Long> channels = channel.get(role.getIdLong());

                if(channels == null)
                    return null;

                result.addAll(channels);
            }
        }

        return result;
    }

    private boolean hasIDasRole(long id) {
        for(long i : ID.values()) {
            if(id == i)
                return true;
        }

        return false;
    }

    private boolean isSetAsRole(long id) {
        return id == moderator || id == member || id == booster || hasIDasRole(id);
    }

    private JsonElement listLongToJsonObject(List<Long> arr) {
        if(arr == null) {
            return JsonNull.INSTANCE;
        }

        JsonArray array = new JsonArray();

        for (long s : arr) {
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

    private List<Long> jsonObjectToListLong(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray ele = obj.getAsJsonArray();

            ArrayList<Long> arr = new ArrayList<>();

            for(int i = 0; i < ele.size(); i++) {
                JsonElement e = ele.get(i);

                if (!(e instanceof JsonPrimitive p))
                    continue;

                if (p.isString()) {
                    arr.add(StaticStore.safeParseLong(p.getAsString()));
                } else {
                    arr.add(p.getAsLong());
                }
            }

            return arr;
        }

        return new ArrayList<>();
    }

    private List<String> jsonObjectToListString(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray ele = obj.getAsJsonArray();

            ArrayList<String> arr = new ArrayList<>();

            for(int i = 0; i < ele.size(); i++) {
                JsonElement e = ele.get(i);

                if (!(e instanceof JsonPrimitive p) || !p.isString())
                    continue;

                arr.add(p.getAsString());
            }

            return arr;
        }

        return new ArrayList<>();
    }

    private Map<CommonStatic.Lang.Locale, Long> jsonObjectToMapLocaleLong(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray();

            Map<CommonStatic.Lang.Locale, Long> result = new TreeMap<>();

            for(int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();

                if(o.has("key") && o.has("val")) {
                    JsonElement key = o.get("key");
                    JsonElement val = o.get("val");

                    if (!(key instanceof JsonPrimitive primitiveKey))
                        continue;

                    if (!(val instanceof JsonPrimitive primitiveValue))
                        continue;

                    CommonStatic.Lang.Locale locale;

                    if (primitiveKey.isNumber())
                        locale = getLocale(primitiveKey.getAsInt());
                    else {
                        String code = key.getAsString();

                        if (code.equals("tw"))
                            code = "zh";

                        locale = CommonStatic.Lang.Locale.valueOf(code.toUpperCase(Locale.ENGLISH));
                    }

                    long id;

                    if (primitiveValue.isNumber())
                        id = primitiveValue.getAsLong();
                    else
                        id = StaticStore.safeParseLong(primitiveValue.getAsString());

                    result.put(locale, id);
                }
            }

            return result;
        }

        return new TreeMap<>();
    }

    private Map<CommonStatic.Lang.Locale, String> jsonObjectToMapLocaleString(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray();

            Map<CommonStatic.Lang.Locale, String> result = new TreeMap<>();

            for(int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();

                if(o.has("key") && o.has("val")) {
                    JsonElement key = o.get("key");
                    JsonElement val = o.get("val");

                    if (!(key instanceof JsonPrimitive primitiveKey))
                        continue;

                    if (!(val instanceof JsonPrimitive primitiveValue))
                        continue;

                    CommonStatic.Lang.Locale locale;

                    if (primitiveKey.isNumber())
                        locale = getLocale(primitiveKey.getAsInt());
                    else {
                        String code = key.getAsString();

                        if (code.equals("tw"))
                            code = "zh";

                        locale = CommonStatic.Lang.Locale.valueOf(code.toUpperCase(Locale.ENGLISH));
                    }

                    result.put(locale, primitiveValue.getAsString());
                }
            }

            return result;
        }

        return new TreeMap<>();
    }

    private JsonObject jsonfyMap(Map<Long, List<Long>> map) {
        JsonObject obj = new JsonObject();

        Set<Long> keys = map.keySet();

        int i = 0;

        for(long key : keys) {
            List<Long> arr = map.get(key);

            if(arr == null)
                continue;

            JsonObject container = new JsonObject();

            container.addProperty("key", key);
            container.add("val" , listLongToJsonObject(arr));

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
            Long id = ID.get(key);

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

    private TreeMap<Long, List<Long>> toMap(JsonObject obj) {
        TreeMap<Long, List<Long>> map = new TreeMap<>();

        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject container = obj.getAsJsonObject(Integer.toString(i));

                JsonElement keyElement = container.get("key");

                if (!(keyElement instanceof JsonPrimitive primitiveKey))
                    continue;

                long key;

                if (primitiveKey.isNumber())
                    key = primitiveKey.getAsLong();
                else
                    key = StaticStore.safeParseLong(primitiveKey.getAsString());

                List<Long> arr = jsonObjectToListLong(container.get("val"));

                map.put(key, arr);

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    private TreeMap<String, Long> toIDMap(JsonObject obj) {
        TreeMap<String, Long> map = new TreeMap<>();

        int i = 0;

        while(true) {
            if (obj.has(Integer.toString(i))) {
                JsonObject container = obj.getAsJsonObject(Integer.toString(i));

                JsonElement elementValue = container.get("val");

                if (!(elementValue instanceof  JsonPrimitive primitiveValue))
                    continue;

                String key = container.get("key").getAsString();

                long val;

                if (primitiveValue.isNumber())
                    val = primitiveValue.getAsLong();
                else
                    val = StaticStore.safeParseLong(primitiveValue.getAsString());

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

            id.bannedPrefix = new ArrayList<>(bannedPrefix);
            id.disableCustomPrefix = disableCustomPrefix;

            for(long key : channel.keySet()) {
                id.channel.put(key, new ArrayList<>(channel.get(key)));
            }

            for(long key : channelException.keySet()) {
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