package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.ScamLinkHandler;

import java.util.HashMap;
import java.util.Map;

public class ScamLinkHandlerHolder {
    public final Map<Long, ScamLinkHandler> servers = new HashMap<>();

    public JsonArray jsonfy() {
        JsonArray arr = new JsonArray();

        for(long key : servers.keySet()) {
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

            JsonElement keyElement = obj.get("key");

            if (!(keyElement instanceof JsonPrimitive keyPrimitive))
                continue;

            long key;

            if (keyPrimitive.isString()) {
                key = StaticStore.safeParseLong(keyPrimitive.getAsString());
            } else {
                key = keyPrimitive.getAsLong();
            }

            JsonObject h = obj.get("val").getAsJsonObject();

            long author;

            JsonElement authorElement = h.get("author");

            if (authorElement instanceof JsonPrimitive authorPrimitive) {
                if (authorPrimitive.isString()) {
                    author = StaticStore.safeParseLong(authorPrimitive.getAsString());
                } else {
                    author = authorPrimitive.getAsLong();
                }
            } else {
                author = -1L;
            }

            long server;

            JsonElement serverElement = h.get("server");

            if (serverElement instanceof JsonPrimitive serverPrimitive) {
                if (serverPrimitive.isString()) {
                    server = StaticStore.safeParseLong(serverPrimitive.getAsString());
                } else {
                    server = serverPrimitive.getAsLong();
                }
            } else {
                server = -1L;
            }

            long channel;

            JsonElement channelElement = h.get("channel");

            if (channelElement instanceof JsonPrimitive channelPrimitive) {
                if (channelPrimitive.isString()) {
                    channel = StaticStore.safeParseLong(channelPrimitive.getAsString());
                } else {
                    channel = channelPrimitive.getAsLong();
                }
            } else {
                channel = -1L;
            }

            boolean noticeAll = h.get("noticeAll").getAsBoolean();
            String mute;

            if(h.has("scamDetector.action.mute")) {
                mute = h.get("scamDetector.action.mute").getAsString();
            } else {
                mute = null;
            }

            ScamLinkHandler.ACTION action = switch (h.get("action").getAsString()) {
                case "scamDetector.action.kick" -> ScamLinkHandler.ACTION.KICK;
                case "scamDetector.action.ban" -> ScamLinkHandler.ACTION.BAN;
                default -> ScamLinkHandler.ACTION.MUTE;
            };

            ScamLinkHandler handler = new ScamLinkHandler(author, server, channel, mute, action, noticeAll);

            servers.put(key, handler);
        }
    }
}
