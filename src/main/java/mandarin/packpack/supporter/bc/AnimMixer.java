package mandarin.packpack.supporter.bc;

import common.pack.Source;
import common.system.VImg;
import common.system.fake.FakeImage;
import common.util.anim.*;
import mandarin.packpack.supporter.awt.FIBI;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

public class AnimMixer implements Source.AnimLoader {
    public static boolean validPng(File image) throws Exception {
        if(!image.exists() || image.isDirectory())
            return false;

        FileInputStream fis = new FileInputStream(image);

        int pngHeader = fis.read();

        if(pngHeader != 137) {
            return false;
        }

        int p = fis.read();
        int n = fis.read();
        int g = fis.read();

        String png = Character.toString(p) + Character.toString(n) + Character.toString(g);

        fis.close();

        return png.equals("PNG");
    }

    public static boolean validMaanim(File maanim) throws Exception {
        if(!maanim.exists() || maanim.isDirectory())
            return false;

        BufferedReader reader = new BufferedReader(new FileReader(maanim));

        String line = reader.readLine();

        if(line == null)
            return false;

        line = line.trim();

        reader.close();

        return line.contains("[modelanim:animation") || line.contains("[maanim]");
    }

    public BufferedImage png;
    public ImgCut imgCut;
    public MaModel model;
    public MaAnim[] anim;

    public AnimCI mixture;

    public AnimMixer(int len) {
        anim = new MaAnim[len];
    }

    public EAnimD<?> getAnim(int index) {
        if(index < 0)
            index = 0;

        if(index >= anim.length)
            index = anim.length - 1;

        if(mixture == null) {
            boolean result = mix();

            if(!result) {
                return null;
            } else {
                return new EAnimD<>(mixture, model, anim[index]);
            }
        } else {
            return new EAnimD<>(mixture, model, anim[index]);
        }
    }

    public boolean mix() {
        if(png != null && imgCut != null && model != null && anim != null) {
            mixture = new AnimCI(this);
            mixture.load();
            return true;
        }

        return false;
    }

    public boolean validImgCut(File imgcut) throws Exception {
        if(!imgcut.exists() || imgcut.isDirectory())
            return false;

        BufferedReader reader = new BufferedReader(new FileReader(imgcut, StandardCharsets.UTF_8));

        String line = reader.readLine();

        if(line == null)
            return false;

        line = line.trim();

        reader.close();

        return line.contains("[imgcut]");
    }

    public boolean validMamodel(File mamodel) throws Exception {
        if(!mamodel.exists() || mamodel.isDirectory())
            return false;

        BufferedReader reader = new BufferedReader(new FileReader(mamodel));

        String line = reader.readLine();

        if(line == null)
            return false;

        line = line.trim();

        reader.close();

        return line.contains("[modelanim:model") || line.contains("[mamodel]");
    }

    @Override
    public VImg getEdi() {
        return null;
    }

    @Override
    public ImgCut getIC() {
        return imgCut;
    }

    @Override
    public MaAnim[] getMA() {
        return anim;
    }

    @Override
    public MaModel getMM() {
        return model;
    }

    @Override
    public Source.ResourceLocation getName() {
        return null;
    }

    @Override
    public FakeImage getNum() {
        return FIBI.build(png);
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public VImg getUni() {
        return null;
    }
}
