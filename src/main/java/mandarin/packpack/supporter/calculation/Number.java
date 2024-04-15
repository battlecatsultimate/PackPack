package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.bc.DataToString;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;

public class Number extends Element {
    public static final BigDecimal cutOff = BigDecimal.valueOf(1E-128);

    public final String raw;
    @Nonnull
    public final BigDecimal bd;

    public Number(double value) {
        this.bd = new BigDecimal(value);
        this.raw = null;
    }

    public Number(@Nonnull String raw) {
        this.raw = raw;

        BigDecimal test = new BigDecimal(raw);

        if(test.abs().compareTo(cutOff) < 0) {
            this.bd = BigDecimal.valueOf(0);
        } else {
            this.bd = test;
        }
    }

    public Number(@Nonnull BigDecimal bd) {
        if(bd.abs().compareTo(cutOff) < 0) {
            this.bd = BigDecimal.valueOf(0);
            this.raw = "0";
        } else {
            this.bd = bd;
            this.raw = bd.toString();
        }
    }

    public Number(@Nonnull BigInteger bi) {
        this.raw = bi.toString();
        this.bd = new BigDecimal(bi);
    }

    @Override
    public String toString() {
        return DataToString.df.format(bd.doubleValue());
    }
}
