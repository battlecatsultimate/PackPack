package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.lang.LangID;

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

    public double calculate(Number n0, Number n1, int lang) {
        switch (type) {
            case ADDITION:
                return n0.value + n1.value;
            case SUBTRACTION:
                return n0.value - n1.value;
            case MULTIPLICATION:
                return n0.value * n1.value;
            case DIVISION:
                if(n1.value == 0) {
                    Equation.error.add(LangID.getStringByID("calc_division0", lang));

                    return 0;
                }

                return n0.value / n1.value;
            case SQUARE:
                return Math.pow(n0.value, n1.value);
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
