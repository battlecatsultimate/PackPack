package mandarin.packpack.supporter.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangID {
    public static JsonObject EN_OBJ;
    public static JsonObject JP_OBJ;
    public static JsonObject KR_OBJ;
    public static JsonObject ZH_OBJ;
    public static JsonObject RU_OBJ;

    private static final Pattern formatRegex = Pattern.compile("%(d|s|(\\.\\d+)?[fg])");

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

        String value = getStringFromPath(obj, id, locale, false);

        if (value.equals(id) && obj != EN_OBJ) {
            value = getStringFromPath(EN_OBJ, id, locale, false);
        }

        return value;
    }

    public static String getStringByIDSuppressed(String id, @Nullable CommonStatic.Lang.Locale locale) {
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

        String value = getStringFromPath(obj, id, locale, true);

        if (value.equals(id) && obj != EN_OBJ) {
            value = getStringFromPath(EN_OBJ, id, locale, true);
        }

        return value;
    }

    public static boolean hasID(String id, CommonStatic.Lang.Locale locale) {
        JsonObject original = switch (locale) {
            case JP -> JP_OBJ;
            case KR -> KR_OBJ;
            case ZH -> ZH_OBJ;
            case RU -> RU_OBJ;
            default -> EN_OBJ;
        };

        JsonObject object = original;

        if (object == null)
            return false;

        String[] pathData = id.split("\\.");

        for (int i = 0; i < pathData.length; i++) {
            String path = pathData[i];

            if (i < pathData.length - 1) {
                if (!object.has(path))
                    return false;

                object = object.getAsJsonObject(path);
            } else {
                return object.has(path);
            }
        }

        return original.has(id);
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

            findMissingTexts("", EN_OBJ, target);

            System.out.println("------------------------");
        }
    }

    @Nonnull
    private static String getStringFromPath(JsonObject obj, String path, CommonStatic.Lang.Locale locale, boolean suppress) {
        JsonObject currentPath = obj;

        String[] segments = path.split("\\.");

        for (int i = 0; i < segments.length; i++) {
            if (!currentPath.has(segments[i])) {
                if (!suppress) {
                    StaticStore.logger.uploadLog("W/LangID::getStringFromPath - No such path found in " + locale + " : " + path);
                }

                return path;
            }

            JsonElement e = currentPath.get(segments[i]);

            if (i < segments.length - 1) {
                if (!(e instanceof JsonObject o)) {
                    if (!suppress) {
                        StaticStore.logger.uploadLog("W/LangID::getStringFromPath - No such path found in " + locale + " : " + path);
                    }

                    return path;
                }

                currentPath = o;
            } else {
                if (!(e instanceof JsonPrimitive p)) {
                    if (!suppress) {
                        StaticStore.logger.uploadLog("W/LangID::getStringFromPath - No such path found in " + locale + " : " + path);
                    }

                    return path;
                }

                return p.getAsString();
            }
        }

        if (!obj.has(path)) {
            if (!suppress) {
                StaticStore.logger.uploadLog("W/LangID::getStringFromPath - No such path found in " + locale + " : " + path);
            }

            return path;
        }

        JsonElement e = obj.get(path);

        if (!(e instanceof JsonPrimitive p)) {
            if (!suppress) {
                StaticStore.logger.uploadLog("W/LangID::getStringFromPath - No such path found in " + locale + " : " + path);
            }

            return path;
        }

        return p.getAsString();
    }

    private static void findMissingTexts(String currentPath, JsonObject en, JsonObject target) {
        for (String key : en.keySet()) {
            JsonElement e = en.get(key);

            if (!target.has(key)) {
                if (e instanceof JsonObject) {
                    System.out.println("O : " + currentPath + key);
                } else {
                    System.out.println("E : " + currentPath + key);
                }

                continue;
            }

            JsonElement te = target.get(key);

            if (e instanceof JsonObject obj && te instanceof JsonObject tObj) {
                findMissingTexts(currentPath + key + ".", obj, tObj);
            } else if (e instanceof JsonObject || te instanceof JsonObject) {
                throw new IllegalStateException("E/LangID::findMissingTexts - Desynced tag found : " + currentPath + key);
            } else if (e instanceof JsonPrimitive ep && te instanceof JsonPrimitive tp) {
                String enValue = ep.getAsString();
                String value = tp.getAsString();

                Matcher enMatcher = formatRegex.matcher(enValue);
                Matcher matcher = formatRegex.matcher(value);

                List<String> enFormat = new ArrayList<>();
                List<String> format = new ArrayList<>();

                while (enMatcher.find()) {
                    enFormat.add(enMatcher.group(0));
                }

                while (matcher.find()) {
                    format.add(matcher.group(0));
                }

                if (enFormat.size() != format.size()) {
                    System.out.println("F : " + currentPath + key + " -> " + enFormat + ", " + format);
                }

                for (int i = 0; i < enFormat.size(); i++) {
                    if (!enFormat.get(i).equals(format.get(i))) {
                        System.out.println("F : " + currentPath + key + " -> " + enFormat + ", " + format);

                        break;
                    }
                }
            }
        }
    }
}