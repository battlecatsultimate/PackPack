package mandarin.packpack.supporter.calculation.nested;

import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

public class NestedNumber extends NestedElement {
    @Nonnull
    public final BigDecimal value;
    public final double v;

    public NestedNumber(@Nonnull Formula formula, @Nonnull BigDecimal value) {
        super(formula);

        this.value = value;
        v = value.doubleValue();
    }

    @Override
    public BigDecimal perform(BigDecimal... input) {
        return value;
    }

    @Override
    protected double performFast(double... input) {
        return v;
    }

    @Override
    protected NestedElement copy(Formula newFormula) {
        return new NestedNumber(newFormula, value);
    }

    @Override
    public String printTree(String tab) {
        return tab + "Number : " + Equation.formatNumber(value) + " -> " + aborted;
    }

    @Override
    public String toString() {
        return "NestedNumber{" +
                "v=" + DataToString.df.format(v) +
                '}';
    }
}
