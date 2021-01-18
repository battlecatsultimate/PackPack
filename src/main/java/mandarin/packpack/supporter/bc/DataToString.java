package mandarin.packpack.supporter.bc;

import common.battle.BasisSet;
import common.battle.Treasure;
import common.battle.data.AtkDataModel;
import common.battle.data.CustomEntity;
import common.battle.data.MaskAtk;
import common.battle.data.MaskUnit;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.lang.LangID;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DataToString {
    private static final Map<Integer, String> talentText = new HashMap<>();
    private static final DecimalFormat df;

    static {
        talentText.put(0, "??");
        talentText.put(1, "Freeze");

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        df = (DecimalFormat) nf;
        df.applyPattern("#.##");
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

    public static String getAtkTime(Form f, boolean isFrame) {
        if(f == null || f.du == null)
            return "";

        if(isFrame) {
            return f.du.getItv()+"f";
        } else {
            return df.format(f.du.getItv()/30.0)+"s";
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

    public static String getPost(Form f, boolean isFrame) {
        if(f == null || f.du == null)
            return "";

        if(isFrame) {
            return f.du.getPost()+"f";
        } else {
            return df.format(f.du.getPost()/30.0)+"s";
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

    public static String getID(int uid, int fid) {
        return Data.trio(uid)+" - "+Data.trio(fid);
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
            atk = f.du.getAtkModel(0);
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

    public static String getTotalAtk(Form f, int lv, MaskUnit du) {
        Treasure t = BasisSet.current().t();

        return String.valueOf((int) (du.allAtk() * t.getAtkMulti() * f.unit.lv.getMult(lv)));
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

    public static String getSiMu(Form f, int lang) {
        if(f == null || f.du == null)
            return "";

        if(Interpret.isType(f.du, 1))
            return LangID.getStringByID("data_area", lang);
        else
            return LangID.getStringByID("data_single", lang);
    }
}
