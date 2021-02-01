package mandarin.packpack.supporter.bc;

import common.battle.BasisSet;
import common.battle.Treasure;
import common.battle.data.*;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import common.util.unit.Form;
import mandarin.packpack.supporter.lang.LangID;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class DataToString {
    private static final Map<Integer, String> talentText = new HashMap<>();
    private static final DecimalFormat df;

    static {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        df = (DecimalFormat) nf;
        df.applyPattern("#.##");
    }

    public static void initialize() {
        talentText.put(0, "??");
        talentText.put(1, "data_weaken");
        talentText.put(2, "data_freeze");
        talentText.put(3, "data_slow");
        talentText.put(4, "data_attackon");
        talentText.put(5, "data_strong");
        talentText.put(6, "data_resistant");
        talentText.put(7, "data_massive");
        talentText.put(8, "data_knockback");
        talentText.put(9, "data_warp");
        talentText.put(10, "data_strength");
        talentText.put(11, "data_survive");
        talentText.put(12, "data_basedest");
        talentText.put(13, "data_critical");
        talentText.put(14, "data_zombiekill");
        talentText.put(15, "data_barrierbreak");
        talentText.put(16, "data_extramon");
        talentText.put(17, "data_wave");
        talentText.put(18, "data_resweak");
        talentText.put(19, "data_resfreeze");
        talentText.put(20, "data_resslow");
        talentText.put(21, "data_reskb");
        talentText.put(22, "data_reswave");
        talentText.put(23, "data_waveshie");
        talentText.put(24, "data_reswarp");
        talentText.put(25, "data_cost");
        talentText.put(26, "data_cooldown");
        talentText.put(27, "data_speed");
        talentText.put(28, "??");
        talentText.put(29, "data_imucurse");
        talentText.put(30, "data_rescurse");
        talentText.put(31, "data_atk");
        talentText.put(32, "data_hp");
        talentText.put(33, "data_red");
        talentText.put(34, "data_float");
        talentText.put(35, "data_black");
        talentText.put(36, "data_metal");
        talentText.put(37, "data_angel");
        talentText.put(38, "data_alien");
        talentText.put(39, "data_zombie");
        talentText.put(40, "data_relic");
        talentText.put(41, "data_white");
        talentText.put(42, "??");
        talentText.put(43, "??");
        talentText.put(44, "data_imuweak");
        talentText.put(45, "data_imufreeze");
        talentText.put(46, "data_imuslow");
        talentText.put(47, "data_imukb");
        talentText.put(48, "data_imuwave");
        talentText.put(49, "data_imuwarp");
        talentText.put(50, "data_savage");
        talentText.put(51, "data_invinci");
        talentText.put(52, "data_respoison");
        talentText.put(53, "data_imupoison");
        talentText.put(54, "data_ressurge");
        talentText.put(55, "data_imusurge");
        talentText.put(56, "data_surge");
    }

    public static String getTitle(Form f, int lang) {
        if(f == null)
            return "";

        String name = MultiLangCont.get(f);

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

    public static String getTitle(Enemy e) {
        if(e == null)
            return "";

        if(MultiLangCont.get(e) == null) {
            return Data.trio(e.id.id);
        } else
            return MultiLangCont.get(e);
    }

    public static String getAtkTime(Form f, boolean isFrame) {
        if(f == null || f.du == null)
            return "";

        if(isFrame) {
            return f.du.getItv()+"f";
        } else {
            return df.format(f.du.getItv()/30.0)+"s";
        }
    }

    public static String getAtkTime(Enemy e, boolean isFrame) {
        if(e == null || e.de == null)
            return "";

        if(isFrame) {
            return e.de.getItv()+"f";
        } else {
            return df.format(e.de.getItv()/30.0)+"s";
        }
    }

    public static String getAbilT(Form f, int lang) {
        if(f == null)
            return "";

        if(f.du == null)
            return "";

        int[][] raw = f.du.rawAtkData();

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

    public static String getAbilT(Enemy e, int lang) {
        if(e == null || e.de == null)
            return "";

        int[][] raw = e.de.rawAtkData();

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

    public static String getPost(Form f, boolean isFrame) {
        if(f == null || f.du == null)
            return "";

        if(isFrame) {
            return f.du.getPost()+"f";
        } else {
            return df.format(f.du.getPost()/30.0)+"s";
        }
    }

    public static String getPost(Enemy e, boolean isFrame) {
        if(e == null || e.de == null)
            return "";

        if(isFrame) {
            return e.de.getPost()+"f";
        } else {
            return df.format(e.de.getPost()/30.0)+"s";
        }
    }

    public static String getTBA(Form f, boolean isFrame) {
        if(f == null || f.du == null)
            return "";

        if(isFrame) {
            return f.du.getTBA()+"f";
        } else {
            return df.format(f.du.getTBA()/30.0)+"s";
        }
    }

    public static String getTBA(Enemy e, boolean isFrame) {
        if(e == null || e.de == null)
            return "";

        if(isFrame) {
            return e.de.getTBA()+"f";
        } else {
            return df.format(e.de.getTBA()/30.0)+"s";
        }
    }

    public static String getPre(Form f, boolean isFrame) {
        if(f == null || f.du == null)
            return "";

        int[][] raw = f.du.rawAtkData();

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

    public static String getPre(Enemy e, boolean isFrame) {
        if(e == null || e.de == null)
            return "";

        int[][] raw = e.de.rawAtkData();

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

    public static String getRange(Form f) {
        if(f == null || f.du == null)
            return "";

        int r = f.du.getRange();

        MaskAtk atk;

        if(f.du.getAtkCount() == 1)
            atk = f.du.getAtkModel(0);
        else
        if(f.du instanceof CustomEntity) {
            if(allRangeSame((CustomEntity) f.du))
                atk = f.du.getAtkModel(0);
            else
                atk = f.du.getRepAtk();
        } else {
            atk = f.du.getRepAtk();
        }

        int lds = atk.getShortPoint();
        int ldr = atk.getLongPoint() - atk.getShortPoint();

        int start = Math.min(lds, lds+ldr);
        int end = Math.max(lds, lds+ldr);

        if(f.du.isLD() || f.du.isOmni()) {
            return r +" / "+start+" ~ "+end;
        } else {
            return String.valueOf(r);
        }
    }

    public static String getRange(Enemy e) {
        if(e == null || e.de == null)
            return "";

        int r = e.de.getRange();

        MaskAtk atk;

        if(e.de.getAtkCount() == 1)
            atk = e.de.getAtkModel(0);
        else {
            if (e.de instanceof CustomEntity) {
                if (allRangeSame((CustomEntity) e.de))
                    atk = e.de.getAtkModel(0);
                else
                    atk = e.de.getRepAtk();
            } else {
                atk = e.de.getRepAtk();
            }
        }

        int lds = atk.getShortPoint();
        int ldr = atk.getLongPoint() - atk.getShortPoint();

        int start = Math.min(lds, lds+ldr);
        int end = Math.max(lds, lds+ldr);

        if(e.de.isLD() || e.de.isOmni()) {
            return r + " / "+start+" ~ "+end;
        } else
            return String.valueOf(r);
    }

    private static boolean allRangeSame(CustomEntity du) {
        ArrayList<Integer> near = new ArrayList<>();
        ArrayList<Integer> far = new ArrayList<>();

        for(AtkDataModel atk : du.atks) {
            near.add(atk.getShortPoint());
            far.add(atk.getLongPoint());
        }

        if(near.isEmpty() && far.isEmpty())
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

    public static String getCD(Form f, boolean isFrame, boolean talent, int[] lvs) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null) {
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        } else
            du = f.du;

        if(isFrame) {
            return BasisSet.current().t().getFinRes(du.getRespawn())+"f";
        } else {
            return df.format(BasisSet.current().t().getFinRes(du.getRespawn())/30.0)+"s";
        }
    }

    public static String getAtk(Form f, int lv, boolean talent, int[] lvs) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        else
            du = f.du;

        if(du.rawAtkData().length > 1)
            return getTotalAtk(f, lv, du) + " " + getAtks(f, lv, du);
        else
            return getTotalAtk(f, lv, du);
    }

    public static String getAtk(Enemy e, int magnification) {
        if(e == null || e.de == null)
            return "";

        if(e.de.rawAtkData().length > 1)
            return getTotalAtk(e, magnification)+" " +getAtks(e, magnification);
        else
            return getTotalAtk(e, magnification);
    }

    public static String getTotalAtk(Form f, int lv, MaskUnit du) {
        Treasure t = BasisSet.current().t();

        return String.valueOf((int) (du.allAtk() * t.getAtkMulti() * f.unit.lv.getMult(lv)));
    }

    public static String getTotalAtk(Enemy e, int magnification) {
        return "" + (int) (e.de.multi(BasisSet.current()) * e.de.allAtk() * magnification / 100.0);
    }

    public static String getAtks(Form f, int lv, MaskUnit du) {
        if(f == null || f.du == null)
            return "";

        int[][] raw = du.rawAtkData();

        Treasure t = BasisSet.current().t();

        ArrayList<Integer> damage = new ArrayList<>();

        for(int[] atk : raw) {
            damage.add((int) (atk[0] * t.getAtkMulti() * f.unit.lv.getMult(lv)));
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

    public static String getAtks(Enemy e, int magnification) {
        if(e == null || e.de == null)
            return "";

        int[][] atks = e.de.rawAtkData();

        ArrayList<Integer> damages = new ArrayList<>();

        for(int[] atk : atks) {
            damages.add((int) (atk[0] * e.de.multi(BasisSet.current()) * magnification / 100.0));
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

    public static String getDPS(Form f, int lv, boolean talent, int[] lvs) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        else
            du = f.du;

        return df.format(Double.parseDouble(getTotalAtk(f, lv, du)) / (du.getItv() / 30.0));
    }

    public static String getDPS(Enemy e, int magnification) {
        if(e == null || e.de == null)
            return "";

        return df.format(Double.parseDouble(getTotalAtk(e, magnification)) / (e.de.getItv() / 30.0));
    }

    public static String getSpeed(Form f, boolean talent, int[] lvs) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        else
            du = f.du;

        return String.valueOf(du.getSpeed());
    }

    public static String getSpeed(Enemy e) {
        if(e == null || e.de == null)
            return "";

        return String.valueOf(e.de.getSpeed());
    }

    public static String getHitback(Form f, boolean talent, int[] lvs) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        else
            du = f.du;

        return String.valueOf(du.getHb());
    }

    public static String getHitback(Enemy e) {
        if(e == null || e.de == null)
            return "";

        return String.valueOf(e.de.getHb());
    }

    public static String getHP(Form f, int lv, boolean talent, int[] lvs) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null && f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        else
            du = f.du;

        Treasure t = BasisSet.current().t();

        return String.valueOf((int) (du.getHp() * t.getDefMulti() * f.unit.lv.getMult(lv)));
    }

    public static String getHP(Enemy e, int magnification) {
        if(e == null || e.de == null)
            return "";

        return "" + (int) (e.de.multi(BasisSet.current()) * e.de.getHp() * magnification / 100.0);
    }

    public static String getTrait(Form f, boolean talent, int[] lvs, int lang) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null & f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        else
            du = f.du;

        StringBuilder allColor = new StringBuilder();
        StringBuilder allTrait = new StringBuilder();

        for(int i = 0; i < Interpret.TRAIT.length; i++) {
            if(i != 0)
                allColor.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");

            allTrait.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");
        }
        allTrait.append(LangID.getStringByID("data_white", lang)).append(", ").append(allColor.toString());

        String trait = Interpret.getTrait(du.getType(), 0, lang);

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

    public static String getTrait(Enemy e, int lang) {
        if(e == null || e.de == null)
            return "";

        StringBuilder allColor = new StringBuilder();
        StringBuilder allTrait = new StringBuilder();

        for(int i = 0; i < Interpret.TRAIT.length; i++) {
            if(i != 0)
                allColor.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");

            allTrait.append(LangID.getStringByID(Interpret.TRAIT[i], lang)).append(", ");
        }
        allTrait.append(LangID.getStringByID("data_white", lang)).append(", ").append(allColor.toString());

        String trait = Interpret.getTrait(e.de.getType(), e.de.getStar(), lang);

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

    public static String getCost(Form f, boolean talent, int[] lvs) {
        if(f == null || f.du == null)
            return "";

        MaskUnit du;

        if(lvs != null & f.getPCoin() != null)
            if(talent)
                du = f.getPCoin().improve(lvs);
            else
                du = f.du;
        else
            du = f.du;

        return String.valueOf((int)(du.getPrice()*1.5));
    }

    public static String getDrop(Enemy e) {
        if(e == null || e.de == null)
            return "";

        Treasure t = BasisSet.current().t();

        return String.valueOf((int) (e.de.getDrop() * t.getDropMulti()));
    }

    public static String getSiMu(Form f, int lang) {
        if(f == null || f.du == null)
            return "";

        if(Interpret.isType(f.du, 1))
            return LangID.getStringByID("data_area", lang);
        else
            return LangID.getStringByID("data_single", lang);
    }

    public static String getSiMu(Enemy e, int lang) {
        if(e == null || e.de == null)
            return "";

        if(Interpret.isType(e.de, 1))
            return LangID.getStringByID("data_area", lang);
        else
            return LangID.getStringByID("data_single", lang);
    }

    public static String getTalent(Form f, int[] lv, int lang) {
        if(f == null || f.getPCoin() == null)
            return LangID.getStringByID("data_notalent", lang);

        int[][] info = f.getPCoin().info;

        StringBuilder sb = new StringBuilder(LangID.getStringByID("data_talen", lang));

        if(f.getPCoin().type != 0) {
            sb.append("[");

            String trait = Interpret.getTrait(f.getPCoin().type, 0, lang);

            if(trait.endsWith(", "))
                trait = trait.substring(0, trait.length() - 2);

            sb.append(trait).append("] ");
        }

        for(int i = 0; i < info.length; i++) {
            int[] data = info[i];

            if(talentText.containsKey(data[0])) {
                sb.append(LangID.getStringByID(talentText.get(data[0]), lang)).append(" [").append(lv[i+1]).append("]");
            } else {
                sb.append("??? [").append(lv[i+1]).append("]");
            }

            if(i != info.length - 1)
                sb.append(", ");
        }

        return sb.toString();
    }

    public static String getBarrier(Enemy e, int lang) {
        if(e == null || e.de == null)
            return "";

        if(e.de.getShield() == 0)
            return LangID.getStringByID("data_none", lang);
        else
            return String.valueOf(e.de.getShield());
    }

    public static String getMagnification(int[] mag) {
        if(mag[0] == mag[1]) {
            return mag[0] + "%";
        } else {
            return "["+mag[0]+", "+mag[1]+"] %";
        }
    }
}
