package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.bc.DataToString;

public class Number extends Element {
    public final double value;

    public Number(double value) {
        super(true);

        this.value = value;
    }

    @Override
    public String toString() {
        return DataToString.df.format(value);
    }
}
