package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.stage.info.DefStageInfo;
import common.util.unit.Combo;
import common.util.unit.Enemy;
import common.util.unit.Form;
import common.util.unit.Unit;
import kotlin.jvm.functions.Function2;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.KoreanSeparater;
import mandarin.packpack.supporter.server.data.AliasHolder;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;

public class EntityFilter {
    private static final int TOLERANCE = 5;

    private static  final int[] storyChapterMonthly = {
            3, 4, 5, 6, 7, 8, 9
    };

    private static final int[] cycloneMonthly = {
            14, 15, 16, 39, 43, 66, 96, 122, 157
    };

    private static final int[] cycloneCatamin = {
            18, 19, 20, 21, 22, 23, 35, 36, 49
    };

    private static final String spaceRegex = "[\\-☆]";
    private static final String apostropheRegex = "‘";

    public static ArrayList<Form> findUnitWithName(String name, boolean trueForm, CommonStatic.Lang.Locale lang) {
        ArrayList<Form> initialData = new ArrayList<>();

        for (Unit u : UserProfile.getBCData().units.getList()) {
            if (u == null)
                continue;

            for (Form f : u.forms) {
                if (f == null)
                    continue;

                initialData.add(f);
            }
        }

        Function<Form, String> idRegexGenerator = f -> "(" + Data.trio(f.unit.id.id) + "|" + f.unit.id.id + ")( *- *)?(" + Data.trio(f.fid) + "|" + f.fid + ")";
        Function2<Form, CommonStatic.Lang.Locale, String> nameGneerator = (f, locale) -> {
            String formName = StaticStore.safeMultiLangGet(f, locale);

            if (formName == null || formName.isBlank()) {
                formName = f.names.toString();

                if (formName.isBlank())
                    formName = "";
            }

            return formName;
        };

        ArrayList<Form> filteredData = filterData(initialData, name, lang, AliasHolder.FALIAS, false, nameGneerator, idRegexGenerator);

        if (!filteredData.isEmpty()) {
            if(trueForm) {
                ArrayList<Form> filtered = new ArrayList<>();

                for(int i = 0; i < filteredData.size(); i++) {
                    Form f = filteredData.get(i);
                    int lastIndex = f.unit.forms.length - 1;

                    if(f.fid == lastIndex && !filtered.contains(f)) {
                        filtered.add(f);
                    } else if(f.fid != lastIndex) {
                        Form finalForm = f.unit.forms[lastIndex];

                        if(!filtered.contains(finalForm))
                            filtered.add(finalForm);
                    }
                }

                return filtered;
            }

            return filteredData;
        }

        ArrayList<Form> dynamicData = filterData(initialData, name, lang, AliasHolder.FALIAS, true, nameGneerator, idRegexGenerator);

        if(trueForm) {
            ArrayList<Form> filtered = new ArrayList<>();

            for(int i = 0; i < dynamicData.size(); i++) {
                Form f = dynamicData.get(i);
                int lastIndex = f.unit.forms.length - 1;

                if(f.fid == lastIndex && !filtered.contains(f)) {
                    filtered.add(f);
                } else if(f.fid != lastIndex) {
                    Form finalForm = f.unit.forms[lastIndex];

                    if(!filtered.contains(finalForm))
                        filtered.add(finalForm);
                }
            }

            return filtered;
        }

        return dynamicData;
    }

    public static ArrayList<Enemy> findEnemyWithName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<Enemy> initialData = new ArrayList<>(UserProfile.getBCData().enemies.getList());

        Function<Enemy, String> idRegexGenerator = e -> Data.trio(e.id.id);
        Function2<Enemy, CommonStatic.Lang.Locale, String> nameGenerator = (e, locale) -> {
            String enemyName = StaticStore.safeMultiLangGet(e, locale);

            if (enemyName == null || enemyName.isBlank()) {
                enemyName = e.names.toString();

                if (enemyName.isBlank())
                    enemyName = "";
            }

            return enemyName;
        };

        ArrayList<Enemy> filteredData = filterData(initialData, name, lang, AliasHolder.EALIAS, false, nameGenerator, idRegexGenerator);

        if (!filteredData.isEmpty())
            return filteredData;

        return filterData(initialData, name, lang, AliasHolder.EALIAS, true, nameGenerator, idRegexGenerator);
    }

    public static ArrayList<MapColc> findMapCollectionWithName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<MapColc> initialMapCollections = new ArrayList<>(MapColc.values());
        ArrayList<MapColc> filteredMapCollections;

        Function<MapColc, String> idRegexGenerator = DataToString::getMapCode;
        Function2<MapColc, CommonStatic.Lang.Locale, String> nameGenerator = (mc, locale) -> {
            String mapCollectionName = StaticStore.safeMultiLangGet(mc, locale);

            if (mapCollectionName == null || mapCollectionName.isBlank())
                mapCollectionName = "";

            return mapCollectionName;
        };

        filteredMapCollections = filterData(initialMapCollections, name, lang, null, false, nameGenerator, idRegexGenerator);

        if (filteredMapCollections.isEmpty())
            filteredMapCollections = filterData(initialMapCollections, name, lang, null, true, nameGenerator, idRegexGenerator);

        return filteredMapCollections;
    }

    public static ArrayList<StageMap> findStageMapWithName(String[] names, CommonStatic.Lang.Locale lang) {
        if ((names[0] == null || names[0].isBlank()) && (names[1] == null || names[1].isBlank())) {
            return new ArrayList<>();
        }

        ArrayList<MapColc> initialMapCollections = new ArrayList<>(MapColc.values());
        ArrayList<MapColc> filteredMapCollections;

        if (names[0] != null && !names[0].isBlank()) {
            Function<MapColc, String> idRegexGenerator = DataToString::getMapCode;
            Function2<MapColc, CommonStatic.Lang.Locale, String> nameGenerator = (mc, locale) -> {
                String mapCollectionName = StaticStore.safeMultiLangGet(mc, locale);

                if (mapCollectionName == null || mapCollectionName.isBlank())
                    mapCollectionName = "";

                return mapCollectionName;
            };

            filteredMapCollections = filterData(initialMapCollections, names[0], lang, null, false, nameGenerator, idRegexGenerator);

            if (filteredMapCollections.isEmpty())
                filteredMapCollections = filterData(initialMapCollections, names[0], lang, null, true, nameGenerator, idRegexGenerator);
        } else {
            filteredMapCollections = new ArrayList<>(initialMapCollections);
        }

        if (filteredMapCollections.isEmpty())
            return new ArrayList<>();

        ArrayList<StageMap> initialStageMaps = new ArrayList<>();
        ArrayList<StageMap> filteredStageMaps;

        for (MapColc mc : filteredMapCollections) {
            initialStageMaps.addAll(mc.maps.getList());
        }

        if (names[1] != null && !names[1].isBlank()) {
            Function<StageMap, String> idRegexGenerator = stm -> DataToString.getMapCode(stm.getCont()) + "(( *- *)?" + Data.trio(stm.id.id) + "| *- *" + stm.id.id + ")";
            Function2<StageMap, CommonStatic.Lang.Locale, String> nameGenerator = (stm, locale) -> {
                String stageMapName = StaticStore.safeMultiLangGet(stm, locale);

                if (stageMapName == null || stageMapName.isBlank()) {
                    stageMapName = "";
                }

                return stageMapName;
            };

            filteredStageMaps = filterData(initialStageMaps, names[1], lang, null, false, nameGenerator, idRegexGenerator);

            if (filteredStageMaps.isEmpty())
                filteredStageMaps = filterData(initialStageMaps, names[1], lang, null, true, nameGenerator, idRegexGenerator);
        } else {
            filteredStageMaps = new ArrayList<>(initialStageMaps);
        }

        return filteredStageMaps;
    }

    public static ArrayList<Stage> findStageWithName(String[] names, CommonStatic.Lang.Locale lang) {
        if ((names[0] == null || names[0].isBlank()) && (names[1] == null || names[1].isBlank()) && (names[2] == null || names[2].isBlank())) {
            return new ArrayList<>();
        }

        ArrayList<MapColc> initialMapCollections = new ArrayList<>(MapColc.values());
        ArrayList<MapColc> filteredMapCollections;

        if (names[0] != null && !names[0].isBlank()) {
            Function<MapColc, String> idRegexGenerator = DataToString::getMapCode;
            Function2<MapColc, CommonStatic.Lang.Locale, String> nameGenerator = (mc, locale) -> {
                String mapCollectionName = StaticStore.safeMultiLangGet(mc, locale);

                if (mapCollectionName == null || mapCollectionName.isBlank())
                    mapCollectionName = "";

                return mapCollectionName;
            };

            filteredMapCollections = filterData(initialMapCollections, names[0], lang, null, false, nameGenerator, idRegexGenerator);

            if (filteredMapCollections.isEmpty())
                filteredMapCollections = filterData(initialMapCollections, names[0], lang, null, true, nameGenerator, idRegexGenerator);
        } else {
            filteredMapCollections = new ArrayList<>(initialMapCollections);
        }

        if (filteredMapCollections.isEmpty())
            return new ArrayList<>();

        ArrayList<StageMap> initialStageMaps = new ArrayList<>();
        ArrayList<StageMap> filteredStageMaps;

        for (MapColc mc : filteredMapCollections) {
            initialStageMaps.addAll(mc.maps.getList());
        }

        if (names[1] != null && !names[1].isBlank()) {
            Function<StageMap, String> idRegexGenerator = stm -> DataToString.getMapCode(stm.getCont()) + "(( *- *)?" + Data.trio(stm.id.id) + "| *- *" + stm.id.id + ")";
            Function2<StageMap, CommonStatic.Lang.Locale, String> nameGenerator = (stm, locale) -> {
                String stageMapName = StaticStore.safeMultiLangGet(stm, locale);

                if (stageMapName == null || stageMapName.isBlank()) {
                    stageMapName = "";
                }

                return stageMapName;
            };

            filteredStageMaps = filterData(initialStageMaps, names[1], lang, null, false, nameGenerator, idRegexGenerator);

            if (filteredStageMaps.isEmpty())
                filteredStageMaps = filterData(initialStageMaps, names[1], lang, null, true, nameGenerator, idRegexGenerator);
        } else {
            filteredStageMaps = new ArrayList<>(initialStageMaps);
        }

        if (filteredStageMaps.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<Stage> initialStages = new ArrayList<>();
        ArrayList<Stage> filteredStages;

        for (StageMap stm : filteredStageMaps) {
            initialStages.addAll(stm.list.getList());
        }

        if (names[2] != null && !names[2].isBlank()) {
            Function<Stage, String> idRegexGenerator = st -> DataToString.getMapCode(st.getCont().getCont()) + "(( *- *)?" + Data.trio(st.getCont().id.id) + "| *- *" + st.getCont().id.id + ")(( *- *)?" + Data.trio(st.id.id) + "| *- *" + st.id.id + ")";
            Function2<Stage, CommonStatic.Lang.Locale, String> nameGenerator = (st, locale) -> {
                String stageName = StaticStore.safeMultiLangGet(st, locale);

                if (stageName == null || stageName.isBlank()) {
                    stageName = "";
                }

                return stageName;
            };

            filteredStages = filterData(initialStages, names[2], lang, AliasHolder.SALIAS, false, nameGenerator, idRegexGenerator);

            if (filteredStages.isEmpty())
                filteredStages = filterData(initialStages, names[2], lang, AliasHolder.SALIAS, false, nameGenerator, idRegexGenerator);
        } else {
            filteredStages = new ArrayList<>(initialStages);
        }

        return filteredStages;
    }

    public static ArrayList<Stage> findStageWithMapName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<Stage> result = new ArrayList<>();

        ArrayList<StageMap> initialStageMaps = new ArrayList<>();

        for (MapColc mc : MapColc.values()) {
            initialStageMaps.addAll(mc.maps.getList());
        }

        Function<StageMap, String> idRegexGenerator = stm -> DataToString.getMapCode(stm.getCont()) + "(( *- *)?" + Data.trio(stm.id.id) + "| *- *" + stm.id.id + ")";
        Function2<StageMap, CommonStatic.Lang.Locale, String> nameGenerator = (stm, locale) -> {
            String stageMapName = StaticStore.safeMultiLangGet(stm, locale);

            if (stageMapName == null || stageMapName.isBlank()) {
                stageMapName = "";
            }

            return stageMapName;
        };

        ArrayList<StageMap> filteredStageMaps = filterData(initialStageMaps, name, lang, null, false, nameGenerator, idRegexGenerator);

        if (filteredStageMaps.isEmpty())
            filteredStageMaps = filterData(initialStageMaps, name, lang, null, true, nameGenerator, idRegexGenerator);

        for (StageMap stm : filteredStageMaps) {
            result.addAll(stm.list.getList());
        }

        return result;
    }

    public static ArrayList<Stage> findStage(List<Enemy> enemies, int music, int background, int castle, boolean hasBoss, boolean orOperator, boolean monthly) {
        ArrayList<Stage> result = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            for(StageMap stm : mc.maps.getList()) {
                if(stm == null)
                    continue;

                if (monthly && !isMonthly(mc, stm)) {
                    continue;
                }

                for(Stage st : stm.list.getList()) {
                    if(st == null)
                        continue;

                    if(music != -1) {
                        boolean mus = st.mus0 != null && st.mus0.id == music;

                        if(!mus && st.mus1 != null && st.mus1.id == music)
                            mus = true;

                        if(!mus)
                            continue;
                    }

                    if(background != -1 && (st.bg == null || st.bg.id != background))
                        continue;

                    if(castle != -1 && (st.castle == null || st.castle.id != castle))
                        continue;

                    if(enemies.isEmpty() || containsEnemies(st.data.datas, enemies, hasBoss, orOperator))
                        result.add(st);
                }
            }
        }

        return result;
    }

    public static ArrayList<Integer> findMedalByName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<Integer> initialMedals = new ArrayList<>();

        for (int i = 0; i < StaticStore.medalNumber; i++) {
            initialMedals.add(i);
        }

        Function<Integer, String> idRegexGenerator = id -> "(" + Data.trio(id) + "|" + id + ")";
        Function2<Integer, CommonStatic.Lang.Locale, String> nameGenerator = (id, locale) -> {
            String medalName = StaticStore.MEDNAME.getCont(id, locale);

            if (medalName == null || medalName.isBlank()) {
                medalName = "";
            }

            return medalName;
        };

        ArrayList<Integer> filteredMedals = filterData(initialMedals, name, lang, null, false, nameGenerator, idRegexGenerator);

        if (filteredMedals.isEmpty())
            filteredMedals = filterData(initialMedals, name, lang, null, true, nameGenerator, idRegexGenerator);

        return filteredMedals;
    }

    public static ArrayList<Combo> filterComboWithUnit(Form f, String name, CommonStatic.Lang.Locale lang) {
        ArrayList<Combo> initialCombos = new ArrayList<>(UserProfile.getBCData().combos.getList());

        Function<Combo, String> idRegexGenerator = c -> "(" + Data.trio(c.id.id) + "|" + c.id.id + ")";
        Function2<Combo, CommonStatic.Lang.Locale, String> nameGenerator = (c, locale) -> MultiLangCont.getStatic().COMNAME.getCont(c, locale) + " | " + DataToString.getComboType(c, locale);

        ArrayList<Combo> filteredCombos = filterData(initialCombos, name, lang, null, false, nameGenerator, idRegexGenerator);

        if (filteredCombos.isEmpty())
            filteredCombos = filterData(initialCombos, name, lang, null, true, nameGenerator, idRegexGenerator);

        if (f != null)
            filteredCombos.removeIf(c -> {
                System.out.println(f);
                System.out.println(Arrays.stream(c.forms).noneMatch(form -> form.unit.id.id == f.unit.id.id && f.fid >= form.fid));

                return Arrays.stream(c.forms).noneMatch(form -> form.unit.id.id == f.unit.id.id && f.fid >= form.fid);
            });

        return filteredCombos;
    }

    public static List<Integer> findRewardByName(String name, CommonStatic.Lang.Locale lang) {
        ArrayList<Integer> initialRewards = new ArrayList<>(StaticStore.existingRewards);

        Function<Integer, String> idRegexGenerator = id -> "(" + Data.trio(id) + "|" + id + ")";
        Function2<Integer, CommonStatic.Lang.Locale, String> nameGenerator = (id, locale) -> {
            String rewardName = MultiLangCont.getStatic().RWNAME.getCont(StaticStore.existingRewards.get(id), locale);

            if (rewardName == null || rewardName.isBlank())
                rewardName = "";

            return rewardName;
        };

        List<Integer> filteredRewards = filterData(initialRewards, name, lang, null, false, nameGenerator, idRegexGenerator);

        if (filteredRewards.isEmpty())
            filteredRewards = filterData(initialRewards, name, lang, null, true, nameGenerator, idRegexGenerator);

        return filteredRewards;
    }

    public static List<Stage> findStageByReward(int reward, double chance, int amount) {
        List<Stage> result = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            for(StageMap map : mc.maps) {
                if(map == null)
                    continue;

                for(Stage st : map.list) {
                    if(st == null || !(st.info instanceof DefStageInfo) || (((DefStageInfo) st.info).drop == null && ((DefStageInfo) st.info).time == null))
                        continue;

                    if(chance == -1) {
                        boolean added = false;

                        if(((DefStageInfo) st.info).drop != null) {
                            for(int[] data : ((DefStageInfo) st.info).drop) {
                                if(data[1] == reward && (amount == -1 || data[2] >= amount)) {
                                    added = true;

                                    result.add(st);

                                    break;
                                }
                            }
                        }

                        if(added)
                            continue;

                        if(((DefStageInfo) st.info).time != null) {
                            for(int[] data : ((DefStageInfo) st.info).time) {
                                if(data[1] == reward && (amount == -1 || data[2] >= amount)) {
                                    result.add(st);

                                    break;
                                }
                            }
                        }
                    } else {
                        if(((DefStageInfo) st.info).drop == null)
                            continue;

                        List<Double> chances = DataToString.getDropChances(st);

                        if(chances == null) {
                            continue;
                        }

                        if(chances.isEmpty()) {
                            double ch = 100.0 / ((DefStageInfo) st.info).drop.length;

                            for(int i = 0; i < ((DefStageInfo) st.info).drop.length; i++) {
                                int[] data = ((DefStageInfo) st.info).drop[i];

                                if(data[1] == reward && ch >= chance && (amount == -1 || data[2] >= amount)) {
                                    result.add(st);

                                    break;
                                }
                            }
                        } else {
                            for(int i = 0; i < ((DefStageInfo) st.info).drop.length; i++) {
                                int[] data = ((DefStageInfo) st.info).drop[i];

                                if(data[1] == reward && chances.get(i) >= chance && (amount == -1 || data[2] >= amount)) {
                                    result.add(st);

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
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
                    table[i][j] = Math.min(table[i][j], table[i-2][j-2] + 1);
                }
            }
        }

        return table[src.length()][compare.length()];
    }

    /**
     * Get word list by separated per number of words in search keywords<br>
     * <br>
     * For example, if target text is `abc def ghi`, and search keyword was `abc def`, then this
     * method will slice target text by 2 each words. Result will be : <br>
     * {@code
     * { abc def, def ghi }
     * }
     * @param src Target text separated by spaces
     * @param numberOfWords Number of words in search keywords
     * @return Array of sliced words
     */
    private static String[] getWords(String[] src, int numberOfWords) {
        int length = Math.max(1, src.length - numberOfWords + 1);

        String[] result = new String[length];

        if (src.length < numberOfWords) {
            result[0] = String.join(" ", src);
        } else {
            for (int i = 0; i < length; i++) {
                StringBuilder builder = new StringBuilder();

                for (int j = i; j < i + numberOfWords; j++) {
                    builder.append(src[j]);

                    if (j < i + numberOfWords - 1) {
                        builder.append(" ");
                    }
                }

                result[i] = builder.toString();
            }
        }

        return result;
    }

    private static int calculateRawDistance(String src, String target) {
        int distance = Integer.MAX_VALUE;

        int wordNumber = StringUtils.countMatches(src, ' ') + 1;

        for (int i = 1; i <= wordNumber; i++) {
            String[] words;

            if(wordNumber != 1) {
                words = getWords(target.split(" "), i);
            } else {
                words = target.split(" ");
            }

            for (String word : words) {
                distance = Math.min(distance, damerauLevenshteinDistance(word, src));
            }
        }

        distance = Math.min(distance, damerauLevenshteinDistance(target, src));

        return distance;
    }

    private static <T> ArrayList<T> filterData(List<T> preData, String name, CommonStatic.Lang.Locale lang, MultiLangCont<T, ArrayList<String>> aliasData, boolean dynamicMode, Function2<T, CommonStatic.Lang.Locale, String> nameGenerator, Function<T, String> idRegexGenerator) {
        String keyword = name.replaceAll(spaceRegex, " ").replaceAll(apostropheRegex, "'").replaceAll("\\.", "").toLowerCase(Locale.ENGLISH);

        if (dynamicMode && lang == CommonStatic.Lang.Locale.KR) {
            keyword = KoreanSeparater.separate(keyword);
        }

        if (!dynamicMode) {
            ArrayList<T> result = new ArrayList<>();
            ArrayList<T> clear = new ArrayList<>();

            for (int i = 0; i < preData.size(); i++) {
                T data = preData.get(i);

                if (idRegexGenerator != null) {
                    String idRegex = idRegexGenerator.apply(data);

                    if (keyword.matches(idRegex)) {
                        clear.add(data);

                        continue;
                    }
                }

                for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                    String dataName = nameGenerator.invoke(data, locale).replace(spaceRegex, "").replace(apostropheRegex, "'").replace("\\.", "").toLowerCase(Locale.ENGLISH);

                    if (dataName.contains(keyword)) {
                        result.add(data);

                        break;
                    }

                    if (aliasData != null) {
                        boolean added = false;

                        ArrayList<String> aliasList = aliasData.getCont(data, locale);

                        if (aliasList != null && !aliasList.isEmpty()) {
                            for (String a : aliasList) {
                                String alias = a.replaceAll(spaceRegex, " ").replaceAll(apostropheRegex, "'").replaceAll("\\.", "").toLowerCase(Locale.ENGLISH);

                                if (alias.equals(keyword)) {
                                    clear.add(data);

                                    added = true;

                                    break;
                                } else if (alias.contains(keyword)) {
                                    result.add(data);

                                    added = true;

                                    break;
                                }
                            }
                        }

                        if (added)
                            break;
                    }
                }
            }

            if (!clear.isEmpty()) {
                return clear;
            } else {
                return result;
            }
        } else {
            Map<T, Integer> scoreMap = new HashMap<>();

            int minScore = TOLERANCE;

            for (int i = 0; i < preData.size(); i++) {
                T data = preData.get(i);

                String dataName = nameGenerator.invoke(data, lang).replace(spaceRegex, "").replace(apostropheRegex, "'").replace("\\.", "").toLowerCase(Locale.ENGLISH);

                if (lang == CommonStatic.Lang.Locale.KR) {
                    dataName = KoreanSeparater.separate(dataName);
                }

                int score = calculateRawDistance(keyword, dataName);

                if (score <= TOLERANCE) {
                    scoreMap.put(data, score);

                    if (score < minScore) {
                        minScore = score;
                        scoreMap.entrySet().removeIf(e -> e.getValue() > score);
                    }
                } else if (aliasData != null) {
                    ArrayList<String> aliasList = aliasData.getCont(data, lang);

                    if (aliasList != null && !aliasList.isEmpty()) {
                        for (String a : aliasList) {
                            String alias = a.replaceAll(spaceRegex, " ").replaceAll(apostropheRegex, "'").replaceAll("\\.", "").toLowerCase(Locale.ENGLISH);

                            if (lang == CommonStatic.Lang.Locale.KR) {
                                alias = KoreanSeparater.separate(alias);
                            }

                            int aliasScore = calculateRawDistance(keyword, alias);

                            if (aliasScore <= TOLERANCE) {
                                scoreMap.put(data, aliasScore);

                                if (aliasScore < minScore) {
                                    minScore = aliasScore;
                                    scoreMap.entrySet().removeIf(e -> e.getValue() > score);
                                }

                                break;
                            }
                        }
                    }
                }
            }

            int finalMinScore = minScore;

            return new ArrayList<>(scoreMap.entrySet().stream().filter(e -> e.getValue() == finalMinScore).map(Map.Entry::getKey).toList());
        }
    }

    private static boolean containsEnemies(SCDef.Line[] lines, List<Enemy> enemies, boolean hasBoss, boolean or) {
        boolean b = !hasBoss;

        if(!b) {
            for(SCDef.Line l : lines) {
                if(l.boss != 0) {
                    b = true;
                    break;
                }
            }
        }

        boolean c = false;

        for(Enemy e : enemies) {
            boolean contain = false;

            for(SCDef.Line l : lines) {
                if(l.enemy != null && l.enemy.equals(e.id)) {
                    contain = true;
                    break;
                }
            }

            if(!or && !contain)
                return false;
            else if(or && contain) {
                c = true;
                break;
            }
        }

        return b && (!or || c);
    }

    private static boolean isMonthly(MapColc mc, StageMap map) {
        switch (mc.getSID()) {
            case "000003" -> {
                int id = map.id.id;

                for (int i = 0; i < storyChapterMonthly.length; i++) {
                    if (id == storyChapterMonthly[i])
                        return true;
                }

            }
            case "000001" -> {
                int id = map.id.id;

                for (int i = 0; i < cycloneMonthly.length; i++) {
                    if (id == cycloneMonthly[i])
                        return true;
                }

            }
            case "000014" -> {
                int id = map.id.id;

                for (int i = 0; i < cycloneCatamin.length; i++) {
                    if (id == cycloneCatamin[i])
                        return true;
                }

            }
        }

        return mc.getSID().equals("000000");
    }
}
