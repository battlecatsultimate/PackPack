package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mandarin.packpack.supporter.StaticStore;

import java.util.HashMap;
import java.util.Map;

public class BoosterHolder {
    public static BoosterHolder parseJson(JsonArray arr) {
        BoosterHolder holder = new BoosterHolder();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject set = arr.get(i).getAsJsonObject();

            if(set.has("key") && set.has("val")) {
                JsonElement keyElement = set.get("key");

                if (!(keyElement instanceof JsonPrimitive keyPrimitive))
                    continue;

                long key;

                if (keyPrimitive.isString()) {
                    key = StaticStore.safeParseLong(keyPrimitive.getAsString());
                } else {
                    key = keyPrimitive.getAsLong();
                }

                BoosterData val = BoosterData.parseJson(set.get("val").getAsJsonObject());

                if(val == null)
                    continue;

                if(val.getRole() == -1L && val.getEmoji() == -1L)
                    continue;

                holder.serverBooster.put(key, val);
            }
        }

        return holder;
    }

    public final Map<Long, BoosterData> serverBooster = new HashMap<>();

    public JsonArray jsonfy() {
        JsonArray arr = new JsonArray();

        for(long key : serverBooster.keySet()) {
            BoosterData data = serverBooster.get(key);

            if(data == null)
                continue;

            if(data.getRole() == -1L && data.getEmoji() == -1L)
                continue;

            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.add("val", data.jsonfy());

            arr.add(obj);
        }

        return arr;
    }
}
