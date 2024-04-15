package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;

import javax.annotation.Nonnull;
import java.util.Arrays;

public record AbilityData(@Nonnull int[] indexes, @Nonnull String format, @Nonnull boolean[] isTime, int ignoreIndex) {
    public AbilityData {
        if (indexes.length != isTime.length) {
            throw new IllegalStateException("Indexes and isTime must be synchronized!\nIndexes : " + Arrays.toString(indexes) + "\nIsTime : " + Arrays.toString(isTime));
        }

    }

    public String beautify(String[] data, boolean isFrame) {
        if (data[ignoreIndex].strip().equals("0"))
            return "";

        String result = format;

        for (int i = 0; i < indexes.length; i++) {
            String replace = "_" + i + "_";

            int ind = indexes[i];

            if (ind < 0 || ind > data.length)
                ind = 0;

            if (isTime[i]) {
                result = result.replace(replace, parseTime(data[ind], isFrame));
            } else {
                result = result.replace(replace, data[ind]);
            }
        }

        return result;
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
        return "AbilityData{" +
                "indexes=" + Arrays.toString(indexes) +
                ", format='" + format + '\'' +
                ", isTime=" + Arrays.toString(isTime) +
                '}';
    }
}
