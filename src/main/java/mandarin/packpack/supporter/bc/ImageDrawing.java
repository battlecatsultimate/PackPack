package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageDrawing {
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

            if(Math.abs(result[1][0]-result[0][0]) >= 500 || Math.abs(result[1][1] - result[2][1]) >= 500)
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

        int w = rect.width;
        int h = rect.height;

        int offY = 0;

        if(h > w && h / w > 2) {
            int oldH = h;

            h = w * 2;

            offY = oldH - h;
        }

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D rg = new FG2D(result.getGraphics());

        rg.setRenderingHint(3, 1);
        rg.enableAntialiasing();

        if(!transparent) {
            rg.setColor(54,57,63,255);
            rg.fillRect(0, 0, w, h);
        }

        anim.draw(rg, new P(-rect.x, -rect.y-offY), siz);

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
