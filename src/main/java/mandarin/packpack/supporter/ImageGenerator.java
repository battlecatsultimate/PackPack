package mandarin.packpack.supporter;

import java.io.File;
import java.util.ArrayList;

public abstract class ImageGenerator {
    public static int space = 30;
    public static int xGap = 5;
    public static int yGap = 2;

    public abstract File generateImage(String message, boolean isStage) throws Exception;

    public abstract File generateRealImage(String message, boolean isStage) throws Exception;

    public abstract ArrayList<String> getInvalids(String message);
}
