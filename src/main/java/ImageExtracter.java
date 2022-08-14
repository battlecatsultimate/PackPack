import common.CommonStatic;
import common.pack.UserProfile;
import common.system.fake.ImageBuilder;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.AssetDownloader;
import mandarin.packpack.supporter.PackContext;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FIBI;
import mandarin.packpack.supporter.lang.LangID;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ImageExtracter {
    public static void main(String[] args) throws Exception {
        initialize();

        File f = new File("./extract");

        if(!f.exists() && !f.mkdirs())
            return;

        for(Enemy e : UserProfile.getBCData().enemies.getList()) {
            System.out.println(e.id.id);
            if(e.anim.getEdi().getImg() == null)
                continue;

            File g = new File(f.getAbsolutePath()+"/"+ Data.trio(e.id.id)+".png");

            if(!g.exists() && !g.createNewFile())
                return;

            ImageIO.write((BufferedImage) e.anim.getEdi().getImg().bimg(), "PNG", g);
        }

        System.out.println("Finished!");
    }

    public static void initialize() {
        if(!StaticStore.initialized) {
            CommonStatic.ctx = new PackContext();
            ImageBuilder.builder = FIBI.builder;
            StaticStore.readServerInfo();

            AssetDownloader.checkAssetDownload();

            StaticStore.postReadServerInfo();

            LangID.initialize();

            StaticStore.saver = new Timer();
            StaticStore.saver.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("Save Process");
                    StaticStore.saveServerInfo();
                }
            }, 0, TimeUnit.MINUTES.toMillis(5));

            StaticStore.initialized = true;
        }
    }
}
