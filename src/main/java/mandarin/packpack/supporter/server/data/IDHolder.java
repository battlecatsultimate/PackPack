package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import discord4j.common.util.Snowflake;
import mandarin.packpack.supporter.lang.LangID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
            id.PRE_MEMBER = id.setOrNull(obj.get("pre").getAsString());
        }

        if(obj.has("pc")) {
            id.BCU_PC_USER = id.setOrNull(obj.get("pc").getAsString());
        }

        if(obj.has("and")) {
            id.BCU_ANDROID = id.setOrNull(obj.get("and").getAsString());
        }

        if(obj.has("mut")) {
            id.MUTED = id.setOrNull(obj.get("mut").getAsString());
        }

        if(obj.has("acc")) {
            id.GET_ACCESS = id.setOrNull(obj.get("acc").getAsString());
        }

        if(obj.has("ann")) {
            id.ANNOUNCE = id.setOrNull(obj.get("ann").getAsString());
        }

        if(obj.has("channel")) {
            id.channel = id.toMap(obj.getAsJsonObject("channel"));
        }

        return id;
    }

    public String serverPrefix = "p!";
    public int serverLocale = LangID.EN;
    public boolean publish = false;

    public String MOD;
    public String MEMBER;
    public String PRE_MEMBER;
    public String BCU_PC_USER;
    public String BCU_ANDROID;
    public String MUTED;

    public String GET_ACCESS;
    public String ANNOUNCE;

    public Map<String, ArrayList<String>> channel = new HashMap<>();

    public IDHolder(String m, String me, String pre, String pc, String and, String acc, String mu) {
        this.MOD = m;
        this.MEMBER = me;
        this.PRE_MEMBER = pre;
        this.BCU_PC_USER = pc;
        this.BCU_ANDROID = and;
        this.MUTED = mu;
        this.GET_ACCESS = acc;
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
        obj.addProperty("pre", getOrNull(PRE_MEMBER));
        obj.addProperty("pc", getOrNull(BCU_PC_USER));
        obj.addProperty("and", getOrNull(BCU_ANDROID));
        obj.addProperty("mut", getOrNull(MUTED));
        obj.addProperty("acc", getOrNull(GET_ACCESS));
        obj.addProperty("ann", getOrNull(ANNOUNCE));
        obj.add("channel", jsonfyMap());

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

    private boolean isSetAsRole(String id) {
        return id.equals(MOD) || id.equals(MEMBER) || id.equals(PRE_MEMBER) || id.equals(BCU_PC_USER) || id.equals(BCU_ANDROID) || id.equals(MUTED);
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

    private HashMap<String, ArrayList<String>> toMap(JsonObject obj) {
        HashMap<String, ArrayList<String>> map = new HashMap<>();

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

    @Override
    public String toString() {
        return "IDHolder{" +
                "serverPrefix='" + serverPrefix + '\'' +
                ", serverLocale=" + serverLocale +
                ", publish=" + publish +
                ", MOD='" + MOD + '\'' +
                ", MEMBER='" + MEMBER + '\'' +
                ", PRE_MEMBER='" + PRE_MEMBER + '\'' +
                ", BCU_PC_USER='" + BCU_PC_USER + '\'' +
                ", BCU_ANDROID='" + BCU_ANDROID + '\'' +
                ", MUTED='" + MUTED + '\'' +
                ", GET_ACCESS='" + GET_ACCESS + '\'' +
                ", ANNOUNCE='" + ANNOUNCE + '\'' +
                ", channel=" + channel +
                '}';
    }
}
