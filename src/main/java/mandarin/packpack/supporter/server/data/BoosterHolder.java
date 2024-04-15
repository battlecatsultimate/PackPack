package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class BoosterHolder {
    public static BoosterHolder parseJson(JsonArray arr) {
        BoosterHolder holder = new BoosterHolder();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if(set.has("key") && set.has("val")) {
                String key = set.get("key").getAsString();
                BoosterData val = BoosterData.parseJson(set.get("val").getAsJsonObject());

                if(val == null)
                    continue;

                if(val.getRole() == null && val.getEmoji() == null)
                    continue;

                holder.serverBooster.put(key, val);
            }
        }

        return holder;
    }

    public final Map<String, BoosterData> serverBooster = new HashMap<>();

    public JsonArray jsonfy() {
        JsonArray arr = new JsonArray();

        for(String key : serverBooster.keySet()) {
            BoosterData data = serverBooster.get(key);

            if(data == null)
                continue;

            if(data.getRole() == null && data.getEmoji() == null)
                continue;

            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.add("val", data.jsonfy());

            arr.add(obj);
        }

        return arr;
    }
}
