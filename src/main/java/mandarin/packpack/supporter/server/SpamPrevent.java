package mandarin.packpack.supporter.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.InteractionData;
import discord4j.discordjson.json.MemberData;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class SpamPrevent {
    public static final long MINIMUM_INTERVAL = 500;
    public static final long PREVENT_TIME = 300000;

    public static Map<String, SpamPrevent> parseJsonMap(JsonArray arr) {
        Map<String, SpamPrevent> result = new HashMap<>();

        for(int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();

            if(obj.has("key") && obj.has("val")) {
                SpamPrevent spam = parseJson(obj.getAsJsonObject("val"));
                String key = obj.get("key").getAsString();

                result.put(key, spam);
            }
        }

        return result;
    }

    public static JsonArray jsonfyMap() {
        JsonArray arr = new JsonArray();

        for(String key : StaticStore.spamData.keySet()) {
            SpamPrevent spam = StaticStore.spamData.get(key);

            if(spam != null) {
                JsonObject obj = new JsonObject();

                obj.addProperty("key", key);
                obj.add("val", spam.jsonfy());

                arr.add(obj);
            }
        }

        return arr;
    }

    public static SpamPrevent parseJson(JsonObject obj) {
        SpamPrevent prevent = new SpamPrevent();

        if(obj.has("count")) {
            prevent.count = obj.get("count").getAsInt();
        }

        if(obj.has("lastTime")) {
            prevent.lastTime = obj.get("lastTime").getAsLong();
        }

        if(obj.has("preventTime")) {
            prevent.preventTime = obj.get("preventTime").getAsLong();
        }

        if(obj.has("scale")) {
            prevent.scale = obj.get("scale").getAsLong();
        }

        return prevent;
    }

    public int count = 0;
    public long lastTime = System.currentTimeMillis();
    long preventTime = 0;
    long scale = 1;

    public boolean isPrevented(MessageChannel ch, int lang, String id) {
        if(id.equals(StaticStore.MANDARIN_SMELL))
            return false;

        long current = System.currentTimeMillis();

        if(preventTime > 0 && current - lastTime > preventTime) {
            preventTime = 0;
            count = 0;

            return false;
        } else if(preventTime > 0)
            return true;

        if(current - lastTime > MINIMUM_INTERVAL) {
            lastTime = current;
        } else {
            count++;

            if(count == 5) {
                preventTime = PREVENT_TIME * scale;

                try {
                    scale *= 4;
                } catch (Exception ignored) {

                }

                ch.createMessage(LangID.getStringByID("command_prevent", lang).replace("_TTT_", beautifyMillis(lang)).replace("_UUU_", id)).subscribe();
                return true;
            }
        }

        return false;
    }

    public Mono<?> isPrevented(InteractionCreateEvent event) {
        InteractionData interaction = event.getInteraction().getData();

        long current = System.currentTimeMillis();

        if(preventTime > 0 && current - lastTime > preventTime) {
            preventTime = 0;
            count = 0;

            return null;
        } else if(preventTime > 0)
            return Mono.empty();

        if(current - lastTime > MINIMUM_INTERVAL) {
            lastTime = current;
        } else {
            count++;

            if(count == 5) {
                preventTime = PREVENT_TIME * scale;

                try {
                    scale *= 2;
                } catch (Exception ignored) {

                }

                int lang = LangID.EN;

                if(!interaction.guildId().isAbsent()) {
                    String gID = interaction.guildId().get();

                    if(gID.equals(StaticStore.BCU_KR_SERVER))
                        lang = LangID.KR;
                }

                if(!interaction.member().isAbsent()) {
                    MemberData m = interaction.member().get();

                    if(StaticStore.locales.containsKey(m.user().id().asString())) {
                        lang =  StaticStore.locales.get(m.user().id().asString());
                    }

                    return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(LangID.getStringByID("command_prevent", lang).replace("_TTT_", beautifyMillis(lang)).replace("_UUU_", m.user().id().asString())));
                }

                return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("You can't use command for "+beautifyMillis(lang)));
            }
        }

        return null;
    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("count", count);
        obj.addProperty("lastTime", lastTime);
        obj.addProperty("preventTime", preventTime);
        obj.addProperty("scale", scale);

        return obj;
    }

    private String beautifyMillis(int lang) {
        long time = preventTime;

        long day = time / (1000 * 60 * 60 * 24);

        time -= day * 1000 * 60 * 60 *24;

        long hour = time / (1000 * 60 * 60);

        time -= hour * 1000 * 60 * 60;

        long min = time / (1000 * 60);

        time -= min * 1000 * 60;

        double sec = time / 1000.0;

        StringBuilder result = new StringBuilder();

        if(day != 0) {
            if(day > 1) {
                result.append(day).append(LangID.getStringByID("days", lang));
            } else {
                result.append(day).append(LangID.getStringByID("day", lang));
            }
        }

        if(hour != 0) {
            if(hour > 1) {
                result.append(hour).append(LangID.getStringByID("hours", lang));
            } else {
                result.append(day).append(LangID.getStringByID("hour", lang));
            }
        }

        if(min != 0) {
            if(min > 1) {
                result.append(min).append(LangID.getStringByID("mins", lang));
            } else {
                result.append(min).append(LangID.getStringByID("min", lang));
            }
        }

        if(sec != 0) {
            if(sec > 1) {
                result.append(DataToString.df.format(sec)).append(LangID.getStringByID("secs", lang));
            } else {
                result.append(DataToString.df.format(sec)).append(LangID.getStringByID("sec", lang));
            }
        }

        return result.toString();
    }
}
