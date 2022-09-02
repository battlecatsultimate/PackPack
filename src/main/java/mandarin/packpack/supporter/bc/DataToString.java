package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.battle.BasisSet;
import common.battle.Treasure;
import common.battle.data.*;
import common.pack.Identifier;
import common.pack.PackData;
import common.pack.UserProfile;
import common.system.files.VFile;
import common.util.Data;
import common.util.lang.Formatter;
import common.util.lang.MultiLangCont;
import common.util.lang.ProcLang;
import common.util.stage.*;
import common.util.stage.info.DefStageInfo;
import common.util.unit.*;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class DataToString extends Data {
    private static final Map<Integer, String> talentText = new HashMap<>();
    private static final Map<Integer, String> talentIcon = new HashMap<>();
    public static final DecimalFormat df;
    private static final List<String> mapIds = Arrays.asList("000000", "000001", "000002", "000003", "000004", "000006", "000007", "000011", "000012", "000013", "000014", "000024", "000025", "000027", "000031");
    private static final String[] mapCodes = {"N", "S", "C", "CH", "E", "T", "V", "R", "M", "A", "B", "RA", "H", "CA", "Q"};
    private static final Map<Integer, int[]> talentLevel = new HashMap<>();
    private static final int maxDifficulty = 11;

    static {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        df = (DecimalFormat) nf;
        df.applyPattern("#.##");
    }

    public static void initialize() {
        addTalentData(0, "??", "");
        addTalentData(1, "data_weaken", "WEAK");
        addTalentData(2, "data_freeze", "STOP");
        addTalentData(3, "data_slow", "SLOW");
        addTalentData(4, "data_attackon", "data_attackon");
        addTalentData(5, "data_strong", "data_strong");
        addTalentData(6, "data_resistant", "data_resistant");
        addTalentData(7, "data_massive", "data_massive");
        addTalentData(8, "data_knockback", "KB");
        addTalentData(9, "data_warp", "WARP");
        addTalentData(10, "data_strength", "STRONG");
        addTalentData(11, "data_survive", "LETHAL");
        addTalentData(12, "data_basedest", "data_basedest");
        addTalentData(13, "data_critical", "CRIT");
        addTalentData(14, "data_zombiekill", "data_zombiekill");
        addTalentData(15, "data_barrierbreak", "BREAK");
        addTalentData(16, "data_extramon", "BOUNTY");
        addTalentData(17, "data_wave", "WAVE");
        addTalentData(18, "data_resweak", "RESWEAK");
        addTalentData(19, "data_resfreeze", "RESSTOP");
        addTalentData(20, "data_resslow", "RESSLOW");
        addTalentData(21, "data_reskb", "RESKB");
        addTalentData(22, "data_reswave", "RESWAVE");
        addTalentData(23, "data_waveshie", "data_waveshie");
        addTalentData(24, "data_reswarp", "RESWARP");
        addTalentData(25, "data_cost", "COSTDEC");
        addTalentData(26, "data_cooldown", "CDDEC");
        addTalentData(27, "data_speed", "SPEEDUP");
        addTalentData(28, "??", "");
        addTalentData(29, "data_imucurse", "IMUCURSE");
        addTalentData(30, "data_rescurse", "RESCURSE");
        addTalentData(31, "data_atk", "ATKUP");
        addTalentData(32, "data_hp", "HPUP");
        addTalentData(33, "data_red", "T_RED");
        addTalentData(34, "data_float", "T_FLOAT");
        addTalentData(35, "data_black", "T_BLACK");
        addTalentData(36, "data_metal", "T_METAL");
        addTalentData(37, "data_angel", "T_ANGEL");
        addTalentData(38, "data_alien", "T_ALIEN");
        addTalentData(39, "data_zombie", "T_ZOMBIE");
        addTalentData(40, "data_relic", "T_RELIC");
        addTalentData(41, "data_white", "T_WHITE");
        addTalentData(42, "??", "");
        addTalentData(43, "??", "");
        addTalentData(44, "data_imuweak", "IMUWEAK");
        addTalentData(45, "data_imufreeze", "IMUSTOP");
        addTalentData(46, "data_imuslow", "IMUSLOW");
        addTalentData(47, "data_imukb", "IMUKB");
        addTalentData(48, "data_imuwave", "IMUWAVE");
        addTalentData(49, "data_imuwarp", "IMUWARP");
        addTalentData(50, "data_savage", "SATK");
        addTalentData(51, "data_invinci", "IMUATK");
        addTalentData(52, "data_respoison", "RESPOIATK");
        addTalentData(53, "data_imupoison", "IMUPOIATK");
        addTalentData(54, "data_ressurge", "RESVOLC");
        addTalentData(55, "data_imusurge", "IMUVOLC");
        addTalentData(56, "data_surge", "VOLC");
        addTalentData(57, "data_demon", "T_AKU");
        addTalentData(58, "data_shieldbreak", "SHIELDBREAK");
        addTalentData(59, "data_corpsekiller", "data_corpsekiller");
        addTalentData(60, "data_curse", "CURSE");

        VFile pCoinLevel = VFile.get("./org/data/SkillLevel.csv");

        if(pCoinLevel != null) {
            Queue<String> qs = pCoinLevel.getData().readLine();

            qs.poll();

            String line;

            while((line = qs.poll()) != null) {
                int[] values = CommonStatic.parseIntsN(line);

                if(values.length < 2)
                    continue;

                int id = values[0];

                int[] costs = new int[values.length - 1];

                System.arraycopy(values, 1, costs, 0, values.length - 1);

                talentLevel.put(id, costs);
            }
        }
    }

    private static void addTalentData(int id, String name, String icon) {
        talentText.put(id, name);
        talentIcon.put(id, icon);
    }

    public static String getTitle(Form f, int lang) {
        if(f == null)
            return "";

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String name = MultiLangCont.get(f);

        CommonStatic.getConfig().lang = oldConfig;

        if(name == null)
            name = "";

        String rarity;

        if(f.unit.rarity == 0)
            rarity = LangID.getStringByID("data_basic", lang);
        else if(f.unit.rarity == 1)
            rarity = LangID.getStringByID("data_ex", lang);
        else if(f.unit.rarity == 2)
            rarity = LangID.getStringByID("data_rare", lang);
        else if(f.unit.rarity == 3)
            rarity = LangID.getStringByID("data_sr", lang);
        else if(f.unit.rarity == 4)
            rarity = LangID.getStringByID("data_ur", lang);
        else if(f.unit.rarity == 5)
            rarity = LangID.getStringByID("data_lr", lang);
        else
            rarity = "Unknown";

        if(name.isBlank()) {
            return rarity;
        } else {
            return rarity + " - " + name;
        }
    }

    public static String getTitle(Enemy e, int lang) {
        if(e == null)
            return "";

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        if(MultiLangCont.get(e) == null) {
            CommonStatic.getConfig().lang = oldConfig;

            return Data.trio(e.id.id);
        } else {
            String res = MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            return res;
        }
    }

    public static String getAtkTime(MaskUnit f, boolean isFrame) {
        if(f == null)
            return "";

        if(isFrame) {
            return f.getItv()+"f";
        } else {
            return df.format(f.getItv()/30.0)+"s";
        }
    }

    public static String getAtkTime(MaskEnemy e, boolean isFrame) {
        if(e == null)
            return "";

        if(isFrame) {
            return e.getItv()+"f";
        } else {
            return df.format(e.getItv()/30.0)+"s";
        }
    }

    public static String getAbilT(MaskUnit f, int lang) {
        if(f == null)
            return "";

        int[][] raw = f.rawAtkData();

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < raw.length; i++) {
            if(raw[i][2] == 1)
                sb.append(LangID.getStringByID("data_true", lang));
            else
                sb.append(LangID.getStringByID("data_false", lang));

            if(i != raw.length-1) {
                sb.append(" / ");
            }
        }

        return sb.toString();
    }

    public static String getAbilT(MaskEnemy e, int lang) {
        if(e == null)
            return "";

        int[][] raw = e.rawAtkData();

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < raw.length; i++) {
            if(raw[i][2] == 1)
                sb.append(LangID.getStringByID("data_true",lang));
            else
                sb.append(LangID.getStringByID("data_false", lang));

            if(i != raw.length - 1)
                sb.append(" / ");
        }

        return sb.toString();
    }

    public static String getPost(MaskUnit f, boolean isFrame) {
        if(f == null)
            return "";

        if(isFrame) {
            return f.getPost()+"f";
        } else {
            return df.format(f.getPost()/30.0)+"s";
        }
    }

    public static String getPost(MaskEnemy e, boolean isFrame) {
        if(e == null)
            return "";

        if(isFrame) {
            return e.getPost()+"f";
        } else {
            return df.format(e.getPost()/30.0)+"s";
        }
    }

    public static String getTBA(MaskUnit f, boolean isFrame) {
        if(f == null)
            return "";

        if(isFrame) {
            return f.getTBA()+"f";
        } else {
            return df.format(f.getTBA()/30.0)+"s";
        }
    }

    public static String getTBA(MaskEnemy e, boolean isFrame) {
        if(e == null)
            return "";

        if(isFrame) {
            return e.getTBA()+"f";
        } else {
            return df.format(e.getTBA()/30.0)+"s";
        }
    }

    public static String getPre(MaskUnit f, boolean isFrame) {
        if(f == null)
            return "";

        int[][] raw = f.rawAtkData();

        if(isFrame) {
            if(raw.length > 1) {
                StringBuilder sb = new StringBuilder();

                for(int i = 0; i < raw.length; i++) {
                    if(i != raw.length -1)
                        sb.append(raw[i][1]).append("f / ");
                    else
                        sb.append(raw[i][1]).append("f");
                }

                return sb.toString();
            } else {
                return raw[0][1]+"f";
            }
        } else {
            if(raw.length > 1) {
                StringBuilder sb = new StringBuilder();

                for(int i = 0; i < raw.length; i++) {
                    if(i != raw.length-1)
                        sb.append(df.format(raw[i][1]/30.0)).append("s / ");
                    else
                        sb.append(df.format(raw[i][1]/30.0)).append("s");
                }

                return sb.toString();
            } else {
                return df.format(raw[0][1]/30.0)+"s";
            }
        }
    }

    public static String getPre(MaskEnemy e, boolean isFrame) {
        if(e == null)
            return "";

        int[][] raw = e.rawAtkData();

        if(isFrame) {
            if(raw.length > 1) {
                StringBuilder sb = new StringBuilder();

                for(int i = 0; i < raw.length; i++) {
                    if(i != raw.length-1)
                        sb.append(raw[i][1]).append("f / ");
                    else
                        sb.append(raw[i][1]).append("f");
                }

                return sb.toString();
            } else {
                return raw[0][1]+"f";
            }
        } else {
            if(raw.length > 1) {
                StringBuilder sb = new StringBuilder();

                for(int i = 0; i < raw.length; i++) {
                    if(i != raw.length - 1)
                        sb.append(df.format(raw[i][1]/30.0)).append("s / ");
                    else
                        sb.append(df.format(raw[i][1]/30.0)).append("s");
                }

                return sb.toString();
            } else {
                return df.format(raw[0][1]/30.0)+"s";
            }
        }
    }

    public static String getID(int uid, int fid) {
        return Data.trio(uid)+" - "+Data.trio(fid);
    }

    public static String getID(int eid) {
        return Data.trio(eid);
    }

    public static String getRange(MaskUnit f) {
        if(f == null)
            return "";

        int r = f.getRange();

        if(!f.isLD() && !f.isOmni())
            return r + "";

        if(f.getAtkCount() == 0 || allRangeSame(f)) {
            MaskAtk ma = f.getAtkModel(0);

            int lds = ma.getShortPoint();
            int ldr = ma.getLongPoint() - ma.getShortPoint();

            int start = Math.min(lds, lds + ldr);
            int end = Math.max(lds, lds + ldr);

            return r + " | " + start + "~" + end;
        } else {
            StringBuilder builder = new StringBuilder()
                    .append(r)
                    .append(" | ");

            for(int i = 0; i < f.getAtkCount(); i++) {
                MaskAtk ma = f.getAtkModel(i);

                int lds = ma.getShortPoint();
                int ldr = ma.getLongPoint() - ma.getShortPoint();

                int start = Math.min(lds, lds + ldr);
                int end = Math.max(lds, lds + ldr);

                builder.append(start)
                        .append("~")
                        .append(end);

                if(i < f.getAtkCount() - 1) {
                    builder.append(" / ");
                }
            }

            return builder.toString();
        }
    }

    public static String getRange(MaskEnemy e) {
        if(e == null)
            return "";

        int r = e.getRange();

        if(!e.isLD() && !e.isOmni())
            return r + "";

        if(e.getAtkCount() == 0 || allRangeSame(e)) {
            MaskAtk atk = e.getAtkModel(0);

            int lds = atk.getShortPoint();
            int ldr = atk.getLongPoint() - atk.getShortPoint();

            int start = Math.min(lds, lds+ldr);
            int end = Math.max(lds, lds+ldr);

            return r + " | "+start+"~"+end;
        } else {
            StringBuilder builder = new StringBuilder()
                    .append(r)
                    .append(" | ");

            for(int i = 0; i < e.getAtkCount(); i++) {
                MaskAtk atk = e.getAtkModel(i);

                int lds = atk.getShortPoint();
                int ldr = atk.getLongPoint() - atk.getShortPoint();

                int start = Math.min(lds, lds+ldr);
                int end = Math.max(lds, lds+ldr);

                builder.append(start)
                        .append("~")
                        .append(end);

                if(i < e.getAtkCount() - 1) {
                    builder.append(" / ");
                }
            }

            return builder.toString();
        }
    }

    private static boolean allRangeSame(MaskEntity du) {
        ArrayList<Integer> near = new ArrayList<>();
        ArrayList<Integer> far = new ArrayList<>();

        for(MaskAtk atk : du.getAtks()) {
            near.add(atk.getShortPoint());
            far.add(atk.getLongPoint());
        }

        if(near.isEmpty())
            return true;

        for(int i : near) {
            if(i != near.get(0))
                return false;
        }

        for(int f : far) {
            if(f != far.get(0))
                return false;
        }

        return true;
    }

    public static String getCD(MaskUnit f, boolean isFrame, boolean talent, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null) {
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f;
        } else
            du = f;

        if(isFrame) {
            return BasisSet.current().t().getFinRes(du.getRespawn())+"f";
        } else {
            return df.format(BasisSet.current().t().getFinRes(du.getRespawn())/30.0)+"s";
        }
    }

    public static String getAtk(MaskUnit f, UnitLevel lv, boolean talent, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(f.getPCoin() != null && talent)
            du = f.getPCoin().improve(lvs);
        else
            du = f;

        if(du.rawAtkData().length > 1)
            return getTotalAtk(lv, du, talent, lvs) + " " + getAtks(lv, du, talent, lvs);
        else
            return getTotalAtk(lv, du, talent, lvs);
    }

    public static String getAtk(MaskEnemy e, int magnification) {
        if(e == null)
            return "";

        if(e.rawAtkData().length > 1)
            return getTotalAtk(e, magnification)+" " +getAtks(e, magnification);
        else
            return getTotalAtk(e, magnification);
    }

    public static String getTotalAtk(UnitLevel lv, MaskUnit du, boolean talent, ArrayList<Integer> lvs) {
        Treasure t = BasisSet.current().t();

        int result = 0;

        int[][] raw = du.rawAtkData();

        for(int[] atk : raw) {
            if(du.getPCoin() != null && talent) {
                result += (int) ((int) (Math.round(atk[0] * lv.getMult(lvs.get(0))) * t.getAtkMulti()) * du.getPCoin().getAtkMultiplication(lvs));
            } else {
                result += (int) (Math.round(atk[0] * lv.getMult(lvs.get(0))) * t.getAtkMulti());
            }
        }

        return String.valueOf(result);
    }

    public static String getTotalAtk(MaskEnemy e, int magnification) {
        if(e == null)
            return "";

        int[][] atks = e.rawAtkData();

        int result = 0;

        for(int[] atk : atks) {
            result += (int) (atk[0] * e.multi(BasisSet.current()) * magnification / 100.0);
        }

        return String.valueOf(result);
    }

    public static String getAtks(UnitLevel lv, MaskUnit du, boolean talent, ArrayList<Integer> lvs) {
        if(du == null)
            return "";

        int[][] raw = du.rawAtkData();

        Treasure t = BasisSet.current().t();

        ArrayList<Integer> damage = new ArrayList<>();

        for(int[] atk : raw) {
            int result;

            if(du.getPCoin() != null && talent) {
                result = (int) ((int) (Math.round(atk[0] * lv.getMult(lvs.get(0))) * t.getAtkMulti()) * du.getPCoin().getAtkMultiplication(lvs));
            } else {
                result = (int) (Math.round(atk[0] * lv.getMult(lvs.get(0))) * t.getAtkMulti());
            }

            damage.add(result);
        }

        StringBuilder result = new StringBuilder("(");

        for(int i = 0; i < damage.size(); i++) {
            if(i < damage.size() -1)
                result.append(damage.get(i)).append(", ");
            else
                result.append(damage.get(i)).append(")");
        }

        return result.toString();
    }

    public static String getAtks(MaskEnemy e, int magnification) {
        if(e == null)
            return "";

        int[][] atks = e.rawAtkData();

        ArrayList<Integer> damages = new ArrayList<>();

        for(int[] atk : atks) {
            damages.add((int) (atk[0] * e.multi(BasisSet.current()) * magnification / 100.0));
        }

        StringBuilder sb = new StringBuilder("(");

        for(int i = 0; i < damages.size(); i++) {
            if(i < damages.size() - 1)
                sb.append(damages.get(i)).append(", ");
            else
                sb.append(damages.get(i)).append(")");
        }

        return sb.toString();
    }

    public static String getDPS(MaskUnit f, UnitLevel lv, boolean talent, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f;
        else
            du = f;

        return df.format(Double.parseDouble(getTotalAtk(lv, du, talent, lvs)) / (du.getItv() / 30.0));
    }

    public static String getDPS(MaskEnemy e, int magnification) {
        if(e == null)
            return "";

        return df.format(Double.parseDouble(getTotalAtk(e, magnification)) / (e.getItv() / 30.0));
    }

    public static String getSpeed(MaskUnit f, boolean talent, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f;
        else
            du = f;

        return String.valueOf(du.getSpeed());
    }

    public static String getSpeed(MaskEnemy e) {
        if(e == null)
            return "";

        return String.valueOf(e.getSpeed());
    }

    public static String getHitback(MaskUnit f, boolean talent, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f;
        else
            du = f;

        return String.valueOf(du.getHb());
    }

    public static String getHitback(MaskEnemy e) {
        if(e == null)
            return "";

        return String.valueOf(e.getHb());
    }

    public static String getHP(MaskUnit f, UnitLevel lv, boolean talent, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(f.getPCoin() != null && talent)
            du = f.getPCoin().improve(lvs);
        else
            du = f;

        Treasure t = BasisSet.current().t();

        int result;

        if(f.getPCoin() != null && talent) {
            result = (int) ((int) (Math.round(du.getHp() * lv.getMult(lvs.get(0))) * t.getDefMulti()) * f.getPCoin().getHPMultiplication(lvs));
        } else {
            result = (int) (Math.round(du.getHp() * lv.getMult(lvs.get(0))) * t.getDefMulti());
        }

        return String.valueOf(result);
    }

    public static String getHP(MaskEnemy e, int magnification) {
        if( e == null)
            return "";

        return "" + (int) (e.multi(BasisSet.current()) * e.getHp() * magnification / 100.0);
    }

    public static String getTrait(MaskUnit f, boolean talent, ArrayList<Integer> lvs, boolean icon, int lang) {
        if(f == null)
            return "";

        MaskUnit du;

        if(lvs != null & f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f;
        else
            du = f;

        StringBuilder allColor = new StringBuilder();
        StringBuilder allTrait = new StringBuilder();

        for(int i = 0; i < Interpret.TRAIT.length; i++) {
            if(i != 0)
                allColor.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");

            allTrait.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");
        }
        allTrait.append(LangID.getStringByID("data_white", lang)).append(", ").append(allColor);

        String trait = Interpret.getTrait(du.getTraits(), 0, icon, lang);

        if(trait.isBlank())
            trait = LangID.getStringByID("data_none", lang);

        if(trait.equals(allColor.toString()))
            trait = LangID.getStringByID("data_allcolor", lang);

        if(trait.equals(allTrait.toString()))
            trait = LangID.getStringByID("data_alltrait", lang);

        if(trait.endsWith(", "))
            trait = trait.substring(0, trait.length()-2);

        return trait;
    }

    public static String getTrait(MaskEnemy e, boolean icon, int lang) {
        if(e == null)
            return "";

        StringBuilder allColor = new StringBuilder();
        StringBuilder allTrait = new StringBuilder();

        for(int i = 0; i < Interpret.TRAIT.length; i++) {
            if(i != 0)
                allColor.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");

            allTrait.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");
        }
        allTrait.append(LangID.getStringByID("data_white", lang)).append(", ").append(allColor);

        String trait = Interpret.getTrait(e.getTraits(), e.getStar(), icon, lang);

        if(trait.isBlank())
            trait = LangID.getStringByID("data_none", lang);

        if(trait.equals(allColor.toString()))
            trait = LangID.getStringByID("data_allcolor", lang);

        if(trait.equals(allTrait.toString()))
            trait = LangID.getStringByID("data_alltrait", lang);

        if(trait.endsWith(", "))
            trait = trait.substring(0, trait.length()-2);

        return trait;
    }

    public static String getCost(MaskUnit f, boolean talent, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(lvs != null & f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f;
        else
            du = f;

        return String.valueOf((int)(du.getPrice()*1.5));
    }

    public static String getDrop(MaskEnemy e) {
        if(e == null)
            return "";

        Treasure t = BasisSet.current().t();

        return String.valueOf((int) (e.getDrop() * t.getDropMulti() / 100));
    }

    public static String getSiMu(MaskUnit f, int lang) {
        if(f == null)
            return "";

        if(Interpret.isType(f, 1))
            return LangID.getStringByID("data_area", lang);
        else
            return LangID.getStringByID("data_single", lang);
    }

    public static String getSiMu(MaskEnemy e, int lang) {
        if(e == null)
            return "";

        if(Interpret.isType(e, 1))
            return LangID.getStringByID("data_area", lang);
        else
            return LangID.getStringByID("data_single", lang);
    }

    public static String getTalent(MaskUnit f, ArrayList<Integer> lv, int lang) {
        if(f == null || f.getPCoin() == null)
            return LangID.getStringByID("data_notalent", lang);

        ArrayList<int[]> info = f.getPCoin().info;

        StringBuilder sb = new StringBuilder(LangID.getStringByID("data_talen", lang));

        if(f.getPCoin().trait.size() != 0) {
            sb.append("[");

            String trait = Interpret.getTrait(f.getPCoin().trait, 0, false, lang);

            if(trait.endsWith(", "))
                trait = trait.substring(0, trait.length() - 2);

            sb.append(trait).append("] ");
        }

        for(int i = 0; i < info.size(); i++) {
            int[] data = info.get(i);

            if(talentText.containsKey(data[0])) {
                sb.append(LangID.getStringByID(talentText.get(data[0]), lang)).append(" [").append(lv.get(i + 1)).append("]");
            } else {
                sb.append("??? [").append(lv.get(i + 1)).append("]");
            }

            if(i != info.size() - 1)
                sb.append(", ");
        }

        return sb.toString();
    }

    public static String getBarrier(MaskEnemy e, int lang) {
        if(e == null)
            return "";

        if(e.getProc().BARRIER.health == 0)
            return LangID.getStringByID("data_none", lang);
        else
            return String.valueOf(e.getProc().BARRIER.health);
    }

    public static String getMagnification(int[] mag, int star) {
        if(mag[0] == mag[1]) {
            return StaticStore.safeParseInt((mag[0] * 1.0 * star / 100)+"") + "%";
        } else {
            return "["+StaticStore.safeParseInt((mag[0] * 1.0 * star / 100)+"")+", "+StaticStore.safeParseInt((mag[1] * 1.0 * star / 100)+"")+"] %";
        }
    }

    public static String getPackName(String id, int lang) {
        if(mapIds.contains(id))
            return LangID.getStringByID("data_default", lang);

        PackData pack = UserProfile.getPack(id);

        if(pack == null)
            return id;
        else if(pack instanceof PackData.DefPack) {
            return LangID.getStringByID("data_default", lang);
        } else if(pack instanceof PackData.UserPack) {
            String p = ((PackData.UserPack) pack).desc.name;

            if(p == null)
                p = id;

            return p;
        }

        return id;
    }

    public static String getStar(Stage st, int star) {
        StageMap stm = st.getCont();

        StringBuilder res = new StringBuilder();

        for(int i = 0; i < stm.stars.length; i++) {
            if(i <= star) {
                res.append(EmojiStore.CROWN_ON.getAsMention());
            } else {
                res.append(EmojiStore.CROWN_OFF.getAsMention());
            }
        }

        res.append(" (").append(stm.stars[star]).append("%)");

        return res.toString();
    }

    public static String getEnergy(Stage st, int lang) {
        if(!(st.info instanceof DefStageInfo))
            return LangID.getStringByID("data_none", lang);

        DefStageInfo info = (DefStageInfo) st.info;

        StageMap stm = st.getCont();

        if(stm == null)
            return info.energy+"";

        MapColc mc = stm.getCont();

        if(mc == null)
            return info.energy+"";

        if(mc.getSID().equals("000014")) {
            if(info.energy < 1000) {
                return LangID.getStringByID("data_catamina", lang).replace("_", info.energy+"")+"!!drink!!";
            } else if(info.energy < 2000) {
                return LangID.getStringByID("data_cataminb", lang).replace("_", (info.energy-1000)+"")+"!!drink!!";
            } else {
                return LangID.getStringByID("data_cataminc", lang).replace("_", (info.energy-2000)+"")+"!!drink!!";
            }
        } else {
            return info.energy+"";
        }
    }

    public static String getBaseHealth(Stage st) {
        return ""+st.health;
    }

    public static String getXP(Stage st) {
        if(!(st.info instanceof DefStageInfo))
            return "" + 0;

        DefStageInfo info = (DefStageInfo) st.info;

        Treasure t = BasisSet.current().t();

        MapColc mc = st.getCont().getCont();

        if(mc.getSID().equals("000000") || mc.getSID().equals("000013"))
            return "" + (int) (info.xp * t.getXPMult() * 9);
        else
            return "" + (int) (info.xp * t.getXPMult());
    }

    public static String getDifficulty(Stage st, int lang) {
        if(!(st.info instanceof DefStageInfo) || ((DefStageInfo) st.info).diff == -1)
            return LangID.getStringByID("data_none", lang);
        else
            return "★"+((DefStageInfo) st.info).diff;
    }

    public static String getContinuable(Stage st, int lang) {
        if(st.non_con) {
            return LangID.getStringByID("data_false", lang);
        } else {
            return LangID.getStringByID("data_true", lang);
        }
    }

    public static String getLevelMagnification(StageMap map) {
        StringBuilder builder = new StringBuilder("(");

        for(int i = 0; i < map.stars.length; i++) {
            builder.append(map.stars[i]);

            if(i < map.stars.length - 1) {
                builder.append(", ");
            }
        }

        return builder.append("%)").toString();
    }

    public static String getLength(Stage st) {
        return ""+st.len;
    }

    public static String getMaxEnemy(Stage st) {
        return ""+st.max;
    }

    public static String getMusic(Stage st, int lang) {
        if(st.mus0 == null || st.mus0.id == -1) {
            return LangID.getStringByID("data_none", lang);
        } else {
            return Data.trio(st.mus0.id);
        }
    }

    public static String getMusicChange(Stage st) {
        return "<"+st.mush+"%";
    }

    public static String getMusic1(Stage st, int lang) {
        if(st.mus1 == null || st.mus1.id == -1) {
            return LangID.getStringByID("data_none", lang);
        } else {
            return Data.trio(st.mus1.id);
        }
    }

    public static String getBackground(Stage st, int lang) {
        if(st.bg == null || st.bg.id == -1) {
            return LangID.getStringByID("data_none", lang);
        } else {
            return Data.trio(st.bg.id);
        }
    }

    public static String getCastle(Stage st, int lang) {
        if(st.castle == null || st.castle.id == -1) {
            return LangID.getStringByID("data_none", lang);
        } else {
            return Data.trio(st.castle.id);
        }
    }

    public static String getMinSpawn(Stage st, boolean isFrame) {
        if(st.minSpawn == st.maxSpawn) {
            if(isFrame) {
                return st.minSpawn+"f";
            } else {
                return df.format(st.minSpawn/30.0)+"s";
            }
        } else {
            if(isFrame) {
                return st.minSpawn + "f ~ " + st.maxSpawn+"f";
            } else {
                return df.format(st.minSpawn/30.0)+"s ~ "+df.format(st.maxSpawn/30.0)+"s";
            }
        }
    }

    public static ArrayList<String> getLimit(Limit l, int lang) {
        ArrayList<String> res = new ArrayList<>();

        if(l == null)
            return res;

        if(l.line != 0) {
            res.add(LangID.getStringByID("data_linelim", lang)+" : "+LangID.getStringByID("data_firstline", lang));
        }

        if(l.max != 0) {
            res.add(LangID.getStringByID("data_maxcolim", lang)+" : "+LangID.getStringByID("data_costmax", lang).replace("_", String.valueOf(l.max)));
        }

        if(l.min != 0) {
            res.add(LangID.getStringByID("data_mincolim", lang)+" : "+LangID.getStringByID("data_costmin", lang).replace("_", String.valueOf(l.min)));
        }

        if(l.rare != 0) {
            String[] rid = {"data_basic", "data_ex", "data_rare", "data_sr", "data_ur", "data_lr"};
            StringBuilder rare = new StringBuilder();

            for(int i = 0; i < rid.length; i++) {
                if(((l.rare >> i) & 1) > 0)
                    rare.append(LangID.getStringByID(rid[i], lang)).append(", ");
            }

            res.add(LangID.getStringByID("data_rarelim", lang)+" : "+ rare.substring(0, rare.length() - 2));
        }

        if(l.num != 0) {
            res.add(LangID.getStringByID("data_maxunitlim", lang)+" : "+l.num);
        }

        if(l.group != null && l.group.set.size() != 0) {
            StringBuilder units = new StringBuilder();

            ArrayList<Unit> u = new ArrayList<>(l.group.set);

            for(int i = 0; i < u.size(); i++) {
                if(u.get(i).forms == null || u.get(i).forms.length == 0)
                    continue;

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String f = MultiLangCont.get(u.get(i).forms[0]);

                CommonStatic.getConfig().lang = oldConfig;

                if(f == null)
                    f = u.get(i).forms[0].names.toString();

                if(f.isBlank())
                    f = LangID.getStringByID("data_unit", lang)+Data.trio(u.get(i).id.id);

                if(i == l.group.set.size() - 1) {
                    units.append(f);
                } else {
                    units.append(f).append(", ");
                }
            }

            String result;

            if(l.group.type == 0) {
                result = LangID.getStringByID("data_charagroup", lang)+" : "+LangID.getStringByID("data_only", lang).replace("_", units.toString());
            } else {
                result = LangID.getStringByID("data_charagroup", lang)+" : "+LangID.getStringByID("data_cantuse", lang).replace("_", units.toString());
            }

            res.add(result);
        }

        return res;
    }

    public static ArrayList<String> getLimit(Limit l, CustomStageMap map, int lang) {
        ArrayList<String> res = new ArrayList<>();

        if(l == null)
            return res;

        if(l.line != 0) {
            res.add(LangID.getStringByID("data_linelim", lang)+" : "+LangID.getStringByID("data_firstline", lang));
        }

        if(l.max != 0) {
            res.add(LangID.getStringByID("data_maxcolim", lang)+" : "+LangID.getStringByID("data_costmax", lang).replace("_", String.valueOf(l.max)));
        }

        if(l.min != 0) {
            res.add(LangID.getStringByID("data_mincolim", lang)+" : "+LangID.getStringByID("data_costmin", lang).replace("_", String.valueOf(l.min)));
        }

        if(l.rare != 0) {
            String[] rid = {"data_basic", "data_ex", "data_rare", "data_sr", "data_ur", "data_lr"};
            StringBuilder rare = new StringBuilder();

            for(int i = 0; i < rid.length; i++) {
                if(((l.rare >> i) & 1) > 0)
                    rare.append(LangID.getStringByID(rid[i], lang)).append(", ");
            }

            res.add(LangID.getStringByID("data_rarelim", lang)+" : "+ rare.substring(0, rare.length() - 2));
        }

        if(l.num != 0) {
            res.add(LangID.getStringByID("data_maxunitlim", lang)+" : "+l.num);
        }

        if(l.group instanceof CustomCharaGroup && ((CustomCharaGroup) l.group).identifiers.size() != 0) {
            StringBuilder units = new StringBuilder();

            List<Identifier<Unit>> u = ((CustomCharaGroup) l.group).identifiers;

            for(int i = 0; i < u.size(); i++) {
                if(u.get(i) == null)
                    continue;

                String f;

                if(u.get(i).id >= UserProfile.getBCData().units.size()) {
                    f = map.unitNames.get(u.get(i).id);

                    if(f == null || f.isBlank())
                        f = LangID.getStringByID("data_unit", lang)+Data.trio(u.get(i).id);
                } else {
                    Unit unit = u.get(i).get();

                    if(unit == null)
                        continue;

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    f = MultiLangCont.get(unit.forms[0]);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(f == null)
                        f = unit.forms[0].names.toString();
                }

                if(i == l.group.set.size() - 1) {
                    units.append(f);
                } else {
                    units.append(f).append(", ");
                }
            }

            String result;

            if(l.group.type == 0) {
                result = LangID.getStringByID("data_charagroup", lang)+" : "+LangID.getStringByID("data_only", lang).replace("_", units.toString());
            } else {
                result = LangID.getStringByID("data_charagroup", lang)+" : "+LangID.getStringByID("data_cantuse", lang).replace("_", units.toString());
            }

            res.add(result);
        }

        for(int i = 0; i < res.size(); i++) {
            String filtered = " - " + res.get(i).replace("**", "");

            res.set(i, filtered);
        }

        return res;
    }

    public static String getMapCode(MapColc mc) {
        int index = mapIds.indexOf(mc.getSID());

        String code;

        if(index == -1)
            code = mc.getSID();
        else
            code = mapCodes[index];

        return code;
    }

    public static String getStageCode(Stage st) {
        StageMap stm = st.getCont();
        MapColc mc = stm.getCont();

        int index = mapIds.indexOf(mc.getSID());

        String code;

        if(index == -1)
            code = mc.getSID()+"-";
        else
            code = mapCodes[index]+"-";

        if(stm.id != null) {
            code += Data.trio(stm.id.id)+"-";
        } else {
            code += "Unknown-";
        }

        if(st.id != null) {
            code += Data.trio(st.id.id);
        } else {
            code += "Unknown";
        }

        return code;
    }

    public static String getDescription(Form f, int lang) {
        if(f.unit == null)
            return null;

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String[] desc = MultiLangCont.getStatic().FEXP.getCont(f);

        CommonStatic.getConfig().lang = oldConfig;

        if(desc == null)
            return null;

        boolean canGo = false;

        for (String s : desc) {
            if (s != null && !s.isBlank()) {
                canGo = true;
                break;
            }
        }

        if(canGo) {
            StringBuilder result = new StringBuilder();

            for(int i = 0; i < desc.length; i++) {
                result.append(desc[i]);

                if(i != desc.length - 1)
                    result.append("\n");
            }

            return result.toString();
        } else {
            return null;
        }
    }

    public static String getDescription(Enemy e, int lang) {
        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String[] desc = MultiLangCont.getStatic().EEXP.getCont(e);

        CommonStatic.getConfig().lang = oldConfig;

        if(desc == null)
            return null;

        boolean canGo = false;

        for(String s : desc) {
            if(s != null && !s.isBlank()) {
                canGo = true;
                break;
            }
        }

        if(canGo) {
            StringBuilder builder = new StringBuilder();

            for(int i = 0; i < desc.length; i++) {
                builder.append(desc[i]);

                if(i != desc.length - 1)
                    builder.append("\n");
            }

            return builder.toString();
        } else {
            return null;
        }
    }

    public static String getCatruitEvolve(Form f, int lang) {
        if(f.unit == null)
            return null;

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String[] cf = MultiLangCont.getStatic().CFEXP.getCont(f.unit.info);

        CommonStatic.getConfig().lang = oldConfig;

        if(cf == null)
            return null;

        boolean canGo = false;

        for(String s : cf) {
            if(s != null && !s.isBlank()) {
                canGo = true;
                break;
            }
        }

        if(canGo) {
            StringBuilder builder = new StringBuilder();

            for(int i = 0; i < cf.length; i++) {
                builder.append(cf[i]);

                if(i != cf.length -1)
                    builder.append("\n");
            }

            return builder.toString();
        } else {
            return null;
        }
    }

    public static String getRewards(Stage s, int lang) {
        if(s == null || !(s.info instanceof DefStageInfo) || ((DefStageInfo) s.info).drop == null || ((DefStageInfo) s.info).drop.length == 0)
            return null;

        DefStageInfo info = (DefStageInfo) s.info;

        ArrayList<String> chances = getDropData(s);

        if(chances == null)
            return null;

        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < info.drop.length; i++) {
            if(!chances.isEmpty() && i < chances.size() && Double.parseDouble(chances.get(i)) == 0.0)
                continue;

            if(s.getCont() != null && s.getCont().getCont() != null && s.getCont().getCont().getSID().equals("000003") && info.drop[i][1] == 1119) {
                return null;
            }

            String chance;

            if(chances.isEmpty())
                chance = String.valueOf(i + 1);
            else
                chance = chances.get(i)+"%";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String reward = MultiLangCont.getStatic().RWNAME.getCont(info.drop[i][1]);

            CommonStatic.getConfig().lang = oldConfig;

            if(reward == null || reward.isBlank())
                reward = LangID.getStringByID("data_dumreward", lang).replace("_", Data.trio(info.drop[i][1]));

            builder.append(chance).append("  |  ").append(reward);

            if(i == 0 && (info.rand == 1 || (info.drop[i][1] >= 1000 && info.drop[i][1] < 30000)))
                builder.append(LangID.getStringByID("data_once", lang));

            if(i == 0 && info.drop[i][0] != 100 && info.rand != -4 && !chances.isEmpty())
                builder.append(EmojiStore.TREASURE_RADAR.getAsMention());

            builder.append("  |  ").append(info.drop[i][2]);

            if(i != info.drop.length - 1)
                builder.append("\n");
        }

        if(chances.isEmpty())
            builder.append("!!number!!");
        else if(info.rand == -4)
            builder.append("!!nofail!!");

        return builder.toString();
    }

    public static List<String[]> getRewards(Stage s, CustomStageMap map, int lang) {
        if(s == null || !(s.info instanceof DefStageInfo) || ((DefStageInfo) s.info).drop == null || ((DefStageInfo) s.info).drop.length == 0)
            return null;

        DefStageInfo info = (DefStageInfo) s.info;

        ArrayList<String> chances = getDropData(s);

        if(chances == null)
            return null;

        List<String[]> result = new ArrayList<>();

        for(int i = 0; i < info.drop.length; i++) {
            if(!chances.isEmpty() && i < chances.size() && Double.parseDouble(chances.get(i)) == 0.0)
                continue;

            String[] data = new String[3];

            String chance;

            if(chances.isEmpty())
                chance = String.valueOf(i + 1);
            else
                chance = chances.get(i)+"%";

            data[0] = chance;

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String reward = MultiLangCont.getStatic().RWNAME.getCont(info.drop[i][1]);

            CommonStatic.getConfig().lang = oldConfig;

            if(reward == null || reward.isBlank())
                reward = map.rewardNames.get(info.drop[i][1]);

            if(reward == null || reward.isBlank())
                reward = LangID.getStringByID("data_dumreward", lang).replace("_", Data.trio(info.drop[i][1]));

            if(i == 0 && (info.rand == 1 || (info.drop[i][1] >= 1000 && info.drop[i][1] < 30000)))
                reward += " " + LangID.getStringByID("data_once", lang);

            data[1] = reward;

            data[2] = info.drop[i][2] + "";

            result.add(data);
        }

        if(chances.isEmpty())
            result.add(new String[1]);

        return result;
    }

    private static ArrayList<String> getDropData(Stage s) {
        ArrayList<String> res = new ArrayList<>();

        if(!(s.info instanceof DefStageInfo))
            return res;

        DefStageInfo info = (DefStageInfo) s.info;

        int[][] data = info.drop;

        int sum = 0;

        for(int[] d : data) {
            sum += d[0];
        }

        if(sum == 0)
            return null;

        if(sum == 1000) {
            for(int[] d : data) {
                res.add(df.format(d[0]/10.0));
            }
        } else if((sum == data.length && sum != 1) || info.rand == -3) {
            return res;
        } else if(sum == 100) {
            for(int[] d : data) {
                res.add(String.valueOf(d[0]));
            }
        } else if(sum > 100 && (info.rand == 0 || info.rand == 1)) {
            double rest = 100.0;

            if(data[0][0] == 100) {
                res.add(100+"");

                for(int i = 1; i < data.length; i++) {
                    double filter = rest * data[i][0] / 100.0;

                    rest -= filter;

                    res.add(df.format(filter));
                }
            } else {
                for(int[] d : data) {
                    double filter = rest * d[0] / 100.0;

                    rest -= filter;

                    res.add(df.format(filter));
                }
            }
        } else if(info.rand == -4) {
            int total = 0;

            for(int[] d : data) {
                total += d[0];
            }

            if(total == 0)
                return null;

            for(int[] d : data) {
                res.add(df.format(d[0] * 100.0 / total));
            }
        } else {
            for(int[] d : data) {
                res.add(String.valueOf(d[0]));
            }
        }

        return res;
    }

    public static String getScoreDrops(Stage st, int lang) {
        if(st == null || !(st.info instanceof DefStageInfo) || ((DefStageInfo) st.info).time == null || ((DefStageInfo) st.info).time.length == 0)
            return null;

        DefStageInfo info = (DefStageInfo) st.info;

        StringBuilder builder = new StringBuilder();

        int[][] data = info.time;

        for(int i = 0; i < info.time.length; i++) {
            String reward = MultiLangCont.getStatic().RWNAME.getCont(data[i][1]);

            if(reward == null || reward.isBlank())
                reward = LangID.getStringByID("data_dumreward", lang).replace("_", Data.trio(data[i][1]));

            builder.append(data[i][0]).append("  |  ").append(reward).append("  |  ").append(data[i][2]);

            if(i != info.time.length - 1)
                builder.append("\n");
        }

        return builder.toString();
    }

    public static List<String[]> getScoreDrops(Stage st, CustomStageMap map, int lang) {
        if(st == null || !(st.info instanceof DefStageInfo) || ((DefStageInfo) st.info).time == null || ((DefStageInfo) st.info).time.length == 0)
            return null;

        DefStageInfo info = (DefStageInfo) st.info;

        List<String[]> result = new ArrayList<>();

        int[][] data = info.time;

        for(int i = 0; i < info.time.length; i++) {
            String[] drop = new String[3];

            String reward = MultiLangCont.getStatic().RWNAME.getCont(data[i][1]);

            if(reward == null || reward.isBlank())
                reward = map.rewardNames.get(data[i][1]);

            if(reward == null || reward.isBlank())
                reward = LangID.getStringByID("data_dumreward", lang).replace("_", Data.trio(data[i][1]));

            drop[0] = data[i][0] + "";
            drop[1] = reward;
            drop[2] = data[i][2] + "";

            result.add(drop);
        }

        return result;
    }

    public static String getComboDescription(Combo c, int lang) {
        int factor = getComboFactor(c);

        String desc = LangID.getStringByID("data_"+getComboKeyword(c)+"combodesc", lang).replace("_", String.valueOf(factor));

        if(c.type == 14) {
            desc = desc.replace("ttt", df.format(0.5 * (100 - factor) / 100.0))
                    .replace("TTT", df.format(0.4 * (100 - factor) / 100.0))
                    .replace("ggg", df.format(1.5 * (100 + factor) / 100.0))
                    .replace("GGG", df.format(1.8 * (100 + factor) / 100.0));
        } else if(c.type == 15) {
            desc = desc.replace("ggg", df.format(3.0 * (100 + factor) / 100.0))
                    .replace("GGG", df.format(4.0 * (100 + factor) / 100.0));
        } else if(c.type == 16) {
            desc = desc.replace("ttt", df.format(0.25 * (100 - factor) / 100.0))
                    .replace("TTT", df.format(0.2 * (100 - factor) / 100.0));
        } else if(c.type == 22 || c.type == 23) {
            desc = desc.replace("ttt", df.format(0.2 / ((100 + factor) / 100.0)))
                    .replace("ggg", df.format(5 * (100 + factor) / 100.0));
        } else if(c.type == 7 || c.type == 11) {
            desc = desc.replace("-", df.format(factor / 30.0));
        }

        return desc;
    }

    public static String getComboType(Combo c, int lang) {
        return LangID.getStringByID("data_"+getComboKeyword(c)+"combo", lang) + " [" + getComboLevel(c, lang)+"]";
    }

    private static String getComboLevel(Combo c, int lang) {
        switch (c.lv) {
            case 0:
                return LangID.getStringByID("data_combosm", lang);
            case 1:
                return LangID.getStringByID("data_combom", lang);
            case 2:
                return LangID.getStringByID("data_combol", lang);
            case 3:
                return LangID.getStringByID("data_comboxl", lang);
            default:
                return "Lv. "+c.lv;
        }
    }

    private static int getComboFactor(Combo c) {
        switch (c.type) {
            case 0:
            case 2:
                return 10 + c.lv * 5;
            case 1:
            case 20:
            case 19:
            case 18:
            case 17:
            case 16:
            case 15:
            case 14:
            case 13:
            case 12:
            case 9:
                return 10 + 10 * c.lv;
            case 3:
                return 20 + 20 * c.lv;
            case 4:
                return 2 + c.lv;
            case 5:
                if (c.lv == 0) {
                    return 300;
                } else if (c.lv == 1) {
                    return 500;
                } else {
                    return 1000;
                }
            case 6:
            case 10:
                return 20 + 30 * c.lv;
            case 7:
                return 150 + 150 * c.lv;
            case 11:
                return 26 + 26 * c.lv;
            case 21:
                return 20 + 10 * c.lv;
            case 22:
            case 23:
                return 100 + 100 * c.lv;
            case 24:
                return 1 + c.lv;
            default:
                return 0;
        }
    }

    private static String getComboKeyword(Combo c) {
        switch (c.type) {
            case 0:
                return "atk";
            case 1:
                return "health";
            case 2:
                return "speed";
            case 14:
                return "strong";
            case 15:
                return "massive";
            case 16:
                return "resistant";
            case 17:
                return "kb";
            case 18:
                return "slow";
            case 19:
                return "freeze";
            case 20:
                return "weaken";
            case 21:
                return "strengthen";
            case 23:
                return "eva";
            case 22:
                return "witch";
            case 24:
                return "critical";
            case 3:
                return "caninitchar";
            case 6:
                return "canatk";
            case 7:
                return "canchar";
            case 10:
                return "basehp";
            case 5:
                return "initmon";
            case 4:
                return "worker";
            case 9:
                return "wallet";
            case 11:
                return "cooldown";
            case 12:
                return "acc";
            case 13:
                return "study";
            default:
                throw new IllegalStateException("Invalid Combo Type : "+c.type);
        }
    }

    public static int getDifficultyColor(int diff) {
        int[] rgb = HSVtoRGB((-220 * (diff - 1 - maxDifficulty) / 11.0) / 360.0);

        return new Color(rgb[0], rgb[1], rgb[2]).getRGB();
    }

    private static int[] HSVtoRGB(double h) {
        double r, g, b, i, f, p, q ,t;

        i = Math.floor(h * 6);
        f = h * 6 - i;
        p = 1 - 0.6;
        q = 1 - f * 0.6;
        t = 1 - (1 - f) * 0.6;

        switch ((int) (i%6)) {
            case 1:
                r = q;
                g = 1.0;
                b = p;
                break;
            case 2:
                r = p;
                g = 1.0;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = 1.0;
                break;
            case 4:
                r = t;
                g = p;
                b = 1.0;
                break;
            case 5:
                r = 1.0;
                g = p;
                b = q;
                break;
            default:
                r = 1.0;
                g = t;
                b = p;
        }

        return new int[] {(int) (r * 255), (int) (g * 255), (int) (b * 255)};
    }

    public static List<String> getMiscellaneous(Stage st, int lang) {
        List<String> result = new ArrayList<>();

        if(st.getCont() == null || st.getCont().info == null)
            return result;

        StageMap.StageMapInfo info = st.getCont().info;

        if(info.resetMode != -1) {
            if(info.resetMode > 0 && info.resetMode < 4)
                result.add(LangID.getStringByID("data_reset"+info.resetMode, lang));
            else
                result.add(LangID.getStringByID("data_resetx", lang));
        }

        if(info.clearLimit != -1) {
            result.add(LangID.getStringByID("data_numberplay", lang).replace("_", ""+info.clearLimit));
        }

        if(info.waitTime != -1) {
            String min;

            if(info.waitTime > 1)
                min = LangID.getStringByID("smins", lang);
            else
                min = LangID.getStringByID("smin", lang);

            result.add(LangID.getStringByID("data_waittime", lang).replace("_NNN_", ""+info.waitTime).replace("_TTT_", min));
        }

        if(info.hiddenUponClear) {
            result.add(LangID.getStringByID("data_hiddenclear", lang));
        }

        for(int i = 0; i < result.size(); i++) {
            result.set(i, result.get(i));
        }

        return result;
    }

    private static String getMapStageName(Stage st, int lang) {
        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String map = MultiLangCont.get(st.getCont());
        String stage = MultiLangCont.get(st);

        CommonStatic.getConfig().lang = oldConfig;

        if(map == null || map.isBlank()) {
            map = st.getCont().getCont().getSID()+"/"+Data.trio(st.getCont().id.id);
        }

        if(stage == null || stage.isBlank()) {
            stage = Data.trio(st.id.id);
        }

        return map + " - " + stage;
    }

    public static String getEXStage(Stage st, int lang) {
        if(!(st.info instanceof DefStageInfo) || (!((DefStageInfo) st.info).exConnection && ((DefStageInfo) st.info).exStages == null)) {
            return LangID.getStringByID("data_none", lang);
        }

        DefStageInfo info = (DefStageInfo) st.info;

        StringBuilder sb = new StringBuilder();

        if(info.exConnection) {
            StageMap sm = MapColc.DefMapColc.getMap(4000 + info.exMapID);

            if(sm == null)
                return LangID.getStringByID("data_none", lang);

            int n = info.exStageIDMax - info.exStageIDMin + 1;

            for(int i = info.exStageIDMin; i <= info.exStageIDMax; i++) {
                Stage s = sm.list.get(i);

                if(s != null) {
                    sb.append(df.format(info.exChance * 1.0 / n)).append("% | ").append(getMapStageName(s, lang));

                    if(i < info.exStageIDMax)
                        sb.append("\n");
                }
            }
        } else {
            for(int i = 0; i < info.exStages.length; i++) {
                sb.append(df.format(info.exChances[i])).append("% | ").append(getMapStageName(info.exStages[i], lang));

                if(i < info.exStages.length - 1)
                    sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static String getCompactTitle(Form f, int lang) {
        if(f == null)
            return "";

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String name = MultiLangCont.get(f);

        CommonStatic.getConfig().lang = oldConfig;

        if(name == null || name.isBlank())
            name = f.names.toString();

        if(name.isBlank())
            name = getID(f.uid.id, f.fid);
        else
            name += " [" + getID(f.uid.id, f.fid) + "]";

        String rarity;

        if(f.unit.rarity == 0)
            rarity = LangID.getStringByID("data_basic", lang);
        else if(f.unit.rarity == 1)
            rarity = LangID.getStringByID("data_ex", lang);
        else if(f.unit.rarity == 2)
            rarity = LangID.getStringByID("data_rare", lang);
        else if(f.unit.rarity == 3)
            rarity = LangID.getStringByID("data_sr", lang);
        else if(f.unit.rarity == 4)
            rarity = LangID.getStringByID("data_ur", lang);
        else if(f.unit.rarity == 5)
            rarity = LangID.getStringByID("data_lr", lang);
        else
            rarity = "Unknown";

        if(name.isBlank()) {
            return rarity;
        } else {
            return rarity + " - " + name;
        }
    }

    public static String getHealthHitback(MaskUnit f, UnitLevel lv, boolean talent, ArrayList<Integer> lvs) {
        return getHP(f, lv, talent, lvs) + " - " + getHitback(f, talent, lvs);
    }

    public static String getHealthHitback(MaskEnemy e, int m) {
        return getHP(e, m) + " - " + getHitback(e);
    }

    public static String getCompactAtk(MaskUnit f, boolean talent, UnitLevel lv, ArrayList<Integer> lvs) {
        if(f == null)
            return "";

        MaskUnit du;

        if(f.getPCoin() != null && talent)
            du = f.getPCoin().improve(lvs);
        else
            du = f;

        if(du.rawAtkData().length > 1)
            return getTotalAtk(lv, du, talent, lvs) + " " + getCompactAtks(du, talent, lv, lvs) + " [" + getDPS(f, lv, talent, lvs) + "]";
        else
            return getTotalAtk(lv, du, talent, lvs) + " [" + getDPS(f, lv, talent, lvs) + "]";
    }

    public static String getCompactAtk(MaskEnemy e, int m) {
        if(e == null)
            return "";

        if(e.rawAtkData().length > 1)
            return getTotalAtk(e, m) + " " + getCompactAtks(e, m) + " [" + getDPS(e, m) + "]";
        else
            return getTotalAtk(e, m) + " [" + getDPS(e, m) + "]";
    }

    private static String getCompactAtks(MaskUnit du, boolean talent, UnitLevel lv, ArrayList<Integer> lvs) {
        if(du == null)
            return "";

        int[][] raw = du.rawAtkData();

        Treasure t = BasisSet.current().t();

        ArrayList<Integer> damage = new ArrayList<>();

        for(int[] atk : raw) {
            int result;

            if(du.getPCoin() != null && talent) {
                result = (int) ((int) (Math.round(atk[0] * lv.getMult(lvs.get(0))) * t.getAtkMulti()) * du.getPCoin().getAtkMultiplication(lvs));
            } else {
                result = (int) (Math.round(atk[0] * lv.getMult(lvs.get(0))) * t.getAtkMulti());
            }

            damage.add(result);
        }

        StringBuilder result = new StringBuilder("(");

        for(int i = 0; i < damage.size(); i++) {
            if(raw[i][2] == 1) {
                result.append("**__")
                        .append(damage.get(i))
                        .append("__**");
            } else {
                result.append(damage.get(i));
            }

            if(i < damage.size() -1)
                result.append(", ");
            else
                result.append(")");
        }

        return result.toString();
    }

    private static String getCompactAtks(MaskEnemy e, int magnification) {
        if(e == null)
            return "";

        int[][] atks = e.rawAtkData();

        ArrayList<Integer> damage = new ArrayList<>();

        for(int[] atk : atks) {
            damage.add((int) (atk[0] * e.multi(BasisSet.current()) * magnification / 100.0));
        }

        StringBuilder result = new StringBuilder("(");

        for(int i = 0; i < damage.size(); i++) {
            if(atks[i][2] == 1) {
                result.append("**__")
                        .append(damage.get(i))
                        .append("__**");
            } else {
                result.append(damage.get(i));
            }

            if(i < damage.size() -1)
                result.append(", ");
            else
                result.append(")");
        }

        return result.toString();
    }

    public static String getCompactAtkTimings(MaskUnit f, boolean isFrame) {
        return getAtkTime(f, isFrame) + " : " + getPre(f, isFrame) + " -> " + getPost(f, isFrame) + " -> " + getTBA(f, isFrame);
    }

    public static String getCompactAtkTimings(MaskEnemy e, boolean isFrame) {
        return getAtkTime(e, isFrame) + " : " + getPre(e, isFrame) + " -> " + getPost(e, isFrame) + " -> " + getTBA(e, isFrame);
    }

    public static String getCostCooldownSpeed(MaskUnit f, boolean isFrame, boolean talent, ArrayList<Integer> lvs) {
        return getCost(f, talent, lvs) + " - " + getCD(f, isFrame, talent, lvs) + " - " + getSpeed(f, talent, lvs);
    }

    public static String getDropBarrierSpeed(MaskEnemy e, int lang) {
        return getDrop(e) + " - " + getBarrier(e, lang) + " - " + getSpeed(e);
    }

    public static String getCompactTitle(Enemy e, int lang) {
        if(e == null)
            return "";

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String name = MultiLangCont.get(e);

        CommonStatic.getConfig().lang = oldConfig;

        if(name == null || name.isBlank()) {
            name = e.names.toString();
        }

        if(name.isBlank()) {
            name = getID(e.id.id);
        } else {
            name += " [" + getID(e.id.id) + "]";
        }

        return name;
    }

    public static String getIdDifficultyLevel(Stage st, int star, int lang) {
        return getStageCode(st) +" - " + getDifficulty(st, lang) + " - " + getStar(st, star);
    }

    public static String getEnergyBaseXP(Stage st, int lang) {
        return getEnergy(st, lang) + " - " + getBaseHealth(st) + " - " + getXP(st);
    }

    public static String getEnemyContinuableLength(Stage st, int lang) {
        return getMaxEnemy(st) + " - " + getContinuable(st, lang) + " - " + getLength(st);
    }

    public static String getMusciBackgroundCastle(Stage st, int lang) {
        String result = getMusic(st, lang);

        if(st.mus1 != null && (st.mus0 == null || st.mus0.id != st.mus1.id)) {
            result += "->" + getMusic1(st, lang);
        }

        return result + " - " + getBackground(st, lang) + " - " + getCastle(st, lang);
    }

    public static String getTalentTitle(MaskUnit du, int index, int lang) {
        if(du == null || du.getPCoin() == null)
            return "";

        int[] data = du.getPCoin().info.get(index);

        String name;

        if(talentText.containsKey(data[0])) {
            name = LangID.getStringByID(talentText.get(data[0]), lang);
        } else {
            name = "???";
        }

        if(data[1] >= 2) {
            name += " [1 ~ " + data[1] + "]";
        }

        if(talentIcon.containsKey(data[0])) {
            String code = talentIcon.get(data[0]);

            RichCustomEmoji emoji;

            if(code.startsWith("T_")) {
                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                emoji = EmojiStore.TRAIT.getCont(code);

                CommonStatic.getConfig().lang = oldConfig;
            } else {
                emoji = EmojiStore.ABILITY.get(code);
            }

            if(emoji != null) {
                name = emoji.getAsMention() + " " + name;
            }
        }

        if(du.getPCoin().trait.size() == 1 && index == 0) {
            String code = Interpret.TRAITICON[du.getPCoin().trait.get(0).id.id];

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            RichCustomEmoji emoji = EmojiStore.TRAIT.getCont(code);

            CommonStatic.getConfig().lang = oldConfig;

            if(emoji != null)
                return emoji.getAsMention() + name;
            else
                return name;
        } else {
            return name;
        }
    }

    public static String getTalentExplanation(MaskUnit du, MaskUnit improved, int index, boolean isFrame, int lang) {
        if(du == null || du.getPCoin() == null)
            return "";

        int[] data = du.getPCoin().info.get(index);

        String talentName;

        if(talentText.containsKey(data[0])) {
            talentName = LangID.getStringByID(talentText.get(data[0]), lang);
        } else {
            talentName = "???";
        }

        int[] type = PC_CORRES[data[0]];

        String desc = "";

        if(du.getPCoin().trait.size() == 1 && index == 0) {
            desc += LangID.getStringByID("talentinfo_together", lang).replace("_", LangID.getStringByID(Interpret.TRAIT[du.getPCoin().trait.get(0).id.id], lang));
        }

        switch (type[0]) {
            case PC_P:
                if (data[1] >= 1) {
                    Proc.ProcItem p = du.getProc().getArr(type[1]);

                    if (!p.exists()) {
                        Formatter.Context c = new Formatter.Context(true, !isFrame, new double[]{0, 0});

                        int oldConfig = CommonStatic.getConfig().lang;
                        CommonStatic.getConfig().lang = lang;

                        String f = ProcLang.get().get(type[1]).format;

                        CommonStatic.getConfig().lang = oldConfig;

                        desc += Formatter.format(f, improved.getProc().getArr(type[1]), c) + "\n\n";
                    }

                    int changedIndex = findDifferentTalentIndex(data);

                    int min = data[2 + changedIndex * 2];
                    int max = data[3 + changedIndex * 2];

                    if(p.exists()) {
                        min += p.get(changedIndex);
                        max += p.get(changedIndex);
                    }

                    if (changedIndex == -1) {
                        StaticStore.logger.uploadLog("W/DataToString::getTalentExplanation - Failed to find different value set in talent : " + data[0]);

                        return "";
                    }

                    String fieldName = p.getFieldName(changedIndex);
                    String descID;

                    switch (fieldName) {
                        case "prob":
                            descID = "talentinfo_chance";

                            break;
                        case "time":
                            descID = "talentinfo_duration";

                            break;
                        case "mult":
                            descID = "talentinfo_multiplier";

                            break;
                        default:
                            throw new IllegalStateException("Unknown proc field name : " + fieldName);
                    }

                    if (fieldName.equals("time")) {
                        if (isFrame) {
                            desc += LangID.getStringByID(descID, lang).replace("_mmm_", min + "f").replace("_MMM_", max + "f") + "\n\n";
                        } else {
                            desc += LangID.getStringByID(descID, lang).replace("_mmm_", df.format(min / 30.0) + "s").replace("_MMM_", df.format(max / 30.0) + "s") + "\n\n";
                        }
                    } else {
                        if (fieldName.equals("mult") && type[1] == P_WEAK) {
                            min = 100 - min;
                            max = 100 - max;
                        }

                        desc += LangID.getStringByID(descID, lang).replace("_mmm_", min + "").replace("_MMM_", max + "") + "\n\n";
                    }
                }

                return desc + fillUpNpCost(data, lang, desc.isBlank());
            case PC_IMU:
            case PC_AB:
                return desc + fillUpNpCost(data, lang, desc.isBlank());
            case PC_BASE:
                int changedIndex = findDifferentTalentIndex(data);

                if(changedIndex == -1) {
                    StaticStore.logger.uploadLog("W/DataToString::getTalentExplanation - Failed to find different value set in talent : " + data[0]);

                    return "";
                }

                int min = data[2 + changedIndex * 2];
                int max = data[3 + changedIndex * 2];

                if(type[1] == PC2_COST) {
                    min = (int) (min * 1.5);
                    max = (int) (max * 1.5);
                }

                String key;

                switch (type[1]) {
                    case PC2_HP:
                        key = "talentinfo_health";

                        break;
                    case PC2_ATK:
                        key = "talentinfo_attack";

                        break;
                    case PC2_SPEED:
                        key = "talentinfo_speed";

                        break;
                    case PC2_COST:
                        key = "talentinfo_cost";

                        break;
                    case PC2_CD:
                        key = "talentinfo_cooldown";

                        break;
                    case PC2_HB:
                        key = "talentinfo_kb";

                        break;
                    default:
                        throw new IllegalStateException("Invalid P_BASE ID : " + type[1]);
                }

                return desc + LangID.getStringByID(key, lang)
                        .replace("_mmm_", min + "")
                        .replace("_MMM_", max + "") +
                        fillUpNpCost(data, lang, true);
            case PC_TRAIT:
                return desc + LangID.getStringByID("talentinfo_trait", lang).replace("_", talentName) + fillUpNpCost(data, lang, true);
            default:
                throw new IllegalStateException("E/DataToString::getTalentExplanation - Somehow reached point where bot couldn't get description of talent " + data[0] + " | Type : " + type[0]);
        }
    }

    private static int findDifferentTalentIndex(int[] data) {
        for(int i = 0; i < 4; i++) {
            if(data[2 + 2 * i] != data[3 + 2 * i]) {
                return i;
            }
        }

        return -1;
    }

    private static String fillUpNpCost(int[] data, int lang, boolean space) {
        if(talentLevel.containsKey(data[11])) {
            if(data[1] < 2) {
                return (space ? "\n** **\n" : "") + LangID.getStringByID("talentinfo_npsingle", lang).replace("_", talentLevel.get(data[11])[0] + "");
            } else {
                int[] costs = talentLevel.get(data[11]);

                StringBuilder cost = new StringBuilder("[");
                int totalCost = 0;

                for(int i = 0; i < data[1]; i++) {
                    if(i < costs.length) {
                        cost.append(costs[i]);
                        totalCost += costs[i];

                        if(i < data[1] - 1 && i < costs.length - 1)
                            cost.append(", ");
                        else
                            cost.append("]");
                    }
                }

                return (space ? "\n** **\n" : "") + LangID.getStringByID("talentinfo_np", lang).replace("_CCC_", cost.toString()).replace("_TTT_", totalCost + "");
            }
        } else {
            StaticStore.logger.uploadLog("W/DataToString::fillUpNpCost - Unknown talent cost group ID : " + data[11]);

            return "";
        }
    }

    public static String accumulateNpCost(PCoin talent, int lang) {
        int cost = 0;

        for(int i = 0; i < talent.info.size(); i++) {
            int[] data = talent.info.get(i);

            if(talentLevel.containsKey(data[11])) {
                int[] costCurve = talentLevel.get(data[11]);

                if(data[1] <= 1) {
                    cost += costCurve[0];
                } else {
                    for(int j = 0; j < data[1]; j++) {
                        if(j < costCurve.length)
                            cost += costCurve[j];
                    }
                }
            }
        }

        return LangID.getStringByID("talentinfo_total", lang).replace("_", cost + "");
    }
}
