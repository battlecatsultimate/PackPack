package mandarin.packpack.supporter.bc;

import java.awt.image.BufferedImage;
import java.util.List;

public class CustomCombo {
    public final String title;
    public final String description;
    public final List<BufferedImage> icons;
    public final List<String> names;
    public final String type;
    public final String level;

    public CustomCombo(String title, String description, List<BufferedImage> icons, List<String> names, String type, String level) {
        this.title = title;
        this.description = description;
        this.icons = icons;
        this.names = names;
        this.type = type;
        this.level = level;

        if(icons.size() != names.size()) {
            throw new IllegalStateException("E/CustomCombo - Size of icons and size of names aren't synchronized!");
        }
    }
}
