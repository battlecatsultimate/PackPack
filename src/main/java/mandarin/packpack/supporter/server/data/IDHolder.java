package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

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
            id.MOD = id.setOrNull(obj.get("mod").getAsString());
        }

        if(obj.has("mem")) {
            id.MEMBER = id.setOrNull(obj.get("mem").getAsString());
        }

        if(obj.has("ann")) {
            id.ANNOUNCE = id.setOrNull(obj.get("ann").getAsString());
        }

        if(obj.has("status")) {
            JsonElement elem = obj.get("status");

            if(!elem.isJsonPrimitive()) {
                id.status = StaticStore.jsonToListString(elem.getAsJsonArray());
            }
        }

        if(obj.has("bo")) {
            id.BOOSTER = id.setOrNull(obj.get("bo").getAsString());
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
            id.logDM = id.setOrNull(obj.get("logDM").getAsString());
        }

        if(obj.has("config")) {
            id.config = ConfigHolder.parseJson(obj.getAsJsonObject("config"));
        }

        if(obj.has("server")) {
            JsonElement e = obj.get("server");

            if (!e.isJsonNull()) {
                id.config.prefix = e.getAsString();
            }
        }

        if(obj.has("locale")) {
            id.config.lang = obj.get("locale").getAsInt();
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
            id.announceMessage = obj.get("announceMessage").getAsString();
        }

        if(obj.has("eventMessage")) {
            id.eventMessage = StaticStore.jsonToMapString(obj.getAsJsonArray("eventMessage").getAsJsonArray());
        }

        if(obj.has("event") && obj.has("eventLocale")) {
            List<Integer> locales = id.jsonObjectToListInteger(obj.getAsJsonArray("eventLocale"));
            String channel = id.setOrNull(obj.get("event").getAsString());

            if(locales != null && channel != null && !channel.isBlank()) {
                for(int l : locales) {
                    id.eventMap.put(l, channel);
                }
            }
        }

        if(obj.has("eventMap")) {
            id.eventMap = id.jsonObjectToMapIntegerString(obj.get("eventMap"));
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

        if(id.config.lang < 0)
            id.config.lang = 0;

        return id;
    }

    /**
     * Moderator role ID, this must not be null value
     */
    public String MOD;
    /**
     * Member role ID, nullable value. If this value is null, it means member will be
     * everyone
     */
    public String MEMBER;
    /**
     * Booster role ID, nullable value
     */
    public String BOOSTER;
    /**
     * Channel where bot will post announcements, nullable value
     */
    public String ANNOUNCE;
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
     * Sorting method for posting event data. Bot will post event data with raw order in the
     * file if this is true
     */
    public boolean eventRaw = false;
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
     * Additional message that will be sent whenever bot posts new event data. Key is locale
     * value (BCEN, BCKR, ...), and value is contents of additional message
     */
    public Map<String, String> eventMessage = new HashMap<>();
    /**
     * Event data posting channels. Key is locale value (BCEN, BCKR, ...), and value is channel
     * ID
     */
    public Map<Integer, String> eventMap = new TreeMap<>();
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
    public String announceMessage = "";

    /**
     * IDHolder constructor if moderator, member, booster role is already known
     *
     * @param m Moderator role ID
     * @param me Member role ID
     * @param bo Booster roel ID
     */
    public IDHolder(String m, String me, String bo) {
        this.MOD = m;
        this.MEMBER = me;
        this.BOOSTER = bo;

        config.lang = LangID.EN;
    }

    /**
     * Default IDHolder constructor for any server
     */
    public IDHolder() {
        config.lang = LangID.EN;
    }

    /**
     * Inject {@code holder} data into this IDHolder
     *
     * @param holder Other IDHolder that will be used for injecting data
     */
    public void inject(IDHolder holder) {
        publish = holder.publish;
        MOD = holder.MOD;
        MEMBER = holder.MEMBER;
        ANNOUNCE = holder.ANNOUNCE;
        status = holder.status;
        BOOSTER = holder.BOOSTER;
        channel = holder.channel;
        ID = holder.ID;
        logDM = holder.logDM;
        eventMap = holder.eventMap;
        config.inject(holder.config);
        banned = holder.banned;
        channelException = holder.channelException;
        forceCompact = holder.forceCompact;
        forceFullTreasure = holder.forceFullTreasure;
        announceMessage = holder.announceMessage;
        eventMessage = holder.eventMessage;
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
        obj.addProperty("mod", getOrNull(MOD));
        obj.addProperty("mem", getOrNull(MEMBER));
        obj.addProperty("ann", getOrNull(ANNOUNCE));
        obj.add("status", StaticStore.listToJsonString(status));
        obj.addProperty("bo", getOrNull(BOOSTER));
        obj.add("channel", jsonfyMap(channel));
        obj.add("id", jsonfyIDs());
        obj.addProperty("logDM", getOrNull(logDM));
        obj.add("eventMap", mapIntegerStringToJsonArray(eventMap));
        obj.add("config", config.jsonfy());
        obj.add("banned", listStringToJsonObject(banned));
        obj.add("channelException", jsonfyMap(channelException));
        obj.addProperty("forceCompact", forceCompact);
        obj.addProperty("forceFullTreasure", forceFullTreasure);
        obj.addProperty("announceMessage", announceMessage);
        obj.add("eventMessage", StaticStore.mapToJsonString(eventMessage));
        obj.addProperty("boosterPin", boosterPin);
        obj.addProperty("boosterAll", boosterAll);
        obj.add("boosterPinChannel", listStringToJsonObject(boosterPinChannel));

        return obj;
    }

    /**
     * Get allowed channels where this {@code member} can use bot's commands
     *
     * @param member Member data who is in this {@link net.dv8tion.jda.api.entities.Guild}
     *
     * @return List of channel ID where {@code member} can use the commands
     */
    public ArrayList<String> getAllAllowedChannels(Member member) {
        List<Role> ids = member.getRoles();
        List<String> exceptions = channelException.get(member.getId());

        ArrayList<String> result = new ArrayList<>();

        if(MEMBER == null) {
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
        return id.equals(MOD) || id.equals(MEMBER) || id.equals(BOOSTER) || hasIDasRole(id);
    }

    private String getOrNull(String id) {
        return id == null ? "null" : id;
    }

    private String setOrNull(String id) {
        return id.equals("null") ? null : id;
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

    private JsonElement mapIntegerStringToJsonArray(Map<Integer, String> map) {
        JsonArray array = new JsonArray();

        for(int key : map.keySet()) {
            if(map.get(key) == null || map.get(key).isBlank())
                continue;

            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.addProperty("val", map.get(key));

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

    private List<Integer> jsonObjectToListInteger(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray ele = obj.getAsJsonArray();

            List<Integer> arr = new ArrayList<>();

            for(int i = 0; i < ele.size(); i++) {
                arr.add(ele.get(i).getAsInt());
            }

            return arr;
        }

        return null;
    }

    private Map<Integer, String> jsonObjectToMapIntegerString(JsonElement obj) {
        if(obj.isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray();

            Map<Integer, String> result = new TreeMap<>();

            for(int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();

                if(o.has("key") && o.has("val")) {
                    result.put(o.get("key").getAsInt(), o.get("val").getAsString());
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

            id.MOD = MOD;
            id.MEMBER = MEMBER;
            id.BOOSTER = BOOSTER;

            id.ANNOUNCE = ANNOUNCE;
            id.logDM = logDM;

            id.publish = publish;
            id.eventRaw = eventRaw;
            id.forceCompact = forceCompact;
            id.forceFullTreasure = forceFullTreasure;
            id.boosterPin = boosterPin;

            id.config = config.clone();

            id.status = new ArrayList<>(status);
            id.banned = new ArrayList<>(banned);
            id.boosterPinChannel = new ArrayList<>(boosterPinChannel);

            id.ID = new HashMap<>(ID);
            id.eventMessage = new HashMap<>(eventMessage);
            id.eventMap = new HashMap<>(eventMap);

            for(String key : channel.keySet()) {
                id.channel.put(key, new ArrayList<>(channel.get(key)));
            }

            for(String key : channelException.keySet()) {
                id.channelException.put(key, new ArrayList<>(channelException.get(key)));
            }

            return id;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}