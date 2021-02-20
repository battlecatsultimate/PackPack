import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Test {
    public static void main(String[] args) throws Exception {
        File ffmpeg = new File("./data/ffmpeg/bin/ffmpeg.exe");

        File image = new File("./temp/images");

        File reference = new File("./temp/images/0000.png");

        BufferedImage img = ImageIO.read(reference);

        File[] list = image.listFiles();

        String mp4Name = StaticStore.findFileName(new File(""), "Result", ".mp4");
        String mp4Name2 = StaticStore.findFileName(new File(""), "Result2", ".mp4");

        if(list == null)
            return;

        long start = System.currentTimeMillis();

        ProcessBuilder builder = new ProcessBuilder("data/ffmpeg/bin/ffmpeg", "-r", "30", "-f", "image2", "-s", img.getWidth()+"x"+img.getHeight(), "-i"
                , "temp/images/%04d.png", "-vcodec", "libx264", "-crf", "25", "-pix_fmt", "yuv420p" , "-y", mp4Name);

        Process pro = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getErrorStream()));

        String line;

        while((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        pro.waitFor();

        long end =System.currentTimeMillis();

        System.out.println(DataToString.df.format((end-start)/1000.0));
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
