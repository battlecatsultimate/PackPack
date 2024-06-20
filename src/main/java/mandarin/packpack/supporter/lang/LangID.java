package mandarin.packpack.supporter.lang;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;

import java.io.File;

public class LangID {
    public static final int EN = 0;
    public static final int ZH = 1;
    public static final int KR = 2;
    public static final int JP = 3;
    public static final int RU = 4;
    public static final int FR = 6;
    public static final int IT = 9;
    public static final int ES = 8;
    public static final int DE = 5;
    public static final int TH = 10;

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

    public static String getStringByID(String id, int locale) {
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

    public static boolean hasID(String id, int locale) {
        return switch (locale) {
            case EN -> EN_OBJ != null && EN_OBJ.has(id);
            case JP -> JP_OBJ != null && JP_OBJ.has(id);
            case KR -> KR_OBJ != null && KR_OBJ.has(id);
            case ZH -> ZH_OBJ != null && ZH_OBJ.has(id);
            default -> false;
        };
    }

    public static void printMissingTags() {
        for(int i = ZH; i <= JP; i++) {
            JsonObject target;

            switch (i) {
                case ZH:
                    target = ZH_OBJ;
                    System.out.println("---------- TW ----------");
                    break;
                case KR:
                    target = KR_OBJ;
                    System.out.println("---------- KR ----------");
                    break;
                case JP:
                    target = JP_OBJ;
                    System.out.println("---------- JP ----------");
                    break;
                default:
                    return;
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
