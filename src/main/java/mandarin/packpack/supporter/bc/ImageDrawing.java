package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.pack.Background;
import common.util.unit.Enemy;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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

    public static File drawFormImage(Form f, int mode, int frame, double siz, boolean transparent, boolean debug) throws Exception {
        f.anim.load();

        CommonStatic.getConfig().ref = false;

        if(mode >= f.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = f.anim.getEAnim(getAnimType(mode, f.anim.anims.length));

        anim.setTime(frame);

        Rectangle rect = new Rectangle();

        for(int i = 0; i < anim.getOrder().length; i++) {
            FakeImage fi = f.anim.parts[anim.getOrder()[i].getVal(2)];

            if(fi.getHeight() == 1 && fi.getWidth() == 1)
                continue;

            RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

            getter.apply(anim.getOrder()[i], siz, false);

            int[][] result = getter.getRect();

            if(Math.abs(result[1][0]-result[0][0]) >= 1000 || Math.abs(result[1][1] - result[2][1]) >= 1000)
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
            for(int i = 0; i < anim.getOrder().length; i++) {
                FakeImage fi = f.anim.parts[anim.getOrder()[i].getVal(2)];

                if(fi.getHeight() == 1 && fi.getWidth() == 1)
                    continue;

                RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

                getter.apply(anim.getOrder()[i], siz, false);

                int[][] res = getter.getRect();

                rg.setColor(FakeGraphics.RED);

                rg.drawLine(-rect.x + res[0][0], -rect.y + res[0][1], -rect.x + res[1][0], -rect.y + res[1][1]);
                rg.drawLine(-rect.x + res[1][0], -rect.y + res[1][1], -rect.x + res[2][0], -rect.y + res[2][1]);
                rg.drawLine(-rect.x + res[2][0], -rect.y + res[2][1], -rect.x + res[3][0], -rect.y + res[3][1]);
                rg.drawLine(-rect.x + res[3][0], -rect.y + res[3][1], -rect.x + res[0][0], -rect.y + res[0][1]);

                rg.setColor(0, 255, 0, 255);

                rg.fillRect(-rect.x + (int) getter.center.x - 2, -rect.y + (int)getter.center.y -2, 4, 4);
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

        f.anim.unload();

        return file;
    }

    public static File drawEnemyImage(Enemy e, int mode, int frame, double siz, boolean transparent, boolean debug) throws Exception {
        e.anim.load();

        CommonStatic.getConfig().ref = false;

        if(mode >= e.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = e.anim.getEAnim(getAnimType(mode, e.anim.anims.length));

        anim.setTime(frame);

        Rectangle rect = new Rectangle();

        ArrayList<int[][]> rects = new ArrayList<>();
        ArrayList<P> centers = new ArrayList<>();

        for(int i = 0; i < anim.getOrder().length; i++) {
            FakeImage fi = e.anim.parts[anim.getOrder()[i].getVal(2)];

            if(fi.getHeight() == 1 && fi.getWidth() == 1)
                continue;

            RawPointGetter getter = new RawPointGetter(fi.getWidth(), fi.getHeight());

            getter.apply(anim.getOrder()[i], siz, false);

            int[][] result = getter.getRect();

            rects.add(result);
            centers.add(getter.center);

            if(Math.abs(result[1][0]-result[0][0]) >= 1000 || Math.abs(result[1][1] - result[2][1]) >= 1000)
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

        e.anim.unload();

        return file;
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
}
