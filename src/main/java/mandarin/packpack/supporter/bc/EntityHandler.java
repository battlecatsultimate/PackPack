package mandarin.packpack.supporter.bc;

import com.google.gson.JsonObject;
import common.CommonStatic;
import common.battle.data.MaskUnit;
import common.pack.PackData;
import common.system.fake.FakeImage;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.awt.FIBI;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.FormReactionHolder;
import mandarin.packpack.supporter.server.holder.StageReactionHolder;
import mandarin.packpack.supporter.server.slash.WebhookBuilder;

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
import java.util.Set;

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

    public static void showUnitEmb(Form f, WebhookBuilder builder, boolean isFrame, boolean talent, int[] lv, int lang) throws Exception {
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

        builder.addEmbed(spec -> {
            Color c;

            if(f.fid == 0)
                c = StaticStore.rainbow[4];
            else if(f.fid == 1)
                c = StaticStore.rainbow[3];
            else
                c = StaticStore.rainbow[2];

            int[] t;

            if(talent && f.getPCoin() != null) {
                t = f.getPCoin().max;
                t[0] = lv[0];
            } else
                t = null;

            if(t != null)
                t = handleTalent(lv, t);
            else
                t = new int[] {lv[0], 0, 0, 0, 0, 0};

            spec.setTitle(DataToString.getTitle(f, lang));

            if(talent && f.getPCoin() != null && talentExists(t)) {
                spec.setDescription(LangID.getStringByID("data_talent", lang));
            }

            spec.setColor(c);
            spec.setThumbnail("attachment://icon.png");
            spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(f.uid.id, f.fid), false);
            spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(f, talent, t), true);
            spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(f, talent, t), true);
            spec.addField(LangID.getStringByID("data_level", lang), l, true);
            spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(f, talent, t), false);
            spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(f, talent, t), true);
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

            if(f.getPCoin() != null)
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

            String explanation = DataToString.getDescription(f, lang);

            if(explanation != null)
                spec.addField(LangID.getStringByID("data_udesc", lang), explanation, false);

            String catfruit = DataToString.getCatruitEvolve(f, lang);

            if(catfruit != null)
                spec.addField(LangID.getStringByID("data_evolve", lang), catfruit, false);

            spec.setImage("attachment://cf.png");

            if(talentExists(t))
                spec.setFooter(DataToString.getTalent(f, t, lang), null);
        });

        if(fis != null)
            builder.addFile("icon.png", fis, img);
        if(cfis != null)
            builder.addFile("cf.png", cfis, cf);

        if(canFirstForm(f)) {
            builder.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.TWOPREVIOUS), "FirstForm", false));
        }

        if(canPreviousForm(f)) {
            builder.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.PREVIOUS), "PreviousForm", false));
        }

        if(canNextForm(f)) {
            builder.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.NEXT), "NextForm", false));
        }

        if(canFinalForm(f)) {
            builder.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.TWONEXT), "FinalForm", false));
        }
    }

    public static Message showUnitEmb(Form f, MessageChannel ch, boolean isFrame, boolean talent, int[] lv, int lang, boolean addEmoji) throws Exception {
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

        Message msg = ch.createMessage(m -> {
            m.setEmbed(spec -> {
                Color c;

                if(f.fid == 0)
                    c = StaticStore.rainbow[4];
                else if(f.fid == 1)
                    c = StaticStore.rainbow[3];
                else
                    c = StaticStore.rainbow[2];

                int[] t;

                if(talent && f.getPCoin() != null) {
                    t = f.getPCoin().max;
                    t[0] = lv[0];
                } else
                    t = null;

                if(t != null)
                    t = handleTalent(lv, t);
                else
                    t = new int[] {lv[0], 0, 0, 0, 0, 0};

                spec.setTitle(DataToString.getTitle(f, lang));

                if(talent && f.getPCoin() != null && talentExists(t)) {
                    spec.setDescription(LangID.getStringByID("data_talent", lang));
                }

                spec.setColor(c);
                spec.setThumbnail("attachment://icon.png");
                spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(f.uid.id, f.fid), false);
                spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(f, talent, t), true);
                spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(f, talent, t), true);
                spec.addField(LangID.getStringByID("data_level", lang), l, true);
                spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(f, talent, t), false);
                spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(f, talent, t), true);
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

                if(f.getPCoin() != null)
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

                String explanation = DataToString.getDescription(f, lang);

                if(explanation != null)
                    spec.addField(LangID.getStringByID("data_udesc", lang), explanation, false);

                String catfruit = DataToString.getCatruitEvolve(f, lang);

                if(catfruit != null)
                    spec.addField(LangID.getStringByID("data_evolve", lang), catfruit, false);

                spec.setImage("attachment://cf.png");

                if(talentExists(t))
                    spec.setFooter(DataToString.getTalent(f, t, lang), null);
            });
            if(fis != null)
                m.addFile("icon.png", fis);
            if(cfis != null)
                m.addFile("cf.png", cfis);
        }).block();

        if(msg != null && addEmoji) {
            if(canFirstForm(f)) {
                msg.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.TWOPREVIOUS), "FirstForm", false)).subscribe();
            }

            if(canPreviousForm(f)) {
                msg.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.PREVIOUS), "PreviousForm", false)).subscribe();
            }

            if(canNextForm(f)) {
                msg.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.NEXT), "NextForm", false)).subscribe();
            }

            if(canFinalForm(f)) {
                msg.addReaction(ReactionEmoji.custom(Snowflake.of(FormReactionHolder.TWONEXT), "FinalForm", false)).subscribe();
            }
        }

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

        f.anim.unload();

        return msg;
    }

    private static int[] handleTalent(int[] lv, int[] t) {
        int[] res = new int[6];

        res[0] = lv[0];

        for(int i = 0; i < t.length; i++) {
            if(i >= lv.length)
                res[i] = t[i];
            else
                res[i] = Math.min(t[i], lv[i]);

            if(res[i] < 0)
                res[i] = t[i];
        }

        return res;
    }

    private static boolean talentExists(int[] t) {
        boolean empty = true;

        for(int i = 1; i < t.length; i++) {
            empty &= t[i] == 0;
        }

        return !empty;
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

                spec.setTitle(DataToString.getTitle(e, lang));
                spec.setColor(c);
                spec.setThumbnail("attachment://icon.png");
                spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(e.id.id), false);
                spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(e, mag[0]), true);
                spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(e), true);
                spec.addField(LangID.getStringByID("data_magnif", lang), DataToString.getMagnification(mag, 100), true);
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

                String explanation = DataToString.getDescription(e, lang);

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

    public static void showEnemyEmb(Enemy e, WebhookBuilder w, boolean isFrame, int[] magnification, int lang) throws Exception {
        File img = generateIcon(e);

        FileInputStream fis;

        if(img != null)
            fis = new FileInputStream(img);
        else
            fis = null;

        w.addEmbed(spec -> {
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

            spec.setTitle(DataToString.getTitle(e, lang));
            spec.setColor(c);
            spec.setThumbnail("attachment://icon.png");
            spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(e.id.id), false);
            spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(e, mag[0]), true);
            spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(e), true);
            spec.addField(LangID.getStringByID("data_magnif", lang), DataToString.getMagnification(mag, 100), true);
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

            String explanation = DataToString.getDescription(e, lang);

            if(explanation != null) {
                spec.addField(LangID.getStringByID("data_edesc", lang), explanation, false);
            }

            spec.setFooter(LangID.getStringByID("enemyst_source", lang), null);
        });

        if(fis != null) {
            w.addFile("icon.png", fis, img);
        }

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

    public static Message showStageEmb(Stage st, MessageChannel ch, boolean isFrame, int star, int lang) throws Exception {
        StageMap stm = st.getCont();

        int sta;
        int stmMagnification;

        if(stm == null) {
            sta = 0;
            stmMagnification = 100;
        } else {
            sta = Math.min(Math.max(star-1, 0), st.getCont().stars.length-1);
            stmMagnification = stm.stars[sta];
        }

        File img = generateScheme(st, isFrame, lang, stmMagnification);
        FileInputStream fis;

        if(img != null) {
            fis = new FileInputStream(img);
        } else {
            fis = null;
        }

        Message result = ch.createMessage(m -> {
            m.setEmbed(spec -> {
                try {

                    if(st.info == null || st.info.diff == -1)
                        spec.setColor(Color.of(217, 217, 217));
                    else
                        spec.setColor(StaticStore.coolHot[st.info.diff]);

                    String name = "";

                    if(stm == null)
                        return;

                    MapColc mc = stm.getCont();

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String mcName = MultiLangCont.get(mc);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(mcName == null || mcName.isBlank())
                        mcName = mc.getSID();

                    name += mcName+" - ";

                    CommonStatic.getConfig().lang = lang;

                    String stmName = MultiLangCont.get(stm);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(stmName == null || stmName.isBlank())
                        if(stm.id != null)
                            stmName = Data.trio(stm.id.id);
                        else
                            stmName = "Unknown";

                    name += stmName + " - ";

                    CommonStatic.getConfig().lang = lang;

                    String stName = MultiLangCont.get(st);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(stName == null || stName.isBlank())
                        if(st.id != null)
                            stName = Data.trio(st.id.id);
                        else
                            stName = "Unknown";

                    name += stName;

                    spec.setTitle(name);
                    spec.addField(LangID.getStringByID("data_id", lang), DataToString.getStageCode(st), false);

                    String energy = DataToString.getEnergy(st, lang);

                    if(energy.endsWith("!!drink!!")) {
                        spec.addField(LangID.getStringByID("data_catamin", lang), energy.replace("!!drink!!", ""), true);
                    } else {
                        spec.addField(LangID.getStringByID("data_energy", lang), energy, true);
                    }

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
                        } else if(drops.endsWith("!!nofail!!")) {
                            spec.addField(LangID.getStringByID("data_chanrewardnofail", lang), drops.replace("!!nofail!!", ""), false);
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
        }).block();

        if(result != null) {
            result.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.CASTLE), "Castle", false)).subscribe();
            result.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.BG), "Background", false)).subscribe();

            if(st.mus0 != null) {
                result.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.MUSIC), "Music", false)).subscribe();
            }

            if(hasTwoMusic(st)) {
                result.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.MUSIC2), "MusicBoss", false)).subscribe();
            }

        }

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

        return result;
    }

    public static void showStageEmb(Stage st, WebhookBuilder builder, boolean isFrame, int star, int lang) throws Exception {
        StageMap stm = st.getCont();

        int sta;
        int stmMagnification;

        if(stm == null) {
            sta = 0;
            stmMagnification = 100;
        } else {
            sta = Math.min(Math.max(star-1, 0), st.getCont().stars.length-1);
            stmMagnification = stm.stars[sta];
        }

        File img = generateScheme(st, isFrame, lang, stmMagnification);
        FileInputStream fis;

        if(img != null) {
            fis = new FileInputStream(img);
        } else {
            fis = null;
        }

        builder.addEmbed(spec -> {
            try {

                if(st.info == null || st.info.diff == -1)
                    spec.setColor(Color.of(217, 217, 217));
                else
                    spec.setColor(StaticStore.coolHot[st.info.diff]);

                String name = "";

                if(stm == null)
                    return;

                MapColc mc = stm.getCont();

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String mcName = MultiLangCont.get(mc);

                CommonStatic.getConfig().lang = oldConfig;

                if(mcName == null || mcName.isBlank())
                    mcName = mc.getSID();

                name += mcName+" - ";

                CommonStatic.getConfig().lang = lang;

                String stmName = MultiLangCont.get(stm);

                CommonStatic.getConfig().lang = oldConfig;

                if(stmName == null || stmName.isBlank())
                    if(stm.id != null)
                        stmName = Data.trio(stm.id.id);
                    else
                        stmName = "Unknown";

                name += stmName + " - ";

                CommonStatic.getConfig().lang = lang;

                String stName = MultiLangCont.get(st);

                CommonStatic.getConfig().lang = oldConfig;

                if(stName == null || stName.isBlank())
                    if(st.id != null)
                        stName = Data.trio(st.id.id);
                    else
                        stName = "Unknown";

                name += stName;

                spec.setTitle(name);
                spec.addField(LangID.getStringByID("data_id", lang), DataToString.getStageCode(st), false);

                String energy = DataToString.getEnergy(st, lang);

                if(energy.endsWith("!!drink!!")) {
                    spec.addField(LangID.getStringByID("data_catamin", lang), energy.replace("!!drink!!", ""), true);
                } else {
                    spec.addField(LangID.getStringByID("data_energy", lang), energy, true);
                }

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
                    } else if(drops.endsWith("!!nofail!!")) {
                        spec.addField(LangID.getStringByID("data_chanrewardnofail", lang), drops.replace("!!nofail!!", ""), false);
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
            builder.addFile("scheme.png", fis, img);

        builder.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.CASTLE), "Castle", false));
        builder.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.BG), "Background", false));

        if(st.mus0 != null) {
            builder.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.MUSIC), "Music", false));
        }

        if(hasTwoMusic(st)) {
            builder.addReaction(ReactionEmoji.custom(Snowflake.of(StageReactionHolder.MUSIC2), "MusicBoss", false));
        }
    }

    private static File generateScheme(Stage st, boolean isFrame, int lang, int star) throws Exception {
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

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            SCDef.Line line = st.data.datas[i];

            AbEnemy enemy = line.enemy.get();

            if(enemy == null)
                continue;

            if(enemy instanceof Enemy) {
                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String eName = MultiLangCont.get(enemy);

                CommonStatic.getConfig().lang = oldConfig;

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

            String magnif = DataToString.getMagnification(new int[] {line.multiple, line.mult_atk}, star);

            magnifs.add(magnif);

            String boss;

            if(line.boss == 0)
                boss = "";
            else
                boss = LangID.getStringByID("data_boss", lang);

            isBoss.add(boss);

            String start;

            if(line.spawn_1 == 0)
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
            AbEnemy e = st.data.datas[st.data.datas.length - 1 - i].enemy.get();

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

    public static void generateFormImage(Form f, MessageChannel ch, int mode, int frame, boolean transparent, boolean debug, int lang) throws Exception {
        f.anim.load();

        if(mode >= f.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = f.anim.getEAnim(ImageDrawing.getAnimType(mode, f.anim.anims.length));

        File img = ImageDrawing.drawAnimImage(anim, frame, 1.0, transparent, debug);

        f.anim.unload();

        if(img != null) {
            FileInputStream fis = new FileInputStream(img);
            CommonStatic.getConfig().lang = lang;

            int finalMode = mode;

            ch.createMessage(m -> {
                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String fName = MultiLangCont.get(f);

                CommonStatic.getConfig().lang = oldConfig;

                if(fName == null || fName.isBlank())
                    fName = f.name;

                if(fName == null || fName.isBlank())
                    fName = LangID.getStringByID("data_unit", lang)+" "+ Data.trio(f.uid.id)+" "+Data.trio(f.fid);

                m.setContent(LangID.getStringByID("fimg_result", lang).replace("_", fName).replace(":::", getModeName(finalMode, f.anim.anims.length, lang)).replace("=", String.valueOf(frame)));
                m.addFile("result.png", fis);
            }).subscribe(null, null, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(img.exists()) {
                    boolean res = img.delete();

                    if(!res) {
                        System.out.println("Can't delete file : "+img.getAbsolutePath());
                    }
                }
            });
        }
    }

    public static void generateEnemyImage(Enemy en, MessageChannel ch, int mode, int frame, boolean transparent, boolean debug, int lang) throws Exception {
        en.anim.load();

        if(mode >= en.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = en.anim.getEAnim(ImageDrawing.getAnimType(mode, en.anim.anims.length));

        File img = ImageDrawing.drawAnimImage(anim, frame, 1.0, transparent, debug);

        en.anim.unload();

        if(img != null) {
            FileInputStream fis = new FileInputStream(img);
            CommonStatic.getConfig().lang = lang;

            int finalMode = mode;
            ch.createMessage(m -> {
                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String fName = MultiLangCont.get(en);

                CommonStatic.getConfig().lang = oldConfig;

                if(fName == null || fName.isBlank())
                    fName = en.name;

                if(fName == null || fName.isBlank())
                    fName = LangID.getStringByID("data_enemy", lang)+" "+ Data.trio(en.id.id);

                m.setContent(LangID.getStringByID("fimg_result", lang).replace("_", fName).replace(":::", getModeName(finalMode, en.anim.anims.length, lang)).replace("=", String.valueOf(frame)));
                m.addFile("result.png", fis);
            }).subscribe(null, null, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(img.exists()) {
                    boolean res = img.delete();

                    if(!res) {
                        System.out.println("Can't delete file : "+img.getAbsolutePath());
                    }
                }
            });
        }
    }

    private static String getModeName(int mode, int max, int lang) {
        switch (mode) {
            case 1:
                return LangID.getStringByID("fimg_idle", lang);
            case 2:
                return LangID.getStringByID("fimg_atk", lang);
            case 3:
                return LangID.getStringByID("fimg_hitback", lang);
            case 4:
                if(max == 5)
                    return LangID.getStringByID("fimg_enter", lang);
                else
                    return LangID.getStringByID("fimg_burrowdown", lang);
            case 5:
                return LangID.getStringByID("fimg_burrowmove", lang);
            case 6:
                return LangID.getStringByID("fimg_burrowup", lang);
            default:
                return LangID.getStringByID("fimg_walk", lang);
        }
    }

    public static boolean generateFormAnim(Form f, MessageChannel ch, int mode, boolean debug, int limit, int lang, boolean raw) throws Exception {
        if(f.unit == null || f.unit.id == null)
            return false;
        else if(!debug) {
            String id = generateID(f, mode);

            String link = StaticStore.imgur.get(id);
            boolean finalized = StaticStore.imgur.finalized(id);

            if(link != null) {
                if(!raw || finalized) {
                    ch.createMessage(LangID.getStringByID("gif_cache", lang).replace("_", link)).subscribe();
                    return false;
                }
            }
        }

        f.anim.load();

        if(mode >= f.anim.anims.length)
            mode = 0;

        if(limit > 0)  {
            ch.createMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", f.anim.len(getAnimType(mode, f.anim.anims.length))+"").replace("-", limit+"")).subscribe();
        } else if(!raw && f.anim.len(getAnimType(mode, f.anim.anims.length)) >= 300) {
            ch.createMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", f.anim.len(getAnimType(mode, f.anim.anims.length))+"").replace("-", 300+"")).subscribe();
        } else {
            ch.createMessage(LangID.getStringByID("gif_length", lang).replace("_", f.anim.len(getAnimType(mode, f.anim.anims.length))+"")).subscribe();
        }

        CommonStatic.getConfig().ref = false;

        if(mode >= f.anim.anims.length)
            mode = 0;

        Message msg = ch.createMessage(LangID.getStringByID("gif_anbox", lang)).block();

        if(msg == null)
            return false;

        long start = System.currentTimeMillis();

        f.anim.load();

        EAnimD<?> anim = f.getEAnim(getAnimType(mode, f.anim.anims.length));

        File img;

        if(raw) {
            img = ImageDrawing.drawAnimMp4(anim, msg, 1.0, debug, limit, lang);
        } else {
            img = ImageDrawing.drawAnimGif(anim, msg, 1.0, debug, false, limit, lang);
        }

        f.anim.unload();

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start)/1000.0);

        FileInputStream fis;

        if(img == null) {
            ch.createMessage(LangID.getStringByID("gif_faile", lang)).subscribe();
            return false;
        } else if(img.length() >= 8 * 1024 * 1024 && img.length() < (raw ? 200 * 1024 * 1024 : 10 * 1024 * 1024)) {
            Message m = ch.createMessage(LangID.getStringByID("gif_filesize", lang)).block();

            if(m == null) {
                ch.createMessage(LangID.getStringByID("gif_failcommand", lang)).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
                return false;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.edit(e -> e.setContent(LangID.getStringByID("gif_failimgur", lang))).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            } else {
                if(!debug && limit <= 0) {
                    String id = generateID(f, mode);

                    StaticStore.imgur.put(id, link, true);
                }

                m.edit(e -> {
                    long finalEnd = System.currentTimeMillis();

                    e.setContent(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link);
                }).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            }

            return true;
        } else if(img.length() < 8 * 1024 * 1024) {
            fis = new FileInputStream(img);

            int finalMode = mode;

            ch.createMessage(
                    m -> {
                        m.setContent(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)));
                        m.addFile(raw ? "result.mp4" : "result.gif", fis);
                    }
            ).subscribe(m -> {if(!debug && limit <= 0) cacheImage(f, finalMode, m);}, null, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(img.exists()) {
                    boolean res = img.delete();

                    if(!res) {
                        System.out.println("Can't delete file : "+img.getAbsolutePath());
                    }
                }
            });
        }

        return true;
    }

    public static boolean generateEnemyAnim(Enemy en, MessageChannel ch, int mode, boolean debug, int limit, int lang, boolean raw) throws Exception {
        if(en.id == null)
            return false;
        else if(!debug) {
            String id = generateID(en, mode);

            String link = StaticStore.imgur.get(id);
            boolean finalized = StaticStore.imgur.finalized(id);

            if(link != null) {
                if(!raw || finalized) {
                    ch.createMessage(LangID.getStringByID("gif_cache", lang).replace("_", link)).subscribe();
                    return false;
                }
            }
        }

        en.anim.load();

        if(mode >= en.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = en.getEAnim(getAnimType(mode, en.anim.anims.length));

        if(limit > 0)  {
            ch.createMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", anim.len()+"").replace("-", limit+"")).subscribe();
        } else if(!raw && anim.len() >= 300) {
            ch.createMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", anim.len()+"").replace("-", 300+"")).subscribe();
        } else {
            ch.createMessage(LangID.getStringByID("gif_length", lang).replace("_", anim.len()+"")).subscribe();
        }

        CommonStatic.getConfig().ref = false;

        if(mode >= en.anim.anims.length)
            mode = 0;

        Message msg = ch.createMessage(LangID.getStringByID("gif_anbox", lang)).block();

        if(msg == null)
            return false;

        long start = System.currentTimeMillis();

        File img;

        if(raw) {
            img = ImageDrawing.drawAnimMp4(anim, msg, 1.0, debug, limit, lang);
        } else {
            img = ImageDrawing.drawAnimGif(anim, msg, 1.0, debug, false, limit, lang);
        }

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start)/1000.0);

        FileInputStream fis;

        if(img == null) {
            ch.createMessage(LangID.getStringByID("gif_faile", lang)).subscribe();
            return false;
        } else if(img.length() >= 8 * 1024 * 1024 && img.length() < (raw ? 200 * 1024 * 1024 : 8 * 1024 * 1024)) {
            Message m = ch.createMessage(LangID.getStringByID("gif_filesize", lang)).block();

            if(m == null) {
                ch.createMessage(LangID.getStringByID("gif_failcommand", lang)).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
                return false;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.edit(e -> e.setContent(LangID.getStringByID("gif_failimgur", lang))).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            } else {
                if(!debug && limit <= 0) {
                    String id = generateID(en, mode);

                    StaticStore.imgur.put(id, link, true);
                }

                m.edit(e -> {
                    long finalEnd = System.currentTimeMillis();

                    e.setContent(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link);
                }).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            }

            return true;
        } else if(img.length() < 8 * 1024 * 1024) {
            fis = new FileInputStream(img);

            int finalMode = mode;

            ch.createMessage(
                    m -> {
                        m.setContent(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)));
                        m.addFile(raw ? "result.mp4" : "result.gif", fis);
                    }
            ).subscribe(m -> {if(!debug && limit <= 0) cacheImage(en, finalMode, m);}, null, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(img.exists()) {
                    boolean res = img.delete();

                    if(!res) {
                        System.out.println("Can't delete file : "+img.getAbsolutePath());
                    }
                }
            });
        }

        return true;
    }

    public static void generateAnim(MessageChannel ch, String md5, AnimMixer mixer, int lang, boolean debug, int limit, boolean raw, boolean transparent, int index) throws Exception {
        if(!debug && md5 != null) {
            String link = StaticStore.imgur.get(md5);
            boolean finalized = StaticStore.imgur.finalized(md5);

            if(link != null) {
                if(!raw || finalized) {
                    ch.createMessage(LangID.getStringByID("gif_cache", lang).replace("_", link)).subscribe();
                    return;
                }
            }
        }

        boolean mix = mixer.mix();

        if(!mix) {
            ch.createMessage("Failed to mix Anim").subscribe();
            return;
        }

        EAnimD<?> anim = mixer.getAnim(index);

        if(anim == null) {
            ch.createMessage("Failed to generate anim instance").subscribe();
            return;
        }

        ch.createMessage(LangID.getStringByID("gif_length", lang).replace("_", anim.len()+"")).subscribe();

        CommonStatic.getConfig().ref = false;

        Message msg = ch.createMessage(LangID.getStringByID("gif_anbox", lang)).block();

        if(msg == null)
            return;

        long start = System.currentTimeMillis();

        File img;

        if(raw) {
            img = ImageDrawing.drawAnimMp4(anim, msg, 1.0, debug, limit, lang);
        } else {
            img = ImageDrawing.drawAnimGif(anim, msg, 1.0, debug, transparent, lang, limit);
        }

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start)/1000.0);

        FileInputStream fis;

        if(img == null) {
            ch.createMessage(LangID.getStringByID("gif_faile", lang)).subscribe();
        } else if(img.length() >= 8 * 1024 * 1024) {
            Message m = ch.createMessage(LangID.getStringByID("gif_filesize", lang)).block();

            if(m == null) {
                ch.createMessage(LangID.getStringByID("gif_failcommand", lang)).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
                return;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.edit(e -> e.setContent(LangID.getStringByID("gif_failimgur", lang))).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            } else {
                if(!debug && md5 != null) {
                    StaticStore.imgur.put(md5, link, true);
                }

                m.edit(e -> {
                    long finalEnd = System.currentTimeMillis();

                    e.setContent(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link);
                }).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            }
        } else if(img.length() < 8 * 1024 * 1024) {
            fis = new FileInputStream(img);

            ch.createMessage(
                    m -> {
                        m.setContent(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)));
                        m.addFile(raw ? "result.mp4" : "result.gif", fis);
                    }
            ).subscribe(m -> {if(!debug) cacheImage(md5, m);}, null, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(img.exists()) {
                    boolean res = img.delete();

                    if(!res) {
                        System.out.println("Can't delete file : "+img.getAbsolutePath());
                    }
                }
            });
        }
    }

    public static boolean generateBCAnim(MessageChannel ch, AnimMixer mixer, int lang) throws Exception {
        boolean mix = mixer.mix();

        if(!mix) {
            ch.createMessage("Failed to mix Anim").subscribe();
            return false;
        }

        CommonStatic.getConfig().ref = false;

        Message msg = ch.createMessage(LangID.getStringByID("gif_anbox", lang)).block();

        if(msg == null)
            return false;

        long start = System.currentTimeMillis();

        File img = ImageDrawing.drawBCAnim(mixer, msg, 1.0, lang);

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start) / 1000.0);

        FileInputStream fis;

        if(img == null) {
            ch.createMessage(LangID.getStringByID("gif_faile", lang)).subscribe();
        } else if(img.length() >= 8 * 1024 * 1024) {
            Message m = ch.createMessage(LangID.getStringByID("gif_filesize", lang)).block();

            if(m == null) {
                ch.createMessage(LangID.getStringByID("gif_failcommand", lang)).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });

                return true;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.edit(e -> e.setContent(LangID.getStringByID("gif_failimgur", lang))).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            } else {
                m.edit(e -> {
                    long finalEnd = System.currentTimeMillis();

                    e.setContent(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link);
                }).subscribe(null, null, () -> {
                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            }
        } else if(img.length() < 8 * 1024 * 1024) {
            fis = new FileInputStream(img);

            ch.createMessage(
                    m -> {
                        m.setContent(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)));
                        m.addFile("result.mp4", fis);
                    }
            ).subscribe(null, null, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                boolean res = img.delete();

                if(!res) {
                    System.out.println("Can't delete file : "+img.getAbsolutePath());
                }
            });
        }

        return true;
    }

    private static String getFileSize(File f) {
        String[] unit = {"B", "KB", "MB"};

        double size = f.length();

        for (String s : unit) {
            if (size < 1024) {
                return DataToString.df.format(size) + s;
            } else {
                size /= 1024.0;
            }
        }

        return DataToString.df.format(size)+unit[2];
    }

    private static void cacheImage(Form f, int mode, Message msg) {
        if(f.unit == null || f.unit.id == null)
            return;

        String id = generateID(f, mode);

        Set<Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Attachment a : att) {
            if (a.getFilename().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, false);

                return;
            } else if(a.getFilename().equals("result.mp4")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, true);

                return;
            }
        }
    }

    public static void getFormSprite(Form f, MessageChannel ch, int mode, int lang) throws Exception {
        if(f.unit == null || f.unit.id == null) {
            ch.createMessage(LangID.getStringByID("fsp_cantunit", lang)).subscribe();
            return;
        }

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File image = new File("./temp/", StaticStore.findFileName(temp, "result", ".png"));

        if(!image.exists()) {
            boolean res = image.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+image.getAbsolutePath());
                return;
            }
        }

        f.anim.load();

        FakeImage img;

        switch (mode) {
            case 0:
                img = f.anim.getNum();
                break;
            case 1:
                img = f.anim.getUni().getImg();
                break;
            case 2:
                if(f.unit.getCont() instanceof PackData.DefPack) {
                    String code;

                    if(f.fid == 0)
                        code = "f";
                    else if(f.fid == 1)
                        code = "c";
                    else
                        code = "s";

                    VFile vf = VFile.get("./org/unit/"+Data.trio(f.unit.id.id)+"/"+code+"/udi"+Data.trio(f.unit.id.id)+"_"+code+".png");

                    if(vf != null) {
                        img = vf.getData().getImg();
                    } else {
                        img = null;
                    }
                } else {
                    img = null;
                }
                break;
            case 3:
                img = f.anim.getEdi().getImg();
                break;
            default:
                throw new IllegalStateException("Mode in sprite getter is incorrect : "+mode);
        }

        if(img == null) {
            ch.createMessage(LangID.getStringByID("fsp_nodata", lang).replace("_", getIconName(mode, lang))).subscribe();
            return;
        }

        BufferedImage result = (BufferedImage) img.bimg();

        ImageIO.write(result, "PNG", image);

        FileInputStream fis = new FileInputStream(image);

        ch.createMessage(m -> {
            String fName = StaticStore.safeMultiLangGet(f, lang);

            if(fName == null || fName.isBlank()) {
                fName = Data.trio(f.unit.id.id)+"-"+Data.trio(f.fid);
            }

            m.setContent(LangID.getStringByID("fsp_result", lang).replace("_", fName).replace("===", getIconName(mode, lang)));
            m.addFile("result.png", fis);
        }).subscribe(null, null, () -> {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(image.exists()) {
                boolean res = image.delete();

                if(!res) {
                    System.out.println("Can't delete file : "+image.getAbsolutePath());
                }
            }
        });

        f.anim.unload();
    }

    public static void getEnemySprite(Enemy e, MessageChannel ch, int mode, int lang) throws Exception {
        if(e.id == null) {
            ch.createMessage(LangID.getStringByID("esp_cantunit", lang)).subscribe();
            return;
        }

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File image = new File("./temp/", StaticStore.findFileName(temp, "result", ".png"));

        if(!image.exists()) {
            boolean res = image.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+image.getAbsolutePath());
                return;
            }
        }

        e.anim.load();

        FakeImage img;

        switch (mode) {
            case 0:
                img = e.anim.getNum();
                break;
            case 1:
                img = e.anim.getUni().getImg();
                break;
            case 3:
                img = e.anim.getEdi().getImg();
                break;
            default:
                throw new IllegalStateException("Mode in sprite getter is incorrect : "+mode);
        }

        if(img == null) {
            ch.createMessage(LangID.getStringByID("fsp_nodata", lang).replace("_", getIconName(mode, lang))).subscribe();
            return;
        }

        BufferedImage result = (BufferedImage) img.bimg();

        ImageIO.write(result, "PNG", image);

        FileInputStream fis = new FileInputStream(image);

        ch.createMessage(m -> {
            String fName = StaticStore.safeMultiLangGet(e, lang);

            if(fName == null || fName.isBlank()) {
                fName = Data.trio(e.id.id);
            }

            m.setContent(LangID.getStringByID("fsp_result", lang).replace("_", fName).replace("===", getIconName(mode, lang)));
            m.addFile("result.png", fis);
        }).subscribe(null, null, () -> {
            try {
                fis.close();
            } catch (IOException err) {
                err.printStackTrace();
            }

            if(image.exists()) {
                boolean res = image.delete();

                if(!res) {
                    System.out.println("Can't delete file : "+image.getAbsolutePath());
                }
            }
        });

        e.anim.unload();
    }

    public static void showMedalEmbed(int id, MessageChannel ch, int lang) throws  Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File image = new File("./temp", StaticStore.findFileName(temp, "result", ".png"));

        if(!image.exists()) {
            boolean res = image.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+image.getAbsolutePath());
                return;
            }
        }

        String medalName = "./org/page/medal/medal_"+Data.trio(id);

        if(id <= 13 && lang != 3) {
            medalName += "_"+getLocaleName(lang);
        }

        medalName += ".png";

        VFile vf = VFile.get(medalName);

        if(vf == null)
            ch.createMessage(LangID.getStringByID("medal_nopng", lang)).subscribe();
        else {
            BufferedImage img = (BufferedImage) vf.getData().getImg().bimg();

            ImageIO.write(img, "PNG", image);

            FileInputStream fis = new FileInputStream(image);

            ch.createMessage(m -> {
                m.setEmbed(e -> {
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String name = StaticStore.MEDNAME.getCont(id);
                    String desc = StaticStore.MEDEXP.getCont(id);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(StaticStore.medalData != null) {
                        JsonObject obj = StaticStore.medalData.getAsJsonArray().get(id).getAsJsonObject();

                        int grade = obj.get("grade").getAsInt();

                        e.setColor(StaticStore.grade[grade]);
                    }

                    e.addField(name, desc, false);
                    e.setImage("attachment://medal.png");
                });
                m.addFile("medal.png", fis);
            }).subscribe(null, null, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(image.exists()) {
                    boolean res = image.delete();

                    if(!res) {
                        System.out.println("Can't delete file : "+image.getAbsolutePath());
                    }
                }
            });
        }
    }

    public static void showComboEmbed(MessageChannel ch, Combo c, int lang) throws Exception {

        File icon = generateComboImage(c);

        FileInputStream fis;

        if(icon == null)
            fis = null;
        else
            fis = new FileInputStream(icon);

        ch.createMessage(m -> {
            m.setEmbed(e -> {
                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String comboName = MultiLangCont.getStatic().COMNAME.getCont(c.name);

                CommonStatic.getConfig().lang = oldConfig;

                if(comboName == null || comboName.isBlank()) {
                    comboName = "Combo "+c.name;
                }

                e.setTitle(comboName);

                if(c.lv == 0) {
                    e.setColor(StaticStore.rainbow[4]);
                } else if(c.lv == 1) {
                    e.setColor(StaticStore.rainbow[3]);
                } else if(c.lv == 2) {
                    e.setColor(StaticStore.rainbow[2]);
                } else {
                    e.setColor(StaticStore.rainbow[0]);
                }

                e.addField(DataToString.getComboType(c, lang), DataToString.getComboDescription(c, lang), false);

                if(fis != null) {
                    e.setImage("attachment://combo.png");
                }
            });

            if(fis != null)
                m.addFile("combo.png", fis);
        }).subscribe(null, null, () -> {
            try {
                if(fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(icon != null && icon.exists()) {
                boolean res = icon.delete();

                if(!res) {
                    System.out.println("Can't delete file : "+icon.getAbsolutePath());
                }
            }
        });
    }

    private static File generateComboImage(Combo c) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : " + temp.getAbsolutePath());
                return null;
            }
        }

        File image = new File("./temp", StaticStore.findFileName(temp, "combo", ".png"));

        if(!image.exists()) {
            boolean res = image.createNewFile();

            if(!res) {
                System.out.println("Can't create new file : "+image.getAbsolutePath());
                return null;
            }
        }

        BufferedImage img = new BufferedImage(600, 95, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(img.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        g.setColor(47, 49, 54, 255);
        g.fillRect(0, 0, 650, 105);

        g.setStroke(2f);
        g.setColor(230, 230, 230, 255);
        g.drawRect(0, 0, 600, 95);

        for(int i = 1; i < 5; i++) {
            g.drawLine(120 * i, 0, 120 * i, 95);
        }

        for(int i = 0; i < 5; i++) {
            if(c.units.get(i) == null)
                continue;

            Unit u = c.units.get(i).unit;

            if(u == null)
                continue;

            Form f = c.units.get(i);

            f.anim.load();

            FakeImage icon = f.anim.getUni().getImg();

            g.drawImage(icon, 120 * i + 5, 5);

            f.anim.unload();
        }

        ImageIO.write(img, "PNG", image);

        return image;
    }

    private static String getIconName(int mode, int lang) {
        if(mode == 0)
            return LangID.getStringByID("fsp_sprite", lang);
        else if(mode == 1)
            return LangID.getStringByID("fsp_uni", lang);
        else if(mode == 2)
            return LangID.getStringByID("fsp_udi", lang);
        else
            return LangID.getStringByID("fsp_edi", lang);
    }

    private static void cacheImage(Enemy e, int mode, Message msg) {
        if(e.id == null)
            return;

        String id = generateID(e, mode);

        Set<Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Attachment a : att) {
            if (a.getFilename().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, false);

                return;
            } else if(a.getFilename().equals("result.mp4")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, true);

                return;
            }
        }
    }

    private static void cacheImage(String md5, Message msg) {
        if(md5 == null)
            return;

        Set<Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Attachment a : att) {
            if (a.getFilename().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(md5, link, false);

                return;
            } else if(a.getFilename().equals("result.mp4")) {
                String link = a.getUrl();

                StaticStore.imgur.put(md5, link, true);

                return;
            }
        }
    }

    private static String generateID(Enemy e, int mode) {
        if(e.id == null)
            return "";

        return "E - "+e.id.pack+" - "+Data.trio(e.id.id)+" - "+Data.trio(mode);
    }

    private static String generateID(Form f, int mode) {
        if(f.unit == null || f.unit.id == null)
            return "";

        return "F - "+f.unit.id.pack+" - "+ Data.trio(f.unit.id.id)+" - "+Data.trio(f.fid) + " - " + Data.trio(mode);
    }

    private static AnimU.UType getAnimType(int mode, int max) {
        switch (mode) {
            case 1:
                return AnimU.UType.IDLE;
            case 2:
                return AnimU.UType.ATK;
            case 3:
                return AnimU.UType.HB;
            case 4:
                if(max == 5)
                    return AnimU.UType.ENTER;
                else
                    return AnimU.UType.BURROW_DOWN;
            case 5:
                return AnimU.UType.BURROW_MOVE;
            case 6:
                return AnimU.UType.BURROW_UP;
            default:
                return AnimU.UType.WALK;
        }
    }

    private static String getLocaleName(int lang) {
        if(lang == 0)
            return "en";
        else if(lang == 1)
            return "tw";
        else if(lang == 2)
            return "kr";
        else
            return "";
    }

    private static boolean hasTwoMusic(Stage st) {
        return st.mush != 0 && st.mush != 100 && st.mus1 != null && st.mus0 != null && st.mus1.id != st.mus0.id;
    }

    private static boolean canFirstForm(Form f) {
        return f.unit != null && f.fid - 2 >= 0;
    }

    private static boolean canPreviousForm(Form f) {
        return f.unit != null && f.fid - 1 >= 0;
    }

    private static boolean canNextForm(Form f) {
        return f.unit != null && f.fid + 1 < f.unit.forms.length;
    }

    private static boolean canFinalForm(Form f) {
        return f.unit != null && f.fid + 2 < f.unit.forms.length;
    }
}
