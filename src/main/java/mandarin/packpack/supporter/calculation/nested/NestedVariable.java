package mandarin.packpack.supporter.calculation.nested;

import mandarin.packpack.supporter.calculation.Formula;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Objects;

public class NestedVariable extends NestedElement {
    @Nonnull
    public final String name;

    public NestedVariable(@Nonnull Formula formula, @Nonnull String name) {
        super(formula);

        this.name = name;
    }

    @Override
    protected BigDecimal perform(BigDecimal... input) {
        int index = formula.variable.indexOf(this);

        if(index >= 0 && index < input.length)
            return input[index];

        return null;
    }

    @Override
    protected double performFast(double... input) {
        int index = formula.variable.indexOf(this);

        if(index >= 0 && index < input.length)
            return input[index];

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        NestedVariable that = (NestedVariable) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    protected NestedElement copy(Formula newFormula) {
        return new NestedVariable(newFormula, name);
    }

    @Override
    public String printTree(String tab) {
        return tab + "Variable : " + name + " -> " + aborted;
    }

    @Override
    public String toString() {
        return "NestedVariable{" +
                "name='" + name + '\'' +
                '}';
    }
}
