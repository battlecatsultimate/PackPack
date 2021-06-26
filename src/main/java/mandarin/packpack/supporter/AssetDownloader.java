package mandarin.packpack.supporter;

import com.google.gson.JsonParser;
import common.CommonStatic;
import common.io.assets.AssetLoader;
import common.io.assets.UpdateCheck;
import common.pack.PackData;
import common.pack.UserProfile;
import common.system.fake.ImageBuilder;
import common.system.files.VFile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import common.util.unit.Unit;
import mandarin.packpack.supporter.awt.PCIB;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class AssetDownloader {
    private static final String[] folder = {"bot/", "jp/", "kr/", "zh/", "fr/", "it/", "es/", "de/"};
    private static final String[] file = {"EnemyName.txt", "StageName.txt", "UnitName.txt", "UnitExplanation.txt", "EnemyExplanation.txt", "CatFruitExplanation.txt", "RewardName.txt", "ComboName.txt", "MedalName.txt", "MedalExplanation.txt"};
    private static final String[] extra = {"fr/StageName.txt", "it/StageName.txt", "es/StageName.txt", "de/StageName.txt"};

    public static void checkAssetDownload() {
        try {
            UpdateCheck.UpdateJson json = UpdateCheck.checkUpdate();

            if(json == null) {
                System.out.println("Failed to check update");
                return;
            }

            List<UpdateCheck.Downloader> asset, music, lang;
            List<String> langFile = new ArrayList<>();

            langFile.add("Difficulty.txt");
            CommonStatic.getConfig().localLangMap.put("Difficulty.txt", StaticStore.langs.getOrDefault("Difficulty.txt", ""));

            for(int i = 0; i < 4; i++) {
                String f = folder[i];

                for(String fi : file) {
                    langFile.add(f + fi);
                    CommonStatic.getConfig().localLangMap.put(f+fi, StaticStore.langs.getOrDefault(f+fi, ""));
                }
            }

            for(String e : extra) {
                langFile.add(e);
                CommonStatic.getConfig().localLangMap.put(e, StaticStore.langs.getOrDefault(e, ""));
            }

            asset = UpdateCheck.checkAsset(json, "android", "bot");
            music = UpdateCheck.checkMusic(json.music);
            lang = UpdateCheck.checkLang(langFile.toArray(new String[0])).get();

            if(!asset.isEmpty()) {
                StringBuilder builder = new StringBuilder("\n--------------------").append("\nNew Asset Update Found\n\n");

                for(UpdateCheck.Downloader d : asset) {
                    builder.append(d.target.getName()).append("\n");
                }

                builder.append("--------------------\n");

                System.out.println(builder);
            }

            for(UpdateCheck.Downloader d : asset) {
                System.out.println("Downloading File : "+d.target.getName());
                d.run((v) -> {});
            }

            AssetLoader.merge();

            if(!music.isEmpty()) {
                System.out.println("\n--------------------\nNew Music Update Found\n--------------------\n");
            }

            for(UpdateCheck.Downloader d : music) {
                System.out.println("Downloading Music : "+d.target.getName());
                d.run((v) -> {});
            }

            if(!lang.isEmpty()) {
                System.out.println("\n--------------------\nNew Lang Update Found\n--------------------\n");
            }

            for(UpdateCheck.Downloader d : lang) {

                System.out.println("Downloading Language File : "+d.target.getName());
                d.run((v) -> {});
            }

            for(String key : CommonStatic.getConfig().localLangMap.keySet()) {
                StaticStore.langs.put(key, CommonStatic.getConfig().localLangMap.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            define();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void define() {
        ImageBuilder.builder = new PCIB();
        System.out.println("Initializing Profile...");
        CommonStatic.ctx.initProfile();
        System.out.println("Loading Assets...");
        AssetLoader.load((v) -> {});
        System.out.println("Loading BC Data...");
        UserProfile.getBCData().load(System.out::println, (v) -> {});
        System.out.println("Reading Language Data...");

        MultiLangCont.getStatic().FNAME.clear();
        MultiLangCont.getStatic().FEXP.clear();
        MultiLangCont.getStatic().CFEXP.clear();
        MultiLangCont.getStatic().COMNAME.clear();
        MultiLangCont.getStatic().ENAME.clear();
        MultiLangCont.getStatic().EEXP.clear();
        MultiLangCont.getStatic().MCNAME.clear();
        MultiLangCont.getStatic().SMNAME.clear();
        MultiLangCont.getStatic().STNAME.clear();
        MultiLangCont.getStatic().RWNAME.clear();
        StaticStore.MEDNAME.clear();
        StaticStore.MEDEXP.clear();

        PackData.DefPack def = UserProfile.getBCData();

        for(String fo : folder) {
            String f;

            if(fo.equals("bot/"))
                f = "en";
            else
                f = fo.substring(0, fo.length()-1);

            for(String fi : file) {
                File g = new File("./data/assets/lang/"+fo+fi);

                if(g.exists()) {
                    VFile vf = VFile.getFile(g);

                    if(vf == null)
                        continue;

                    Queue<String> qs = vf.getData().readLine();

                    switch (fi) {
                        case "UnitName.txt":
                            int size = qs.size();
                            int i = 0;

                            while(i < size) {
                                String line = qs.poll();

                                if (line == null) {
                                    i++;
                                    continue;
                                }

                                String[] str = line.trim().split("\t");

                                Unit u = def.units.get(CommonStatic.parseIntN(str[0]));

                                if(u == null) {
                                    i++;
                                    continue;
                                }

                                int j = 0;

                                while(j < Math.min(str.length-1, u.forms.length)) {
                                    MultiLangCont.getStatic().FNAME.put(f, u.forms[j], str[j+1].trim());
                                    j++;
                                }

                                i++;
                            }
                            break;
                        case "UnitExplanation.txt":
                            size = qs.size();
                            i = 0;
                            while (i < size) {
                                String line = qs.poll();

                                if(line == null) {
                                    i++;
                                    continue;
                                }

                                String[] str = line.trim().split("\t");

                                Unit u = def.units.get(CommonStatic.parseIntN(str[0]));

                                int j = 0;

                                while(j < Math.min(u.forms.length, str.length-1)) {
                                    String[] lines = str[j+1].trim().split("<br>");
                                    MultiLangCont.getStatic().FEXP.put(f, u.forms[j], lines);
                                    j++;
                                }

                                i++;
                            }
                            break;
                        case "CatFruitExplanation.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                Unit u = def.units.get(CommonStatic.parseIntN(str[0]));

                                if(u == null)
                                    continue;

                                String[] lines = str[1].split("<br>");

                                MultiLangCont.getStatic().CFEXP.put(f, u.info, lines);
                            }
                            break;
                        case "ComboName.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                if(str.length <= 1)
                                    continue;

                                int id = Integer.parseInt(str[0].trim());
                                String name = str[1].trim();

                                MultiLangCont.getStatic().COMNAME.put(f, id, name);
                            }
                            break;
                        case "EnemyName.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                Enemy e = def.enemies.get(CommonStatic.parseIntN(str[0]));

                                if(e == null)
                                    continue;

                                if(str.length == 1)
                                    continue;

                                String name;

                                if(str[1].trim().startsWith("\u3010")) {
                                    name = str[1].trim().substring(1, str[1].trim().length()-1);
                                } else {
                                    name = str[1].trim();
                                }

                                MultiLangCont.getStatic().ENAME.put(f, e, name);
                            }
                            break;
                        case "EnemyExplanation.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                Enemy e = def.enemies.get(CommonStatic.parseIntN(str[0]));

                                if(str.length == 1)
                                    continue;

                                String[] lines = str[1].trim().split("<br>");

                                MultiLangCont.getStatic().EEXP.put(f, e, lines);
                            }
                            break;
                        case "StageName.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                if(str.length == 1)
                                    continue;

                                String id = str[0].trim();
                                String name = str[str.length - 1].trim();

                                if(id.isBlank() || name.isBlank())
                                    continue;

                                String[] ids = id.trim().split("-");

                                int id0 = CommonStatic.parseIntN(ids[0].trim());

                                MapColc mc = MapColc.get(Data.hex(id0));

                                if(mc == null)
                                    continue;

                                if(ids.length == 1) {
                                    MultiLangCont.getStatic().MCNAME.put(f, mc, name);
                                    continue;
                                }

                                int id1 = CommonStatic.parseIntN(ids[1].trim());

                                if(id1 >= mc.maps.getList().size() || id1 < 0)
                                    continue;

                                StageMap stm = mc.maps.getList().get(id1);

                                if(ids.length == 2) {
                                    MultiLangCont.getStatic().SMNAME.put(f, stm, name);
                                    continue;
                                }

                                int id2 = CommonStatic.parseIntN(ids[2].trim());

                                if(id2 >= stm.list.getList().size() || id2 < 0)
                                    continue;

                                Stage st = stm.list.getList().get(id2);

                                MultiLangCont.getStatic().STNAME.put(f, st, name);
                            }
                            break;
                        case "RewardName.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                if(str.length <= 1)
                                    continue;

                                String id = str[0].trim();
                                String name = str[1].trim();

                                MultiLangCont.getStatic().RWNAME.put(f, Integer.parseInt(id), name);
                            }
                            break;
                        case "MedalName.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                if(str.length == 1)
                                    continue;

                                int id = Integer.parseInt(str[0].trim());
                                String name = str[1].trim();

                                StaticStore.MEDNAME.put(f, id, name);
                            }
                            break;
                        case "MedalExplanation.txt":
                            for(String line : qs) {
                                if(line == null)
                                    continue;

                                String[] str = line.trim().split("\t");

                                if(str.length == 1)
                                    continue;

                                int id = Integer.parseInt(str[0].trim());
                                String name = str[1].trim();

                                StaticStore.MEDEXP.put(f, id, name);
                            }
                    }
                }
            }
        }

        int i = 0;

        while(true) {
            VFile vf = VFile.get("./org/page/medal/medal_"+Data.trio(i)+".png");

            if(vf != null) {
                i++;
            } else {
                break;
            }
        }

        StaticStore.medalNumber = i;

        VFile medalJson = VFile.get("./org/data/medallist.json");

        if(medalJson != null) {
            String json = new String(medalJson.getData().getBytes(), StandardCharsets.UTF_8);

            StaticStore.medalData = JsonParser.parseString(json).getAsJsonObject().get("iconID");
        }

        File g = new File("./data/assets/lang/Difficulty.txt");

        if(g.exists()) {
            VFile vf = VFile.getFile(g);

            if(vf != null) {
                Queue<String> qs = vf.getData().readLine();

                for(String line : qs) {
                    if(line == null)
                        continue;

                    String[] str = line.trim().split("\t");

                    if(str.length < 2)
                        continue;

                    String num = str[1].trim();
                    String[] numbers = str[0].trim().split("-");

                    if(numbers.length < 3)
                        continue;

                    int id0 = CommonStatic.parseIntN(numbers[0].trim());
                    int id1 = CommonStatic.parseIntN(numbers[1].trim());
                    int id2 = CommonStatic.parseIntN(numbers[2].trim());

                    MapColc mc = MapColc.get(Data.hex(id0));

                    if(mc == null || id1 >= mc.maps.getList().size() || id1 < 0)
                        continue;

                    StageMap stm = mc.maps.getList().get(id1);

                    if(stm == null || id2 >= stm.list.getList().size() || id2 < 0)
                        continue;

                    Stage st = stm.list.getList().get(id2);

                    if(st.info != null) {
                        st.info.diff = Integer.parseInt(num);
                    }
                }
            }
        }
    }
}
