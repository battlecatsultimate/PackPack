package mandarin.packpack.supporter.lang;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;

import java.io.File;

public class LangID {
    public static final int EN = 0;
    public static final int ZH = 1;
    public static final int KR = 2;
    public static final int JP = 3;
    public static final int FR = 6;
    public static final int IT = 9;
    public static final int ES = 8;
    public static final int DE = 5;

    public static JsonObject EN_OBJ;
    public static JsonObject JP_OBJ;
    public static JsonObject KR_OBJ;
    public static JsonObject ZH_OBJ;

    public static void initialize() {
        File f = new File("./data/lang");

        File[] fs = f.listFiles();

        if(fs != null) {
            for(File g : fs) {
                if(g.getName().equals("en.json")) {
                    EN_OBJ = StaticStore.getJsonFile("lang/en");
                } else if(g.getName().equals("jp.json")) {
                    JP_OBJ = StaticStore.getJsonFile("lang/jp");
                } else if(g.getName().equals("kr.json")) {
                    KR_OBJ = StaticStore.getJsonFile("lang/kr");
                } else if(g.getName().equals("zh.json")) {
                    ZH_OBJ = StaticStore.getJsonFile("lang/zh");
                }
            }
        }
    }

    public static String getStringByID(String id, int locale) {
        switch (locale) {
            case EN:
                if(EN_OBJ == null)
                    return id;

                if(EN_OBJ.has(id))
                    return EN_OBJ.get(id).getAsString();

                break;
            case JP:
                if(JP_OBJ == null || !JP_OBJ.has(id)) {
                    if(EN_OBJ == null)
                        return id;

                    if(EN_OBJ.has(id))
                        return EN_OBJ.get(id).getAsString();
                    else
                        return id;
                }

                if(JP_OBJ.has(id))
                    return JP_OBJ.get(id).getAsString();

                break;
            case KR:
                if(KR_OBJ == null || !KR_OBJ.has(id)) {
                    if(EN_OBJ == null)
                        return id;

                    if(EN_OBJ.has(id))
                        return EN_OBJ.get(id).getAsString();
                    else
                        return id;
                }

                if(KR_OBJ.has(id))
                    return KR_OBJ.get(id).getAsString();

                break;
            case ZH:
                if(ZH_OBJ == null || !ZH_OBJ.has(id)) {
                    if(EN_OBJ == null)
                        return id;

                    if(EN_OBJ.has(id))
                        return EN_OBJ.get(id).getAsString();
                    else
                        return id;
                }

                if(ZH_OBJ.has(id))
                    return ZH_OBJ.get(id).getAsString();

                break;
            default:
                if(EN_OBJ == null)
                    return id;

                if(EN_OBJ.has(id))
                    return EN_OBJ.get(id).getAsString();

                break;
        }

        return id;
    }
}
