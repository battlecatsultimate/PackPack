package mandarin.packpack.supporter.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class LangID {
    public static JsonObject EN_OBJ;
    public static JsonObject JP_OBJ;
    public static JsonObject KR_OBJ;
    public static JsonObject ZH_OBJ;
    public static JsonObject RU_OBJ;

    public static void initialize() {
        File f = new File("./data/lang");

        File[] fs = f.listFiles();

        if(fs != null) {
            for(File g : fs) {
                switch (g.getName()) {
                    case "en.json" -> EN_OBJ = StaticStore.getJsonFile("lang/en");
                    case "jp.json" -> JP_OBJ = StaticStore.getJsonFile("lang/jp");
                    case "kr.json" -> KR_OBJ = StaticStore.getJsonFile("lang/kr");
                    case "zh.json" -> ZH_OBJ = StaticStore.getJsonFile("lang/zh");
                    case "ru.json" -> RU_OBJ = StaticStore.getJsonFile("lang/ru");
                }
            }
        }

        printMissingTags();
    }

    public static String getStringByID(String id, @Nullable CommonStatic.Lang.Locale locale) {
        if (locale == null)
            locale = CommonStatic.Lang.Locale.EN;

        JsonObject obj = switch (locale) {
            case JP -> JP_OBJ;
            case KR -> KR_OBJ;
            case ZH -> ZH_OBJ;
            case RU -> RU_OBJ;
            default -> EN_OBJ;
        };

        if (obj == null) {
            if (locale == CommonStatic.Lang.Locale.EN) {
                return id;
            } else {
                obj = EN_OBJ;
            }
        }

        if (obj == null)
            return id;

        String value = getStringFromPath(obj, id, locale);

        if (value.equals(id) && obj != EN_OBJ) {
            value = getStringFromPath(EN_OBJ, id, locale);
        }

        return value;
    }

    public static boolean hasID(String id, CommonStatic.Lang.Locale locale) {
        return switch (locale) {
            case EN -> EN_OBJ != null && EN_OBJ.has(id);
            case JP -> JP_OBJ != null && JP_OBJ.has(id);
            case KR -> KR_OBJ != null && KR_OBJ.has(id);
            case ZH -> ZH_OBJ != null && ZH_OBJ.has(id);
            default -> false;
        };
    }

    public static void printMissingTags() {
        CommonStatic.Lang.Locale[] localeList = {
                CommonStatic.Lang.Locale.ZH,
                CommonStatic.Lang.Locale.KR,
                CommonStatic.Lang.Locale.JP,
                CommonStatic.Lang.Locale.RU
        };

        for(CommonStatic.Lang.Locale locale : localeList) {
            JsonObject target;

            switch (locale) {
                case ZH -> {
                    target = ZH_OBJ;
                    System.out.println("---------- TW ----------");
                }
                case KR -> {
                    target = KR_OBJ;
                    System.out.println("---------- KR ----------");
                }
                case JP -> {
                    target = JP_OBJ;
                    System.out.println("---------- JP ----------");
                }
                case RU -> {
                    target = RU_OBJ;
                    System.out.println("---------- RU ----------");
                }
                default -> {
                    return;
                }
            }

            for(String key : EN_OBJ.keySet()) {
                if (!target.has(key)) {
                    System.out.println(key);
                }
            }

            System.out.println("------------------------");
        }
    }

    @NotNull
    private static String getStringFromPath(JsonObject obj, String path, CommonStatic.Lang.Locale locale) {
        JsonObject currentPath = obj;

        String[] segments = path.split("\\.");

        for (int i = 0; i < segments.length; i++) {
            if (!currentPath.has(segments[i])) {
                StaticStore.logger.uploadLog("W/LangID::getStringFromPath - No such path found in " + locale + " : " + path);

                return path;
            }

            JsonElement e = currentPath.get(segments[i]);

            if (i < segments.length - 1) {
                if (!(e instanceof JsonObject o)) {
                    StaticStore.logger.uploadLog("W/LangID::getStringFromPath - Invalid path found in " + locale + " : " + path);

                    return path;
                }

                currentPath = o;
            } else {
                if (!(e instanceof JsonPrimitive p)) {
                    StaticStore.logger.uploadLog("W/LangID::getStringFromPath - Invalid path found in " + locale + " : " + path);

                    return path;
                }

                return p.getAsString();
            }
        }

        if (!obj.has(path)) {
            StaticStore.logger.uploadLog("W/LangID::getStringFromPath - No such path found in " + locale + " : " + path);

            return path;
        }

        JsonElement e = obj.get(path);

        if (!(e instanceof JsonPrimitive p)) {
            StaticStore.logger.uploadLog("W/LangID::getStringFromPath - Invalid path found in " + locale + " : " + path);

            return path;
        }

        return p.getAsString();
    }
}