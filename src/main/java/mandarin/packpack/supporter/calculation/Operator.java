package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.lang.LangID;

import java.math.BigDecimal;
import java.math.MathContext;

public class Operator extends Element {
    public enum TYPE {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,
        SQUARE
    }

    public final TYPE type;

    public Operator(TYPE type) {
        super(false);

        this.type = type;
    }

    public Number calculate(Number n0, Number n1, int lang) {
        BigDecimal d0;
        BigDecimal d1;

        if (n0.raw != null)
            d0 = new BigDecimal(n0.raw);
        else if(n0.bd != null)
            d0 = new BigDecimal(n0.bd.toString());
        else
            d0 = new BigDecimal(n0.value);

        if (n1.raw != null)
            d1 = new BigDecimal(n1.raw);
        else if(n1.bd != null)
            d1 = new BigDecimal(n1.bd.toString());
        else
            d1 = new BigDecimal(n1.value);

        switch (type) {
            case ADDITION:
                return new Number(d0.add(d1));
            case SUBTRACTION:
                return new Number(d0.subtract(d1));
            case MULTIPLICATION:
                return new Number(d0.multiply(d1));
            case DIVISION:
                if(d1.doubleValue() == 0) {
                    Equation.error.add(LangID.getStringByID("calc_division0", lang));

                    return new Number(0, "0");
                }

                return new Number(d0.divide(d1, MathContext.DECIMAL64));
            case SQUARE:
                if(d1.equals(new BigDecimal(d1.intValue())) && Math.abs(d1.intValue()) <= 999999999) {
                    return new Number(d0.pow(d1.intValue(), MathContext.DECIMAL64));
                } else {
                    return new Number(Math.pow(n0.value, n1.value));
                }
            default:
                throw new IllegalStateException("Invalid operator type : " + type);
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case ADDITION:
                return "+";
            case SUBTRACTION:
                return "-";
            case MULTIPLICATION:
                return "*";
            case DIVISION:
                return "/";
            default:
                return "^";
        }
    }
}
