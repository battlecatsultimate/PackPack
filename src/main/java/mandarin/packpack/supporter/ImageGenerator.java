package mandarin.packpack.supporter;

import java.io.File;
import java.util.ArrayList;

public interface ImageGenerator {
    int space = 30;
    int xGap = 5;
    int yGap = 2;

    File generateImage(String message, boolean isStage);

    File generateRealImage(String message, boolean isStage);

    ArrayList<String> getInvalids(String message);
}
