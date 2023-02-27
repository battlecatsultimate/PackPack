package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Formula {
    public enum ALGORITHM {
        SMART,
        NEWTON_RAPHSON,
        FALSE_POSITION,
        SECANT,
        BISECTION
    }

    public enum SNAP {
        FRONT,
        CENTER,
        BACK
    }

    public static List<String> error = new ArrayList<>();

    public static final BigDecimal H = BigDecimal.ONE.divide(BigDecimal.TEN.pow(5), Equation.context);
    public static final int maximumIteration = 100;
    public static final BigDecimal minimumError = new BigDecimal("0.001");

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
    public Variable variable;
    
    public Formula(String formula, int lang) {
        String stabilized = formula.replaceAll("[\\[{]", "(").replaceAll("[]}]", ")").replaceAll("\\)\\(", ")*(").replaceAll("\\s", "").toLowerCase(Locale.ENGLISH).trim();

        if(analyze(stabilized, lang)) {
            this.formula = stabilized;
        } else {
            this.formula = "0";
        }
    }

    public String substitute(String equation, int lang) {
        String stabilized = equation.replaceAll("[\\[{]", "(").replaceAll("[]}]", ")").replaceAll("\\)\\(", ")*(").replaceAll("\\s", "").toLowerCase(Locale.ENGLISH);

        if(variable != null) {
            return formula.replace("sqrt" + variable.name, "sqrt" + Equation.calculate(stabilized, null, false, lang)).replace("log" + variable.name, "log" + Equation.calculate(stabilized, null, false, lang)).replace(variable.name, "(" + stabilized + ")");
        } else {
            return formula;
        }
    }

    public NumericalResult solveByIteration(BigDecimal startPoint, BigDecimal endPoint, int numberOfIteration, ALGORITHM algorithm, int lang) {
        BigDecimal err = BigDecimal.TEN.multiply(BigDecimal.TEN);
        BigDecimal pre;

        switch (algorithm) {
            case NEWTON_RAPHSON:
                for(int i = 0; i < numberOfIteration; i++) {
                    pre = startPoint;

                    BigDecimal y = Equation.calculate(substitute(startPoint.toPlainString(), lang), null, false, lang);

                    if(y.compareTo(BigDecimal.ZERO) == 0) {
                        return new NumericalResult(pre, BigDecimal.ZERO, algorithm);
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

                return new NumericalResult(startPoint, err, algorithm);
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
                        return new NumericalResult(endPoint, BigDecimal.ZERO, algorithm);
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

                return new NumericalResult(endPoint, err, algorithm);
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
                        return new NumericalResult(endPoint, BigDecimal.ZERO, algorithm);
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

                return new NumericalResult(endPoint, err, algorithm);
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
                        return new NumericalResult(m, BigDecimal.ZERO, algorithm);

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

                return new NumericalResult(m, err, algorithm);
            case SMART:
                NumericalResult trial = solveByIteration(startPoint, endPoint, numberOfIteration, ALGORITHM.NEWTON_RAPHSON, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                trial = solveByIteration(startPoint, endPoint, numberOfIteration, ALGORITHM.BISECTION, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                error.add(LangID.getStringByID("calc_solvefail", lang));

                return null;
            default:
                throw new IllegalStateException("Invalid algorithm : " +algorithm);
        }
    }

    public NumericalResult solveByError(BigDecimal startPoint, BigDecimal endPoint, BigDecimal endError, ALGORITHM algorithm, int lang) {
        BigDecimal err = BigDecimal.TEN.multiply(BigDecimal.TEN);
        BigDecimal pre;

        int iteration = 0;

        switch (algorithm) {
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

                NumericalResult result = new NumericalResult(startPoint, err, algorithm);

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

                result = new NumericalResult(startPoint, err, algorithm);

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
                        return new NumericalResult(endPoint, BigDecimal.ZERO, algorithm);
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

                result = new NumericalResult(startPoint, err, algorithm);

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
                        return new NumericalResult(m, BigDecimal.ZERO, algorithm);

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

                result = new NumericalResult(startPoint, err, algorithm);

                if(iteration >= maximumIteration) {
                    result.warning = String.format(LangID.getStringByID("calc_solvefail", lang), Equation.formatNumber(err));
                }

                return result;
            case SMART:
                NumericalResult trial = solveByError(startPoint, endPoint, endError, ALGORITHM.NEWTON_RAPHSON, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                trial = solveByError(startPoint, endPoint, endError, ALGORITHM.BISECTION, lang);

                if(trial != null && error.isEmpty()) {
                    return trial;
                } else {
                    error.clear();
                }

                error.add(LangID.getStringByID("calc_solvefail", lang));

                return null;
            default:
                throw new IllegalStateException("Invalid algorithm : " +algorithm);
        }
    }

    private BigDecimal differentiate(BigDecimal x, BigDecimal h, SNAP snap, int lang) {
        if(h.compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalStateException("h must not be 0");

        switch (snap) {
            case BACK:
            case FRONT:
                BigDecimal fa = Equation.calculate(substitute(x.toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffback", lang));

                    return null;
                }

                BigDecimal fah = Equation.calculate(substitute(x.add(h).toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffback", lang));

                    return null;
                }

                if(snap == SNAP.BACK) {
                    return fa.subtract(fah).divide(h, Equation.context);
                } else {
                    return fah.subtract(fa).divide(h, Equation.context);
                }
            case CENTER:
                BigDecimal fha = Equation.calculate(substitute(x.toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffback", lang));

                    return null;
                }

                fah = Equation.calculate(substitute(x.add(h).toPlainString(), lang), null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    error.add(LangID.getStringByID("calc_diffback", lang));

                    return null;
                }

                return fah.subtract(fha).divide(h, Equation.context);
            default:
                throw new IllegalStateException("Invalid snap : " + snap);
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

                if(variable != null && !variable.name.equals(v)) {
                    error.add(String.format(LangID.getStringByID("calc_twovar", lang), v, variable.name));

                    return true;
                } else {
                    variable = new Variable(v);
                }
            }
        }

        return false;
    }
}
