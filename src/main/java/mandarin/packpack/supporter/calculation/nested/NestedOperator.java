package mandarin.packpack.supporter.calculation.nested;

import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.calculation.Operator;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.MathContext;

public class NestedOperator extends NestedElement {
    @Nonnull
    public final Operator.TYPE type;

    public NestedOperator(@Nonnull Formula formula, @Nonnull Operator.TYPE type) {
        super(formula);

        this.type = type;
    }

    @Override
    public BigDecimal perform(BigDecimal... input) {
        BigDecimal d0 = children.getFirst().calculate(input);
        BigDecimal d1 = children.get(1).calculate(input);

        if(d0 == null || d1 == null) {
            abort();

            return BigDecimal.ZERO;
        }

        if(aborted) {
            abort();

            return BigDecimal.ZERO;
        }

        if(critical) {
            broken();

            return BigDecimal.ZERO;
        }

        switch (type) {
            case ADDITION:
                return d0.add(d1);
            case SUBTRACTION:
                return d0.subtract(d1);
            case MULTIPLICATION:
                return d0.multiply(d1);
            case DIVISION:
                if(d1.doubleValue() == 0) {
                    abort();

                    return BigDecimal.ZERO;
                }

                return d0.divide(d1, Equation.context);
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
                            if(result.compareTo(BigDecimal.ZERO) == 0) {
                                abort();

                                return BigDecimal.ZERO;
                            }

                            result = result.pow(-1, Equation.context);
                        }

                        return result;
                    } catch (ArithmeticException ignored) {
                        abort();

                        return BigDecimal.ZERO;
                    }
                } else {
                    double check = Math.pow(d0.doubleValue(), d1.doubleValue());

                    if(Double.isFinite(check)) {
                        return BigDecimal.valueOf(check);
                    } else {
                        abort();

                        return BigDecimal.ZERO;
                    }
                }
            default:
                throw new IllegalStateException("Invalid operator type : " + type);
        }
    }

    @Override
    protected double performFast(double... input) {
        double d0 = children.getFirst().calculateFast(input);
        double d1 = children.get(1).calculateFast(input);

        if(aborted) {
            abort();

            return 0;
        }

        if(critical) {
            broken();

            return 0;
        }

        switch (type) {
            case ADDITION:
                return d0 + d1;
            case SUBTRACTION:
                return d0 - d1;
            case MULTIPLICATION:
                return d0 * d1;
            case DIVISION:
                if(d1 == 0) {
                    abort();

                    return 0;
                }

                return d0 / d1;
            case SQUARE:
                double check = Math.pow(d0, d1);

                if(Double.isFinite(check)) {
                    return check;
                } else {
                    abort();

                    return 0;
                }
            default:
                throw new IllegalStateException("Invalid operator type : " + type);
        }
    }

    @Override
    protected NestedElement copy(Formula newFormula) {
        NestedOperator operator = new NestedOperator(newFormula, type);

        for(int i = 0; i < children.size(); i++) {
            operator.addChild(children.get(i).copy(newFormula));
        }

        return operator;
    }

    @Override
    public String toString() {
        return "NestedOperator{" +
                "type=" + type +
                '}';
    }

    @Override
    public String printTree(String tab) {
        StringBuilder builder = new StringBuilder();

        builder.append(tab).append("Operator : ").append(type).append(" -> ").append(aborted).append("\n");

        String downTab = tab.replace("├", "│").replace("└", " ");

        for(int i = 0; i < children.size(); i++) {
            builder.append(children.get(i).printTree(downTab + (i == children.size() - 1 ? " └ " : " ├ ")));

            if(i < children.size() - 1)
                builder.append("\n");
        }

        return builder.toString();
    }
}
