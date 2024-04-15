package mandarin.packpack.supporter.bc;

import common.system.fake.FakeImage;

import java.util.List;

public record CustomCombo(String title, String description, List<FakeImage> icons, List<String> names, String type, String level) {
    public CustomCombo(String title, String description, List<FakeImage> icons, List<String> names, String type, String level) {
        this.title = title;
        this.description = description;
        this.icons = icons;
        this.names = names;
        this.type = type;
        this.level = level;

        if (icons.size() != names.size()) {
            throw new IllegalStateException("E/CustomCombo - Size of icons and size of names aren't synchronized!");
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<FakeImage> getIcons() {
        return icons;
    }

    public List<String> getNames() {
        return names;
    }

    public String getType() {
        return type;
    }

    public String getLevel() {
        return level;
    }
}
