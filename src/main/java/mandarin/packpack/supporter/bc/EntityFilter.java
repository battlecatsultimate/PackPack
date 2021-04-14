package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Combo;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;
import mandarin.packpack.supporter.KoreanSeparater;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class EntityFilter {
    public static ArrayList<Form> findUnitWithName(String name, int lang) {
        ArrayList<Form> res = new ArrayList<>();

        for(Unit u : UserProfile.getBCData().units.getList()) {
            if(u == null)
                continue;

            for(Form f : u.forms) {
                for(int i = 0; i < 4; i++) {
                    StringBuilder fname = new StringBuilder(Data.trio(u.id.id)+"-"+Data.trio(f.fid)+" "+Data.trio(u.id.id)+" - "+Data.trio(f.fid) + " "
                    +u.id.id+"-"+f.fid+" "+Data.trio(u.id.id)+"-"+f.fid+" ");
                    fname.append(Data.trio(u.id.id)).append(Data.trio(f.fid)).append(" ");

                    if(MultiLangCont.get(f) != null) {
                        fname.append(StaticStore.safeMultiLangGet(f, i));
                    }

                    if(f.name != null) {
                        fname.append(" ").append(f.name);
                    }

                    if(fname.toString().toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                        res.add(f);
                        break;
                    }
                }
            }
        }

        if(res.isEmpty()) {
            ArrayList<Form> similar = new ArrayList<>();
            ArrayList<Integer> similarity = new ArrayList<>();

            name = name.toLowerCase(Locale.ENGLISH);

            if(lang == LangID.KR)
                name = KoreanSeparater.separate(name);

            int sMin = 10;

            for(Unit u : UserProfile.getBCData().units.getList()) {
                if(u == null)
                    continue;

                for(Form f : u.forms) {
                    CommonStatic.getConfig().lang = lang;
                    String fname = StaticStore.safeMultiLangGet(f, lang);

                    if(fname == null || fname.isBlank()) {
                        fname = f.name;
                    }

                    if(fname == null || fname.isBlank()) {
                        continue;
                    }

                    fname = fname.toLowerCase(Locale.ENGLISH);

                    if(lang == LangID.KR)
                        fname = KoreanSeparater.separate(fname);

                    int wordNumber = StringUtils.countMatches(name, ' ') + 1;

                    String[] words;

                    if(wordNumber != 1) {
                        words = getWords(fname.split(" "), wordNumber);
                    } else {
                        words = fname.split(" ");
                    }

                    for (String word : words) {
                        int s = damerauLevenshteinDistance(word, name);

                        if (s <= 5) {
                            similar.add(f);
                            similarity.add(s);

                            sMin = Math.min(s, sMin);

                            break;
                        }
                    }
                }
            }

            ArrayList<Form> finalResult = new ArrayList<>();

            for(int i = 0; i < similar.size(); i++) {
                if(similarity.get(i) == sMin)
                    finalResult.add(similar.get(i));
            }

            return finalResult;
        } else {
            return res;
        }
    }

    public static ArrayList<Enemy> findEnemyWithName(String name, int lang) {
        ArrayList<Enemy> res = new ArrayList<>();

        for(Enemy e : UserProfile.getBCData().enemies.getList()) {
            if(e == null)
                continue;

            for(int i = 0; i < 4; i++) {
                StringBuilder ename = new StringBuilder(Data.trio(e.id.id))
                        .append(" ").append(duo(i)).append(" ");

                if(MultiLangCont.get(e) != null) {
                    ename.append(StaticStore.safeMultiLangGet(e, i));
                }

                if(ename.toString().toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    res.add(e);
                    break;
                }
            }
        }

        if(res.isEmpty()) {
            ArrayList<Enemy> similar = new ArrayList<>();
            ArrayList<Integer> similarity = new ArrayList<>();

            name = name.toLowerCase(Locale.ENGLISH);

            int sMin = 10;

            if(lang == LangID.KR)
                name = KoreanSeparater.separate(name);

            for(Enemy e : UserProfile.getBCData().enemies.getList()) {
                if(e == null)
                    continue;

                String ename = StaticStore.safeMultiLangGet(e, lang);

                if(ename == null || ename.isBlank())
                    ename = e.name;

                if(ename == null || ename.isBlank())
                    continue;

                ename = ename.toLowerCase(Locale.ENGLISH);

                if(lang == LangID.KR)
                    ename = KoreanSeparater.separate(ename);

                int wordNumber = StringUtils.countMatches(name, ' ') + 1;

                String[] words;

                if(wordNumber != 1) {
                    words = getWords(ename.split(" "), wordNumber);
                } else {
                    words = ename.split(" ");
                }

                for(String word : words) {
                    int s = damerauLevenshteinDistance(word, name);

                    if(s <=  5) {
                        similar.add(e);
                        similarity.add(s);

                        sMin = Math.min(s, sMin);

                        break;
                    }
                }
            }

            if(similar.isEmpty())
                return similar;

            ArrayList<Enemy> finalResult = new ArrayList<>();

            for(int i = 0; i < similar.size(); i++) {
                if(similarity.get(i) == sMin)
                    finalResult.add(similar.get(i));
            }

            return finalResult;
        } else {
            return res;
        }
    }

    public static ArrayList<Stage> findStageWithName(String[] names, int lang) {
        ArrayList<Stage> res = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            if(searchMapColc(names) && names[0] != null && !names[0].isBlank()) {
                ArrayList<Stage> mcStages = new ArrayList<>();

                for(int i = 0; i < 4; i++) {
                    String mcName = StaticStore.safeMultiLangGet(mc, lang);

                    if(mcName == null || mcName.isBlank())
                        continue;

                    if(!mcName.isBlank()) {
                        if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                            for(StageMap stm : mc.maps.getList()) {
                                if(stm == null)
                                    continue;

                                for(Stage st : stm.list.getList()) {
                                    if(st == null)
                                        continue;

                                    mcStages.add(st);
                                }
                            }

                            break;
                        }
                    }
                }

                if(mcStages.isEmpty()) {
                    String mcName = StaticStore.safeMultiLangGet(mc, lang);

                    if(mcName == null || mcName.isBlank())
                        continue;

                    mcName = mcName.toLowerCase(Locale.ENGLISH);

                    if(lang == LangID.KR)
                        mcName = KoreanSeparater.separate(mcName);

                    String nam = names[0].toLowerCase(Locale.ENGLISH);

                    if(lang == LangID.KR)
                        nam = KoreanSeparater.separate(nam);

                    String[] words;

                    int wordNumber = StringUtils.countMatches(nam, ' ') + 1;

                    if(wordNumber != 1) {
                        words = getWords(mcName.split(" "), wordNumber);
                    } else {
                        words = mcName.split(" ");
                    }

                    for(String word : words) {
                        int s = damerauLevenshteinDistance(word, nam);

                        if(s <= 5) {
                            for(StageMap stm : mc.maps.getList()) {
                                if(stm == null)
                                    continue;

                                for(Stage st : stm.list.getList()) {
                                    if(st == null)
                                        continue;

                                    mcStages.add(st);
                                }
                            }

                            break;
                        }
                    }


                }

                if(!mcStages.isEmpty()) {
                    res.addAll(mcStages);
                }

                continue;
            }

            for(StageMap stm : mc.maps.getList()) {
                if(stm == null)
                    continue;

                if(searchStageMap(names) && names[1] != null && !names[1].isBlank()) {
                    ArrayList<Stage> stmStages = new ArrayList<>();

                    for(int i = 0; i < 4; i++) {
                        boolean s0 = true;

                        if(names[0] != null && !names[0].isBlank()) {
                            String mcName = StaticStore.safeMultiLangGet(mc, i);

                            if(mcName != null && !mcName.isBlank()) {
                                s0 = mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        if(!s0)
                            continue;

                        boolean s1 = true;

                        if(names[1] != null && !names[1].isBlank()) {
                            String stmName = StaticStore.safeMultiLangGet(stm, i);

                            if(stmName == null || stmName.isBlank())
                                continue;

                            if(!stmName.isBlank()) {
                                s1 = stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        if(s1) {
                            for(Stage st : stm.list.getList()) {
                                if(st == null)
                                    continue;

                                stmStages.add(st);
                            }

                            break;
                        }
                    }

                    if(stmStages.isEmpty()) {
                        boolean s0 = true;

                        if(names[0] != null && !names[0].isBlank()) {
                            String mcName = StaticStore.safeMultiLangGet(mc, lang);

                            if(mcName != null && !mcName.isBlank()) {
                                mcName = mcName.toLowerCase(Locale.ENGLISH);
                                String nam = names[0].toLowerCase(Locale.ENGLISH);

                                if(lang == LangID.KR) {
                                    mcName = KoreanSeparater.separate(mcName);

                                    nam = KoreanSeparater.separate(nam);
                                }

                                String[] words;

                                int wordNumber = StringUtils.countMatches(nam, ' ') + 1;

                                if(wordNumber != 1) {
                                    words = getWords(mcName.split(" "), wordNumber);
                                } else {
                                    words = mcName.split(" ");
                                }

                                for(String word : words) {
                                    if(damerauLevenshteinDistance(word, nam) <= 5) {
                                        s0 = true;
                                        break;
                                    }

                                    s0 = false;
                                }
                            }
                        }

                        if(!s0)
                            continue;

                        boolean s1 = true;

                        if(names[1] != null && !names[1].isBlank()) {
                            String stmName = StaticStore.safeMultiLangGet(stm, lang);

                            if(stmName != null && !stmName.isBlank()) {
                                stmName = stmName.toLowerCase(Locale.ENGLISH);
                                String nam = names[1].toLowerCase(Locale.ENGLISH);

                                if (lang == LangID.KR) {
                                    stmName = KoreanSeparater.separate(stmName);
                                    nam = KoreanSeparater.separate(nam);
                                }

                                String[] words;

                                int wordNumber = StringUtils.countMatches(nam, ' ' ) + 1;

                                if (wordNumber != 1) {
                                    words = getWords(stmName.split(" "), wordNumber);
                                } else {
                                    words = stmName.split(" ");
                                }

                                for (String word : words) {
                                    int s = damerauLevenshteinDistance(word, nam);

                                    System.out.println("WORD : "+word+" | NAME : "+nam+ " = " + s);

                                    if (s <= 3) {
                                        s1 = true;
                                        break;
                                    }

                                    s1 = false;
                                }
                            }
                        }

                        if(s1) {
                            for(Stage st : stm.list.getList()) {
                                if(st == null)
                                    continue;

                                stmStages.add(st);
                            }
                        }
                    }

                    if(!stmStages.isEmpty()) {
                        res.addAll(stmStages);
                    }

                    continue;
                }

                for(Stage st : stm.list.getList()) {
                    boolean added = false;

                    for(int i = 0; i < 4; i++) {
                        CommonStatic.getConfig().lang = i;

                        if(names[2] == null)
                            continue;

                        String stName = StaticStore.safeMultiLangGet(st, i);

                        if(stName == null || stName.isBlank())
                            continue;

                        boolean s0 = true;

                        if(names[0] != null && !names[0].isBlank()) {
                            String mcName = MultiLangCont.get(mc);

                            if(mcName != null && !mcName.isBlank()) {
                                s0 = mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        if(!s0)
                            continue;

                        boolean s1 = true;

                        if(names[1] != null && !names[1].isBlank()) {
                            String stmName = MultiLangCont.get(stm);

                            if(stmName != null && !stmName.isBlank()) {
                                s1 = stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        if(!s1)
                            continue;

                        boolean s2 = false;

                        if(!stName.isBlank()) {
                            s2 = stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH));
                        }

                        String id = mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id)+" "+mc.getSID()+"-"+stm.id.id+"-"+st.id.id;

                        boolean s3 = id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH));

                        if(s2 || s3) {
                            res.add(st);
                            added = true;

                            break;
                        }
                    }

                    if(!added) {
                        boolean s0 = true;

                        if(names[0] != null && !names[0].isBlank()) {
                            String mcName = StaticStore.safeMultiLangGet(mc, lang);

                            if(mcName != null && !mcName.isBlank()) {
                                mcName = mcName.toLowerCase(Locale.ENGLISH);
                                String nam = names[0].toLowerCase(Locale.ENGLISH);

                                if(lang == LangID.KR) {
                                    mcName = KoreanSeparater.separate(mcName);

                                    nam = KoreanSeparater.separate(nam);
                                }

                                String[] words;

                                int wordNumber = StringUtils.countMatches(nam, ' ') + 1;

                                if(wordNumber != 1) {
                                    words = getWords(mcName.split(" "), wordNumber);
                                } else {
                                    words = mcName.split(" ");
                                }

                                for(String word : words) {
                                    if(damerauLevenshteinDistance(word, nam) <= 5) {
                                        s0 = true;
                                        break;
                                    }

                                    s0 = false;
                                }
                            }
                        }

                        if(!s0)
                            continue;

                        boolean s1 = true;

                        if(names[1] != null && !names[1].isBlank()) {
                            String stmName = StaticStore.safeMultiLangGet(stm, lang);

                            if(stmName != null && !stmName.isBlank()) {
                                stmName = stmName.toLowerCase(Locale.ENGLISH);
                                String nam = names[1].toLowerCase(Locale.ENGLISH);

                                if (lang == LangID.KR) {
                                    stmName = KoreanSeparater.separate(stmName);
                                    nam = KoreanSeparater.separate(nam);
                                }

                                String[] words;

                                int wordNumber = StringUtils.countMatches(nam, ' ' ) + 1;

                                if (wordNumber != 1) {
                                    words = getWords(stmName.split(" "), wordNumber);
                                } else {
                                    words = stmName.split(" ");
                                }

                                for (String word : words) {
                                    if (damerauLevenshteinDistance(word, nam) <= 3) {
                                        s1 = true;
                                        break;
                                    }

                                    s1 = false;
                                }
                            }
                        }

                        if(!s1)
                            continue;

                        boolean s2 = false;

                        if(names[2] != null && !names[2].isBlank()) {
                            String stName = StaticStore.safeMultiLangGet(st, lang);

                            if(stName != null && !stName.isBlank()) {
                                stName = stName.toLowerCase(Locale.ENGLISH);
                                String nam = names[2].toLowerCase(Locale.ENGLISH);

                                if(lang == LangID.KR) {
                                    stName = KoreanSeparater.separate(stName);
                                    nam = KoreanSeparater.separate(nam);
                                }

                                String[] words;

                                int wordNumber = StringUtils.countMatches(nam, ' ') + 1;

                                if(wordNumber != 1) {
                                    words = getWords(stName.split(" "), wordNumber);
                                } else {
                                    words = stName.split(" ");
                                }

                                for(String word : words) {
                                    if(damerauLevenshteinDistance(word, nam) <= 3) {
                                        s2 = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if(s2) {
                            res.add(st);
                        }
                    }
                }
            }
        }

        return res;
    }

    public static ArrayList<Integer> findMedalByName(String name, int lang) {
        ArrayList<Integer> result = new ArrayList<>();

        for(int i = 0; i < StaticStore.medalNumber; i++) {
            for(int j = 0; j < 4; j++) {
                int oldConfg = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = j;

                String medalName = StaticStore.MEDNAME.getCont(i);

                CommonStatic.getConfig().lang = oldConfg;

                if(medalName == null || medalName.isBlank()) {
                    medalName = Data.trio(i);
                } else {
                    medalName += " " + Data.trio(i);
                }

                if(medalName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    result.add(i);
                    break;
                }
            }
        }

        if(result.isEmpty()) {
            ArrayList<Integer> similar = new ArrayList<>();
            ArrayList<Integer> similarity = new ArrayList<>();

            int sMin = 10;

            name = name.toLowerCase(Locale.ENGLISH);

            if(lang == LangID.KR)
                name = KoreanSeparater.separate(name);

            for(int i = 0; i < StaticStore.medalNumber; i++) {
                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String medalName = StaticStore.MEDNAME.getCont(i);

                if(medalName == null || medalName.isBlank())
                    continue;

                CommonStatic.getConfig().lang = oldConfig;

                medalName = medalName.toLowerCase(Locale.ENGLISH);

                if(lang == LangID.KR)
                    medalName = KoreanSeparater.separate(medalName);

                int wordNumber = StringUtils.countMatches(name, ' ') + 1;

                String[] words;

                if(wordNumber != 1) {
                    words = getWords(medalName.split(" "), wordNumber);
                } else {
                    words = medalName.split(" ");
                }

                for(String word : words) {
                    int s = damerauLevenshteinDistance(word, name);

                    if(s <= 5) {
                        similar.add(i);
                        similarity.add(s);

                        sMin = Math.min(sMin, s);

                        break;
                    }
                }
            }

            if(similar.isEmpty())
                return similar;

            ArrayList<Integer> finalResult = new ArrayList<>();

            for(int i = 0; i < similar.size(); i++) {
                if (sMin == similarity.get(i)) {
                    finalResult.add(similar.get(i));
                }
            }

            return finalResult;
        } else {
            return result;
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public static ArrayList<Combo> filterComboWithUnit(Form f, String cName) {
        ArrayList<Combo> result = new ArrayList<>();

        for(int i = 0; i < CommonStatic.getBCAssets().combos.length; i++) {
            Combo[] combos = CommonStatic.getBCAssets().combos[i];

            for(int j = 0; j < combos.length; j++) {
                Combo c = combos[j];

                if(f == null) {
                    if(cName != null) {
                        for(int l = 0; l < 4; l++) {
                            int oldConfig = CommonStatic.getConfig().lang;
                            CommonStatic.getConfig().lang = l;

                            String comboName = MultiLangCont.getStatic().COMNAME.getCont(c.name);

                            CommonStatic.getConfig().lang = oldConfig;

                            if(comboName.toLowerCase(Locale.ENGLISH).contains(cName.toLowerCase(Locale.ENGLISH))) {
                                result.add(c);
                                break;
                            }
                        }
                    } else {
                        result.add(c);
                    }
                } else {
                    for(int k = 0; k < 5; k++) {
                        boolean added = false;

                        if(c.units.get(k) == null || c.units.get(k).unit == null)
                            continue;

                        if(c.units.get(k).unit.id.id == f.unit.id.id && c.units.get(k).fid <= f.fid) {
                            if(cName != null) {
                                for(int l = 0; l < 4; l++) {
                                    int oldConfig = CommonStatic.getConfig().lang;
                                    CommonStatic.getConfig().lang = l;

                                    String comboName = MultiLangCont.getStatic().COMNAME.getCont(c.name);

                                    CommonStatic.getConfig().lang = oldConfig;

                                    if(comboName.toLowerCase(Locale.ENGLISH).contains(cName.toLowerCase(Locale.ENGLISH))) {
                                        result.add(c);
                                        added = true;
                                        break;
                                    }
                                }
                            } else {
                                result.add(c);
                                added = true;
                            }
                        }

                        if(added)
                            break;
                    }
                }
            }
        }

        result.sort(Comparator.comparingInt(c -> c.id));

        return result;
    }

    private static String duo(int i) {
        if(i < 10)
            return "0"+i;
        else
            return String.valueOf(i);
    }

    private static boolean searchMapColc(String[] names) {
        return names[1] == null && names[2] == null;
    }

    private static boolean searchStageMap(String[] names) {
        return names[2] == null;
    }

    private static int damerauLevenshteinDistance(String src, String compare) {
        int[][] table = new int[src.length() + 1][compare.length() + 1];

        for(int i = 0; i < src.length() + 1; i++) {
            table[i][0] = i;
        }

        for(int i  = 0; i < compare.length() + 1; i++) {
            table[0][i] = i;
        }

        for(int i = 1; i < src.length() + 1; i++) {
            for(int j = 1; j < compare.length() + 1; j++) {
                int cost;

                if(src.charAt(i-1) == compare.charAt(j-1))
                    cost = 0;
                else
                    cost = 1;

                table[i][j] = Math.min(Math.min(table[i-1][j] + 1, table[i][j-1] + 1), table[i-1][j-1] + cost);

                if(i > 1 && j > 1 && src.charAt(i-1) == compare.charAt(j-2) && src.charAt(i-2) == compare.charAt(j-1)) {
                    table[i][j] = Math.min(table[i][j], table[i-2][j-2]);
                }
            }
        }

        return table[src.length()][compare.length()];
    }

    private static String[] getWords(String[] src, int numberOfWords) {
        int length;

        if(src.length % numberOfWords == 0)
            length = src.length / numberOfWords;
        else
            length = src.length / numberOfWords + 1;

        String[] result = new String[length];

        for(int i = 0; i < src.length; i += numberOfWords) {
            StringBuilder builder = new StringBuilder();

            for(int j = 0; j < numberOfWords; j++) {
                if(i + j < src.length) {
                    builder.append(src[i+j]);

                    if(j < numberOfWords - 1 && i+j < src.length - 1)
                        builder.append(" ");
                }
            }

            result[i/numberOfWords] = builder.toString();
        }

        return result;
    }
}
