package mandarin.packpack.supporter.calculation;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.calculation.nested.*;
import mandarin.packpack.supporter.lang.LangID;
import org.jetbrains.annotations.Range;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Formula {
    public enum ROOT {
        SMART,
        NEWTON_RAPHSON,
        FALSE_POSITION,
        SECANT,
        BISECTION
    }

    public enum INTEGRATION {
        SMART,
        BOOLE,
        SIMPSON38,
        SIMPSON,
        TRAPEZOIDAL
    }

    public enum SNAP {
        FRONT,
        CENTER,
        BACK
    }

    public static final List<String> error = new ArrayList<>();

    public static final BigDecimal H = BigDecimal.ONE.divide(BigDecimal.TEN.pow(8), Equation.context);
    public static final int maximumIteration = 100;
    public static final BigDecimal minimumError = new BigDecimal("0.001");
    public static final int maximumSections = 1000;
    public static final int numberOfElements = 2048;

    private static final char[] operators = {
            '+', '-', '/', '*', '×', '÷', '^'
    };

    public static String getErrorMessage() {
        StringBuilder builder = new StringBuilder();

        List<String> realError = new ArrayList<>();

        for(String e : error) {
            if(!realError.contains(e))
                realError.add(e);
        }

        for(int i = 0; i < realError.size(); i++) {
            builder.append(realError.get(i));

            if(i < realError.size() - 1) {
                builder.append("\n\n");
            }
        }

        error.clear();

        return builder.toString();
    }

    public final int maxVariable;
    @Nonnull
    public List<NestedVariable> variable = new ArrayList<>();

    public NestedElement element;

    private String full;
    private String retry;
    
    public Formula(String formula, @Range(from = 1, to = Integer.MAX_VALUE) int maxVariable, CommonStatic.Lang.Locale lang) {
        String stabilized = formula
                .replaceAll("[\\[{]", "(")
                .replaceAll("[]}]", ")")
                .replaceAll("\\)\\(", ")*(")
                .replaceAll("\\|\\(", "|*(")
                .replaceAll("\\)\\|", ")*|")
                .replaceAll("\\s", "")
                .toLowerCase(Locale.ENGLISH).trim();

        String[] test = stabilized.split("=");

        if (test.length == 2) {
            stabilized = test[0] + "-(" + test[1] + ")";
        }

        if(stabilized.length() >= 2 && stabilized.substring(0, 2).matches("^-[^.\\d]")) {
            stabilized = "-1*" + stabilized.substring(1);
        }

        this.maxVariable = maxVariable;

        full = stabilized;

        NestedElement e = analyze(stabilized, lang);

        while(retry != null) {
            error.clear();
            variable.clear();

            String backup = full = retry;

            retry = null;

            e = analyze(backup, lang);
        }

        if(e == null) {
            error.add(LangID.getStringByID("calculator.failed.invalidFormula", lang));

            element = new NestedNumber(this, BigDecimal.ZERO);
        } else {
            element = e;
        }

        boolean xy = variable.size() == 2;

        for(int i = 0; i < variable.size(); i++) {
            if(!variable.get(i).name.equals("x") && !variable.get(i).name.equals("y")) {
                xy = false;

                break;
            }
        }

        if(xy && !variable.getFirst().name.equals("x")) {
            List<NestedVariable> changed = new ArrayList<>();

            changed.add(variable.get(1));
            changed.add(variable.getFirst());

            variable = changed;
        }

        if (test.length == 2 && variable.size() == 1) {
            if (variable.getFirst().name.equals("x")) {
                variable.add(new NestedVariable(this, "y"));
            } else {
                variable.addFirst(new NestedVariable(this, "x"));
            }
        }

        boolean rt = variable.size() == 2;

        for(int i = 0; i < variable.size(); i++) {
            if(!variable.get(i).name.equals("r") && !variable.get(i).name.equals("t")) {
                rt = false;

                break;
            }
        }

        if(rt && !variable.getFirst().name.equals("r")) {
            List<NestedVariable> changed = new ArrayList<>();

            changed.add(variable.get(1));
            changed.add(variable.getFirst());

            variable = changed;
        }

        if (test.length == 2 && variable.size() == 1) {
            if (variable.getFirst().name.equals("r")) {
                variable.add(new NestedVariable(this, "t"));
            } else {
                variable.addFirst(new NestedVariable(this, "r"));
            }
        }
    }

    public Formula(int maxVariable) {
        this.maxVariable = maxVariable;
    }

    public Formula getInjectedFormula(double value, int index) {
        NestedVariable v;

        if(index < 0 || index >= variable.size())
            v = null;
        else
            v = variable.get(index);

        Formula formula = new Formula(maxVariable - 1);

        formula.element = element.injectVariableFast(formula, value, v);

        if(formula.element == null)
            return null;

        List<NestedVariable> changed = new ArrayList<>();

        for(int i = 0; i < variable.size(); i++) {
            if(i != index)
                changed.add(variable.get(i));
        }

        formula.variable = changed;

        return formula;
    }

    @Nullable
    public BigDecimal substitute(BigDecimal... equation) {
        if (variable.isEmpty()) {
            return element.calculate();
        }

        if (equation.length != variable.size()) {
            throw new IllegalStateException("Desynced number of variable and substitution : " + equation.length + " -> " + variable.size() + " : " + variable);
        }

        return element.calculate(equation);
    }

    public double substitute(double... equation) {
        if (variable.isEmpty()) {
            return element.calculateFast();
        }

        if (equation.length != variable.size()) {
            throw new IllegalStateException("Desynced number of variable and substitution : " + equation.length + " -> " + variable.size() + " : " + variable);
        }

        return element.calculateFast(equation);
    }

    public NumericalResult solveByIteration(BigDecimal startPoint, BigDecimal endPoint, int numberOfIteration, ROOT ROOT, CommonStatic.Lang.Locale lang) {
        BigDecimal err = BigDecimal.TEN.multiply(BigDecimal.TEN);
        BigDecimal pre;

        switch (ROOT) {
            case NEWTON_RAPHSON:
                for(int i = 0; i < numberOfIteration; i++) {
                    pre = startPoint;

                    BigDecimal y = substitute(startPoint);

                    if(!Equation.error.isEmpty() || y == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.newtonRaphson", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if(y.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(pre, BigDecimal.ZERO, ROOT);
                    }

                    startPoint = startPoint.subtract(y.divide(differentiate(startPoint, H, SNAP.CENTER, lang), Equation.context));

                    if(startPoint.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(startPoint).divide(startPoint, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }
                }

                return new NumericalResult(startPoint, err, ROOT);
            case FALSE_POSITION:
                for(int i = 0; i < numberOfIteration; i++) {
                    pre = endPoint;

                    BigDecimal ey = substitute(endPoint);

                    if(!Equation.error.isEmpty() || ey == null) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calculator.failed.solve.falsePosition", lang));

                        return null;
                    }

                    if(ey.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(endPoint, BigDecimal.ZERO, ROOT);
                    }

                    BigDecimal sy = substitute(startPoint);

                    if(!Equation.error.isEmpty() || sy == null) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calculator.failed.solve.falsePosition", lang));

                        return null;
                    }

                    endPoint = startPoint.multiply(ey).subtract(endPoint.multiply(sy)).divide(ey.subtract(sy), Equation.context);

                    if(pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(endPoint).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }
                }

                return new NumericalResult(endPoint, err, ROOT);
            case SECANT:
                for(int i = 0; i < numberOfIteration; i++) {
                    pre = endPoint;

                    BigDecimal ey = substitute(endPoint);

                    if(!Equation.error.isEmpty() || ey == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.secant", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if(ey.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(endPoint, BigDecimal.ZERO, ROOT);
                    }

                    BigDecimal sy = substitute(startPoint);

                    if(!Equation.error.isEmpty() || sy == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.secant", lang));

                        Equation.error.clear();

                        return null;
                    }

                    BigDecimal r = endPoint.subtract(ey.multiply(endPoint.subtract(startPoint).divide(ey.subtract(sy), Equation.context)));

                    startPoint = endPoint;
                    endPoint = r;

                    if(pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(r).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }
                }

                return new NumericalResult(endPoint, err, ROOT);
            case BISECTION:
                BigDecimal m = BigDecimal.ZERO;

                for(int i = 0; i < numberOfIteration; i++) {
                    pre = m;

                    m = endPoint.add(startPoint).divide(BigDecimal.valueOf(2), Equation.context);

                    BigDecimal fm = substitute(m);

                    if(!Equation.error.isEmpty() || fm == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.bisection", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if(fm.compareTo(BigDecimal.ZERO) == 0)
                        return new NumericalResult(m, BigDecimal.ZERO, ROOT);

                    BigDecimal fs = substitute(startPoint);

                    if(!Equation.error.isEmpty() || fs == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.bisection", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if (fm.signum() == fs.signum())
                        startPoint = m;
                    else
                        endPoint = m;

                    if(pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(m).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }
                }

                return new NumericalResult(m, err, ROOT);
            case SMART:
                NumericalResult trial = solveByIteration(startPoint, endPoint, numberOfIteration, Formula.ROOT.NEWTON_RAPHSON, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                trial = solveByIteration(startPoint, endPoint, numberOfIteration, Formula.ROOT.BISECTION, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                error.add(LangID.getStringByID("calculator.failed.solve.unknown", lang));

                return null;
            default:
                throw new IllegalStateException("Invalid algorithm : " + ROOT);
        }
    }

    public NumericalResult solveByError(BigDecimal startPoint, BigDecimal endPoint, BigDecimal endError, ROOT ROOT, CommonStatic.Lang.Locale lang) {
        BigDecimal err = BigDecimal.TEN.multiply(BigDecimal.TEN);
        BigDecimal pre;

        int iteration = 0;

        switch (ROOT) {
            case NEWTON_RAPHSON -> {
                while (iteration < maximumIteration) {
                    pre = startPoint;

                    BigDecimal y = substitute(startPoint);

                    if (!Equation.error.isEmpty() || y == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.newtonRaphson", lang));

                        Equation.error.clear();

                        return null;
                    }

                    BigDecimal ddx = differentiate(startPoint, H, SNAP.CENTER, lang);

                    if (ddx != null && ddx.compareTo(BigDecimal.ZERO) != 0) {
                        startPoint = startPoint.subtract(y.divide(ddx, Equation.context));
                    }

                    if (startPoint.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(startPoint).divide(startPoint, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }

                    if (err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                NumericalResult result = new NumericalResult(startPoint, err, ROOT);

                if (iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calculator.failed.solve.unknown", lang), Equation.formatNumber(err));
                }

                return result;
            }
            case FALSE_POSITION -> {
                while (iteration < maximumIteration) {
                    pre = endPoint;

                    BigDecimal sy = substitute(startPoint);

                    if (!Equation.error.isEmpty() || sy == null) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calculator.failed.solve.falsePosition", lang));

                        return null;
                    }

                    BigDecimal ey = substitute(endPoint);

                    if (!Equation.error.isEmpty() || ey == null) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calculator.failed.solve.falsePosition", lang));

                        return null;
                    }

                    endPoint = startPoint.multiply(ey).subtract(endPoint.multiply(sy)).divide(ey.subtract(sy), Equation.context);

                    if (pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(endPoint).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }

                    if (err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                NumericalResult result = new NumericalResult(startPoint, err, ROOT);

                if (iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calculator.failed.solve.unknown", lang), Equation.formatNumber(err));
                }

                return result;
            }
            case SECANT -> {
                while (iteration < maximumIteration) {
                    pre = endPoint;

                    BigDecimal ey = substitute(endPoint);

                    if (!Equation.error.isEmpty() || ey == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.secant", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if (ey.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(endPoint, BigDecimal.ZERO, ROOT);
                    }

                    BigDecimal sy = substitute(startPoint);

                    if (!Equation.error.isEmpty() || sy == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.secant", lang));

                        Equation.error.clear();

                        return null;
                    }

                    BigDecimal r = endPoint.subtract(ey.multiply(endPoint.subtract(startPoint).divide(ey.subtract(sy), Equation.context)));

                    startPoint = endPoint;
                    endPoint = r;

                    if (pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(r).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }

                    if (err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                NumericalResult result = new NumericalResult(startPoint, err, ROOT);

                if (iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calculator.failed.solve.unknown", lang), Equation.formatNumber(err));
                }

                return result;
            }
            case BISECTION -> {
                BigDecimal m = BigDecimal.ZERO;

                while (iteration < maximumIteration) {
                    pre = m;
                    m = endPoint.add(startPoint).divide(BigDecimal.valueOf(2), Equation.context);

                    BigDecimal fm = substitute(m);

                    if (!Equation.error.isEmpty() || fm == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.bisection", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if (fm.compareTo(BigDecimal.ZERO) == 0)
                        return new NumericalResult(m, BigDecimal.ZERO, ROOT);

                    BigDecimal fs = substitute(startPoint);

                    if (!Equation.error.isEmpty() || fs == null) {
                        error.add(LangID.getStringByID("calculator.failed.solve.bisection", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if (fm.signum() == fs.signum())
                        startPoint = m;
                    else
                        endPoint = m;

                    if (pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(m).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }

                    if (err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                NumericalResult result = new NumericalResult(startPoint, err, ROOT);

                if (iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calculator.failed.solve.unknown", lang), Equation.formatNumber(err));
                }

                return result;
            }
            case SMART -> {
                NumericalResult trial = solveByError(startPoint, endPoint, endError, Formula.ROOT.NEWTON_RAPHSON, lang);

                if (trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                trial = solveByError(startPoint, endPoint, endError, Formula.ROOT.BISECTION, lang);

                if (trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                error.add(LangID.getStringByID("calculator.failed.solve.unknown", lang));

                return null;
            }
            default -> throw new IllegalStateException("Invalid algorithm : " + ROOT);
        }
    }

    public BigDecimal differentiate(BigDecimal x, BigDecimal h, SNAP snap, CommonStatic.Lang.Locale lang) {
        if(h.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException("h must not be 0");

        switch (snap) {
            case BACK -> {
                BigDecimal fha = substitute(x.subtract(h));

                if (!Equation.error.isEmpty() || fha == null) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calculator.failed.differentiate.backwardDifference", lang));

                    return null;
                }

                BigDecimal fa = substitute(x);

                if (!Equation.error.isEmpty() || fa == null) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calculator.failed.differentiate.backwardDifference", lang));

                    return null;
                }

                return fa.subtract(fha).divide(h, Equation.context);
            }
            case FRONT -> {
                BigDecimal fa = substitute(x);

                if (!Equation.error.isEmpty() || fa == null) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calculator.failed.differentiate.forwardDifference", lang));

                    return null;
                }

                BigDecimal fah = substitute(x.add(h));

                if (!Equation.error.isEmpty() || fah == null) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calculator.failed.differentiate.forwardDifference", lang));

                    return null;
                }

                return fah.subtract(fa).divide(h, Equation.context);
            }
            case CENTER -> {
                BigDecimal fha = substitute(x.subtract(h));

                if (!Equation.error.isEmpty() || fha == null) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calculator.failed.differentiate.centralDifference", lang));

                    return null;
                }

                BigDecimal fah = substitute(x.add(h));

                if (!Equation.error.isEmpty() || fah == null) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calculator.failed.differentiate.centralDifference", lang));

                    return null;
                }

                return fah.subtract(fha).divide(h.multiply(BigDecimal.valueOf(2)), Equation.context);
            }
            default -> throw new IllegalStateException("Invalid snap : " + snap);
        }
    }

    public BigDecimal integrate(BigDecimal start, BigDecimal end, int section, INTEGRATION algorithm, CommonStatic.Lang.Locale lang) {
        if(section <= 0) {
            throw new IllegalStateException("Step size can't be zero");
        }

        switch (algorithm) {
            case SIMPSON:
                if(section % 2 != 0) {
                    error.add(LangID.getStringByID("integration.failed.invalidSectionNumber.simpson1/3", lang));

                    return BigDecimal.ZERO;
                }

                break;
            case SIMPSON38:
                if(section % 3 != 0) {
                    error.add(LangID.getStringByID("integration.failed.invalidSectionNumber.simpson3/8", lang));

                    return BigDecimal.ZERO;
                }

                break;
            case BOOLE:
                if(section % 4 != 0) {
                    error.add(LangID.getStringByID("integration.failed.invalidSectionNumber.boole", lang));

                    return BigDecimal.ZERO;
                }
        }

        BigDecimal[][] coordinates = new BigDecimal[section + 1][];

        BigDecimal h = end.subtract(start).divide(BigDecimal.valueOf(section), Equation.context);

        for(int i = 0; i < section + 1; i++) {
            BigDecimal[] c = new BigDecimal[2];

            BigDecimal x = start.add(h.multiply(BigDecimal.valueOf(i)));

            BigDecimal y = substitute(x);

            if(!Equation.error.isEmpty() || y == null) {
                error.add(LangID.getStringByID("integration.failed.integrationFailed", lang));

                return BigDecimal.ZERO;
            }

            c[0] = x;
            c[1] = y;

            coordinates[i] = c;
        }

        switch (algorithm) {
            case TRAPEZOIDAL -> {
                BigDecimal result = BigDecimal.ZERO;

                for (int i = 0; i < coordinates.length - 1; i++) {
                    result = result.add(h.multiply(coordinates[i][1].add(coordinates[i + 1][1]).divide(BigDecimal.valueOf(2), Equation.context)));
                }

                return result;
            }
            case SIMPSON -> {
                BigDecimal result = BigDecimal.ZERO;

                if ((coordinates.length - 1) / 2 != section / 2) {
                    System.out.println("W/Formula::integrate - Desynced section size : " + ((coordinates.length - 1) / 2) + " | " + (section / 2) + " - " + algorithm);
                }

                for (int i = 0; i < (coordinates.length - 1) / 2; i++) {
                    result = result.add(h.multiply(coordinates[i * 2][1].add(coordinates[i * 2 + 1][1].multiply(BigDecimal.valueOf(4))).add(coordinates[i * 2 + 2][1])).divide(BigDecimal.valueOf(3), Equation.context));
                }

                return result;
            }
            case SIMPSON38 -> {
                BigDecimal result = BigDecimal.ZERO;

                if ((coordinates.length - 1) / 3 != section / 3) {
                    System.out.println("W/Formula::integrate - Desynced section size : " + ((coordinates.length - 1) / 3) + " | " + (section / 3) + " - " + algorithm);
                }

                for (int i = 0; i < (coordinates.length - 1) / 3; i++) {
                    result = result.add(h.multiply(coordinates[i * 3][1].add(coordinates[i * 3 + 1][1].multiply(BigDecimal.valueOf(3))).add(coordinates[i * 3 + 2][1].multiply(BigDecimal.valueOf(3))).add(coordinates[i * 3 + 3][1])).multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(8), Equation.context));
                }

                return result;
            }
            case BOOLE -> {
                BigDecimal result = BigDecimal.ZERO;

                if ((coordinates.length - 1) / 4 != section / 4) {
                    System.out.println("W/Formula::integrate - Desynced section size : " + ((coordinates.length - 1) / 4) + " | " + (section / 4) + " - " + algorithm);
                }

                for (int i = 0; i < (coordinates.length - 1) / 4; i++) {
                    result = result.add(h.multiply(coordinates[i * 4][1].multiply(BigDecimal.valueOf(7)).add(coordinates[i * 4 + 1][1].multiply(BigDecimal.valueOf(32))).add(coordinates[i * 4 + 2][1].multiply(BigDecimal.valueOf(12))).add(coordinates[i * 4 + 3][1].multiply(BigDecimal.valueOf(32))).add(coordinates[i * 4 + 4][1].multiply(BigDecimal.valueOf(7)))).multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(45), Equation.context));
                }

                return result;
            }
            case SMART -> {
                BigDecimal result = BigDecimal.ZERO;

                int index = 0;
                int s = section;

                int boole, simp, simp38, trape;

                boole = s / 4;

                s -= boole * 4;

                simp38 = s / 3;

                s -= simp38 * 3;

                simp = s / 2;

                s -= simp * 2;

                trape = s;

                for (int i = 0; i < boole; i++) {
                    result = result.add(h.multiply(coordinates[index][1].multiply(BigDecimal.valueOf(7)).add(coordinates[index + 1][1].multiply(BigDecimal.valueOf(32))).add(coordinates[index + 2][1].multiply(BigDecimal.valueOf(12))).add(coordinates[index + 3][1].multiply(BigDecimal.valueOf(32))).add(coordinates[index + 4][1].multiply(BigDecimal.valueOf(7)))).multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(45), Equation.context));

                    index += 4;
                }

                for (int i = 0; i < simp38; i++) {
                    result = result.add(h.multiply(coordinates[index][1].add(coordinates[index + 1][1].multiply(BigDecimal.valueOf(3))).add(coordinates[index + 2][1].multiply(BigDecimal.valueOf(3))).add(coordinates[index + 3][1])).multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(8), Equation.context));

                    index += 3;
                }

                for (int i = 0; i < simp; i++) {
                    result = result.add(h.multiply(coordinates[index][1].add(coordinates[index + 1][1].multiply(BigDecimal.valueOf(4))).add(coordinates[index + 2][1])).divide(BigDecimal.valueOf(3), Equation.context));

                    index += 2;
                }

                for (int i = 0; i < trape; i++) {
                    result = result.add(h.multiply(coordinates[index][1].add(coordinates[index + 1][1]).divide(BigDecimal.valueOf(2), Equation.context)));
                }

                return result;
            }
            default -> throw new IllegalStateException("Invalid algorithm : " + algorithm);
        }
    }

    private NestedElement analyze(String equation, CommonStatic.Lang.Locale lang) {
        if(openedBracket(equation)) {
            return null;
        }

        int depth = 0;
        int absDepth = 0;

        for(int i = equation.length() - 1; i >= 0; i--) {
            char ch = equation.charAt(i);

            if((ch == '+' || ch == '-') && depth <= 0 && absDepth <= 0) {
                if(ch == '-' && i == 0)
                    continue;

                NestedElement primary = analyze(equation.substring(0, i), lang);

                if(primary == null)
                    return null;

                NestedElement secondary = analyze(equation.substring(i + 1), lang);

                if(secondary == null)
                    return null;

                NestedOperator operator = new NestedOperator(this, ch == '+' ? Operator.TYPE.ADDITION : Operator.TYPE.SUBTRACTION);

                operator.addChild(primary);
                operator.addChild(secondary);

                return operator;
            } else if(ch == '(') {
                depth--;
            } else if(ch == ')') {
                depth++;
            } else if(ch == '|') {
                if(endOfAbs(equation, i)) {
                    absDepth++;
                } else {
                    absDepth--;
                }
            }
        }

        depth = 0;
        absDepth = 0;

        for(int i = equation.length() - 1; i >= 0; i--) {
            char ch = equation.charAt(i);

            if((ch == '*' || ch == '×' || ch == '/' || ch == '÷') && depth <= 0 && absDepth <= 0) {
                NestedElement primary = analyze(equation.substring(0, i), lang);

                if(primary == null)
                    return null;

                NestedElement secondary = analyze(equation.substring(i + 1), lang);

                if(secondary == null)
                    return null;

                Operator.TYPE type;

                if(ch == '*' || ch == '×') {
                    type = Operator.TYPE.MULTIPLICATION;
                } else {
                    type = Operator.TYPE.DIVISION;
                }

                NestedOperator operator = new NestedOperator(this, type);

                operator.addChild(primary);
                operator.addChild(secondary);

                return operator;
            } else if(ch == '(') {
                depth--;
            } else if(ch == ')') {
                depth++;
            } else if(ch == '|') {
                if(endOfAbs(equation, i)) {
                    absDepth++;
                } else {
                    absDepth--;
                }
            }
        }

        depth = 0;
        absDepth = 0;

        for(int i = 0; i < equation.length(); i++) {
            char ch = equation.charAt(i);

            if(ch == '^' && depth <= 0 && absDepth <= 0) {
                NestedElement primary = analyze(equation.substring(0, i), lang);

                if(primary == null)
                    return null;

                NestedElement secondary = analyze(equation.substring(i + 1), lang);

                if(secondary == null)
                    return null;

                NestedOperator operator = new NestedOperator(this, Operator.TYPE.SQUARE);

                operator.addChild(primary);
                operator.addChild(secondary);

                return operator;
            } else if(ch == '(') {
                depth++;
            } else if(ch == ')') {
                depth--;
            } else if(ch == '|') {
                if(endOfAbs(equation, i)) {
                    absDepth--;
                } else {
                    absDepth++;
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < equation.length(); i++) {
            switch (equation.charAt(i)) {
                case '|':
                    String pre;

                    if(!builder.isEmpty()) {
                        pre = builder.toString();

                        builder.setLength(0);
                    } else {
                        pre = null;
                    }

                    i++;

                    int start = i;

                    int max = 1;
                    int level = 1;
                    int last = i;

                    while (i < equation.length()) {
                        if (endOfAbs(equation, i)) {
                            level--;

                            if (level < max) {
                                last = i;
                            }

                            max = Math.min(max, level);
                        } else if (equation.charAt(i) == '|') {
                            level++;
                        }

                        i++;
                    }

                    if(start == last) {
                        return new NestedNumber(this, BigDecimal.ZERO);
                    } else {
                        builder.append(equation, start, last);

                        NestedElement test = analyze(builder.toString(), lang);

                        if(test == null) {
                            return null;
                        }

                        NestedElement abs = new NestedFunction(this, NestedFunction.FUNC.ABS);

                        abs.addChild(test);

                        if(pre != null) {
                            NestedElement preTest = analyze(pre, lang);

                            if(preTest == null) {
                                return null;
                            }

                            NestedOperator operator = new NestedOperator(this, Operator.TYPE.MULTIPLICATION);

                            operator.addChild(preTest);
                            operator.addChild(abs);

                            return operator;
                        } else {
                            return abs;
                        }
                    }
                case '(':
                    String prefix;

                    if(!builder.isEmpty()) {
                        prefix = builder.toString();

                        builder.setLength(0);
                    } else {
                        prefix = null;
                    }

                    i++;

                    int open = 0;

                    while(true) {
                        if(i >= equation.length())
                            throw new IllegalStateException("E/Parenthesis::parse - Bracket is opened even though it passed validation\n\nCode : " + equation);

                        if(equation.charAt(i) == '(')
                            open++;

                        if(equation.charAt(i) == ')')
                            if(open != 0)
                                open--;
                            else
                                break;

                        builder.append(equation.charAt(i));

                        i++;
                    }

                    if(prefix != null) {
                        switch (prefix) {
                            case "npr":
                            case "ncr":
                                List<String> data = filterData(builder.toString(), ',');

                                if(data.size() != 2) {
                                    return null;
                                }

                                NestedElement n = analyze(data.getFirst(), lang);

                                if(n == null) {
                                    return null;
                                }

                                NestedElement r = analyze(data.get(1), lang);

                                if(r == null) {
                                    return null;
                                }

                                NestedFunction func = new NestedFunction(this, prefix.equals("npr") ? NestedFunction.FUNC.NPR : NestedFunction.FUNC.NCR);

                                func.addChild(n);
                                func.addChild(r);

                                return func;
                            default:
                                NestedElement inner = analyze(builder.toString(), lang);

                                if(inner == null) {
                                    return null;
                                }

                                if(StaticStore.isNumeric(prefix)) {
                                    NestedOperator operator = new NestedOperator(this, Operator.TYPE.MULTIPLICATION);

                                    NestedNumber number = new NestedNumber(this, new BigDecimal(prefix));

                                    operator.addChild(number);
                                    operator.addChild(inner);

                                    return operator;
                                } else {
                                    NestedFunction.FUNC type;

                                    List<NestedElement> elements = new ArrayList<>();

                                    switch (prefix) {
                                        case "sin":
                                            type = NestedFunction.FUNC.SIN;

                                            break;
                                        case "cos":
                                            type = NestedFunction.FUNC.COS;

                                            break;
                                        case "tan":
                                            type = NestedFunction.FUNC.TAN;

                                            break;
                                        case "csc":
                                            type = NestedFunction.FUNC.CSC;

                                            break;
                                        case "date.second.singular":
                                            type = NestedFunction.FUNC.SEC;

                                            break;
                                        case "cot":
                                            type = NestedFunction.FUNC.COT;

                                            break;
                                        case "ln":
                                        case "loge":
                                            type = NestedFunction.FUNC.LOG;

                                            elements.add(new NestedNumber(this, BigDecimal.valueOf(Math.E)));

                                            break;
                                        case "log":
                                            type = NestedFunction.FUNC.LOG;

                                            elements.add(new NestedNumber(this, BigDecimal.TEN));

                                            break;
                                        case "sqrt":
                                        case "root":
                                        case "sqrt2":
                                            type = NestedFunction.FUNC.SQRT;

                                            elements.add(new NestedNumber(this, BigDecimal.valueOf(2)));

                                            break;
                                        case "exp":
                                            type = NestedFunction.FUNC.EXP;

                                            break;
                                        case "arcsin":
                                        case "asin":
                                            type = NestedFunction.FUNC.ARCSIN;

                                            break;
                                        case "arccos":
                                        case "acos":
                                            type = NestedFunction.FUNC.ARCCOS;

                                            break;
                                        case "arctan":
                                        case "atan":
                                            type = NestedFunction.FUNC.ARCTAN;

                                            break;
                                        case "arccsc":
                                        case "acsc":
                                            type = NestedFunction.FUNC.ARCCSC;

                                            break;
                                        case "arcsec":
                                        case "asec":
                                            type = NestedFunction.FUNC.ARCSEC;

                                            break;
                                        case "arccot":
                                        case "acot":
                                            type = NestedFunction.FUNC.ARCCOT;

                                            break;
                                        case "abs":
                                            type = NestedFunction.FUNC.ABS;

                                            break;
                                        case "sign":
                                        case "sgn":
                                            type = NestedFunction.FUNC.SIGN;

                                            break;
                                        case "floor":
                                            type = NestedFunction.FUNC.FLOOR;

                                            break;
                                        case "ceil":
                                            type = NestedFunction.FUNC.CEIL;

                                            break;
                                        case "round":
                                            type = NestedFunction.FUNC.ROUND;

                                            break;
                                        default:
                                            type = null;
                                    }

                                    if(type != null) {
                                        elements.add(inner);

                                        NestedFunction function = new NestedFunction(this, type);

                                        for(int j = 0; j < elements.size(); j++) {
                                            function.addChild(elements.get(j));
                                        }

                                        return function;
                                    } else {
                                        if(prefix.startsWith("log")) {
                                            String b = prefix.replaceAll("^log", "");

                                            NestedElement base = analyze(b, lang);

                                            if(base == null)
                                                return null;

                                            NestedFunction function = new NestedFunction(this, NestedFunction.FUNC.LOG);

                                            function.addChild(base);
                                            function.addChild(inner);

                                            return function;
                                        } else if(prefix.startsWith("sqrt")) {
                                            String b = prefix.replaceAll("^sqrt", "");

                                            NestedElement base = analyze(b, lang);

                                            if(base == null) {
                                                return null;
                                            }

                                            NestedFunction function = new NestedFunction(this, NestedFunction.FUNC.SQRT);

                                            function.addChild(base);
                                            function.addChild(inner);

                                            return function;
                                        } else {
                                            retry = full.replace(prefix + "(" + builder + ")", prefix + "*(" + builder +")");

                                            return null;
                                        }
                                    }
                                }
                        }
                    }

                    break;
                default:
                    builder.append(equation.charAt(i));
            }
        }

        if(!builder.isEmpty()) {
            String prefix = builder.toString();

            if(StaticStore.isNumeric(prefix)) {
                return new NestedNumber(this, new BigDecimal(prefix));
            } else {
                switch (prefix) {
                    case "pi":
                    case "π":
                        return new NestedNumber(this, BigDecimal.valueOf(Math.PI));
                    case "e":
                        return new NestedNumber(this, BigDecimal.valueOf(Math.E));
                    default:
                        if(prefix.endsWith("!")) {
                            String filtered = prefix.replaceAll("!$", "");

                            NestedElement test = analyze(filtered, lang);

                            if(test == null)
                                return null;

                            NestedFunction function = new NestedFunction(this, NestedFunction.FUNC.FACTORIAL);

                            function.addChild(test);

                            return function;
                        } else if(prefix.matches(".+p.+") || prefix.matches(".+c.+")) {
                            String[] data;

                            if(prefix.matches(".+p.+")) {
                                data = builder.toString().split("p");
                            } else {
                                data = builder.toString().split("c");
                            }

                            if(data.length != 2) {
                                return handleLast(equation, prefix, lang);
                            }

                            NestedElement n = analyze(data[0], lang);

                            if(n == null) {
                                return handleLast(equation, prefix, lang);
                            }

                            NestedElement r = analyze(data[1], lang);

                            if(r == null)
                                return handleLast(equation, prefix, lang);

                            NestedFunction function = new NestedFunction(this, prefix.matches(".+p.+") ? NestedFunction.FUNC.NPR : NestedFunction.FUNC.NCR);

                            function.addChild(n);
                            function.addChild(r);

                            return function;
                        } else if(prefix.matches("^(\\d+)?(\\.\\d+)?[a-z]$")) {
                            String decimal = prefix.replaceAll("[a-z]$", "");
                            String v = prefix.replaceAll("^(\\d+)?(\\.\\d+)?", "");

                            if(StaticStore.isNumeric(decimal)) {
                                retry = full.replace(prefix, decimal + "*" + v);

                                return null;
                            } else {
                                NestedVariable va = new NestedVariable(this, prefix);

                                if(!variable.contains(va)) {
                                    if(variable.size() >= maxVariable) {
                                        error.add(String.format(LangID.getStringByID("calculator.failed.tooManyVariables", lang), va.name, maxVariable, variable.getLast().name));

                                        return null;
                                    } else {
                                        variable.add(va);
                                    }
                                }

                                return va;
                            }
                        } else {
                            return handleLast(equation, prefix, lang);
                        }
                }
            }
        }

        return null;
    }

    private boolean openedBracket(String raw) {
        int open = 0;
        int count = 0;

        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '(')
                open++;
            else if (raw.charAt(i) == ')')
                open--;
            else if (raw.charAt(i) == '|')
                count++;
        }

        return open != 0 || count % 2 == 1;
    }

    @SuppressWarnings("SameParameterValue")
    private List<String> filterData(String raw, char separator) {
        List<String> result = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        int depth = 0;

        for(int i = 0; i < raw.length(); i++) {
            char letter = raw.charAt(i);

            if(letter == '(') {
                depth++;
            } else if(letter == ')') {
                if(depth == 0)
                    return new ArrayList<>();
                else
                    depth--;
            }

            if(letter == separator) {
                if(depth == 0) {
                    result.add(builder.toString());

                    builder.setLength(0);
                } else {
                    builder.append(letter);
                }
            } else {
                builder.append(letter);
            }
        }

        if(!builder.isEmpty())
            result.add(builder.toString());

        return result;
    }

    private boolean endOfAbs(String equation, int index) {
        char c = equation.charAt(index);

        if(c == '|') {
            if(index - 1 < 0) {
                return false;
            }

            char before = equation.charAt(index - 1);

            if(Character.isDigit(before) || before == ')' || before == '|') {
                return true;
            }

            if(isOperator(before))
                return false;

            String previous = equation.substring(0, index);

            return previous.matches("(.+)?(pi|[a-z])$");
        } else {
            return false;
        }
    }

    private boolean isOperator(char ch) {
        for(int i = 0; i < operators.length; i++) {
            if (ch == operators[i])
                return true;
        }

        return false;
    }

    private NestedElement handleLast(String equation, String last, CommonStatic.Lang.Locale lang) {
        if(!equation.equals(last)) {
            NestedElement test = analyze(last, lang);

            if(test != null)
                return test;
        }

        NestedVariable v = new NestedVariable(this, last);

        if(!variable.contains(v)) {
            if(variable.size() >= maxVariable) {
                error.add(String.format(LangID.getStringByID("calculator.failed.tooManyVariables", lang), v.name, maxVariable, variable.getLast().name));

                return null;
            } else {
                variable.add(v);
            }
        }

        return v;
    }
}
