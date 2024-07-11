package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import org.jetbrains.annotations.Nullable;

public class ConfigHolder implements Cloneable {
    public static ConfigHolder parseJson(JsonObject obj) {
        ConfigHolder holder = new ConfigHolder();

        if (obj.has("prefix")) {
            holder.prefix = obj.get("prefix").getAsString();
        }

        if(obj.has("lang")) {
            JsonElement e = obj.get("lang");

            if (e instanceof JsonPrimitive primitive) {
                if (primitive.isNumber()) {
                    holder.lang = holder.findLocale(primitive.getAsInt());
                } else {
                    String lang = obj.get("lang").getAsString();

                    holder.lang = lang.isBlank() ? null : CommonStatic.Lang.Locale.valueOf(lang);
                }
            }
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
    @Nullable
    public CommonStatic.Lang.Locale lang = null;
    public int defLevel = 30;
    public boolean useFrame = true, extra = false, compact = false, trueForm = false, treasure = false;

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("prefix", prefix);
        obj.addProperty("lang", lang == null ? "" : lang.name());
        obj.addProperty("defLevel", defLevel);
        obj.addProperty("useFrame", useFrame);
        obj.addProperty("extra", extra);
        obj.addProperty("compact", compact);
        obj.addProperty("trueForm", trueForm);
        obj.addProperty("treasure", treasure);

        return obj;
    }

    public void inject(ConfigHolder clone) {
        prefix = clone.prefix;
        lang = clone.lang;
        defLevel = clone.defLevel;
        useFrame = clone.useFrame;
        extra = clone.extra;
        compact = clone.compact;
        trueForm = clone.trueForm;
        treasure = clone.treasure;
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

    private CommonStatic.Lang.Locale findLocale(int language) {
        if (language == -1)
            return null;

        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.Locale.values()) {
            if (language == locale.ordinal())
                return locale;
        }

        return null;
    }
}
