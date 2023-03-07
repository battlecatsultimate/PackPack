package mandarin.packpack.supporter.calculation.nested;

import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class NestedFunction extends NestedElement {
    public enum FUNC {
        NPR(2),
        NCR(2),
        SIN(1),
        COS(1),
        TAN(1),
        CSC(1),
        SEC(1),
        COT(1),
        LOG(2),
        SQRT(2),
        EXP(1),
        ARCSIN(1),
        ARCCOS(1),
        ARCTAN(1),
        ARCCSC(1),
        ARCSEC(1),
        ARCCOT(1),
        ABS(1),
        SIGN(1),
        FLOOR(1),
        CEIL(1),
        ROUND(1),
        FACTORIAL(1);

        public int requiredParam;

        FUNC(int requiredParam) {
            this.requiredParam = requiredParam;
        }
    }

    @Nonnull
    public final FUNC type;

    public NestedFunction(@Nonnull Formula formula, @Nonnull FUNC type) {
        super(formula);

        this.type = type;
    }

    @Override
    public BigDecimal perform(BigDecimal... input) {
        BigDecimal primary = children.get(0).calculate(input);

        if(!check() || primary == null)
            return BigDecimal.ZERO;

        switch (type) {
            case NPR:
                BigDecimal secondary = children.get(1).calculate(input);

                if(!check() || secondary == null)
                    return BigDecimal.ZERO;

                return nPr(primary, secondary);
            case NCR:
                secondary = children.get(1).calculate(input);

                if(!check() || secondary == null)
                    return BigDecimal.ZERO;

                return nCr(primary, secondary);
            case SIN:
            case CSC:
                BigDecimal sin = BigDecimal.valueOf(Math.sin(primary.doubleValue()));

                if(type == FUNC.SIN) {
                    return sin;
                } else {
                    if(sin.compareTo(BigDecimal.ZERO) == 0) {
                        abort();

                        return BigDecimal.ZERO;
                    }

                    return BigDecimal.ONE.divide(sin, Equation.context);
                }
            case COS:
            case SEC:
                BigDecimal cos = BigDecimal.valueOf(Math.cos(primary.doubleValue()));

                if(type == FUNC.COS) {
                    return cos;
                } else {
                    if(cos.compareTo(BigDecimal.ZERO) == 0) {
                        abort();

                        return BigDecimal.ZERO;
                    }

                    return BigDecimal.ONE.divide(cos, Equation.context);
                }
            case TAN:
            case COT:
                boolean isPi = primary.remainder(BigDecimal.valueOf(Math.PI)).compareTo(BigDecimal.ZERO) == 0;
                boolean isHalfPi = primary.remainder(BigDecimal.valueOf(Math.PI / 2.0)).compareTo(BigDecimal.ZERO) == 0;

                if(type == FUNC.COT) {
                    if(isPi) {
                        abort();

                        return BigDecimal.ZERO;
                    } else if(isHalfPi) {
                        return BigDecimal.ZERO;
                    }
                } else if(!isPi && isHalfPi) {
                    abort();

                    return BigDecimal.ZERO;
                }

                BigDecimal tan = BigDecimal.valueOf(Math.tan(primary.doubleValue()));

                if(type == FUNC.TAN) {
                    return tan;
                } else {
                    return BigDecimal.ONE.divide(tan, Equation.context);
                }
            case LOG:
                secondary = children.get(1).calculate(input);

                if(!check() || secondary == null)
                    return BigDecimal.ZERO;

                if(primary.min(secondary).compareTo(BigDecimal.ZERO) <= 0) {
                    abort();

                    return BigDecimal.ZERO;
                }

                return BigDecimal.valueOf(Math.log(secondary.doubleValue())).divide(BigDecimal.valueOf(Math.log(primary.doubleValue())), Equation.context);
            case SQRT:
                secondary = children.get(1).calculate(input);

                if(primary.compareTo(BigDecimal.ZERO) == 0) {
                    abort();

                    return BigDecimal.ZERO;
                }

                if(!check() || secondary == null)
                    return BigDecimal.ZERO;

                double check = Math.pow(secondary.doubleValue(), BigDecimal.ONE.divide(primary, Equation.context).doubleValue());

                if(!Double.isFinite(check)) {
                    abort();

                    return BigDecimal.ZERO;
                }

                return BigDecimal.valueOf(check);
            case EXP:
                if(primary.stripTrailingZeros().scale() <= 0 && primary.abs().longValue() < 999999999) {
                    return BigDecimal.valueOf(Math.E).pow(primary.intValue());
                } else {
                    return BigDecimal.valueOf(Math.pow(Math.E, primary.doubleValue()));
                }
            case ARCSIN:
            case ARCCOS:
                if(primary.compareTo(BigDecimal.ONE.negate()) < 0 || primary.compareTo(BigDecimal.ONE) > 0) {
                    abort();

                    return BigDecimal.ZERO;
                }

                if(type == FUNC.ARCSIN) {
                    return BigDecimal.valueOf(Math.asin(primary.doubleValue()));
                } else {
                    return BigDecimal.valueOf(Math.acos(primary.doubleValue()));
                }
            case ARCTAN:
                return BigDecimal.valueOf(Math.atan(primary.doubleValue()));
            case ARCCSC:
            case ARCSEC:
                if(primary.compareTo(BigDecimal.ONE.negate()) > 0 && primary.compareTo(BigDecimal.ONE) < 0) {
                    abort();

                    return BigDecimal.ZERO;
                }

                if(type == FUNC.ARCCSC) {
                    return BigDecimal.valueOf(Math.asin(BigDecimal.ONE.divide(primary, Equation.context).doubleValue()));
                } else {
                    return BigDecimal.valueOf(Math.acos(BigDecimal.ONE.divide(primary, Equation.context).doubleValue()));
                }
            case ARCCOT:
                return BigDecimal.valueOf(Math.atan(BigDecimal.ONE.divide(primary, Equation.context).doubleValue()));
            case ABS:
                return primary.abs();
            case SIGN:
                return BigDecimal.valueOf(primary.signum());
            case FLOOR:
                return new BigDecimal(primary.setScale(0, RoundingMode.FLOOR).unscaledValue());
            case ROUND:
                return new BigDecimal(primary.setScale(0, RoundingMode.HALF_UP).unscaledValue());
            case CEIL:
                return new BigDecimal(primary.setScale(0, RoundingMode.CEILING).unscaledValue());
            case FACTORIAL:
                return factorial(primary);
            default:
                throw new IllegalStateException("Invalid function type : " + type);
        }
    }

    @Override
    protected double performFast(double... input) {
        double primary = children.get(0).calculateFast(input);

        if(!check())
            return 0;

        switch (type) {
            case NPR:
                double secondary = children.get(1).calculateFast(input);

                if(!check())
                    return 0;

                return nPr(primary, secondary);
            case NCR:
                secondary = children.get(1).calculateFast(input);

                if(!check())
                    return 0;

                return nCr(primary, secondary);
            case SIN:
            case CSC:
                double sin = Math.sin(primary);

                if(type == FUNC.SIN) {
                    return sin;
                } else {
                    if(sin == 0) {
                        abort();

                        return 0;
                    }

                    return 1 / sin;
                }
            case COS:
            case SEC:
                double cos = Math.cos(primary);

                if(type == FUNC.COS) {
                    return cos;
                } else {
                    if(cos == 0) {
                        abort();

                        return 0;
                    }

                    return 1 / cos;
                }
            case TAN:
            case COT:
                boolean isPi = primary % Math.PI == 0;
                boolean isHalfPi = primary % (Math.PI / 2.0) == 0;

                if(type == FUNC.COT) {
                    if(isPi) {
                        abort();

                        return 0;
                    } else if(isHalfPi) {
                        return 0;
                    }
                } else if(!isPi && isHalfPi) {
                    abort();

                    return 0;
                }

                double tan = Math.tan(primary);

                if(type == FUNC.TAN) {
                    return tan;
                } else {
                    return 1 / tan;
                }
            case LOG:
                secondary = children.get(1).calculateFast(input);

                if(!check())
                    return 0;

                if(Math.min(primary, secondary) <= 0) {
                    abort();

                    return 0;
                }

                return Math.log(secondary) / Math.log(primary);
            case SQRT:
                if(primary == 0) {
                    abort();

                    return 0;
                }

                secondary = children.get(1).calculateFast(input);

                if(!check())
                    return 0;

                double check = Math.pow(secondary, 1 / primary);

                if(!Double.isFinite(check)) {
                    abort();

                    return 0;
                }

                return check;
            case EXP:
                return Math.exp(primary);
            case ARCSIN:
            case ARCCOS:
                if(primary < -1 || primary > 1) {
                    abort();

                    return 0;
                }

                if(type == FUNC.ARCSIN) {
                    return Math.asin(primary);
                } else {
                    return Math.acos(primary);
                }
            case ARCTAN:
                return Math.atan(primary);
            case ARCCSC:
            case ARCSEC:
                if(primary > -1 && primary < 1) {
                    abort();

                    return 0;
                }

                if(type == FUNC.ARCCSC) {
                    return Math.asin(1 / primary);
                } else {
                    return Math.acos(1 / primary);
                }
            case ARCCOT:
                if(primary == 0) {
                    abort();

                    return 0;
                }

                return Math.atan(1 / primary);
            case ABS:
                return Math.abs(primary);
            case SIGN:
                return Math.signum(primary);
            case FLOOR:
                return Math.floor(primary);
            case ROUND:
                return Math.round(primary);
            case CEIL:
                return Math.ceil(primary);
            case FACTORIAL:
                return factorial(primary);
            default:
                throw new IllegalStateException("Invalid function type : " + type);
        }
    }

    private BigDecimal nPr(BigDecimal n, BigDecimal r) {
        if(n.compareTo(r) < 0) {
            abort();

            return BigDecimal.ZERO;
        }

        return factorial(n).divide(factorial(n.subtract(r)), Equation.context);
    }

    private double nPr(double n, double r) {
        if(n < r) {
            abort();

            return 0;
        }

        return factorial(n) / factorial(n - r);
    }

    private BigDecimal nCr(BigDecimal n, BigDecimal r) {
        if(n.compareTo(r) < 0) {
            abort();

            return BigDecimal.ZERO;
        }

        return factorial(n).divide(factorial(r).multiply(factorial(n.subtract(r))), Equation.context);
    }

    private double nCr(double n, double r) {
        if(n < r) {
            abort();

            return 0;
        }

        return factorial(n) / (factorial(n - r));
    }

    private BigDecimal factorial(BigDecimal n) {
        if(n.compareTo(BigDecimal.ZERO) < 0) {
            abort();

            return BigDecimal.ZERO;
        }

        BigDecimal f = BigDecimal.ONE;

        for(BigDecimal i = new BigDecimal("2"); i.compareTo(n) <= 0; i = i.add(BigDecimal.ONE)) {
            f = f.multiply(i);
        }

        return f;
    }

    private double factorial(double n) {
        if(n < 0) {
            abort();

            return 0;
        }

        double result = 0;

        for(double i = 2; i <= n; i++) {
            result *= i;
        }

        return result;
    }

    @Override
    protected NestedElement copy(Formula newFormula) {
        NestedFunction function = new NestedFunction(newFormula, type);

        for(int i = 0; i < children.size(); i++) {
            function.addChild(children.get(i).copy(newFormula));
        }

        return function;
    }

    @Override
    public String printTree(String tab) {
        StringBuilder builder = new StringBuilder();

        builder.append(tab).append("Function : ").append(type).append(" -> ").append(aborted).append("\n");

        String downTab = tab.replace("├", "│").replace("└", " ");

        for(int i = 0; i < children.size(); i++) {
            builder.append(children.get(i).printTree(downTab + (i == children.size() - 1 ? " └ " : " ├ ")));

            if(i < children.size() - 1)
                builder.append("\n");
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "NestedFunction{" +
                "type=" + type +
                '}';
    }
}
