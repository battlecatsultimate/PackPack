package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mandarin.packpack.supporter.server.ScamLinkHandler;

import java.util.HashMap;
import java.util.Map;

public class ScamLinkHandlerHolder {
    public final Map<String, ScamLinkHandler> servers = new HashMap<>();

    public JsonArray jsonfy() {
        JsonArray arr = new JsonArray();

        for(String key : servers.keySet()) {
            ScamLinkHandler handler = servers.get(key);

            if(handler == null)
                continue;

            JsonObject obj = new JsonObject();

            obj.addProperty("key", key);
            obj.add("val", handler.jsonfy());

            arr.add(obj);
        }

        return arr;
    }

    public void readData(JsonArray arr) {
        for(int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();

            if(!obj.has("key") || !obj.has("val"))
                continue;

            String key = obj.get("key").getAsString();

            JsonObject h = obj.get("val").getAsJsonObject();

            String author = h.get("author").getAsString();
            String server = h.get("server").getAsString();
            String channel = h.get("channel").getAsString();
            boolean noticeAll = h.get("noticeAll").getAsBoolean();
            String mute;

            if(h.has("mute")) {
                mute = h.get("mute").getAsString();
            } else {
                mute = null;
            }

            ScamLinkHandler.ACTION action = switch (h.get("action").getAsString()) {
                case "kick" -> ScamLinkHandler.ACTION.KICK;
                case "ban" -> ScamLinkHandler.ACTION.BAN;
                default -> ScamLinkHandler.ACTION.MUTE;
            };

            ScamLinkHandler handler = new ScamLinkHandler(author, server, channel, mute, action, noticeAll);

            servers.put(key, handler);
        }
    }
}
