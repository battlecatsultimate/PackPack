package mandarin.packpack.supporter.bc;

import common.battle.data.MaskUnit;
import common.util.Data;
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

    public static void showUnitEmb(Form f, MessageChannel ch, boolean isFrame, boolean talent, int lv, int lang) {
        int[] t;

        if(talent && f.getPCoin() != null)
            t = f.getPCoin().max;
        else
            t = null;

        int level = lv;
        int levelp = 0;

        if(level > f.unit.max) {
            level = f.unit.max;
            levelp = lv - level;
        }

        String l;

        if(levelp == 0)
            l = "" + level;
        else
            l = level + " + " + levelp;

        ch.createEmbed(m -> {
            try {
                m.setTitle(DataToString.getTitle(f, lang));
                if(talent && f.getPCoin() != null) {
                    m.setDescription(LangID.getStringByID("data_talent", lang));
                }

                Color c;

                if(f.fid == 0)
                    c = StaticStore.rainbow[4];
                else if(f.fid == 1)
                    c = StaticStore.rainbow[3];
                else
                    c = StaticStore.rainbow[2];

                m.setColor(c);
                m.setThumbnail("https://github.com/MandarinSmell/PackPackImages/raw/master/unit/"+ Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+".png");
                m.addField(LangID.getStringByID("data_id", lang), DataToString.getID(f.uid.id, f.fid), false);
                m.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(f, lv, talent, t), true);
                m.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(f, talent, t), true);
                m.addField(LangID.getStringByID("data_level", lang), l, true);
                m.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(f, lv, talent, t), false);
                m.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(f, lv, talent, t), true);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).subscribe();
    }
}
