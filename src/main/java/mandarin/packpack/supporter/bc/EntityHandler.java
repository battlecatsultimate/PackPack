package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.battle.data.MaskUnit;
import common.system.fake.FakeImage;
import common.system.files.VFile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.AbEnemy;
import common.util.unit.EneRand;
import common.util.unit.Enemy;
import common.util.unit.Form;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.awt.FIBI;
import mandarin.packpack.supporter.lang.LangID;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class EntityHandler {
    private static final DecimalFormat df;
    private static Font font;

    static {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);

        df = (DecimalFormat) nf;
        df.applyPattern("#.##");

        File fon = new File("./data/ForceFont.otf");

        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fon).deriveFont(24f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showUnitEmb(Form f, MessageChannel ch, boolean isFrame, boolean talent, int[] lv, int lang) throws Exception {
        int level = lv[0];
        int levelp = 0;

        if(level <= 0) {
            if(f.unit.rarity == 0)
                level = 110;
            else
                level = 30;
        }

        if(level > f.unit.max) {
            levelp = level - f.unit.max;
            level = f.unit.max;

            if(levelp > f.unit.maxp)
                levelp = f.unit.maxp;

            if(levelp < 0)
                levelp = 0;
        }

        lv[0] = level + levelp;

        String l;

        if(levelp == 0)
            l = "" + level;
        else
            l = level + " + " + levelp;

        File img = generateIcon(f);
        File cf = generateCatfruit(f);

        FileInputStream fis;
        FileInputStream cfis;

        if(img != null)
            fis = new FileInputStream(img);
        else
            fis = null;

        if(cf != null)
            cfis = new FileInputStream(cf);
        else
            cfis = null;

        ch.createMessage(m -> {
            m.setEmbed(spec -> {
                Color c;

                if(f.fid == 0)
                    c = StaticStore.rainbow[4];
                else if(f.fid == 1)
                    c = StaticStore.rainbow[3];
                else
                    c = StaticStore.rainbow[2];

                int[] t;

                if(talent && f.getPCoin() != null)
                    t = f.getPCoin().max;
                else
                    t = null;

                if(t != null)
                    t = handleTalent(lv, t);
                else
                    t = new int[] {lv[0], 0, 0, 0, 0, 0};

                if(emptyTalent(t))
                    t = null;

                spec.setTitle(DataToString.getTitle(f, lang));

                if(talent && f.getPCoin() != null && t != null) {
                    spec.setDescription(LangID.getStringByID("data_talent", lang));
                }

                spec.setColor(c);
                spec.setThumbnail("attachment://icon.png");
                spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(f.uid.id, f.fid), false);
                spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(f, lv[0], talent, t), true);
                spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(f, talent, t), true);
                spec.addField(LangID.getStringByID("data_level", lang), l, true);
                spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(f, lv[0], talent, t), false);
                spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(f, lv[0], talent, t), true);
                spec.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(f, isFrame), true);
                spec.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(f, lang), true);
                spec.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(f, isFrame), true);
                spec.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(f, isFrame), true);
                spec.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(f, isFrame), true);
                spec.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(f, talent, t, lang), false);
                spec.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(f, lang), true);
                spec.addField(LangID.getStringByID("data_cost", lang), DataToString.getCost(f, talent, t), true);
                spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(f), true);
                spec.addField(LangID.getStringByID("data_cooldown", lang), DataToString.getCD(f,isFrame, talent, t), true);
                spec.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(f, talent, t), true);

                MaskUnit du;

                if(t != null && f.getPCoin() != null)
                    if(talent)
                        du = f.getPCoin().improve(t);
                    else
                        du = f.du;
                else
                    du = f.du;

                ArrayList<String> abis = Interpret.getAbi(du, lang);
                abis.addAll(Interpret.getProc(du, !isFrame, lang));

                StringBuilder sb = new StringBuilder();

                for(int i = 0; i < abis.size(); i++) {
                    if(i == abis.size() - 1)
                        sb.append(abis.get(i));
                    else
                        sb.append(abis.get(i)).append("\n");
                }

                String res = sb.toString();

                if(res.isBlank())
                    res = LangID.getStringByID("data_none", lang);

                spec.addField(LangID.getStringByID("data_ability", lang), res, false);

                String explanation = DataToString.getDescription(f);

                if(explanation != null)
                    spec.addField(LangID.getStringByID("data_udesc", lang), explanation, false);

                String catfruit = DataToString.getCatruitEvolve(f);

                if(catfruit != null)
                    spec.addField(LangID.getStringByID("data_evolve", lang), catfruit, false);

                spec.setImage("attachment://cf.png");

                if(t != null)
                    spec.setFooter(DataToString.getTalent(f, t, lang), null);
            });
            if(fis != null)
                m.addFile("icon.png", fis);
            if(cfis != null)
                m.addFile("cf.png", cfis);
        }).subscribe(null, null, () -> {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(cfis != null) {
                try {
                    cfis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(img != null && img.exists()) {
                boolean res = img.delete();

                if(!res)
                    System.out.println("Can't delete file : "+img.getAbsolutePath());
            }

            if(cf != null && cf.exists()) {
                boolean res = cf.delete();

                if(!res)
                    System.out.println("Can't delete file : "+cf.getAbsolutePath());
            }
        });

        f.anim.unload();
    }

    private static int[] handleTalent(int[] lv, int[] t) {
        int[] res = new int[6];

        res[0] = lv[0];

        for(int i = 0; i < t.length; i++) {
            if(i >= lv.length)
                res[i] = t[i];
            else
                res[i] = Math.min(t[i], lv[i]);
        }

        return res;
    }

    private static boolean emptyTalent(int[] t) {
        boolean empty = true;

        for(int i = 1; i < t.length; i++) {
            empty &= t[i] == 0;
        }

        return empty;
    }

    public static void showEnemyEmb(Enemy e, MessageChannel ch, boolean isFrame, int[] magnification, int lang) throws Exception {
        File img = generateIcon(e);

        FileInputStream fis;

        if(img != null)
            fis = new FileInputStream(img);
        else
            fis = null;

        ch.createMessage(m -> {
            m.setEmbed(spec -> {
                Color c = StaticStore.rainbow[0];

                int[] mag = new int[2];

                if(magnification.length == 1) {
                    if(magnification[0] <= 0) {
                        mag[0] = mag[1] = 100;
                    } else {
                        mag[0] = mag[1] = magnification[0];
                    }
                } else if(magnification.length == 2) {
                    mag = magnification;

                    if(mag[0] <= 0)
                        mag[0] = 100;

                    if(mag[1] < 0)
                        mag[1] = 0;
                }

                spec.setTitle(DataToString.getTitle(e));
                spec.setColor(c);
                spec.setThumbnail("attachment://icon.png");
                spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(e.id.id), false);
                spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(e, mag[0]), true);
                spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(e), true);
                spec.addField(LangID.getStringByID("data_magnif", lang), DataToString.getMagnification(mag), true);
                spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(e, mag[1]), false);
                spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(e, mag[1]), true);
                spec.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(e, isFrame), true);
                spec.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(e, lang), true);
                spec.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(e, isFrame), true);
                spec.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(e, isFrame), true);
                spec.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(e, isFrame), true);
                spec.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(e, lang), false);
                spec.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(e, lang), true);
                spec.addField(LangID.getStringByID("data_drop", lang), DataToString.getDrop(e), true);
                spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(e), true);
                spec.addField(LangID.getStringByID("data_barrier", lang), DataToString.getBarrier(e, lang), true);
                spec.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(e), true);

                ArrayList<String> abis = Interpret.getAbi(e.de, lang);
                abis.addAll(Interpret.getProc(e.de, !isFrame, lang));

                StringBuilder sb = new StringBuilder();

                for(int i = 0; i < abis.size(); i++) {
                    if(i == abis.size() - 1)
                        sb.append(abis.get(i));
                    else
                        sb.append(abis.get(i)).append("\n");
                }

                String res = sb.toString();

                if(res.isBlank())
                    res = LangID.getStringByID("data_none", lang);

                spec.addField(LangID.getStringByID("data_ability", lang), res, false);

                String explanation = DataToString.getDescription(e);

                if(explanation != null) {
                    spec.addField(LangID.getStringByID("data_edesc", lang), explanation, false);
                }

                spec.setFooter(LangID.getStringByID("enemyst_source", lang), null);
            });
            if(fis != null) {
                m.addFile("icon.png", fis);
            }
        }).subscribe(null, null, () -> {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            if(img != null && img.exists()) {
                boolean res = img.delete();

                if(!res)
                    System.out.println("Can't delete file : "+img.getAbsolutePath());
            }
        });

        e.anim.unload();
    }

    private static File generateIcon(Enemy e) throws IOException {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = new File("./temp/", StaticStore.findFileName(temp, "result", ".png"));

        if(!img.exists()) {
            boolean res = img.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+img.getAbsolutePath());
                return null;
            }
        }

        Object image;

        if(e.anim.getEdi() != null)
            image = e.anim.getEdi().getImg().bimg();
        else
            return null;

        ImageIO.write((BufferedImage) image, "PNG", img);

        return img;
    }

    private static File generateIcon(Form f) throws IOException {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = new File("./temp/", StaticStore.findFileName(temp, "result", ".png"));

        if(!img.exists()) {
            boolean res = img.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+img.getAbsolutePath());
                return null;
            }
        }

        Object image;

        if(f.anim.getUni() != null)
            image = f.anim.getUni().getImg().bimg();
        else if(f.anim.getEdi() != null)
            image = f.anim.getEdi().getImg().bimg();
        else
            return null;

        ImageIO.write((BufferedImage) image, "PNG", img);

        return img;
    }

    private static File generateCatfruit(Form f) throws IOException {
        if(f.unit == null)
            return null;

        if(f.unit.info.evo == null)
            return null;

        File tmp = new File("./temp");

        if(!tmp.exists()) {
            boolean res = tmp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+tmp.getAbsolutePath());
                return null;
            }
        }

        File img = new File("./temp", StaticStore.findFileName(tmp, "result", ".png"));

        if(!img.exists()) {
            boolean res = img.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+img.getAbsolutePath());
                return null;
            }
        }

        BufferedImage image = new BufferedImage(600, 150, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(image.getGraphics());

        g.setFont(font);

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setStroke(2f);
        g.setColor(47, 49, 54, 255);

        g.fillRect(0, 0, 600, 150);

        g.setColor(238, 238, 238, 128);

        g.drawRect(0, 0, 600, 150);

        for(int i = 1; i < 6; i++) {
            g.drawLine(100 * i, 0, 100* i , 150);
        }

        g.drawLine(0, 100, 600, 100);

        g.setColor(238, 238, 238, 255);

        for(int i = 0; i < 6; i++) {
            if(i == 0) {
                VFile vf = VFile.get("./org/page/catfruit/xp.png");

                if(vf != null) {
                    BufferedImage icon = (BufferedImage) vf.getData().getImg().bimg();
                    FakeImage fi = FIBI.build(icon);

                    g.drawImage(fi, 510, 10, 80, 80);
                    g.drawCenteredText(String.valueOf(f.unit.info.evo[i][0]), 550, 125);
                }
            } else {
                if(f.unit.info.evo[i][0] != 0) {
                    VFile vf = VFile.get("./org/page/catfruit/gatyaitemD_"+f.unit.info.evo[i][0]+"_f.png");

                    if(vf != null) {
                        BufferedImage icon = (BufferedImage) vf.getData().getImg().bimg();
                        FakeImage fi = FIBI.build(icon);

                        g.drawImage(fi, 100 * (i-1)+5, 10, 80, 80);
                        g.drawCenteredText(String.valueOf(f.unit.info.evo[i][1]), 100 * (i-1) + 50, 125);
                    }
                }
            }
        }

        ImageIO.write(image, "PNG", img);

        return img;
    }

    public static void showStageEmb(Stage st, MessageChannel ch, boolean isFrame, int star, int lang) throws Exception {
        File img = generateScheme(st, isFrame, lang);
        FileInputStream fis;

        if(img != null) {
            fis = new FileInputStream(img);
        } else {
            fis = null;
        }

        ch.createMessage(m -> {
            m.setEmbed(spec -> {
                try {
                    int sta = Math.min(Math.max(star-1, 0), st.getCont().stars.length-1);

                    if(st.info == null || st.info.diff == -1)
                        spec.setColor(Color.of(217, 217, 217));
                    else
                        spec.setColor(StaticStore.coolHot[st.info.diff]);

                    String name = "";

                    StageMap stm = st.getCont();
                    MapColc mc = stm.getCont();

                    String mcName = MultiLangCont.get(mc);

                    if(mcName == null || mcName.isBlank())
                        mcName = mc.getSID();

                    name += mcName+" - ";

                    String stmName = MultiLangCont.get(stm);

                    if(stmName == null || stmName.isBlank())
                        if(stm.id != null)
                            stmName = Data.trio(stm.id.id);
                        else
                            stmName = "Unknown";

                    name += stmName + " - ";

                    String stName = MultiLangCont.get(st);

                    if(stName == null || stName.isBlank())
                        if(st.id != null)
                            stName = Data.trio(st.id.id);
                        else
                            stName = "Unknown";

                    name += stName;

                    spec.setTitle(name);
                    spec.addField(LangID.getStringByID("data_id", lang), DataToString.getStageCode(st), false);
                    spec.addField(LangID.getStringByID("data_energy", lang), DataToString.getEnergy(st), true);
                    spec.addField(LangID.getStringByID("data_star", lang), DataToString.getStar(st, sta), true);
                    spec.addField(LangID.getStringByID("data_base", lang), DataToString.getBaseHealth(st), true);
                    spec.addField(LangID.getStringByID("data_xp", lang), DataToString.getXP(st), true);
                    spec.addField(LangID.getStringByID("data_diff", lang), DataToString.getDifficulty(st, lang), true);
                    spec.addField(LangID.getStringByID("data_continuable", lang), DataToString.getContinuable(st, lang), true);
                    spec.addField(LangID.getStringByID("data_length", lang), DataToString.getLength(st), true);
                    spec.addField(LangID.getStringByID("data_music", lang), DataToString.getMusic(st, lang), true);
                    spec.addField(DataToString.getMusicChange(st), DataToString.getMusic1(st, lang) , true);
                    spec.addField(LangID.getStringByID("data_maxenem", lang), DataToString.getMaxEnemy(st), true);
                    spec.addField(LangID.getStringByID("data_loop0", lang), DataToString.getLoop0(st), true);
                    spec.addField(LangID.getStringByID("data_loop1", lang), DataToString.getLoop1(st) ,true);
                    spec.addField(LangID.getStringByID("data_bg", lang), DataToString.getBackground(st, lang),true);
                    spec.addField(LangID.getStringByID("data_castle", lang), DataToString.getCastle(st, lang), true);
                    spec.addField(LangID.getStringByID("data_minspawn", lang), DataToString.getMinSpawn(st, isFrame), true);

                    ArrayList<String> limit = DataToString.getLimit(st.getLim(sta), lang);

                    StringBuilder sb = new StringBuilder();

                    if(limit.isEmpty()) {
                        sb.append(LangID.getStringByID("data_none", lang));
                    } else {
                        for(int i = 0; i < limit.size(); i ++) {
                            sb.append(limit.get(i));

                            if(i < limit.size()-1)
                                sb.append("\n");
                        }
                    }

                    spec.addField(LangID.getStringByID("data_limit", lang), sb.toString(), false);

                    String drops = DataToString.getRewards(st, lang);

                    if(drops != null) {
                        if(drops.endsWith("!!number!!")) {
                            spec.addField(LangID.getStringByID("data_numreward", lang), drops.replace("!!number!!", ""), false);
                        } else {
                            spec.addField(LangID.getStringByID("data_chanreward", lang), drops, false);
                        }
                    }

                    String score = DataToString.getScoreDrops(st, lang);

                    if(score != null) {
                        spec.addField(LangID.getStringByID("data_score", lang), score, false);
                    }

                    if(fis != null) {
                        spec.addField(LangID.getStringByID("data_scheme", lang), "** **", false);
                        spec.setImage("attachment://scheme.png");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if(fis != null)
                m.addFile("scheme.png", fis);
        }).subscribe(null, null, () -> {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(img != null && img.exists()) {
                boolean res = img.delete();

                if(!res) {
                    System.out.println("Can't delete file : "+img.getAbsolutePath());
                }
            }
        });
    }

    private static File generateScheme(Stage st, boolean isFrame, int lang) throws Exception {
        File temp = new File("./temp/");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = new File("./temp/", StaticStore.findFileName(temp, "scheme", ".png"));

        if(!img.exists()) {
            boolean res = img.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+img.getAbsolutePath());
                return null;
            }
        }

        Canvas cv = new Canvas();
        FontMetrics fm = cv.getFontMetrics(font);

        ArrayList<String> enemies = new ArrayList<>();
        ArrayList<String> numbers = new ArrayList<>();
        ArrayList<String> magnifs = new ArrayList<>();
        ArrayList<String> isBoss = new ArrayList<>();
        ArrayList<String> baseHealth = new ArrayList<>();
        ArrayList<String> startRespawn = new ArrayList<>();
        ArrayList<String> layers = new ArrayList<>();

        for(int i = 0; i < st.data.datas.length; i++) {
            SCDef.Line line = st.data.datas[i];

            AbEnemy enemy = line.enemy.get();

            if(enemy == null)
                continue;

            if(enemy instanceof Enemy) {
                String eName = MultiLangCont.get(enemy);

                if(eName == null || eName.isBlank())
                    eName = ((Enemy) enemy).name;

                if(eName == null || eName.isBlank())
                    eName = DataToString.getPackName(((Enemy) enemy).id.pack, lang)+" - "+Data.trio(((Enemy) enemy).id.id);

                enemies.add(eName);
            } else if(enemy instanceof EneRand) {
                String name = ((EneRand) enemy).name;

                if(name == null || name.isBlank()) {
                    name = DataToString.getPackName(((EneRand) enemy).id.pack, lang)+"-"+Data.trio(((EneRand) enemy).id.id);
                }

                enemies.add(name);
            } else
                continue;

            String number;

            if(line.number == 0)
                number = LangID.getStringByID("data_infinite", lang);
            else
                number = String.valueOf(line.number);

            numbers.add(number);

            String magnif = DataToString.getMagnification(new int[] {line.multiple, line.mult_atk});

            magnifs.add(magnif);

            String boss;

            if(line.boss == 0)
                boss = "";
            else
                boss = LangID.getStringByID("data_boss", lang);

            isBoss.add(boss);

            String start;

            if(line.spawn_0 == line.spawn_1)
                if(isFrame)
                    start = line.spawn_0+"f";
                else
                    start = df.format(line.spawn_0/30.0)+"s";
            else {
                int minSpawn = Math.min(line.spawn_0, line.spawn_1);
                int maxSpawn = Math.max(line.spawn_0, line.spawn_1);

                if(isFrame)
                    start = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    start = df.format(minSpawn/30.0)+"s ~ "+df.format(maxSpawn/30.0)+"s";
            }

            String respawn;

            if(line.respawn_0 == line.respawn_1)
                if(isFrame)
                    respawn = line.respawn_0+"f";
                else
                    respawn = df.format(line.respawn_0/30.0)+"s";
            else {
                int minSpawn = Math.min(line.respawn_0, line.respawn_1);
                int maxSpawn = Math.max(line.respawn_0, line.respawn_1);

                if(isFrame)
                    respawn = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    respawn = df.format(minSpawn/30.0)+"s ~ "+df.format(maxSpawn/30.0)+"s";
            }

            String startResp = start+" ("+respawn+")";

            startRespawn.add(startResp);

            String baseHP;

            if(line.castle_0 == line.castle_1 || line.castle_1 == 0)
                baseHP = line.castle_0+"%";
            else {
                int minHealth = Math.min(line.castle_0, line.castle_1);
                int maxHealth = Math.max(line.castle_0, line.castle_1);

                baseHP = minHealth + " ~ " + maxHealth + "%";
            }

            baseHealth.add(baseHP);

            String layer;

            if(line.layer_0 != line.layer_1) {
                int minLayer = Math.min(line.layer_0, line.layer_1);
                int maxLayer = Math.max(line.layer_0, line.layer_1);

                layer = minLayer + " ~ " + maxLayer;
            } else {
                layer = String.valueOf(line.layer_0);
            }

            layers.add(layer);
        }

        double eMax = fm.stringWidth(LangID.getStringByID("data_enemy", lang));
        double nMax = fm.stringWidth(LangID.getStringByID("data_number", lang));
        double mMax = fm.stringWidth(LangID.getStringByID("data_magnif", lang));
        double iMax = fm.stringWidth(LangID.getStringByID("data_isboss", lang));
        double bMax = fm.stringWidth(LangID.getStringByID("data_basehealth", lang));
        double sMax = fm.stringWidth(LangID.getStringByID("data_startres", lang));
        double lMax = fm.stringWidth(LangID.getStringByID("data_layer", lang));

        for(int i = 0; i < enemies.size(); i++) {
            eMax = Math.max(eMax, fm.stringWidth(enemies.get(i)));

            nMax = Math.max(nMax, fm.stringWidth(numbers.get(i)));

            mMax = Math.max(mMax, fm.stringWidth(magnifs.get(i)));

            iMax = Math.max(iMax, fm.stringWidth(isBoss.get(i)));

            bMax = Math.max(bMax, fm.stringWidth(baseHealth.get(i)));

            sMax = Math.max(sMax, fm.stringWidth(startRespawn.get(i)));

            lMax = Math.max(lMax, fm.stringWidth(layers.get(i)));
        }

        int xGap = 16;
        int yGap = 10;

        eMax += xGap + 93;
        nMax += xGap;
        mMax += xGap;
        iMax += xGap;
        bMax += xGap;
        sMax += xGap;
        lMax += xGap;

        int ySeg = Math.max(fm.getHeight() + yGap, 32 + yGap);

        int w = (int) (eMax + nMax + mMax + iMax + bMax + sMax + lMax);
        int h = ySeg * (enemies.size() + 1);

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(image.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setFont(font);

        g.setColor(47, 49, 54, 255);
        g.fillRect(0, 0, w, h);

        g.setColor(54, 57, 63, 255);
        g.fillRect(0, 0, w, ySeg);

        g.setColor(32, 34, 37, 255);
        g.setStroke(4f);

        g.drawRect(0, 0, w, h);

        g.setStroke(2f);

        for(int i = 1; i < enemies.size() + 1; i++) {
            g.drawLine(0, ySeg * i, w, ySeg * i);
        }

        int x = (int) eMax;

        g.drawLine(x, 0, x, h);

        x += (int) nMax;

        g.drawLine(x, 0, x, h);

        x += (int) bMax;

        g.drawLine(x, 0, x, h);

        x += (int) mMax;

        g.drawLine(x, 0, x, h);

        x += (int) sMax;

        g.drawLine(x, 0, x, h);

        x += (int) lMax;

        g.drawLine(x, 0, x, h);

        x += (int) iMax;

        g.drawLine(x, 0, x, h);

        g.setColor(238, 238, 238, 255);

        int initX = (int) (eMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_enemy", lang), initX, ySeg / 2);

        initX += (int) (eMax / 2 + nMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_number", lang), initX, ySeg / 2);

        initX += (int) (nMax / 2 + bMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_basehealth", lang), initX, ySeg / 2);

        initX += (int) (bMax / 2 + mMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_magnif", lang), initX, ySeg / 2);

        initX += (int) (mMax / 2 + sMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_startres", lang), initX, ySeg / 2);

        initX += (int) (sMax / 2 + lMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_layer", lang), initX, ySeg / 2);

        initX += (int) (lMax / 2 + iMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_isboss", lang), initX, ySeg / 2);

        for(int i = 0; i < enemies.size(); i++) {
            AbEnemy e = st.data.datas[i].enemy.get();

            if(e != null) {
                BufferedImage edi;

                if(e instanceof Enemy) {
                    if(((Enemy) e).anim.getEdi() != null) {
                        edi = (BufferedImage) ((Enemy) e).anim.getEdi().getImg().bimg();
                    } else {
                        edi = (BufferedImage) CommonStatic.getBCAssets().ico[0][0].getImg().bimg();
                    }
                } else {
                    edi = (BufferedImage) CommonStatic.getBCAssets().ico[0][0].getImg().bimg();
                }

                FakeImage fi = FIBI.build(edi);

                g.drawImage(fi, xGap / 2.0, ySeg * (i + 1) + yGap / 2.0);
            } else
                continue;

            int px = 93 + xGap / 2;
            int py = ySeg * (i + 2) - ySeg / 2;

            g.drawVerticalCenteredText(enemies.get(i), px, py);

            px = (int) eMax + xGap / 2;

            g.drawVerticalCenteredText(numbers.get(i), px, py);

            px += (int) nMax;

            g.drawVerticalCenteredText(baseHealth.get(i), px, py);

            px += (int) bMax;

            g.drawVerticalCenteredText(magnifs.get(i), px, py);

            px += (int) mMax;

            g.drawVerticalCenteredText(startRespawn.get(i), px, py);

            px += (int) sMax;

            g.drawVerticalCenteredText(layers.get(i), px, py);

            px += (int) lMax;

            g.drawVerticalCenteredText(isBoss.get(i), px, py);
        }

        ImageIO.write(image, "PNG", img);

        return img;
    }
}
