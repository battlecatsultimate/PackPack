package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.pack.Background;
import discord4j.core.object.entity.Message;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lzw.AnimatedGifEncoder;
import org.apache.commons.lang3.SystemUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ImageDrawing {
    public static File drawBGImage(Background bg, int w, int h) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File image = new File("./temp", StaticStore.findFileName(temp, "result", ".png"));

        if(!image.exists()) {
            boolean res = image.createNewFile();

            if(!res) {
                System.out.println("Can't create new file : "+image.getAbsolutePath());
                return null;
            }
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(img.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        double groundRatio = 0.1;
        double skyRatio = 0.1;

        double ratio = h * (1.0 - groundRatio - skyRatio) / 2.0 / 512.0;

        int groundHeight = (int) (groundRatio * h);

        bg.load();

        g.gradRect(0, h - groundHeight, w, groundHeight, 0, h - groundHeight, bg.cs[2], 0, h, bg.cs[3]);

        int pos = (int) ((-bg.parts[Background.BG].getWidth()+256) * ratio);

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

            pos = (int) ((-bg.parts[Background.BG].getWidth() + 256) * ratio);
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

        ImageIO.write(img, "PNG", image);

        return image;
    }

    public static File drawAnimImage(EAnimD<?> anim, int frame, double siz, boolean transparent, boolean debug) throws Exception {
        CommonStatic.getConfig().ref = false;

        anim.setTime(frame);

        Rectangle rect = new Rectangle();

        ArrayList<int[][]> rects = new ArrayList<>();
        ArrayList<P> centers = new ArrayList<>();

        for(int i = 1; i < anim.getOrder().length; i++) {
            if(anim.anim().parts(anim.getOrder()[i].getVal(2)) == null)
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
        File file = new File("./temp", StaticStore.findFileName(temp, "result", ".png"));

        if(!file.exists()) {
            boolean res = file.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+file.getAbsolutePath());
                return null;
            }
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

        String folderName = StaticStore.findFileName(temp, "images", "");

        File folder = new File("./temp/"+folderName);

        if(!folder.exists()) {
            boolean res = folder.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+folder.getAbsolutePath());

                return null;
            }
        }

        File gif = new File("./temp", StaticStore.findFileName(temp, "result", ".mp4"));

        if(!gif.exists()) {
            boolean res = gif.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+gif.getAbsolutePath());
                return null;
            }
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

            for(int j = 1; j < anim.getOrder().length; j++) {
                if(anim.anim().parts(anim.getOrder()[j].getVal(2)) == null)
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

        msg.edit(m -> m.setContent(cont)).subscribe();

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

        String finalFinCont = finCont;

        msg.edit(m -> m.setContent(finalFinCont)).subscribe();

        P pos = new P(-rect.x, -rect.y);

        long current = System.currentTimeMillis();

        for(int i = 0; i < frame; i++) {
            if(System.currentTimeMillis() - current >= 1500) {
                int finalI = i;
                int finalFrame = frame;
                msg.edit(m -> {
                    String content = finalFinCont +"\n\n";

                    content += LangID.getStringByID("gif_makepng", lang).replace("_", DataToString.df.format(finalI * 100.0 / finalFrame));

                    m.setContent(content);
                }).subscribe();
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

        msg.edit(m -> {
            String content = finalFinCont + "\n\n" + LangID.getStringByID("gif_makepng", lang).replace("_", "100")
                    +"\n\n"+ LangID.getStringByID("gif_converting", lang);

            m.setContent(content);
        }).subscribe();

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

        msg.edit(m -> {
            String content = finalFinCont + "\n\n"+LangID.getStringByID("gif_makepng", lang).replace("_", "100")+"\n\n"
                    +LangID.getStringByID("gif_converting", lang)+"\n\n"+LangID.getStringByID("gif_uploadmp4", lang);

            m.setContent(content);
        }).subscribe();

        return gif;
    }

    public static File drawAnimGif(EAnimD<?> anim, Message msg, double siz, boolean debug, int limit, int lang) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File gif = new File("./temp", StaticStore.findFileName(temp, "result", ".gif"));

        if(!gif.exists()) {
            boolean res = gif.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+gif.getAbsolutePath());
                return null;
            }
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

            for(int j = 1; j < anim.getOrder().length; j++) {
                if(anim.anim().parts(anim.getOrder()[j].getVal(2)) == null)
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

        String finalCont = cont;
        msg.edit(m -> m.setContent(finalCont)).subscribe();

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

        FileOutputStream fos = new FileOutputStream(gif);

        encoder.start(fos);

        P pos = new P(-rect.x, -rect.y);

        long current = System.currentTimeMillis();

        for(int i = 0; i < Math.min(frame, 300); i++) {
            if(System.currentTimeMillis() - current >= 1000) {
                int finalI = i;

                String finalCont1 = cont;

                int finalFrame = frame;

                msg.edit(m -> {
                    String content = finalCont1 +"\n\n";

                    content += LangID.getStringByID("gif_making", lang).replace("_", DataToString.df.format(finalI * 100.0 / Math.min(300, finalFrame)));

                    m.setContent(content);
                }).subscribe();

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

        String finalCont2 = cont;
        msg.edit(m -> {
            String content = finalCont2 + "\n\n"+LangID.getStringByID("gif_making", lang).replace("_", "100")+"\n\n"+LangID.getStringByID("gif_uploading", lang);

            m.setContent(content);
        }).subscribe();

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

        String folderName = StaticStore.findFileName(temp, "images", "");

        File folder = new File("./temp/"+folderName);

        if(!folder.exists()) {
            boolean res = folder.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+folder.getAbsolutePath());

                return null;
            }
        }

        File gif = new File("./temp", StaticStore.findFileName(temp, "result", ".mp4"));

        if(!gif.exists()) {
            boolean res = gif.createNewFile();

            if(!res) {
                System.out.println("Can't create file : "+gif.getAbsolutePath());
                return null;
            }
        }

        CommonStatic.getConfig().ref = false;

        Rectangle rect = new Rectangle();

        for(int i = 0; i < mixer.anim.length; i++) {
            EAnimD<?> anim = mixer.getAnim(i);

            if(anim != null) {
                anim.setTime(0);

                for(int j = 0; j < anim.len(); j++) {
                    anim.setTime(j);

                    for(int k = 1; k < anim.getOrder().length; k++) {
                        if(anim.anim().parts(anim.getOrder()[k].getVal(2)) == null)
                            continue;

                        FakeImage fi = anim.anim().parts(anim.getOrder()[k].getVal(2));

                        if(fi.getWidth() == 1 && fi.getHeight() == 1)
                            continue;

                        RawPointGetter getter = new RawPointGetter(fi.getHeight(), fi.getHeight());

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

        msg.edit(m -> m.setContent(cont)).subscribe();

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

        String finalFinCont = finCont;

        msg.edit(m -> m.setContent(finalFinCont)).subscribe();

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
                                int finalProg = progress;
                                int finalTotal = totalFrame;

                                msg.edit(m -> {
                                    String content = finalFinCont +"\n\n";

                                    content += LangID.getStringByID("gif_makepng", lang).replace("_", DataToString.df.format(finalProg * 100.0 / finalTotal));

                                    m.setContent(content);
                                }).subscribe();

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
                            int finalProg = progress;
                            int finalTotal = totalFrame;

                            msg.edit(m -> {
                                String content = finalFinCont +"\n\n";

                                content += LangID.getStringByID("gif_makepng", lang).replace("_", DataToString.df.format(finalProg * 100.0 / finalTotal));

                                m.setContent(content);
                            }).subscribe();

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

        msg.edit(m -> {
            String content = finalFinCont + "\n\n" + LangID.getStringByID("gif_makepng", lang).replace("_", "100")
                    +"\n\n"+ LangID.getStringByID("gif_converting", lang);

            m.setContent(content);
        }).subscribe();


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

        msg.edit(m -> {
            String content = finalFinCont + "\n\n"+LangID.getStringByID("gif_makepng", lang).replace("_", "100")+"\n\n"
                    +LangID.getStringByID("gif_converting", lang)+"\n\n"+LangID.getStringByID("gif_uploadmp4", lang);

            m.setContent(content);
        }).subscribe();

        return gif;
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
}
