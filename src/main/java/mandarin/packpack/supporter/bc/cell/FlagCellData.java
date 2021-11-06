package mandarin.packpack.supporter.bc.cell;

import javax.annotation.Nonnull;

public class FlagCellData {
    @Nonnull
    public final String name;
    public final int index;

    public FlagCellData(@Nonnull String name, int index) {
        this.name = name;
        this.index = index;
    }

    public String dataToString(String[] data) {
        int ind = index;

        if(ind < 0 || ind >= data.length)
            ind = 0;

        if(data[ind].strip().equals("0"))
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
