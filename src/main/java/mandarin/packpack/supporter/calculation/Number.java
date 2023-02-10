package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.bc.DataToString;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

public class Number extends Element {
    public final double value;
    public final String raw;
    public final BigDecimal bd;

    public Number(double value) {
        super(true);

        this.value = value;
        this.raw = null;
        this.bd = null;
    }

    public Number(double value, @Nonnull String raw) {
        super(true);

        this.value = value;
        this.raw = raw;
        this.bd = new BigDecimal(raw);
    }

    public Number(@Nonnull BigDecimal bd) {
        super(true);

        this.value = bd.doubleValue();
        this.raw = bd.toString();
        this.bd = bd;
    }

    @Override
    public String toString() {
        return DataToString.df.format(value);
    }
}
