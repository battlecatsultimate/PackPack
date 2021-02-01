package mandarin.packpack.supporter.bc;

import common.battle.data.MaskUnit;
import common.util.Data;
import common.util.unit.Enemy;
import common.util.unit.Form;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class EntityHandler {
    private static final DecimalFormat df;

    static {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);

        df = (DecimalFormat) nf;
        df.applyPattern("#.##");
    }

    public static void showUnitEmb(Form f, MessageChannel ch, boolean isFrame, boolean talent, int[] lv, int lang) {
        int level = lv[0];
        int levelp = 0;

        if(level > f.unit.max) {
            level = f.unit.max;
            levelp = lv[0] - level;
        }

        String l;

        if(levelp == 0)
            l = "" + level;
        else
            l = level + " + " + levelp;

        ch.createEmbed(m -> {
            try {
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

                m.setTitle(DataToString.getTitle(f, lang));

                if(talent && f.getPCoin() != null && t != null) {
                    m.setDescription(LangID.getStringByID("data_talent", lang));
                }

                m.setColor(c);
                m.setThumbnail("https://github.com/MandarinSmell/PackPackImages/raw/master/unit/"+ Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+".png");
                m.addField(LangID.getStringByID("data_id", lang), DataToString.getID(f.uid.id, f.fid), false);
                m.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(f, lv[0], talent, t), true);
                m.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(f, talent, t), true);
                m.addField(LangID.getStringByID("data_level", lang), l, true);
                m.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(f, lv[0], talent, t), false);
                m.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(f, lv[0], talent, t), true);
                m.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(f, isFrame), true);
                m.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(f, lang), true);
                m.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(f, isFrame), true);
                m.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(f, isFrame), true);
                m.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(f, isFrame), true);
                m.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(f, talent, t, lang), false);
                m.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(f, lang), true);
                m.addField(LangID.getStringByID("data_cost", lang), DataToString.getCost(f, talent, t), true);
                m.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(f), true);
                m.addField(LangID.getStringByID("data_cooldown", lang), DataToString.getCD(f,isFrame, talent, t), true);
                m.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(f, talent, t), true);

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

                m.addField(LangID.getStringByID("data_ability", lang), res, false);

                if(t != null)
                    m.setFooter(DataToString.getTalent(f, t, lang), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).subscribe();
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

    public static void showEnemyEmb(Enemy e, MessageChannel ch, boolean isFrame, int[] magnification, int lang) {
        ch.createEmbed(m -> {
            try {
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

                m.setTitle(DataToString.getTitle(e));
                m.setColor(c);
                m.setThumbnail("https://github.com/MandarinSmell/PackPackImages/raw/master/enemy/"+Data.trio(e.id.id)+".png");
                m.addField(LangID.getStringByID("data_id", lang), DataToString.getID(e.id.id), false);
                m.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(e, mag[0]), true);
                m.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(e), true);
                m.addField(LangID.getStringByID("data_magnif", lang), DataToString.getMagnification(mag), true);
                m.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(e, mag[1]), false);
                m.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(e, mag[1]), true);
                m.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(e, isFrame), true);
                m.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(e, lang), true);
                m.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(e, isFrame), true);
                m.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(e, isFrame), true);
                m.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(e, isFrame), true);
                m.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(e, lang), false);
                m.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(e, lang), true);
                m.addField(LangID.getStringByID("data_drop", lang), DataToString.getDrop(e), true);
                m.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(e), true);
                m.addField(LangID.getStringByID("data_barrier", lang), DataToString.getBarrier(e, lang), true);
                m.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(e), true);

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

                m.addField(LangID.getStringByID("data_ability", lang), res, false);

                m.setFooter(LangID.getStringByID("enemyst_source", lang), null);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }).subscribe();
    }
}
