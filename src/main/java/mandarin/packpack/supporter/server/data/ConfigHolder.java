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

        if(obj.has("compact")) {
            holder.compact = obj.get("compact").getAsBoolean();
        }

        if(obj.has("trueForm")) {
            holder.trueForm = obj.get("trueForm").getAsBoolean();
        }

        if(obj.has("treasure")) {
            holder.treasure = obj.get("treasure").getAsBoolean();
        }

        if (obj.has("showUnitDescription")) {
            holder.showUnitDescription = obj.get("showUnitDescription").getAsBoolean();
        }

        if (obj.has("showEvolveImage")) {
            holder.showEvolveImage = obj.get("showEvolveImage").getAsBoolean();
        }

        if (obj.has("showEvolveDescription")) {
            holder.showEvolveDescription = obj.get("showEvolveDescription").getAsBoolean();
        }

        if (obj.has("showEnemyDescription")) {
            holder.showEnemyDescription = obj.get("showEnemyDescription").getAsBoolean();
        }

        if (obj.has("showMiscellaneous")) {
            holder.showMiscellaneous = obj.get("showMiscellaneous").getAsBoolean();
        }

        if (obj.has("showMaterialDrop")) {
            holder.showMaterialDrop = obj.get("showMaterialDrop").getAsBoolean();
        }

        if (obj.has("showExtraStage")) {
            holder.showExtraStage = obj.get("showExtraStage").getAsBoolean();
        }

        if (obj.has("showDropInfo")) {
            holder.showDropInfo = obj.get("showDropInfo").getAsBoolean();
        }

        if (obj.has("extra")) {
            holder.showUnitDescription = true;
            holder.showEvolveDescription = true;
            holder.showEvolveImage = true;

            holder.showEnemyDescription = true;

            holder.showMiscellaneous = true;
            holder.showMaterialDrop = true;
            holder.showExtraStage = true;
            holder.showDropInfo = true;
        }

        return holder;
    }

    public String prefix = StaticStore.globalPrefix;
    @Nullable
    public CommonStatic.Lang.Locale lang = null;
    public int defLevel = 30;
    public boolean useFrame = true, compact = false, trueForm = false, treasure = false;

    // Unit Command Config
    public boolean showUnitDescription = false;
    public boolean showEvolveImage = false;
    public boolean showEvolveDescription = false;

    //Enemy Command Config
    public boolean showEnemyDescription = false;

    //Stage Command Config
    public boolean showMiscellaneous = false;
    public boolean showMaterialDrop = false;
    public boolean showExtraStage = false;
    public boolean showDropInfo = false;

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("prefix", prefix);
        obj.addProperty("lang", lang == null ? "" : lang.name());
        obj.addProperty("defLevel", defLevel);
        obj.addProperty("useFrame", useFrame);
        obj.addProperty("compact", compact);
        obj.addProperty("trueForm", trueForm);
        obj.addProperty("treasure", treasure);
        obj.addProperty("showUnitDescription", showUnitDescription);
        obj.addProperty("showEvolveImage", showEvolveImage);
        obj.addProperty("showEvolveDescription", showEvolveDescription);
        obj.addProperty("showEnemyDescription", showEnemyDescription);
        obj.addProperty("showMiscellaneous", showMiscellaneous);
        obj.addProperty("showMaterialDrop", showMaterialDrop);
        obj.addProperty("showExtraStage", showExtraStage);
        obj.addProperty("showDropInfo", showDropInfo);

        return obj;
    }

    public void inject(ConfigHolder clone) {
        prefix = clone.prefix;
        lang = clone.lang;
        defLevel = clone.defLevel;
        useFrame = clone.useFrame;
        compact = clone.compact;
        trueForm = clone.trueForm;
        treasure = clone.treasure;
        showUnitDescription = clone.showUnitDescription;
        showEvolveImage = clone.showEvolveImage;
        showEnemyDescription = clone.showEnemyDescription;
        showMiscellaneous = clone.showMiscellaneous;
        showMaterialDrop = clone.showMaterialDrop;
        showExtraStage = clone.showExtraStage;
        showDropInfo = clone.showDropInfo;
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
        c.compact = compact;
        c.trueForm = trueForm;
        c.treasure = treasure;
        c.showUnitDescription = showUnitDescription;
        c.showEvolveImage = showEvolveImage;
        c.showEvolveDescription = showEvolveDescription;
        c.showEnemyDescription = showEnemyDescription;
        c.showMiscellaneous = showMiscellaneous;
        c.showMaterialDrop = showMaterialDrop;
        c.showExtraStage = showExtraStage;
        c.showDropInfo = showDropInfo;

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
