package mandarin.packpack.supporter.calculation.nested;

import mandarin.packpack.supporter.calculation.Formula;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class NestedElement {
    @Nullable
    public NestedElement parent = null;
    @Nonnull
    public Formula formula;

    public boolean aborted = false;

    protected boolean critical = false;

    public NestedElement(@Nonnull Formula formula) {
        this.formula = formula;
    }

    protected List<NestedElement> children = new ArrayList<>();

    @Nullable
    public BigDecimal calculate(BigDecimal... input) {
        if (critical) {
            broken();

            return null;
        }

        if(aborted) {
            abort();

            return null;
        }

        return perform(input);
    }

    public NestedElement injectVariableFast(Formula newFormula, double input, NestedVariable variable) {
        NestedElement prepare = copy(newFormula);

        if(prepare instanceof NestedVariable && prepare.equals(variable)) {
            NestedNumber number = new NestedNumber(newFormula, BigDecimal.valueOf(input));
            number.parent = prepare.parent;

            return number;
        } else {
            for(int i = 0; i < prepare.children.size(); i++) {
                if(prepare.children.get(i).equals(variable)) {
                    NestedNumber number = new NestedNumber(newFormula, BigDecimal.valueOf(input));
                    number.parent = prepare;

                    prepare.children.set(i, number);
                } else {
                    NestedElement injected = prepare.children.get(i).injectVariableFast(newFormula, input, variable);
                    injected.parent = prepare;

                    prepare.children.set(i, injected);
                }
            }
        }

        return prepare;
    }

    public double calculateFast(double... input) {
        if (critical) {
            broken();

            return 0;
        }

        if(aborted) {
            abort();

            return 0;
        }

        return performFast(input);
    }

    protected abstract BigDecimal perform(BigDecimal... input);

    protected abstract double performFast(double... input);

    public void abort() {
        if(parent != null) {
            parent.aborted = true;
            aborted = false;
        }
    }

    public void broken() {
        critical = true;

        if(parent != null)
            parent.critical = true;
    }

    public boolean isCritical() {
        return critical;
    }

    public boolean isAborted() {
        boolean backup = aborted;
        aborted = false;

        return backup;
    }

    public void addChild(NestedElement element) {
        element.parent = this;

        children.add(element);
    }

    protected boolean check() {
        if (critical) {
            broken();

            return false;
        }

        if(aborted) {
            abort();

            return false;
        }

        return true;
    }

    protected abstract NestedElement copy(Formula newFormula);

    public abstract String printTree(String tab);
}
