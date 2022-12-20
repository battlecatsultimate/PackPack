package mandarin.packpack.supporter.bc;

import java.awt.image.BufferedImage;
import java.util.List;

public class TalentData {
    public final List<Integer> cost;
    public final String title;
    public final String[] description;
    public final BufferedImage icon;

    public TalentData(List<Integer> cost, String title, String description, BufferedImage icon) {
        this.cost = cost;
        this.title = title;
        this.description = description.split("\n");
        this.icon = icon;
    }

    public boolean hasDescription() {
        for(int i = 0; i < description.length; i++) {
            if(!description[i].isBlank())
                return true;
        }

        return false;
    }
}
