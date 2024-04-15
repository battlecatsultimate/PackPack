package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Equation {
    public static final List<String> error = new ArrayList<>();

    public static final MathContext context = new MathContext(256, RoundingMode.HALF_EVEN);

    private static final String[] suffix = { "k", "m", "b", "t" };
    private static final BigDecimal PI = new BigDecimal(Math.PI);
    private static final BigDecimal PI2 = PI.divide(new BigDecimal("2"), context);
    private static final BigDecimal nearZero = BigDecimal.ONE.divide(BigDecimal.TEN.pow(40), context);

    public static String formatNumber(BigDecimal value) {
        if(value.abs().compareTo(BigDecimal.ZERO) == 0)
            return "0";

        DecimalFormat df = new DecimalFormat("#.########");

        if (value.abs().compareTo(BigDecimal.TEN.pow(5)) > 0) {
            int m = 0;

            while(value.abs().compareTo(BigDecimal.TEN) > 0) {
                value = value.divide(BigDecimal.TEN, context);

                m++;
            }

            return df.format(value) + "E+" + m;
        } else if(value.abs().compareTo(BigDecimal.TEN.pow(-5, Equation.context)) < 0) {
            int m = 0;

            while(value.abs().compareTo(BigDecimal.ONE) < 0) {
                value = value.multiply(BigDecimal.TEN);

                m++;
            }

            return df.format(value) + "E-" + m;
        } else {
            return df.format(value);
        }
    }

    public static String simpleNumber(BigDecimal value) {
        if(value.abs().compareTo(BigDecimal.ZERO) == 0)
            return "0";

        DecimalFormat simple = new DecimalFormat("#.####");

        if (value.abs().compareTo(BigDecimal.TEN.pow(5)) > 0) {
            int m = 0;

            while(value.abs().compareTo(BigDecimal.TEN) > 0) {
                value = value.divide(BigDecimal.TEN, context);

                m++;
            }

            return simple.format(value) + "E+" + m;
        } else if(value.abs().compareTo(BigDecimal.TEN.pow(-2, Equation.context)) < 0) {
            int m = 0;

            while(value.abs().compareTo(BigDecimal.ONE) < 0) {
                value = value.multiply(BigDecimal.TEN);

                m++;
            }

            return simple.format(value) + "E-" + m;
        } else {
            return simple.format(value);
        }
    }

    public static String simpleNumber(BigDecimal value, int allowance) {
        if(value.abs().compareTo(BigDecimal.ZERO) == 0)
            return "0";

        DecimalFormat simple = new DecimalFormat("#.####");

        if (value.abs().compareTo(BigDecimal.TEN.pow(allowance)) > 0) {
            int m = 0;

            while(value.abs().compareTo(BigDecimal.TEN) > 0) {
                value = value.divide(BigDecimal.TEN, context);

                m++;
            }

            return simple.format(value) + "E+" + m;
        } else if(value.abs().compareTo(BigDecimal.TEN.pow(-2, Equation.context)) < 0) {
            int m = 0;

            while(value.abs().compareTo(BigDecimal.ONE) < 0) {
                value = value.multiply(BigDecimal.TEN);

                m++;
            }

            return simple.format(value) + "E-" + m;
        } else {
            return simple.format(value);
        }
    }

    public static BigDecimal calculate(String equation, String parent, boolean formula, int lang) {
        if(equation.equals(parent)) {
            error.add(String.format(LangID.getStringByID("calc_notnum", lang), equation));

            return new BigDecimal(0);
        }

        equation = equation.replaceAll("\\s", "");

        List<Element> elements = parse(equation, formula, lang);

        if(elements.isEmpty())
            return new BigDecimal(0);

        List<Element> filtered = new ArrayList<>();

        //Remove squares
        for(int i = elements.size() - 1; i >= 0; i--) {
            Element e = elements.get(i);

            if(e instanceof Operator && ((Operator) e).type == Operator.TYPE.SQUARE) {
                if(i == 0 || i == elements.size() - 1 || !(elements.get(i - 1) instanceof Number) || !(elements.get(i + 1) instanceof Number)) {
                    StaticStore.logger.uploadLog("W/Equation::calculate - Invalid equation format in square process : " + equation + "\n\nData : " + elements);

                    continue;
                }

                filtered.addFirst(((Operator) e).calculate((Number) elements.get(i - 1), (Number) filtered.getFirst(), lang));
                filtered.remove(1);

                i--;
            } else {
                filtered.addFirst(e);
            }
        }

        elements.clear();
        elements.addAll(filtered);

        filtered.clear();

        //Remove multiplication & division
        for(int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);

            if(e instanceof Operator && (((Operator) e).type == Operator.TYPE.DIVISION || ((Operator) e).type == Operator.TYPE.MULTIPLICATION) ) {
                if(i == 0 || i == elements.size() - 1 || !(elements.get(i - 1) instanceof Number) || !(elements.get(i + 1) instanceof Number)) {
                    StaticStore.logger.uploadLog("W/Equation::calculate - Invalid equation format in multiplication/division process : " + equation + "\n\nData : " + elements);

                    continue;
                }

                filtered.add(((Operator) e).calculate((Number) filtered.getLast(), (Number) elements.get(i + 1), lang));
                filtered.remove(filtered.size() - 2);

                i++;
            } else {
                filtered.add(e);
            }
        }

        elements.clear();
        elements.addAll(filtered);

        filtered.clear();

        //Remove addition & subtraction
        for(int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);

            if(e instanceof Operator && (((Operator) e).type == Operator.TYPE.ADDITION || ((Operator) e).type == Operator.TYPE.SUBTRACTION) ) {
                if(i == 0 || i == elements.size() - 1 || !(elements.get(i - 1) instanceof Number) || !(elements.get(i + 1) instanceof Number)) {
                    StaticStore.logger.uploadLog("W/Equation::calculate - Invalid equation format in addition/subtraction process : " + equation + "\n\nData : " + elements);

                    continue;
                }

                filtered.add(((Operator) e).calculate((Number) filtered.getLast(), (Number) elements.get(i + 1), lang));
                filtered.remove(filtered.size() - 2);

                i++;
            } else {
                filtered.add(e);
            }
        }

        elements.clear();
        elements.addAll(filtered);

        filtered.clear();

        if(elements.size() != 1 || !(elements.getFirst() instanceof Number)) {
            StaticStore.logger.uploadLog("W/Equation::calculate - Invalid equation format : " + equation + "\n\nData : " + elements);

            error.add(LangID.getStringByID("calc_fail", lang));

            return new BigDecimal(0);
        } else {
            return ((Number) elements.getFirst()).bd;
        }
    }

    public static String getErrorMessage(String... messages) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < messages.length; i++) {
            builder.append(messages[i]);

            if(i < messages.length - 1)
                builder.append("\n");
        }

        if(messages.length > 0)
            builder.append("\n\n");

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

    private static List<Element> parse(String equation, boolean formula, int lang) {
        if(openedBracket(equation)) {
            error.add(LangID.getStringByID("calc_opened", lang));

            return new ArrayList<>();
        }

        equation = equation.replaceAll("[\\[{]", "(").replaceAll("[]}]", ")").replaceAll("\\)\\(", ")*(").toLowerCase(Locale.ENGLISH);

        if(equation.length() >= 2 && equation.substring(0, 2).matches("^-[^.\\d]")) {
            equation = "-1*" + equation.substring(1);
        }

        List<Element> elements = new ArrayList<>();

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

                    while(true) {
                        if(i >= equation.length()) {
                            i = last;

                            break;
                        }

                        if(endOfAbs(equation, i)) {
                            level--;

                            if(level < max) {
                                last = i;
                            }

                            max = Math.min(max, level);
                        } else if(equation.charAt(i) == '|') {
                            level++;
                        }

                        i++;
                    }

                    if(start == last) {
                        elements.add(new Number("0"));
                    } else {
                        builder.append(equation, start, last);

                        int size = error.size();

                        BigDecimal test = calculate(builder.toString(), equation, formula, lang);

                        if(size != error.size()) {
                            error.add(String.format(LangID.getStringByID("calc_absfail", lang), builder));

                            return new ArrayList<>();
                        }

                        if(pre != null) {
                            BigDecimal preTest = calculate(builder.toString(), equation, formula, lang);

                            if(size != error.size()) {
                                error.add(String.format(LangID.getStringByID("calc_abspre", lang), pre + "|" + builder + "|"));

                                return new ArrayList<>();
                            }

                            elements.add(new Number(test.abs().multiply(preTest)));
                        } else {
                            elements.add(new Number(test.abs()));
                        }
                    }

                    builder.setLength(0);

                    break;
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
                            case "npr", "ncr" -> {
                                List<String> data = filterData(builder.toString(), ',');
                                if (data.size() != 2) {
                                    error.add(String.format(LangID.getStringByID("calc_npcrparam", lang), 2, data.size(), data));

                                    return new ArrayList<>();
                                }
                                for (int j = 0; j < data.size(); j++) {
                                    int originalLength = error.size();

                                    BigDecimal valD = calculate(data.get(j), null, formula, lang);

                                    if (originalLength != error.size()) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnum", lang), prefix + "(" + data.getFirst() + ", " + data.get(1) + ")", data.get(j)));

                                        return new ArrayList<>();
                                    } else if (valD.divideAndRemainder(new BigDecimal(1))[1].doubleValue() != 0) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang), prefix + "(" + data.getFirst() + ", " + data.get(1) + ")", data.get(j)));

                                        return new ArrayList<>();
                                    } else if (valD.doubleValue() < 0) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang), prefix + "(" + data.getFirst() + ", " + data.get(1) + ")", data.get(j)));

                                        return new ArrayList<>();
                                    }
                                }
                                BigInteger n = calculate(data.getFirst(), null, formula, lang).toBigInteger();
                                BigInteger r = calculate(data.get(1), null, formula, lang).toBigInteger();
                                if (n.compareTo(r) < 0) {
                                    error.add(String.format(LangID.getStringByID("calc_npcrsize", lang), prefix + "(" + data.getFirst() + ", " + data.get(1) + ")", data.getFirst(), data.get(1)));
                                }
                                if (prefix.equals("npr")) {
                                    elements.add(new Number(nPr(n, r, lang)));
                                } else {
                                    elements.add(new Number(nCr(n, r, lang)));
                                }
                            }
                            default -> {
                                BigDecimal inner = calculate(builder.toString(), equation, formula, lang);
                                if (StaticStore.isNumeric(prefix)) {
                                    elements.add(new Number(new BigDecimal(prefix)));
                                    elements.add(new Operator(Operator.TYPE.MULTIPLICATION));
                                    elements.add(new Number(inner));
                                } else {
                                    final boolean b1 = inner.compareTo(BigDecimal.valueOf(-1)) < 0 || inner.compareTo(BigDecimal.ONE) > 0;
                                    final boolean b = inner.compareTo(BigDecimal.valueOf(-1)) > 0 && inner.compareTo(BigDecimal.ONE) < 0;

                                    switch (prefix) {
                                        case "sin" -> elements.add(new Number(Math.sin(inner.doubleValue())));
                                        case "cos" -> elements.add(new Number(Math.cos(inner.doubleValue())));
                                        case "tan" -> {
                                            if (inner.compareTo(BigDecimal.ZERO) != 0 && zeroEnough(inner.remainder(PI2, Equation.context)) && !zeroEnough(inner.remainder(PI, Equation.context))) {
                                                error.add(String.format(LangID.getStringByID("calc_tan", lang), "tan(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.tan(inner.doubleValue())));
                                        }
                                        case "csc" -> {
                                            if (zeroEnough(inner.remainder(PI, context))) {
                                                error.add(String.format(LangID.getStringByID("calc_csc", lang), "csc(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            BigDecimal check = BigDecimal.valueOf(Math.sin(inner.doubleValue()));

                                            elements.add(new Number(BigDecimal.ONE.divide(check, Equation.context)));
                                        }
                                        case "sec" -> {
                                            if (zeroEnough(inner.remainder(PI2, context)) && !zeroEnough(inner.remainder(PI, context))) {
                                                error.add(String.format(LangID.getStringByID("calc_sec", lang), "sec(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(BigDecimal.ONE.divide(BigDecimal.valueOf(Math.cos(inner.doubleValue())), Equation.context)));
                                        }
                                        case "cot" -> {
                                            if (zeroEnough(inner.remainder(PI, context))) {
                                                error.add(String.format(LangID.getStringByID("calc_cot", lang), "cot(" + builder + ")"));

                                                return new ArrayList<>();
                                            } else if (zeroEnough(inner.remainder(PI2, context))) {
                                                elements.add(new Number("0"));
                                            } else {
                                                elements.add(new Number(BigDecimal.ONE.divide(BigDecimal.valueOf(Math.tan(inner.doubleValue())), Equation.context)));
                                            }
                                        }
                                        case "ln", "loge" -> {
                                            if (inner.compareTo(BigDecimal.ZERO) <= 0) {
                                                error.add(String.format(LangID.getStringByID("calc_ln", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.log(inner.doubleValue())));
                                        }
                                        case "log" -> {
                                            if (inner.compareTo(BigDecimal.ZERO) <= 0) {
                                                error.add(String.format(LangID.getStringByID("calc_ln", lang), "log(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.log10(inner.doubleValue())));
                                        }
                                        case "sqrt", "root", "sqrt2" -> {
                                            if (inner.compareTo(BigDecimal.ZERO) < 0) {
                                                error.add(String.format(LangID.getStringByID("calc_sqrt", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(inner.sqrt(Equation.context)));
                                        }
                                        case "exp" -> {
                                            if (inner.stripTrailingZeros().scale() <= 0 && inner.abs().longValue() < 999999999) {
                                                elements.add(new Number(BigDecimal.valueOf(Math.E).pow(inner.intValue())));
                                            } else {
                                                elements.add(new Number(Math.pow(Math.E, inner.doubleValue())));
                                            }
                                        }
                                        case "arcsin", "asin" -> {
                                            if (b1) {
                                                error.add(String.format(LangID.getStringByID("calc_arcsin", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.asin(inner.doubleValue())));
                                        }
                                        case "arccos", "acos" -> {
                                            if (b1) {
                                                error.add(String.format(LangID.getStringByID("calc_arccos", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.acos(inner.doubleValue())));
                                        }
                                        case "arctan", "atan" -> elements.add(new Number(Math.atan(inner.doubleValue())));
                                        case "arccsc", "acsc" -> {
                                            if (b) {
                                                error.add(String.format(LangID.getStringByID("calc_arccsc", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.asin(1.0 / inner.doubleValue())));
                                        }
                                        case "arcsec", "asec" -> {
                                            if (b) {
                                                error.add(String.format(LangID.getStringByID("calc_arcsec", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.acos(1.0 / inner.doubleValue())));
                                        }
                                        case "arccot", "acot" -> {
                                            if (inner.compareTo(BigDecimal.ZERO) == 0) {
                                                elements.add(new Number(Math.PI / 2));
                                            } else {
                                                elements.add(new Number(Math.atan(1.0 / inner.doubleValue())));
                                            }
                                        }
                                        case "abs" -> elements.add(new Number(inner.abs()));
                                        case "sign", "sgn" -> elements.add(new Number(inner.signum()));
                                        case "floor" -> elements.add(new Number(inner.setScale(0, RoundingMode.FLOOR).unscaledValue()));
                                        case "ceil" -> elements.add(new Number(inner.setScale(0, RoundingMode.CEILING).unscaledValue()));
                                        case "round" -> elements.add(new Number(inner.setScale(0, RoundingMode.HALF_UP).unscaledValue()));
                                        default -> {
                                            if (prefix.startsWith("log")) {
                                                String base = prefix.replaceAll("^log", "");

                                                double log = Math.log(inner.doubleValue());

                                                if (!Double.isFinite(log)) {
                                                    error.add(LangID.getStringByID("calc_outofrange", lang));

                                                    return new ArrayList<>();
                                                }

                                                final BigDecimal bigDecimal = BigDecimal.valueOf(log);

                                                if (StaticStore.isNumeric(base)) {
                                                    double value = Double.parseDouble(base);

                                                    if (value <= 0) {
                                                        error.add(String.format(LangID.getStringByID("calc_logbase", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    if (inner.compareTo(BigDecimal.ZERO) <= 0) {
                                                        error.add(String.format(LangID.getStringByID("calc_ln", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    double l = Math.log(Double.parseDouble(base));

                                                    if (!Double.isFinite(l)) {
                                                        error.add(LangID.getStringByID("calc_outofrange", lang));

                                                        return new ArrayList<>();
                                                    }

                                                    elements.add(new Number(bigDecimal.divide(BigDecimal.valueOf(l), Equation.context)));
                                                } else {
                                                    int originalLength = error.size();

                                                    BigDecimal value = calculate(base, prefix + "(" + builder + ")", formula, lang);

                                                    if (originalLength != error.size()) {
                                                        error.add(String.format(LangID.getStringByID("calc_unknownfunc", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    } else {
                                                        if (value.compareTo(BigDecimal.ZERO) <= 0) {
                                                            error.add(String.format(LangID.getStringByID("calc_logbase", lang), prefix + "(" + builder + ")"));

                                                            return new ArrayList<>();
                                                        }

                                                        double l = Math.log(value.doubleValue());

                                                        if (!Double.isFinite(l)) {
                                                            error.add(LangID.getStringByID("calc_outofrange", lang));

                                                            return new ArrayList<>();
                                                        }

                                                        elements.add(new Number(bigDecimal.divide(BigDecimal.valueOf(l), Equation.context)));
                                                    }
                                                }
                                            } else if (prefix.startsWith("sqrt")) {
                                                String base = prefix.replaceAll("^sqrt", "");

                                                if (StaticStore.isNumeric(base)) {
                                                    BigDecimal value = new BigDecimal(base);

                                                    if (value.compareTo(BigDecimal.ZERO) == 0 && inner.compareTo(BigDecimal.ZERO) < 0) {
                                                        error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    double checkValue = Math.pow(inner.doubleValue(), BigDecimal.ONE.divide(value, Equation.context).doubleValue());

                                                    if (!Double.isFinite(checkValue)) {
                                                        error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    elements.add(new Number(BigDecimal.valueOf(checkValue)));
                                                } else {
                                                    int originalLength = error.size();

                                                    BigDecimal value = calculate(base, prefix + "(" + builder + ")", formula, lang);

                                                    if (originalLength != error.size()) {
                                                        error.add(String.format(LangID.getStringByID("calc_unknownfunc", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    } else {
                                                        if (value.remainder(BigDecimal.valueOf(2)).compareTo(BigDecimal.ZERO) == 0 && inner.compareTo(BigDecimal.ZERO) < 0) {
                                                            error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                            return new ArrayList<>();
                                                        }

                                                        double checkValue = Math.pow(inner.doubleValue(), BigDecimal.ONE.divide(value, Equation.context).doubleValue());

                                                        if (!Double.isFinite(checkValue)) {
                                                            error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                            return new ArrayList<>();
                                                        }

                                                        elements.add(new Number(BigDecimal.valueOf(checkValue)));
                                                    }
                                                }
                                            } else {
                                                int len = error.size();

                                                BigDecimal check = calculate(prefix, prefix + "(" + builder + ")", formula, lang);

                                                if (len != error.size()) {
                                                    error.add(String.format(LangID.getStringByID("calc_unknownfunc", lang), prefix + "(" + builder + ")"));

                                                    return new ArrayList<>();
                                                }

                                                elements.add(new Number(check.multiply(inner)));
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        builder.setLength(0);
                    }

                    break;
                case 'x':
                    if(formula) {
                        error.add(String.format(LangID.getStringByID("calc_notnum", lang), "x"));

                        return new ArrayList<>();
                    }
                case '+':
                case '-':
                case '*':
                case '×':
                case '/':
                case '÷':
                case '^':
                    if((builder.isEmpty() && i != 0 && equation.charAt(i - 1) != ')' && equation.charAt(i - 1) != '|') || i == equation.length() - 1) {
                        error.add(LangID.getStringByID("calc_alone", lang));

                        return new ArrayList<>();
                    }

                    if(builder.isEmpty() && i == 0 && equation.charAt(i) == '-') {
                        builder.append(equation.charAt(i));

                        continue;
                    }

                    if(!builder.isEmpty()) {
                        String wait = builder.toString();

                        if(StaticStore.isNumeric(wait)) {
                            elements.add(new Number(wait));
                        } else {
                            switch (wait) {
                                case "pi", "π" -> elements.add(new Number(Math.PI));
                                case "e" -> elements.add(new Number(Math.E));
                                default -> {
                                    if (wait.endsWith("!")) {
                                        String filtered = wait.replaceAll("!$", "");

                                        int originalSize = error.size();

                                        BigDecimal valD = calculate(filtered, null, formula, lang);

                                        if (originalSize != error.size()) {
                                            error.add(String.format(LangID.getStringByID("calc_notnum", lang), wait));

                                            return new ArrayList<>();
                                        } else {
                                            if (valD.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                                                error.add(String.format(LangID.getStringByID("calc_factnum", lang), wait));

                                                return new ArrayList<>();
                                            } else if (valD.compareTo(BigDecimal.ZERO) < 0) {
                                                error.add(String.format(LangID.getStringByID("calc_factrange", lang), wait));

                                                return new ArrayList<>();
                                            } else {
                                                elements.add(new Number(factorial(valD.toBigInteger(), lang)));
                                            }
                                        }
                                    } else if (wait.matches("\\d+p\\d+") || wait.matches("\\d+c\\d+")) {
                                        String[] data;

                                        if (wait.matches("\\d+p\\d+")) {
                                            data = builder.toString().split("p");
                                        } else {
                                            data = builder.toString().split("c");
                                        }

                                        if (data.length != 2) {
                                            error.add(String.format(LangID.getStringByID("calc_npcrparam", lang), 2, data.length, Arrays.toString(data)));

                                            return new ArrayList<>();
                                        }

                                        for (int j = 0; j < data.length; j++) {
                                            if (StaticStore.isNumeric(data[j])) {
                                                long valL = Long.parseLong(data[j]);
                                                double valD = Double.parseDouble(data[j]);

                                                if (valL != valD) {
                                                    error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang), wait, data[j]));

                                                    return new ArrayList<>();
                                                } else if (valL < 0) {
                                                    error.add(String.format(LangID.getStringByID("calc_npcrrange", lang), wait, data[j]));

                                                    return new ArrayList<>();
                                                }
                                            } else {
                                                error.add(String.format(LangID.getStringByID("calc_npcrnum", lang), wait, data[j]));

                                                return new ArrayList<>();
                                            }
                                        }

                                        BigInteger n = new BigInteger(data[0]);
                                        BigInteger r = new BigInteger(data[1]);

                                        if (n.compareTo(r) < 0) {
                                            error.add(String.format(LangID.getStringByID("calc_npcrsize", lang), wait, data[0], data[1]));
                                        }

                                        if (wait.matches("\\d+p\\d+")) {
                                            elements.add(new Number(nPr(n, r, lang)));
                                        } else {
                                            elements.add(new Number(nCr(n, r, lang)));
                                        }
                                    } else {
                                        double suffixCheck = checkSuffix(wait);

                                        if (Double.isNaN(suffixCheck)) {
                                            int len = error.size();

                                            BigDecimal check = calculate(wait, equation, formula, lang);

                                            if (len != error.size()) {
                                                error.add(String.format(LangID.getStringByID("calc_notnum", lang), wait));

                                                return new ArrayList<>();
                                            } else {
                                                elements.add(new Number(check));
                                            }
                                        } else {
                                            elements.add(new Number(suffixCheck));
                                        }
                                    }
                                }
                            }
                        }

                        builder.setLength(0);
                    }

                    Operator.TYPE operator = switch (equation.charAt(i)) {
                        case '*', '×', 'x' -> Operator.TYPE.MULTIPLICATION;
                        case '+' -> Operator.TYPE.ADDITION;
                        case '-' -> Operator.TYPE.SUBTRACTION;
                        case '/', '÷' -> Operator.TYPE.DIVISION;
                        default -> Operator.TYPE.SQUARE;
                    };

                    elements.add(new Operator(operator));

                    break;
                default:
                    builder.append(equation.charAt(i));
            }
        }

        if(!builder.isEmpty()) {
            String prefix = builder.toString();

            if(StaticStore.isNumeric(prefix)) {
                elements.add(new Number(prefix));
            } else {
                switch (prefix) {
                    case "pi", "π" -> elements.add(new Number(Math.PI));
                    case "e" -> elements.add(new Number(Math.E));
                    default -> {
                        if (prefix.endsWith("!")) {
                            String filtered = prefix.replaceAll("!$", "");

                            int originalSize = error.size();

                            BigDecimal valD = calculate(filtered, null, formula, lang);

                            if (originalSize != error.size()) {
                                error.add(String.format(LangID.getStringByID("calc_notnum", lang), prefix));

                                return new ArrayList<>();
                            } else {
                                if (valD.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                                    error.add(String.format(LangID.getStringByID("calc_factnum", lang), prefix));

                                    return new ArrayList<>();
                                } else if (valD.compareTo(BigDecimal.ZERO) < 0) {
                                    error.add(String.format(LangID.getStringByID("calc_factrange", lang), prefix));

                                    return new ArrayList<>();
                                } else {
                                    elements.add(new Number(factorial(valD.toBigInteger(), lang)));
                                }
                            }
                        } else if (prefix.matches("\\d+p\\d+") || prefix.matches("\\d+c\\d+")) {
                            String[] data;

                            if (prefix.matches("\\d+p\\d+")) {
                                data = builder.toString().split("p");
                            } else {
                                data = builder.toString().split("c");
                            }

                            if (data.length != 2) {
                                error.add(String.format(LangID.getStringByID("calc_npcrparam", lang), 2, data.length, Arrays.toString(data)));

                                return new ArrayList<>();
                            }

                            for (int j = 0; j < data.length; j++) {
                                if (StaticStore.isNumeric(data[j])) {
                                    long valL = Long.parseLong(data[j]);
                                    double valD = Double.parseDouble(data[j]);

                                    if (valL != valD) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang), prefix, data[j]));

                                        return new ArrayList<>();
                                    } else if (valL < 0) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrrange", lang), prefix, data[j]));

                                        return new ArrayList<>();
                                    }
                                } else {
                                    error.add(String.format(LangID.getStringByID("calc_npcrnum", lang), prefix, data[j]));

                                    return new ArrayList<>();
                                }
                            }

                            BigInteger n = new BigInteger(data[0]);
                            BigInteger r = new BigInteger(data[1]);

                            if (n.compareTo(r) < 0) {
                                error.add(String.format(LangID.getStringByID("calc_npcrsize", lang), prefix + "(" + data[0] + ", " + data[1] + ")", data[0], data[1]));
                            }

                            if (prefix.matches("\\d+p\\d+")) {
                                elements.add(new Number(nPr(n, r, lang)));
                            } else {
                                elements.add(new Number(nCr(n, r, lang)));
                            }
                        } else {
                            double suffixCheck = checkSuffix(prefix);

                            if (Double.isNaN(suffixCheck)) {
                                int len = error.size();

                                BigDecimal check = calculate(prefix, equation, formula, lang);

                                if (len != error.size()) {
                                    error.add(String.format(LangID.getStringByID("calc_notnum", lang), prefix));

                                    return new ArrayList<>();
                                } else {
                                    elements.add(new Number(check));
                                }
                            } else {
                                elements.add(new Number(suffixCheck));
                            }
                        }
                    }
                }
            }
        }

        return elements;
    }

    private static boolean openedBracket(String raw) {
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
    private static List<String> filterData(String raw, char separator) {
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

    private static double checkSuffix(String value) {
        for(int i = 0; i < suffix.length; i++) {
            if(value.toLowerCase(Locale.ENGLISH).endsWith(suffix[i])) {
                String prefix = value.toLowerCase(Locale.ENGLISH).replaceAll(suffix[i] +"$", "");

                if(StaticStore.isNumeric(prefix)) {
                    return Double.parseDouble(prefix) * Math.pow(10, (i + 1) * 3);
                } else if(prefix.equals("pi")) {
                    return Math.PI * Math.pow(10, (i + 1) * 3);
                } else if(prefix.equals("e")) {
                    return Math.E * Math.pow(10, (i + 1) * 3);
                } else {
                    return Double.NaN;
                }
            }
        }

        return Double.NaN;
    }

    private static BigDecimal nPr(BigInteger n, BigInteger r, int lang) {
        return factorial(n, lang).divide(factorial(n.subtract(r), lang), Equation.context);
    }

    private static BigDecimal nCr(BigInteger n, BigInteger r, int lang) {
        return factorial(n, lang).divide(factorial(r, lang).multiply(factorial(n.subtract(r), lang)), Equation.context);
    }

    private static BigDecimal factorial(BigInteger n, int lang) {
        if(n.compareTo(BigInteger.ZERO) < 0)
            return BigDecimal.ONE;

        if (n.compareTo(BigInteger.valueOf(10000)) > 0) {
            error.add(LangID.getStringByID("calc_factorial", lang));

            return BigDecimal.ONE;
        }

        BigDecimal f = new BigDecimal("1");

        for(BigInteger i = new BigInteger("2"); i.compareTo(n) <= 0; i = i.add(new BigInteger("1"))) {
            f = f.multiply(new BigDecimal(i));
        }

        return f;
    }

    private static boolean endOfAbs(String equation, int index) {
        char c = equation.charAt(index);

        if(c == '|') {
            if(index - 1 < 0) {
                return false;
            }

            char before = equation.charAt(index - 1);

            if(Character.isDigit(before) || before == ')' || before == '|') {
                return true;
            }

            String previous = equation.substring(0, index);

            return previous.matches("(.+)?(pi|[^a-z]?e)$");
        } else {
            return false;
        }
    }

    private static boolean zeroEnough(BigDecimal value) {
        return value.abs().compareTo(nearZero) < 0;
    }
}
