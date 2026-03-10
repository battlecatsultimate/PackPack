package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;

import java.util.ArrayList;
import java.util.List;

public class ScamLinkHolder {
    public List<String> links = new ArrayList<>();
    public List<Long> servers = new ArrayList<>();

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.add("links", StaticStore.listToJsonString(links));
        obj.add("servers", StaticStore.listToJsonLong(servers));

        return obj;
    }

    public void readData(JsonObject obj) {
        if(obj.has("links")) {
            links = StaticStore.jsonToListString(obj.getAsJsonArray("links"));
        }

        if(obj.has("servers")) {
            servers = StaticStore.jsonToListLong(obj.getAsJsonArray("servers"));
        }
    }
}
