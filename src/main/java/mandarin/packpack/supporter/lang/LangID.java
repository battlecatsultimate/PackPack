package mandarin.packpack.supporter.lang;

import com.google.gson.JsonObject;
import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
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

        switch (locale) {
            case EN -> {
                if (EN_OBJ == null)
                    return id;

                if (EN_OBJ.has(id))
                    return EN_OBJ.get(id).getAsString();
            }
            case JP -> {
                if (JP_OBJ == null || !JP_OBJ.has(id)) {
                    if (EN_OBJ == null)
                        return id;

                    if (EN_OBJ.has(id))
                        return EN_OBJ.get(id).getAsString();
                    else
                        return id;
                }

                if (JP_OBJ.has(id))
                    return JP_OBJ.get(id).getAsString();
            }
            case KR -> {
                if (KR_OBJ == null || !KR_OBJ.has(id)) {
                    if (EN_OBJ == null)
                        return id;

                    if (EN_OBJ.has(id))
                        return EN_OBJ.get(id).getAsString();
                    else
                        return id;
                }

                if (KR_OBJ.has(id))
                    return KR_OBJ.get(id).getAsString();
            }
            case ZH -> {
                if (ZH_OBJ == null || !ZH_OBJ.has(id)) {
                    if (EN_OBJ == null)
                        return id;

                    if (EN_OBJ.has(id))
                        return EN_OBJ.get(id).getAsString();
                    else
                        return id;
                }

                if (ZH_OBJ.has(id))
                    return ZH_OBJ.get(id).getAsString();
            }
            case RU -> {
                if (RU_OBJ == null || !RU_OBJ.has(id)) {
                    if (EN_OBJ == null)
                        return id;

                    if (EN_OBJ.has(id))
                        return EN_OBJ.get(id).getAsString();
                    else
                        return id;
                }

                if (RU_OBJ.has(id))
                    return RU_OBJ.get(id).getAsString();
            }
            default -> {
                if (EN_OBJ == null)
                    return id;

                if (EN_OBJ.has(id))
                    return EN_OBJ.get(id).getAsString();
            }
        }

        return id;
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
}
