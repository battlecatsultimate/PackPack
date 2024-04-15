package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;

import javax.annotation.Nonnull;
import java.util.Arrays;

public record CellData(@Nonnull String name, boolean oneLine, int[] index, boolean isTimeUnit, boolean ignoreZero) {

    public String dataToString(String[] data, boolean isFrame) {
        StringBuilder cont = new StringBuilder();

        for (int k = 0; k < index.length; k++) {
            int ind;

            if (index[k] < 0 || index[k] >= data.length)
                ind = 0;
            else
                ind = index[k];

            if (data[ind].strip().equals("0") && ignoreZero) {
                continue;
            } else {
                if (isTimeUnit) {
                    cont.append(parseTime(data[ind], isFrame));
                } else {
                    cont.append(data[ind]);
                }
            }

            if (k < index.length - 1) {
                cont.append(" / ");
            }
        }

        return cont.toString();
    }

    private String parseTime(String v, boolean isFrame) {
        if (StaticStore.isNumeric(v)) {
            int iv = StaticStore.safeParseInt(v);

            if (isFrame) {
                return iv + "f";
            } else {
                return DataToString.df.format(iv / 30.0) + "s";
            }
        }

        return "Not a number";
    }

    @Override
    public String toString() {
        return "CellData{" +
                "name='" + name + '\'' +
                ", oneLine=" + oneLine +
                ", index=" + Arrays.toString(index) +
                ", isTimeUnit=" + isTimeUnit +
                '}';
    }

    public boolean isOneLine() {
        return oneLine;
    }

    public String getName() {
        return name;
    }
}
