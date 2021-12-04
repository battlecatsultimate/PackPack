import common.CommonStatic;
import common.system.P;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import mandarin.packpack.supporter.AssetDownloader;
import mandarin.packpack.supporter.PackContext;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.bc.*;
import mandarin.packpack.supporter.bc.cell.*;
import mandarin.packpack.supporter.lang.LangID;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    public static void main(String[] args) throws Exception {
        int[] a = {2, 2, 3, 4, 5, 6, 7, 7, 8, 9, 9, 9, 10, 1};

        System.out.println(containAll(a, 1,2,3));
    }

    private static boolean containAll(int[] data, int... ids) {
        for(int i = 0; i < ids.length; i++) {
            System.out.println(ids[i]);
            if(ArrayUtils.contains(data, ids[i]))
                return false;
        }

        return true;
    }
}
