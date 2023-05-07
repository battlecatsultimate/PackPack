package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.system.fake.FakeTransform;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.pack.Background;
import common.util.pack.bgeffect.BackgroundEffect;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.info.DefStageInfo;
import common.util.unit.AbEnemy;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.awt.FIBI;
import mandarin.packpack.supporter.bc.cell.AbilityCellDrawer;
import mandarin.packpack.supporter.bc.cell.CellDrawer;
import mandarin.packpack.supporter.bc.cell.NormalCellDrawer;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lzw.AnimatedGifEncoder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.SystemUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class ImageDrawing {
    private static final int bgAnimTime = 450;
    private static final int bgAnimHeight = 720;
    private static final double bgAnimRatio = bgAnimHeight * 0.8 / 2 / 512.0;
    private static final int[] preferredBGAnimWidth = {
            5000, //star
            5000, //rain
            5000, //bubble
            5000, //falling snow
            5000, //snow
            5000, //snow star
            5000, //blizzard
            5000, //shining
            5000, //balloon
            5000, //rock
            5000, //102
            3900, //103
            4400, //110
            5000, //117
            3900, //121
            5400, //128
            4400, //132
            5400, //137
            3900, //141
            3900, //142
            5400, //145
            4600, //148
            3600, //153
            4400, //154
            4100, //155
            5400, //157
            5400, //158
            4700, //159
            6200, //164
            5400, //166
            4900, //172
            5200, //173
            4400, //174
            3900, //180
            3900, //181
            3900, //182
            4400, //183
            4400, //184
            6000, //191
            4000, //192
            6200, //193
            5000, //195
            5200, //196
            5400, //197
            4200, //198
            3900, //1000
            4600, //1002
            4900, //1003
            4400, //1004
            5400, //1005
            4900, //1006
            4400, //1007
            5400, //1008
            5000, //1009
            5200, //1010
            3000, //1011
            5100, //1012
            4400, //1013
            5000, //1014
            5000, //1015
            3400, //1016
            5400, //1017
            4500, //1018
            4400, //1019
            5400, //1020
            4200, //1021
            5300, //1022
            4600, //1023
            5400, //1024
            4400, //1025
            3600, //1026
            4900, //1027
            3600, //1029
            3600, //1030
            4900, //1031
            5100, //1032
            5400, //1033
            4400, //1034
            5400, //1035
            5400, //1036
            4400, //1037
            5400, //1038
            4000, //1039
            3600 //1040
    };

    private static Font titleFont;
    private static Font typeFont;
    private static Font nameFont;
    private static Font contentFont;
    private static Font levelFont;
    private static Font fruitFont;
    private static Font plotFont;

    private static final int statPanelMargin = 120;
    private static final int bgMargin = 80;
    private static final int nameMargin = 80;
    private static final int cornerRadius = 150;
    private static final int typeUpDownMargin = 28;
    private static final int typeLeftRightMargin = 66;
    private static final int levelMargin = 36;
    private static final int cellMargin = 110;
    private static final int enemyIconStroke = 15;
    private static final int enemyIconGap = 40;
    private static final int innerTableCornerRadius = 75;
    private static final int innerTableTextMargin = 100;
    private static final int innerTableCellMargin = 200;
    private static final int headerSeparatorHeight = 135;
    private static final int rewardIconSize = 160;
    private static final int lineSpace = 12;
    private static final int typeCornerRadius = 36;

    private static final float headerStroke = 4f;
    private static final float innerTableLineStroke = 3f;

    private static final int fruitGap = 60;
    private static final double fruitRatio = 0.125;
    private static final double fruitTextGapRatio = 0.025;
    private static final double fruitUpperGapRatio = 0.025;
    private static final double fruitDownerGapRatio = 0.05;
    private static final double enemyIconRatio = 1.25; // w/h
    private static final double enemyInnerIconRatio = 0.95;

    private static final int talentIconGap = 60;
    private static final int talentNameGap = 80;
    private static final int talentCostTableGap = 48;
    private static final int talentCostGap = 120;
    private static final int talentTableGap = 40;
    private static final int talentGap = 120;
    private static final int totalCostGap = 120;

    private static final int comboTitleGap = 90;
    private static final int comboTypeGap = 40;
    private static final int comboTypeInnerGap = 15;
    private static final int comboTypeRadius = 30;
    private static final double comboIconScaleFactor = 2.5;
    private static final int comboIconTableRadius = 75;
    private static final int comboIconGap = 60;
    private static final int comboIconLeftRightGap = 80;
    private static final int comboIconUpDownGap = 60;
    private static final int comboIconNameGap = 80;
    private static final int comboContentGap = 120;

    private static final int plotWidthHeight = 1024;
    private static final float axisStroke = 1.5f;
    private static final float indicatorStroke = 2f;
    private static final double indicatorRatio = 0.025;
    private static final float subIndicatorStroke = 1f;
    private static final int indicatorGap = 10;
    private static final float plotStroke = 3f;
    private static final double angleLimit = 89.9995;

    private static final int CHANCE_WIDTH = 0;
    private static final int REWARD_WIDTH = 1;
    private static final int AMOUNT_WIDTH = 2;
    private static final int TOTAL_WIDTH = 3;
    private static final int TOTAL_HEIGHT = 4;

    private static final int ENEMY = 0;
    private static final int NUMBER = 1;
    private static final int BASE = 2;
    private static final int MAGNIFICATION = 3;
    private static final int START = 4;
    private static final int LAYER = 5;
    private static final int RESPECT = 6;
    private static final int KILL = 7;
    private static final int BOSS = 8;
    private static final int STAGE_WIDTH = 9;
    private static final int STAGE_HEIGHT = 10;

    static {
        File regular = new File("./data/NotoRegular.otf");
        File medium = new File("./data/NotoMedium.otf");

        try {
            titleFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(144f);
            typeFont = Font.createFont(Font.TRUETYPE_FONT, regular).deriveFont(96f);
            nameFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(63f);
            contentFont = Font.createFont(Font.TRUETYPE_FONT, regular).deriveFont(84f);
            levelFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(96f);
            fruitFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(120f);
            plotFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(28f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File drawBGImage(Background bg, int w, int h, boolean eff) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null) {
            return null;
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(img.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        double groundRatio = 0.1;

        double ratio = h * (1.0 - groundRatio * 2) / 2.0 / 512.0;

        int groundHeight = (int) (groundRatio * h);

        bg.load();

        g.gradRect(0, h - groundHeight, w, groundHeight, 0, h - groundHeight, bg.cs[2], 0, h, bg.cs[3]);

        int pos = (int) ((-bg.parts[Background.BG].getWidth()+200) * ratio);

        int y = h - groundHeight;

        int lowHeight = (int) (bg.parts[Background.BG].getHeight() * ratio);
        int lowWidth = (int) (bg.parts[Background.BG].getWidth() * ratio);

        while(pos < w) {
            g.drawImage(bg.parts[Background.BG], pos, y - lowHeight, lowWidth, lowHeight);

            pos += Math.max(1, (int) (bg.parts[0].getWidth() * ratio));
        }

        if(bg.top) {
            int topHeight = (int) (bg.parts[Background.TOP].getHeight() * ratio);
            int topWidth = (int) (bg.parts[Background.TOP].getWidth() * ratio);

            pos = (int) ((-bg.parts[Background.BG].getWidth() + 200) * ratio);
            y = h - groundHeight - lowHeight;

            while(pos < w) {
                g.drawImage(bg.parts[Background.TOP], pos, y - topHeight, topWidth, topHeight);

                pos += Math.max(1, (int) (bg.parts[0].getWidth() * ratio));
            }

            if(y - topHeight > 0) {
                g.gradRect(0, 0, w, h - groundHeight - lowHeight - topHeight, 0, 0, bg.cs[0], 0, h - groundHeight - lowHeight - topHeight, bg.cs[1]);
            }
        } else {
            g.gradRect(0, 0, w, h - groundHeight - lowHeight, 0, 0, bg.cs[0], 0, h - groundHeight - lowHeight, bg.cs[1]);
        }

        if(eff && bg.effect != -1) {
            BackgroundEffect effect;

            if(bg.effect < 0) {
                effect = BackgroundEffect.mixture.get(-bg.effect);
            } else {
                effect = CommonStatic.getBCAssets().bgEffects.get(bg.effect);
            }

            System.out.println(bg.effect);

            int len = (int) ((w / ratio - 400) / CommonStatic.BattleConst.ratio);
            int bgHeight = (int) (h / ratio);
            int midH = (int) (h * groundRatio / ratio);

            effect.initialize(len, bgHeight, midH, bg);

            for(int i = 0; i < 30; i++) {
                effect.update(len, bgHeight, midH);
            }

            P base = P.newP((int) (h * 0.0025), (int) (BackgroundEffect.BGHeight * 3 * ratio - h * 0.905));

            effect.preDraw(g, base, ratio, midH);
            effect.postDraw(g, base, ratio, midH);

            P.delete(base);
        }

        if(eff && bg.overlay != null) {
            g.gradRectAlpha(0, 0, w, h, 0, 0, bg.overlayAlpha, bg.overlay[1], 0, h, bg.overlayAlpha, bg.overlay[0]);
        }

        ImageIO.write(img, "PNG", image);

        return image;
    }

    public static File drawBGAnimEffect(Background bg, Message msg, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());
            return null;
        }

        File folder = StaticStore.generateTempFile(temp, "images", "", true);

        if(folder == null) {
            return null;
        }

        String folderName = folder.getName();

        File mp4 = StaticStore.generateTempFile(temp, "result", ".mp4", false);

        if(mp4 == null) {
            return null;
        }

        int h = bgAnimHeight;

        int pw;

        if(bg.effect < 0)
            pw = preferredBGAnimWidth[handleMixedBGEffect(bg.effect)];
        else
            pw = preferredBGAnimWidth[bg.effect];

        int w = (int) ((400 + pw * CommonStatic.BattleConst.ratio) * bgAnimRatio);

        if(w % 2 == 1)
            w -= 1;

        double groundRatio = 0.1;

        int groundHeight = (int) (groundRatio * h);

        BackgroundEffect eff;

        if(bg.effect < 0)
            eff = BackgroundEffect.mixture.get(-bg.effect);
        else
            eff = CommonStatic.getBCAssets().bgEffects.get(bg.effect);

        int len = (int) ((w / bgAnimRatio - 400) / CommonStatic.BattleConst.ratio);
        int bgHeight = (int) (h / bgAnimRatio);
        int midH = (int) (h * groundRatio / bgAnimRatio);

        eff.initialize(len, bgHeight, midH, bg);

        String cont = LangID.getStringByID("bg_dimen", lang).replace("_WWW_", String.valueOf(w)).replace("_HHH_", bgAnimHeight+"") +"\n\n"+
                LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", "  0")
                        .replace("_LLL_", bgAnimTime + "")
                        .replace("_BBB_", getProgressBar(0, bgAnimTime))
                        .replace("_VVV_", 0.0 + "")
                        .replace("_SSS_", "-");

        msg.editMessage(cont).queue();

        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();
        final int finalW = w;

        for(int i = 0; i < bgAnimTime; i++) {
            if(System.currentTimeMillis() - current >= 1500) {
                String prog = DataToString.df.format(i * 100.0 / bgAnimTime);
                String eta = getETA(start, System.currentTimeMillis(), i, bgAnimTime);
                String ind = String.valueOf(i);
                String content = LangID.getStringByID("bg_dimen", lang).replace("_WWW_", String.valueOf(finalW)).replace("_HHH_", bgAnimHeight+"") +"\n\n"+
                        LangID.getStringByID("bg_prog", lang)
                                .replace("_PPP_", " ".repeat(Math.max(0, 3 - ind.length()))+ind)
                                .replace("_LLL_", bgAnimTime+"")
                                .replace("_BBB_", getProgressBar(i, bgAnimTime))
                                .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length()))+prog)
                                .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length()))+eta);

                msg.editMessage(content).queue();

                current = System.currentTimeMillis();
            }

            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            FG2D g = new FG2D(image.getGraphics());

            g.setRenderingHint(3, 2);
            g.enableAntialiasing();

            bg.load();

            g.gradRect(0, h - groundHeight, w, groundHeight, 0, h - groundHeight, bg.cs[2], 0, h, bg.cs[3]);

            int pos = (int) ((-bg.parts[Background.BG].getWidth()+200) * bgAnimRatio);

            int y = h - groundHeight;

            int lowHeight = (int) (bg.parts[Background.BG].getHeight() * bgAnimRatio);
            int lowWidth = (int) (bg.parts[Background.BG].getWidth() * bgAnimRatio);

            while(pos < w) {
                g.drawImage(bg.parts[Background.BG], pos, y - lowHeight, lowWidth, lowHeight);

                pos += Math.max(1, (int) (bg.parts[0].getWidth() * bgAnimRatio));
            }

            if(bg.top) {
                int topHeight = (int) (bg.parts[Background.TOP].getHeight() * bgAnimRatio);
                int topWidth = (int) (bg.parts[Background.TOP].getWidth() * bgAnimRatio);

                pos = (int) ((-bg.parts[Background.BG].getWidth() + 200) * bgAnimRatio);
                y = h - groundHeight - lowHeight;

                while(pos < w) {
                    g.drawImage(bg.parts[Background.TOP], pos, y - topHeight, topWidth, topHeight);

                    pos += Math.max(1, (int) (bg.parts[0].getWidth() * bgAnimRatio));
                }

                if(y - topHeight > 0) {
                    g.gradRect(0, 0, w, h - groundHeight - lowHeight - topHeight, 0, 0, bg.cs[0], 0, h - groundHeight - lowHeight - topHeight, bg.cs[1]);
                }
            } else {
                g.gradRect(0, 0, w, h - groundHeight - lowHeight, 0, 0, bg.cs[0], 0, h - groundHeight - lowHeight, bg.cs[1]);
            }

            P base = P.newP((int) (h * 0.0025), (int) (BackgroundEffect.BGHeight * 3 * bgAnimRatio - h * 0.905));

            eff.preDraw(g, base, bgAnimRatio, midH);
            eff.postDraw(g, base, bgAnimRatio, midH);

            P.delete(base);

            if(bg.overlay != null) {
                g.gradRectAlpha(0, 0, w, h, 0, 0, bg.overlayAlpha, bg.overlay[1], 0, h, bg.overlayAlpha, bg.overlay[0]);
            }

            File img = new File("./temp/"+folderName+"/", quad(i)+".png");

            if(!img.exists() && !img.createNewFile()) {
                StaticStore.logger.uploadLog("Can't create file : "+img.getAbsolutePath());
                return null;
            }

            ImageIO.write(image, "PNG", img);

            eff.update(len, bgHeight, midH);
        }

        String content = LangID.getStringByID("bg_dimen", lang).replace("_WWW_", String.valueOf(finalW)).replace("_HHH_", bgAnimHeight+"") +"\n\n"+
                LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", bgAnimTime + "")
                        .replace("_LLL_", bgAnimTime + "")
                        .replace("_BBB_", getProgressBar(bgAnimTime, bgAnimTime))
                        .replace("_VVV_", "100.00")
                        .replace("_SSS_", "     0") + "\n"+
                LangID.getStringByID("bg_upload", lang);

        msg.editMessage(content).queue();

        ProcessBuilder builder = new ProcessBuilder(SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg", "-r", "30", "-f", "image2", "-s", w+"x"+h,
                "-i", "temp/"+folderName+"/%04d.png", "-vcodec", "libx264", "-crf", "25", "-pix_fmt", "yuv420p", "-y", "temp/"+mp4.getName());
        builder.redirectErrorStream(true);

        Process pro = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

        String line;

        while((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        pro.waitFor();

        StaticStore.deleteFile(folder, true);

        return mp4;
    }

    public static File drawAnimImage(EAnimD<?> anim, int frame, double siz, boolean transparent, boolean debug) throws Exception {
        CommonStatic.getConfig().ref = false;

        anim.setTime(frame);

        Rectangle rect = new Rectangle();

        ArrayList<int[][]> rects = new ArrayList<>();
        ArrayList<P> centers = new ArrayList<>();

        for(int i = 0; i < anim.getOrder().length; i++) {
            if(anim.anim().parts(anim.getOrder()[i].getVal(2)) == null || anim.getOrder()[i].getVal(1) == -1)
                continue;

            FakeImage fi = anim.anim().parts(anim.getOrder()[i].getVal(2));

            if(fi.getHeight() == 1 && fi.getWidth() == 1)
                continue;

            RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

            getter.apply(anim.getOrder()[i], siz, false);

            int[][] result = getter.getRect();

            if(Math.abs(result[1][0]-result[0][0]) >= 1000 || Math.abs(result[1][1] - result[2][1]) >= 1000)
                continue;

            rects.add(result);
            centers.add(getter.center);

            int oldX = rect.x;
            int oldY = rect.y;

            rect.x = Math.min(minAmong(result[0][0], result[1][0], result[2][0], result[3][0]), rect.x);
            rect.y = Math.min(minAmong(result[0][1], result[1][1], result[2][1], result[3][1]), rect.y);

            if(oldX != rect.x) {
                rect.width += oldX - rect.x;
            }

            if(oldY != rect.y) {
                rect.height += oldY - rect.y;
            }

            rect.width = Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width);
            rect.height = Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height);
        }

        if(rect.width == 0)
            rect.width = 2;

        if(rect.height == 0)
            rect.height = 2;

        if(rect.width % 2 == 1)
            rect.width++;

        if(rect.height % 2 == 1)
            rect.height++;

        BufferedImage result = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
        FG2D rg = new FG2D(result.getGraphics());

        rg.setRenderingHint(3, 1);
        rg.enableAntialiasing();

        if(!transparent) {
            rg.setColor(54,57,63,255);
            rg.fillRect(0, 0, rect.width, rect.height);
        }

        FakeTransform t = rg.getTransform();

        anim.draw(rg, new P(-rect.x, -rect.y), siz);

        rg.setTransform(t);

        rg.setStroke(1.5f);

        if(debug) {
            for(int i = 0; i < rects.size(); i++) {
                int[][] res = rects.get(i);

                rg.setColor(FakeGraphics.RED);

                rg.drawLine(-rect.x + res[0][0], -rect.y + res[0][1], -rect.x + res[1][0], -rect.y + res[1][1]);
                rg.drawLine(-rect.x + res[1][0], -rect.y + res[1][1], -rect.x + res[2][0], -rect.y + res[2][1]);
                rg.drawLine(-rect.x + res[2][0], -rect.y + res[2][1], -rect.x + res[3][0], -rect.y + res[3][1]);
                rg.drawLine(-rect.x + res[3][0], -rect.y + res[3][1], -rect.x + res[0][0], -rect.y + res[0][1]);

                rg.setColor(0, 255, 0, 255);

                rg.fillRect(-rect.x + (int) centers.get(i).x - 2, -rect.y + (int) centers.get(i).y -2, 4, 4);
            }
        }

        File temp = new File("./temp");
        File file = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(file == null) {
            return null;
        }

        ImageIO.write(result, "PNG", file);


        return file;
    }

    public static File drawAnimMp4(EAnimD<?> anim, Message msg, double siz, boolean debug, int limit, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File folder = StaticStore.generateTempFile(temp, "images", "", true);

        if(folder == null) {
            return null;
        }

        String folderName = folder.getName();

        File gif = StaticStore.generateTempFile(temp, "result", ".mp4", false);

        if(gif == null) {
            return null;
        }

        CommonStatic.getConfig().ref = false;

        anim.setTime(0);

        int frame = Math.min(anim.len(), limit);

        if(frame <= 0)
            frame = anim.len();

        Rectangle rect = new Rectangle();

        ArrayList<ArrayList<int[][]>> rectFrames = new ArrayList<>();
        ArrayList<ArrayList<P>> centerFrames = new ArrayList<>();

        for(int i = 0; i < frame; i++) {
            anim.setTime(i);

            ArrayList<int[][]> rects = new ArrayList<>();
            ArrayList<P> centers = new ArrayList<>();

            for(int j = 0; j < anim.getOrder().length; j++) {
                if(anim.anim().parts(anim.getOrder()[j].getVal(2)) == null || anim.getOrder()[j].getVal(1) == -1)
                    continue;

                FakeImage fi = anim.anim().parts(anim.getOrder()[j].getVal(2));

                if(fi.getWidth() == 1 && fi.getHeight() == 1)
                    continue;

                RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

                getter.apply(anim.getOrder()[j], siz, false);

                int[][] result = getter.getRect();

                if(Math.abs(result[1][0]-result[0][0]) * Math.abs(result[1][1] - result[2][1]) >= (750 * siz) * (750 * siz))
                    continue;

                rects.add(result);
                centers.add(getter.center);

                int oldX = rect.x;
                int oldY = rect.y;

                rect.x = Math.min(minAmong(result[0][0], result[1][0], result[2][0], result[3][0]), rect.x);
                rect.y = Math.min(minAmong(result[0][1], result[1][1], result[2][1], result[3][1]), rect.y);

                if(oldX != rect.x) {
                    rect.width += oldX - rect.x;
                }

                if(oldY != rect.y) {
                    rect.height += oldY - rect.y;
                }

                rect.width = Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width);
                rect.height = Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height);
            }

            rectFrames.add(rects);
            centerFrames.add(centers);
        }

        double ratio;

        String cont = LangID.getStringByID("gif_anbox", lang)+ "\n"
                + LangID.getStringByID("gif_result", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x));

        msg.editMessage(cont).queue();

        if(rect.width * rect.height > 1500 * 1500) {
            ratio = 1.0;

            while(rect.width * rect.height > 1500 * 1500) {
                ratio *= 0.5;

                rect.width = (int) (0.5 * rect.width);
                rect.height = (int) (0.5 * rect.height);
                rect.x = (int) (0.5 * rect.x);
                rect.y = (int) (0.5 * rect.y);
            }
        } else {
            ratio = 1.0;
        }

        String finCont = cont+"\n\n";

        if(ratio == 1.0) {
            finCont += LangID.getStringByID("gif_cango", lang)+"\n\n";
        } else {
            finCont += LangID.getStringByID("gif_adjust", lang).replace("_", DataToString.df.format(ratio * 100.0))+"\n\n";
        }

        if(rect.height % 2 == 1) {
            rect.height -= 1;
            rect.y += 1;
        }

        if(rect.width % 2 == 1) {
            rect.width -= 1;
            rect.x += 1;
        }

        if(rect.width == 0)
            rect.width = 2;

        if(rect.height == 0)
            rect.height = 2;

        finCont += LangID.getStringByID("gif_final", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x))+"\n";

        msg.editMessage(finCont).queue();

        P pos = new P(-rect.x, -rect.y);

        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();

        for(int i = 0; i < frame; i++) {
            if(System.currentTimeMillis() - current >= 1500) {
                String content = finCont +"\n\n";

                String prog = DataToString.df.format(i * 100.0 / frame);
                String eta = getETA(start, System.currentTimeMillis(), i, frame);
                String ind = String.valueOf(i);
                String len = String.valueOf(frame);

                content += LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", " ".repeat(Math.max(0, len.length() - ind.length()))+ind)
                        .replace("_LLL_", len)
                        .replace("_BBB_", getProgressBar(i, frame))
                        .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length()))+prog)
                        .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length()))+eta);

                msg.editMessage(content).queue();

                current = System.currentTimeMillis();
            }

            anim.setTime(i);

            BufferedImage image = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
            FG2D g = new FG2D(image.getGraphics());

            g.setRenderingHint(3, 2);
            g.enableAntialiasing();

            g.setStroke(1.5f);

            g.setColor(54,57,63,255);
            g.fillRect(0, 0, rect.width, rect.height);

            if(debug) {
                for(int j = 0; j < rectFrames.get(i).size(); j++) {
                    int[][] r = rectFrames.get(i).get(j);
                    P c = centerFrames.get(i).get(j);

                    g.setColor(FakeGraphics.RED);

                    g.drawLine(-rect.x + (int) (ratio * r[0][0]), -rect.y + (int) (ratio * r[0][1]), -rect.x + (int) (ratio * r[1][0]), -rect.y + (int) (ratio * r[1][1]));
                    g.drawLine(-rect.x + (int) (ratio * r[1][0]), -rect.y + (int) (ratio * r[1][1]), -rect.x + (int) (ratio * r[2][0]), -rect.y + (int) (ratio * r[2][1]));
                    g.drawLine(-rect.x + (int) (ratio * r[2][0]), -rect.y + (int) (ratio * r[2][1]), -rect.x + (int) (ratio * r[3][0]), -rect.y + (int) (ratio * r[3][1]));
                    g.drawLine(-rect.x + (int) (ratio * r[3][0]), -rect.y + (int) (ratio * r[3][1]), -rect.x + (int) (ratio * r[0][0]), -rect.y + (int) (ratio * r[0][1]));

                    g.setColor(0, 255, 0, 255);

                    g.fillRect(-rect.x + (int) (c.x * ratio) - 2, -rect.y + (int) (c.y * ratio) -2, 4, 4);
                }
            } else {
                anim.draw(g, pos, siz * ratio);
            }

            File img = new File("./temp/"+folderName+"/", quad(i)+".png");

            if(!img.exists()) {
                boolean res = img.createNewFile();

                if(!res) {
                    System.out.println("Can't create new file : "+img.getAbsolutePath());
                    return null;
                }
            } else {
                return null;
            }

            ImageIO.write(image, "PNG", img);
        }

        String content = finCont + "\n\n" + LangID.getStringByID("gif_makepng", lang).replace("_", "100")
                +"\n\n"+ LangID.getStringByID("gif_converting", lang);

        msg.editMessage(content).queue();

        ProcessBuilder builder = new ProcessBuilder(SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg", "-r", "30", "-f", "image2", "-s", rect.width+"x"+rect.height,
                "-i", "temp/"+folderName+"/%04d.png", "-vcodec", "libx264", "-crf", "25", "-pix_fmt", "yuv420p", "-y", "temp/"+gif.getName());
        builder.redirectErrorStream(true);

        Process pro = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

        String line;

        while((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        pro.waitFor();

        StaticStore.deleteFile(folder, true);

        content = finCont + "\n\n" +
                LangID.getStringByID("gif_makepng", lang).replace("_", "100") + "\n\n" +
                LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", String.valueOf(frame))
                        .replace("_LLL_", String.valueOf(frame))
                        .replace("_BBB_", getProgressBar(frame, frame))
                        .replace("_VVV_", "100.00")
                        .replace("_SSS_", "     0") + "\n" +
                LangID.getStringByID("gif_uploadmp4", lang);

        msg.editMessage(content).queue();

        return gif;
    }

    public static File drawAnimGif(EAnimD<?> anim, Message msg, double siz, boolean debug, boolean transparent, int limit, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File gif = StaticStore.generateTempFile(temp, "result", ".gif", false);

        if(gif == null) {
            return null;
        }

        CommonStatic.getConfig().ref = false;

        anim.setTime(0);

        int frame = Math.min(anim.len(), limit);

        if(frame <= 0)
            frame = anim.len();

        Rectangle rect = new Rectangle();

        ArrayList<ArrayList<int[][]>> rectFrames = new ArrayList<>();
        ArrayList<ArrayList<P>> centerFrames = new ArrayList<>();

        for(int i = 0; i < Math.min(frame, 300); i++) {
            anim.setTime(i);

            ArrayList<int[][]> rects = new ArrayList<>();
            ArrayList<P> centers = new ArrayList<>();

            for(int j = 0; j < anim.getOrder().length; j++) {
                if(anim.anim().parts(anim.getOrder()[j].getVal(2)) == null || anim.getOrder()[j].getVal(1) == -1)
                    continue;

                FakeImage fi = anim.anim().parts(anim.getOrder()[j].getVal(2));

                if(fi.getWidth() == 1 && fi.getHeight() == 1)
                    continue;

                RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

                getter.apply(anim.getOrder()[j], siz * 0.5, false);

                int[][] result = getter.getRect();

                if(Math.abs(result[1][0]-result[0][0]) >= (1000 * siz * 0.5) || Math.abs(result[1][1] - result[2][1]) >= (1000 * siz * 0.5))
                    continue;

                rects.add(result);
                centers.add(getter.center);

                int oldX = rect.x;
                int oldY = rect.y;

                rect.x = Math.min(minAmong(result[0][0], result[1][0], result[2][0], result[3][0]), rect.x);
                rect.y = Math.min(minAmong(result[0][1], result[1][1], result[2][1], result[3][1]), rect.y);

                if(oldX != rect.x) {
                    rect.width += oldX - rect.x;
                }

                if(oldY != rect.y) {
                    rect.height += oldY - rect.y;
                }

                rect.width = Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width);
                rect.height = Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height);
            }

            rectFrames.add(rects);
            centerFrames.add(centers);
        }

        int minSize = 300;

        double ratio;

        long surface = (long) rect.width * rect.height;

        if(surface > minSize * minSize) {
            ratio = (surface - (surface - 300 * 300) * 1.0 * Math.min(300, frame) / 300.0) / surface;
        } else {
            ratio = 1.0;
        }

        String cont = LangID.getStringByID("gif_anbox", lang)+ "\n"
                + LangID.getStringByID("gif_result", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x))+"\n";

        if(ratio != 1.0) {
            cont += LangID.getStringByID("gif_adjust", lang).replace("_", DataToString.df.format(ratio * 100.0))+"\n";
        } else {
            cont += LangID.getStringByID("gif_cango", lang)+"\n";
        }

        cont += LangID.getStringByID("gif_final", lang).replace("_WWW_", String.valueOf((int) (ratio * rect.width)))
                .replace("_HHH_", String.valueOf((int) (ratio * rect.height))).replace("_XXX_", String.valueOf((int) (ratio * rect.x)))
                .replace("_YYY_", String.valueOf((int) (ratio * rect.y)));

        msg.editMessage(cont).queue();

        rect.x = (int) (ratio * rect.x);
        rect.y = (int) (ratio * rect.y);
        rect.width = (int) (ratio * rect.width);
        rect.height = (int) (ratio* rect.height);

        if(rect.width == 0)
            rect.width = 2;

        if(rect.height == 0)
            rect.height = 2;

        AnimatedGifEncoder encoder = new AnimatedGifEncoder();

        encoder.setSize(rect.width, rect.height);
        encoder.setFrameRate(30);
        encoder.setRepeat(0);

        if(transparent)
            encoder.setTransparent(new Color(54, 57, 63, 255));

        FileOutputStream fos = new FileOutputStream(gif);

        encoder.start(fos);

        P pos = new P(-rect.x, -rect.y);

        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();

        for(int i = 0; i < Math.min(frame, 300); i++) {
            if(System.currentTimeMillis() - current >= 1000) {
                String content = cont +"\n\n";

                String prog = DataToString.df.format(i * 100.0 / frame);
                String eta = getETA(start, System.currentTimeMillis(), i, frame);
                String ind = String.valueOf(i);
                String len = String.valueOf(frame);

                content += LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", " ".repeat(Math.max(0, len.length() - ind.length()))+ind)
                        .replace("_LLL_", len)
                        .replace("_BBB_", getProgressBar(i, frame))
                        .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length()))+prog)
                        .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length()))+eta);

                msg.editMessage(content).queue();

                current = System.currentTimeMillis();
            }

            anim.setTime(i);

            BufferedImage image = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
            FG2D g = new FG2D(image.getGraphics());

            g.setRenderingHint(3, 2);
            g.enableAntialiasing();

            g.setStroke(1.5f);

            g.setColor(54,57,63,255);
            g.fillRect(0, 0, rect.width, rect.height);

            if(debug) {
                for(int j = 0; j < rectFrames.get(i).size(); j++) {
                    int[][] r = rectFrames.get(i).get(j);
                    P c = centerFrames.get(i).get(j);

                    g.setColor(FakeGraphics.RED);

                    g.drawLine(-rect.x + (int) (r[0][0] * ratio), -rect.y + (int) (r[0][1] * ratio), -rect.x + (int) (r[1][0] * ratio), -rect.y + (int) (r[1][1] * ratio));
                    g.drawLine(-rect.x + (int) (r[1][0] * ratio), -rect.y + (int) (r[1][1] * ratio), -rect.x + (int) (r[2][0] * ratio), -rect.y + (int) (r[2][1] * ratio));
                    g.drawLine(-rect.x + (int) (r[2][0] * ratio), -rect.y + (int) (r[2][1] * ratio), -rect.x + (int) (r[3][0] * ratio), -rect.y + (int) (r[3][1] * ratio));
                    g.drawLine(-rect.x + (int) (r[3][0] * ratio), -rect.y + (int) (r[3][1] * ratio), -rect.x + (int) (r[0][0] * ratio), -rect.y + (int) (r[0][1] * ratio));

                    g.setColor(0, 255, 0, 255);

                    g.fillRect(-rect.x + (int) (ratio * c.x) - 2, -rect.y + (int) (ratio * c.y) -2, 4, 4);
                }
            } else {
                anim.setTime(i);

                anim.draw(g, pos, siz * ratio * 0.5);
            }

            encoder.addFrame(image);
        }

        encoder.finish();

        fos.close();

        String content = cont + "\n\n"+
                LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", String.valueOf(frame))
                        .replace("_LLL_", String.valueOf(frame))
                        .replace("_BBB_", getProgressBar(frame, frame))
                        .replace("_VVV_", "100.00")
                        .replace("_SSS_", "     0") + "\n"+
                LangID.getStringByID("gif_uploading", lang);

        msg.editMessage(content).queue();

        return gif;
    }

    public static File drawBCAnim(AnimMixer mixer, Message msg, double siz, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File folder = StaticStore.generateTempFile(temp, "images", "", true);

        if(folder == null) {
            return null;
        }

        String folderName = folder.getName();

        File gif = StaticStore.generateTempFile(temp, "result", ".mp4", false);

        if(gif == null) {
            return null;
        }

        CommonStatic.getConfig().ref = false;

        Rectangle rect = new Rectangle();

        for(int i = 0; i < mixer.anim.length; i++) {
            EAnimD<?> anim = mixer.getAnim(i);

            if(anim != null) {
                anim.setTime(0);

                for(int j = 0; j < anim.len(); j++) {
                    anim.setTime(j);

                    for(int k = 0; k < anim.getOrder().length; k++) {
                        if(anim.anim().parts(anim.getOrder()[k].getVal(2)) == null || anim.getOrder()[k].getVal(1) == -1)
                            continue;

                        FakeImage fi = anim.anim().parts(anim.getOrder()[k].getVal(2));

                        if(fi.getWidth() == 1 && fi.getHeight() == 1)
                            continue;

                        RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

                        getter.apply(anim.getOrder()[k], siz, false);

                        int[][] result = getter.getRect();

                        if(Math.abs(result[1][0]-result[0][0]) * Math.abs(result[1][1] - result[2][1]) >= (750 * siz) * (750 * siz))
                            continue;

                        int oldX = rect.x;
                        int oldY = rect.y;

                        rect.x = Math.min(minAmong(result[0][0], result[1][0], result[2][0], result[3][0]), rect.x);
                        rect.y = Math.min(minAmong(result[0][1], result[1][1], result[2][1], result[3][1]), rect.y);

                        if(oldX != rect.x) {
                            rect.width += oldX - rect.x;
                        }

                        if(oldY != rect.y) {
                            rect.height += oldY - rect.y;
                        }

                        rect.width = Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width);
                        rect.height = Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height);
                    }
                }
            }
        }

        double ratio;

        String cont = LangID.getStringByID("gif_anbox", lang) + "\n"
                + LangID.getStringByID("gif_result", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.y));

        msg.editMessage(cont).queue();

        if(rect.width * rect.height > 1500 * 1500) {
            ratio = 1.0;

            while(rect.width * rect.height > 1500 * 1500) {
                ratio *= 0.5;

                rect.width = (int) (0.5 * rect.width);
                rect.height = (int) (0.5 * rect.height);
                rect.x = (int) (0.5 * rect.x);
                rect.y = (int) (0.5 * rect.y);
            }
        } else {
            ratio = 1.0;
        }

        String finCont = cont +"\n\n";

        if(ratio == 1.0) {
            finCont += LangID.getStringByID("gif_cango", lang)+"\n\n";
        } else {
            finCont += LangID.getStringByID("gif_adjust", lang).replace("_", DataToString.df.format(ratio * 100.0))+"\n\n";
        }

        if(rect.height % 2 == 1) {
            rect.height -= 1;
            rect.y += 1;
        }

        if(rect.width % 2 == 1) {
            rect.width -= 1;
            rect.x += 1;
        }

        if(rect.width == 0)
            rect.width = 2;

        if(rect.height == 0)
            rect.height = 2;

        finCont += LangID.getStringByID("gif_final", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x))+"\n";

        msg.editMessage(finCont).queue();

        P pos = new P(-rect.x, -rect.y);

        long current = System.currentTimeMillis();

        int totalFrame = 0;
        int progress = 0;

        for(int i = 0; i < mixer.anim.length; i++) {
            EAnimD<?> anim = mixer.getAnim(i);

            if(anim != null) {
                switch (i) {
                    case 0, 1 -> totalFrame += Math.max(60, Math.min(150, anim.len()));
                    case 2 -> totalFrame += Math.max(60, anim.len());
                    case 3, 5 -> totalFrame += 60;
                    case 4, 6 -> totalFrame += anim.len();
                }
            }
        }

        long start = System.currentTimeMillis();

        for(int i = 0; i < mixer.anim.length; i++) {
            //60 ~ 150, 60 ~ 150, 60 ~, 60, one cycle, 60, one cycle

            EAnimD<?> anim = mixer.getAnim(i);

            if(anim != null) {
                int frame = switch (i) {
                    case 0, 1 -> Math.max(60, Math.min(150, anim.len()));
                    case 2 -> Math.max(60, anim.len());
                    case 3, 5 -> 60;
                    case 4, 6 -> anim.len();
                    default -> 0;
                };

                if(i == 2) {
                    int stackFrame = 0;

                    while(stackFrame < 60) {
                        for(int j = 0; j < frame; j++) {
                            if(System.currentTimeMillis() - current >= 1500) {
                                String content = finCont +"\n\n";

                                String prog = DataToString.df.format(progress * 100.0 / totalFrame);
                                String eta = getETA(start, System.currentTimeMillis(), progress, totalFrame);

                                content += LangID.getStringByID("gif_makepng", lang) +
                                        LangID.getStringByID("bg_prog", lang)
                                                .replace("_PPP_", String.valueOf(progress))
                                                .replace("_LLL_", String.valueOf(totalFrame))
                                                .replace("_BBB_", getProgressBar(progress, totalFrame))
                                                .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length())) + prog)
                                                .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length())) + eta);

                                msg.editMessage(content).queue();

                                current = System.currentTimeMillis();
                            }

                            anim.setTime(j);

                            BufferedImage image = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
                            FG2D g = new FG2D(image.getGraphics());

                            g.setRenderingHint(3, 2);
                            g.enableAntialiasing();

                            g.setColor(54,57,63,255);
                            g.fillRect(0, 0, rect.width, rect.height);

                            anim.draw(g, pos, siz * ratio);

                            File img = new File("./temp/"+folderName+"/", quad(progress)+".png");

                            if(!img.exists()) {
                                boolean res = img.createNewFile();

                                if(!res) {
                                    System.out.println("Can't create new file : "+img.getAbsolutePath());
                                    return null;
                                }
                            } else {
                                return null;
                            }

                            ImageIO.write(image, "PNG", img);

                            progress++;
                        }

                        stackFrame += frame;
                    }
                } else {
                    for(int j = 0; j < frame - 1; j++) {
                        if(System.currentTimeMillis() - current >= 1500) {
                            String content = finCont +"\n\n";

                            String prog = DataToString.df.format(progress * 100.0 / totalFrame);
                            String eta = getETA(start, System.currentTimeMillis(), progress, totalFrame);

                            content += LangID.getStringByID("gif_makepng", lang) +
                                    LangID.getStringByID("bg_prog", lang)
                                            .replace("_PPP_", String.valueOf(progress))
                                            .replace("_LLL_", String.valueOf(totalFrame))
                                            .replace("_BBB_", getProgressBar(progress, totalFrame))
                                            .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length())) + prog)
                                            .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length())) + eta);

                            msg.editMessage(content).queue();

                            current = System.currentTimeMillis();
                        }

                        anim.setTime(j);

                        BufferedImage image = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
                        FG2D g = new FG2D(image.getGraphics());

                        g.setRenderingHint(3, 2);
                        g.enableAntialiasing();

                        g.setColor(54,57,63,255);
                        g.fillRect(0, 0, rect.width, rect.height);

                        anim.draw(g, pos, siz * ratio);

                        File img = new File("./temp/"+folderName+"/", quad(progress)+".png");

                        if(!img.exists()) {
                            boolean res = img.createNewFile();

                            if(!res) {
                                System.out.println("Can't create new file : "+img.getAbsolutePath());
                                return null;
                            }
                        } else {
                            return null;
                        }

                        ImageIO.write(image, "PNG", img);

                        progress++;
                    }
                }


            }
        }

        String content = finCont + "\n\n" +
                LangID.getStringByID("gif_makepng", lang) +
                LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", String.valueOf(totalFrame))
                        .replace("_LLL_", String.valueOf(totalFrame))
                        .replace("_BBB_", getProgressBar(totalFrame, totalFrame))
                        .replace("_VVV_", "100.00")
                        .replace("_SSS_", "     0") + "\n" +
                LangID.getStringByID("gif_converting", lang);

        msg.editMessage(content).queue();

        ProcessBuilder builder = new ProcessBuilder(SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg", "-r", "30", "-f", "image2", "-s", rect.width+"x"+rect.height,
                "-i", "temp/"+folderName+"/%04d.png", "-vcodec", "libx264", "-crf", "25", "-pix_fmt", "yuv420p", "-y", "temp/"+gif.getName());
        builder.redirectErrorStream(true);

        Process pro = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

        String line;

        while((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        pro.waitFor();

        StaticStore.deleteFile(folder, true);

        content = finCont + "\n\n" +
                LangID.getStringByID("gif_makepng", lang) +
                LangID.getStringByID("bg_prog", lang)
                        .replace("_PPP_", String.valueOf(totalFrame))
                        .replace("_LLL_", String.valueOf(totalFrame))
                        .replace("_BBB_", getProgressBar(totalFrame, totalFrame))
                        .replace("_VVV_", "100.00")
                        .replace("_SSS_", "     0") + "\n" +
                LangID.getStringByID("gif_converting", lang) + "\n\n" +
                LangID.getStringByID("gif_uploadmp4", lang);

        msg.editMessage(content).queue();

        return gif;
    }

    public static File drawStatImage(CustomMaskUnit[] units, List<List<CellDrawer>> cellGroup, int lv, String[] name, String type, File container, File itemContainer, boolean trueFormMode, int uID, int[] egg, int[][] trueForm) throws Exception {
        Canvas cv = new Canvas();

        FontMetrics nfm = cv.getFontMetrics(nameFont);
        FontMetrics cfm = cv.getFontMetrics(contentFont);

        int uh = 0;
        int uw = 0;

        int ah = 0;
        int aw = 0;

        int offset = 0;

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellDrawer> group = cellGroup.get(i);

            for(int j = 0; j < group.size() - 1; j++) {
                if(group.get(j) instanceof AbilityCellDrawer)
                    continue;

                group.get(j).initialize(nameFont, contentFont, nfm, cfm, 0);

                if(group.get(j) instanceof NormalCellDrawer)
                    offset = Math.max(((NormalCellDrawer) group.get(j)).offset, offset);

                int tempH = Math.max(((NormalCellDrawer) group.get(j)).h, ((NormalCellDrawer) group.get(j)).ih);
                int tempUw = ((NormalCellDrawer) group.get(j)).uw;

                if(((NormalCellDrawer) group.get(j)).isSingleData()) {
                    uh = Math.max(tempH, uh);

                    if(tempUw > uw * 3 + CellDrawer.lineOffset * 4) {
                        uw = (tempUw - CellDrawer.lineOffset * 4) / 3;
                    }
                } else {
                    uh = Math.max(tempH, uh);
                    uw = Math.max(tempUw, uw);
                }
            }
        }

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellDrawer> group = cellGroup.get(i);

            group.get(group.size() - 1).initialize(nameFont, contentFont, nfm, cfm, (int) Math.round((uw * 3 + CellDrawer.lineOffset * 4) * 1.5));

            offset = Math.max(((AbilityCellDrawer) group.get(group.size() - 1)).offset, offset);

            int tempH = ((AbilityCellDrawer) group.get(group.size() - 1)).h;
            int tempUw = ((AbilityCellDrawer) group.get(group.size() - 1)).w;

            ah = Math.max(tempH, ah);
            aw = Math.max(tempUw, aw);
        }

        if(aw > uw * 3 + CellDrawer.lineOffset * 4) {
            uw = (aw - CellDrawer.lineOffset * 4) / 3;
        }

        List<BufferedImage[]> images = new ArrayList<>();

        if(trueFormMode) {
            BufferedImage[] imgs = new BufferedImage[2];

            FontMetrics bf = cv.getFontMetrics(titleFont);
            FontMetrics sf = cv.getFontMetrics(typeFont);
            FontMetrics lf = cv.getFontMetrics(levelFont);

            File icon = new File(container, "uni"+Data.trio(uID)+"_s00.png");

            BufferedImage title = getUnitTitleImage(icon, name[0], type, lv, bf, sf, lf);

            imgs[1] = title;

            images.add(imgs);

            if(uw * 3 + CellDrawer.lineOffset * 4 + statPanelMargin * 2 < title.getWidth()) {
                uw = (title.getWidth() - statPanelMargin * 2 - CellDrawer.lineOffset * 4) / 3;
            }
        } else {
            for(int i = 0; i < units.length; i++) {
                BufferedImage[] imgs = new BufferedImage[2];

                FontMetrics bf = cv.getFontMetrics(titleFont);
                FontMetrics sf = cv.getFontMetrics(typeFont);
                FontMetrics lf = cv.getFontMetrics(levelFont);

                File icon;

                if(egg != null && i < egg.length && egg[i] != -1) {
                    icon = new File(container, "uni"+Data.trio(egg[i])+"_m"+Data.duo(i)+".png");
                } else {
                    icon = new File(container, "uni"+Data.trio(uID)+"_"+getUnitCode(i)+"00.png");
                }

                BufferedImage title = getUnitTitleImage(icon, name[i], type, lv, bf, sf, lf);

                imgs[1] = title;

                images.add(imgs);

                if(uw * 3 + CellDrawer.lineOffset * 4 + statPanelMargin * 2 < title.getWidth()) {
                    uw = (title.getWidth() - statPanelMargin * 2 - CellDrawer.lineOffset * 4) / 3;
                }
            }
        }

        int h = 0;
        int w = uw * 3 + CellDrawer.lineOffset * 4;

        for(int j = 0; j < units.length; j++) {
            List<CellDrawer> group = cellGroup.get(j);

            int th = 0;

            for(int i = 0; i < group.size(); i++) {
                if(i < group.size() - 1) {
                    th += uh;

                    th += cellMargin;
                } else {
                    th += ah + cellMargin;
                }
            }

            h = Math.max(th, h);
        }

        for(int j = 0; j < units.length; j++) {
            List<CellDrawer> group = cellGroup.get(j);

            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            FG2D g = new FG2D(img.getGraphics());

            g.setRenderingHint(3, 1);
            g.enableAntialiasing();

            int x = 0;
            int y = 0;

            for(int i = 0; i < group.size(); i++) {
                group.get(i).draw(g, x, y, uw, offset, uh, nameFont, contentFont);

                y += uh + cellMargin;

                if(i == group.size() - 2)
                    y += cellMargin;
            }

            images.get(j)[0] = img;
        }

        int titleW = 0;
        int imgW = 0;

        int titleH = 0;
        int imgH = 0;

        for(int i = 0; i < images.size(); i++) {
            titleW = Math.max(titleW, images.get(i)[1].getWidth());
            titleH = Math.max(titleH, images.get(i)[1].getHeight());

            imgW = Math.max(imgW, images.get(i)[0].getWidth());
            imgH = Math.max(imgH, images.get(i)[0].getHeight());
        }

        int finW = Math.max(titleW, imgW + statPanelMargin * 2) + bgMargin * 2;
        int finH = bgMargin * 5 + titleH + statPanelMargin * 2 + imgH;
        int fruitH = 0;

        if(trueForm != null && (trueFormMode || units.length >= 3)) {
            GlyphVector glyph = fruitFont.createGlyphVector(cv.getFontMetrics(fruitFont).getFontRenderContext(), "1234567890Mk");

            int textHeight = glyph.getPixelBounds(null, 0, 0).height;

            fruitH = (int) (textHeight + (finW - 2 * bgMargin) * (fruitRatio + fruitTextGapRatio + fruitUpperGapRatio + fruitDownerGapRatio) + fruitGap);

            finH += fruitH;
        }

        BufferedImage result = new BufferedImage(finW * units.length, finH, BufferedImage.TYPE_INT_ARGB);
        FG2D rg = new FG2D(result.getGraphics());

        int bx = 0;

        rg.setColor(50, 53, 59);
        rg.fillRect(0, 0, finW * units.length, finH);

        rg.setColor(24, 25, 28);
        rg.fillRoundRect(0, -cornerRadius, finW * units.length, cornerRadius + bgMargin * 8 + titleH, cornerRadius, cornerRadius);

        for(int i = 0; i < units.length; i++) {
            rg.setColor(64, 68, 75);

            if(!trueFormMode && units.length >= 3 && trueForm != null && i != 2) {
                rg.fillRoundRect(bx + bgMargin, bgMargin * 4 + titleH, imgW + statPanelMargin * 2, imgH + statPanelMargin * 2 + fruitH, cornerRadius, cornerRadius);
            } else {
                rg.fillRoundRect(bx + bgMargin, bgMargin * 4 + titleH, imgW + statPanelMargin * 2, imgH + statPanelMargin * 2, cornerRadius, cornerRadius);
            }

            rg.drawImage(images.get(i)[1], bx + bgMargin, bgMargin * 2);
            rg.drawImage(images.get(i)[0], bx + bgMargin + statPanelMargin, bgMargin * 4 + titleH + statPanelMargin);

            bx += finW;
        }

        if((units.length >= 3 || trueFormMode) && trueForm != null) {
            BufferedImage trueFormImage = generateEvolveImage(itemContainer, trueForm, finW - bgMargin * 2, cfm);

            bx -= finW;

            rg.drawImage(trueFormImage, bx + bgMargin, bgMargin * 4 + titleH + imgH + statPanelMargin * 2 + fruitGap);
        }

        BufferedImage scaledDown = new BufferedImage(result.getWidth() / 2, result.getHeight() / 2, BufferedImage.TYPE_INT_ARGB);

        FG2D sdg = new FG2D(scaledDown.getGraphics());

        sdg.setRenderingHint(3, 1);
        sdg.enableAntialiasing();

        sdg.drawImage(result, 0, 0, scaledDown.getWidth(), scaledDown.getHeight());

        File f = new File("./temp/");

        if(!f.exists() && !f.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(f, "result", ".png", false);

        if(image == null) {
            return null;
        }

        ImageIO.write(scaledDown, "PNG", image);

        return image;
    }

    public static File drawEnemyStatImage(List<CellDrawer> cellGroup, String mag, String name, File container, int eID) throws Exception {
        Canvas cv = new Canvas();

        FontMetrics nfm = cv.getFontMetrics(nameFont);
        FontMetrics cfm = cv.getFontMetrics(contentFont);

        int uh = 0;
        int uw = 0;

        int ah = 0;
        int aw = 0;

        int offset = 0;

        for(int i = 0; i < cellGroup.size() - 1; i++) {
            if(cellGroup.get(i) instanceof AbilityCellDrawer)
                continue;

            cellGroup.get(i).initialize(nameFont, contentFont, nfm, cfm, 0);

            if(cellGroup.get(i) instanceof NormalCellDrawer)
                offset = Math.max(((NormalCellDrawer) cellGroup.get(i)).offset, offset);

            int tempH = Math.max(((NormalCellDrawer) cellGroup.get(i)).h, ((NormalCellDrawer) cellGroup.get(i)).ih);
            int tempUw = ((NormalCellDrawer) cellGroup.get(i)).uw;

            if(((NormalCellDrawer) cellGroup.get(i)).isSingleData()) {
                uh = Math.max(tempH, uh);

                if(tempUw > uw * 3 + CellDrawer.lineOffset * 4) {
                    uw = (tempUw - CellDrawer.lineOffset * 4) / 3;
                }
            } else {
                uh = Math.max(tempH, uh);
                uw = Math.max(tempUw, uw);
            }
        }

        cellGroup.get(cellGroup.size() - 1).initialize(nameFont, contentFont, nfm, cfm, (int) Math.round((uw * 3 + CellDrawer.lineOffset * 4) * 1.5));

        offset = Math.max(((AbilityCellDrawer) cellGroup.get(cellGroup.size() - 1)).offset, offset);

        int tempH = ((AbilityCellDrawer) cellGroup.get(cellGroup.size() - 1)).h;
        int tempUw = ((AbilityCellDrawer) cellGroup.get(cellGroup.size() - 1)).w;

        ah = Math.max(tempH, ah);
        aw = Math.max(tempUw, aw);

        if(aw > uw * 3 + CellDrawer.lineOffset * 4) {
            uw = (aw - CellDrawer.lineOffset * 4) / 3;
        }

        BufferedImage[] imgs = new BufferedImage[2];

        FontMetrics bf = cv.getFontMetrics(titleFont);
        FontMetrics lf = cv.getFontMetrics(levelFont);

        File icon = new File(container, "enemy_icon_"+Data.trio(eID)+".png");

        BufferedImage title = getEnemyTitleImage(icon, name, mag, bf, lf);

        imgs[1] = title;

        if(uw * 3 + CellDrawer.lineOffset * 4 + statPanelMargin * 2 < title.getWidth()) {
            uw = (title.getWidth() - statPanelMargin * 2 - CellDrawer.lineOffset * 4) / 3;
        }

        int h = 0;
        int w = uw * 3 + CellDrawer.lineOffset * 4;

        for(int i = 0; i < cellGroup.size(); i++) {
            if(i < cellGroup.size() - 1) {
                h += uh;

                h += cellMargin;
            } else {
                h += ah + cellMargin;
            }
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(img.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        int x = 0;
        int y = 0;

        for(int i = 0; i < cellGroup.size(); i++) {
            cellGroup.get(i).draw(g, x, y, uw, offset, uh, nameFont, contentFont);

            y += uh + cellMargin;

            if(i == cellGroup.size() - 2)
                y += cellMargin;
        }

        imgs[0] = img;

        int titleW = imgs[1].getWidth();
        int imgW = imgs[0].getWidth();

        int titleH = imgs[1].getHeight();
        int imgH = imgs[0].getHeight();

        int finW = Math.max(titleW, imgW + statPanelMargin * 2) + bgMargin * 2;
        int finH = bgMargin * 5 + titleH + statPanelMargin * 2 + imgH;

        BufferedImage result = new BufferedImage(finW, finH, BufferedImage.TYPE_INT_ARGB);
        FG2D rg = new FG2D(result.getGraphics());

        rg.setColor(50, 53, 59);
        rg.fillRect(0, 0, finW, finH);

        rg.setColor(24, 25, 28);
        rg.fillRoundRect(0, -cornerRadius, finW, cornerRadius + bgMargin * 8 + titleH, cornerRadius, cornerRadius);

        rg.setColor(64, 68, 75);

        rg.fillRoundRect(bgMargin, bgMargin * 4 + titleH, imgW + statPanelMargin * 2, imgH + statPanelMargin * 2, cornerRadius, cornerRadius);

        rg.drawImage(imgs[1], bgMargin, bgMargin * 2);
        rg.drawImage(imgs[0], bgMargin + statPanelMargin, bgMargin * 4 + titleH + statPanelMargin);

        BufferedImage scaledDown = new BufferedImage(result.getWidth() / 2, result.getHeight() / 2, BufferedImage.TYPE_INT_ARGB);

        FG2D sdg = new FG2D(scaledDown.getGraphics());

        sdg.setRenderingHint(3, 1);
        sdg.enableAntialiasing();

        sdg.drawImage(result, 0, 0, scaledDown.getWidth(), scaledDown.getHeight());

        File f = new File("./temp/");

        if(!f.exists() && !f.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(f, "result", ".png", false);

        if(image == null) {
            return null;
        }

        ImageIO.write(scaledDown, "PNG", image);

        return image;
    }

    public static File drawStageStatImage(CustomStageMap map, List<CellDrawer> group, boolean isFrame, int lv, String name, String code, int index, int lang) throws Exception {
        Stage st = map.list.get(index);
        boolean isRanking = code.replaceAll(" - \\d+ - \\d+", "").equals("R") || code.replaceAll(" - \\d+ - \\d+", "").equals("T");

        Canvas cv = new Canvas();

        FontMetrics nfm = cv.getFontMetrics(nameFont);
        FontMetrics cfm = cv.getFontMetrics(contentFont);
        FontMetrics tfm = cv.getFontMetrics(titleFont);

        int uh = 0;
        int uw = 0;
        int ch = 0;

        int offset = 0;

        for(int i = 0; i < group.size() - 2; i++) {
            group.get(i).initialize(nameFont, contentFont, nfm, cfm, 0);

            if(group.get(i) instanceof NormalCellDrawer)
                offset = Math.max(((NormalCellDrawer) group.get(i)).offset, offset);

            int tempH = Math.max(((NormalCellDrawer) group.get(i)).h, ((NormalCellDrawer) group.get(i)).ih);
            int tempUw = ((NormalCellDrawer) group.get(i)).uw;

            if(((NormalCellDrawer) group.get(i)).isSingleData()) {
                uh = Math.max(tempH, uh);

                if(tempUw > uw * 3 + CellDrawer.lineOffset * 4) {
                    uw = (tempUw - CellDrawer.lineOffset * 4) / 3;
                }
            } else {
                uh = Math.max(tempH, uh);
                uw = Math.max(tempUw, uw);
            }

            ch = Math.max(((NormalCellDrawer) group.get(i)).ch, ch);
        }

        group.get(group.size() - 2).initialize(nameFont, contentFont, nfm, cfm, (int) ((uw * 4 + CellDrawer.lineOffset * 6) * 1.5));

        offset = Math.max(((AbilityCellDrawer) group.get(group.size() - 2)).offset, offset);

        int ah = ((AbilityCellDrawer) group.get(group.size() - 2)).h;
        int aw = ((AbilityCellDrawer) group.get(group.size() - 2)).w;

        if(aw > uw * 4 + CellDrawer.lineOffset * 6) {
            uw = (aw - CellDrawer.lineOffset * 6) / 4;
        }

        group.get(group.size() - 1).initialize(nameFont, contentFont, nfm, cfm, (int) ((uw * 4 + CellDrawer.lineOffset * 6) * 1.5));

        offset = Math.max(((AbilityCellDrawer) group.get(group.size() - 1)).offset, offset);

        int mh = ((AbilityCellDrawer) group.get(group.size() - 1)).h;
        int mw = ((AbilityCellDrawer) group.get(group.size() - 1)).w;

        if(mw > uw * 4 + CellDrawer.lineOffset * 6) {
            uw = (mw - CellDrawer.lineOffset * 6) / 4;
        }

        FontRenderContext bfrc = tfm.getFontRenderContext();
        FontRenderContext lfrc = cfm.getFontRenderContext();

        Rectangle2D nRect = titleFont.createGlyphVector(bfrc, name).getPixelBounds(null, 0, 0);
        Rectangle2D lRect = contentFont.createGlyphVector(lfrc, code).getPixelBounds(null, 0, 0);

        int titleHeight = (int) Math.round(nRect.getHeight() + nameMargin + lRect.getHeight());
        int titleWidth = (int) Math.max(nRect.getWidth(), lRect.getWidth()) + bgMargin;

        if(titleWidth > uw * 4 + CellDrawer.lineOffset * 6) {
            uw = (titleWidth - CellDrawer.lineOffset * 6) / 4;
        }

        int[] stw = measureEnemySchemeWidth(st, map, cfm, isRanking, isFrame, lv, lang);
        int[] dw = measureDropTableWidth(st, map, cfm, lang, true);
        int[] sw = measureDropTableWidth(st, map, cfm, lang, false);

        if(dw != null && sw == null) {
            sw = dw;
        }

        if(sw != null && dw == null) {
            dw = sw;
        }

        int desiredStageGap = innerTableTextMargin;
        int desiredRewardGap = innerTableTextMargin;
        int desiredScoreGap = innerTableTextMargin;

        if(dw != null) {
            int tw = maxAmong(dw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2, sw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2, stw[STAGE_WIDTH] - statPanelMargin * 2);

            if(tw > uw * 4 + CellDrawer.lineOffset * 6) {
                uw = (int) Math.round((tw - CellDrawer.lineOffset * 6) / 4.0);

                if(tw > dw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2) {
                    desiredRewardGap = (int) Math.round((tw - 2 * (dw[CHANCE_WIDTH] + dw[REWARD_WIDTH] + dw[AMOUNT_WIDTH] + rewardIconSize + CellDrawer.lineOffset)) / 14.0);
                }

                if(tw > sw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2) {
                    desiredScoreGap = (int) Math.round((tw - 2 * (sw[CHANCE_WIDTH] + sw[REWARD_WIDTH] + sw[AMOUNT_WIDTH] + rewardIconSize + CellDrawer.lineOffset)) / 14.0);
                }

                if(tw > stw[STAGE_WIDTH] - statPanelMargin * 2) {
                    int tempTotalWidth = tw + statPanelMargin * 2;

                    for(int i = ENEMY; i <= BOSS; i++) {
                        tempTotalWidth -= stw[i];
                    }

                    desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 19.0);
                }
            } else {
                desiredRewardGap = (int) Math.round((uw * 2 + CellDrawer.lineOffset * 2 - dw[CHANCE_WIDTH] - dw[REWARD_WIDTH] - dw[AMOUNT_WIDTH] - rewardIconSize) / 7.0);
                desiredScoreGap = (int) Math.round((uw * 2 + CellDrawer.lineOffset * 2 - sw[CHANCE_WIDTH] - sw[REWARD_WIDTH] - sw[AMOUNT_WIDTH] - rewardIconSize) / 7.0);

                int tempTotalWidth = uw * 4 + CellDrawer.lineOffset * 6;

                for(int i = ENEMY; i <= BOSS; i++) {
                    tempTotalWidth -= stw[i];
                }

                desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 19.0);
            }
        } else {
            if(stw[STAGE_WIDTH] > uw * 4 + CellDrawer.lineOffset * 6 + statPanelMargin * 2) {
                uw = (int) Math.round((stw[STAGE_WIDTH] - CellDrawer.lineOffset * 6 - statPanelMargin * 2) / 4.0);
            } else {
                int tempTotalWidth = uw * 4 + CellDrawer.lineOffset * 6 + statPanelMargin * 2;

                for(int i = ENEMY; i <= BOSS; i++) {
                    tempTotalWidth -= stw[i];
                }

                desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 19.0);
            }
        }

        int schemeHeight = innerTableCellMargin * (st.data.datas.length + 1);

        int infoWidth = uw * 4 + CellDrawer.lineOffset * 6;
        int infoHeight = 0;

        for(int i = 0; i < group.size(); i++) {
            if(i < group.size() - 2) {
                infoHeight += uh;
                infoHeight += cellMargin;
            } else if(i < group.size() - 1) {
                infoHeight += ah + cellMargin * 2;
            } else {
                infoHeight += mh + cellMargin * 2;
            }
        }

        int finW = infoWidth + statPanelMargin * 2 + bgMargin * 2;
        int finH = bgMargin * 6 + titleHeight + statPanelMargin * 2 + infoHeight + cellMargin * 2 + uh - CellDrawer.textMargin - ch;
        int panelH = statPanelMargin * 2 + infoHeight + cellMargin * 2 + uh - CellDrawer.textMargin - ch;

        List<String[]> rewardData = DataToString.getRewards(st, map, lang);
        List<String[]> scoreData = DataToString.getScoreDrops(st, map, lang);

        if(rewardData != null || scoreData != null) {
            int tableH = 0;

            if(rewardData != null) {
                assert dw != null;

                tableH = Math.max(dw[TOTAL_HEIGHT], tableH);
            }

            if(scoreData != null) {
                assert sw != null;
                tableH = Math.max(sw[TOTAL_HEIGHT], tableH);
            }

            finH += tableH;
            panelH += tableH;
        } else {
            finH += ch;
            panelH += ch;
        }

        finH += schemeHeight;

        BufferedImage result = new BufferedImage(finW, finH, BufferedImage.TYPE_INT_ARGB);

        FG2D g = new FG2D(result.getGraphics());

        g.setColor(50, 53, 59);

        g.fillRect(0, 0, finW, finH);

        g.setColor(24, 25, 28);

        g.fillRoundRect(0, -cornerRadius, finW, cornerRadius + bgMargin * 8 + titleHeight, cornerRadius, cornerRadius);

        g.setColor(64, 68, 75);

        g.fillRoundRect(bgMargin, bgMargin * 4 + titleHeight, infoWidth + statPanelMargin * 2, panelH, cornerRadius, cornerRadius);

        drawStageTitleImage(g, name, code, tfm, cfm);

        int x = bgMargin + statPanelMargin;
        int y = bgMargin * 4 + titleHeight + statPanelMargin;

        for(int i = 0; i < group.size(); i++) {
            group.get(i).draw(g, x, y, uw, offset, uh, nameFont, contentFont);

            if(i < group.size() - 1)
                y += uh + cellMargin;
            else
                y += ah + cellMargin;

            if(i == group.size() - 3 || i == group.size() - 2)
                y += cellMargin;
        }

        g.setColor(191, 191, 191);

        g.setFont(nameFont);

        g.drawText(LangID.getStringByID("data_rewarddrop", lang), bgMargin + statPanelMargin, bgMargin * 4 + titleHeight + statPanelMargin + infoHeight + cellMargin + offset / 2);
        g.drawText(LangID.getStringByID("data_scoredrop", lang), bgMargin + statPanelMargin + uw * 2 + CellDrawer.lineOffset * 4, bgMargin * 4 + titleHeight + statPanelMargin + infoHeight + cellMargin + offset / 2);

        int stack = bgMargin * 4 + titleHeight + statPanelMargin + infoHeight + cellMargin * 2 + uh - ch - CellDrawer.textMargin;

        if(rewardData != null) {
            drawRewardTable(g, bgMargin + statPanelMargin, stack, st, map, dw, desiredRewardGap, lang, true);
        } else {
            g.setFont(contentFont);
            g.setColor(239, 239, 239);

            g.drawText(LangID.getStringByID("data_none", lang), bgMargin + statPanelMargin, stack + offset / 2);
        }

        if(scoreData != null) {
            drawRewardTable(g, bgMargin + statPanelMargin + uw * 2 + CellDrawer.lineOffset * 4, stack, st, map, dw, desiredScoreGap, lang, false);
        } else {
            g.setFont(contentFont);
            g.setColor(239, 239, 239);

            g.drawText(LangID.getStringByID("data_none", lang), bgMargin + statPanelMargin + uw * 2 + CellDrawer.lineOffset * 4, stack + offset / 2);
        }

        drawEnemySchemeTable(g, bgMargin * 4 + titleHeight  + panelH + bgMargin, st, map, stw, desiredStageGap, isRanking, isFrame, lv, lang);

        File f = new File("./temp/");

        if(!f.exists() && !f.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(f, "result", ".png", false);

        if(image == null) {
            return null;
        }

        ImageIO.write(result, "PNG", image);

        return image;
    }

    public static File drawTalentImage(String name, String type, CustomTalent talent, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            return null;
        }

        File image = StaticStore.generateTempFile(temp, "talent", ".png", false);

        if(image == null || !image.exists())
            return null;

        Canvas cv = new Canvas();

        FontMetrics nfm = cv.getFontMetrics(nameFont);
        FontMetrics tyfm = cv.getFontMetrics(typeFont);
        FontMetrics cfm = cv.getFontMetrics(contentFont);
        FontMetrics tfm = cv.getFontMetrics(titleFont);
        FontMetrics lvm = cv.getFontMetrics(levelFont);

        Rectangle2D nameRect = titleFont.createGlyphVector(tfm.getFontRenderContext(), name).getPixelBounds(null, 0, 0);
        Rectangle2D typeRect = typeFont.createGlyphVector(tyfm.getFontRenderContext(), type).getPixelBounds(null, 0, 0);

        int titleHeight = (int) Math.round(nameRect.getHeight() + nameMargin + typeRect.getHeight() + typeUpDownMargin * 2);

        int icw = (int) Math.round(titleHeight * 1.0 / talent.icon.getHeight() * talent.icon.getWidth());

        int titleWidth = icw + nameMargin + (int) Math.round(Math.max(nameRect.getWidth(), typeRect.getWidth() + typeLeftRightMargin * 2));

        int maxDescLineHeight = 0;
        int maxDescLineWidth = 0;

        int maxTitleHeight = 0;
        int maxTitleWidth = 0;

        int maxCostWidth = 0;

        int totalCost = 0;

        for(int i = 0; i < talent.talents.size(); i++) {
            TalentData data = talent.talents.get(i);

            for(int j = 0; j < data.cost.size(); j++) {
                totalCost += data.cost.get(j);
            }

            Rectangle2D titleRect = levelFont.createGlyphVector(lvm.getFontRenderContext(), data.title).getPixelBounds(null, 0, 0);

            maxTitleHeight = (int) Math.round(Math.max(maxTitleHeight, titleRect.getHeight()));
            maxTitleWidth = (int) Math.round(Math.max(maxTitleWidth, titleRect.getWidth()));

            for(int j = 0; j < data.description.length; j++) {
                Rectangle2D descRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), data.description[j]).getPixelBounds(null, 0, 0);

                maxDescLineHeight = (int) Math.round(Math.max(maxDescLineHeight, descRect.getHeight()));
                maxDescLineWidth = (int) Math.round(Math.max(maxDescLineWidth, descRect.getWidth()));
            }

            if(data.cost.size() == 1) {
                String cost = String.format(LangID.getStringByID("talanalyzer_singlenp", lang), data.cost.get(0));

                Rectangle2D costRect = contentFont.createGlyphVector(cfm.getFontRenderContext(), cost).getPixelBounds(null, 0, 0);

                maxCostWidth = (int) Math.round(Math.max(maxCostWidth, costRect.getWidth()));
            } else {
                String costTitle = LangID.getStringByID("talanalyzer_npcost", lang);
                StringBuilder cost = new StringBuilder("[");
                int costSummary = 0;

                for(int j = 0; j < data.cost.size(); j++) {
                    costSummary += data.cost.get(j);

                    cost.append(data.cost.get(j));

                    if(j < data.cost.size() - 1)
                        cost.append(", ");
                }

                cost.append("] => ").append(costSummary);

                Rectangle2D costTitleRect = contentFont.createGlyphVector(cfm.getFontRenderContext(), costTitle).getPixelBounds(null, 0, 0);
                Rectangle2D costRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), cost.toString()).getPixelBounds(null, 0, 0);

                maxCostWidth = (int) Math.round(Math.max(maxCostWidth, Math.max(costTitleRect.getWidth(), talentCostTableGap * 2 + costRect.getWidth())));
            }
        }

        int talentIconDimension = (int) Math.round(maxTitleHeight * 1.5);

        String totalCostText = LangID.getStringByID("talentinfo_total", lang).replace("_", String.valueOf(totalCost));
        Rectangle2D totalRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), totalCostText).getPixelBounds(null, 0, 0);

        int totalCostWidth = (int) Math.round(totalRect.getWidth());
        int totalCostHeight = (int) Math.round(totalRect.getHeight());

        int panelWidth = statPanelMargin * 2 + Math.max(maxCostWidth, Math.max(maxDescLineWidth, talentIconDimension * 2 + talentNameGap + maxTitleWidth));
        int panelHeight = statPanelMargin * 2;

        for(int i = 0; i < talent.talents.size(); i++) {
            TalentData data = talent.talents.get(i);

            panelHeight += talentIconDimension;

            if(data.hasDescription()) {
                panelHeight += talentIconGap;

                for(int j = 0; j < data.description.length; j++) {
                    panelHeight += maxDescLineHeight;

                    if(j < data.description.length - 1) {
                        panelHeight += lineSpace;
                    }
                }
            }

            panelHeight += talentCostGap;

            if(data.cost.size() == 1) {
                String cost = String.format(LangID.getStringByID("talanalyzer_singlenp", lang), data.cost.get(0));

                Rectangle2D costRect = contentFont.createGlyphVector(cfm.getFontRenderContext(), cost).getPixelBounds(null, 0, 0);

                panelHeight += Math.round(costRect.getHeight());
            } else {
                String costTitle = LangID.getStringByID("talanalyzer_npcost", lang);
                StringBuilder cost = new StringBuilder("[");
                int costSummary = 0;

                for(int j = 0; j < data.cost.size(); j++) {
                    costSummary += data.cost.get(j);

                    cost.append(data.cost.get(j));

                    if(j < data.cost.size() - 1)
                        cost.append(", ");
                }

                cost.append("] => ").append(costSummary);

                Rectangle2D costTitleRect = contentFont.createGlyphVector(cfm.getFontRenderContext(), costTitle).getPixelBounds(null, 0, 0);
                Rectangle2D costRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), cost.toString()).getPixelBounds(null, 0, 0);

                panelHeight += Math.round(costTitleRect.getHeight()) + talentTableGap + talentCostTableGap * 2 + costRect.getHeight();
            }

            if(i < talent.talents.size() - 1)
                panelHeight += talentGap;
        }

        panelWidth = Math.max(totalCostWidth, Math.max(panelWidth, titleWidth + statPanelMargin));

        int totalHeight = bgMargin * 2 + titleHeight + bgMargin * 2 + panelHeight + bgMargin + Math.max(totalCostGap, totalCostHeight);
        int totalWidth = bgMargin * 2 + panelWidth;

        BufferedImage result = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setColor(51, 53, 60, 255);
        g.fillRect( 0, 0, totalWidth, totalHeight);

        g.setColor(24, 25, 28, 255);
        g.fillRoundRect(0, -cornerRadius / 2, totalWidth, cornerRadius + bgMargin * 8 + titleHeight, cornerRadius, cornerRadius);

        g.setColor(64, 68, 75, 255);
        g.fillRoundRect(bgMargin, bgMargin * 4 + titleHeight, panelWidth, panelHeight, cornerRadius, cornerRadius);

        g.drawImage(talent.icon, bgMargin, bgMargin * 2, icw, titleHeight);

        g.setFont(titleFont);
        g.setColor(238, 238, 238, 255);
        g.drawText(name, (int) Math.round(bgMargin + icw + nameMargin - nameRect.getX()), (int) Math.round(bgMargin * 2 - nameRect.getY()));

        g.setColor(88, 101, 242, 255);
        g.fillRoundRect(bgMargin + icw + nameMargin, (int) Math.round(bgMargin * 2 + nameRect.getHeight() + nameMargin), (int) Math.round(typeLeftRightMargin * 2 + typeRect.getWidth()), (int) Math.round(typeUpDownMargin * 2 + typeRect.getHeight()), typeCornerRadius, typeCornerRadius);

        g.setFont(typeFont);
        g.setColor(238, 238, 238, 255);
        g.drawText(type, (int) Math.round(bgMargin + icw + nameMargin + typeLeftRightMargin - typeRect.getX()), (int) Math.round(bgMargin * 2 + nameRect.getHeight() + nameMargin + typeUpDownMargin - typeRect.getY()));

        int x = bgMargin + statPanelMargin;
        int y = bgMargin * 2 + titleHeight + bgMargin * 2 + statPanelMargin;

        for(int i = 0; i < talent.talents.size(); i++) {
            TalentData data = talent.talents.get(i);

            int talentTitleOffset = talentIconDimension + talentNameGap;

            if(i == 0 && talent.traitIcon != null) {
                g.drawImage(talent.traitIcon, x, y, talentIconDimension, talentIconDimension);
                g.drawImage(data.icon, x + talentIconDimension + lineSpace, y, talentIconDimension, talentIconDimension);

                talentTitleOffset += talentIconDimension + lineSpace;
            } else {
                g.drawImage(data.icon, x, y, talentIconDimension, talentIconDimension);
            }

            g.setFont(levelFont);
            g.setColor(238, 238, 238, 255);

            Rectangle2D talentNameRect = levelFont.createGlyphVector(lvm.getFontRenderContext(), data.title).getPixelBounds(null, 0, 0);

            g.drawText(data.title, (int) Math.round(x + talentTitleOffset - talentNameRect.getX()), (int) Math.round(y + (talentIconDimension - talentNameRect.getHeight()) / 2.0 - talentNameRect.getY()));

            y += talentIconDimension;

            if(data.hasDescription()) {
                y += talentIconGap;

                g.setFont(nameFont);
                g.setColor(191, 191, 191, 255);

                for(int j = 0; j < data.description.length; j++) {
                    if(!data.description[j].isBlank()) {
                        Rectangle2D descRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), data.description[j]).getPixelBounds(null, 0, 0);

                        g.drawText(data.description[j], (int) Math.round(x - descRect.getX()), (int) Math.round(y - descRect.getY()));
                    }

                    y += maxDescLineHeight;

                    if(j < data.description.length - 1)
                        y += lineSpace;
                }
            }

            y += talentCostGap;

            if(data.cost.size() == 1) {
                String cost = String.format(LangID.getStringByID("talanalyzer_singlenp", lang), data.cost.get(0));

                g.setFont(contentFont);
                g.setColor(238, 238, 238, 255);

                Rectangle2D costRect = contentFont.createGlyphVector(cfm.getFontRenderContext(), cost).getPixelBounds(null, 0, 0);

                g.drawText(cost, (int) Math.round(x - costRect.getX()), (int) Math.round(y - costRect.getY()));

                y += Math.round(costRect.getHeight());
            } else {
                String costTitle = LangID.getStringByID("talanalyzer_npcost", lang);
                StringBuilder cost = new StringBuilder("[");
                int costSummary = 0;

                for(int j = 0; j < data.cost.size(); j++) {
                    costSummary += data.cost.get(j);

                    cost.append(data.cost.get(j));

                    if(j < data.cost.size() - 1)
                        cost.append(", ");
                }

                cost.append("] => ").append(costSummary);

                Rectangle2D costTitleRect = contentFont.createGlyphVector(cfm.getFontRenderContext(), costTitle).getPixelBounds(null, 0, 0);
                Rectangle2D costRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), cost.toString()).getPixelBounds(null, 0, 0);

                g.setFont(contentFont);
                g.setColor(238, 238, 238, 255);
                g.drawText(costTitle, (int) Math.round(x - costTitleRect.getX()), (int) Math.round(y - costTitleRect.getY()));

                y += Math.round(talentTableGap + costTitleRect.getHeight());

                g.setColor(51, 54, 60);
                g.fillRoundRect(x, y, (int) Math.round(talentCostTableGap * 2 + costRect.getWidth()), (int) Math.round(talentCostTableGap * 2 + costRect.getHeight()), innerTableCornerRadius, innerTableCornerRadius);

                g.setFont(nameFont);
                g.setColor(238, 238, 238, 255);

                g.drawText(cost.toString(), (int) Math.round(x + talentCostTableGap - costRect.getX()), (int) Math.round(y + talentCostTableGap - costRect.getY()));

                y += Math.round(talentCostTableGap * 2 + costRect.getHeight());
            }

            if(i < talent.talents.size() - 1) {
                y += talentGap / 2.0;

                g.setColor(191, 191, 191, 255);
                g.setStroke(CellDrawer.lineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                g.drawLine(x, y, x + panelWidth - statPanelMargin * 2, y);

                y += talentGap / 2.0;
            }
        }

        g.setFont(nameFont);
        g.setColor(191, 191, 191, 255);
        g.drawText(totalCostText, (int) Math.round(totalWidth - bgMargin - totalCostWidth - totalRect.getX()), (int) Math.round(totalHeight - bgMargin - totalCostHeight - totalRect.getY()));

        ImageIO.write(result, "PNG", image);

        return image;
    }

    public static File drawComboImage(File folder, CustomCombo combo) throws Exception {
        File image = StaticStore.generateTempFile(folder, "combo", ".png", false);

        if(image == null || !image.exists())
            return null;

        Canvas cv = new Canvas();

        FontMetrics tfm = cv.getFontMetrics(titleFont);
        FontMetrics tyfm = cv.getFontMetrics(typeFont);
        FontMetrics nfm = cv.getFontMetrics(nameFont);
        FontMetrics cfm = cv.getFontMetrics(contentFont);

        Rectangle2D titleRect = titleFont.createGlyphVector(tfm.getFontRenderContext(), combo.title).getPixelBounds(null, 0, 0);
        Rectangle2D typeRect = typeFont.createGlyphVector(tyfm.getFontRenderContext(), combo.type).getPixelBounds(null, 0, 0);
        Rectangle2D levelRect = typeFont.createGlyphVector(tyfm.getFontRenderContext(), combo.level).getPixelBounds(null, 0, 0);

        int levelBoxDimension = (int) Math.round(Math.max(levelRect.getWidth(), levelRect.getHeight()) + comboTypeInnerGap * 2);

        int typeBoxHeight = (int) Math.round(Math.max(typeRect.getHeight(), levelBoxDimension));
        int typeBoxWidth = (int) Math.round(typeRect.getWidth() + comboTypeGap + levelBoxDimension);

        int titleHeight = (int) Math.round(titleRect.getHeight() + comboTitleGap + typeBoxHeight);
        int titleWidth = (int) Math.round(Math.max(titleRect.getWidth(), typeBoxWidth));

        int maxIconTableWidth = (int) Math.round(comboIconLeftRightGap * 2 + combo.icons.get(0).getWidth() * comboIconScaleFactor);
        int maxUnitNameHeight = 0;

        for(int i = 0; i < combo.icons.size(); i++) {
            Rectangle2D unitNameRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), combo.names.get(i)).getPixelBounds(null, 0, 0);

            maxUnitNameHeight = (int) Math.round(Math.max(maxUnitNameHeight, unitNameRect.getHeight()));
            maxIconTableWidth = (int) Math.round(Math.max(maxIconTableWidth, comboIconLeftRightGap * 2 + unitNameRect.getWidth()));
        }

        int maxIconTableHeight = (int) Math.round(comboIconUpDownGap * 2 + combo.icons.get(0).getHeight() * comboIconScaleFactor + comboIconNameGap + maxUnitNameHeight);

        Rectangle2D descRect = contentFont.createGlyphVector(cfm.getFontRenderContext(), combo.description).getPixelBounds(null, 0, 0);

        maxIconTableWidth = (int) Math.round(Math.max(maxIconTableWidth, (descRect.getWidth() - comboIconGap * (combo.icons.size() - 1)) / (1.0 * combo.icons.size())));

        int panelHeight = (int) Math.round(statPanelMargin * 2 + maxIconTableHeight + comboContentGap + descRect.getHeight());
        int panelWidth = (int) Math.round(statPanelMargin * 2 + Math.max(maxIconTableWidth * combo.icons.size() + comboIconGap * (combo.icons.size() - 1), descRect.getWidth()));

        if(titleWidth > panelWidth) {
            panelWidth = titleWidth + bgMargin * 2 + statPanelMargin;

            maxIconTableWidth = (int) Math.round((panelWidth - statPanelMargin * 2 - comboIconGap * (combo.icons.size() - 1)) / (1.0 * combo.icons.size()));
        }

        int totalHeight = bgMargin * 5 + titleHeight + panelHeight;
        int totalWidth = Math.max(bgMargin * 4 + titleWidth, bgMargin * 2 + panelWidth);

        BufferedImage result = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);

        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setColor(51, 53, 60, 255);
        g.fillRect( 0, 0, totalWidth, totalHeight);

        g.setColor(24, 25, 28, 255);
        g.fillRoundRect(0, -cornerRadius / 2, totalWidth, cornerRadius + bgMargin * 8 + titleHeight, cornerRadius, cornerRadius);

        g.setColor(64, 68, 75, 255);
        g.fillRoundRect(bgMargin, bgMargin * 4 + titleHeight, panelWidth, panelHeight, cornerRadius, cornerRadius);

        g.setFont(titleFont);
        g.setColor(238, 238, 238, 255);
        g.drawText(combo.title, (int) Math.round(bgMargin * 2 - titleRect.getX()), (int) Math.round(bgMargin * 2 - titleRect.getY()));

        g.setFont(typeFont);
        g.drawText(combo.type, (int) Math.round(bgMargin * 2 - typeRect.getX()), (int) Math.round(bgMargin * 2 + titleRect.getHeight() + comboTitleGap + (Math.max(typeRect.getHeight(), levelBoxDimension) - typeRect.getHeight()) / 2.0 - typeRect.getY()));

        g.setColor(88, 101, 242, 255);
        g.fillRoundRect((int) Math.round(bgMargin * 2 + typeRect.getWidth() + comboTypeGap), (int) Math.round(bgMargin * 2 + titleRect.getHeight() + comboTitleGap + (Math.max(typeRect.getHeight(), levelBoxDimension) - levelBoxDimension) / 2.0), levelBoxDimension, levelBoxDimension, comboTypeRadius, comboTypeRadius);

        g.setColor(238, 238, 238, 255);
        g.drawText(combo.level, (int) Math.round(bgMargin * 2 + typeRect.getWidth() + comboTypeGap + (Math.max(typeRect.getHeight(), levelBoxDimension) - levelRect.getWidth()) / 2.0 - levelRect.getX()), (int) Math.round(bgMargin * 2 + titleRect.getHeight() + comboTitleGap + (Math.max(typeRect.getHeight(), levelBoxDimension) - levelRect.getHeight()) / 2.0 - levelRect.getY()));

        int x = bgMargin + statPanelMargin;
        int y = bgMargin * 4 + titleHeight + statPanelMargin;

        g.setFont(nameFont);

        for(int i = 0; i < combo.icons.size(); i++) {
            Rectangle2D unitNameRect = nameFont.createGlyphVector(nfm.getFontRenderContext(), combo.names.get(i)).getPixelBounds(null, 0, 0);

            g.setColor(51, 53, 60, 255);
            g.fillRoundRect(x, y, maxIconTableWidth, maxIconTableHeight, comboIconTableRadius, comboIconTableRadius);

            g.drawImage(combo.icons.get(i), Math.round(x + (maxIconTableWidth - combo.icons.get(i).getWidth() * comboIconScaleFactor) / 2.0), y + comboIconUpDownGap, (int) Math.round(combo.icons.get(i).getWidth() * comboIconScaleFactor), (int) Math.round(combo.icons.get(i).getHeight() * comboIconScaleFactor));

            g.setColor(191, 191, 191, 255);
            g.drawText(combo.names.get(i), (int) Math.round(x + (maxIconTableWidth - unitNameRect.getWidth()) / 2.0 - unitNameRect.getX()), (int) Math.round(y + comboIconUpDownGap + combo.icons.get(i).getHeight() * comboIconScaleFactor + comboIconNameGap - unitNameRect.getY()));

            x += maxIconTableWidth + comboIconGap;
        }

        x = bgMargin + statPanelMargin;
        y += maxIconTableHeight + comboContentGap;

        g.setFont(contentFont);
        g.setColor(238, 238, 238, 255);
        g.drawText(combo.description, (int) Math.round(x - descRect.getX()), (int) Math.round(y - descRect.getY()));

        ImageIO.write(result, "PNG", image);

        return image;
    }

    public static Object[] plotGraph(BigDecimal[][] coordinates, BigDecimal[] xRange, BigDecimal[] yRange, boolean keepRatio, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        BigDecimal xWidth = xRange[1].subtract(xRange[0]);
        BigDecimal yWidth = yRange[1].subtract(yRange[0]);

        if(yWidth.divide(xWidth, Equation.context).compareTo(BigDecimal.valueOf(10)) > 0 || yWidth.compareTo(BigDecimal.ZERO) == 0)
            keepRatio = true;

        BufferedImage result = new BufferedImage(plotWidthHeight, plotWidthHeight, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        g.setColor(51, 53, 60, 255);
        g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

        if(keepRatio) {
            BigDecimal center = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

            yRange[0] = center.subtract(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
            yRange[1] = center.add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));

            yWidth = yRange[1].subtract(yRange[0]);
        }

        BigDecimal centerX = xRange[0].add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
        BigDecimal centerY = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

        int xLine = convertCoordinateToPixel(BigDecimal.ZERO, xWidth, centerX, true);
        int yLine = convertCoordinateToPixel(BigDecimal.ZERO, yWidth, centerY, false);

        g.setColor(238, 238, 238, 255);
        g.setStroke(axisStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        g.drawLine(xLine, 0, xLine, plotWidthHeight);
        g.drawLine(0, yLine, plotWidthHeight, yLine);

        BigDecimal xSegment = xWidth.divide(BigDecimal.TEN, Equation.context);

        int xScale = (int) - (Math.round(Math.log10(xSegment.doubleValue())) + 0.5 - 0.5 * Math.signum(xSegment.doubleValue()));

        if (xScale >= 0) {
            xSegment = xSegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            xSegment = xSegment.divide(BigDecimal.TEN.pow(-xScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-xScale));
        }

        BigDecimal ySegment = yWidth.divide(BigDecimal.TEN, Equation.context);

        int yScale = (int) - (Math.round(Math.log10(ySegment.doubleValue())) + 0.5 - 0.5 * Math.signum(ySegment.doubleValue()));

        if (yScale >= 0) {
            ySegment = ySegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            ySegment = ySegment.divide(BigDecimal.TEN.pow(-yScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-yScale));
        }

        BigDecimal xPosition = xRange[0].divideToIntegralValue(xSegment).multiply(xSegment);
        BigDecimal yPosition = yRange[0].divideToIntegralValue(ySegment).multiply(ySegment);

        while(xPosition.compareTo(xRange[1]) <= 0) {
            if(xPosition.compareTo(BigDecimal.ZERO) != 0) {
                int xPos = convertCoordinateToPixel(xPosition, xWidth, centerX, true);

                g.setColor(238, 238, 238, 255);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPos, (int) Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0), xPos, (int) Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0));

                g.setColor(238, 238, 238, 64);
                g.setStroke(subIndicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPos, 0, xPos, plotWidthHeight);

                long textPosition;

                boolean positive = true;

                if(yLine < - plotWidthHeight * indicatorRatio / 2.0) {
                    textPosition = indicatorGap;
                } else if(yLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                    positive = false;

                    textPosition = plotWidthHeight - indicatorGap;
                } else {
                    textPosition = Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                    if(textPosition > plotWidthHeight) {
                        positive = false;
                        textPosition = Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                    }
                }

                g.setFont(plotFont);
                g.setColor(238, 238, 238, 255);

                if(positive) {
                    g.drawHorizontalCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                } else {
                    g.drawHorizontalLowerCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                }
            }

            xPosition = xPosition.add(xSegment);
        }

        while(yPosition.compareTo(yRange[1]) <= 0) {
            if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                int yPos = convertCoordinateToPixel(yPosition, yWidth, centerY, false);

                g.setColor(238, 238, 238, 255);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine((int) Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0), yPos, (int) Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0), yPos);

                g.setColor(238, 238, 238, 64);
                g.setStroke(subIndicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(0, yPos, plotWidthHeight, yPos);

                long textPosition;

                boolean positive = true;

                if(xLine < - plotWidthHeight * indicatorRatio / 2.0) {
                    textPosition = indicatorGap;
                } else if(xLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                    positive = false;

                    textPosition = plotWidthHeight - indicatorGap;
                } else {
                    textPosition = Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                    if(textPosition > plotWidthHeight) {
                        positive = false;

                        textPosition = Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                    }
                }

                g.setFont(plotFont);
                g.setColor(238, 238, 238, 255);

                if(positive) {
                    g.drawVerticalCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                } else {
                    g.drawVerticalLowerCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                }
            }

            yPosition = yPosition.add(ySegment);
        }

        g.setColor(118, 224, 85, 255);
        g.setStroke(plotStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 0; i < coordinates.length - 1; i++) {
            if(coordinates[i][1] == null || coordinates[i + 1][1] == null || coordinates[i][0] == null || coordinates[i + 1][0] == null)
                continue;

            int x0 = convertCoordinateToPixel(coordinates[i][0], xWidth, centerX, true);
            int x1 = convertCoordinateToPixel(coordinates[i + 1][0], xWidth, centerX, true);

            int y0 = convertCoordinateToPixel(coordinates[i][1], yWidth, centerY, false);
            int y1 = convertCoordinateToPixel(coordinates[i + 1][1], yWidth, centerY, false);

            double angle = Math.abs(Math.toDegrees(Math.atan2(coordinates[i + 1][1].subtract(coordinates[i][1]).doubleValue(), coordinates[i + 1][0].subtract(coordinates[i][0]).doubleValue())));
            int v = (int) angle / 90;

            angle = angle - 90 * v;

            if (angle > angleLimit) {
                continue;
            }

            g.drawLine(x0, y0, x1, y1);
        }

        String text = String.format(
                LangID.getStringByID("plot_success", lang),
                Equation.formatNumber(centerX.subtract(xWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                Equation.formatNumber(centerX.add(xWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                Equation.formatNumber(centerY.subtract(yWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                Equation.formatNumber(centerY.add(yWidth.divide(BigDecimal.valueOf(2), Equation.context)))
        );

        ImageIO.write(result, "PNG", image);

        return new Object[] {image, text};
    }

    public static Object[] plotTGraph(BigDecimal[][] coordinates, BigDecimal[] xRange, BigDecimal[] yRange, BigDecimal[] tRange, boolean keepRatio, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        BigDecimal xWidth = xRange[1].subtract(xRange[0]);
        BigDecimal yWidth = yRange[1].subtract(yRange[0]);

        if(yWidth.divide(xWidth, Equation.context).compareTo(BigDecimal.valueOf(10)) > 0 || yWidth.compareTo(BigDecimal.ZERO) == 0)
            keepRatio = true;

        BufferedImage result = new BufferedImage(plotWidthHeight, plotWidthHeight, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        g.setColor(51, 53, 60, 255);
        g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

        if(keepRatio) {
            BigDecimal center = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

            yRange[0] = center.subtract(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
            yRange[1] = center.add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));

            yWidth = yRange[1].subtract(yRange[0]);
        }

        BigDecimal centerX = xRange[0].add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
        BigDecimal centerY = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

        int xLine = convertCoordinateToPixel(BigDecimal.ZERO, xWidth, centerX, true);
        int yLine = convertCoordinateToPixel(BigDecimal.ZERO, yWidth, centerY, false);

        g.setColor(238, 238, 238, 255);
        g.setStroke(axisStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        g.drawLine(xLine, 0, xLine, plotWidthHeight);
        g.drawLine(0, yLine, plotWidthHeight, yLine);

        BigDecimal xSegment = xWidth.divide(BigDecimal.TEN, Equation.context);

        int xScale = (int) - (Math.round(Math.log10(xSegment.doubleValue())) + 0.5 - 0.5 * Math.signum(xSegment.doubleValue()));

        if (xScale >= 0) {
            xSegment = xSegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            xSegment = xSegment.divide(BigDecimal.TEN.pow(-xScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-xScale));
        }

        BigDecimal ySegment = yWidth.divide(BigDecimal.TEN, Equation.context);

        int yScale = (int) - (Math.round(Math.log10(ySegment.doubleValue())) + 0.5 - 0.5 * Math.signum(ySegment.doubleValue()));

        if (yScale >= 0) {
            ySegment = ySegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            ySegment = ySegment.divide(BigDecimal.TEN.pow(-yScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-yScale));
        }

        BigDecimal xPosition = xRange[0].divideToIntegralValue(xSegment).multiply(xSegment);
        BigDecimal yPosition = yRange[0].divideToIntegralValue(ySegment).multiply(ySegment);

        while(xPosition.compareTo(xRange[1]) <= 0) {
            if(xPosition.compareTo(BigDecimal.ZERO) != 0) {
                int xPos = convertCoordinateToPixel(xPosition, xWidth, centerX, true);

                g.setColor(238, 238, 238, 255);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPos, (int) Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0), xPos, (int) Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0));

                g.setColor(238, 238, 238, 64);
                g.setStroke(subIndicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPos, 0, xPos, plotWidthHeight);

                long textPosition;

                boolean positive = true;

                if(yLine < - plotWidthHeight * indicatorRatio / 2.0) {
                    textPosition = indicatorGap;
                } else if(yLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                    positive = false;

                    textPosition = plotWidthHeight - indicatorGap;
                } else {
                    textPosition = Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                    if(textPosition > plotWidthHeight) {
                        positive = false;
                        textPosition = Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                    }
                }

                g.setFont(plotFont);
                g.setColor(238, 238, 238, 255);

                if(positive) {
                    g.drawHorizontalCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                } else {
                    g.drawHorizontalLowerCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                }
            }

            xPosition = xPosition.add(xSegment);
        }

        while(yPosition.compareTo(yRange[1]) <= 0) {
            if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                int yPos = convertCoordinateToPixel(yPosition, yWidth, centerY, false);

                g.setColor(238, 238, 238, 255);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine((int) Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0), yPos, (int) Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0), yPos);

                g.setColor(238, 238, 238, 64);
                g.setStroke(subIndicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(0, yPos, plotWidthHeight, yPos);

                long textPosition;

                boolean positive = true;

                if(xLine < - plotWidthHeight * indicatorRatio / 2.0) {
                    textPosition = indicatorGap;
                } else if(xLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                    positive = false;

                    textPosition = plotWidthHeight - indicatorGap;
                } else {
                    textPosition = Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                    if(textPosition > plotWidthHeight) {
                        positive = false;

                        textPosition = Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                    }
                }

                g.setFont(plotFont);
                g.setColor(238, 238, 238, 255);

                if(positive) {
                    g.drawVerticalCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                } else {
                    g.drawVerticalLowerCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                }
            }

            yPosition = yPosition.add(ySegment);
        }

        g.setColor(118, 224, 85, 255);
        g.setStroke(plotStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 0; i < coordinates.length - 1; i++) {
            if(coordinates[i][1] == null || coordinates[i + 1][1] == null || coordinates[i][0] == null || coordinates[i + 1][0] == null)
                continue;

            int x0 = convertCoordinateToPixel(coordinates[i][0], xWidth, centerX, true);
            int x1 = convertCoordinateToPixel(coordinates[i + 1][0], xWidth, centerX, true);

            int y0 = convertCoordinateToPixel(coordinates[i][1], yWidth, centerY, false);
            int y1 = convertCoordinateToPixel(coordinates[i + 1][1], yWidth, centerY, false);

            double angle = Math.abs(Math.toDegrees(Math.atan2(coordinates[i + 1][1].subtract(coordinates[i][1]).doubleValue(), coordinates[i + 1][0].subtract(coordinates[i][0]).doubleValue())));
            int v = (int) angle / 90;

            angle = angle - 90 * v;

            if (angle > angleLimit) {
                continue;
            }

            g.drawLine(x0, y0, x1, y1);
        }

        String text = String.format(
                LangID.getStringByID("tplot_success", lang),
                Equation.formatNumber(tRange[0]),
                Equation.formatNumber(tRange[1]),
                Equation.formatNumber(centerX.subtract(xWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                Equation.formatNumber(centerX.add(xWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                Equation.formatNumber(centerY.subtract(yWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                Equation.formatNumber(centerY.add(yWidth.divide(BigDecimal.valueOf(2), Equation.context)))
        );

        ImageIO.write(result, "PNG", image);

        return new Object[] {image, text};
    }

    public static Object[] plotXYGraph(Formula formula, BigDecimal[] xRange, BigDecimal[] yRange, boolean keepRatio, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        double xWidth = xRange[1].doubleValue() - xRange[0].doubleValue();
        double yWidth = yRange[1].doubleValue() - yRange[0].doubleValue();

        if(yWidth / xWidth > 10 || yWidth == 0)
            keepRatio = true;

        BufferedImage result = new BufferedImage(plotWidthHeight, plotWidthHeight, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        g.setColor(51, 53, 60, 255);
        g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

        if(keepRatio) {
            BigDecimal center = yRange[0].add(yRange[1]).divide(BigDecimal.valueOf(2), Equation.context);

            yRange[0] = center.subtract(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(2), Equation.context));
            yRange[1] = center.add(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(2), Equation.context));

            yWidth = yRange[1].doubleValue() - yRange[0].doubleValue();
        }

        double centerX = xRange[0].doubleValue() + xWidth / 2.0;
        double centerY = yRange[0].doubleValue() + yWidth / 2.0;

        int xLine = convertCoordinateToPixel(0, xWidth, centerX, true);
        int yLine = convertCoordinateToPixel(0, yWidth, centerY, false);

        g.setColor(238, 238, 238, 255);
        g.setStroke(axisStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        g.drawLine(xLine, 0, xLine, plotWidthHeight);
        g.drawLine(0, yLine, plotWidthHeight, yLine);

        BigDecimal xSegment = BigDecimal.valueOf(xWidth).divide(BigDecimal.TEN, Equation.context);

        int xScale = (int) - (Math.round(Math.log10(xSegment.doubleValue())) + 0.5 - 0.5 * Math.signum(xSegment.doubleValue()));

        if (xScale >= 0) {
            xSegment = xSegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            xSegment = xSegment.divide(BigDecimal.TEN.pow(-xScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-xScale));
        }

        BigDecimal ySegment = BigDecimal.valueOf(yWidth).divide(BigDecimal.TEN, Equation.context);

        int yScale = (int) - (Math.round(Math.log10(ySegment.doubleValue())) + 0.5 - 0.5 * Math.signum(ySegment.doubleValue()));

        if (yScale >= 0) {
            ySegment = ySegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            ySegment = ySegment.divide(BigDecimal.TEN.pow(-yScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-yScale));
        }

        BigDecimal xPosition = xRange[0].divideToIntegralValue(xSegment).multiply(xSegment);
        BigDecimal yPosition = yRange[0].divideToIntegralValue(ySegment).multiply(ySegment);

        while(xPosition.compareTo(xRange[1]) <= 0) {
            if(xPosition.compareTo(BigDecimal.ZERO) != 0) {
                int xPos = convertCoordinateToPixel(xPosition.doubleValue(), xWidth, centerX, true);

                g.setColor(238, 238, 238, 255);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPos, (int) Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0), xPos, (int) Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0));

                g.setColor(238, 238, 238, 64);
                g.setStroke(subIndicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPos, 0, xPos, plotWidthHeight);

                long textPosition;

                boolean positive = true;

                if(yLine < - plotWidthHeight * indicatorRatio / 2.0) {
                    textPosition = indicatorGap;
                } else if(yLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                    positive = false;

                    textPosition = plotWidthHeight - indicatorGap;
                } else {
                    textPosition = Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                    if(textPosition > plotWidthHeight) {
                        positive = false;
                        textPosition = Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                    }
                }

                g.setFont(plotFont);
                g.setColor(238, 238, 238, 255);

                if(positive) {
                    g.drawHorizontalCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                } else {
                    g.drawHorizontalLowerCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                }
            }

            xPosition = xPosition.add(xSegment);
        }

        while(yPosition.compareTo(yRange[1]) <= 0) {
            if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                int yPos = convertCoordinateToPixel(yPosition.doubleValue(), yWidth, centerY, false);

                g.setColor(238, 238, 238, 255);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine((int) Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0), yPos, (int) Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0), yPos);

                g.setColor(238, 238, 238, 64);
                g.setStroke(subIndicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(0, yPos, plotWidthHeight, yPos);

                long textPosition;

                boolean positive = true;

                if(xLine < - plotWidthHeight * indicatorRatio / 2.0) {
                    textPosition = indicatorGap;
                } else if(xLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                    positive = false;

                    textPosition = plotWidthHeight - indicatorGap;
                } else {
                    textPosition = Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                    if(textPosition > plotWidthHeight) {
                        positive = false;

                        textPosition = Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                    }
                }

                g.setFont(plotFont);
                g.setColor(238, 238, 238, 255);

                if(positive) {
                    g.drawVerticalCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                } else {
                    g.drawVerticalLowerCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                }
            }

            yPosition = yPosition.add(ySegment);
        }

        g.setColor(118, 224, 85, 255);
        g.setStroke(plotStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        double segment = plotWidthHeight * 1.0 / Formula.numberOfElements;

        for(int x = 0; x < Formula.numberOfElements; x++) {
            double xv = convertPixelToCoordinate(x * segment, xWidth, centerX, true);

            Formula substituted = formula.getInjectedFormula(xv, 0);

            if(substituted == null)
                continue;

            for(int y = 0; y < Formula.numberOfElements - 1; y++) {
                double y0 = convertPixelToCoordinate(y * segment, yWidth, centerY, false);
                double y1 = convertPixelToCoordinate((y + 1) * segment, yWidth, centerY, false);

                double v0 = substituted.substitute(y0);

                if(formula.element.isCritical()) {
                    return null;
                }

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    Equation.error.clear();

                    continue;
                }

                if(v0 == 0) {
                    g.fillOval((int) Math.round(x * segment), (int) Math.round(y * segment), 3, 3);

                    continue;
                }

                double v1 = substituted.substitute(y1);

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    Equation.error.clear();

                    y++;

                    continue;
                }

                if(v1 == 0) {
                    g.fillOval((int) Math.round(x * segment), (int) Math.round((y + 1) * segment), 3, 3);

                    y++;
                } else if(v0 * v1 < 0) {
                    g.fillOval((int) Math.round(x * segment), convertCoordinateToPixel(-v1 * (y1 - y0) / (v1 - v0) + y1, yWidth, centerY, false), 3, 3);
                }
            }
        }

        for(int y = 0; y < Formula.numberOfElements; y++) {
            double yv = convertPixelToCoordinate(y * segment, yWidth, centerY, false);

            Formula substituted = formula.getInjectedFormula(yv, 1);

            if(substituted == null)
                continue;

            for(int x = 0; x < Formula.numberOfElements - 1; x++) {
                double x0 = convertPixelToCoordinate(x * segment, xWidth, centerX, true);
                double x1 = convertPixelToCoordinate((x + 1) * segment, xWidth, centerX, true);

                double v0 = substituted.substitute(x0);

                if(formula.element.isCritical()) {
                    return null;
                }

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    Equation.error.clear();

                    continue;
                }

                if(v0 == 0) {
                    g.fillOval((int) Math.round(x * segment), (int) Math.round(y * segment), 3, 3);
                }

                double v1 = substituted.substitute(x1);

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    Equation.error.clear();

                    x++;

                    continue;
                }

                if(v1 == 0) {
                    g.fillOval((int) Math.round((x + 1) * segment), (int) Math.round(y * segment), 3, 3);

                    x++;
                } else if(v0 * v1 < 0) {
                    g.fillOval(convertCoordinateToPixel(-v1 * (x1 - x0) / (v1 - v0) + x1, xWidth, centerX, true), (int) Math.round(y * segment), 3, 3);
                }
            }
        }

        ImageIO.write(result, "PNG", image);

        String text = String.format(
                LangID.getStringByID("plot_success", lang),
                DataToString.df.format(centerX - xWidth / 2.0),
                DataToString.df.format(centerX + xWidth / 2.0),
                DataToString.df.format(centerY - yWidth / 2.0),
                DataToString.df.format(centerY + yWidth / 2.0)
        );

        return new Object[] {image, text};
    }

    public static Object[] plotRThetaGraph(Formula formula, BigDecimal[] xRange, BigDecimal[] yRange, double[] rRange, double[] tRange, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        double xWidth = xRange[1].doubleValue() - xRange[0].doubleValue();
        double yWidth = yRange[1].doubleValue() - yRange[0].doubleValue();

        if(yWidth != xWidth) {
            BigDecimal center = yRange[0].add(yRange[1]).divide(BigDecimal.valueOf(2), Equation.context);

            yRange[0] = center.subtract(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(2), Equation.context));
            yRange[1] = center.add(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(2), Equation.context));

            yWidth = yRange[1].doubleValue() - yRange[0].doubleValue();
        }

        BufferedImage result = new BufferedImage(plotWidthHeight, plotWidthHeight, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        g.setColor(51, 53, 60, 255);
        g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

        double centerX = xRange[0].doubleValue() + xWidth / 2.0;
        double centerY = yRange[0].doubleValue() + yWidth / 2.0;

        int xLine = convertCoordinateToPixel(0, xWidth, centerX, true);
        int yLine = convertCoordinateToPixel(0, yWidth, centerY, false);

        g.setColor(238, 238, 238, 255);
        g.setStroke(axisStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        g.drawLine(xLine, 0, xLine, plotWidthHeight);
        g.drawLine(0, yLine, plotWidthHeight, yLine);

        BigDecimal xSegment = BigDecimal.valueOf(xWidth).divide(BigDecimal.TEN, Equation.context);

        int xScale = (int) - (Math.round(Math.log10(xSegment.doubleValue())) + 0.5 - 0.5 * Math.signum(xSegment.doubleValue()));

        if (xScale >= 0) {
            xSegment = xSegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            xSegment = xSegment.divide(BigDecimal.TEN.pow(-xScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-xScale));
        }

        BigDecimal ySegment = BigDecimal.valueOf(yWidth).divide(BigDecimal.TEN, Equation.context);

        int yScale = (int) - (Math.round(Math.log10(ySegment.doubleValue())) + 0.5 - 0.5 * Math.signum(ySegment.doubleValue()));

        if (yScale >= 0) {
            ySegment = ySegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            ySegment = ySegment.divide(BigDecimal.TEN.pow(-yScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-yScale));
        }

        BigDecimal xPosition = xRange[0].divideToIntegralValue(xSegment).multiply(xSegment);
        BigDecimal yPosition = yRange[0].divideToIntegralValue(ySegment).multiply(ySegment);

        int zeroX = convertCoordinateToPixel(0, xWidth, centerX, true);
        int zeroY = convertCoordinateToPixel(0, yWidth, centerY, false);

        BigDecimal xw = xRange[1].max(xRange[0]);
        BigDecimal yw = yRange[1].max(yRange[0]);

        while(xPosition.compareTo(xw.pow(2).add(yw.pow(2)).sqrt(Equation.context)) <= 0) {
            if(xPosition.compareTo(BigDecimal.ZERO) != 0) {
                int xPos = convertCoordinateToPixel(xPosition.doubleValue(), xWidth, centerX, true);

                g.setColor(238, 238, 238, 64);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawOval(zeroX * 2 - xPos, zeroY + zeroX - xPos, (xPos - zeroX) * 2, (xPos - zeroX) * 2);

                g.setStroke(indicatorStroke / 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                for(int i = 1; i < 5; i++) {
                    int subXPos = convertCoordinateToPixel(xPosition.doubleValue() + xSegment.doubleValue() / 5.0 * i, xWidth, centerX, true);

                    g.drawOval(zeroX * 2 - subXPos, zeroY + zeroX - subXPos, (subXPos - zeroX) * 2, (subXPos - zeroX) * 2);
                }

                if(xPosition.compareTo(xRange[1]) <= 0) {
                    g.setColor(238, 238, 238, 255);
                    g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                    g.drawLine(xPos, (int) Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0), xPos, (int) Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0));

                    long textPosition;

                    boolean positive = true;

                    if(yLine < - plotWidthHeight * indicatorRatio / 2.0) {
                        textPosition = indicatorGap;
                    } else if(yLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                        positive = false;

                        textPosition = plotWidthHeight - indicatorGap;
                    } else {
                        textPosition = Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                        if(textPosition > plotWidthHeight) {
                            positive = false;
                            textPosition = Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                        }
                    }

                    g.setFont(plotFont);
                    g.setColor(238, 238, 238, 255);

                    if(positive) {
                        g.drawHorizontalCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                    } else {
                        g.drawHorizontalLowerCenteredText(Equation.simpleNumber(xPosition), xPos, (int) textPosition);
                    }
                }
            }

            xPosition = xPosition.add(xSegment);
        }

        while(yPosition.compareTo(yRange[1]) <= 0) {
            if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                int yPos = convertCoordinateToPixel(yPosition.doubleValue(), yWidth, centerY, false);

                g.setColor(238, 238, 238, 255);
                g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine((int) Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0), yPos, (int) Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0), yPos);

                long textPosition;

                boolean positive = true;

                if(xLine < - plotWidthHeight * indicatorRatio / 2.0) {
                    textPosition = indicatorGap;
                } else if(xLine >= plotWidthHeight * (1 + indicatorRatio / 2.0)) {
                    positive = false;

                    textPosition = plotWidthHeight - indicatorGap;
                } else {
                    textPosition = Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap);

                    if(textPosition > plotWidthHeight) {
                        positive = false;

                        textPosition = Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap);
                    }
                }

                g.setFont(plotFont);
                g.setColor(238, 238, 238, 255);

                if(positive) {
                    g.drawVerticalCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                } else {
                    g.drawVerticalLowerCenteredText(Equation.simpleNumber(yPosition), (int) textPosition, yPos);
                }
            }

            yPosition = yPosition.add(ySegment);
        }

        g.setColor(238, 238, 238, 64);
        g.setStroke(indicatorStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 1; i < 12; i++) {
            if(i == 6)
                continue;

            double slope = Math.tan(Math.PI / 12.0 * i);

            int yMin = convertCoordinateToPixel(xRange[0].doubleValue() * slope, yWidth, centerY, false);
            int yMax = convertCoordinateToPixel(xRange[1].doubleValue() * slope, yWidth, centerY, false);

            g.drawLine(0, yMin,plotWidthHeight, yMax);
        }

        g.setColor(118, 224, 85, 255);
        g.setStroke(plotStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        int number = 4096;

        double segment = 1.0 / number;

        double rWidth = rRange[1] - rRange[0];
        double tWidth;

        double totalAngle = tRange[1] - tRange[0];

        double[] square = new double[] {
                Math.atan2(yRange[1].doubleValue(), xRange[0].doubleValue()),
                Math.atan2(yRange[0].doubleValue(), xRange[0].doubleValue()),
                Math.atan2(yRange[0].doubleValue(), xRange[1].doubleValue()),
                Math.atan2(yRange[1].doubleValue(), xRange[1].doubleValue())
        };

        double minAngle = square[0];
        double maxAngle = square[0];

        for(int i = 1; i < square.length; i++) {
            minAngle = Math.min(minAngle, square[i]);
            maxAngle = Math.max(maxAngle, square[i]);
        }

        double angleRange= maxAngle - minAngle;

        if(0 <= zeroX && zeroX <= plotWidthHeight && 0 <= zeroY && zeroY <= plotWidthHeight) {
            tWidth = tRange[1] - tRange[0];
        } else {
            double actualAngle = 0;
            double summedAngle = 0;

            while(summedAngle <= totalAngle) {
                summedAngle += minAngle;

                if(summedAngle > totalAngle)
                    break;

                if(summedAngle + angleRange > totalAngle) {
                    actualAngle += totalAngle - summedAngle;

                    break;
                } else {
                    actualAngle += angleRange;
                    summedAngle += Math.PI;
                }
            }

            tWidth = actualAngle;
        }

        int pi = 0;
        int back = 0;

        for(int t = 0; t < number + 1; t++) {
            double tv;

            if(tWidth == totalAngle) {
                tv = tRange[0] + (t - back) * segment * tWidth;
            } else {
                tv = Math.PI * pi + minAngle + (t - back) * segment * tWidth;
            }

            if(tv > tRange[1])
                break;

            Formula substituted = formula.getInjectedFormula(tv, 1);

            if(substituted == null) {
                if(tWidth != totalAngle && tv - Math.PI * pi > maxAngle) {
                    pi++;
                    back = t;
                }

                continue;
            }

            for(int r = 0; r < number; r++) {
                double r0 = rRange[0] + r * segment * rWidth;
                double r1 = rRange[0] + (r + 1) * segment * rWidth;

                double v0 = substituted.substitute(r0);

                if(formula.element.isCritical()) {
                    return null;
                }

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    Equation.error.clear();

                    continue;
                }

                if(v0 == 0) {
                    g.fillOval(
                            convertCoordinateToPixel(r0 * Math.cos(tv), xWidth, centerX, true),
                            convertCoordinateToPixel(r0 * Math.sin(tv), yWidth, centerY, false),
                            3, 3
                    );
                }

                double v1 = substituted.substitute(r1);

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    Equation.error.clear();

                    r++;

                    continue;
                }

                if(v1 == 0) {
                    g.fillOval(
                            convertCoordinateToPixel(r1 * Math.cos(tv), xWidth, centerX, true),
                            convertCoordinateToPixel(r1 * Math.sin(tv), yWidth, centerY, false),
                            3, 3
                    );

                    r++;
                } else if(v0 * v1 < 0) {
                    double rp = -v1 * (r1 - r0) / (v1 - v0) + r1;

                    g.fillOval(
                            convertCoordinateToPixel(rp * Math.cos(tv), xWidth, centerX, true),
                            convertCoordinateToPixel(rp * Math.sin(tv), yWidth, centerY, false),
                            3, 3
                    );
                }
            }

            if(tWidth != totalAngle && tv - Math.PI * pi > maxAngle) {
                pi++;
                back = t;
            }
        }

        for(int r = 0; r < number + 1; r++) {
            double rv = rRange[0] + r * segment * rWidth;

            Formula substituted = formula.getInjectedFormula(rv, 0);

            if(substituted == null)
                continue;

            pi = 0;
            back = 0;

            for(int t = 0; t < number; t++) {
                double t0;
                double t1;

                if(tWidth == totalAngle) {
                    t0 = tRange[0] + (t - back) * segment * tWidth;
                    t1 = tRange[0] + (t - back + 1) * segment * tWidth;
                } else {
                    t0 = Math.PI * pi + minAngle + t * segment * angleRange;
                    t1 = Math.PI * pi + minAngle + (t + 1) * segment * angleRange;
                }

                if(t1 > tRange[1])
                    break;

                double v0 = substituted.substitute(t0);

                if(formula.element.isCritical()) {
                    return null;
                }

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    if(tWidth != totalAngle && t0 - Math.PI * pi > maxAngle) {
                        pi++;
                        back = t;
                    }

                    Equation.error.clear();

                    continue;
                }

                if(v0 == 0) {
                    g.fillOval(
                            convertCoordinateToPixel(rv * Math.cos(t0), xWidth, centerX, true),
                            convertCoordinateToPixel(rv * Math.sin(t0), yWidth, centerY, false),
                            3, 3
                    );
                }

                double v1 = substituted.substitute(t1);

                if(!Equation.error.isEmpty() || substituted.element.isAborted()) {
                    Equation.error.clear();

                    t++;

                    if(tWidth != totalAngle && t0 - Math.PI * pi > maxAngle) {
                        pi++;
                        back = t;
                    }

                    continue;
                }

                if(v1 == 0) {
                    g.fillOval(
                            convertCoordinateToPixel(rv * Math.cos(t1), xWidth, centerX, true),
                            convertCoordinateToPixel(rv * Math.sin(t1), yWidth, centerY, false),
                            3, 3
                    );

                    t++;
                } else if(v0 * v1 < 0) {
                    double slope = (v1 - v0) / (t1 - t0);
                    double angle = Math.toDegrees(Math.atan(Math.abs(slope)));

                    if(angle > 89.99) {
                        if(tWidth != totalAngle && t0 - Math.PI * pi > maxAngle) {
                            pi++;
                            back = t;
                        }

                        continue;
                    }

                    double tp = -v1 * (t1 - t0) / (v1 - v0) + t1;

                    g.fillOval(
                            convertCoordinateToPixel(rv * Math.cos(tp), xWidth, centerX, true),
                            convertCoordinateToPixel(rv * Math.sin(tp), yWidth, centerY, false),
                            3, 3
                    );
                }

                if(tWidth != totalAngle && t0 - Math.PI * pi > maxAngle) {
                    pi++;
                    back = t;
                }
            }
        }

        ImageIO.write(result, "PNG", image);

        String text = String.format(
                LangID.getStringByID("rplot_success", lang),
                DataToString.df.format(tRange[0]),
                DataToString.df.format(tRange[1]),
                DataToString.df.format(rRange[0]),
                DataToString.df.format(rRange[1]),
                DataToString.df.format(centerX - xWidth / 2.0),
                DataToString.df.format(centerX + xWidth / 2.0),
                DataToString.df.format(centerY - yWidth / 2.0),
                DataToString.df.format(centerY + yWidth / 2.0)
        );

        return new Object[] {image, text};
    }

    private static int convertCoordinateToPixel(BigDecimal coordinate, BigDecimal range, BigDecimal center, boolean x) {
        if(range.compareTo(BigDecimal.ZERO) == 0)
            return -1;

        if(x) {
            return coordinate.subtract(center).add(range.divide(BigDecimal.valueOf(2), Equation.context)).divide(range, Equation.context).multiply(BigDecimal.valueOf(plotWidthHeight)).round(new MathContext(0, RoundingMode.HALF_EVEN)).intValue();
        } else {
            return range.divide(BigDecimal.valueOf(2), Equation.context).subtract(coordinate.subtract(center)).divide(range, Equation.context).multiply(BigDecimal.valueOf(plotWidthHeight)).round(new MathContext(0, RoundingMode.HALF_EVEN)).intValue();
        }
    }

    private static int convertCoordinateToPixel(double coordinate, double range, double center, boolean x) {
        if(range == 0)
            return -1;

        if(x) {
            return (int) Math.round((coordinate - center + range / 2.0) / range * plotWidthHeight);
        } else {
            return (int) Math.round((range / 2.0 - (coordinate - center)) / range * plotWidthHeight);
        }
    }

    private static double convertPixelToCoordinate(double pixel, double range, double center, boolean x) {
        if(range == 0)
            return 0;

        double segment = range / plotWidthHeight;

        if (x) {
            return center - range / 2.0 + segment * pixel;
        } else {
            return center + range / 2.0 - segment * pixel;
        }
    }

    private static String getUnitCode(int ind) {
        return switch (ind) {
            case 0 -> "f";
            case 1 -> "c";
            case 2 -> "s";
            default -> String.valueOf(ind);
        };
    }

    private static BufferedImage getUnitTitleImage(File icon, String name, String type, int lv, FontMetrics bfm, FontMetrics sfm, FontMetrics lfm) throws Exception {
        BufferedImage ic = ImageIO.read(icon).getSubimage(9, 21, 110, 85);

        FontRenderContext bfrc = bfm.getFontRenderContext();
        FontRenderContext sfrc = sfm.getFontRenderContext();
        FontRenderContext lfrc = lfm.getFontRenderContext();

        Rectangle2D nRect = titleFont.createGlyphVector(bfrc, name).getPixelBounds(null, 0, 0);
        Rectangle2D tRect = typeFont.createGlyphVector(sfrc, type).getPixelBounds(null, 0, 0);
        Rectangle2D lRect = levelFont.createGlyphVector(lfrc, "Lv. "+lv).getPixelBounds(null, 0, 0);

        int h = (int) Math.round(nRect.getHeight() + nameMargin + tRect.getHeight() + typeUpDownMargin * 2 + levelMargin + lRect.getHeight());

        int icw = (int) ((h - lRect.getHeight() - levelMargin) * 1.0 * ic.getWidth() / ic.getHeight());

        int w = icw + nameMargin + (int) Math.max(nRect.getWidth(), tRect.getWidth() + typeLeftRightMargin * 2);

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setColor(238, 238, 238, 255);
        g.setFont(titleFont);

        g.drawText(name, (int) (icw + nameMargin - nRect.getX()), (int) (-nRect.getY()));

        g.setFont(levelFont);

        g.drawText("Lv. "+lv, (int) ((icw - lRect.getWidth()) / 2 - lRect.getX()), (int) (h - lRect.getHeight() - lRect.getY()));

        g.setColor(88, 101, 242, 255);

        g.fillRoundRect(icw + nameMargin, (int) (nRect.getHeight() + nameMargin), (int) (typeLeftRightMargin * 2 + tRect.getWidth()), (int) (typeUpDownMargin * 2 + tRect.getHeight()), typeCornerRadius, typeCornerRadius);

        g.setColor(238, 238, 238, 255);
        g.setFont(typeFont);

        g.drawText(type, (int) (icw + nameMargin + typeLeftRightMargin - tRect.getX()), (int) (nRect.getHeight() + nameMargin + typeUpDownMargin - tRect.getY()));

        g.drawImage(ic, 0, 0, icw, h - lRect.getHeight() - levelMargin);

        return result;
    }

    private static BufferedImage getEnemyTitleImage(File icon, String name, String mag, FontMetrics bfm, FontMetrics lfm) throws Exception {
        BufferedImage ic = ImageIO.read(icon);

        FontRenderContext bfrc = bfm.getFontRenderContext();
        FontRenderContext lfrc = lfm.getFontRenderContext();

        Rectangle2D nRect = titleFont.createGlyphVector(bfrc, name).getPixelBounds(null, 0, 0);
        Rectangle2D lRect = levelFont.createGlyphVector(lfrc, mag).getPixelBounds(null, 0, 0);

        int h = (int) Math.round(nRect.getHeight() + nameMargin + lRect.getHeight() + enemyIconGap * 3);

        int icw = (int) (h * enemyIconRatio);

        int w = (int) (icw + nameMargin + Math.max(nRect.getWidth(), lRect.getWidth()));

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setColor(238, 238, 238, 255);
        g.setFont(titleFont);

        g.drawText(name, (int) (icw + nameMargin - nRect.getX()), (int) (enemyIconGap -nRect.getY()));

        g.setFont(levelFont);

        if(CommonStatic.parseIntN(mag) != 100) {
            g.drawText(mag, (int) (icw + nameMargin - lRect.getX()), (int) (nRect.getHeight() + nameMargin + enemyIconGap - lRect.getY()));
        }

        g.setColor(54, 57, 63);

        g.fillRoundRect(enemyIconStroke / 2, enemyIconStroke / 2, icw - enemyIconStroke, h - enemyIconStroke, cornerRadius, cornerRadius);

        int size = (int) Math.min(h * enemyInnerIconRatio, h - enemyIconStroke * 2);

        g.drawImage(ic, (icw - h + enemyIconStroke) / 2.0, enemyIconStroke / 2.0, size, size);

        g.setColor(191, 191, 191);
        g.setStroke(enemyIconStroke);

        g.roundRect(enemyIconStroke / 2, enemyIconStroke / 2, icw - enemyIconStroke, h - enemyIconStroke, cornerRadius, cornerRadius);

        return result;
    }

    private static void drawStageTitleImage(FG2D g, String name, String code, FontMetrics bfm, FontMetrics lfm) {
        FontRenderContext bfrc = bfm.getFontRenderContext();
        FontRenderContext lfrc = lfm.getFontRenderContext();

        Rectangle2D nRect = titleFont.createGlyphVector(bfrc, name).getPixelBounds(null, 0, 0);
        Rectangle2D lRect = contentFont.createGlyphVector(lfrc, code).getPixelBounds(null, 0, 0);

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setColor(238, 238, 238, 255);
        g.setFont(titleFont);

        g.drawText(name, (int) (bgMargin + bgMargin - nRect.getX()), (int) (bgMargin * 2 - nRect.getY()));

        if(!code.equals(name)) {
            g.setColor(191, 191, 191);
            g.setFont(contentFont);

            g.drawText(code, (int) (bgMargin + bgMargin - lRect.getX()), (int) (bgMargin * 2 + nRect.getHeight() + nameMargin - lRect.getY()));
        }
    }

    private static BufferedImage generateEvolveImage(File container, int[][] data, int targetWidth, FontMetrics metrics) {
        GlyphVector glyph = fruitFont.createGlyphVector(metrics.getFontRenderContext(), "1234567890Mk");

        int textHeight = glyph.getPixelBounds(null, 0, 0).height;

        double h = textHeight + targetWidth * (fruitRatio + fruitTextGapRatio + fruitUpperGapRatio + fruitDownerGapRatio);

        BufferedImage img = new BufferedImage(targetWidth, (int) h, BufferedImage.TYPE_INT_ARGB);

        FG2D g = new FG2D(img.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        double panelPadding = targetWidth * (1.0 - fruitRatio * data.length) / (5.0 * data.length - 1);
        double padding = panelPadding * 2;

        double panelWidth = padding * 2 + fruitRatio * targetWidth;

        g.setFont(fruitFont);

        double x = 0;

        for(int i = 0; i < data.length; i++) {
            g.setColor(64, 68, 75, 255);

            g.fillRoundRect((int) x, 0, (int) panelWidth, (int) h, cornerRadius, cornerRadius);

            try {
                BufferedImage icon = getFruitImage(container, data[i][0]);

                FakeImage ic = FIBI.build(icon);

                g.drawImage(ic, x + padding, targetWidth * fruitUpperGapRatio, targetWidth * fruitRatio, targetWidth * fruitRatio);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::generateEvolveImage - Failed to generate fruit image : "+data[i][0]);
            }

            g.setColor(238, 238, 238, 255);

            g.drawCenteredText(convertValue(data[i][1]), (int) (x + panelWidth / 2), (int) Math.round(targetWidth * (fruitUpperGapRatio + fruitRatio + fruitTextGapRatio) + textHeight / 2.0));

            x += panelWidth + panelPadding;
        }

        return img;
    }

    private static BufferedImage getFruitImage(File container, int id) throws Exception {
        if(id == -1) {
            VFile vf = VFile.get("./org/page/catfruit/xp.png");

            if(vf != null) {
                return (BufferedImage) vf.getData().getImg().bimg();
            }
        } else {
            String name = "gatyaitemD_"+id+"_f.png";
            VFile vf = VFile.get("./org/page/catfruit/"+name);

            if(vf == null) {
                File icon = new File(container, name);

                if(icon.exists()) {
                    return ImageIO.read(icon);
                }
            } else {
                return (BufferedImage) vf.getData().getImg().bimg();
            }
        }

        return null;
    }

    /**
     *
     * @param st Stage
     * @param map Stage map
     * @param cfm Fonrt metrics to measure text width/height
     * @param lang Language value
     * @return Returns { Width of Chance, Width of Item, Width of Amount, Total Width, Total Height }
     */
    private static int[] measureDropTableWidth(Stage st, CustomStageMap map, FontMetrics cfm, int lang, boolean reward) {
        List<String[]> dropData;

        if(reward) {
            dropData = DataToString.getRewards(st, map, lang);
        } else {
            dropData = DataToString.getScoreDrops(st, map, lang);
        }

        if(dropData == null)
            return null;

        int[] result = new int[5];

        String chance;

        if(dropData.get(dropData.size() - 1).length == 1) {
            dropData.remove(dropData.size() - 1);

            chance = LangID.getStringByID("data_rewardno", lang);
        } else {
            chance = LangID.getStringByID("data_chance", lang);
        }

        int cw = cfm.stringWidth(chance);

        int rw = cfm.stringWidth(LangID.getStringByID("data_reward", lang));

        int aw = cfm.stringWidth(LangID.getStringByID("data_amount", lang));

        for(int i = 0; i < dropData.size(); i++) {
            String[] data = dropData.get(i);

            if(data.length != 3)
                continue;

            cw = Math.max(cw, cfm.stringWidth(data[0]));
            rw = Math.max(rw, cfm.stringWidth(data[1]));
            aw = Math.max(aw, cfm.stringWidth(data[2]));
        }

        result[CHANCE_WIDTH] = cw;
        result[REWARD_WIDTH] = rw;
        result[AMOUNT_WIDTH] = aw;
        result[TOTAL_WIDTH] = innerTableTextMargin * 2 + cw + innerTableTextMargin * 3 + rewardIconSize + rw + innerTableTextMargin * 2 + aw;
        result[TOTAL_HEIGHT] = innerTableCellMargin * (dropData.size() + 1);

        return result;
    }

    /**
     *
     * @param st Stage
     * @param map Stage map
     * @param cfm Fonrt metrics to measure text width/height
     * @param lang Language value
     * @return Returns { Width of Enemy, Width of Number, Width of Base, Width of Magnification, Width of Start, Width of Layer, Width of Boss, Total Width, Total Height }
     */
    private static int[] measureEnemySchemeWidth(Stage st, CustomStageMap map, FontMetrics cfm, boolean isRanking, boolean isFrame, int lv, int lang) {
        int[] result = new int[11];

        int ew = cfm.stringWidth(LangID.getStringByID("data_enemy", lang));
        int nw = cfm.stringWidth(LangID.getStringByID("data_number", lang));
        int bw = cfm.stringWidth(LangID.getStringByID(isRanking ? "data_basedealt" : "data_basehealth", lang));
        int mw = cfm.stringWidth(LangID.getStringByID("data_manif", lang));
        int sw = cfm.stringWidth(LangID.getStringByID("data_startres", lang));
        int lw = cfm.stringWidth(LangID.getStringByID("data_layer", lang));
        int rw = cfm.stringWidth(LangID.getStringByID("data_respect", lang));
        int kw = cfm.stringWidth(LangID.getStringByID("data_killcount", lang));
        int bow = cfm.stringWidth(LangID.getStringByID("data_isboss", lang));

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            SCDef.Line line = st.data.datas[i];

            Identifier<AbEnemy> id = line.enemy;

            String enemyName = null;

            if(id.id >= UserProfile.getBCData().enemies.size()) {
                enemyName = map.enemyNames.get(id.id);
            } else {
                AbEnemy enemy = id.get();

                if(enemy instanceof Enemy) {
                    enemyName = MultiLangCont.get(enemy, lang);

                    if(enemyName == null || enemyName.isBlank()) {
                        enemyName = ((Enemy) enemy).names.toString();
                    }

                    if(enemyName.isBlank()) {
                        enemyName = map.enemyNames.get(id.id);
                    }
                }
            }

            if(enemyName == null || enemyName.isBlank()) {
                enemyName = LangID.getStringByID("data_enemy", lang)+" - "+Data.trio(id.id);
            }

            ew = Math.max(ew, cfm.stringWidth(enemyName));

            String number;

            if(line.number == 0)
                number = LangID.getStringByID("data_infinite", lang);
            else
                number = String.valueOf(line.number);

            nw = Math.max(nw, cfm.stringWidth(number));

            String baseHP;
            String suffix = isRanking ? "" : "%";

            if(line.castle_0 == line.castle_1 || line.castle_1 == 0)
                baseHP = line.castle_0 + suffix;
            else {
                int minHealth = Math.min(line.castle_0, line.castle_1);
                int maxHealth = Math.max(line.castle_0, line.castle_1);

                baseHP = minHealth + " ~ " + maxHealth + suffix;
            }

            bw = Math.max(bw, cfm.stringWidth(baseHP));

            mw = Math.max(mw, cfm.stringWidth(DataToString.getMagnification(new int[] {line.multiple, line.mult_atk}, lv)));

            String start;

            if(line.spawn_1 == 0)
                if(isFrame)
                    start = Math.abs(line.spawn_0)+"f";
                else
                    start = DataToString.df.format(Math.abs(line.spawn_0) / 30.0)+"s";
            else {
                int minSpawn = Math.abs(Math.min(line.spawn_0, line.spawn_1));
                int maxSpawn = Math.abs(Math.max(line.spawn_0, line.spawn_1));

                if(isFrame)
                    start = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    start = DataToString.df.format(minSpawn/30.0)+"s ~ " + DataToString.df.format(maxSpawn/30.0)+"s";
            }

            String respawn;

            if(line.respawn_0 == line.respawn_1)
                if(isFrame)
                    respawn = line.respawn_0+"f";
                else
                    respawn = DataToString.df.format(line.respawn_0/30.0)+"s";
            else {
                int minSpawn = Math.min(line.respawn_0, line.respawn_1);
                int maxSpawn = Math.max(line.respawn_0, line.respawn_1);

                if(isFrame)
                    respawn = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    respawn = DataToString.df.format(minSpawn/30.0)+"s ~ "+DataToString.df.format(maxSpawn/30.0)+"s";
            }

            String startResp = start+" ("+respawn+")";

            sw = Math.max(sw, cfm.stringWidth(startResp));

            String layer;

            if(line.layer_0 != line.layer_1) {
                int minLayer = Math.min(line.layer_0, line.layer_1);
                int maxLayer = Math.max(line.layer_0, line.layer_1);

                layer = minLayer + " ~ " + maxLayer;
            } else {
                layer = String.valueOf(line.layer_0);
            }

            lw = Math.max(lw, cfm.stringWidth(layer));

            String respect = (line.spawn_0 < 0 || line.spawn_1 < 0) ? LangID.getStringByID("data_true", lang) : "";

            rw = Math.max(rw, cfm.stringWidth(respect));

            kw = Math.max(kw, cfm.stringWidth(String.valueOf(line.kill_count)));

            String boss;

            if(line.boss == 0)
                boss = "";
            else if(line.boss == 1)
                boss = LangID.getStringByID("data_boss", lang);
            else
                boss = LangID.getStringByID("data_bossshake", lang);

            bow = Math.max(bow, cfm.stringWidth(boss));
        }

        result[ENEMY] = ew;
        result[NUMBER] = nw;
        result[BASE] = bw;
        result[MAGNIFICATION] = mw;
        result[START] = sw;
        result[LAYER] = lw;
        result[RESPECT] = rw;
        result[KILL] = kw;
        result[BOSS] = bow;
        result[STAGE_WIDTH] =
                innerTableTextMargin * 3 + rewardIconSize + ew +
                        innerTableTextMargin * 2 + nw +
                        innerTableTextMargin * 2 + bw +
                        innerTableTextMargin * 2 + mw +
                        innerTableTextMargin * 2 + sw +
                        innerTableTextMargin * 2 + lw +
                        innerTableTextMargin * 2 + rw +
                        innerTableTextMargin * 2 + kw +
                        innerTableTextMargin * 2 + bow;
        result[STAGE_HEIGHT] = innerTableCellMargin * (st.data.datas.length + 1);

        return result;
    }

    private static void drawRewardTable(FG2D g, int x, int y, Stage st, CustomStageMap map, int[] dimension, int desiredGap, int lang, boolean reward) throws Exception {
        List<String[]> data;

        if(reward)
            data = DataToString.getRewards(st, map, lang);
        else
            data = DataToString.getScoreDrops(st, map, lang);

        if(data != null) {
            int w = desiredGap * 7 + dimension[CHANCE_WIDTH] + dimension[REWARD_WIDTH] + dimension[AMOUNT_WIDTH] + rewardIconSize;
            int h = dimension[TOTAL_HEIGHT];

            g.setRenderingHint(3, 1);
            g.enableAntialiasing();

            g.setFont(contentFont);

            g.setColor(51, 53, 60);

            g.fillRoundRect(x, y, w, h, innerTableCornerRadius, innerTableCornerRadius);

            g.setColor(24, 25, 28);

            g.fillRoundRect(x, y, w,innerTableCellMargin + innerTableCornerRadius, innerTableCornerRadius, innerTableCornerRadius);

            g.setColor(51, 53, 60);

            g.fillRect(x, y + innerTableCellMargin, w, innerTableCornerRadius);

            int x1 = x;

            for(int i = 0; i < 3; i++) {
                double tx = desiredGap * 2 + dimension[i];

                if(i == 1)
                    tx += desiredGap + rewardIconSize;

                tx /= 2.0;

                switch (i) {
                    case CHANCE_WIDTH -> {
                        String chance;

                        if (data.get(data.size() - 1).length == 1) {
                            data.remove(data.size() - 1);

                            chance = LangID.getStringByID("data_rewardno", lang);
                        } else {
                            chance = LangID.getStringByID("data_chance", lang);
                        }

                        g.setColor(191, 191, 191);

                        g.drawCenteredText(chance, x1 + (int) tx, y + innerTableCellMargin / 2);

                        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                        int chaneLineY = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

                        g.drawLine((int) (x1 + tx * 2.0), y + chaneLineY, (int) (x1 + tx * 2.0), y + innerTableCellMargin - chaneLineY);

                        int chanceY = y + innerTableCellMargin;

                        for (int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawCenteredText(data.get(j)[i], x1 + (int) tx, chanceY + innerTableCellMargin / 2);

                            g.setColor(191, 191, 191, 64);

                            g.drawLine((int) (x1 + tx * 2.0), chanceY + chaneLineY, (int) (x1 + tx * 2.0), chanceY + innerTableCellMargin - chaneLineY);

                            chanceY += innerTableCellMargin;
                        }
                    }
                    case REWARD_WIDTH -> {
                        g.setColor(191, 191, 191);

                        g.drawCenteredText(LangID.getStringByID("data_reward", lang), x1 + (int) tx, y + innerTableCellMargin / 2);

                        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                        int rewardLineY = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

                        g.drawLine((int) (x1 + tx * 2.0), y + rewardLineY, (int) (x1 + tx * 2.0), y + innerTableCellMargin - rewardLineY);

                        int rewardY = y + innerTableCellMargin;

                        int rx = (int) Math.round((tx * 2.0 - desiredGap - rewardIconSize) / 2.0);

                        for (int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawCenteredText(data.get(j)[i], x1 + desiredGap + rewardIconSize + rx, rewardY + innerTableCellMargin / 2);

                            g.setColor(65, 69, 76);

                            g.fillOval(x1 + desiredGap, rewardY + (innerTableCellMargin - rewardIconSize) / 2, rewardIconSize, rewardIconSize);

                            BufferedImage icon;

                            if (reward) {
                                icon = getRewardImage(((DefStageInfo) st.info).drop[j][i], map);
                            } else {
                                icon = getRewardImage(((DefStageInfo) st.info).time[j][i], map);
                            }

                            if (icon != null) {
                                g.drawImage(icon, x1 + desiredGap, rewardY + (innerTableCellMargin - rewardIconSize) / 2.0, rewardIconSize, rewardIconSize);
                            }

                            g.setColor(191, 191, 191, 64);

                            g.drawLine((int) (x1 + tx * 2.0), rewardY + rewardLineY, (int) (x1 + tx * 2.0), rewardY + innerTableCellMargin - rewardLineY);

                            rewardY += innerTableCellMargin;
                        }
                    }
                    case AMOUNT_WIDTH -> {
                        g.setColor(191, 191, 191);
                        g.drawCenteredText(LangID.getStringByID("data_amount", lang), x1 + (int) tx, y + innerTableCellMargin / 2);
                        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                        int amountY = y + innerTableCellMargin;
                        for (int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawCenteredText(data.get(j)[i], x1 + (int) tx, amountY + innerTableCellMargin / 2);

                            g.setColor(191, 191, 191, 64);

                            amountY += innerTableCellMargin;
                        }
                    }
                }

                x1 += (int) (tx * 2.0);
            }

            g.setColor(191, 191, 191, 64);

            g.setStroke(innerTableLineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            int y1 = y + innerTableCellMargin * 2;

            for(int i = 0; i < data.size() - 1; i++) {
                g.drawLine(x + innerTableTextMargin, y1, w - innerTableTextMargin, y1);

                y1 += innerTableCellMargin;
            }
        }
    }

    private static void drawEnemySchemeTable(FG2D g, int y, Stage st, CustomStageMap map, int[] dimension, int desiredGap, boolean isRanking, boolean isFrame, int lv, int lang) throws Exception {
        int w = desiredGap * 19 + rewardIconSize;

        for(int i = ENEMY; i <= BOSS; i++) {
            w += dimension[i];
        }

        int h = innerTableCellMargin * (st.data.datas.length + 1);

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setFont(contentFont);

        g.setColor(65, 69, 76);

        g.fillRoundRect(bgMargin, y, w, h, cornerRadius, cornerRadius);

        g.setColor(24, 25, 28);

        g.fillRoundRect(bgMargin, y, w, innerTableCellMargin + cornerRadius / 2, cornerRadius, cornerRadius);

        g.setColor(65, 69, 76);

        g.fillRect(bgMargin, y + innerTableCellMargin, w, cornerRadius / 2);

        String[] headerText = {
                LangID.getStringByID("data_enemy", lang),
                LangID.getStringByID("data_number", lang),
                LangID.getStringByID(isRanking ? "data_basedealt" : "data_basehealth", lang),
                LangID.getStringByID("data_manif", lang),
                LangID.getStringByID("data_startres", lang),
                LangID.getStringByID("data_layer", lang),
                LangID.getStringByID("data_respect", lang),
                LangID.getStringByID("data_killcount", lang),
                LangID.getStringByID("data_isboss", lang)
        };

        int x1 = bgMargin;

        g.setColor(191, 191, 191);

        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = ENEMY; i <= BOSS; i++) {
            double tx = desiredGap * 2 + dimension[i];

            if(i == ENEMY)
                tx += desiredGap + rewardIconSize;

            tx /= 2.0;

            int ly = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

            g.drawCenteredText(headerText[i], (int) Math.round(x1 + tx),  y + innerTableCellMargin / 2);

            if(i < BOSS)
                g.drawLine((int) (x1 + tx * 2.0),  y + ly, (int) (x1 + tx * 2.0),  y + innerTableCellMargin - ly);

            x1 += (int) (tx * 2.0);
        }

        int y1 = y + innerTableCellMargin;

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            x1 = bgMargin;

            SCDef.Line line = st.data.datas[i];

            for(int j = ENEMY; j <= BOSS; j++) {
                double tx = desiredGap * 2 + dimension[j];

                if(j == ENEMY)
                    tx += desiredGap + rewardIconSize;

                tx /= 2.0;

                String content = "";

                int rx = (int) Math.round((tx * 2.0 - desiredGap - rewardIconSize) / 2.0);
                int ly = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

                switch (j) {
                    case ENEMY -> {
                        Identifier<AbEnemy> id = line.enemy;

                        content = null;

                        if (id.id >= UserProfile.getBCData().enemies.size()) {
                            content = map.enemyNames.get(id.id);
                        } else {
                            AbEnemy enemy = id.get();

                            if (enemy instanceof Enemy) {
                                content = MultiLangCont.get(enemy, lang);

                                if (content == null || content.isBlank()) {
                                    content = ((Enemy) enemy).names.toString();
                                }

                                if (content.isBlank()) {
                                    content = map.enemyNames.get(id.id);
                                }
                            }
                        }

                        if (content == null || content.isBlank()) {
                            content = LangID.getStringByID("data_enemy", lang) + " - " + Data.trio(id.id);
                        }
                    }
                    case NUMBER -> {
                        if (line.number == 0)
                            content = LangID.getStringByID("data_infinite", lang);
                        else
                            content = String.valueOf(line.number);
                    }
                    case BASE -> {
                        String suffix = isRanking ? "" : "%";

                        if (line.castle_0 == line.castle_1 || line.castle_1 == 0)
                            content = line.castle_0 + suffix;
                        else {
                            int minHealth = Math.min(line.castle_0, line.castle_1);
                            int maxHealth = Math.max(line.castle_0, line.castle_1);

                            content = minHealth + " ~ " + maxHealth + suffix;
                        }
                    }
                    case MAGNIFICATION ->
                            content = DataToString.getMagnification(new int[]{line.multiple, line.mult_atk}, map.stars[lv]);
                    case START -> {
                        String start;

                        if (line.spawn_1 == 0)
                            if (isFrame)
                                start = Math.abs(line.spawn_0) + "f";
                            else
                                start = DataToString.df.format(Math.abs(line.spawn_0) / 30.0) + "s";
                        else {
                            int minSpawn = Math.abs(Math.min(line.spawn_0, line.spawn_1));
                            int maxSpawn = Math.abs(Math.max(line.spawn_0, line.spawn_1));

                            if (isFrame)
                                start = minSpawn + "f ~ " + maxSpawn + "f";
                            else
                                start = DataToString.df.format(minSpawn / 30.0) + "s ~ " + DataToString.df.format(maxSpawn / 30.0) + "s";
                        }

                        String respawn;

                        if (line.respawn_0 == line.respawn_1)
                            if (isFrame)
                                respawn = line.respawn_0 + "f";
                            else
                                respawn = DataToString.df.format(line.respawn_0 / 30.0) + "s";
                        else {
                            int minSpawn = Math.min(line.respawn_0, line.respawn_1);
                            int maxSpawn = Math.max(line.respawn_0, line.respawn_1);

                            if (isFrame)
                                respawn = minSpawn + "f ~ " + maxSpawn + "f";
                            else
                                respawn = DataToString.df.format(minSpawn / 30.0) + "s ~ " + DataToString.df.format(maxSpawn / 30.0) + "s";
                        }

                        content = start + " (" + respawn + ")";
                    }
                    case LAYER -> {
                        if (line.layer_0 != line.layer_1) {
                            int minLayer = Math.min(line.layer_0, line.layer_1);
                            int maxLayer = Math.max(line.layer_0, line.layer_1);

                            content = minLayer + " ~ " + maxLayer;
                        } else {
                            content = String.valueOf(line.layer_0);
                        }
                    }
                    case RESPECT ->
                            content = (line.spawn_0 < 0 || line.spawn_1 < 0) ? LangID.getStringByID("data_true", lang) : "";
                    case KILL -> content = String.valueOf(line.kill_count);
                    case BOSS -> {
                        if (line.boss == 0)
                            content = "";
                        else
                            content = LangID.getStringByID("data_boss", lang);
                    }
                }

                if(j == ENEMY) {
                    g.setColor(51, 53, 60);

                    g.fillOval(bgMargin + desiredGap, y1 + (innerTableCellMargin - rewardIconSize) / 2, rewardIconSize, rewardIconSize);

                    BufferedImage icon = getEnemyIcon(line.enemy.id, map);

                    if(icon != null) {
                        g.drawImage(icon, bgMargin + desiredGap + 30, y1 + (innerTableCellMargin - rewardIconSize) / 2.0 + 30, 100, 100);
                    }

                    g.setColor(239, 239, 239);

                    g.drawCenteredText(content, bgMargin + desiredGap + rewardIconSize + rx, y1 + innerTableCellMargin / 2);
                } else {
                    g.setColor(239, 239, 239);

                    g.drawCenteredText(content, (int) (x1 + tx), y1 + innerTableCellMargin / 2);
                }

                g.setColor(191, 191, 191, 64);

                if(j < BOSS) {
                    g.drawLine((int) (x1 + tx * 2.0), y1 + ly, (int) (x1 + tx * 2.0), y1 + innerTableCellMargin - ly);
                }

                x1 += (int) (tx * 2.0);
            }

            y1 += innerTableCellMargin;
        }

        y1 = y + innerTableCellMargin * 2;

        g.setStroke(innerTableLineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 0; i < st.data.datas.length - 1; i++) {
            g.drawLine(bgMargin + innerTableTextMargin, y1, w - innerTableTextMargin, y1);

            y1 += innerTableCellMargin;
        }
    }

    public static AnimU.UType getAnimType(int mode, int max) {
        switch (mode) {
            case 1 -> {
                return AnimU.UType.IDLE;
            }
            case 2 -> {
                return AnimU.UType.ATK;
            }
            case 3 -> {
                return AnimU.UType.HB;
            }
            case 4 -> {
                if (max == 5)
                    return AnimU.UType.ENTER;
                else
                    return AnimU.UType.BURROW_DOWN;
            }
            case 5 -> {
                return AnimU.UType.BURROW_MOVE;
            }
            case 6 -> {
                return AnimU.UType.BURROW_UP;
            }
            default -> {
                return AnimU.UType.WALK;
            }
        }
    }

    private static int maxAmong(int... values) {
        if(values.length == 1)
            return values[0];
        else if(values.length == 2) {
            return Math.max(values[0], values[1]);
        } else if(values.length >= 3) {
            int val = Math.max(values[0], values[1]);

            for(int i = 2; i < values.length; i++) {
                val = Math.max(values[i], val);
            }

            return val;
        } else {
            return  0;
        }
    }

    private static int minAmong(int... values) {
        if(values.length == 1)
            return values[0];
        else if(values.length == 2) {
            return Math.min(values[0], values[1]);
        } else if(values.length >= 3) {
            int val = Math.min(values[0], values[1]);

            for(int i = 2; i < values.length; i++) {
                val = Math.min(values[i], val);
            }

            return val;
        } else {
            return  0;
        }
    }

    private static String quad(int n) {
        if(n < 10) {
            return "000"+n;
        } else if(n < 100) {
            return "00"+n;
        } else if(n < 1000) {
            return "0"+n;
        } else {
            return String.valueOf(n);
        }
    }

    private static String getProgressBar(int prog, int time) {
        int ratio = (int) (prog * 40.0 / time);

        return "▓".repeat(Math.max(0, ratio)) +
                "░".repeat(Math.max(0, 40 - ratio));
    }

    private static String getETA(long start, long current, int prog, int time) {
        double unit = (current - start) / 1000.0 / prog;

        return DataToString.df.format(unit * (time - prog));
    }

    private static int handleMixedBGEffect(int ind) {
        for(int i = 0; i < BackgroundEffect.jsonList.size(); i++) {
            if(BackgroundEffect.jsonList.get(i) == -ind)
                return 10 + i;
        }

        return -1;
    }

    private static String convertValue(int value) {
        String[] prefix = { "", "k", "M" };

        int i = 0;

        while(true) {
            if(i == 2 || value < 1000)
                return value + prefix[i];
            else {
                value /= 1000;
                i++;
            }
        }
    }

    private static BufferedImage getRewardImage(int id, CustomStageMap map) throws Exception {
        if(id < 1000) {
            File icon = map.rewardIcons.get(id);

            if(icon != null) {
                return ImageIO.read(icon);
            }

            if(id >= 11 && id <= 13)
                id += 9;

            String name = "gatyaitemD_" + Data.duo(id) + "_f.png";

            VFile vf = VFile.get("./org/page/items/"+name);

            if(vf != null) {
                return (BufferedImage) vf.getData().getImg().bimg();
            }
        } else if(id < 30000) {
            File icon;

            if(id < 10000)
                icon = map.unitIcons.get(id);
            else
                icon = map.trueFormIcons.get(id);

            if(icon != null) {
                return ImageIO.read(icon);
            }

            String name = map.rewardToUnitIcon.get(id);

            if(name != null) {
                int uid = CommonStatic.parseIntN(name);

                String path;

                if(name.endsWith("_m.png")) {
                    path = "./org/img/m/" + Data.trio(uid) + "/" + name;
                } else {
                    path = "./org/unit/" + Data.trio(uid) + "/" + name;
                }

                VFile vf = VFile.get(path);

                if(vf != null) {
                    return (BufferedImage) vf.getData().getImg().bimg();
                }
            }
        }

        return null;
    }

    private static BufferedImage getEnemyIcon(int eid, CustomStageMap map) throws Exception {
        File icon = map.enemyIcons.get(eid);

        if(icon != null) {
            return ImageIO.read(icon);
        } else {
            VFile vf = VFile.get("./org/enemy/" + Data.trio(eid) + "/enemy_icon_" + Data.trio(eid) + ".png");

            if(vf != null) {
                return (BufferedImage) vf.getData().getImg().bimg();
            }
        }

        return null;
    }
}