package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.lang.LangID;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

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
        else
            d0 = new BigDecimal(n0.bd.toString());

        if (n1.raw != null)
            d1 = new BigDecimal(n1.raw);
        else
            d1 = new BigDecimal(n1.bd.toString());

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

                    return new Number("0");
                }

                return new Number(d0.divide(d1, Equation.context));
            case SQUARE:
                if(d1.compareTo(new BigDecimal(d1.toBigInteger())) == 0) {
                    try {
                        BigDecimal target = d1.abs();
                        BigDecimal max = BigDecimal.valueOf(999999998);

                        BigDecimal result = new BigDecimal("1");

                        while (target.compareTo(BigDecimal.ZERO) > 0) {
                            if(target.compareTo(max) > 0) {
                                result = result.multiply(d0.pow(max.intValue(), MathContext.UNLIMITED));
                                target = target.subtract(max);
                            } else {
                                result = result.multiply(d0.pow(target.intValue(), MathContext.UNLIMITED));
                                target = target.subtract(target);
                            }
                        }

                        if(d1.compareTo(BigDecimal.ZERO) < 0) {
                            result = result.pow(-1, Equation.context);
                        }

                        return new Number(result);
                    } catch (ArithmeticException e) {
                        e.printStackTrace();
                        Equation.error.add(LangID.getStringByID("calc_outofrange", lang));

                        return new Number(0);
                    }
                } else {
                    double check = Math.pow(n0.bd.doubleValue(), n1.bd.doubleValue());

                    if(Double.isFinite(check)) {
                        return new Number(check);
                    } else {
                        Equation.error.add(LangID.getStringByID("calc_outofrange", lang));

                        return new Number(0);
                    }
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
