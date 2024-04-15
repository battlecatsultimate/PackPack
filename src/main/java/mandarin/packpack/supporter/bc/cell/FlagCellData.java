package mandarin.packpack.supporter.bc.cell;

import javax.annotation.Nonnull;

public record FlagCellData(@Nonnull String name, int index) {

    public String dataToString(String[] data) {
        int ind = index;

        if (ind < 0 || ind >= data.length)
            ind = 0;

        if (data[ind].strip().equals("0"))
            return "";
        else
            return name;
    }

    @Override
    public String toString() {
        return "FlagCellData{" +
                "name='" + name + '\'' +
                ", index=" + index +
                '}';
    }
}
