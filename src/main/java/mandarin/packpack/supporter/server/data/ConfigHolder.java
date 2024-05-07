package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;

public class ConfigHolder implements Cloneable {
    public static ConfigHolder parseJson(JsonObject obj) {
        ConfigHolder holder = new ConfigHolder();

        if (obj.has("prefix")) {
            holder.prefix = obj.get("prefix").getAsString();
        }

        if(obj.has("lang")) {
            holder.lang = obj.get("lang").getAsInt();
        }

        if(obj.has("defLevel")) {
            holder.defLevel = obj.get("defLevel").getAsInt();
        }

        if(obj.has("useFrame")) {
            holder.useFrame = obj.get("useFrame").getAsBoolean();
        }

        if(obj.has("extra")) {
            holder.extra = obj.get("extra").getAsBoolean();
        }

        if(obj.has("compact")) {
            holder.compact = obj.get("compact").getAsBoolean();
        }

        if(obj.has("trueForm")) {
            holder.trueForm = obj.get("trueForm").getAsBoolean();
        }

        if(obj.has("treasure")) {
            holder.treasure = obj.get("treasure").getAsBoolean();
        }

        return holder;
    }

    public String prefix = StaticStore.globalPrefix;
    public int lang = -1, defLevel = 30;
    public boolean useFrame = true, extra = false, compact = false, trueForm = false, treasure = false;

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("prefix", prefix);
        obj.addProperty("lang", lang);
        obj.addProperty("defLevel", defLevel);
        obj.addProperty("useFrame", useFrame);
        obj.addProperty("extra", extra);
        obj.addProperty("compact", compact);
        obj.addProperty("trueForm", trueForm);
        obj.addProperty("treasure", treasure);

        return obj;
    }

    @Override
    public ConfigHolder clone() {
        ConfigHolder c;

        try {
            c = (ConfigHolder) super.clone();
        } catch (CloneNotSupportedException e) {
            StaticStore.logger.uploadErrorLog(e, "Couldn't clone config data");

            c = new ConfigHolder();
        }

        c.prefix = prefix;
        c.lang = lang;
        c.defLevel = defLevel;
        c.useFrame = useFrame;
        c.extra = extra;
        c.compact = compact;
        c.trueForm = trueForm;
        c.treasure = treasure;

        return c;
    }
}
