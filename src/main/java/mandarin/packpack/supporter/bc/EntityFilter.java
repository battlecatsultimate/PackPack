package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Combo;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;
import mandarin.packpack.supporter.KoreanSeparater;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EntityFilter {
    public static ArrayList<Form> findUnitWithName(String name, int lang) {
        ArrayList<Form> res = new ArrayList<>();

        for(Unit u : UserProfile.getBCData().units.getList()) {
            if(u == null)
                continue;

            for(Form f : u.forms) {
                for(int i = 0; i < StaticStore.langIndex.length; i++) {
                    StringBuilder fname = new StringBuilder(Data.trio(u.id.id)+"-"+Data.trio(f.fid)+" "+Data.trio(u.id.id)+" - "+Data.trio(f.fid) + " "
                    +u.id.id+"-"+f.fid+" "+Data.trio(u.id.id)+"-"+f.fid+" ");
                    fname.append(Data.trio(u.id.id)).append(Data.trio(f.fid)).append(" ");

                    if(MultiLangCont.get(f) != null) {
                        fname.append(StaticStore.safeMultiLangGet(f, StaticStore.langIndex[i]));
                    }

                    if(!f.names.toString().isBlank()) {
                        fname.append(" ").append(f.names.toString());
                    }

                    if(fname.toString().toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                        res.add(f);
                        break;
                    }

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = StaticStore.langIndex[i];

                    ArrayList<String> alias = AliasHolder.FALIAS.getCont(f);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(alias != null && !alias.isEmpty()) {
                        boolean added = false;

                        for(String a : alias) {
                            if(a.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                                if(a.toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                                    res.clear();

                                    res.add(f);

                                    return res;
                                }

                                added = true;

                                res.add(f);

                                break;
                            }
                        }

                        if(added)
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
                    String fname = StaticStore.safeMultiLangGet(f, lang);

                    if(fname == null || fname.isBlank()) {
                        fname = f.names.toString();
                    }

                    boolean done = false;

                    if(!fname.isBlank()) {
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
                                done = true;

                                similar.add(f);
                                similarity.add(s);

                                sMin = Math.min(s, sMin);

                                break;
                            }
                        }

                        if(!done) {
                            int s = damerauLevenshteinDistance(fname, name);

                            if(s <= 5) {
                                done = true;
                                similar.add(f);
                                similarity.add(s);

                                sMin = Math.min(s, sMin);
                            }
                        }
                    }

                    if(!done) {
                        int oldConfig = CommonStatic.getConfig().lang;
                        CommonStatic.getConfig().lang = lang;

                        ArrayList<String> alias = AliasHolder.FALIAS.getCont(f);

                        CommonStatic.getConfig().lang = oldConfig;

                        if(alias != null && !alias.isEmpty()) {
                            for(String a : alias) {
                                boolean added = false;

                                a = a.toLowerCase(Locale.ENGLISH);

                                if(lang == LangID.KR)
                                    a = KoreanSeparater.separate(a);

                                int wordNumber = StringUtils.countMatches(a, ' ') + 1;

                                String[] words;

                                if(wordNumber != 1) {
                                    words = getWords(a.split(" "), wordNumber);
                                } else {
                                    words = a.split(" ");
                                }

                                for (String word : words) {
                                    int s = damerauLevenshteinDistance(word, name);

                                    if (s <= 5) {
                                        added = true;
                                        similar.add(f);
                                        similarity.add(s);

                                        sMin = Math.min(s, sMin);

                                        break;
                                    }
                                }

                                if(!added) {
                                    int s = damerauLevenshteinDistance(a, name);

                                    if(s <= 5) {
                                        added = true;
                                        similar.add(f);
                                        similarity.add(s);

                                        sMin = Math.min(s, sMin);
                                    }
                                }

                                if(added)
                                    break;
                            }
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

    public static Form pickOneForm(String name, int lang) {
        ArrayList<Form> forms = findUnitWithName(name, lang);

        if(forms.isEmpty())
            return null;
        else if(forms.size() == 1)
            return forms.get(0);
        else {
            ArrayList<Integer> min = getFullDistances(forms, name, lang, Form.class);

            int allMin = Integer.MAX_VALUE;

            for(int m : min) {
                if(m >= 0)
                    allMin = Math.min(allMin, m);
            }

            int num = 0;
            int ind = 0;

            for(int i = 0; i < min.size(); i++) {
                if(min.get(i) == allMin) {
                    num++;
                    ind = i;
                }
            }

            if(num > 1)
                return null;
            else
                return forms.get(ind);
        }
    }

    public static ArrayList<Enemy> findEnemyWithName(String name, int lang) {
        ArrayList<Enemy> res = new ArrayList<>();

        for(Enemy e : UserProfile.getBCData().enemies.getList()) {
            if(e == null)
                continue;

            for(int i = 0; i < StaticStore.langIndex.length; i++) {
                StringBuilder ename = new StringBuilder(Data.trio(e.id.id))
                        .append(" ").append(duo(i)).append(" ");

                if(MultiLangCont.get(e) != null) {
                    ename.append(StaticStore.safeMultiLangGet(e, StaticStore.langIndex[i]));
                }

                if(ename.toString().toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    res.add(e);
                    break;
                }

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = StaticStore.langIndex[i];

                ArrayList<String> alias = AliasHolder.EALIAS.getCont(e);

                CommonStatic.getConfig().lang = oldConfig;

                if(alias != null && !alias.isEmpty()) {
                    boolean added = false;

                    for(String a : alias) {
                        if(a.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                            if(a.toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                                res.clear();

                                res.add(e);

                                return res;
                            }

                            added = true;

                            res.add(e);

                            break;
                        }
                    }

                    if(added)
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
                    ename = e.names.toString();

                boolean done = false;

                if(!ename.isBlank()) {
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
                            done = true;
                            similar.add(e);
                            similarity.add(s);

                            sMin = Math.min(s, sMin);

                            break;
                        }
                    }

                    if(!done) {
                        int s = damerauLevenshteinDistance(ename, name);

                        if(s <= 5) {
                            done = true;
                            similar.add(e);
                            similarity.add(s);

                            sMin = Math.min(s, sMin);
                        }
                    }
                }

                if(!done) {
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    ArrayList<String> alias = AliasHolder.EALIAS.getCont(e);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            boolean added = false;

                            a = a.toLowerCase(Locale.ENGLISH);

                            if(lang == LangID.KR)
                                a = KoreanSeparater.separate(a);

                            int wordNumber = StringUtils.countMatches(a, ' ') + 1;

                            String[] words;

                            if(wordNumber != 1) {
                                words = getWords(a.split(" "), wordNumber);
                            } else {
                                words = a.split(" ");
                            }

                            for (String word : words) {
                                int s = damerauLevenshteinDistance(word, name);

                                if (s <= 5) {
                                    added = true;
                                    similar.add(e);
                                    similarity.add(s);

                                    sMin = Math.min(s, sMin);

                                    break;
                                }
                            }

                            if(!added) {
                                int s = damerauLevenshteinDistance(a, name);

                                if(s <= 5) {
                                    added = true;
                                    similar.add(e);
                                    similarity.add(s);

                                    sMin = Math.min(s, sMin);
                                }
                            }

                            if(added)
                                break;
                        }
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

    public static Enemy pickOneEnemy(String name, int lang) {
        ArrayList<Enemy> e = findEnemyWithName(name, lang);

        if(e.isEmpty())
            return null;
        else if(e.size() == 1)
            return e.get(0);
        else {
            ArrayList<Integer> min = getFullDistances(e, name, lang, Enemy.class);

            int allMin = Integer.MAX_VALUE;

            for(int m : min) {
                if(m >= 0)
                    allMin = Math.min(allMin, m);
            }

            int num = 0;
            int ind = 0;

            for(int i = 0; i < min.size(); i++) {
                if(min.get(i) == allMin) {
                    num++;
                    ind = i;
                }
            }

            if(num > 1)
                return null;
            else
                return e.get(ind);
        }
    }

    public static ArrayList<Stage> findStageWithName(String[] names, int lang) {
        ArrayList<Stage> result = new ArrayList<>();

        if(searchMapColc(names) && names[0] != null && !names[0].isBlank()) {
            ArrayList<Stage> mcResult = new ArrayList<>();

            //Search normally
            for(MapColc mc : MapColc.values()) {
                if(mc == null)
                    continue;

                for(int i = 0; i < StaticStore.langIndex.length; i++ ) {
                    String mcName = StaticStore.safeMultiLangGet(mc, StaticStore.langIndex[i]);

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

                                    mcResult.add(st);
                                }
                            }

                            break;
                        }
                    }
                }
            }

            //Start autocorrect mode if no map colc found
            if(mcResult.isEmpty()) {
                ArrayList<Integer> distances = getDistances(MapColc.values(), names[0], lang, MapColc.class);

                int disMin = Integer.MAX_VALUE;

                for(int d : distances) {
                    if(d != -1) {
                        disMin = Math.min(d, disMin);
                    }
                }

                if(disMin <= 5) {
                    int i = 0;

                    for(MapColc mc : MapColc.values()) {
                        if(distances.get(i) == disMin) {
                            for(StageMap stm : mc.maps.getList()) {
                                mcResult.addAll(stm.list.getList());
                            }
                        }

                        i++;
                    }

                    result.addAll(mcResult);
                }
            } else {
                result.addAll(mcResult);
            }
        } else if(searchStageMap(names) && names[1] != null && !names[1].isBlank()) {
            ArrayList<Stage> stmResult = new ArrayList<>();

            boolean mcContain;

            if(names[0] != null && !names[0].isBlank())
                mcContain = needToPerformContainMode(MapColc.values(), names[0], MapColc.class);
            else
                mcContain = true;

            boolean stmContain = false;

            for(MapColc mc : MapColc.values()) {
                if(mc == null)
                    continue;

                stmContain |= needToPerformContainMode(mc.maps.getList(), names[1], StageMap.class);
            }

            if(mcContain) {
                for(MapColc mc : MapColc.values()) {
                    if(mc == null)
                        continue;

                    boolean s0 = false;

                    if(names[0] != null && !names[0].isBlank()) {
                        for(int i = 0; i < StaticStore.langIndex.length; i++) {
                            String mcName = StaticStore.safeMultiLangGet(mc, StaticStore.langIndex[i]);

                            if(mcName == null || mcName.isBlank())
                                continue;

                            if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }
                        }
                    } else {
                        s0 = true;
                    }

                    if(!s0)
                        continue;

                    if(stmContain) {
                        for(StageMap stm : mc.maps.getList()) {
                            if(stm == null)
                                continue;

                            boolean s1 = false;

                            for(int i = 0; i < StaticStore.langIndex.length; i++) {
                                String stmName = StaticStore.safeMultiLangGet(stm, StaticStore.langIndex[i]);

                                if(stmName == null || stmName.isBlank())
                                    continue;

                                if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                    s1 = true;
                                    break;
                                }
                            }

                            if(s1) {
                                stmResult.addAll(stm.list.getList());
                            }
                        }
                    } else {
                        ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                        int disMin = Integer.MAX_VALUE;

                        for(int d : stmDistances)
                            if(d != -1)
                                disMin = Math.min(d, disMin);

                        if(disMin <= 3) {
                            int i = 0;

                            for(StageMap stm : mc.maps.getList()) {
                                if(stmDistances.get(i) == disMin) {
                                    stmResult.addAll(stm.list.getList());
                                }

                                i++;
                            }
                        }
                    }
                }
            } else {
                ArrayList<Integer> mcDistances = getDistances(MapColc.values(), names[0], lang, MapColc.class);

                int mcMin = Integer.MAX_VALUE;

                for(int d : mcDistances)
                    if(d != -1)
                        mcMin = Math.min(d, mcMin);

                if(mcMin <= 5) {
                    int i = 0;

                    for(MapColc mc : MapColc.values()) {
                        if(mcDistances.get(i) == mcMin) {
                            if(stmContain) {
                                for(StageMap stm : mc.maps.getList()) {
                                    if(stm == null)
                                        continue;

                                    boolean s1 = false;

                                    for(int j = 0; j < 4; j++) {
                                        String stmName = StaticStore.safeMultiLangGet(stm, j);

                                        if(stmName == null || stmName.isBlank())
                                            continue;

                                        if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                            s1 = true;
                                            break;
                                        }
                                    }

                                    if(s1) {
                                        stmResult.addAll(stm.list.getList());
                                    }
                                }
                            } else {
                                ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                                int stmMin = Integer.MAX_VALUE;

                                for(int d : stmDistances)
                                    if(d != -1)
                                        stmMin = Math.min(d, stmMin);

                                if(stmMin <= 3) {
                                    int j = 0;

                                    for(StageMap stm : mc.maps.getList()) {
                                        if(stmDistances.get(j) == stmMin) {
                                            stmResult.addAll(stm.list.getList());
                                        }

                                        j++;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if(!stmResult.isEmpty()) {
                result.addAll(stmResult);
            }
        } else if(names[2] != null && !names[2].isBlank()) {
            ArrayList<Stage> stResult = new ArrayList<>();

            boolean mcContain;

            if(names[0] != null && !names[0].isBlank()) {
                mcContain = needToPerformContainMode(MapColc.values(), names[0], MapColc.class);
            } else {
                mcContain = true;
            }

            boolean stmContain = false;

            if(names[1] != null && !names[1].isBlank()) {
                for(MapColc mc : MapColc.values()) {
                    if(mc == null)
                        continue;

                    stmContain |= needToPerformContainMode(mc.maps.getList(), names[1], StageMap.class);
                }
            } else {
                stmContain = true;
            }

            boolean stContain = false;

            for(MapColc mc : MapColc.values()) {
                if(mc == null)
                    continue;

                for(StageMap stm : mc.maps.getList()) {
                    if(stm == null)
                        continue;

                    stContain |= needToPerformContainMode(stm.list.getList(), names[2], Stage.class);
                }
            }

            if(mcContain) {
                for(MapColc mc : MapColc.values()) {
                    if(mc == null)
                        continue;

                    boolean s0 = false;

                    if(names[0] != null && !names[0].isBlank()) {
                        for(int i = 0; i < StaticStore.langIndex.length; i++) {
                            String mcName = StaticStore.safeMultiLangGet(mc, StaticStore.langIndex[i]);

                            if(mcName == null || mcName.isBlank())
                                continue;

                            if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                                s0 = true;
                                break;
                            }
                        }
                    } else {
                        s0 = true;
                    }

                    if(!s0)
                        continue;

                    if(stmContain) {
                        for(StageMap stm : mc.maps.getList()) {
                            if(stm == null)
                                continue;

                            boolean s1 = false;

                            if(names[1] != null && !names[1].isBlank()) {
                                for(int i = 0; i < StaticStore.langIndex.length; i++) {
                                    String stmName = StaticStore.safeMultiLangGet(stm, StaticStore.langIndex[i]);

                                    if(stmName == null || stmName.isBlank())
                                        continue;

                                    if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                        s1 = true;
                                        break;
                                    }
                                }
                            } else {
                                s1 = true;
                            }

                            if(!s1)
                                continue;

                            if(stContain) {
                                for(Stage st : stm.list.getList()) {
                                    if(st == null)
                                        continue;

                                    boolean s2 = false;

                                    for(int i = 0; i < StaticStore.langIndex.length; i++) {
                                        String stName = StaticStore.safeMultiLangGet(st, StaticStore.langIndex[i]);

                                        if(stName == null || stName.isBlank())
                                            continue;

                                        if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                            s2 = true;
                                            break;
                                        }

                                        int oldConfig = CommonStatic.getConfig().lang;
                                        CommonStatic.getConfig().lang = StaticStore.langIndex[i];

                                        ArrayList<String> alias = AliasHolder.SALIAS.getCont(st);

                                        CommonStatic.getConfig().lang = oldConfig;

                                        if(alias != null && !alias.isEmpty()) {
                                            boolean added = false;

                                            for(String a : alias) {
                                                if(a.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    if(a.toLowerCase(Locale.ENGLISH).equals(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        stResult.clear();

                                                        stResult.add(st);

                                                        return stResult;
                                                    }

                                                    added = true;

                                                    s2 = true;

                                                    break;
                                                }
                                            }

                                            if(added)
                                                break;
                                        }
                                    }

                                    String[] ids = {
                                            mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                            mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                            DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                            DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                    };

                                    boolean s3 = false;

                                    for(String id : ids) {
                                        if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                            s3 = true;
                                            break;
                                        }
                                    }

                                    if(s2 || s3) {
                                        stResult.add(st);
                                    }
                                }
                            } else {
                                ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                int stMin = Integer.MAX_VALUE;

                                for(int d : stDistances)
                                    if(d != -1)
                                        stMin = Math.min(stMin, d);

                                if(stMin <= 3) {
                                    int stInd = 0;

                                    for(Stage st : stm.list.getList()) {
                                        if(stDistances.get(stInd) == stMin) {
                                            stResult.add(st);
                                        }

                                        stInd++;
                                    }
                                } else {
                                    for(Stage st : stm.list.getList()) {
                                        String[] ids = {
                                                mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                        };

                                        boolean s3 = false;

                                        for(String id : ids) {
                                            if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                s3 = true;
                                                break;
                                            }
                                        }

                                        if(s3)
                                            stResult.add(st);
                                    }
                                }
                            }
                        }
                    } else {
                        ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                        int stmMin = Integer.MAX_VALUE;

                        for(int d : stmDistances)
                            if(d != -1)
                                stmMin = Math.min(stmMin, d);

                        if(stmMin <= 3) {
                            int stmInd = 0;

                            for(StageMap stm : mc.maps.getList()) {
                                if(stmDistances.get(stmInd) == stmMin) {
                                    if(stContain) {
                                        for(Stage st : stm.list.getList()) {
                                            if(st == null)
                                                continue;

                                            boolean s2 = false;

                                            for(int i = 0; i < StaticStore.langIndex.length; i++) {
                                                String stName = StaticStore.safeMultiLangGet(st, StaticStore.langIndex[i]);

                                                if(stName == null || stName.isBlank())
                                                    continue;

                                                if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    s2 = true;
                                                    break;
                                                }

                                                int oldConfig = CommonStatic.getConfig().lang;
                                                CommonStatic.getConfig().lang = StaticStore.langIndex[i];

                                                ArrayList<String> alias = AliasHolder.SALIAS.getCont(st);

                                                CommonStatic.getConfig().lang = oldConfig;

                                                if(alias != null && !alias.isEmpty()) {
                                                    boolean added = false;

                                                    for(String a : alias) {
                                                        if(a.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                            added = true;
                                                            s2 = true;
                                                            break;
                                                        }
                                                    }

                                                    if(added)
                                                        break;
                                                }
                                            }

                                            String[] ids = {
                                                    mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                    DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                            };

                                            boolean s3 = false;

                                            for(String id : ids) {
                                                if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    s3 = true;
                                                    break;
                                                }
                                            }

                                            if(s2 || s3) {
                                                stResult.add(st);
                                            }
                                        }
                                    } else {
                                        ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                        int stMin = Integer.MAX_VALUE;

                                        for(int d : stDistances)
                                            if(d != -1)
                                                stMin = Math.min(stMin, d);

                                        if(stMin <= 3) {
                                            int stInd = 0;

                                            for(Stage st : stm.list.getList()) {
                                                if(stDistances.get(stInd) == stMin) {
                                                    stResult.add(st);
                                                }

                                                stInd++;
                                            }
                                        } else {
                                            for(Stage st : stm.list.getList()) {
                                                String[] ids = {
                                                        mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                        DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                };

                                                boolean s3 = false;

                                                for(String id : ids) {
                                                    if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s3 = true;
                                                        break;
                                                    }
                                                }

                                                if(s3)
                                                    stResult.add(st);
                                            }
                                        }
                                    }
                                }

                                stmInd++;
                            }
                        }
                    }
                }
            } else {
                ArrayList<Integer> mcDistances = getDistances(MapColc.values(), names[0], lang, MapColc.class);

                int mcMin = Integer.MAX_VALUE;

                for(int d : mcDistances)
                    if(d != -1)
                        mcMin = Math.min(d, mcMin);

                if(mcMin <= 5) {
                    int mcInd = 0;

                    for(MapColc mc : MapColc.values()) {
                        if(mcDistances.get(mcInd) == mcMin) {
                            if(stmContain) {
                                for(StageMap stm : mc.maps.getList()) {
                                    if(stm == null)
                                        continue;

                                    boolean s1 = false;

                                    if(names[1] != null && !names[1].isBlank()) {
                                        for(int i = 0; i < StaticStore.langIndex.length; i++) {
                                            String stmName = StaticStore.safeMultiLangGet(stm, StaticStore.langIndex[i]);

                                            if(stmName == null || stmName.isBlank())
                                                continue;

                                            if(stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH))) {
                                                s1 = true;
                                                break;
                                            }
                                        }
                                    } else {
                                        s1 = true;
                                    }

                                    if(!s1)
                                        continue;

                                    if(stContain) {
                                        for(Stage st : stm.list.getList()) {
                                            if(st == null)
                                                continue;

                                            boolean s2 = false;

                                            for(int i = 0; i < StaticStore.langIndex.length; i++) {
                                                String stName = StaticStore.safeMultiLangGet(st, StaticStore.langIndex[i]);

                                                if(stName == null || stName.isBlank())
                                                    continue;

                                                if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    s2 = true;
                                                    break;
                                                }
                                            }

                                            String[] ids = {
                                                    mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                    DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                    DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                            };

                                            boolean s3 = false;

                                            for(String id : ids) {
                                                if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                    s3 = true;
                                                    break;
                                                }
                                            }

                                            if(s2 || s3) {
                                                stResult.add(st);
                                            }
                                        }
                                    } else {
                                        ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                        int stMin = Integer.MAX_VALUE;

                                        for(int d : stDistances)
                                            if(d != -1)
                                                stMin = Math.min(stMin, d);

                                        if(stMin <= 3) {
                                            int stInd = 0;

                                            for(Stage st : stm.list.getList()) {
                                                if(stDistances.get(stInd) == stMin) {
                                                    stResult.add(st);
                                                }

                                                stInd++;
                                            }
                                        } else {
                                            for(Stage st : stm.list.getList()) {
                                                String[] ids = {
                                                        mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                        DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                        DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                };

                                                boolean s3 = false;

                                                for(String id : ids) {
                                                    if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                        s3 = true;
                                                        break;
                                                    }
                                                }

                                                if(s3)
                                                    stResult.add(st);
                                            }
                                        }
                                    }
                                }
                            } else {
                                ArrayList<Integer> stmDistances = getDistances(mc.maps.getList(), names[1], lang, StageMap.class);

                                int stmMin = Integer.MAX_VALUE;

                                for(int d : stmDistances)
                                    if(d != -1)
                                        stmMin = Math.min(stmMin, d);

                                if(stmMin <= 3) {
                                    int stmInd = 0;

                                    for(StageMap stm : mc.maps.getList()) {
                                        if(stmDistances.get(stmInd) == stmMin) {
                                            if(stContain) {
                                                for(Stage st : stm.list.getList()) {
                                                    if(st == null)
                                                        continue;

                                                    boolean s2 = false;

                                                    for(int i = 0; i < StaticStore.langIndex.length; i++) {
                                                        String stName = StaticStore.safeMultiLangGet(st, StaticStore.langIndex[i]);

                                                        if(stName == null || stName.isBlank())
                                                            continue;

                                                        if(stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                            s2 = true;
                                                            break;
                                                        }
                                                    }

                                                    String[] ids = {
                                                            mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                            mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                            DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                            DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                    };

                                                    boolean s3 = false;

                                                    for(String id : ids) {
                                                        if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                            s3 = true;
                                                            break;
                                                        }
                                                    }

                                                    if(s2 || s3) {
                                                        stResult.add(st);
                                                    }
                                                }
                                            } else {
                                                ArrayList<Integer> stDistances = getDistances(stm.list.getList(), names[2], lang, Stage.class);

                                                int stMin = Integer.MAX_VALUE;

                                                for(int d : stDistances)
                                                    if(d != -1)
                                                        stMin = Math.min(stMin, d);

                                                if(stMin <= 3) {
                                                    int stInd = 0;

                                                    for(Stage st : stm.list.getList()) {
                                                        if(stDistances.get(stInd) == stMin) {
                                                            stResult.add(st);
                                                        }
                                                        stInd++;
                                                    }
                                                } else {
                                                    for(Stage st : stm.list.getList()) {
                                                        String[] ids = {
                                                                mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                                mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                                                DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                                                DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                                                        };

                                                        boolean s3 = false;

                                                        for(String id : ids) {
                                                            if(id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH))) {
                                                                s3 = true;
                                                                break;
                                                            }
                                                        }

                                                        if(s3)
                                                            stResult.add(st);
                                                    }
                                                }
                                            }
                                        }

                                        stmInd++;
                                    }
                                }
                            }
                        }

                        mcInd++;
                    }
                }
            }

            if(!stResult.isEmpty())
                result.addAll(stResult);
        }

        return result;
    }

    public static ArrayList<Stage> findStageWithMapName(String name) {
        ArrayList<Stage> stmResult = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            for(StageMap stm : mc.maps.getList()) {
                if(stm == null)
                    continue;

                boolean s1 = false;

                for(int i = 0; i < StaticStore.langIndex.length; i++) {
                    String stmName = StaticStore.safeMultiLangGet(stm, StaticStore.langIndex[i]);

                    if(stmName == null || stmName.isBlank())
                        continue;

                    if(stmName.toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                        s1 = true;
                        break;
                    }
                }

                if(s1) {
                    stmResult.addAll(stm.list.getList());
                }
            }
        }

        return stmResult;
    }

    public static Stage pickOneStage(String[] names, int lang) {
        ArrayList<Stage> stages = findStageWithName(names, lang);

        if(stages.isEmpty() && names[0] == null && names[1] == null) {
            stages = EntityFilter.findStageWithMapName(names[2]);
        }

        if(stages.isEmpty()) {
            return null;
        } else if(stages.size() == 1) {
            return stages.get(0);
        } else {
            if(names[2] == null) {
                return null;
            } else {
                ArrayList<Integer> mins = getFullDistances(stages, names[0], lang, Stage.class);

                int allMin = Integer.MAX_VALUE;

                for(int m : mins) {
                    if(m >= 0)
                        allMin = Math.min(m, allMin);
                }

                int num = 0;
                int ind = 0;

                for(int i = 0; i < stages.size(); i++) {
                    if(mins.get(i) == allMin) {
                        num++;
                        ind = i;
                    }
                }

                if(num > 1)
                    return null;
                else
                    return stages.get(ind);
            }
        }
    }

    public static ArrayList<Stage> findStageByEnemy(Enemy e) {
        ArrayList<Stage> result = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            for(StageMap stm : mc.maps.getList()) {
                if(stm == null)
                    continue;

                for(Stage st : stm.list.getList()) {
                    if(st == null)
                        continue;

                    for(SCDef.Line line : st.data.datas) {
                        if(line.enemy.equals(e.id)) {
                            result.add(st);
                            break;
                        }
                    }
                }
            }
        }

        return result;
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

                sMin = Math.min(sMin, damerauLevenshteinDistance(medalName, name));
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

        List<Combo> fullCombo = UserProfile.getBCData().combos.getList();

        for(int i = 0; i < fullCombo.size(); i++) {
            Combo c = fullCombo.get(i);

            if(f == null) {
                if(cName != null) {
                    for(int l = 0; l < 4; l++) {
                        int oldConfig = CommonStatic.getConfig().lang;
                        CommonStatic.getConfig().lang = l;

                        String comboName = MultiLangCont.getStatic().COMNAME.getCont(c) + " | " + DataToString.getComboType(c, l);

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

                    if(k >= c.forms.length || c.forms[k] == null || c.forms[k].unit == null)
                        continue;

                    if(c.forms[k].unit.id.id == f.unit.id.id && c.forms[k].fid <= f.fid) {
                        if(cName != null) {
                            for(int l = 0; l < 4; l++) {
                                int oldConfig = CommonStatic.getConfig().lang;
                                CommonStatic.getConfig().lang = l;

                                String comboName = MultiLangCont.getStatic().COMNAME.getCont(c) + " | " + DataToString.getComboType(c, l);

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

        result.sort(Comparator.comparingInt(c -> c.id.id));

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

    private static <T> boolean needToPerformContainMode(Iterable<T> set, String keyword, Class<T> cls) {
        for(T t : set) {
            if(t == null)
                continue;

            keyword = keyword.toLowerCase(Locale.ENGLISH);

            for(int i = 0; i < StaticStore.langIndex.length; i++) {
                String name = StaticStore.safeMultiLangGet(t, StaticStore.langIndex[i]);

                if(name != null && !name.isBlank()) {
                    if(name.toLowerCase(Locale.ENGLISH).contains(keyword))
                        return true;

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = StaticStore.langIndex[i];

                    ArrayList<String> alias;

                    if(cls == Form.class) {
                        alias = AliasHolder.FALIAS.getCont((Form) t);
                    } else if(cls == Enemy.class) {
                        alias = AliasHolder.EALIAS.getCont((Enemy) t);
                    } else if(cls == Stage.class) {
                        alias = AliasHolder.SALIAS.getCont((Stage) t);
                    } else {
                        CommonStatic.getConfig().lang = oldConfig;
                        continue;
                    }

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            if(a.toLowerCase(Locale.ENGLISH).contains(keyword))
                                return true;
                        }
                    }
                } else {
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = StaticStore.langIndex[i];

                    ArrayList<String> alias;

                    if(cls == Form.class) {
                        alias = AliasHolder.FALIAS.getCont((Form) t);
                    } else if(cls == Enemy.class) {
                        alias = AliasHolder.EALIAS.getCont((Enemy) t);
                    } else if(cls == Stage.class) {
                        alias = AliasHolder.SALIAS.getCont((Stage) t);
                    } else {
                        CommonStatic.getConfig().lang = oldConfig;
                        continue;
                    }

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            if(a.toLowerCase(Locale.ENGLISH).contains(keyword))
                                return true;
                        }
                    }
                }
            }

            if(cls == Stage.class) {
                Stage st = (Stage) t;

                StageMap stm = st.getCont();

                if(stm != null) {
                    MapColc mc = stm.getCont();

                    if(mc != null) {
                        String[] ids = {
                                mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                mc.getSID()+"-"+stm.id.id+"-"+st.id.id,
                                DataToString.getMapCode(mc)+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id),
                                DataToString.getMapCode(mc)+"-"+stm.id.id+"-"+st.id.id
                        };

                        for(String id : ids) {
                            if(id.toLowerCase(Locale.ENGLISH).contains(keyword)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static <T> ArrayList<Integer> getFullDistances(Iterable<T> set, String keyword, int lang, Class<T> cls) {
        ArrayList<Integer> distances = new ArrayList<>();
        int allMin = Integer.MAX_VALUE;

        keyword = keyword.toLowerCase(Locale.ENGLISH);

        if(lang == LangID.KR)
            keyword = KoreanSeparater.separate(keyword);

        for(T t : set) {
            if(t == null) {
                distances.add(-1);
                continue;
            }

            String name = StaticStore.safeMultiLangGet(t, lang);

            if(name == null || name.isBlank()) {
                int disMin = -1;

                ArrayList<String> alias;

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                if(cls == Form.class) {
                    alias = AliasHolder.FALIAS.getCont((Form) t);
                } else if(cls == Enemy.class) {
                    alias = AliasHolder.EALIAS.getCont((Enemy) t);
                } else if(cls == Stage.class) {
                    alias = AliasHolder.SALIAS.getCont((Stage) t);
                } else {
                    CommonStatic.getConfig().lang = oldConfig;

                    distances.add(disMin);
                    continue;
                }

                CommonStatic.getConfig().lang = oldConfig;

                if(alias != null && !alias.isEmpty()) {
                    for(String a : alias) {
                        a = a.toLowerCase(Locale.ENGLISH);

                        if(lang == LangID.KR)
                            a = KoreanSeparater.separate(a);

                        int aliasResult = damerauLevenshteinDistance(a, keyword);

                        disMin = Math.min(disMin, aliasResult);
                        allMin = Math.min(allMin, aliasResult);
                    }
                }

                distances.add(disMin);
            } else {
                name = name.toLowerCase(Locale.ENGLISH);

                if(lang == LangID.KR) {
                    name = KoreanSeparater.separate(name);
                }

                int disMin;

                int result = damerauLevenshteinDistance(name, keyword);

                disMin = result;
                allMin = Math.min(allMin, result);

                if(disMin > allMin) {
                    ArrayList<String> alias;

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(cls == Form.class) {
                        alias = AliasHolder.FALIAS.getCont((Form) t);
                    } else if(cls == Enemy.class) {
                        alias = AliasHolder.EALIAS.getCont((Enemy) t);
                    } else if(cls == Stage.class) {
                        alias = AliasHolder.SALIAS.getCont((Stage) t);
                    } else {
                        CommonStatic.getConfig().lang = oldConfig;

                        distances.add(disMin);
                        continue;
                    }

                    CommonStatic.getConfig().lang = oldConfig;

                    if(alias != null && !alias.isEmpty()) {
                        for(String a : alias) {
                            a = a.toLowerCase(Locale.ENGLISH);

                            if(lang == LangID.KR)
                                a = KoreanSeparater.separate(a);

                            int aliasResult = damerauLevenshteinDistance(a, keyword);

                            disMin = Math.min(disMin, aliasResult);
                            allMin = Math.min(allMin, aliasResult);
                        }
                    }
                }

                distances.add(disMin);
            }
        }

        return distances;
    }

    private static <T> ArrayList<Integer> getDistances(Iterable<T> set, String keyword, int lang, Class<T> cls) {
        ArrayList<Integer> distances = new ArrayList<>();
        int allMin = Integer.MAX_VALUE;

        keyword = keyword.toLowerCase(Locale.ENGLISH);

        if(lang == LangID.KR)
            keyword = KoreanSeparater.separate(keyword);

        for(T t : set) {
            if(t == null) {
                distances.add(-1);
                continue;
            }

            String name = StaticStore.safeMultiLangGet(t, lang);

            if(name == null || name.isBlank()) {
                distances.add(-1);
                continue;
            }

            name = name.toLowerCase(Locale.ENGLISH);

            if(lang == LangID.KR) {
                name = KoreanSeparater.separate(name);
            }

            String[] words;

            int wordNumber = StringUtils.countMatches(keyword, ' ') + 1;

            if(wordNumber != 1) {
                words = getWords(name.split(" "), wordNumber);
            } else {
                words = name.split(" ");
            }

            int disMin = Integer.MAX_VALUE;

            for(String word : words) {
                int result = damerauLevenshteinDistance(word, keyword);
                disMin = Math.min(disMin, result);
                allMin = Math.min(allMin, result);
            }

            if(disMin > allMin) {
                ArrayList<String> alias;

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                if(cls == Form.class) {
                    alias = AliasHolder.FALIAS.getCont((Form) t);
                } else if(cls == Enemy.class) {
                    alias = AliasHolder.EALIAS.getCont((Enemy) t);
                } else if(cls == Stage.class) {
                    alias = AliasHolder.SALIAS.getCont((Stage) t);
                } else {
                    CommonStatic.getConfig().lang = oldConfig;

                    distances.add(disMin);
                    continue;
                }

                CommonStatic.getConfig().lang = oldConfig;

                if(alias != null && !alias.isEmpty()) {
                    for(String a : alias) {
                        a = a.toLowerCase(Locale.ENGLISH);

                        if(lang == LangID.KR)
                            a = KoreanSeparater.separate(a);

                        wordNumber = StringUtils.countMatches(keyword, ' ') + 1;

                        if(wordNumber != 1) {
                            words = getWords(a.split(" "), wordNumber);
                        } else {
                            words = a.split(" ");
                        }

                        for(String word : words) {
                            int result = damerauLevenshteinDistance(word, keyword);
                            disMin = Math.min(disMin, result);
                            allMin = Math.min(allMin, result);
                        }
                    }
                }
            }

            disMin = Math.min(disMin, damerauLevenshteinDistance(name, keyword));
            allMin = Math.min(allMin, damerauLevenshteinDistance(name, keyword));

            distances.add(disMin);
        }

        return distances;
    }
}
