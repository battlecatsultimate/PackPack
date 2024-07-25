package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.system.fake.FakeTransform;
import common.system.fake.ImageBuilder;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.pack.Background;
import common.util.pack.bgeffect.BackgroundEffect;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.stage.info.DefStageInfo;
import common.util.unit.AbEnemy;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.cell.AbilityCellDrawer;
import mandarin.packpack.supporter.bc.cell.CellDrawer;
import mandarin.packpack.supporter.bc.cell.NormalCellDrawer;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lwjgl.GLGraphics;
import mandarin.packpack.supporter.lwjgl.opengl.model.FontModel;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class ImageDrawing {
    public enum Mode {
        NORMAL,
        TRUE_FORM,
        ZERO_FORM
    }

    private static final int bgAnimTime = 450;
    private static final int bgAnimHeight = 720;
    private static final float bgAnimRatio = bgAnimHeight * 0.8f / 2 / 512f;
    private static final int backgroundOffset = 600;

    private static FontModel titleFont;
    private static FontModel typeFont;
    private static FontModel nameFont;
    private static FontModel contentFont;
    private static FontModel levelFont;
    private static FontModel fruitFont;
    private static FontModel plotFont;
    private static FontModel axisFont;

    private static final int statPanelMargin = 60;
    private static final int bgMargin = 40;
    private static final int nameMargin = 40;
    private static final int cornerRadius = 75;
    private static final int typeUpDownMargin = 15;
    private static final int typeLeftRightMargin = 32;
    private static final int levelMargin = 18;
    private static final int cellMargin = 55;
    private static final float enemyIconStroke = 7.5f;
    private static final int enemyIconGap = 20;
    private static final float innerTableCornerRadius = 32.5f;
    private static final int innerTableTextMargin = 50;
    private static final int innerTableCellMargin = 100;
    private static final float headerSeparatorHeight = 62.5f;
    private static final int rewardIconSize = 80;
    private static final int lineSpace = 6;
    private static final int typeCornerRadius = 18;

    private static final float headerStroke = 2f;
    private static final float innerTableLineStroke = 1.5f;

    private static final int fruitGap = 30;
    private static final float fruitRatio = 0.125f;
    private static final float fruitTextGapRatio = 0.025f;
    private static final float fruitUpperGapRatio = 0.025f;
    private static final float fruitDownerGapRatio = 0.05f;
    private static final float enemyIconRatio = 1.25f; // w/h
    private static final float enemyInnerIconRatio = 0.95f;

    private static final int talentIconGap = 30;
    private static final int talentNameGap = 40;
    private static final int talentCostTableGap = 24;
    private static final int talentCostGap = 60;
    private static final int talentTableGap = 20;
    private static final int talentGap = 60;
    private static final int totalCostGap = 60;

    private static final int comboTitleGap = 45;
    private static final int comboTypeGap = 20;
    private static final float comboTypeInnerGap = 7.5f;
    private static final int comboTypeRadius = 15;
    private static final float comboIconScaleFactor = 2.5f;
    private static final float comboIconTableRadius = 37.5f;
    private static final int comboIconGap = 30;
    private static final int comboIconLeftRightGap = 40;
    private static final int comboIconUpDownGap = 30;
    private static final int comboIconNameGap = 40;
    private static final int comboContentGap = 60;

    private static final int plotWidthHeight = 2048;
    private static final float axisStroke = 3f;
    private static final float indicatorStroke = 4f;
    private static final float indicatorRatio = 0.025f;
    private static final float subIndicatorStroke = 2f;
    private static final int indicatorGap = 20;
    private static final float plotStroke = 8f;
    private static final float angleLimit = 89.9995f;
    private static final float multivariableAngleLimit = 89.99f;

    private static final int axisTitleGap = 80;
    private static final int plotGraphOffset = 200;

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

    public static void initialize() {
        File regular = new File("./data/NotoRegular.otf");
        File medium = new File("./data/NotoMedium.otf");

        StaticStore.renderManager.queueGL(() -> {
            try {
                titleFont = new FontModel(72f, medium, FontModel.Type.FILL, 0f);
                typeFont = new FontModel(48f, regular, FontModel.Type.FILL, 0f);
                nameFont = new FontModel(32f, medium, FontModel.Type.FILL, 0f);
                contentFont = new FontModel(42f, regular, FontModel.Type.FILL, 0f);
                levelFont = new FontModel(48f, medium, FontModel.Type.FILL, 0f);
                fruitFont = new FontModel(60f, medium, FontModel.Type.FILL, 0f);
                plotFont = new FontModel(56f, medium, FontModel.Type.FILL, 0f);
                axisFont = new FontModel(72f, medium, FontModel.Type.FILL, 0f);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::initialize - Failed to initialize font file");
            }
        });
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

        float groundRatio = 0.1f;

        float ratio = h * (1f - groundRatio * 2) / 2f / 512f;

        int groundHeight = (int) (groundRatio * h);

        bg.load();

        BackgroundEffect effect;

        if(eff && bg.effect != -1) {
            if(bg.effect < 0) {
                effect = BackgroundEffect.mixture.get(-bg.effect);
            } else {
                effect = CommonStatic.getBCAssets().bgEffects.get(bg.effect);
            }

            int len = (int) ((w / ratio - 400) / CommonStatic.BattleConst.ratio);
            int bgHeight = (int) (h / ratio);
            int midH = (int) (h * groundRatio / ratio);

            effect.initialize(len, bgHeight, midH, bg);
            effect.check();
        } else {
            effect = null;
        }

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(w, h, temp, connector -> {
            connector.queue(g -> {
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

                if(eff && bg.effect != -1 && effect != null) {
                    int len = (int) ((w / ratio - 400) / CommonStatic.BattleConst.ratio);
                    int bgHeight = (int) (h / ratio);
                    int midH = (int) (h * groundRatio / ratio);

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

                return null;
            });

            return null;
        }, progress -> image, () -> {
            bg.unload();

            if (effect != null)
                effect.release();

            waiter.countDown();

            return null;
        });

        waiter.await();

        return image;
    }

    public static File drawBGAnimEffect(Background bg, Message msg, CommonStatic.Lang.Locale lang) throws Exception {
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

        if (StaticStore.backgroundStageLength.containsKey(bg.id)) {
            pw = StaticStore.backgroundStageLength.get(bg.id);
        } else {
            pw = 600;

            for (MapColc mc : MapColc.values()) {
                for (StageMap map : mc.maps) {
                    for (Stage stage : map.list) {
                        if (stage.bg.equals(bg.id)) {
                            pw = Math.max(pw, stage.len - backgroundOffset);
                        }
                    }
                }
            }

            StaticStore.backgroundStageLength.put(bg.id, pw);
        }

        int w = (int) ((400 + pw * CommonStatic.BattleConst.ratio) * bgAnimRatio);

        if(w % 2 == 1)
            w -= 1;

        float groundRatio = 0.1f;

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

        String cont = LangID.getStringByID("data.animation.background.dimension", lang).replace("_WWW_", String.valueOf(w)).replace("_HHH_", bgAnimHeight+"") +"\n\n"+
                LangID.getStringByID("data.animation.background.progress", lang)
                        .replace("_PPP_", "  0")
                        .replace("_LLL_", bgAnimTime + "")
                        .replace("_BBB_", getProgressBar(0, bgAnimTime))
                        .replace("_VVV_", 0.0 + "")
                        .replace("_SSS_", "-");

        msg.editMessage(cont).queue();

        final int finalW = w;
        CountDownLatch pause = new CountDownLatch(1);

        AtomicReference<Long> start = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> current = new AtomicReference<>(System.currentTimeMillis());

        StaticStore.renderManager.createRenderer(w, h, temp, connector -> {
            bg.load();

            for(int i = 0; i < bgAnimTime; i++) {
                final int finalI = i;

                connector.queue(g -> {
                    if(System.currentTimeMillis() - current.get() >= 1500) {
                        String prog = DataToString.df.format(finalI * 100.0 / bgAnimTime);
                        String eta = getETA(start.get(), System.currentTimeMillis(), finalI, bgAnimTime);
                        String ind = String.valueOf(finalI);
                        String content = LangID.getStringByID("data.animation.background.dimension", lang).replace("_WWW_", String.valueOf(finalW)).replace("_HHH_", bgAnimHeight+"") +"\n\n"+
                                LangID.getStringByID("data.animation.background.progress", lang)
                                        .replace("_PPP_", " ".repeat(Math.max(0, 3 - ind.length()))+ind)
                                        .replace("_LLL_", bgAnimTime+"")
                                        .replace("_BBB_", getProgressBar(finalI, bgAnimTime))
                                        .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length()))+prog)
                                        .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length()))+eta);

                        msg.editMessage(content).queue();

                        current.set(System.currentTimeMillis());
                    }

                    g.gradRect(0, h - groundHeight, finalW, groundHeight, 0, h - groundHeight, bg.cs[2], 0, h, bg.cs[3]);

                    int pos = (int) ((-bg.parts[Background.BG].getWidth()+200) * bgAnimRatio);

                    int y = h - groundHeight;

                    int lowHeight = (int) (bg.parts[Background.BG].getHeight() * bgAnimRatio);
                    int lowWidth = (int) (bg.parts[Background.BG].getWidth() * bgAnimRatio);

                    while(pos < finalW) {
                        g.drawImage(bg.parts[Background.BG], pos, y - lowHeight, lowWidth, lowHeight);

                        pos += Math.max(1, (int) (bg.parts[0].getWidth() * bgAnimRatio));
                    }

                    if(bg.top) {
                        int topHeight = (int) (bg.parts[Background.TOP].getHeight() * bgAnimRatio);
                        int topWidth = (int) (bg.parts[Background.TOP].getWidth() * bgAnimRatio);

                        pos = (int) ((-bg.parts[Background.BG].getWidth() + 200) * bgAnimRatio);
                        y = h - groundHeight - lowHeight;

                        while(pos < finalW) {
                            g.drawImage(bg.parts[Background.TOP], pos, y - topHeight, topWidth, topHeight);

                            pos += Math.max(1, (int) (bg.parts[0].getWidth() * bgAnimRatio));
                        }

                        if(y - topHeight > 0) {
                            g.gradRect(0, 0, finalW, h - groundHeight - lowHeight - topHeight, 0, 0, bg.cs[0], 0, h - groundHeight - lowHeight - topHeight, bg.cs[1]);
                        }
                    } else {
                        g.gradRect(0, 0, finalW, h - groundHeight - lowHeight, 0, 0, bg.cs[0], 0, h - groundHeight - lowHeight, bg.cs[1]);
                    }

                    P base = P.newP((int) (h * 0.0025), (int) (BackgroundEffect.BGHeight * 3 * bgAnimRatio - h * 0.905));

                    eff.preDraw(g, base, bgAnimRatio, midH);
                    eff.postDraw(g, base, bgAnimRatio, midH);

                    P.delete(base);

                    if(bg.overlay != null) {
                        g.gradRectAlpha(0, 0, finalW, h, 0, 0, bg.overlayAlpha, bg.overlay[1], 0, h, bg.overlayAlpha, bg.overlay[0]);
                    }

                    eff.update(len, bgHeight, midH);

                    return null;
                });
            }

            return null;
        }, progress -> new File("./temp/"+folderName+"/", quad(progress)+".png"), () -> {
            try {
                String content = LangID.getStringByID("data.animation.background.dimension", lang).replace("_WWW_", String.valueOf(finalW)).replace("_HHH_", bgAnimHeight+"") +"\n\n"+
                        LangID.getStringByID("data.animation.background.progress", lang)
                                .replace("_PPP_", bgAnimTime + "")
                                .replace("_LLL_", bgAnimTime + "")
                                .replace("_BBB_", getProgressBar(bgAnimTime, bgAnimTime))
                                .replace("_VVV_", "100.00")
                                .replace("_SSS_", "     0") + "\n"+
                        LangID.getStringByID("data.animation.background.uploading", lang);

                msg.editMessage(content).queue();

                ProcessBuilder builder = new ProcessBuilder(SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg", "-r", "30", "-f", "image2", "-s", finalW+"x"+h,
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

                bg.unload();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawBGAnimEffect - Failed to generate BG effect animation");
            }

            eff.release();

            pause.countDown();

            return null;
        });

        pause.await();

        return mp4;
    }

    public static File drawAnimImage(EAnimD<?> anim, int frame, float siz, boolean transparent, boolean debug) throws Exception {
        File temp = new File("./temp");

        File file = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(file == null) {
            return null;
        }

        CommonStatic.getConfig().ref = false;

        anim.setTime(frame);

        Rectangle rect = new Rectangle();

        ArrayList<int[][]> rects = new ArrayList<>();
        ArrayList<P> centers = new ArrayList<>();

        for(int i = 0; i < anim.getOrder().length; i++) {
            if(anim.anim().parts((int) anim.getOrder()[i].getValRaw(2)) == null || anim.getOrder()[i].getValRaw(1) == -1)
                continue;

            FakeImage fi = anim.anim().parts((int) anim.getOrder()[i].getValRaw(2));

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

            rect.width = Math.round(Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width));
            rect.height = Math.round(Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height));
        }

        if(rect.width == 0)
            rect.width = 2;

        if(rect.height == 0)
            rect.height = 2;

        if(rect.width % 2 == 1)
            rect.width++;

        if(rect.height % 2 == 1)
            rect.height++;

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(rect.width, rect.height, temp, connector -> {
            connector.queue(rg -> {
                if(!transparent) {
                    rg.setColor(54,57,63,255);
                    rg.fillRect(0, 0, rect.width, rect.height);
                }

                FakeTransform t = rg.getTransform();

                anim.draw(rg, new P(-rect.x, -rect.y), siz);

                rg.setTransform(t);

                rg.setStroke(1.5f, GLGraphics.LineEndMode.VERTICAL);

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

                return null;
            });

            return null;
        }, progress -> file, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return file;
    }

    public static File drawAnimMp4(EAnimD<?> anim, Message msg, float siz, boolean performance, boolean debug, int limit, CommonStatic.Lang.Locale lang) throws Exception {
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

        File mp4 = StaticStore.generateTempFile(temp, "result", ".mp4", false);

        if(mp4 == null) {
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

        for(float i = 0; i < frame; i += performance ? 0.5f : 1f) {
            anim.setTime(i);

            ArrayList<int[][]> rects = new ArrayList<>();
            ArrayList<P> centers = new ArrayList<>();

            for(int j = 0; j < anim.getOrder().length; j++) {
                if(anim.anim().parts((int) anim.getOrder()[j].getValRaw(2)) == null || anim.getOrder()[j].getValRaw(1) == -1)
                    continue;

                FakeImage fi = anim.anim().parts((int) anim.getOrder()[j].getValRaw(2));

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

                rect.width = Math.round(Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width));
                rect.height = Math.round(Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height));
            }

            rectFrames.add(rects);
            centerFrames.add(centers);
        }

        float ratio;

        String cont = LangID.getStringByID("data.animation.gif.analyzingBox", lang)+ "\n"
                + LangID.getStringByID("data.animation.gif.analysis.result", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x));

        msg.editMessage(cont).queue();


        if (rect.width * rect.height > 500 * 500) {
            if (rect.width * 2 < rect.height) {
                int originalHeight = rect.height;

                rect.height = (int) Math.round(rect.width * 1.5);

                int diff = originalHeight - rect.height;

                rect.y += diff;
            } else if (rect.height * 2 < rect.width) {
                int originalWidth = rect.width;

                rect.width = (int) Math.round(rect.height * 1.5);

                int diff = originalWidth - rect.width;

                rect.x += diff;
            }
        }

        if(rect.width * rect.height > 1500 * 1500) {
            ratio = 1f;

            while(rect.width * rect.height > 1500 * 1500) {
                ratio *= 0.5f;

                rect.width = (int) (0.5 * rect.width);
                rect.height = (int) (0.5 * rect.height);
                rect.x = (int) (0.5 * rect.x);
                rect.y = (int) (0.5 * rect.y);
            }
        } else {
            ratio = 1f;
        }

        String baseContent = cont+"\n\n";

        if(ratio == 1.0) {
            baseContent += LangID.getStringByID("data.animation.gif.analysis.canGo", lang)+"\n\n";
        } else {
            baseContent += LangID.getStringByID("data.animation.gif.analysis.adjust.gif", lang).replace("_", DataToString.df.format(ratio * 100.0))+"\n\n";
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

        baseContent += LangID.getStringByID("data.animation.gif.analysis.final", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x))+"\n";

        msg.editMessage(baseContent).queue();

        P pos = new P(-rect.x, -rect.y);

        int finalFrame = frame;
        String finalBaseContent = baseContent;
        float finalRatio = ratio;

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(rect.width, rect.height, temp, connector -> {
            AtomicReference<Long> start = new AtomicReference<>(System.currentTimeMillis());
            AtomicReference<Long> current = new AtomicReference<>(System.currentTimeMillis());

            for(float i = 0; i < finalFrame; i += performance ? 0.5f : 1f) {
                final float finalF = i;

                connector.queue(g -> {
                    if(System.currentTimeMillis() - current.get() >= 1500) {
                        String content = finalBaseContent +"\n\n";

                        String prog = DataToString.df.format(finalF * 100.0 / finalFrame);
                        String eta = getETA(start.get(), System.currentTimeMillis(), finalF, performance ? finalFrame * 2 : finalFrame);
                        String ind = String.valueOf(finalF);
                        String len = String.valueOf(finalFrame);

                        content += LangID.getStringByID("data.animation.background.progress", lang)
                                .replace("_PPP_", " ".repeat(Math.max(0, len.length() - ind.length()))+ind)
                                .replace("_LLL_", len)
                                .replace("_BBB_", getProgressBar(finalF, finalFrame))
                                .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length()))+prog)
                                .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length()))+eta);

                        msg.editMessage(content).queue();

                        current.set(System.currentTimeMillis());
                    }

                    anim.setTime(finalF);

                    g.setStroke(1.5f, GLGraphics.LineEndMode.VERTICAL);

                    g.setColor(54,57,63,255);
                    g.fillRect(0, 0, rect.width, rect.height);

                    if(debug) {
                        int rectIndex = (int) (performance ? finalF * 2f : finalF);

                        for(int j = 0; j < rectFrames.get(rectIndex).size(); j++) {
                            int[][] r = rectFrames.get(rectIndex).get(j);
                            P c = centerFrames.get(rectIndex).get(j);

                            g.setColor(FakeGraphics.RED);

                            g.drawLine(-rect.x + (int) (finalRatio * r[0][0]), -rect.y + (int) (finalRatio * r[0][1]), -rect.x + (int) (finalRatio * r[1][0]), -rect.y + (int) (finalRatio * r[1][1]));
                            g.drawLine(-rect.x + (int) (finalRatio * r[1][0]), -rect.y + (int) (finalRatio * r[1][1]), -rect.x + (int) (finalRatio * r[2][0]), -rect.y + (int) (finalRatio * r[2][1]));
                            g.drawLine(-rect.x + (int) (finalRatio * r[2][0]), -rect.y + (int) (finalRatio * r[2][1]), -rect.x + (int) (finalRatio * r[3][0]), -rect.y + (int) (finalRatio * r[3][1]));
                            g.drawLine(-rect.x + (int) (finalRatio * r[3][0]), -rect.y + (int) (finalRatio * r[3][1]), -rect.x + (int) (finalRatio * r[0][0]), -rect.y + (int) (finalRatio * r[0][1]));

                            g.setColor(0, 255, 0, 255);

                            g.fillRect(-rect.x + (int) (c.x * finalRatio) - 2, -rect.y + (int) (c.y * finalRatio) -2, 4, 4);
                        }
                    } else {
                        anim.draw(g, pos, siz * finalRatio);
                    }

                    return null;
                });
            }

            return null;
        }, progress -> new File("./temp/"+folderName+"/", quad(progress)+".png"), () -> {
            try {
                String content = finalBaseContent + "\n\n" + LangID.getStringByID("data.animation.gif.making.png", lang).replace("_", "100")
                        +"\n\n"+ LangID.getStringByID("data.animation.gif.converting", lang);

                msg.editMessage(content).queue();

                ProcessBuilder builder = new ProcessBuilder(SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg", "-r", performance ? "60" : "30", "-f", "image2", "-s", rect.width+"x"+rect.height,
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

                content = finalBaseContent + "\n\n" +
                        LangID.getStringByID("data.animation.gif.making.png", lang).replace("_", "100") + "\n\n" +
                        LangID.getStringByID("data.animation.background.progress", lang)
                                .replace("_PPP_", String.valueOf(finalFrame))
                                .replace("_LLL_", String.valueOf(finalFrame))
                                .replace("_BBB_", getProgressBar(finalFrame, finalFrame))
                                .replace("_VVV_", "100.00")
                                .replace("_SSS_", "     0") + "\n" +
                        LangID.getStringByID("data.animation.gif.uploading.mp4", lang);

                msg.editMessage(content).queue();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawAnimMp4 - Failed to generate mp4 file");
            }

            waiter.countDown();

            return null;
        });

        waiter.await();

        return mp4;
    }

    public static File drawAnimGif(EAnimD<?> anim, Message msg, float siz, boolean performance, boolean debug, int limit, CommonStatic.Lang.Locale lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File targetFolder = StaticStore.generateTempFile(temp, "gifSession", "", true);

        if (targetFolder == null)
            return null;

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

        for(float i = 0; i < Math.min(frame, 300); i += performance ? 0.5f : 1f) {
            anim.setTime(i);

            ArrayList<int[][]> rects = new ArrayList<>();
            ArrayList<P> centers = new ArrayList<>();

            for(int j = 0; j < anim.getOrder().length; j++) {
                if(anim.anim().parts((int) anim.getOrder()[j].getValRaw(2)) == null || anim.getOrder()[j].getValRaw(1) == -1)
                    continue;

                FakeImage fi = anim.anim().parts((int) anim.getOrder()[j].getValRaw(2));

                if(fi.getWidth() == 1 && fi.getHeight() == 1)
                    continue;

                RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

                getter.apply(anim.getOrder()[j], siz * 0.5f, false);

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

                rect.width = Math.round(Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width));
                rect.height = Math.round(Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height));
            }

            rectFrames.add(rects);
            centerFrames.add(centers);
        }

        int minSize = 300;

        float ratio;

        long surface = (long) rect.width * rect.height;

        if(surface > minSize * minSize) {
            ratio = (surface - (surface - 300 * 300) * 1f * Math.min(300, frame) / 300f) / surface;
        } else {
            ratio = 1f;
        }

        String cont = LangID.getStringByID("data.animation.gif.analyzingBox", lang)+ "\n"
                + LangID.getStringByID("data.animation.gif.analysis.result", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x))+"\n";

        if(ratio != 1.0) {
            cont += LangID.getStringByID("data.animation.gif.analysis.adjust.gif", lang).replace("_", DataToString.df.format(ratio * 100.0))+"\n";
        } else {
            cont += LangID.getStringByID("data.animation.gif.analysis.canGo", lang)+"\n";
        }

        cont += LangID.getStringByID("data.animation.gif.analysis.final", lang).replace("_WWW_", String.valueOf((int) (ratio * rect.width)))
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

        P pos = new P(-rect.x, -rect.y);

        long start = System.currentTimeMillis();
        final long[] current = {System.currentTimeMillis()};

        CountDownLatch waiter = new CountDownLatch(1);

        int finalFrame = frame;
        String finalCont = cont;

        StaticStore.renderManager.createRenderer(rect.width, rect.height, targetFolder, connector -> {
            for(float i = 0; i < Math.min(finalFrame, 300); i += performance ? 0.5f : 1f) {
                final float finalF = i;

                connector.queue(g -> {
                    if(System.currentTimeMillis() - current[0] >= 1000) {
                        String content = finalCont +"\n\n";

                        String prog = DataToString.df.format(finalF * 100.0 / finalFrame);
                        String eta = getETA(start, System.currentTimeMillis(), finalF, performance ? finalFrame * 2 : finalFrame);
                        String ind = String.valueOf(finalF);
                        String len = String.valueOf(finalFrame);

                        content += LangID.getStringByID("data.animation.background.progress", lang)
                                .replace("_PPP_", " ".repeat(Math.max(0, len.length() - ind.length()))+ind)
                                .replace("_LLL_", len)
                                .replace("_BBB_", getProgressBar(finalF, finalFrame))
                                .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length()))+prog)
                                .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length()))+eta);

                        msg.editMessage(content).queue();

                        current[0] = System.currentTimeMillis();
                    }

                    anim.setTime(finalF);

                    g.setStroke(1.5f, GLGraphics.LineEndMode.VERTICAL);

                    g.setColor(54,57,63,255);
                    g.fillRect(0, 0, rect.width, rect.height);

                    if(debug) {
                        int rectIndex = (int) (performance ? finalF * 2f : finalF);

                        for(int j = 0; j < rectFrames.get(rectIndex).size(); j++) {
                            int[][] r = rectFrames.get(rectIndex).get(j);
                            P c = centerFrames.get(rectIndex).get(j);

                            g.setColor(FakeGraphics.RED);

                            g.drawLine(-rect.x + (int) (r[0][0] * ratio), -rect.y + (int) (r[0][1] * ratio), -rect.x + (int) (r[1][0] * ratio), -rect.y + (int) (r[1][1] * ratio));
                            g.drawLine(-rect.x + (int) (r[1][0] * ratio), -rect.y + (int) (r[1][1] * ratio), -rect.x + (int) (r[2][0] * ratio), -rect.y + (int) (r[2][1] * ratio));
                            g.drawLine(-rect.x + (int) (r[2][0] * ratio), -rect.y + (int) (r[2][1] * ratio), -rect.x + (int) (r[3][0] * ratio), -rect.y + (int) (r[3][1] * ratio));
                            g.drawLine(-rect.x + (int) (r[3][0] * ratio), -rect.y + (int) (r[3][1] * ratio), -rect.x + (int) (r[0][0] * ratio), -rect.y + (int) (r[0][1] * ratio));

                            g.setColor(0, 255, 0, 255);

                            g.fillRect(-rect.x + (int) (ratio * c.x) - 2, -rect.y + (int) (ratio * c.y) -2, 4, 4);
                        }
                    } else {
                        anim.setTime(finalF);

                        anim.draw(g, pos, siz * ratio * 0.5f);
                    }

                    return null;
                });
            }

            return null;
        }, null, () -> {
            String content = finalCont + "\n\n"+
                    LangID.getStringByID("data.animation.background.progress", lang)
                            .replace("_PPP_", String.valueOf(finalFrame))
                            .replace("_LLL_", String.valueOf(finalFrame))
                            .replace("_BBB_", getProgressBar(finalFrame, finalFrame))
                            .replace("_VVV_", "100.00")
                            .replace("_SSS_", "     0") + "\n"+
                    LangID.getStringByID("data.animation.gif.uploading.gif", lang);

            msg.editMessage(content).queue();

            try {
                ProcessBuilder builder = new ProcessBuilder(
                        SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg", "-i",
                        "temp/" + targetFolder.getName() + "/%04d.png", "-vf", "palettegen",
                        "temp/" + targetFolder.getName() + "/palette.png"
                );

                builder.redirectErrorStream(true);

                Process pro = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

                String line;

                while((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                pro.waitFor();
                reader.close();

                builder = new ProcessBuilder(
                        SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg",
                        "-r", performance ? "50" : "30", "-i", "temp/" + targetFolder.getName() + "/%04d.png",
                        "-i", "temp/" + targetFolder.getName() + "/palette.png", "-lavfi", "paletteuse",
                        "-y", "temp/" + gif.getName()
                );

                builder.redirectErrorStream(true);

                pro = builder.start();

                reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

                while((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                pro.waitFor();
                reader.close();

                StaticStore.deleteFile(targetFolder, true);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawAnimGif - Failed to generate gif file with FFMPEG");
            }

            waiter.countDown();

            return null;
        });

        waiter.await();

        return gif;
    }

    public static File drawBCAnim(AnimMixer mixer, Message msg, float siz, boolean performance, CommonStatic.Lang.Locale lang) throws Exception {
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

                for(float j = 0; j < anim.len(); j += performance ? 0.5f : 1f) {
                    anim.setTime(j);

                    for(int k = 0; k < anim.getOrder().length; k++) {
                        if(anim.anim().parts((int) anim.getOrder()[k].getValRaw(2)) == null || anim.getOrder()[k].getValRaw(1) == -1)
                            continue;

                        FakeImage fi = anim.anim().parts((int) anim.getOrder()[k].getValRaw(2));

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

                        rect.width = Math.round(Math.max(Math.abs(maxAmong(result[0][0], result[1][0], result[2][0], result[3][0]) - rect.x), rect.width));
                        rect.height = Math.round(Math.max(Math.abs(maxAmong(result[0][1], result[1][1], result[2][1], result[3][1]) - rect.y), rect.height));
                    }
                }
            }
        }

        float ratio;

        String cont = LangID.getStringByID("data.animation.gif.analyzingBox", lang) + "\n"
                + LangID.getStringByID("data.animation.gif.analysis.result", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.y));

        msg.editMessage(cont).queue();

        if (rect.width * rect.height > 500 * 500) {
            if (rect.width * 2 < rect.height) {
                int originalHeight = rect.height;

                rect.height = (int) Math.round(rect.width * 1.5);

                int diff = originalHeight - rect.height;

                rect.y += diff;
            } else if (rect.height * 2 < rect.width) {
                int originalWidth = rect.width;

                rect.width = (int) Math.round(rect.height * 1.5);

                int diff = originalWidth - rect.width;

                rect.x += diff;
            }
        }

        if(rect.width * rect.height > 1500 * 1500) {
            ratio = 1f;

            while(rect.width * rect.height > 1500 * 1500) {
                ratio *= 0.5f;

                rect.width = (int) (0.5 * rect.width);
                rect.height = (int) (0.5 * rect.height);
                rect.x = (int) (0.5 * rect.x);
                rect.y = (int) (0.5 * rect.y);
            }
        } else {
            ratio = 1f;
        }

        String content = cont +"\n\n";

        if(ratio == 1f) {
            content += LangID.getStringByID("data.animation.gif.analysis.canGo", lang)+"\n\n";
        } else {
            content += LangID.getStringByID("data.animation.gif.analysis.adjust.gif", lang).replace("_", DataToString.df.format(ratio * 100.0))+"\n\n";
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

        content += LangID.getStringByID("data.animation.gif.analysis.final", lang).replace("_WWW_", String.valueOf(rect.width))
                .replace("_HHH_", String.valueOf(rect.height)).replace("_XXX_", String.valueOf(rect.x))
                .replace("_YYY_", String.valueOf(rect.x))+"\n";

        msg.editMessage(content).queue();

        P pos = new P(-rect.x, -rect.y);

        int totalFrame = 0;

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

        int finalTotalFrame = totalFrame;
        String finalContent = content;
        float finalRatio = ratio;

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(rect.width, rect.height, folder, connector -> {
            AtomicReference<Long> current = new AtomicReference<>(System.currentTimeMillis());
            AtomicReference<Integer> progress = new AtomicReference<>(0);

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
                            for(float j = 0; j < frame; j += performance ? 0.5f : 1f) {
                                final float finalJ = j;

                                connector.queue(g -> {
                                    if(System.currentTimeMillis() - current.get() >= 1500) {
                                        String editContent = finalContent +"\n\n";

                                        String prog = DataToString.df.format(progress.get() * 100.0 / (performance ? finalTotalFrame * 2 : finalTotalFrame));
                                        String eta = getETA(start, System.currentTimeMillis(), progress.get(), performance ? finalTotalFrame * 2 : finalTotalFrame);

                                        editContent += LangID.getStringByID("data.animation.gif.making.png", lang) +
                                                LangID.getStringByID("data.animation.background.progress", lang)
                                                        .replace("_PPP_", DataToString.df.format(performance ? progress.get() / 2f : progress.get()))
                                                        .replace("_LLL_", String.valueOf(finalTotalFrame))
                                                        .replace("_BBB_", getProgressBar(progress.get(), performance ? finalTotalFrame * 2 : finalTotalFrame))
                                                        .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length())) + prog)
                                                        .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length())) + eta);

                                        msg.editMessage(editContent).queue();

                                        current.set(System.currentTimeMillis());
                                    }

                                    anim.setTime(finalJ);

                                    g.setColor(54,57,63,255);
                                    g.fillRect(0, 0, rect.width, rect.height);

                                    anim.draw(g, pos, siz * finalRatio);

                                    progress.set(progress.get() + 1);

                                    return null;
                                });
                            }

                            stackFrame += frame;
                        }
                    } else {
                        for(float j = 0; j < frame - 1; j += performance ? 0.5f : 1f) {
                            final float finalJ = j;

                            connector.queue(g -> {
                                if(System.currentTimeMillis() - current.get() >= 1500) {
                                    String editContent = finalContent +"\n\n";

                                    String prog = DataToString.df.format(progress.get() * 100.0 / (performance ? finalTotalFrame * 2 : finalTotalFrame));
                                    String eta = getETA(start, System.currentTimeMillis(), progress.get(), performance ? finalTotalFrame * 2 : finalTotalFrame);

                                    editContent += LangID.getStringByID("data.animation.gif.making.png", lang) +
                                            LangID.getStringByID("data.animation.background.progress", lang)
                                                    .replace("_PPP_", DataToString.df.format(performance ? progress.get() / 2f : progress.get()))
                                                    .replace("_LLL_", String.valueOf(finalTotalFrame))
                                                    .replace("_BBB_", getProgressBar(progress.get(), performance ? finalTotalFrame * 2 : finalTotalFrame))
                                                    .replace("_VVV_", " ".repeat(Math.max(0, 6 - prog.length())) + prog)
                                                    .replace("_SSS_", " ".repeat(Math.max(0, 6 - eta.length())) + eta);

                                    msg.editMessage(editContent).queue();

                                    current.set(System.currentTimeMillis());
                                }

                                anim.setTime(finalJ);

                                g.setColor(54,57,63,255);
                                g.fillRect(0, 0, rect.width, rect.height);

                                anim.draw(g, pos, siz * finalRatio);

                                progress.set(progress.get() + 1);

                                return null;
                            });
                        }
                    }
                }
            }

            return null;
        }, progress -> new File(folder, quad(progress) + ".png"), () -> {
            try {
                String editContent = finalContent + "\n\n" +
                        LangID.getStringByID("data.animation.gif.making.png", lang) +
                        LangID.getStringByID("data.animation.background.progress", lang)
                                .replace("_PPP_", String.valueOf(finalTotalFrame))
                                .replace("_LLL_", String.valueOf(finalTotalFrame))
                                .replace("_BBB_", getProgressBar(finalTotalFrame, finalTotalFrame))
                                .replace("_VVV_", "100.00")
                                .replace("_SSS_", "     0") + "\n" +
                        LangID.getStringByID("data.animation.gif.converting", lang);

                msg.editMessage(editContent).queue();

                ProcessBuilder builder = new ProcessBuilder(SystemUtils.IS_OS_WINDOWS ? "data/ffmpeg/bin/ffmpeg" : "ffmpeg", "-r", performance ? "60" : "30", "-f", "image2", "-s", rect.width+"x"+rect.height,
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

                editContent = finalContent + "\n\n" +
                        LangID.getStringByID("data.animation.gif.making.png", lang) +
                        LangID.getStringByID("data.animation.background.progress", lang)
                                .replace("_PPP_", String.valueOf(finalTotalFrame))
                                .replace("_LLL_", String.valueOf(finalTotalFrame))
                                .replace("_BBB_", getProgressBar(finalTotalFrame, finalTotalFrame))
                                .replace("_VVV_", "100.00")
                                .replace("_SSS_", "     0") + "\n" +
                        LangID.getStringByID("data.animation.gif.converting", lang) + "\n\n" +
                        LangID.getStringByID("data.animation.gif.uploading.mp4", lang);

                msg.editMessage(editContent).queue();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawBCAnim - Failed to export animation as mp4");
            }

            waiter.countDown();

            return null;
        });

        waiter.await();

        return gif;
    }

    public static File drawStatImage(CustomMaskUnit[] units, List<List<CellDrawer>> cellGroup, int lv, String[] name, String type, File container, File itemContainer, Mode mode, int uID, int[] egg, int[][] trueForm) throws Exception {
        File f = new File("./temp/");

        if(!f.exists() && !f.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(f, "result", ".png", false);

        if(image == null) {
            return null;
        }

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

                group.get(j).initialize(nameFont, contentFont, 0);

                if(group.get(j) instanceof NormalCellDrawer)
                    offset = Math.round(Math.max(((NormalCellDrawer) group.get(j)).offset, offset));

                int tempH = Math.round(Math.max(((NormalCellDrawer) group.get(j)).h, ((NormalCellDrawer) group.get(j)).ih));
                int tempUw = Math.round(((NormalCellDrawer) group.get(j)).uw);

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

            group.getLast().initialize(nameFont, contentFont, (int) Math.round((uw * 3 + CellDrawer.lineOffset * 4) * 1.5));

            offset = Math.round(Math.max(((AbilityCellDrawer) group.getLast()).offset, offset));

            int tempH = Math.round(((AbilityCellDrawer) group.getLast()).h);
            int tempUw = Math.round(((AbilityCellDrawer) group.getLast()).w);

            ah = Math.max(tempH, ah);
            aw = Math.max(tempUw, aw);
        }

        if(aw > uw * 3 + CellDrawer.lineOffset * 4) {
            uw = (aw - CellDrawer.lineOffset * 4) / 3;
        }

        int titleW = 0;
        int titleH = 0;

        if(mode != Mode.NORMAL) {
            int[] titleDimension = measureUnitTitleImage(name[0], type, lv);

            titleW = Math.max(titleW, titleDimension[0]);
            titleH = Math.max(titleH, titleDimension[1]);

            if(uw * 3 + CellDrawer.lineOffset * 4 + statPanelMargin * 2 < titleDimension[0]) {
                uw = (titleDimension[0] - statPanelMargin * 2 - CellDrawer.lineOffset * 4) / 3;
            }
        } else {
            for(int i = 0; i < units.length; i++) {
                int[] titleDimension = measureUnitTitleImage(name[i], type, lv);

                titleW = Math.max(titleW, titleDimension[0]);
                titleH = Math.max(titleH, titleDimension[1]);

                if(uw * 3 + CellDrawer.lineOffset * 4 + statPanelMargin * 2 < titleDimension[0]) {
                    uw = (titleDimension[0] - statPanelMargin * 2 - CellDrawer.lineOffset * 4) / 3;
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

        int totalWidth = Math.max(titleW, w + statPanelMargin * 2) + bgMargin * 2;
        int totalHeight = bgMargin * 5 + titleH + statPanelMargin * 2 + h;
        int fruitH;

        if(trueForm != null && (mode != Mode.NORMAL || units.length >= 3)) {
            float textHeight = fruitFont.measureDimension("1234567890Mk")[3];

            fruitH = (int) (textHeight + (totalWidth - 2 * bgMargin) * (fruitRatio + fruitTextGapRatio + fruitUpperGapRatio + fruitDownerGapRatio) + fruitGap);

            totalHeight += fruitH;
        } else {
            fruitH = 0;
        }

        CountDownLatch waiter = new CountDownLatch(1);

        int finalTotalHeight = totalHeight;
        int finalTitleH = titleH;
        int finalH = h;
        int finalUw = uw;
        int finalUh = uh;
        int finalOffset = offset;

        StaticStore.renderManager.createRenderer(totalWidth * units.length, totalHeight, f, connector -> {
            connector.queue(g -> {
                int bx = 0;

                g.setColor(50, 53, 59);
                g.fillRect(0, 0, totalWidth * units.length, finalTotalHeight);

                g.setColor(24, 25, 28);
                g.fillRoundRect(0, -cornerRadius, totalWidth * units.length, cornerRadius + bgMargin * 8 + finalTitleH, cornerRadius, cornerRadius);

                for(int i = 0; i < units.length; i++) {
                    g.setColor(64, 68, 75);

                    if(mode == Mode.NORMAL && units.length >= 3 && trueForm != null && i != 2) {
                        g.fillRoundRect(bx + bgMargin, bgMargin * 4 + finalTitleH, w + statPanelMargin * 2, finalH + statPanelMargin * 2 + fruitH, cornerRadius, cornerRadius);
                    } else {
                        g.fillRoundRect(bx + bgMargin, bgMargin * 4 + finalTitleH, w + statPanelMargin * 2, finalH + statPanelMargin * 2, cornerRadius, cornerRadius);
                    }

                    int baseX = bx + bgMargin;
                    int baseY = bgMargin * 2;

                    g.translate(baseX, baseY);

                    File icon;

                    if (mode == Mode.TRUE_FORM) {
                        icon = new File(container, "uni"+Data.trio(uID)+"_s00.png");
                    } else if (mode == Mode.ZERO_FORM) {
                        icon = new File(container, "uni"+Data.trio(uID)+"_u00.png");
                    } else {
                        if (egg != null && i < egg.length && egg[i] != -1) {
                            icon = new File(container, "uni" + Data.trio(egg[i]) + "_m" + Data.duo(i) + ".png");
                        } else {
                            icon = new File(container, "uni" + Data.trio(uID) + "_" + getUnitCode(i) + "00.png");
                        }
                    }

                    FakeImage ic;

                    try {
                        ic = ImageBuilder.builder.build(icon).getSubimage(9, 21, 110, 85);
                    } catch (IOException e) {
                        StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawStatImage - Failed to read icon image");

                        return null;
                    }

                    float[] titleDimension = titleFont.measureDimension(name[i]);
                    float[] typeDimension = typeFont.measureDimension(type);
                    float[] levelDimension = levelFont.measureDimension("Lv. "+lv);

                    int titleHeight = Math.round(titleDimension[3] + nameMargin + typeDimension[3] + typeUpDownMargin * 2 + levelMargin + levelDimension[3]);
                    int iconWidth = (int) ((titleHeight - levelDimension[3] - levelMargin) * 1.0 * 110 / 85);

                    g.setColor(238, 238, 238, 255);
                    g.setFontModel(titleFont);

                    g.drawText(name[i], iconWidth + nameMargin, 0, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                    g.setFontModel(levelFont);

                    g.drawText("Lv. " + lv, iconWidth / 2f, (int) (titleHeight - levelDimension[3]), GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);

                    g.setColor(88, 101, 242, 255);

                    g.fillRoundRect(iconWidth + nameMargin, (int) (titleDimension[3] + nameMargin), (int) (typeLeftRightMargin * 2 + typeDimension[2]), (int) (typeUpDownMargin * 2 + typeDimension[3]), typeCornerRadius, typeCornerRadius);

                    g.setColor(238, 238, 238, 255);
                    g.setFontModel(typeFont);

                    g.drawText(type, iconWidth + nameMargin + typeLeftRightMargin, (int) (titleDimension[3] + nameMargin + typeUpDownMargin), GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                    g.drawImage(ic, 0, 0, iconWidth, titleHeight - levelDimension[3] - levelMargin);

                    g.translate(-baseX, -baseY);

                    baseX = bx + bgMargin + statPanelMargin;
                    baseY = bgMargin * 4 + finalTitleH + statPanelMargin;

                    g.translate(baseX, baseY);

                    List<CellDrawer> group = cellGroup.get(i);

                    int x = 0;
                    int y = 0;

                    for(int k = 0; k < group.size(); k++) {
                        group.get(k).draw(g, x, y, finalUw, finalOffset, finalUh, nameFont, contentFont);

                        y += finalUh + cellMargin;

                        if(k == group.size() - 2)
                            y += cellMargin;
                    }

                    g.translate(-baseX, -baseY);

                    bx += totalWidth;
                }

                if((units.length >= 3 || mode != Mode.NORMAL) && trueForm != null) {
                    bx -= totalWidth;

                    float baseX = bx + bgMargin;
                    float baseY = bgMargin * 4 + finalTitleH + finalH + statPanelMargin * 2 + fruitGap;

                    g.translate(baseX, baseY);

                    int targetWidth = totalWidth - bgMargin * 2;
                    float textHeight = contentFont.measureDimension("1234567890Mk")[3];

                    float evolveHeight = textHeight + targetWidth * (fruitRatio + fruitTextGapRatio + fruitUpperGapRatio + fruitDownerGapRatio);

                    float panelPadding = targetWidth * (1f - fruitRatio * trueForm.length) / (5f * trueForm.length - 1);
                    float padding = panelPadding * 2;

                    float panelWidth = padding * 2 + fruitRatio * targetWidth;

                    g.setFontModel(fruitFont);

                    float x = 0;

                    for(int i = 0; i < trueForm.length; i++) {
                        g.setColor(64, 68, 75, 255);

                        g.fillRoundRect((int) x, 0, (int) panelWidth, (int) evolveHeight, cornerRadius, cornerRadius);

                        try {
                            FakeImage icon = getFruitImage(itemContainer, trueForm[i][0]);

                            if (icon != null) {
                                g.drawImage(icon, x + padding, targetWidth * fruitUpperGapRatio, targetWidth * fruitRatio, targetWidth * fruitRatio);
                            }
                        } catch (Exception e) {
                            StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::generateEvolveImage - Failed to generate fruit image : "+trueForm[i][0]);
                        }

                        g.setColor(238, 238, 238, 255);

                        g.drawText(convertValue(trueForm[i][1]), (int) (x + panelWidth / 2), (int) Math.round(targetWidth * (fruitUpperGapRatio + fruitRatio + fruitTextGapRatio) + textHeight / 2.0), GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                        x += panelWidth + panelPadding;
                    }

                    g.translate(-baseX, -baseY);
                }

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return image;
    }

    public static File drawEnemyStatImage(List<CellDrawer> cellGroup, String mag, String name, File container, int eID) throws Exception {
        File f = new File("./temp/");

        if(!f.exists() && !f.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(f, "result", ".png", false);

        if(image == null) {
            return null;
        }

        int uh = 0;
        int uw = 0;

        int ah = 0;
        int aw = 0;

        int offset = 0;

        for(int i = 0; i < cellGroup.size() - 1; i++) {
            if(cellGroup.get(i) instanceof AbilityCellDrawer)
                continue;

            cellGroup.get(i).initialize(nameFont, contentFont, 0);

            if(cellGroup.get(i) instanceof NormalCellDrawer)
                offset = Math.round(Math.max(((NormalCellDrawer) cellGroup.get(i)).offset, offset));

            int tempH = Math.round(Math.max(((NormalCellDrawer) cellGroup.get(i)).h, ((NormalCellDrawer) cellGroup.get(i)).ih));
            int tempUw = Math.round(((NormalCellDrawer) cellGroup.get(i)).uw);

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

        cellGroup.getLast().initialize(nameFont, contentFont, (int) Math.round((uw * 3 + CellDrawer.lineOffset * 4) * 1.5));

        offset = Math.round(Math.max(((AbilityCellDrawer) cellGroup.getLast()).offset, offset));

        int tempH = Math.round(((AbilityCellDrawer) cellGroup.getLast()).h);
        int tempUw = Math.round(((AbilityCellDrawer) cellGroup.getLast()).w);

        ah = Math.max(tempH, ah);
        aw = Math.max(tempUw, aw);

        if(aw > uw * 3 + CellDrawer.lineOffset * 4) {
            uw = (aw - CellDrawer.lineOffset * 4) / 3;
        }

        File icon = new File(container, "enemy_icon_"+Data.trio(eID)+".png");

        float[] titleDimension = measureEnemyTitle(name, mag);

        if(uw * 3 + CellDrawer.lineOffset * 4 + statPanelMargin * 2 < titleDimension[0]) {
            uw = Math.round((titleDimension[0] - statPanelMargin * 2 - CellDrawer.lineOffset * 4) / 3);
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

        int finW = Math.round(Math.max(titleDimension[0], w + statPanelMargin * 2) + bgMargin * 2);
        int finH = Math.round(bgMargin * 5 + titleDimension[1] + statPanelMargin * 2 + h);

        CountDownLatch waiter = new CountDownLatch(1);

        int finalUw = uw;
        int finalOffset = offset;
        int finalUh = uh;
        int finalH = h;

        StaticStore.renderManager.createRenderer(finW, finH, f, connector -> {
            connector.queue(g -> {
                g.setColor(50, 53, 59);
                g.fillRect(0, 0, finW, finH);

                g.setColor(24, 25, 28);
                g.fillRoundRect(0, -cornerRadius, finW, cornerRadius + bgMargin * 8 + titleDimension[1], cornerRadius, cornerRadius);

                g.setColor(64, 68, 75);

                g.fillRoundRect(bgMargin, bgMargin * 4 + titleDimension[1], w + statPanelMargin * 2, finalH + statPanelMargin * 2, cornerRadius, cornerRadius);

                g.translate(bgMargin, bgMargin * 2);

                FakeImage ic;

                try {
                    ic = ImageBuilder.builder.build(icon);

                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawEnemyStatImage - Failed to get icon : " + icon.getAbsolutePath());

                    return null;
                }

                float[] nRect = titleFont.measureDimension(name);

                int icw = (int) (titleDimension[1] * enemyIconRatio);

                g.setColor(238, 238, 238, 255);
                g.setFontModel(titleFont);

                g.drawText(name, icw + nameMargin, enemyIconGap, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                g.setFontModel(levelFont);

                if(CommonStatic.parseIntN(mag) != 100) {
                    g.drawText(mag, icw + nameMargin, (int) (nRect[2] + nameMargin + enemyIconGap), GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
                }

                g.setColor(54, 57, 63);

                g.fillRoundRect(enemyIconStroke / 2f, enemyIconStroke / 2f, icw - enemyIconStroke, titleDimension[1] - enemyIconStroke, cornerRadius, cornerRadius);

                int size = (int) Math.min(titleDimension[1] * enemyInnerIconRatio, titleDimension[1] - enemyIconStroke * 2);

                g.drawImage(ic, (icw - titleDimension[1] + enemyIconStroke) / 2f, enemyIconStroke / 2f, size, size);

                g.setColor(191, 191, 191);
                g.setStroke(enemyIconStroke, GLGraphics.LineEndMode.ROUND);

                g.roundRect(enemyIconStroke / 2f, enemyIconStroke / 2f, icw - enemyIconStroke, titleDimension[1] - enemyIconStroke, cornerRadius, cornerRadius);

                g.translate(bgMargin + statPanelMargin - bgMargin, bgMargin * 4 + titleDimension[1] + statPanelMargin - bgMargin * 2);

                int x = 0;
                int y = 0;

                for(int i = 0; i < cellGroup.size(); i++) {
                    cellGroup.get(i).draw(g, x, y, finalUw, finalOffset, finalUh, nameFont, contentFont);

                    y += finalUh + cellMargin;

                    if(i == cellGroup.size() - 2)
                        y += cellMargin;
                }

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return image;
    }

    public static File drawStageStatImage(CustomStageMap map, List<CellDrawer> group, boolean isFrame, int lv, String name, String code, int index, CommonStatic.Lang.Locale lang) throws Exception {
        File f = new File("./temp/");

        if(!f.exists() && !f.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(f, "result", ".png", false);

        if(image == null) {
            return null;
        }

        Stage st = map.list.get(index);

        boolean isRanking = code.replaceAll(" - \\d+ - \\d+", "").equals("R") || code.replaceAll(" - \\d+ - \\d+", "").equals("T");

        int uh = 0;
        int uw = 0;
        int ch = 0;

        int offset = 0;

        for(int i = 0; i < group.size() - 2; i++) {
            group.get(i).initialize(nameFont, contentFont, 0);

            if(group.get(i) instanceof NormalCellDrawer)
                offset = Math.round(Math.max(((NormalCellDrawer) group.get(i)).offset, offset));

            int tempH = Math.round(Math.max(((NormalCellDrawer) group.get(i)).h, ((NormalCellDrawer) group.get(i)).ih));
            int tempUw = Math.round(((NormalCellDrawer) group.get(i)).uw);

            if(((NormalCellDrawer) group.get(i)).isSingleData()) {
                uh = Math.max(tempH, uh);

                if(tempUw > uw * 3 + CellDrawer.lineOffset * 4) {
                    uw = (tempUw - CellDrawer.lineOffset * 4) / 3;
                }
            } else {
                uh = Math.max(tempH, uh);
                uw = Math.max(tempUw, uw);
            }

            ch = Math.round(Math.max(((NormalCellDrawer) group.get(i)).ch, ch));
        }

        group.get(group.size() - 2).initialize(nameFont, contentFont, (int) ((uw * 4 + CellDrawer.lineOffset * 6) * 1.5));

        offset = Math.round(Math.max(((AbilityCellDrawer) group.get(group.size() - 2)).offset, offset));

        int ah = Math.round(((AbilityCellDrawer) group.get(group.size() - 2)).h);
        int aw = Math.round(((AbilityCellDrawer) group.get(group.size() - 2)).w);

        if(aw > uw * 4 + CellDrawer.lineOffset * 6) {
            uw = (aw - CellDrawer.lineOffset * 6) / 4;
        }

        group.getLast().initialize(nameFont, contentFont, (int) ((uw * 4 + CellDrawer.lineOffset * 6) * 1.5));

        offset = Math.round(Math.max(((AbilityCellDrawer) group.getLast()).offset, offset));

        int mh = Math.round(((AbilityCellDrawer) group.getLast()).h);
        int mw = Math.round(((AbilityCellDrawer) group.getLast()).w);

        if(mw > uw * 4 + CellDrawer.lineOffset * 6) {
            uw = (mw - CellDrawer.lineOffset * 6) / 4;
        }

        float[] nRect = titleFont.measureDimension(name);
        float[] lRect = contentFont.measureDimension(code);

        int titleHeight = Math.round(nRect[3] + nameMargin + lRect[3]);
        int titleWidth = (int) Math.max(nRect[2], lRect[2]) + bgMargin;

        if(titleWidth > uw * 4 + CellDrawer.lineOffset * 6) {
            uw = (titleWidth - CellDrawer.lineOffset * 6) / 4;
        }

        float[] stw = measureEnemySchemeWidth(st, map, isRanking, isFrame, lv, lang);
        float[] dw = measureDropTableWidth(st, map, lang, true);
        float[] sw = measureDropTableWidth(st, map, lang, false);

        if(dw != null && sw == null) {
            sw = dw;
        }

        if(sw != null && dw == null) {
            dw = sw;
        }

        int desiredStageGap;
        int desiredRewardGap;
        int desiredScoreGap;

        if(dw != null) {
            int tw = Math.round(maxAmong(dw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2, sw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2, stw[STAGE_WIDTH] - statPanelMargin * 2));

            if(tw > uw * 4 + CellDrawer.lineOffset * 6) {
                uw = (int) Math.round((tw - CellDrawer.lineOffset * 6) / 4.0);

                if(tw > dw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2) {
                    desiredRewardGap = (int) Math.round((tw - 2 * (dw[CHANCE_WIDTH] + dw[REWARD_WIDTH] + dw[AMOUNT_WIDTH] + rewardIconSize + CellDrawer.lineOffset)) / 14.0);
                } else {
                    desiredRewardGap = innerTableTextMargin;
                }

                if(tw > sw[TOTAL_WIDTH] * 2 + CellDrawer.lineOffset * 2) {
                    desiredScoreGap = (int) Math.round((tw - 2 * (sw[CHANCE_WIDTH] + sw[REWARD_WIDTH] + sw[AMOUNT_WIDTH] + rewardIconSize + CellDrawer.lineOffset)) / 14.0);
                } else {
                    desiredScoreGap = innerTableTextMargin;
                }

                if(tw > stw[STAGE_WIDTH] - statPanelMargin * 2) {
                    int tempTotalWidth = tw + statPanelMargin * 2;

                    for(int i = ENEMY; i <= BOSS; i++) {
                        tempTotalWidth = Math.round(tempTotalWidth - stw[i]);
                    }

                    desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 19.0);
                } else {
                    desiredStageGap = innerTableTextMargin;
                }
            } else {
                desiredRewardGap = (int) Math.round((uw * 2 + CellDrawer.lineOffset * 2 - dw[CHANCE_WIDTH] - dw[REWARD_WIDTH] - dw[AMOUNT_WIDTH] - rewardIconSize) / 7.0);
                desiredScoreGap = (int) Math.round((uw * 2 + CellDrawer.lineOffset * 2 - sw[CHANCE_WIDTH] - sw[REWARD_WIDTH] - sw[AMOUNT_WIDTH] - rewardIconSize) / 7.0);

                int tempTotalWidth = uw * 4 + CellDrawer.lineOffset * 6 + statPanelMargin * 2;

                for(int i = ENEMY; i <= BOSS; i++) {
                    tempTotalWidth = Math.round(tempTotalWidth - stw[i]);
                }

                desiredStageGap = (int) Math.round((tempTotalWidth - rewardIconSize) / 19.0);
            }
        } else {
            desiredScoreGap = innerTableTextMargin;
            desiredRewardGap = innerTableTextMargin;

            if(stw[STAGE_WIDTH] > uw * 4 + CellDrawer.lineOffset * 6 + statPanelMargin * 2) {
                desiredStageGap = innerTableTextMargin;
                uw = (int) Math.round((stw[STAGE_WIDTH] - CellDrawer.lineOffset * 6 - statPanelMargin * 2) / 4.0);
            } else {
                int tempTotalWidth = uw * 4 + CellDrawer.lineOffset * 6 + statPanelMargin * 2;

                for(int i = ENEMY; i <= BOSS; i++) {
                    tempTotalWidth = Math.round(tempTotalWidth - stw[i]);
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
        int totalHeight = bgMargin * 6 + titleHeight + statPanelMargin * 2 + infoHeight + cellMargin * 2 + uh - CellDrawer.textMargin - ch;
        int panelH = statPanelMargin * 2 + infoHeight + cellMargin * 2 + uh - CellDrawer.textMargin - ch;

        List<String[]> rewardData = DataToString.getRewards(st, map, lang);
        List<String[]> scoreData = DataToString.getScoreDrops(st, map, lang);

        if(rewardData != null || scoreData != null) {
            int tableH = 0;

            if(rewardData != null) {
                assert dw != null;

                tableH = Math.round(Math.max(dw[TOTAL_HEIGHT], tableH));
            }

            if(scoreData != null) {
                assert sw != null;
                tableH = Math.round(Math.max(sw[TOTAL_HEIGHT], tableH));
            }

            totalHeight += tableH;
            panelH += tableH;
        } else {
            totalHeight += ch;
            panelH += ch;
        }

        totalHeight += schemeHeight;

        CountDownLatch waiter = new CountDownLatch(1);

        int finalTotalHeight = totalHeight;
        int finalPanelH = panelH;
        
        int finalUw = uw;
        int finalUh = uh;
        
        int finalOffset = offset;
        int finalInfoHeight = infoHeight;
        int finalCh = ch;
        
        float[] finalDw = dw;
        float[] finalSw = sw;
        
        StaticStore.renderManager.createRenderer(finW, totalHeight, f, connector -> {
            connector.queue(g -> {
                g.setColor(50, 53, 59);

                g.fillRect(0, 0, finW, finalTotalHeight);

                g.setColor(24, 25, 28);

                g.fillRoundRect(0, -cornerRadius, finW, cornerRadius + bgMargin * 8 + titleHeight, cornerRadius, cornerRadius);

                g.setColor(64, 68, 75);

                g.fillRoundRect(bgMargin, bgMargin * 4 + titleHeight, infoWidth + statPanelMargin * 2, finalPanelH, cornerRadius, cornerRadius);

                drawStageTitleImage(g, name, code);

                int x = bgMargin + statPanelMargin;
                int y = bgMargin * 4 + titleHeight + statPanelMargin;

                for(int i = 0; i < group.size(); i++) {
                    group.get(i).draw(g, x, y, finalUw, finalOffset, finalUh, nameFont, contentFont);

                    if(i < group.size() - 2)
                        y += finalUh + cellMargin;
                    else
                        y += ah + cellMargin;

                    if(i == group.size() - 3 || i == group.size() - 2)
                        y += cellMargin;
                }

                g.setColor(191, 191, 191);

                g.setFontModel(nameFont);

                g.drawText(LangID.getStringByID("data.stage.reward.drop.reward", lang), bgMargin + statPanelMargin, bgMargin * 4 + titleHeight + statPanelMargin + finalInfoHeight + cellMargin + finalOffset / 2f, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
                g.drawText(LangID.getStringByID("data.stage.reward.drop.score", lang), bgMargin + statPanelMargin + finalUw * 2 + CellDrawer.lineOffset * 4, bgMargin * 4 + titleHeight + statPanelMargin + finalInfoHeight + cellMargin + finalOffset / 2f, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                int stack = bgMargin * 4 + titleHeight + statPanelMargin + finalInfoHeight + cellMargin * 2 + finalUh - finalCh - CellDrawer.textMargin;

                if(rewardData != null) {
                    try {
                        drawRewardTable(g, bgMargin + statPanelMargin, stack, st, map, finalDw, desiredRewardGap, lang, true);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawStageStatImage - Failed to draw reward table");

                        return null;
                    }
                } else {
                    g.setFontModel(contentFont);
                    g.setColor(239, 239, 239);

                    g.drawText(LangID.getStringByID("data.none", lang), bgMargin + statPanelMargin, stack + finalOffset / 2f, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
                }

                if(scoreData != null) {
                    try {
                        drawRewardTable(g, bgMargin + statPanelMargin + finalUw * 2 + CellDrawer.lineOffset * 4, stack, st, map, finalSw, desiredScoreGap, lang, false);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawStageStatImage - Failed to draw reward table");
                        
                        return null;
                    }
                } else {
                    g.setFontModel(contentFont);
                    g.setColor(239, 239, 239);

                    g.drawText(LangID.getStringByID("data.none", lang), bgMargin + statPanelMargin + finalUw * 2 + CellDrawer.lineOffset * 4, stack + finalOffset / 2f, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
                }

                try {
                    drawEnemySchemeTable(g, bgMargin * 4 + titleHeight  + finalPanelH + bgMargin, st, map, stw, desiredStageGap, isRanking, isFrame, lv, lang);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/ImageDrawing::drawStageStatImage - Failed to draw enemy scheme table");
                }

                return null;
            });
            
            return null;
        }, progress -> image, () -> {
            waiter.countDown();
            
            return null;
        });
        
        waiter.await();

        return image;
    }

    public static File drawTalentImage(String name, String type, CustomTalent talent, CommonStatic.Lang.Locale lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            return null;
        }

        File image = StaticStore.generateTempFile(temp, "talent", ".png", false);

        if(image == null || !image.exists())
            return null;

        float[] nameRect = titleFont.measureDimension(name);
        float[] typeRect = typeFont.measureDimension(type);

        int titleHeight = Math.round(nameRect[3] + nameMargin + typeRect[3] + typeUpDownMargin * 2);

        int icw = (int) Math.round(titleHeight * 1.0 / talent.icon.getHeight() * talent.icon.getWidth());

        int titleWidth = icw + nameMargin + Math.round(Math.max(nameRect[2], typeRect[2] + typeLeftRightMargin * 2));

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

            float[] titleRect = levelFont.measureDimension(data.title);

            maxTitleHeight = Math.round(Math.max(maxTitleHeight, titleRect[3]));
            maxTitleWidth = Math.round(Math.max(maxTitleWidth, titleRect[2]));

            for(int j = 0; j < data.description.length; j++) {
                float[] descRect = contentFont.measureDimension(data.description[j]);

                maxDescLineHeight = Math.round(Math.max(maxDescLineHeight, descRect[3]));
                maxDescLineWidth = Math.round(Math.max(maxDescLineWidth, descRect[2]));
            }

            if(data.cost.size() == 1) {
                String cost = String.format(LangID.getStringByID("talentAnalyzer.npCost.single", lang), data.cost.getFirst());

                float[] costRect = contentFont.measureDimension(cost);

                maxCostWidth = Math.round(Math.max(maxCostWidth, costRect[2]));
            } else {
                String costTitle = LangID.getStringByID("talentAnalyzer.npCost.level", lang);
                StringBuilder cost = new StringBuilder("[");
                int costSummary = 0;

                for(int j = 0; j < data.cost.size(); j++) {
                    costSummary += data.cost.get(j);

                    cost.append(data.cost.get(j));

                    if(j < data.cost.size() - 1)
                        cost.append(", ");
                }

                cost.append("] => ").append(costSummary);

                float costTitleWidth = contentFont.textWidth(costTitle);
                float costWidth = nameFont.textWidth(cost.toString());

                maxCostWidth = Math.round(Math.max(maxCostWidth, Math.max(costTitleWidth, talentCostTableGap * 2 + costWidth)));
            }
        }

        int talentIconDimension = (int) Math.round(maxTitleHeight * 1.5);

        String totalCostText = LangID.getStringByID("data.talent.npCost.total", lang).replace("_", String.valueOf(totalCost));
        float[] totalRect = nameFont.measureDimension(totalCostText);

        int totalCostWidth = Math.round(totalRect[2]);
        int totalCostHeight = Math.round(totalRect[3]);

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
                String cost = String.format(LangID.getStringByID("talentAnalyzer.npCost.single", lang), data.cost.getFirst());

                float[] costRect = contentFont.measureDimension(cost);

                panelHeight += Math.round(costRect[3]);
            } else {
                String costTitle = LangID.getStringByID("talentAnalyzer.npCost.level", lang);
                StringBuilder cost = new StringBuilder("[");
                int costSummary = 0;

                for(int j = 0; j < data.cost.size(); j++) {
                    costSummary += data.cost.get(j);

                    cost.append(data.cost.get(j));

                    if(j < data.cost.size() - 1)
                        cost.append(", ");
                }

                cost.append("] => ").append(costSummary);

                float[] costTitleRect = contentFont.measureDimension(costTitle);
                float[] costRect = nameFont.measureDimension(cost.toString());

                panelHeight = Math.round(panelHeight + Math.round(costTitleRect[3]) + talentTableGap + talentCostTableGap * 2 + costRect[3]);
            }

            if(i < talent.talents.size() - 1)
                panelHeight += talentGap;
        }

        panelWidth = Math.max(totalCostWidth, Math.max(panelWidth, titleWidth + statPanelMargin));

        int totalHeight = bgMargin * 2 + titleHeight + bgMargin * 2 + panelHeight + bgMargin + Math.max(totalCostGap, totalCostHeight);
        int totalWidth = bgMargin * 2 + panelWidth;

        int finalPanelWidth = panelWidth;
        int finalPanelHeight = panelHeight;
        
        CountDownLatch waiter = new CountDownLatch(1);

        int finalMaxDescLineHeight = maxDescLineHeight;

        StaticStore.renderManager.createRenderer(totalWidth, totalHeight, temp, connector -> {
            connector.queue(g -> {
                g.setColor(51, 53, 60, 255);
                g.fillRect( 0, 0, totalWidth, totalHeight);

                g.setColor(24, 25, 28, 255);
                g.fillRoundRect(0, -cornerRadius / 2f, totalWidth, cornerRadius + bgMargin * 8 + titleHeight, cornerRadius, cornerRadius);

                g.setColor(64, 68, 75, 255);
                g.fillRoundRect(bgMargin, bgMargin * 4 + titleHeight, finalPanelWidth, finalPanelHeight, cornerRadius, cornerRadius);

                g.drawImage(talent.icon, bgMargin, bgMargin * 2, icw, titleHeight);

                g.setFontModel(titleFont);
                g.setColor(238, 238, 238, 255);
                g.drawText(name, bgMargin + icw + nameMargin, bgMargin * 2, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                g.setColor(88, 101, 242, 255);
                g.fillRoundRect(bgMargin + icw + nameMargin, Math.round(bgMargin * 2 + nameRect[3] + nameMargin), Math.round(typeLeftRightMargin * 2 + typeRect[2]), Math.round(typeUpDownMargin * 2 + typeRect[3]), typeCornerRadius, typeCornerRadius);

                g.setFontModel(typeFont);
                g.setColor(238, 238, 238, 255);
                g.drawText(type, bgMargin + icw + nameMargin + typeLeftRightMargin, Math.round(bgMargin * 2 + nameRect[3] + nameMargin + typeUpDownMargin), GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

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

                    g.setFontModel(levelFont);
                    g.setColor(238, 238, 238, 255);

                    g.drawText(data.title, x + talentTitleOffset, Math.round(y + talentIconDimension / 2f), GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                    y += talentIconDimension;

                    if(data.hasDescription()) {
                        y += talentIconGap;

                        g.setFontModel(contentFont);
                        g.setColor(191, 191, 191, 255);

                        for(int j = 0; j < data.description.length; j++) {
                            if(!data.description[j].isBlank()) {

                                g.drawText(data.description[j], x, y, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
                            }

                            y += finalMaxDescLineHeight;

                            if(j < data.description.length - 1)
                                y += lineSpace;
                        }
                    }

                    y += talentCostGap;

                    if(data.cost.size() == 1) {
                        String cost = String.format(LangID.getStringByID("talentAnalyzer.npCost.single", lang), data.cost.getFirst());

                        g.setFontModel(contentFont);
                        g.setColor(238, 238, 238, 255);

                        float[] costRect = contentFont.measureDimension(cost);

                        g.drawText(cost, x, y, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                        y += Math.round(costRect[3]);
                    } else {
                        String costTitle = LangID.getStringByID("talentAnalyzer.npCost.level", lang);
                        StringBuilder cost = new StringBuilder("[");
                        int costSummary = 0;

                        for(int j = 0; j < data.cost.size(); j++) {
                            costSummary += data.cost.get(j);

                            cost.append(data.cost.get(j));

                            if(j < data.cost.size() - 1)
                                cost.append(", ");
                        }

                        cost.append("] => ").append(costSummary);

                        float[] costTitleRect = contentFont.measureDimension(costTitle);
                        float[] costRect = nameFont.measureDimension(cost.toString());

                        g.setFontModel(contentFont);
                        g.setColor(238, 238, 238, 255);
                        g.drawText(costTitle, x, y, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                        y += Math.round(talentTableGap + costTitleRect[3]);

                        g.setColor(51, 54, 60);
                        g.fillRoundRect(x, y, Math.round(talentCostTableGap * 2 + costRect[2]), Math.round(talentCostTableGap * 2 + costRect[3]), innerTableCornerRadius, innerTableCornerRadius);

                        g.setFontModel(nameFont);
                        g.setColor(238, 238, 238, 255);

                        g.drawText(cost.toString(), x + talentCostTableGap, y + talentCostTableGap, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                        y += Math.round(talentCostTableGap * 2 + costRect[3]);
                    }

                    if(i < talent.talents.size() - 1) {
                        y = Math.round(y + talentGap / 2f);

                        g.setColor(191, 191, 191, 255);
                        g.setStroke(CellDrawer.lineStroke, GLGraphics.LineEndMode.ROUND);
                        g.drawLine(x, y, x + finalPanelWidth - statPanelMargin * 2, y);

                        y = Math.round(y + talentGap / 2f);
                    }
                }

                g.setFontModel(nameFont);
                g.setColor(191, 191, 191, 255);
                g.drawText(totalCostText, totalWidth - bgMargin - totalCostWidth, totalHeight - bgMargin - totalCostHeight, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
            
                return null;
            });
            
            return null;
        }, progress -> image, () -> {
            waiter.countDown();
            
            return null;
        });
        
        waiter.await();

        return image;
    }

    public static File drawComboImage(File folder, CustomCombo combo) throws Exception {
        File image = StaticStore.generateTempFile(folder, "combo", ".png", false);

        if(image == null || !image.exists())
            return null;

        float[] titleRect = titleFont.measureDimension(combo.getTitle());
        float[] typeRect = typeFont.measureDimension(combo.getType());
        float[] levelRect = typeFont.measureDimension(combo.getLevel());

        int levelBoxDimension = Math.round(Math.max(levelRect[2], levelRect[3]) + comboTypeInnerGap * 2);

        int typeBoxHeight = Math.round(Math.max(typeRect[3], levelBoxDimension));
        int typeBoxWidth = Math.round(typeRect[2] + comboTypeGap + levelBoxDimension);

        int titleHeight = Math.round(titleRect[3] + comboTitleGap + typeBoxHeight);
        int titleWidth = Math.round(Math.max(titleRect[2], typeBoxWidth));

        int maxIconTableWidth = Math.round(comboIconLeftRightGap * 2 + combo.getIcons().getFirst().getWidth() * comboIconScaleFactor);
        int maxUnitNameHeight = 0;

        for(int i = 0; i < combo.getIcons().size(); i++) {
            float[] unitNameRect = nameFont.measureDimension(combo.getNames().get(i));

            maxUnitNameHeight = Math.round(Math.max(maxUnitNameHeight, unitNameRect[3]));
            maxIconTableWidth = Math.round(Math.max(maxIconTableWidth, comboIconLeftRightGap * 2 + unitNameRect[2]));
        }

        int maxIconTableHeight = Math.round(comboIconUpDownGap * 2 + combo.getIcons().getFirst().getHeight() * comboIconScaleFactor + comboIconNameGap + maxUnitNameHeight);

        float[] descRect = contentFont.measureDimension(combo.getDescription());

        maxIconTableWidth = (int) Math.round(Math.max(maxIconTableWidth, (descRect[2] - comboIconGap * (combo.getIcons().size() - 1)) / (1.0 * combo.getIcons().size())));

        int panelHeight = Math.round(statPanelMargin * 2 + maxIconTableHeight + comboContentGap + descRect[3]);
        int panelWidth = Math.round(statPanelMargin * 2 + Math.max(maxIconTableWidth * combo.getIcons().size() + comboIconGap * (combo.getIcons().size() - 1), descRect[2]));

        if(titleWidth > panelWidth) {
            panelWidth = titleWidth + bgMargin * 2 + statPanelMargin;

            maxIconTableWidth = (int) Math.round((panelWidth - statPanelMargin * 2 - comboIconGap * (combo.getIcons().size() - 1)) / (1.0 * combo.getIcons().size()));
        }

        int totalHeight = bgMargin * 5 + titleHeight + panelHeight;
        int totalWidth = Math.max(bgMargin * 4 + titleWidth, bgMargin * 2 + panelWidth);

        CountDownLatch waiter = new CountDownLatch(1);

        int finalMaxIconTableWidth = maxIconTableWidth;

        int finalPanelWidth = panelWidth;
        StaticStore.renderManager.createRenderer(totalWidth, totalHeight, folder, connector -> {
            connector.queue(g -> {
                g.setColor(51, 53, 60, 255);
                g.fillRect( 0, 0, totalWidth, totalHeight);

                g.setColor(24, 25, 28, 255);
                g.fillRoundRect(0, -cornerRadius / 2f, totalWidth, cornerRadius + bgMargin * 8 + titleHeight, cornerRadius, cornerRadius);

                g.setColor(64, 68, 75, 255);
                g.fillRoundRect(bgMargin, bgMargin * 4 + titleHeight, finalPanelWidth, panelHeight, cornerRadius, cornerRadius);

                g.setFontModel(titleFont);
                g.setColor(238, 238, 238, 255);
                g.drawText(combo.getTitle(), bgMargin * 2, bgMargin * 2, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                g.setFontModel(typeFont);
                g.drawText(combo.getType(), bgMargin * 2, (int) Math.round(bgMargin * 2 + titleRect[3] + comboTitleGap + (Math.max(typeRect[3], levelBoxDimension)) / 2.0), GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

                g.setColor(88, 101, 242, 255);
                g.fillRoundRect(Math.round(bgMargin * 2 + typeRect[2] + comboTypeGap), (int) Math.round(bgMargin * 2 + titleRect[3] + comboTitleGap + (Math.max(typeRect[3], levelBoxDimension) - levelBoxDimension) / 2.0), levelBoxDimension, levelBoxDimension, comboTypeRadius, comboTypeRadius);

                g.setColor(238, 238, 238, 255);
                g.drawText(combo.getLevel(), Math.round(bgMargin * 2 + typeRect[2] + comboTypeGap + (Math.max(typeRect[3], levelBoxDimension)) / 2f), Math.round(bgMargin * 2 + titleRect[3] + comboTitleGap + (Math.max(typeRect[3], levelBoxDimension)) / 2f), GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                int x = bgMargin + statPanelMargin;
                int y = bgMargin * 4 + titleHeight + statPanelMargin;

                g.setFontModel(nameFont);

                for(int i = 0; i < combo.getIcons().size(); i++) {
                    g.setColor(51, 53, 60, 255);
                    g.fillRoundRect(x, y, finalMaxIconTableWidth, maxIconTableHeight, comboIconTableRadius, comboIconTableRadius);

                    g.drawImage(combo.getIcons().get(i), Math.round(x + (finalMaxIconTableWidth - combo.getIcons().get(i).getWidth() * comboIconScaleFactor) / 2.0), y + comboIconUpDownGap, Math.round(combo.getIcons().get(i).getWidth() * comboIconScaleFactor), Math.round(combo.getIcons().get(i).getHeight() * comboIconScaleFactor));

                    g.setColor(191, 191, 191, 255);
                    g.drawText(combo.getNames().get(i), (int) Math.round(x + (finalMaxIconTableWidth) / 2.0), Math.round(y + comboIconUpDownGap + combo.getIcons().get(i).getHeight() * comboIconScaleFactor + comboIconNameGap), GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);

                    x += finalMaxIconTableWidth + comboIconGap;
                }

                x = bgMargin + statPanelMargin;
                y += maxIconTableHeight + comboContentGap;

                g.setFontModel(contentFont);
                g.setColor(238, 238, 238, 255);
                g.drawText(combo.getDescription(), x, y, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return image;
    }

    public static Object[] plotGraph(BigDecimal[][] coordinates, BigDecimal[] xRange, BigDecimal[] yRange, boolean keepRatio, CommonStatic.Lang.Locale lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        BigDecimal xw = xRange[1].subtract(xRange[0]);
        BigDecimal yw = yRange[1].subtract(yRange[0]);

        if(yw.divide(xw, Equation.context).compareTo(BigDecimal.valueOf(10)) > 0 || yw.compareTo(BigDecimal.ZERO) == 0)
            keepRatio = true;

        AtomicReference<String> text = new AtomicReference<>(" ");

        boolean finalKeepRatio = keepRatio;

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(plotWidthHeight, plotWidthHeight, temp, connector -> {
            connector.queue(g -> {
                BigDecimal xWidth = xRange[1].subtract(xRange[0]);
                BigDecimal yWidth = yRange[1].subtract(yRange[0]);

                g.setColor(51, 53, 60, 255);
                g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

                if(finalKeepRatio) {
                    BigDecimal center = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

                    yRange[0] = center.subtract(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
                    yRange[1] = center.add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));

                    yWidth = yRange[1].subtract(yRange[0]);
                }

                BigDecimal centerX = xRange[0].add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
                BigDecimal centerY = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

                int xLine = convertCoordinateToPixel(plotWidthHeight, BigDecimal.ZERO, xWidth, centerX, true);
                int yLine = convertCoordinateToPixel(plotWidthHeight, BigDecimal.ZERO, yWidth, centerY, false);

                g.setColor(238, 238, 238, 255);
                g.setStroke(axisStroke, GLGraphics.LineEndMode.VERTICAL);

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
                        int xPos = convertCoordinateToPixel(plotWidthHeight, xPosition, xWidth, centerX, true);

                        g.setColor(238, 238, 238, 255);
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                        g.drawLine(xPos, (int) Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0), xPos, (int) Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0));

                        g.setColor(238, 238, 238, 64);
                        g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                        g.setFontModel(plotFont);
                        g.setColor(238, 238, 238, 255);

                        if(positive) {
                            g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);
                        } else {
                            g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.BOTTOM);
                        }
                    }

                    xPosition = xPosition.add(xSegment);
                }

                while(yPosition.compareTo(yRange[1]) <= 0) {
                    if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                        int yPos = convertCoordinateToPixel(plotWidthHeight, yPosition, yWidth, centerY, false);

                        g.setColor(238, 238, 238, 255);
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                        g.drawLine((int) Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0), yPos, (int) Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0), yPos);

                        g.setColor(238, 238, 238, 64);
                        g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                        g.setFontModel(plotFont);
                        g.setColor(238, 238, 238, 255);

                        if(positive) {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);
                        } else {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.LEFT, GLGraphics.VerticalSnap.MIDDLE);
                        }
                    }

                    yPosition = yPosition.add(ySegment);
                }

                g.setColor(118, 224, 85, 255);
                g.setStroke(plotStroke, GLGraphics.LineEndMode.ROUND);

                for(int i = 0; i < coordinates.length - 1; i++) {
                    if(coordinates[i][1] == null || coordinates[i + 1][1] == null || coordinates[i][0] == null || coordinates[i + 1][0] == null)
                        continue;

                    int x0 = convertCoordinateToPixel(plotWidthHeight, coordinates[i][0], xWidth, centerX, true);
                    int x1 = convertCoordinateToPixel(plotWidthHeight, coordinates[i + 1][0], xWidth, centerX, true);

                    int y0 = convertCoordinateToPixel(plotWidthHeight, coordinates[i][1], yWidth, centerY, false);
                    int y1 = convertCoordinateToPixel(plotWidthHeight, coordinates[i + 1][1], yWidth, centerY, false);

                    double angle = Math.abs(Math.toDegrees(Math.atan2(coordinates[i + 1][1].subtract(coordinates[i][1]).doubleValue(), coordinates[i + 1][0].subtract(coordinates[i][0]).doubleValue())));
                    int v = (int) angle / 90;

                    angle = angle - 90 * v;

                    if (angle > angleLimit) {
                        continue;
                    }

                    g.drawLine(x0, y0, x1, y1);
                }

                text.set(String.format(
                        LangID.getStringByID("plot.success", lang),
                        Equation.formatNumber(centerX.subtract(xw.divide(BigDecimal.valueOf(2), Equation.context))),
                        Equation.formatNumber(centerX.add(xw.divide(BigDecimal.valueOf(2), Equation.context))),
                        Equation.formatNumber(centerY.subtract(yw.divide(BigDecimal.valueOf(2), Equation.context))),
                        Equation.formatNumber(centerY.add(yw.divide(BigDecimal.valueOf(2), Equation.context)))
                ));

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return new Object[] { image, text.get() };
    }

    public static Object[] plotTGraph(BigDecimal[][] coordinates, BigDecimal[] xRange, BigDecimal[] yRange, BigDecimal[] tRange, boolean keepRatio, CommonStatic.Lang.Locale lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        BigDecimal xw = xRange[1].subtract(xRange[0]);
        BigDecimal yw = yRange[1].subtract(yRange[0]);

        if(yw.divide(xw, Equation.context).compareTo(BigDecimal.valueOf(10)) > 0 || yw.compareTo(BigDecimal.ZERO) == 0)
            keepRatio = true;

        AtomicReference<String> text = new AtomicReference<>("");

        boolean finalKeepRatio = keepRatio;

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(plotWidthHeight, plotWidthHeight, temp, connector -> {
            connector.queue(g -> {
                BigDecimal xWidth = xRange[1].subtract(xRange[0]);
                BigDecimal yWidth = yRange[1].subtract(yRange[0]);

                g.setColor(51, 53, 60, 255);
                g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

                if(finalKeepRatio) {
                    BigDecimal center = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

                    yRange[0] = center.subtract(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
                    yRange[1] = center.add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));

                    yWidth = yRange[1].subtract(yRange[0]);
                }

                BigDecimal centerX = xRange[0].add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
                BigDecimal centerY = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

                int xLine = convertCoordinateToPixel(plotWidthHeight, BigDecimal.ZERO, xWidth, centerX, true);
                int yLine = convertCoordinateToPixel(plotWidthHeight, BigDecimal.ZERO, yWidth, centerY, false);

                g.setColor(238, 238, 238, 255);
                g.setStroke(axisStroke, GLGraphics.LineEndMode.VERTICAL);

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
                        int xPos = convertCoordinateToPixel(plotWidthHeight, xPosition, xWidth, centerX, true);

                        g.setColor(238, 238, 238, 255);
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                        g.drawLine(xPos, (int) Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0), xPos, (int) Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0));

                        g.setColor(238, 238, 238, 64);
                        g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                        g.setFontModel(plotFont);
                        g.setColor(238, 238, 238, 255);

                        if(positive) {
                            g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);
                        } else {
                            g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.BOTTOM);
                        }
                    }

                    xPosition = xPosition.add(xSegment);
                }

                while(yPosition.compareTo(yRange[1]) <= 0) {
                    if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                        int yPos = convertCoordinateToPixel(plotWidthHeight, yPosition, yWidth, centerY, false);

                        g.setColor(238, 238, 238, 255);
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                        g.drawLine((int) Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0), yPos, (int) Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0), yPos);

                        g.setColor(238, 238, 238, 64);
                        g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                        g.setFontModel(plotFont);
                        g.setColor(238, 238, 238, 255);

                        if(positive) {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);
                        } else {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.LEFT, GLGraphics.VerticalSnap.MIDDLE);
                        }
                    }

                    yPosition = yPosition.add(ySegment);
                }

                g.setColor(118, 224, 85, 255);
                g.setStroke(plotStroke, GLGraphics.LineEndMode.ROUND);

                for(int i = 0; i < coordinates.length - 1; i++) {
                    if(coordinates[i][1] == null || coordinates[i + 1][1] == null || coordinates[i][0] == null || coordinates[i + 1][0] == null)
                        continue;

                    int x0 = convertCoordinateToPixel(plotWidthHeight, coordinates[i][0], xWidth, centerX, true);
                    int x1 = convertCoordinateToPixel(plotWidthHeight, coordinates[i + 1][0], xWidth, centerX, true);

                    int y0 = convertCoordinateToPixel(plotWidthHeight, coordinates[i][1], yWidth, centerY, false);
                    int y1 = convertCoordinateToPixel(plotWidthHeight, coordinates[i + 1][1], yWidth, centerY, false);

                    double angle = Math.abs(Math.toDegrees(Math.atan2(coordinates[i + 1][1].subtract(coordinates[i][1]).doubleValue(), coordinates[i + 1][0].subtract(coordinates[i][0]).doubleValue())));
                    int v = (int) angle / 90;

                    angle = angle - 90 * v;

                    if (angle > angleLimit) {
                        continue;
                    }

                    g.drawLine(x0, y0, x1, y1);
                }

                text.set(String.format(
                        LangID.getStringByID("tPlot.success", lang),
                        Equation.formatNumber(tRange[0]),
                        Equation.formatNumber(tRange[1]),
                        Equation.formatNumber(centerX.subtract(xWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                        Equation.formatNumber(centerX.add(xWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                        Equation.formatNumber(centerY.subtract(yWidth.divide(BigDecimal.valueOf(2), Equation.context))),
                        Equation.formatNumber(centerY.add(yWidth.divide(BigDecimal.valueOf(2), Equation.context)))
                ));

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return new Object[] { image, text.get() };
    }

    public static Object[] plotXYGraph(Formula formula, BigDecimal[] xRange, BigDecimal[] yRange, boolean keepRatio, CommonStatic.Lang.Locale lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        double xw = xRange[1].doubleValue() - xRange[0].doubleValue();
        double yw = yRange[1].doubleValue() - yRange[0].doubleValue();

        if(yw / xw > 10 || yw == 0)
            keepRatio = true;

        AtomicReference<String> text = new AtomicReference<>("");

        CountDownLatch waiter = new CountDownLatch(1);

        boolean finalKeepRatio = keepRatio;

        StaticStore.renderManager.createRenderer(plotWidthHeight, plotWidthHeight, temp, connector -> {
            connector.queue(g -> {
                double xWidth = xRange[1].doubleValue() - xRange[0].doubleValue();
                double yWidth = yRange[1].doubleValue() - yRange[0].doubleValue();

                g.setColor(51, 53, 60, 255);
                g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

                if(finalKeepRatio) {
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
                g.setStroke(axisStroke, GLGraphics.LineEndMode.VERTICAL);

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
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                        g.drawLine(xPos, (int) Math.round(yLine - plotWidthHeight * indicatorRatio / 2.0), xPos, (int) Math.round(yLine + plotWidthHeight * indicatorRatio / 2.0));

                        g.setColor(238, 238, 238, 64);
                        g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                        g.setFontModel(plotFont);
                        g.setColor(238, 238, 238, 255);

                        if(positive) {
                            g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);
                        } else {
                            g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.BOTTOM);
                        }
                    }

                    xPosition = xPosition.add(xSegment);
                }

                while(yPosition.compareTo(yRange[1]) <= 0) {
                    if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                        int yPos = convertCoordinateToPixel(yPosition.doubleValue(), yWidth, centerY, false);

                        g.setColor(238, 238, 238, 255);
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                        g.drawLine((int) Math.round(xLine - plotWidthHeight * indicatorRatio / 2.0), yPos, (int) Math.round(xLine + plotWidthHeight * indicatorRatio / 2.0), yPos);

                        g.setColor(238, 238, 238, 64);
                        g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                        g.setFontModel(plotFont);
                        g.setColor(238, 238, 238, 255);

                        if(positive) {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);
                        } else {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.LEFT, GLGraphics.VerticalSnap.MIDDLE);
                        }
                    }

                    yPosition = yPosition.add(ySegment);
                }

                g.setColor(118, 224, 85, 255);
                g.setStroke(plotStroke, GLGraphics.LineEndMode.ROUND);

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
                            double ym = convertPixelToCoordinate((y + 0.5) * segment, yWidth, centerY, false);
                            double vm = substituted.substitute(ym);

                            if (!Equation.error.isEmpty() || substituted.element.isAborted()) {
                                Equation.error.clear();

                                continue;
                            }

                            double slope0 = Math.toDegrees(Math.atan((vm - v0) / (ym - y0)));
                            double slope1 = Math.toDegrees(Math.atan((v1 - vm) / (y1 - ym)));

                            if (Math.abs(slope0) >= multivariableAngleLimit || Math.abs(slope1) >= multivariableAngleLimit || slope0 * slope1 < 0)
                                continue;

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
                            double xm = convertPixelToCoordinate((x + 0.5) * segment, xWidth, centerX, true);
                            double vm = substituted.substitute(xm);

                            if (!Equation.error.isEmpty() || substituted.element.isAborted()) {
                                Equation.error.clear();

                                continue;
                            }

                            double slope0 = Math.toDegrees(Math.atan((vm - v0) / (xm - x0)));
                            double slope1 = Math.toDegrees(Math.atan((v1 - vm) / (x1 - xm)));

                            if (Math.abs(slope0) >= multivariableAngleLimit || Math.abs(slope1) >= multivariableAngleLimit || slope0 * slope1 < 0) {
                                continue;
                            }

                            g.fillOval(convertCoordinateToPixel(-v1 * (x1 - x0) / (v1 - v0) + x1, xWidth, centerX, true), (int) Math.round(y * segment), 3, 3);
                        }
                    }
                }

                text.set(String.format(
                        LangID.getStringByID("plot.success", lang),
                        DataToString.df.format(centerX - xWidth / 2.0),
                        DataToString.df.format(centerX + xWidth / 2.0),
                        DataToString.df.format(centerY - yWidth / 2.0),
                        DataToString.df.format(centerY + yWidth / 2.0)
                ));

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return new Object[] { image, text.get() };
    }

    public static Object[] plotRThetaGraph(Formula formula, BigDecimal[] xRange, BigDecimal[] yRange, double[] rRange, double[] tRange, CommonStatic.Lang.Locale lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        double xRangeWidth = xRange[1].doubleValue() - xRange[0].doubleValue();
        double yRangeWidth = yRange[1].doubleValue() - yRange[0].doubleValue();

        if(yRangeWidth != xRangeWidth) {
            BigDecimal center = yRange[0].add(yRange[1]).divide(BigDecimal.valueOf(2), Equation.context);

            yRange[0] = center.subtract(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(2), Equation.context));
            yRange[1] = center.add(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(2), Equation.context));
        }

        CountDownLatch waiter = new CountDownLatch(1);

        AtomicReference<String> text = new AtomicReference<>("");

        StaticStore.renderManager.createRenderer(plotWidthHeight, plotWidthHeight, temp, connector -> {
            connector.queue(g -> {
                double xWidth = xRange[1].doubleValue() - xRange[0].doubleValue();
                double yWidth = yRange[1].doubleValue() - yRange[0].doubleValue();

                g.setColor(51, 53, 60, 255);
                g.fillRect(0, 0, plotWidthHeight, plotWidthHeight);

                double centerX = xRange[0].doubleValue() + xWidth / 2.0;
                double centerY = yRange[0].doubleValue() + yWidth / 2.0;

                int xLine = convertCoordinateToPixel(0, xWidth, centerX, true);
                int yLine = convertCoordinateToPixel(0, yWidth, centerY, false);

                g.setColor(238, 238, 238, 255);
                g.setStroke(axisStroke, GLGraphics.LineEndMode.VERTICAL);

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
                        int radius = Math.abs(xPos - zeroX);

                        g.setColor(238, 238, 238, 64);
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                        g.drawOval(zeroX - radius, zeroY - radius, radius, radius);

                        g.setStroke(indicatorStroke / 4f, GLGraphics.LineEndMode.ROUND);

                        for(int i = 1; i < 5; i++) {
                            int subXPos = convertCoordinateToPixel(xPosition.doubleValue() + xSegment.doubleValue() / 5.0 * i, xWidth, centerX, true);
                            int subRadius = Math.abs(subXPos - zeroX);

                            g.drawOval(zeroX - subRadius, zeroY - subRadius, subRadius, subRadius);
                        }

                        if(xPosition.compareTo(xRange[1]) <= 0) {
                            g.setColor(238, 238, 238, 255);
                            g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                            g.setFontModel(plotFont);
                            g.setColor(238, 238, 238, 255);

                            if(positive) {
                                g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);
                            } else {
                                g.drawText(Equation.simpleNumber(xPosition), xPos, (int) textPosition, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.BOTTOM);
                            }
                        }
                    }

                    xPosition = xPosition.add(xSegment);
                }

                while(yPosition.compareTo(yRange[1]) <= 0) {
                    if(yPosition.compareTo(BigDecimal.ZERO) != 0) {
                        int yPos = convertCoordinateToPixel(yPosition.doubleValue(), yWidth, centerY, false);

                        g.setColor(238, 238, 238, 255);
                        g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

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

                        g.setFontModel(plotFont);
                        g.setColor(238, 238, 238, 255);

                        if(positive) {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);
                        } else {
                            g.drawText(Equation.simpleNumber(yPosition), (int) textPosition, yPos, GLGraphics.HorizontalSnap.LEFT, GLGraphics.VerticalSnap.MIDDLE);
                        }
                    }

                    yPosition = yPosition.add(ySegment);
                }

                g.setColor(238, 238, 238, 64);
                g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                for(int i = 1; i < 12; i++) {
                    if(i == 6)
                        continue;

                    double slope = Math.tan(Math.PI / 12.0 * i);

                    int yMin = convertCoordinateToPixel(xRange[0].doubleValue() * slope, yWidth, centerY, false);
                    int yMax = convertCoordinateToPixel(xRange[1].doubleValue() * slope, yWidth, centerY, false);

                    g.drawLine(0, yMin,plotWidthHeight, yMax);
                }

                g.setColor(118, 224, 85, 255);
                g.setStroke(plotStroke, GLGraphics.LineEndMode.ROUND);

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

                text.set(String.format(
                        LangID.getStringByID("rPlot.success", lang),
                        DataToString.df.format(tRange[0]),
                        DataToString.df.format(tRange[1]),
                        DataToString.df.format(rRange[0]),
                        DataToString.df.format(rRange[1]),
                        DataToString.df.format(centerX - xWidth / 2.0),
                        DataToString.df.format(centerX + xWidth / 2.0),
                        DataToString.df.format(centerY - yWidth / 2.0),
                        DataToString.df.format(centerY + yWidth / 2.0)
                ));

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return new Object[] { image, text.get() };
    }

    public static File plotDPSGraph(BigDecimal[][] coordinates, @Nullable BigDecimal[][] withTreasure, BigDecimal[] xRange, BigDecimal[] yRange, CommonStatic.Lang.Locale lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return null;

        File image = StaticStore.generateTempFile(temp, "plot", ".png", false);

        if(image == null)
            return null;

        BigDecimal xWidth = xRange[1].subtract(xRange[0]);
        BigDecimal yWidth = yRange[1].subtract(yRange[0]);

        BigDecimal centerX = xRange[0].add(xWidth.divide(BigDecimal.valueOf(2), Equation.context));
        BigDecimal centerY = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context));

        BigDecimal xSegment = xWidth.divide(BigDecimal.TEN, Equation.context);

        int xScale = (int) - (Math.round(Math.log10(xSegment.doubleValue())) + 0.5 - 0.5 * Math.signum(xSegment.doubleValue()));

        if (xScale >= 0) {
            xSegment = xSegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            xSegment = xSegment.divide(BigDecimal.TEN.pow(-xScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-xScale));
        }

        if (xSegment.compareTo(BigDecimal.ZERO) == 0) {
            xSegment = BigDecimal.ONE;
        }

        BigDecimal ySegment = yWidth.divide(BigDecimal.TEN, Equation.context);

        int yScale = (int) - (Math.round(Math.log10(ySegment.doubleValue())) + 0.5 - 0.5 * Math.signum(ySegment.doubleValue()));

        if (yScale >= 0) {
            ySegment = ySegment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            ySegment = ySegment.divide(BigDecimal.TEN.pow(-yScale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-yScale));
        }

        if (ySegment.compareTo(BigDecimal.ZERO) == 0) {
            ySegment = BigDecimal.ONE;
        }

        AtomicReference<BigDecimal> xPosition = new AtomicReference<>(xRange[0].divideToIntegralValue(xSegment).multiply(xSegment));
        AtomicReference<BigDecimal> yPosition = new AtomicReference<>(yRange[0].divideToIntegralValue(ySegment).multiply(ySegment));

        int dpsWidth = Math.round(axisFont.measureDimension(LangID.getStringByID("data.dps", lang))[3]);
        int rangeHeight = Math.round(axisFont.measureDimension(LangID.getStringByID("data.range", lang))[3]);

        int xAxisNumberHeight = 0;
        int yAxisNumberWidth = 0;

        while(xPosition.get().compareTo(xRange[1]) <= 0) {
            float[] boundary = plotFont.measureDimension(Equation.simpleNumber(xPosition.get(), 7));

            xAxisNumberHeight = Math.round(Math.max(xAxisNumberHeight, boundary[3]));

            xPosition.set(xPosition.get().add(xSegment));
        }

        while(yPosition.get().compareTo(yRange[1]) <= 0) {
            float[] boundary = plotFont.measureDimension(Equation.simpleNumber(yPosition.get(), 7));

            yAxisNumberWidth = Math.round(Math.max(yAxisNumberWidth, boundary[2]));

            yPosition.set(yPosition.get().add(ySegment));
        }

        int finalWidth = (int) Math.round(axisTitleGap * 2 + dpsWidth + yAxisNumberWidth + indicatorGap + plotWidthHeight * indicatorRatio * 0.5 + plotWidthHeight * 1.5 + plotGraphOffset);
        int finalHeight = (int) Math.round(plotGraphOffset + plotWidthHeight + plotWidthHeight * indicatorRatio * 0.5 + indicatorGap + xAxisNumberHeight + axisTitleGap * 2 + rangeHeight);

        int offsetX = (int) Math.round(axisTitleGap * 2 + dpsWidth + yAxisNumberWidth + indicatorGap + plotWidthHeight * indicatorRatio * 0.5);
        int offsetY = plotGraphOffset;

        CountDownLatch waiter = new CountDownLatch(1);

        int finalXAxisNumberHeight = xAxisNumberHeight;

        BigDecimal finalXSegment = xSegment;
        BigDecimal finalYSegment = ySegment;

        StaticStore.renderManager.createRenderer(finalWidth, finalHeight, temp, connector -> {
            connector.queue(g -> {
                g.setColor(51, 53, 60, 255);
                g.fillRect(0, 0, finalWidth, finalHeight);

                g.setColor(238, 238, 238, 255);
                g.setStroke(axisStroke, GLGraphics.LineEndMode.VERTICAL);

                g.drawRect(offsetX, offsetY, (int) Math.round(plotWidthHeight * 1.5), plotWidthHeight);

                xPosition.set(xRange[0].divideToIntegralValue(finalXSegment).multiply(finalXSegment));
                yPosition.set(yRange[0].divideToIntegralValue(finalYSegment).multiply(finalYSegment));

                while(xPosition.get().compareTo(xRange[1]) <= 0) {
                    int xPos = convertCoordinateToPixel((int) Math.round(plotWidthHeight * 1.5), xPosition.get(), xWidth, centerX, true);

                    g.setColor(238, 238, 238, 255);
                    g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                    g.drawLine(offsetX + xPos, offsetY + plotWidthHeight, offsetX + xPos, (int) Math.round(offsetY + plotWidthHeight + plotWidthHeight * indicatorRatio / 2.0));

                    g.setColor(238, 238, 238, 64);
                    g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

                    g.drawLine(offsetX + xPos, offsetY, offsetX + xPos, offsetY + plotWidthHeight);
                    g.setFontModel(plotFont);
                    g.setColor(238, 238, 238, 255);

                    g.drawText(Equation.simpleNumber(xPosition.get(), 7), offsetX + xPos, (int) Math.round(offsetY + plotWidthHeight + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap), GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);

                    xPosition.set(xPosition.get().add(finalXSegment));
                }

                while(yPosition.get().compareTo(yRange[1]) <= 0) {
                    int yPos = convertCoordinateToPixel(plotWidthHeight, yPosition.get(), yWidth, centerY, false);

                    g.setColor(238, 238, 238, 255);
                    g.setStroke(indicatorStroke, GLGraphics.LineEndMode.ROUND);

                    g.drawLine(offsetX, offsetY + yPos, (int) Math.round(offsetX - plotWidthHeight * indicatorRatio / 2.0), offsetY + yPos);

                    g.setColor(238, 238, 238, 64);
                    g.setStroke(subIndicatorStroke, GLGraphics.LineEndMode.ROUND);

                    g.drawLine(offsetX, offsetY + yPos, (int) Math.round(offsetX + plotWidthHeight * 1.5), offsetY + yPos);

                    g.setFontModel(plotFont);
                    g.setColor(238, 238, 238, 255);

                    g.drawText(Equation.simpleNumber(yPosition.get(), 7), (int) Math.round(offsetX - plotWidthHeight * indicatorRatio / 2.0 - indicatorGap), offsetY + yPos, GLGraphics.HorizontalSnap.LEFT, GLGraphics.VerticalSnap.MIDDLE);

                    yPosition.set(yPosition.get().add(finalYSegment));
                }

                g.setColor(118, 224, 85, 255);
                g.setStroke(plotStroke, GLGraphics.LineEndMode.ROUND);

                ArrayList<Float> vertices = new ArrayList<>();

                for(int i = 0; i < coordinates.length - 1; i++) {
                    if(coordinates[i][1] == null || coordinates[i + 1][1] == null || coordinates[i][0] == null || coordinates[i + 1][0] == null)
                        continue;

                    if (i == 0) {
                        int x0 = convertCoordinateToPixel((int) Math.round(plotWidthHeight * 1.5), coordinates[i][0], xWidth, centerX, true);
                        int y0 = convertCoordinateToPixel(plotWidthHeight, coordinates[i][1], yWidth, centerY, false);

                        vertices.add((float) (offsetX + x0));
                        vertices.add((float) (offsetY + y0));
                    }

                    int x1 = convertCoordinateToPixel((int) Math.round(plotWidthHeight * 1.5), coordinates[i + 1][0], xWidth, centerX, true);
                    int y1 = convertCoordinateToPixel(plotWidthHeight, coordinates[i + 1][1], yWidth, centerY, false);

                    vertices.add((float) (offsetX + x1));
                    vertices.add((float) (offsetY + y1));
                }

                g.drawVertices(vertices);

                if (withTreasure != null) {
                    g.setColor(235, 64, 52, 255);
                    g.setStrokeType(GLGraphics.LineType.DASH, 8, 0xAAAA);

                    vertices = new ArrayList<>();

                    for(int i = 0; i < withTreasure.length - 1; i++) {
                        if(withTreasure[i][1] == null || withTreasure[i + 1][1] == null || withTreasure[i][0] == null || withTreasure[i + 1][0] == null)
                            continue;

                        if (i == 0) {
                            int x0 = convertCoordinateToPixel((int) Math.round(plotWidthHeight * 1.5), withTreasure[i][0], xWidth, centerX, true);
                            int y0 = convertCoordinateToPixel(plotWidthHeight, withTreasure[i][1], yWidth, centerY, false);

                            vertices.add((float) (offsetX + x0));
                            vertices.add((float) (offsetY + y0));
                        }

                        int x1 = convertCoordinateToPixel((int) Math.round(plotWidthHeight * 1.5), withTreasure[i + 1][0], xWidth, centerX, true);
                        int y1 = convertCoordinateToPixel(plotWidthHeight, withTreasure[i + 1][1], yWidth, centerY, false);

                        vertices.add((float) (offsetX + x1));
                        vertices.add((float) (offsetY + y1));
                    }

                    g.drawVertices(vertices);
                }

                g.setFontModel(axisFont);
                g.setColor(238, 238, 238, 255);

                g.drawText(LangID.getStringByID("data.range", lang), (int) Math.round(offsetX + plotWidthHeight * 1.5 / 2.0), (int) Math.round(plotGraphOffset + plotWidthHeight + plotWidthHeight * indicatorRatio / 2.0 + indicatorGap + finalXAxisNumberHeight + axisTitleGap), GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);

                g.translate(axisTitleGap, plotGraphOffset + plotWidthHeight / 2f);
                g.rotate((float) (-Math.PI / 2.0));

                g.drawText(LangID.getStringByID("data.dps", lang), 0f, 0f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.TOP);

                g.reset();

                return null;
            });

            return null;
        }, progress -> image, () -> {
            waiter.countDown();

            return null;
        });

        waiter.await();

        return image;
    }

    private static int convertCoordinateToPixel(int length, BigDecimal coordinate, BigDecimal range, BigDecimal center, boolean x) {
        if(range.compareTo(BigDecimal.ZERO) == 0)
            return -1;

        if(x) {
            return coordinate.subtract(center).add(range.divide(BigDecimal.valueOf(2), Equation.context)).divide(range, Equation.context).multiply(BigDecimal.valueOf(length)).round(new MathContext(0, RoundingMode.HALF_EVEN)).intValue();
        } else {
            return range.divide(BigDecimal.valueOf(2), Equation.context).subtract(coordinate.subtract(center)).divide(range, Equation.context).multiply(BigDecimal.valueOf(length)).round(new MathContext(0, RoundingMode.HALF_EVEN)).intValue();
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
            case 3 -> "u";
            default -> String.valueOf(ind);
        };
    }

    private static int[] measureUnitTitleImage(String name, String type, int lv) {
        int[] dimension = new int[2];

        float[] nameDimension = titleFont.measureDimension(name);
        float[] typeDimension = typeFont.measureDimension(type);
        float[] levelDimension = levelFont.measureDimension("Lv. "+lv);

        int h = Math.round(nameDimension[3] + nameMargin + typeDimension[3] + typeUpDownMargin * 2 + levelMargin + levelDimension[3]);

        int icw = (int) ((h - levelDimension[3] - levelMargin) * 1.0 * 110 / 85);

        int w = icw + nameMargin + (int) Math.max(nameDimension[2], typeDimension[3] + typeLeftRightMargin * 2);

        dimension[0] = w;
        dimension[1] = h;

        return dimension;
    }

    private static float[] measureEnemyTitle(String name, String mag) {
        float[] nRect = titleFont.measureDimension(name);
        float[] lRect = levelFont.measureDimension(mag);

        int h = Math.round(nRect[3] + nameMargin + lRect[3] + enemyIconGap * 3);

        int icw = (int) (h * enemyIconRatio);

        int w = (int) (icw + nameMargin + Math.max(nRect[2], lRect[2]));

        return new float[] { w, h };
    }

    private static void drawStageTitleImage(GLGraphics g, String name, String code) {
        float[] nRect = titleFont.measureDimension(name);

        g.setColor(238, 238, 238, 255);
        g.setFontModel(titleFont);

        g.drawText(name, bgMargin + bgMargin, bgMargin * 2, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);

        if(!code.equals(name)) {
            g.setColor(191, 191, 191);
            g.setFontModel(contentFont);

            g.drawText(code, bgMargin + bgMargin, (int) (bgMargin * 2 + nRect[2] + nameMargin), GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
        }
    }

    private static FakeImage getFruitImage(File container, int id) throws Exception {
        if(id == -1) {
            VFile vf = VFile.get("./org/page/catfruit/xp.png");

            if(vf != null) {
                return vf.getData().getImg();
            }
        } else {
            String name = "gatyaitemD_"+id+"_f.png";
            VFile vf = VFile.get("./org/page/catfruit/"+name);

            if(vf == null) {
                File icon = new File(container, name);

                if(icon.exists()) {
                    return ImageBuilder.builder.build(icon);
                }
            } else {
                return vf.getData().getImg();
            }
        }

        return null;
    }

    /**
     *
     * @param st Stage
     * @param map Stage map
     * @param lang Language value
     * @return Returns { Width of Chance, Width of Item, Width of Amount, Total Width, Total Height }
     */
    private static float[] measureDropTableWidth(Stage st, CustomStageMap map, CommonStatic.Lang.Locale lang, boolean reward) {
        List<String[]> dropData;

        if(reward) {
            dropData = DataToString.getRewards(st, map, lang);
        } else {
            dropData = DataToString.getScoreDrops(st, map, lang);
        }

        if(dropData == null)
            return null;

        float[] result = new float[5];

        String chance;

        if(dropData.getLast().length == 1) {
            dropData.removeLast();

            chance = LangID.getStringByID("data.stage.reward.number", lang);
        } else {
            chance = LangID.getStringByID("data.stage.reward.chance", lang);
        }

        float cw = contentFont.textWidth(chance);
        float rw = contentFont.textWidth(LangID.getStringByID("data.stage.reward.reward", lang));
        float aw = contentFont.textWidth(LangID.getStringByID("data.stage.reward.amount", lang));

        for(int i = 0; i < dropData.size(); i++) {
            String[] data = dropData.get(i);

            if(data.length != 3)
                continue;

            cw = Math.max(cw, contentFont.textWidth(data[0]));
            rw = Math.max(rw, contentFont.textWidth(data[1]));
            aw = Math.max(aw, contentFont.textWidth(data[2]));
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
     * @param lang Language value
     * @return Returns { Width of Enemy, Width of Number, Width of Base, Width of Magnification, Width of Start, Width of Layer, Width of Boss, Total Width, Total Height }
     */
    private static float[] measureEnemySchemeWidth(Stage st, CustomStageMap map, boolean isRanking, boolean isFrame, int lv, CommonStatic.Lang.Locale lang) {
        float[] result = new float[11];
        
        float ew = contentFont.textWidth(LangID.getStringByID("data.stage.enemy", lang));
        float nw = contentFont.textWidth(LangID.getStringByID("data.stage.number", lang));
        float bw = contentFont.textWidth(LangID.getStringByID(isRanking ? "data.stage.totalDamage" : "data.stage.basePercentage", lang));
        float mw = contentFont.textWidth(LangID.getStringByID("data.stage.magnification", lang));
        float sw = contentFont.textWidth(LangID.getStringByID("data.stage.start", lang));
        float lw = contentFont.textWidth(LangID.getStringByID("data.stage.layer", lang));
        float rw = contentFont.textWidth(LangID.getStringByID("data.stage.respectStart", lang));
        float kw = contentFont.textWidth(LangID.getStringByID("data.stage.killCount", lang));
        float bow = contentFont.textWidth(LangID.getStringByID("data.stage.isBoss", lang));

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
                enemyName = LangID.getStringByID("data.stage.enemy", lang)+" - "+Data.trio(id.id);
            }

            ew = Math.max(ew, contentFont.textWidth(enemyName));

            String number;

            if(line.number == 0)
                number = LangID.getStringByID("data.stage.infinite", lang);
            else
                number = String.valueOf(line.number);

            nw = Math.max(nw, contentFont.textWidth(number));

            String baseHP;
            String suffix = isRanking ? "" : "%";

            if(line.castle_0 == line.castle_1 || line.castle_1 == 0)
                baseHP = line.castle_0 + suffix;
            else {
                int minHealth = Math.min(line.castle_0, line.castle_1);
                int maxHealth = Math.max(line.castle_0, line.castle_1);

                baseHP = minHealth + " ~ " + maxHealth + suffix;
            }

            bw = Math.max(bw, contentFont.textWidth(baseHP));

            mw = Math.max(mw, contentFont.textWidth(DataToString.getMagnification(new int[] {line.multiple, line.mult_atk}, lv)));

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

            sw = Math.max(sw, contentFont.textWidth(startResp));

            String layer;

            if(line.layer_0 != line.layer_1) {
                int minLayer = Math.min(line.layer_0, line.layer_1);
                int maxLayer = Math.max(line.layer_0, line.layer_1);

                layer = minLayer + " ~ " + maxLayer;
            } else {
                layer = String.valueOf(line.layer_0);
            }

            lw = Math.max(lw, contentFont.textWidth(layer));

            String respect = (line.spawn_0 < 0 || line.spawn_1 < 0) ? LangID.getStringByID("data.true", lang) : "";

            rw = Math.max(rw, contentFont.textWidth(respect));

            kw = Math.max(kw, contentFont.textWidth(String.valueOf(line.kill_count)));

            String boss;

            if(line.boss == 0)
                boss = "";
            else if(line.boss == 1)
                boss = LangID.getStringByID("data.stage.boss.normal", lang);
            else
                boss = LangID.getStringByID("data.stage.boss.shake", lang);

            bow = Math.max(bow, contentFont.textWidth(boss));
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

    private static void drawRewardTable(GLGraphics g, int x, int y, Stage st, CustomStageMap map, float[] dimension, int desiredGap, CommonStatic.Lang.Locale lang, boolean reward) throws Exception {
        List<String[]> data;

        if(reward)
            data = DataToString.getRewards(st, map, lang);
        else
            data = DataToString.getScoreDrops(st, map, lang);

        if(data != null) {
            int w = Math.round(desiredGap * 7 + dimension[CHANCE_WIDTH] + dimension[REWARD_WIDTH] + dimension[AMOUNT_WIDTH] + rewardIconSize);
            int h = Math.round(dimension[TOTAL_HEIGHT]);

            g.setFontModel(contentFont);

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

                        if (data.getLast().length == 1) {
                            data.removeLast();

                            chance = LangID.getStringByID("data.stage.reward.number", lang);
                        } else {
                            chance = LangID.getStringByID("data.stage.reward.chance", lang);
                        }

                        g.setColor(191, 191, 191);

                        g.drawText(chance, x1 + (int) tx, y + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                        g.setStroke(headerStroke, GLGraphics.LineEndMode.ROUND);

                        int chaneLineY = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

                        g.drawLine((int) (x1 + tx * 2.0), y + chaneLineY, (int) (x1 + tx * 2.0), y + innerTableCellMargin - chaneLineY);

                        int chanceY = y + innerTableCellMargin;

                        for (int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawText(data.get(j)[i], x1 + (int) tx, chanceY + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                            g.setColor(191, 191, 191, 64);

                            g.drawLine((int) (x1 + tx * 2.0), chanceY + chaneLineY, (int) (x1 + tx * 2.0), chanceY + innerTableCellMargin - chaneLineY);

                            chanceY += innerTableCellMargin;
                        }
                    }
                    case REWARD_WIDTH -> {
                        g.setColor(191, 191, 191);

                        g.drawText(LangID.getStringByID("data.stage.reward.reward", lang), x1 + (int) tx, y + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                        g.setStroke(headerStroke, GLGraphics.LineEndMode.ROUND);

                        int rewardLineY = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

                        g.drawLine((int) (x1 + tx * 2.0), y + rewardLineY, (int) (x1 + tx * 2.0), y + innerTableCellMargin - rewardLineY);

                        int rewardY = y + innerTableCellMargin;

                        int rx = (int) Math.round((tx * 2.0 - desiredGap - rewardIconSize) / 2.0);

                        for (int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawText(data.get(j)[i], x1 + desiredGap + rewardIconSize + rx, rewardY + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                            g.setColor(65, 69, 76);

                            g.fillOval(x1 + desiredGap, rewardY + (innerTableCellMargin - rewardIconSize) / 2f, rewardIconSize / 2f, rewardIconSize / 2f);

                            FakeImage icon;

                            if (reward) {
                                icon = getRewardImage(((DefStageInfo) st.info).drop[j][i], map);
                            } else {
                                icon = getRewardImage(((DefStageInfo) st.info).time[j][i], map);
                            }

                            if (icon != null) {
                                g.drawImage(icon, x1 + desiredGap, rewardY + (innerTableCellMargin - rewardIconSize) / 2f, rewardIconSize, rewardIconSize);
                            }

                            g.setColor(191, 191, 191, 64);

                            g.drawLine((int) (x1 + tx * 2.0), rewardY + rewardLineY, (int) (x1 + tx * 2.0), rewardY + innerTableCellMargin - rewardLineY);

                            rewardY += innerTableCellMargin;
                        }
                    }
                    case AMOUNT_WIDTH -> {
                        g.setColor(191, 191, 191);
                        g.drawText(LangID.getStringByID("data.stage.reward.amount", lang), x1 + (int) tx, y + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
                        g.setStroke(headerStroke, GLGraphics.LineEndMode.ROUND);
                        int amountY = y + innerTableCellMargin;
                        for (int j = 0; j < data.size(); j++) {
                            g.setColor(239, 239, 239);

                            g.drawText(data.get(j)[i], x1 + (int) tx, amountY + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

                            g.setColor(191, 191, 191, 64);

                            amountY += innerTableCellMargin;
                        }
                    }
                }

                x1 += (int) (tx * 2.0);
            }

            g.setColor(191, 191, 191, 64);

            g.setStroke(innerTableLineStroke, GLGraphics.LineEndMode.ROUND);

            int y1 = y + innerTableCellMargin * 2;

            for(int i = 0; i < data.size() - 1; i++) {
                g.drawLine(x + innerTableTextMargin, y1, x + w - innerTableTextMargin, y1);

                y1 += innerTableCellMargin;
            }
        }
    }

    private static void drawEnemySchemeTable(GLGraphics g, int y, Stage st, CustomStageMap map, float[] dimension, int desiredGap, boolean isRanking, boolean isFrame, int lv, CommonStatic.Lang.Locale lang) throws Exception {
        int w = desiredGap * 19 + rewardIconSize;

        for(int i = ENEMY; i <= BOSS; i++) {
            w = Math.round(w + dimension[i]);
        }

        int h = innerTableCellMargin * (st.data.datas.length + 1);

        g.setFontModel(contentFont);

        g.setColor(65, 69, 76);

        g.fillRoundRect(bgMargin, y, w, h, cornerRadius, cornerRadius);

        g.setColor(24, 25, 28);

        g.fillRoundRect(bgMargin, y, w, innerTableCellMargin + cornerRadius / 2f, cornerRadius, cornerRadius);

        g.setColor(65, 69, 76);

        g.fillRect(bgMargin, y + innerTableCellMargin, w, cornerRadius / 2f);

        String[] headerText = {
                LangID.getStringByID("data.stage.enemy", lang),
                LangID.getStringByID("data.stage.number", lang),
                LangID.getStringByID(isRanking ? "data.stage.totalDamage" : "data.stage.basePercentage", lang),
                LangID.getStringByID("data.stage.magnification", lang),
                LangID.getStringByID("data.stage.start", lang),
                LangID.getStringByID("data.stage.layer", lang),
                LangID.getStringByID("data.stage.respectStart", lang),
                LangID.getStringByID("data.stage.killCount", lang),
                LangID.getStringByID("data.stage.isBoss", lang)
        };

        int x1 = bgMargin;

        g.setColor(191, 191, 191);

        g.setStroke(headerStroke, GLGraphics.LineEndMode.ROUND);

        for(int i = ENEMY; i <= BOSS; i++) {
            double tx = desiredGap * 2 + dimension[i];

            if(i == ENEMY)
                tx += desiredGap + rewardIconSize;

            tx /= 2.0;

            int ly = (int) Math.round((innerTableCellMargin - headerSeparatorHeight) / 2.0);

            g.drawText(headerText[i], (int) Math.round(x1 + tx),  y + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);

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
                            content = LangID.getStringByID("data.stage.enemy", lang) + " - " + Data.trio(id.id);
                        }
                    }
                    case NUMBER -> {
                        if (line.number == 0)
                            content = LangID.getStringByID("data.stage.infinite", lang);
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
                            content = (line.spawn_0 < 0 || line.spawn_1 < 0) ? LangID.getStringByID("data.true", lang) : "";
                    case KILL -> content = String.valueOf(line.kill_count);
                    case BOSS -> {
                        if (line.boss == 0)
                            content = "";
                        else
                            content = LangID.getStringByID("data.stage.boss.normal", lang);
                    }
                }

                if(j == ENEMY) {
                    g.setColor(51, 53, 60);

                    g.fillOval(bgMargin + desiredGap, y1 + (innerTableCellMargin - rewardIconSize) / 2f, rewardIconSize / 2f, rewardIconSize / 2f);

                    FakeImage icon = getEnemyIcon(line.enemy.id, map);

                    if(icon != null) {
                        g.drawImage(icon, bgMargin + desiredGap + 15, y1 + (innerTableCellMargin - rewardIconSize) / 2f + 15, 50, 50);
                    }

                    g.setColor(239, 239, 239);

                    g.drawText(content, bgMargin + desiredGap + rewardIconSize + rx, y1 + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
                } else {
                    g.setColor(239, 239, 239);

                    g.drawText(content, (int) (x1 + tx), y1 + innerTableCellMargin / 2f, GLGraphics.HorizontalSnap.MIDDLE, GLGraphics.VerticalSnap.MIDDLE);
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

        g.setStroke(innerTableLineStroke, GLGraphics.LineEndMode.ROUND);

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

    private static float maxAmong(float... values) {
        if(values.length == 1)
            return values[0];
        else if(values.length == 2) {
            return Math.max(values[0], values[1]);
        } else if(values.length >= 3) {
            float val = Math.max(values[0], values[1]);

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

    private static String getProgressBar(float prog, int time) {
        int ratio = (int) (prog * 40f / time);

        return "▓".repeat(Math.max(0, ratio)) +
                "░".repeat(Math.max(0, 40 - ratio));
    }

    private static String getETA(long start, long current, float prog, int time) {
        double unit = (current - start) / 1000f / prog;

        return DataToString.df.format(unit * (time - prog));
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

    private static FakeImage getRewardImage(int id, CustomStageMap map) throws Exception {
        if(id < 1000) {
            File icon = map.rewardIcons.get(id);

            if(icon != null) {
                return ImageBuilder.builder.build(icon);
            }

            if(id >= 11 && id <= 13)
                id += 9;

            String name = "gatyaitemD_" + Data.duo(id) + "_f.png";

            VFile vf = VFile.get("./org/page/items/"+name);

            if(vf != null) {
                return vf.getData().getImg();
            }
        } else if(id < 30000) {
            File icon;

            if(id < 10000)
                icon = map.unitIcons.get(id);
            else
                icon = map.trueFormIcons.get(id);

            if(icon != null) {
                return ImageBuilder.builder.build(icon);
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
                    return vf.getData().getImg();
                }
            }
        }

        return null;
    }

    private static FakeImage getEnemyIcon(int eid, CustomStageMap map) throws Exception {
        File icon = map.enemyIcons.get(eid);

        if(icon != null) {
            return ImageBuilder.builder.build(icon);
        } else {
            VFile vf = VFile.get("./org/enemy/" + Data.trio(eid) + "/enemy_icon_" + Data.trio(eid) + ".png");

            if(vf != null) {
                return vf.getData().getImg();
            }
        }

        return null;
    }
}