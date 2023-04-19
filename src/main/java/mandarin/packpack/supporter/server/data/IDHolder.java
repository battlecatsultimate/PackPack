package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.*;

public class IDHolder {
    public static IDHolder jsonToIDHolder(JsonObject obj) {
        IDHolder id = new IDHolder();

        if(obj.has("server")) {
            id.serverPrefix = id.setOr(obj.get("server").getAsString());
        }

        if (obj.has("publish")) {
            id.publish = obj.get("publish").getAsBoolean();
        }

        if(obj.has("mod")) {
            id.MOD = id.setOrNull(obj.get("mod").getAsString());
        }

        if(obj.has("mem")) {
            id.MEMBER = id.setOrNull(obj.get("mem").getAsString());
        }

        if(obj.has("acc")) {
            id.GET_ACCESS = id.setOrNull(obj.get("acc").getAsString());
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
        }

        if(obj.has("logDM")) {
            id.logDM = id.setOrNull(obj.get("logDM").getAsString());
        }

        if(obj.has("config")) {
            id.config = ConfigHolder.parseJson(obj.getAsJsonObject("config"));
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

        if(id.config.lang < 0)
            id.config.lang = 0;

        return id;
    }

    public String serverPrefix = "p!";
    public boolean publish = false;
    public String logDM = null;

    public String MOD;
    public String MEMBER;
    public String BOOSTER;

    public String GET_ACCESS;
    public String ANNOUNCE;

    public Map<String, String> ID = new TreeMap<>();
    public Map<String, List<String>> channel = new TreeMap<>();
    public Map<Integer, String> eventMap = new TreeMap<>();
    public List<String> status = new ArrayList<>();
    public List<Integer> eventLocale = new ArrayList<>();
    public boolean eventRaw = false, forceCompact = false;
    public ConfigHolder config = new ConfigHolder();
    public List<String> banned = new ArrayList<>();
    public Map<String, List<String>> channelException = new HashMap<>();

    public String announceMessage = "";
    public Map<String, String> eventMessage = new HashMap<>();

    public IDHolder(String m, String me, String bo, String acc) {
        this.MOD = m;
        this.MEMBER = me;
        this.GET_ACCESS = acc;
        this.BOOSTER = bo;

        config.lang = LangID.EN;
    }

    public IDHolder() {
        config.lang = LangID.EN;
    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("server", getOrNull(serverPrefix));
        obj.addProperty("publish", publish);
        obj.addProperty("mod", getOrNull(MOD));
        obj.addProperty("mem", getOrNull(MEMBER));
        obj.addProperty("acc", getOrNull(GET_ACCESS));
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
        obj.addProperty("announceMessage", announceMessage);
        obj.add("eventMessage", StaticStore.mapToJsonString(eventMessage));

        return obj;
    }

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

    private String setOr(String id) {
        return id.equals("null") ? "p!" : id;
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
    public String toString() {
        return "IDHolder{" +
                "serverPrefix='" + serverPrefix + '\'' +
                ", publish=" + publish +
                ", MOD='" + MOD + '\'' +
                ", MEMBER='" + MEMBER + '\'' +
                ", BOOSTER='" + BOOSTER + '\'' +
                ", GET_ACCESS='" + GET_ACCESS + '\'' +
                ", ANNOUNCE='" + ANNOUNCE + '\'' +
                ", ID=" + ID +
                ", channel=" + channel +
                '}';
    }
}