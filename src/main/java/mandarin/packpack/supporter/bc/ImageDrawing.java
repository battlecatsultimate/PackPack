package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
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
            3000 //1011
    };

    private static Font titleFont;
    private static Font typeFont;
    private static Font nameFont;
    private static Font contentFont;
    private static Font levelFont;
    private static Font fruitFont;

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

    private static final float headerStroke = 4f;
    private static final float innerTableLineStroke = 3f;

    private static final int fruitGap = 60;
    private static final double fruitRatio = 0.125;
    private static final double fruitTextGapRatio = 0.025;
    private static final double fruitUpperGapRatio = 0.025;
    private static final double fruitDownerGapRatio = 0.05;
    private static final double enemyIconRatio = 1.25; // w/h
    private static final double enemyInnerIconRatio = 0.95;

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
    private static final int BOSS = 6;
    private static final int STAGE_WIDTH = 7;
    private static final int STAGE_HEIGHT = 8;

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

        String cont = LangID.getStringByID("bg_dimen", lang).replace("_WWW_", w+"").replace("_HHH_", bgAnimHeight+"") +"\n\n"+
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
                String ind = ""+ i;
                String content = LangID.getStringByID("bg_dimen", lang).replace("_WWW_", finalW+"").replace("_HHH_", bgAnimHeight+"") +"\n\n"+
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

        String content = LangID.getStringByID("bg_dimen", lang).replace("_WWW_", finalW+"").replace("_HHH_", bgAnimHeight+"") +"\n\n"+
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
            rect.width = 1;

        if(rect.height == 0)
            rect.height = 1;

        BufferedImage result = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
        FG2D rg = new FG2D(result.getGraphics());

        rg.setRenderingHint(3, 1);
        rg.enableAntialiasing();

        if(!transparent) {
            rg.setColor(54,57,63,255);
            rg.fillRect(0, 0, rect.width, rect.height);
        }

        anim.draw(rg, new P(-rect.x, -rect.y), siz);

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
                + LangID.getStringByID("gif_result", lang).replace("_WWW_", ""+rect.width)
                .replace("_HHH_", rect.height+"").replace("_XXX_", rect.x+"")
                .replace("_YYY_", rect.x+"");

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

        finCont += LangID.getStringByID("gif_final", lang).replace("_WWW_", ""+rect.width)
                .replace("_HHH_", rect.height+"").replace("_XXX_", rect.x+"")
                .replace("_YYY_", rect.x+"")+"\n";

        msg.editMessage(finCont).queue();

        P pos = new P(-rect.x, -rect.y);

        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();

        for(int i = 0; i < frame; i++) {
            if(System.currentTimeMillis() - current >= 1500) {
                String content = finCont +"\n\n";

                String prog = DataToString.df.format(i * 100.0 / frame);
                String eta = getETA(start, System.currentTimeMillis(), i, frame);
                String ind = ""+ i;
                String len = frame +"";

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
                        .replace("_PPP_", frame + "")
                        .replace("_LLL_", frame + "")
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
                + LangID.getStringByID("gif_result", lang).replace("_WWW_", ""+rect.width)
                .replace("_HHH_", rect.height+"").replace("_XXX_", rect.x+"")
                .replace("_YYY_", rect.x+"")+"\n";

        if(ratio != 1.0) {
            cont += LangID.getStringByID("gif_adjust", lang).replace("_", DataToString.df.format(ratio * 100.0))+"\n";
        } else {
            cont += LangID.getStringByID("gif_cango", lang)+"\n";
        }

        cont += LangID.getStringByID("gif_final", lang).replace("_WWW_", (int) (ratio * rect.width) + "")
                .replace("_HHH_", (int) (ratio* rect.height)+"").replace("_XXX_", (int) (ratio * rect.x)+"")
                .replace("_YYY_", (int) (ratio * rect.y)+"");

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
                String ind = ""+ i;
                String len = frame + "";

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
                        .replace("_PPP_", frame + "")
                        .replace("_LLL_", frame + "")
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
                + LangID.getStringByID("gif_result", lang).replace("_WWW_", ""+rect.width)
                .replace("_HHH_", ""+rect.height).replace("_XXX_", rect.x+"")
                .replace("_YYY_", ""+rect.y);

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

        finCont += LangID.getStringByID("gif_final", lang).replace("_WWW_", ""+rect.width)
                .replace("_HHH_", rect.height+"").replace("_XXX_", rect.x+"")
                .replace("_YYY_", rect.x+"")+"\n";

        msg.editMessage(finCont).queue();

        P pos = new P(-rect.x, -rect.y);

        long current = System.currentTimeMillis();

        int totalFrame = 0;
        int progress = 0;

        for(int i = 0; i < mixer.anim.length; i++) {
            EAnimD<?> anim = mixer.getAnim(i);

            if(anim != null) {
                switch (i) {
                    case 0:
                    case 1:
                        totalFrame += Math.max(60, Math.min(150, anim.len()));
                        break;
                    case 2:
                        totalFrame += Math.max(60, anim.len());
                        break;
                    case 3:
                    case 5:
                        totalFrame += 60;
                        break;
                    case 4:
                    case 6:
                        totalFrame += anim.len();
                        break;
                }
            }
        }

        long start = System.currentTimeMillis();

        for(int i = 0; i < mixer.anim.length; i++) {
            //60 ~ 150, 60 ~ 150, 60 ~, 60, one cycle, 60, one cycle

            EAnimD<?> anim = mixer.getAnim(i);

            if(anim != null) {
                int frame;

                switch (i) {
                    case 0:
                    case 1:
                        frame = Math.max(60, Math.min(150, anim.len()));
                        break;
                    case 2:
                        frame = Math.max(60, anim.len());
                        break;
                    case 3:
                    case 5:
                        frame = 60;
                        break;
                    case 4:
                    case 6:
                        frame = anim.len();
                        break;
                    default:
                        frame = 0;
                }

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
                                                .replace("_PPP_", progress + "")
                                                .replace("_LLL_", totalFrame + "")
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
                                            .replace("_PPP_", progress + "")
                                            .replace("_LLL_", totalFrame + "")
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
                        .replace("_PPP_", totalFrame + "")
                        .replace("_LLL_", totalFrame + "")
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
                        .replace("_PPP_", totalFrame + "")
                        .replace("_LLL_", totalFrame + "")
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
        rg.fillRoundRect(0, -cornerRadius, finW * units.length, cornerRadius + bgMargin * 6 + titleH, cornerRadius, cornerRadius);

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
        rg.fillRoundRect(0, -cornerRadius, finW, cornerRadius + bgMargin * 6 + titleH, cornerRadius, cornerRadius);

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

        BufferedImage title = getStageTitleImage(name, code, tfm, cfm);

        if(title.getWidth() > uw * 4 + CellDrawer.lineOffset * 6) {
            uw = (title.getWidth() - CellDrawer.lineOffset * 6) / 4;
        }

        int[] stw = measureEnemySchemeWidth(st, map, cfm, isFrame, lv, lang);
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
            int tw = maxAmong(dw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2, sw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2, stw[STAGE_WIDTH]);

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

                    desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 15.0);
                }
            } else {
                desiredRewardGap = (int) Math.round((uw * 2 + CellDrawer.lineOffset * 2 - dw[CHANCE_WIDTH] - dw[REWARD_WIDTH] - dw[AMOUNT_WIDTH] - rewardIconSize) / 7.0);
                desiredScoreGap = (int) Math.round((uw * 2 + CellDrawer.lineOffset * 2 - sw[CHANCE_WIDTH] - sw[REWARD_WIDTH] - sw[AMOUNT_WIDTH] - rewardIconSize) / 7.0);

                int tempTotalWidth = uw * 4 + CellDrawer.lineOffset * 6;

                for(int i = ENEMY; i <= BOSS; i++) {
                    tempTotalWidth -= stw[i];
                }

                desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 15.0);
            }
        } else {
            if(stw[STAGE_WIDTH] > uw * 4 + CellDrawer.lineOffset * 6 + statPanelMargin * 2) {
                uw = (int) Math.round((stw[STAGE_WIDTH] - CellDrawer.lineOffset * 6 - statPanelMargin * 2) / 4.0);
            } else {
                int tempTotalWidth = uw * 4 + CellDrawer.lineOffset * 6 + statPanelMargin * 2;

                for(int i = ENEMY; i <= BOSS; i++) {
                    tempTotalWidth -= stw[i];
                }

                desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 15.0);
            }
        }

        BufferedImage enemySchematic = drawEnemySchemeTable(st, map, stw, desiredStageGap, isFrame, lv, lang);

        BufferedImage rewardTable = drawRewardTable(st, map, dw, desiredRewardGap, lang, true);
        BufferedImage scoreTable = drawRewardTable(st, map, sw, desiredScoreGap, lang, false);

        int w = uw * 4 + CellDrawer.lineOffset * 6;
        int h = 0;

        for(int i = 0; i < group.size(); i++) {
            if(i < group.size() - 2) {
                h += uh;
                h += cellMargin;
            } else if(i < group.size() - 1) {
                h += ah + cellMargin * 2;
            } else {
                h += mh + cellMargin * 2;
            }
        }

        BufferedImage infoPanel = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        FG2D pg = new FG2D(infoPanel.getGraphics());

        int x = 0;
        int y = 0;

        for(int i = 0; i < group.size(); i++) {
            group.get(i).draw(pg, x, y, uw, offset, uh, nameFont, contentFont);

            if(i < group.size() - 1)
                y += uh + cellMargin;
            else
                y += ah + cellMargin;

            if(i == group.size() - 3 || i == group.size() - 2)
                y += cellMargin;
        }

        int finW = infoPanel.getWidth() + statPanelMargin * 2 + bgMargin * 2;
        int finH = bgMargin * 6 + title.getHeight() + statPanelMargin * 2 + infoPanel.getHeight() + cellMargin * 2 + uh - CellDrawer.textMargin - ch;
        int panelH = statPanelMargin * 2 + infoPanel.getHeight() + cellMargin * 2 + uh - CellDrawer.textMargin - ch;

        if(rewardTable != null || scoreTable != null) {
            int tableH = 0;

            if(rewardTable != null)
                tableH = Math.max(rewardTable.getHeight(), tableH);

            if(scoreTable != null)
                tableH = Math.max(scoreTable.getHeight(), tableH);

            finH += tableH;
            panelH += tableH;
        } else {
            finH += ch;
            panelH += ch;
        }

        finH += enemySchematic.getHeight();

        BufferedImage result = new BufferedImage(finW, finH, BufferedImage.TYPE_INT_ARGB);

        FG2D g = new FG2D(result.getGraphics());

        g.setColor(50, 53, 59);

        g.fillRect(0, 0, finW, finH);

        g.setColor(24, 25, 28);

        g.fillRoundRect(0, -cornerRadius, finW, cornerRadius + bgMargin * 10 + title.getHeight(), cornerRadius, cornerRadius);

        g.setColor(64, 68, 75);

        g.fillRoundRect(bgMargin, bgMargin * 4 + title.getHeight(), infoPanel.getWidth() + statPanelMargin * 2, panelH, cornerRadius, cornerRadius);

        g.drawImage(title, bgMargin, bgMargin * 2);
        g.drawImage(infoPanel, bgMargin + statPanelMargin, bgMargin * 4 + title.getHeight() + statPanelMargin);

        g.setColor(191, 191, 191);

        g.setFont(nameFont);

        g.drawText(LangID.getStringByID("data_rewarddrop", lang), bgMargin + statPanelMargin, bgMargin * 4 + title.getHeight() + statPanelMargin + infoPanel.getHeight() + cellMargin + offset / 2);
        g.drawText(LangID.getStringByID("data_scoredrop", lang), bgMargin + statPanelMargin + uw * 2 + CellDrawer.lineOffset * 4, bgMargin * 4 + title.getHeight() + statPanelMargin + infoPanel.getHeight() + cellMargin + offset / 2);

        if(rewardTable != null) {
            g.drawImage(rewardTable, bgMargin + statPanelMargin, bgMargin * 4 + title.getHeight() + statPanelMargin + infoPanel.getHeight() + cellMargin * 2 + uh - ch - CellDrawer.textMargin);
        } else {
            g.setFont(contentFont);
            g.setColor(239, 239, 239);

            g.drawText(LangID.getStringByID("data_none", lang), bgMargin + statPanelMargin, bgMargin * 4 + title.getHeight() + statPanelMargin + infoPanel.getHeight() + cellMargin * 2 + uh - ch - CellDrawer.textMargin + offset / 2);
        }

        if(scoreTable != null) {
            g.drawImage(scoreTable, bgMargin + statPanelMargin + uw * 2 + CellDrawer.lineOffset * 4, bgMargin * 4 + title.getHeight() + statPanelMargin + infoPanel.getHeight() + cellMargin * 2 + uh - ch - CellDrawer.textMargin);
        } else {
            g.setFont(contentFont);
            g.setColor(239, 239, 239);

            g.drawText(LangID.getStringByID("data_none", lang), bgMargin + statPanelMargin + uw * 2 + CellDrawer.lineOffset * 4, bgMargin * 4 + title.getHeight() + statPanelMargin + infoPanel.getHeight() + cellMargin * 2 + uh - ch - CellDrawer.textMargin + offset / 2);
        }

        g.drawImage(enemySchematic, bgMargin, bgMargin * 4 + title.getHeight() + panelH + bgMargin);

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

    private static String getUnitCode(int ind) {
        switch (ind) {
            case 0:
                return "f";
            case 1:
                return "c";
            case 2:
                return "s";
            default:
                return ""+ind;
        }
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

        g.fillRoundRect(icw + nameMargin, (int) (nRect.getHeight() + nameMargin), (int) (typeLeftRightMargin * 2 + tRect.getWidth()), (int) (typeUpDownMargin * 2 + tRect.getHeight()), 36, 36);

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

    private static BufferedImage getStageTitleImage(String name, String code, FontMetrics bfm, FontMetrics lfm) {
        FontRenderContext bfrc = bfm.getFontRenderContext();
        FontRenderContext lfrc = lfm.getFontRenderContext();

        Rectangle2D nRect = titleFont.createGlyphVector(bfrc, name).getPixelBounds(null, 0, 0);
        Rectangle2D lRect = contentFont.createGlyphVector(lfrc, code).getPixelBounds(null, 0, 0);

        int h = (int) Math.round(nRect.getHeight() + nameMargin + lRect.getHeight());

        int w = (int) Math.max(nRect.getWidth(), lRect.getWidth()) + bgMargin;

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setColor(238, 238, 238, 255);
        g.setFont(titleFont);

        g.drawText(name, (int) (bgMargin - nRect.getX()), (int) -nRect.getY());

        g.setColor(191, 191, 191);
        g.setFont(contentFont);

        g.drawText(code, (int) (bgMargin - lRect.getX()), (int) (lRect.getHeight() + nameMargin - lRect.getY()));

        return result;
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
    private static int[] measureEnemySchemeWidth(Stage st, CustomStageMap map, FontMetrics cfm, boolean isFrame, int lv, int lang) {
        int[] result = new int[9];

        int ew = cfm.stringWidth(LangID.getStringByID("data_enemy", lang));
        int nw = cfm.stringWidth(LangID.getStringByID("data_number", lang));
        int bw = cfm.stringWidth(LangID.getStringByID("data_basehealth", lang));
        int mw = cfm.stringWidth(LangID.getStringByID("data_manif", lang));
        int sw = cfm.stringWidth(LangID.getStringByID("data_startres", lang));
        int lw = cfm.stringWidth(LangID.getStringByID("data_layer", lang));
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
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    enemyName = MultiLangCont.get(enemy);

                    CommonStatic.getConfig().lang = oldConfig;

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

            if(line.castle_0 == line.castle_1 || line.castle_1 == 0)
                baseHP = line.castle_0+"%";
            else {
                int minHealth = Math.min(line.castle_0, line.castle_1);
                int maxHealth = Math.max(line.castle_0, line.castle_1);

                baseHP = minHealth + " ~ " + maxHealth + "%";
            }

            bw = Math.max(bw, cfm.stringWidth(baseHP));

            mw = Math.max(mw, cfm.stringWidth(DataToString.getMagnification(new int[] {line.multiple, line.mult_atk}, lv)));

            String start;

            if(line.spawn_1 == 0)
                if(isFrame)
                    start = line.spawn_0+"f";
                else
                    start = DataToString.df.format(line.spawn_0/30.0)+"s";
            else {
                int minSpawn = Math.min(line.spawn_0, line.spawn_1);
                int maxSpawn = Math.max(line.spawn_0, line.spawn_1);

                if(isFrame)
                    start = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    start = DataToString.df.format(minSpawn/30.0)+"s ~ "+DataToString.df.format(maxSpawn/30.0)+"s";
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

            String boss;

            if(line.boss == 0)
                boss = "";
            else
                boss = LangID.getStringByID("data_boss", lang);

            bow = Math.max(bow, cfm.stringWidth(boss));
        }

        result[ENEMY] = ew;
        result[NUMBER] = nw;
        result[BASE] = bw;
        result[MAGNIFICATION] = mw;
        result[START] = sw;
        result[LAYER] = lw;
        result[BOSS] = bow;
        result[STAGE_WIDTH] =
                innerTableTextMargin * 3 + rewardIconSize + ew +
                        innerTableTextMargin * 2 + nw +
                        innerTableTextMargin * 2 + bw +
                        innerTableTextMargin * 2 + mw +
                        innerTableTextMargin * 2 + sw +
                        innerTableTextMargin * 2 + lw +
                        innerTableTextMargin * 2 + bow;
        result[STAGE_HEIGHT] = innerTableCellMargin * (st.data.datas.length + 1);

        return result;
    }

    private static BufferedImage drawRewardTable(Stage st, CustomStageMap map, int[] dimension, int desiredGap, int lang, boolean reward) throws Exception {
        List<String[]> data;

        if(reward)
            data = DataToString.getRewards(st, map, lang);
        else
            data = DataToString.getScoreDrops(st, map, lang);

        if(data == null) {
            return null;
        } else {
            int w = desiredGap * 7 + dimension[CHANCE_WIDTH] + dimension[REWARD_WIDTH] + dimension[AMOUNT_WIDTH] + rewardIconSize;

            int h = dimension[TOTAL_HEIGHT];

            BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            FG2D g = new FG2D(result.getGraphics());

            g.setRenderingHint(3, 1);
            g.enableAntialiasing();

            g.setFont(contentFont);

            g.setColor(51, 53, 60);

            g.fillRoundRect(0, 0, w, h, innerTableCornerRadius, innerTableCornerRadius);

            g.setColor(24, 25, 28);

            g.fillRoundRect(0, 0, w,innerTableCellMargin + innerTableCornerRadius, innerTableCornerRadius, innerTableCornerRadius);

            g.setColor(51, 53, 60);

            g.fillRect(0, innerTableCellMargin, w, innerTableCornerRadius);

            int x = 0;

            for(int i = 0; i < 3; i++) {
                double tx = desiredGap * 2 + dimension[i];

                if(i == 1)
                    tx += desiredGap + rewardIconSize;

                tx /= 2.0;

                switch (i) {
                    case CHANCE_WIDTH:
                        String chance;

                        if(data.get(data.size() - 1).length == 1) {
                            data.remove(data.size() - 1);

                            chance = LangID.getStringByID("data_rewardno", lang);
                        } else {
                            chance = LangID.getStringByID("data_chance", lang);
                        }

                        g.setColor(191, 191, 191);

                        g.drawCenteredText(chance, x + (int) tx, innerTableCellMargin / 2);

                        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                        int ly = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

                        g.drawLine((int) (x + tx * 2.0), ly, (int) (x + tx * 2.0), innerTableCellMargin - ly);

                        int y = innerTableCellMargin;

                        for(int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawCenteredText(data.get(j)[i], x + (int) tx, y + innerTableCellMargin / 2);

                            g.setColor(191, 191, 191, 64);

                            g.drawLine((int) (x + tx * 2.0), y + ly, (int) (x + tx * 2.0), y + innerTableCellMargin - ly);

                            y += innerTableCellMargin;
                        }

                        break;
                    case REWARD_WIDTH:
                        g.setColor(191, 191, 191);

                        g.drawCenteredText(LangID.getStringByID("data_reward", lang), x + (int) tx, innerTableCellMargin / 2);

                        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                        ly = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

                        g.drawLine((int) (x + tx * 2.0), ly, (int) (x + tx * 2.0), innerTableCellMargin - ly);

                        y = innerTableCellMargin;

                        int rx = (int) Math.round((tx * 2.0 - desiredGap - rewardIconSize) / 2.0);

                        for(int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawCenteredText(data.get(j)[i], x + desiredGap + rewardIconSize + rx, y + innerTableCellMargin / 2);

                            g.setColor(65, 69, 76);

                            g.fillOval(x + desiredGap, y + (innerTableCellMargin - rewardIconSize) / 2, rewardIconSize, rewardIconSize);

                            BufferedImage icon;

                            if(reward) {
                                icon = getRewardImage(((DefStageInfo) st.info).drop[j][i], map);
                            } else {
                                icon = getRewardImage(((DefStageInfo) st.info).time[j][i], map);
                            }

                            if(icon != null) {
                                g.drawImage(icon, x + desiredGap, y + (innerTableCellMargin - rewardIconSize) / 2.0, rewardIconSize, rewardIconSize);
                            }

                            g.setColor(191, 191, 191, 64);

                            g.drawLine((int) (x + tx * 2.0), y + ly, (int) (x + tx * 2.0), y + innerTableCellMargin - ly);

                            y += innerTableCellMargin;
                        }

                        break;
                    case AMOUNT_WIDTH:
                        g.setColor(191, 191, 191);

                        g.drawCenteredText(LangID.getStringByID("data_amount", lang), x + (int) tx, innerTableCellMargin / 2);

                        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                        y = innerTableCellMargin;

                        for(int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawCenteredText(data.get(j)[i], x + (int) tx, y + innerTableCellMargin / 2);

                            g.setColor(191, 191, 191, 64);

                            y += innerTableCellMargin;
                        }

                        break;
                }

                x += (int) (tx * 2.0);
            }

            g.setColor(191, 191, 191, 64);

            g.setStroke(innerTableLineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            int y = innerTableCellMargin * 2;

            for(int i = 0; i < data.size() - 1; i++) {
                g.drawLine(innerTableTextMargin, y, w - innerTableTextMargin, y);

                y += innerTableCellMargin;
            }

            return result;
        }
    }

    private static BufferedImage drawEnemySchemeTable(Stage st, CustomStageMap map, int[] dimension, int desiredGap, boolean isFrame, int lv, int lang) throws Exception {
        int w = desiredGap * 15 + rewardIconSize;

        for(int i = ENEMY; i <= BOSS; i++) {
            w += dimension[i];
        }

        int h = innerTableCellMargin * (st.data.datas.length + 1);

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setFont(contentFont);

        g.setColor(65, 69, 76);

        g.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        g.setColor(24, 25, 28);

        g.fillRoundRect(0, 0, w, innerTableCellMargin * 2, cornerRadius, cornerRadius);

        g.setColor(65, 69, 76);

        g.fillRect(0, innerTableCellMargin, w, innerTableCellMargin);

        String[] headerText = {
                LangID.getStringByID("data_enemy", lang),
                LangID.getStringByID("data_number", lang),
                LangID.getStringByID("data_basehealth", lang),
                LangID.getStringByID("data_manif", lang),
                LangID.getStringByID("data_startres", lang),
                LangID.getStringByID("data_layer", lang),
                LangID.getStringByID("data_isboss", lang)
        };

        int x = 0;

        g.setColor(191, 191, 191);

        g.setStroke(headerStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = ENEMY; i <= BOSS; i++) {
            double tx = desiredGap * 2 + dimension[i];

            if(i == ENEMY)
                tx += desiredGap + rewardIconSize;

            tx /= 2.0;

            int ly = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

            g.drawCenteredText(headerText[i], (int) Math.round(x + tx), innerTableCellMargin / 2);

            if(i < BOSS)
                g.drawLine((int) (x + tx * 2.0), ly, (int) (x + tx * 2.0), innerTableCellMargin - ly);

            x += (int) (tx * 2.0);
        }

        int y = innerTableCellMargin;

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            x = 0;

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
                    case ENEMY:
                        Identifier<AbEnemy> id = line.enemy;

                        content = null;

                        if(id.id >= UserProfile.getBCData().enemies.size()) {
                            content = map.enemyNames.get(id.id);
                        } else {
                            AbEnemy enemy = id.get();

                            if(enemy instanceof Enemy) {
                                int oldConfig = CommonStatic.getConfig().lang;
                                CommonStatic.getConfig().lang = lang;

                                content = MultiLangCont.get(enemy);

                                CommonStatic.getConfig().lang = oldConfig;

                                if(content == null || content.isBlank()) {
                                    content = ((Enemy) enemy).names.toString();
                                }

                                if(content.isBlank()) {
                                    content = map.enemyNames.get(id.id);
                                }
                            }
                        }

                        if(content == null || content.isBlank()) {
                            content = LangID.getStringByID("data_enemy", lang)+" - "+Data.trio(id.id);
                        }

                        break;
                    case NUMBER:
                        if(line.number == 0)
                            content = LangID.getStringByID("data_infinite", lang);
                        else
                            content = String.valueOf(line.number);

                        break;
                    case BASE:
                        if(line.castle_0 == line.castle_1 || line.castle_1 == 0)
                            content = line.castle_0+"%";
                        else {
                            int minHealth = Math.min(line.castle_0, line.castle_1);
                            int maxHealth = Math.max(line.castle_0, line.castle_1);

                            content = minHealth + " ~ " + maxHealth + "%";
                        }

                        break;
                    case MAGNIFICATION:
                        content = DataToString.getMagnification(new int[] {line.multiple, line.mult_atk}, map.stars[lv]);

                        break;
                    case START:
                        String start;

                        if(line.spawn_1 == 0)
                            if(isFrame)
                                start = line.spawn_0+"f";
                            else
                                start = DataToString.df.format(line.spawn_0/30.0)+"s";
                        else {
                            int minSpawn = Math.min(line.spawn_0, line.spawn_1);
                            int maxSpawn = Math.max(line.spawn_0, line.spawn_1);

                            if(isFrame)
                                start = minSpawn+"f ~ "+maxSpawn+"f";
                            else
                                start = DataToString.df.format(minSpawn/30.0)+"s ~ "+DataToString.df.format(maxSpawn/30.0)+"s";
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

                        content = start+" ("+respawn+")";

                        break;
                    case LAYER:
                        if(line.layer_0 != line.layer_1) {
                            int minLayer = Math.min(line.layer_0, line.layer_1);
                            int maxLayer = Math.max(line.layer_0, line.layer_1);

                            content = minLayer + " ~ " + maxLayer;
                        } else {
                            content = String.valueOf(line.layer_0);
                        }

                        break;
                    case BOSS:
                        if(line.boss == 0)
                            content = "";
                        else
                            content = LangID.getStringByID("data_boss", lang);

                        break;
                }

                if(j == ENEMY) {
                    g.setColor(51, 53, 60);

                    g.fillOval(desiredGap, y + (innerTableCellMargin - rewardIconSize) / 2, rewardIconSize, rewardIconSize);

                    BufferedImage icon = getEnemyIcon(line.enemy.id, map);

                    if(icon != null) {
                        g.drawImage(icon, desiredGap + 30, y + (innerTableCellMargin - rewardIconSize) / 2.0 + 30, 100, 100);
                    }

                    g.setColor(239, 239, 239);

                    g.drawCenteredText(content, desiredGap + rewardIconSize + rx, y + innerTableCellMargin / 2);
                } else {
                    g.setColor(239, 239, 239);

                    g.drawCenteredText(content, (int) (x + tx), y + innerTableCellMargin / 2);
                }

                g.setColor(191, 191, 191, 64);

                if(j < BOSS) {
                    g.drawLine((int) (x + tx * 2.0), y + ly, (int) (x + tx * 2.0), y + innerTableCellMargin - ly);
                }

                x += (int) (tx * 2.0);
            }

            y += innerTableCellMargin;
        }

        y = innerTableCellMargin * 2;

        g.setStroke(innerTableLineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 0; i < st.data.datas.length - 1; i++) {
            g.drawLine(innerTableTextMargin, y, w - innerTableTextMargin, y);

            y += innerTableCellMargin;
        }

        return result;
    }

    public static AnimU.UType getAnimType(int mode, int max) {
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
            return ""+n;
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
        for(int i = 0; i < BackgroundEffect.jsonList.length; i++) {
            if(BackgroundEffect.jsonList[i] == -ind)
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
