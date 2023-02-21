import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    public static void main(String[] args) throws Exception {
        String formula = "3x^4 + 10x^3 - 2x^2 + 10x - 54";

        Formula f = new Formula(formula, 0);
        String input = "5";

        System.out.println("Formula : " + formula);
        System.out.println("Found variable : " + f.variable.name);
        System.out.println("Input : " + input);
        System.out.println("Substitution : " + f.substitute(input, 0));
        System.out.println("Result : " + Equation.calculate(f.substitute(input, 0), null,  false,0));
    }
}
