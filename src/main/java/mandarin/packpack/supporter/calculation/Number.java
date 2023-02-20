package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.bc.DataToString;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;

public class Number extends Element {
    public final String raw;
    @Nonnull
    public final BigDecimal bd;

    public Number(double value) {
        super(true);

        this.bd = new BigDecimal(value);
        this.raw = null;
    }

    public Number(@Nonnull String raw) {
        super(true);

        this.raw = raw;
        this.bd = new BigDecimal(raw);
    }

    public Number(@Nonnull BigDecimal bd) {
        super(true);

        this.raw = bd.toString();
        this.bd = bd;
    }

    public Number(@Nonnull BigInteger bi) {
        super(true);

        this.raw = bi.toString();
        this.bd = new BigDecimal(bi);
    }

    @Override
    public String toString() {
        return DataToString.df.format(bd.doubleValue());
    }
}
