package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import org.jetbrains.annotations.Range;

import javax.annotation.Nonnull;
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

    public static List<String> error = new ArrayList<>();

    public static final BigDecimal H = BigDecimal.ONE.divide(BigDecimal.TEN.pow(8), Equation.context);
    public static final int maximumIteration = 100;
    public static final BigDecimal minimumError = new BigDecimal("0.001");
    public static final int maximumSections = 1000;

    private static final char[] operators = {
            '(', ')', '|', '+', '-', '/', '*', '×', '÷', '^', '.', ','
    };

    private static final String[] knownFunction = {
            "pi", "e", "π", "npr", "ncr", "sin", "cos", "tan", "csc", "sec", "cot", "ln", "loge", "log", "sqrt", "root",
            "sqrt2", "exp", "arcsin", "asin", "arccos", "acos", "arctan", "atan", "arccsc", "acsc", "arcsec", "asec",
            "arccot", "acot", "abs", "sign", "sgn", "floor", "ceil", "round", "loge", "logpi"
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

    @Nonnull
    public final String formula;
    public final int maxVariable;
    @Nonnull
    public List<Variable> variable = new ArrayList<>();
    
    public Formula(String formula, @Range(from = 1, to = Integer.MAX_VALUE) int maxVariable, int lang) {
        String stabilized = formula.replaceAll("[\\[{]", "(").replaceAll("[]}]", ")").replaceAll("\\)\\(", ")*(").replaceAll("\\s", "").toLowerCase(Locale.ENGLISH).trim();

        if(analyze(stabilized, lang)) {
            this.formula = stabilized;
        } else {
            this.formula = "0";
        }

        this.maxVariable = maxVariable;
    }

    public String substitute(String[] equation, int lang) {
        if (variable.isEmpty()) {
            return formula;
        }

        if (equation.length != variable.size()) {
            throw new IllegalStateException("Desynced number of variable and substitution : " + equation.length + " -> " + variable.size());
        }

        String f = formula;

        for(int i = 0; i < equation.length; i++) {
            String stabilized = equation[i].replaceAll("[\\[{]", "(").replaceAll("[]}]", ")").replaceAll("\\)\\(", ")*(").replaceAll("\\s", "").toLowerCase(Locale.ENGLISH);

            f = f.replace("sqrt" + variable.get(i).name, "sqrt" + Equation.calculate(stabilized, null, false, lang)).replace("log" + variable.get(i).name, "log" + Equation.calculate(stabilized, null, false, lang)).replace(variable.get(i).name, "(" + stabilized + ")");
        }

        return f;
    }

    public String substitute(String equation, int lang) {
        String stabilized = equation.replaceAll("[\\[{]", "(").replaceAll("[]}]", ")").replaceAll("\\)\\(", ")*(").replaceAll("\\s", "").toLowerCase(Locale.ENGLISH);

        if(!variable.isEmpty()) {
            return formula.replace("sqrt" + variable.get(0).name, "sqrt" + Equation.calculate(stabilized, null, false, lang)).replace("log" + variable.get(0).name, "log" + Equation.calculate(stabilized, null, false, lang)).replace(variable.get(0).name, "(" + stabilized + ")");
        } else {
            return formula;
        }
    }

    public NumericalResult solveByIteration(BigDecimal startPoint, BigDecimal endPoint, int numberOfIteration, ROOT ROOT, int lang) {
        BigDecimal err = BigDecimal.TEN.multiply(BigDecimal.TEN);
        BigDecimal pre;

        switch (ROOT) {
            case NEWTON_RAPHSON:
                for(int i = 0; i < numberOfIteration; i++) {
                    pre = startPoint;

                    BigDecimal y = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(y.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(pre, BigDecimal.ZERO, ROOT);
                    }

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_newtonfail", lang));

                        Equation.error.clear();

                        return null;
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

                    BigDecimal ey = Equation.calculate(substitute(endPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calc_falsefail", lang));

                        return null;
                    }

                    if(ey.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(endPoint, BigDecimal.ZERO, ROOT);
                    }

                    BigDecimal sy = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calc_falsefail", lang));

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

                    BigDecimal ey = Equation.calculate(substitute(endPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_secantfail", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if(ey.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(endPoint, BigDecimal.ZERO, ROOT);
                    }

                    BigDecimal sy = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_secantfail", lang));

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

                    BigDecimal fm = Equation.calculate(substitute(m.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_bisectionfail", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if(fm.compareTo(BigDecimal.ZERO) == 0)
                        return new NumericalResult(m, BigDecimal.ZERO, ROOT);

                    BigDecimal fs = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_bisectionfail", lang));

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

                error.add(LangID.getStringByID("calc_solvefail", lang));

                return null;
            default:
                throw new IllegalStateException("Invalid algorithm : " + ROOT);
        }
    }

    public NumericalResult solveByError(BigDecimal startPoint, BigDecimal endPoint, BigDecimal endError, ROOT ROOT, int lang) {
        BigDecimal err = BigDecimal.TEN.multiply(BigDecimal.TEN);
        BigDecimal pre;

        int iteration = 0;

        switch (ROOT) {
            case NEWTON_RAPHSON:
                while (iteration < maximumIteration) {
                    pre = startPoint;

                    BigDecimal y = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_newtonfail", lang));

                        Equation.error.clear();

                        return null;
                    }

                    BigDecimal ddx = differentiate(startPoint, H, SNAP.CENTER, lang);

                    if(ddx != null && ddx.compareTo(BigDecimal.ZERO) != 0) {
                        startPoint = startPoint.subtract(y.divide(ddx, Equation.context));
                    }

                    if(startPoint.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(startPoint).divide(startPoint, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }

                    if(err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                NumericalResult result = new NumericalResult(startPoint, err, ROOT);

                if(iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calc_solvefail", lang), Equation.formatNumber(err));
                }

                return result;
            case FALSE_POSITION:
                while(iteration < maximumIteration) {
                    pre = endPoint;

                    BigDecimal sy = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calc_falsefail", lang));

                        return null;
                    }

                    BigDecimal ey = Equation.calculate(substitute(endPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        Equation.error.clear();

                        error.add(LangID.getStringByID("calc_falsefail", lang));

                        return null;
                    }

                    endPoint = startPoint.multiply(ey).subtract(endPoint.multiply(sy)).divide(ey.subtract(sy), Equation.context);

                    if(pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(endPoint).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }

                    if(err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                result = new NumericalResult(startPoint, err, ROOT);

                if(iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calc_solvefail", lang), Equation.formatNumber(err));
                }

                return result;
            case SECANT:
                while(iteration < maximumIteration) {
                    pre = endPoint;

                    BigDecimal ey = Equation.calculate(substitute(endPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_secantfail", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if(ey.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(endPoint, BigDecimal.ZERO, ROOT);
                    }

                    BigDecimal sy = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_secantfail", lang));

                        Equation.error.clear();

                        return null;
                    }

                    BigDecimal r = endPoint.subtract(ey.multiply(endPoint.subtract(startPoint).divide(ey.subtract(sy), Equation.context)));

                    startPoint = endPoint;
                    endPoint = r;

                    if(pre.compareTo(BigDecimal.ZERO) != 0) {
                        err = pre.subtract(r).divide(pre, Equation.context).abs().multiply(BigDecimal.TEN.pow(2));
                    }

                    if(err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                result = new NumericalResult(startPoint, err, ROOT);

                if(iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calc_solvefail", lang), Equation.formatNumber(err));
                }

                return result;
            case BISECTION:
                BigDecimal m = BigDecimal.ZERO;

                while(iteration < maximumIteration) {
                    pre = m;
                    m = endPoint.add(startPoint).divide(BigDecimal.valueOf(2), Equation.context);

                    BigDecimal fm = Equation.calculate(substitute(m.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_bisectionfail", lang));

                        Equation.error.clear();

                        return null;
                    }

                    if(fm.compareTo(BigDecimal.ZERO) == 0)
                        return new NumericalResult(m, BigDecimal.ZERO, ROOT);

                    BigDecimal fs = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(!Equation.error.isEmpty()) {
                        error.add(LangID.getStringByID("calc_bisectionfail", lang));

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

                    if(err.compareTo(endError) <= 0) {
                        break;
                    } else {
                        iteration++;
                    }
                }

                result = new NumericalResult(startPoint, err, ROOT);

                if(iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calc_solvefail", lang), Equation.formatNumber(err));
                }

                return result;
            case SMART:
                NumericalResult trial = solveByError(startPoint, endPoint, endError, Formula.ROOT.NEWTON_RAPHSON, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                trial = solveByError(startPoint, endPoint, endError, Formula.ROOT.BISECTION, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                error.add(LangID.getStringByID("calc_solvefail", lang));

                return null;
            default:
                throw new IllegalStateException("Invalid algorithm : " + ROOT);
        }
    }

    public BigDecimal differentiate(BigDecimal x, BigDecimal h, SNAP snap, int lang) {
        if(h.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException("h must not be 0");

        switch (snap) {
            case BACK:
                BigDecimal fha = Equation.calculate(substitute(x.subtract(h).toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffback", lang));

                    return null;
                }

                BigDecimal fa = Equation.calculate(substitute(x.toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffback", lang));

                    return null;
                }

                return fa.subtract(fha).divide(h, Equation.context);
            case FRONT:
                fa = Equation.calculate(substitute(x.toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_difffront", lang));

                    return null;
                }

                BigDecimal fah = Equation.calculate(substitute(x.add(h).toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_difffront", lang));

                    return null;
                }

                return fah.subtract(fa).divide(h, Equation.context);
            case CENTER:
                fha = Equation.calculate(substitute(x.subtract(h).toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffcenter", lang));

                    return null;
                }

                fah = Equation.calculate(substitute(x.add(h).toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffcenter", lang));

                    return null;
                }

                return fah.subtract(fha).divide(h.multiply(BigDecimal.valueOf(2)), Equation.context);
            default:
                throw new IllegalStateException("Invalid snap : " + snap);
        }
    }

    public BigDecimal integrate(BigDecimal start, BigDecimal end, int section, INTEGRATION algorithm, int lang) {
        if(section <= 0) {
            throw new IllegalStateException("Step size can't be zero");
        }

        switch (algorithm) {
            case SIMPSON:
                if(section % 2 != 0) {
                    error.add(LangID.getStringByID("int_simpson", lang));

                    return BigDecimal.ZERO;
                }

                break;
            case SIMPSON38:
                if(section % 3 != 0) {
                    error.add(LangID.getStringByID("int_sipson38", lang));

                    return BigDecimal.ZERO;
                }

                break;
            case BOOLE:
                if(section % 4 != 0) {
                    error.add(LangID.getStringByID("int_boole", lang));

                    return BigDecimal.ZERO;
                }
        }

        BigDecimal[][] coordinates = new BigDecimal[section + 1][];

        BigDecimal h = end.subtract(start).divide(BigDecimal.valueOf(section), Equation.context);

        for(int i = 0; i < section + 1; i++) {
            BigDecimal[] c = new BigDecimal[2];

            BigDecimal x = start.add(h.multiply(BigDecimal.valueOf(i)));

            BigDecimal y = Equation.calculate(substitute(x.toPlainString(), lang), null, false, lang);

            if(!Equation.error.isEmpty()) {
                error.add(LangID.getStringByID("int_fail", lang));

                return BigDecimal.ZERO;
            }

            c[0] = x;
            c[1] = y;

            coordinates[i] = c;
        }

        switch (algorithm) {
            case TRAPEZOIDAL:
                BigDecimal result = BigDecimal.ZERO;

                for(int i = 0; i < coordinates.length - 1; i++) {
                    result = result.add(h.multiply(coordinates[i][1].add(coordinates[i + 1][1]).divide(BigDecimal.valueOf(2), Equation.context)));
                }

                return result;
            case SIMPSON:
                result = BigDecimal.ZERO;

                if((coordinates.length - 1) / 2 != section / 2) {
                    System.out.println("W/Formula::integrate - Desynced section size : " + ((coordinates.length - 1) / 2) + " | " + (section / 2) + " - " + algorithm);
                }

                for(int i = 0; i < (coordinates.length - 1) / 2; i++) {
                    result = result.add(h.multiply(coordinates[i * 2][1].add(coordinates[i * 2 + 1][1].multiply(BigDecimal.valueOf(4))).add(coordinates[i * 2 + 2][1])).divide(BigDecimal.valueOf(3), Equation.context));
                }

                return result;
            case SIMPSON38:
                result = BigDecimal.ZERO;

                if((coordinates.length - 1) / 3 != section / 3) {
                    System.out.println("W/Formula::integrate - Desynced section size : " + ((coordinates.length - 1) / 3) + " | " + (section / 3) + " - " + algorithm);
                }

                for(int i = 0; i < (coordinates.length - 1) / 3; i++) {
                    result = result.add(h.multiply(coordinates[i * 3][1].add(coordinates[i * 3 + 1][1].multiply(BigDecimal.valueOf(3))).add(coordinates[i * 3 + 2][1].multiply(BigDecimal.valueOf(3))).add(coordinates[i * 3 + 3][1])).multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(8), Equation.context));
                }

                return result;
            case BOOLE:
                result = BigDecimal.ZERO;

                if((coordinates.length - 1) / 4 != section / 4) {
                    System.out.println("W/Formula::integrate - Desynced section size : " + ((coordinates.length - 1) / 4) + " | " + (section / 4) + " - " + algorithm);
                }

                for(int i = 0; i < (coordinates.length - 1) / 4; i++) {
                    result = result.add(h.multiply(coordinates[i * 4][1].multiply(BigDecimal.valueOf(7)).add(coordinates[i * 4 + 1][1].multiply(BigDecimal.valueOf(32))).add(coordinates[i * 4 + 2][1].multiply(BigDecimal.valueOf(12))).add(coordinates[i * 4 + 3][1].multiply(BigDecimal.valueOf(32))).add(coordinates[i * 4 + 4][1].multiply(BigDecimal.valueOf(7)))).multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(45), Equation.context));
                }

                return result;
            case SMART:
                result = BigDecimal.ZERO;

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

                for(int i = 0; i < boole; i++) {
                    result = result.add(h.multiply(coordinates[index][1].multiply(BigDecimal.valueOf(7)).add(coordinates[index + 1][1].multiply(BigDecimal.valueOf(32))).add(coordinates[index + 2][1].multiply(BigDecimal.valueOf(12))).add(coordinates[index + 3][1].multiply(BigDecimal.valueOf(32))).add(coordinates[index + 4][1].multiply(BigDecimal.valueOf(7)))).multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(45), Equation.context));

                    index += 4;
                }

                for(int i = 0; i < simp38; i++) {
                    result = result.add(h.multiply(coordinates[index][1].add(coordinates[index + 1][1].multiply(BigDecimal.valueOf(3))).add(coordinates[index + 2][1].multiply(BigDecimal.valueOf(3))).add(coordinates[index + 3][1])).multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(8), Equation.context));

                    index += 3;
                }

                for(int i = 0; i < simp; i++) {
                    result = result.add(h.multiply(coordinates[index][1].add(coordinates[index + 1][1].multiply(BigDecimal.valueOf(4))).add(coordinates[index + 2][1])).divide(BigDecimal.valueOf(3), Equation.context));

                    index += 2;
                }

                for(int i = 0; i < trape; i++) {
                    result = result.add(h.multiply(coordinates[index][1].add(coordinates[index + 1][1]).divide(BigDecimal.valueOf(2), Equation.context)));
                }

                return result;
            default:
                throw new IllegalStateException("Invalid algorithm : " + algorithm);
        }
    }

    private boolean analyze(String equation, int lang) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < equation.length(); i++) {
            char ch = equation.charAt(i);

            if(Character.isDigit(ch) || isOperator(ch)) {
                if(builder.length() != 0 && handleVariable(builder, lang)) {
                    return false;
                }

                builder.setLength(0);
            } else {
                builder.append(ch);
            }
        }

        return builder.length() == 0 || !handleVariable(builder, lang);
    }

    private boolean isOperator(char ch) {
        for(int i = 0; i < operators.length; i++) {
            if (ch == operators[i])
                return true;
        }

        return false;
    }

    private boolean isKnownFunction(String func) {
        for(int i = 0; i < knownFunction.length; i++) {
            if(func.equals(knownFunction[i]))
                return true;
        }

        return false;
    }

    private boolean handleVariable(StringBuilder builder, int lang) {
        String trial = builder.toString();

        if(!StaticStore.isNumeric(trial) && !isKnownFunction(trial)) {
            if(trial.matches(".+p.+")) {
                String[] data = trial.split("p", 2);

                Equation.calculate(data[0], null, true, lang);

                if(!Equation.error.isEmpty() && !analyze(data[0], 0)) {
                    Equation.error.clear();

                    return true;
                }

                Equation.error.clear();

                Equation.calculate(data[1], null, true, lang);

                if(!Equation.error.isEmpty() && !analyze(data[1], lang)) {
                    Equation.error.clear();

                    return true;
                }

                Equation.error.clear();
            } else if(trial.matches(".+c.+")) {
                String[] data = trial.split("c", 2);

                Equation.calculate(data[0], null, true, lang);

                if(!Equation.error.isEmpty() && !analyze(data[0], 0)) {
                    Equation.error.clear();

                    return true;
                }

                Equation.error.clear();

                Equation.calculate(data[1], null, true, lang);

                if(!Equation.error.isEmpty() && !analyze(data[1], lang)) {
                    Equation.error.clear();

                    return true;
                }

                Equation.error.clear();
            }else if(trial.matches(".+!")) {
                String data = trial.replaceAll("!$", "");

                Equation.calculate(data, null, true, lang);

                if(!Equation.error.isEmpty() && !analyze(data, lang)) {
                    Equation.error.clear();

                    return true;
                }
            } else if(trial.matches("(sqrt|log).+")) {
                String data = trial.replaceAll("^(sqrt|log)", "");

                Equation.calculate(data, null, true, lang);

                if(!Equation.error.isEmpty() && !analyze(data, lang)) {
                    Equation.error.clear();

                    return true;
                }
            } else {
                String v = trial.replaceAll("^(pi|e)", "");

                if(variable.size() >= maxVariable) {
                    error.add(String.format(LangID.getStringByID("calc_var", lang), v, maxVariable, variable.get(0).name));

                    return true;
                } else {
                    variable.add(new Variable(v));
                }
            }
        }

        return false;
    }
}
