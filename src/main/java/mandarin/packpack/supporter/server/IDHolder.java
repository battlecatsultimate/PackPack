package mandarin.packpack.supporter.server;

import com.google.gson.JsonObject;

public class IDHolder {
    public static IDHolder jsonToIDHolder(JsonObject obj) {
        IDHolder id = new IDHolder();

        if(obj.has("server")) {
            id.serverPrefix = id.setOr(obj.get("server").getAsString());
        }

        if(obj.has("dev")) {
            id.DEV = id.setOrNull(obj.get("dev").getAsString());
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

        return id;
    }

    public String serverPrefix = "p!";

    public String DEV;
    public String MOD;
    public String MEMBER;
    public String PRE_MEMBER;
    public String BCU_PC_USER;
    public String BCU_ANDROID;
    public String MUTED;

    public String GET_ACCESS;

    public IDHolder(String d, String m, String me, String pre, String pc, String and, String acc, String mu) {
        this.DEV = d;
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
        obj.addProperty("dev", getOrNull(DEV));
        obj.addProperty("mod", getOrNull(MOD));
        obj.addProperty("mem", getOrNull(MEMBER));
        obj.addProperty("pre", getOrNull(PRE_MEMBER));
        obj.addProperty("pc", getOrNull(BCU_PC_USER));
        obj.addProperty("and", getOrNull(BCU_ANDROID));
        obj.addProperty("mut", getOrNull(MUTED));
        obj.addProperty("acc", getOrNull(GET_ACCESS));

        return obj;
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
}
