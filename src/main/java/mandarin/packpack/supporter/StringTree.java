package mandarin.packpack.supporter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class StringTree {
    public final String[] data;
    public final List<StringTree> children = new ArrayList<>();

    public StringTree(String[] data) {
        this.data = data;
    }

    public void addChild(@Nonnull StringTree child) {
        children.add(child);
    }
}
