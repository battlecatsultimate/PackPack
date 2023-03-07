import mandarin.packpack.supporter.calculation.Formula;

import java.math.BigDecimal;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    private static final double indicatorLengthRatio = 0.025;

    public static void main(String[] args) throws Exception {
        String f = "2*x";

        Formula formula = new Formula(f, 1, 0);

        Formula.error.clear();

        System.out.println("Formula : " + f);
        System.out.println(formula.element.printTree(""));
        System.out.println("f(2) = " + formula.substitute(new BigDecimal("2")));
    }
}
