package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;

import java.util.ArrayList;

public class ScamLinkHolder {
    public ArrayList<String> links = new ArrayList<>();
    public ArrayList<String> servers = new ArrayList<>();

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.add("links", StaticStore.listToJsonString(links));
        obj.add("servers", StaticStore.listToJsonString(servers));

        return obj;
    }

    public void readData(JsonObject obj) {
        if(obj.has("links")) {
            links = StaticStore.jsonToListString(obj.getAsJsonArray("links"));
        }

        if(obj.has("servers")) {
            servers = StaticStore.jsonToListString(obj.getAsJsonArray("servers"));
        }
    }
}
