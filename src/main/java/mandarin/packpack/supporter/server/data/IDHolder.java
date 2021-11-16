package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import discord4j.common.util.Snowflake;
import mandarin.packpack.supporter.lang.LangID;

import java.util.*;

public class IDHolder {
    public static IDHolder jsonToIDHolder(JsonObject obj) {
        IDHolder id = new IDHolder();

        if(obj.has("server")) {
            id.serverPrefix = id.setOr(obj.get("server").getAsString());
        }

        if(obj.has("locale")) {
            id.serverLocale = obj.get("locale").getAsInt();
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

        if(obj.has("pre")) {
            String content = obj.get("pre").getAsString();

            if(!content.equals("null")) {
                id.ID.put("Pre Member", content);
            }
        }

        if(obj.has("pc")) {
            String content = obj.get("pc").getAsString();

            if(!content.equals("null")) {
                id.ID.put("BCU PC User", content);
            }
        }

        if(obj.has("and")) {
            String content = obj.get("and").getAsString();

            if(!content.equals("null")) {
                id.ID.put("BCU Android User", content);
            }
        }

        if(obj.has("mut")) {
            String content = obj.get("mut").getAsString();

            if(!content.equals("null")) {
                id.ID.put("Muted", content);
            }
        }

        if(obj.has("acc")) {
            id.GET_ACCESS = id.setOrNull(obj.get("acc").getAsString());
        }

        if(obj.has("ann")) {
            id.ANNOUNCE = id.setOrNull(obj.get("ann").getAsString());
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

        return id;
    }

    public String serverPrefix = "p!";
    public int serverLocale = LangID.EN;
    public boolean publish = false;

    public String MOD;
    public String MEMBER;
    public String BOOSTER;

    public String GET_ACCESS;
    public String ANNOUNCE;


    public Map<String, String> ID = new TreeMap<>();
    public Map<String, ArrayList<String>> channel = new TreeMap<>();

    public IDHolder(String m, String me, String bo, String acc) {
        this.MOD = m;
        this.MEMBER = me;
        this.GET_ACCESS = acc;
        this.BOOSTER = bo;
    }

    public IDHolder() {

    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("server", getOrNull(serverPrefix));
        obj.addProperty("locale", serverLocale);
        obj.addProperty("publish", publish);
        obj.addProperty("mod", getOrNull(MOD));
        obj.addProperty("mem", getOrNull(MEMBER));
        obj.addProperty("acc", getOrNull(GET_ACCESS));
        obj.addProperty("ann", getOrNull(ANNOUNCE));
        obj.addProperty("bo", getOrNull(BOOSTER));
        obj.add("channel", jsonfyMap());
        obj.add("id", jsonfyIDs());

        return obj;
    }

    public ArrayList<String> getAllAllowedChannels(Set<Snowflake> ids) {
        ArrayList<String> result = new ArrayList<>();

        for(Snowflake id : ids) {
            if(isSetAsRole(id.asString())) {
                ArrayList<String> channels = channel.get(id.asString());

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

    private JsonElement arrayListToJsonObject(ArrayList<String> arr) {
        if(arr == null) {
            return JsonNull.INSTANCE;
        }

        JsonArray array = new JsonArray();

        for (String s : arr) {
            array.add(s);
        }

        return array;
    }

    private ArrayList<String> jsonObjectToArrayList(JsonElement obj) {
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

    private JsonObject jsonfyMap() {
        JsonObject obj = new JsonObject();

        Set<String> keys = channel.keySet();

        int i = 0;

        for(String key : keys) {
            ArrayList<String> arr = channel.get(key);

            if(arr == null)
                continue;

            JsonObject container = new JsonObject();

            container.addProperty("key", key);
            container.add("val" , arrayListToJsonObject(arr));

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

    private TreeMap<String, ArrayList<String>> toMap(JsonObject obj) {
        TreeMap<String, ArrayList<String>> map = new TreeMap<>();

        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject container = obj.getAsJsonObject(Integer.toString(i));

                String key = container.get("key").getAsString();
                ArrayList<String> arr = jsonObjectToArrayList(container.get("val"));

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
                ", serverLocale=" + serverLocale +
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