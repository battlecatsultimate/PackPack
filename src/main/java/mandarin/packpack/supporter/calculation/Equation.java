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
    public static List<String> error = new ArrayList<>();
    public static DecimalFormat df = new DecimalFormat("#.######");

    private static final String[] suffix = { "k", "m", "b", "t" };

    public static BigDecimal calculate(String equation, String parent, int lang) {
        if(equation.equals(parent)) {
            error.add(String.format(LangID.getStringByID("calc_notnum", lang), equation));

            return new BigDecimal(0);
        }

        List<Element> elements = parse(equation, lang);

        if(elements.isEmpty())
            return new BigDecimal(0);

        List<Element> filtered = new ArrayList<>();

        //Remove squares
        for(int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);

            if(e instanceof Operator && ((Operator) e).type == Operator.TYPE.SQUARE) {
                if(i == 0 || i == elements.size() - 1 || !(elements.get(i - 1) instanceof Number) || !(elements.get(i + 1) instanceof Number)) {
                    StaticStore.logger.uploadLog("W/Equation::calculate - Invalid equation format in square process : " + equation + "\n\nData : " + elements);

                    continue;
                }

                filtered.add(((Operator) e).calculate((Number) filtered.get(filtered.size() - 1), (Number) elements.get(i + 1), lang));
                filtered.remove(filtered.size() - 2);

                i++;
            } else {
                filtered.add(e);
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

                filtered.add(((Operator) e).calculate((Number) filtered.get(filtered.size() - 1), (Number) elements.get(i + 1), lang));
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

                filtered.add(((Operator) e).calculate((Number) filtered.get(filtered.size() - 1), (Number) elements.get(i + 1), lang));
                filtered.remove(filtered.size() - 2);

                i++;
            } else {
                filtered.add(e);
            }
        }

        elements.clear();
        elements.addAll(filtered);

        filtered.clear();

        if(elements.size() != 1 || !(elements.get(0) instanceof Number)) {
            StaticStore.logger.uploadLog("W/Equation::calculate - Invalid equation format : " + equation + "\n\nData : " + elements);

            error.add(LangID.getStringByID("calc_fail", lang));

            return new BigDecimal(0);
        } else {
            return ((Number) elements.get(0)).bd;
        }
    }

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

    private static List<Element> parse(String equation, int lang) {
        if(openedBracket(equation)) {
            error.add(LangID.getStringByID("calc_opened", lang));

            return new ArrayList<>();
        }

        equation = equation.replaceAll("[\\[{]", "(").replaceAll("[]}]", ")").replaceAll("\\)\\(", ")*(").toLowerCase(Locale.ENGLISH);

        List<Element> elements = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < equation.length(); i++) {
            switch (equation.charAt(i)) {
                case '|':
                    String pre;

                    if(builder.length() != 0) {
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

                        BigDecimal test = calculate(builder.toString(), equation, lang);

                        if(size != error.size()) {
                            error.add(String.format(LangID.getStringByID("calc_absfail", lang), builder));

                            return new ArrayList<>();
                        }

                        if(pre != null) {
                            BigDecimal preTest = calculate(builder.toString(), equation, lang);

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

                    if(builder.length() != 0) {
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
                                    error.add(String.format(LangID.getStringByID("calc_npcrparam", lang), 2, data.size(), data));

                                    return new ArrayList<>();
                                }

                                for(int j = 0; j < data.size(); j++) {
                                    int originalLength = error.size();

                                    BigDecimal valD = calculate(data.get(j), null, lang);

                                    if(originalLength != error.size()) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnum", lang), prefix + "(" + data.get(0) + ", " + data.get(1) + ")", data.get(j)));

                                        return new ArrayList<>();
                                    } else if(valD.divideAndRemainder(new BigDecimal(1))[1].doubleValue() != 0) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang),  prefix + "(" + data.get(0) + ", " + data.get(1) + ")", data.get(j)));

                                        return new ArrayList<>();
                                    } else if(valD.doubleValue() < 0) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang),  prefix + "(" + data.get(0) + ", " + data.get(1) + ")", data.get(j)));

                                        return new ArrayList<>();
                                    }
                                }

                                BigInteger n = calculate(data.get(0), null, lang).toBigInteger();
                                BigInteger r = calculate(data.get(1), null, lang).toBigInteger();

                                if(n.compareTo(r) < 0) {
                                    error.add(String.format(LangID.getStringByID("calc_npcrsize", lang), prefix + "(" + data.get(0) + ", " + data.get(1)+ ")", data.get(0), data.get(1)));
                                }

                                if (prefix.equals("npr")) {
                                    elements.add(new Number(nPr(n, r)));
                                } else {
                                    elements.add(new Number(nCr(n, r)));
                                }

                                break;
                            default:
                                BigDecimal inner = calculate(builder.toString(), equation, lang);

                                if(StaticStore.isNumeric(prefix)) {
                                    elements.add(new Number(inner.multiply(new BigDecimal(prefix))));
                                } else {
                                    final boolean b1 = inner.compareTo(BigDecimal.valueOf(-1)) < 0 || inner.compareTo(BigDecimal.ONE) > 0;
                                    final boolean b = inner.compareTo(BigDecimal.valueOf(-1)) > 0 && inner.compareTo(BigDecimal.ONE) < 0;
                                    switch (prefix) {
                                        case "sin":
                                            elements.add(new Number(Math.sin(inner.doubleValue())));

                                            break;
                                        case "cos":
                                            elements.add(new Number(Math.cos(inner.doubleValue())));

                                            break;
                                        case "tan":
                                            if(inner.divideAndRemainder(BigDecimal.valueOf(Math.PI / 2))[1].doubleValue() == 0) {
                                                error.add(String.format(LangID.getStringByID("calc_tan", lang), "tan(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.tan(inner.doubleValue())));

                                            break;
                                        case "csc":
                                            BigDecimal check = BigDecimal.valueOf(Math.sin(inner.doubleValue()));

                                            if(check.compareTo(BigDecimal.ZERO) == 0) {
                                                error.add(String.format(LangID.getStringByID("calc_csc", lang), "csc(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(BigDecimal.ONE.divide(check, MathContext.UNLIMITED)));

                                            break;
                                        case "sec":
                                            check = BigDecimal.valueOf(Math.cos(inner.doubleValue()));

                                            if(check.compareTo(BigDecimal.ZERO) == 0) {
                                                error.add(String.format(LangID.getStringByID("calc_sec", lang), "sec(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(BigDecimal.ONE.divide(check, MathContext.UNLIMITED)));

                                            break;
                                        case "cot":
                                            if(inner.remainder(BigDecimal.valueOf(Math.PI)).compareTo(BigDecimal.ZERO) == 0) {
                                                error.add(String.format(LangID.getStringByID("calc_cot", lang), "cot(" + builder + ")"));

                                                return new ArrayList<>();
                                            } else if(inner.remainder(BigDecimal.valueOf(Math.PI / 2)).compareTo(BigDecimal.ZERO) == 0) {
                                                elements.add(new Number("0"));
                                            } else {
                                                elements.add(new Number(BigDecimal.ONE.divide(BigDecimal.valueOf(Math.tan(inner.doubleValue())), MathContext.UNLIMITED)));
                                            }

                                            break;
                                        case "ln":
                                        case "loge":
                                            if(inner.compareTo(BigDecimal.ZERO) <= 0) {
                                                error.add(String.format(LangID.getStringByID("calc_ln", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.log(inner.doubleValue())));

                                            break;
                                        case "log":
                                            if(inner.compareTo(BigDecimal.ZERO) <= 0) {
                                                error.add(String.format(LangID.getStringByID("calc_ln", lang), "log(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.log10(inner.doubleValue())));

                                            break;
                                        case "sqrt":
                                        case "root":
                                        case "sqrt2":
                                            if(inner.compareTo(BigDecimal.ZERO) < 0) {
                                                error.add(String.format(LangID.getStringByID("calc_sqrt", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(inner.sqrt(MathContext.UNLIMITED)));

                                            break;
                                        case "exp":
                                            if (inner.stripTrailingZeros().scale() <= 0 && inner.abs().longValue() < 999999999) {
                                                elements.add(new Number(BigDecimal.valueOf(Math.E).pow(inner.intValue())));
                                            } else {
                                                elements.add(new Number(Math.pow(Math.E, inner.doubleValue())));
                                            }

                                            break;
                                        case "arcsin":
                                        case "asin":
                                            if(b1) {
                                                error.add(String.format(LangID.getStringByID("calc_arcsin", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.asin(inner.doubleValue())));

                                            break;
                                        case "arccos":
                                        case "acos":
                                            if(b1) {
                                                error.add(String.format(LangID.getStringByID("calc_arccos", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.acos(inner.doubleValue())));

                                            break;
                                        case "arctan":
                                        case "atan":
                                            elements.add(new Number(Math.atan(inner.doubleValue())));

                                            break;
                                        case "arccsc":
                                        case "acsc":
                                            if(b) {
                                                error.add(String.format(LangID.getStringByID("calc_arccsc", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.asin(1.0 / inner.doubleValue())));

                                            break;
                                        case "arcsec":
                                        case "asec":
                                            if(b) {
                                                error.add(String.format(LangID.getStringByID("calc_arcsec", lang), prefix + "(" + builder + ")"));

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(Math.acos(1.0 / inner.doubleValue())));

                                            break;
                                        case "arccot":
                                        case "acot":
                                            if (inner.compareTo(BigDecimal.ZERO) == 0) {
                                                elements.add(new Number(Math.PI / 2));
                                            } else {
                                                elements.add(new Number(Math.atan(1.0 / inner.doubleValue())));
                                            }

                                            break;
                                        case "abs":
                                            elements.add(new Number(inner.abs()));

                                            break;
                                        case "sign":
                                        case "sgn":
                                            elements.add(new Number(inner.signum()));

                                            break;
                                        case "floor":
                                            elements.add(new Number(inner.setScale(0, RoundingMode.FLOOR).unscaledValue()));

                                            break;
                                        case "ceil":
                                            elements.add(new Number(inner.setScale(0, RoundingMode.CEILING).unscaledValue()));

                                            break;
                                        case "round":
                                            elements.add(new Number(inner.setScale(0, RoundingMode.HALF_UP).unscaledValue()));

                                            break;
                                        default:
                                            if(prefix.startsWith("log")) {
                                                String base = prefix.replaceAll("^log", "");

                                                final BigDecimal bigDecimal = BigDecimal.valueOf(Math.log(inner.doubleValue()));

                                                if(StaticStore.isNumeric(base)) {
                                                    double value = Double.parseDouble(base);

                                                    if(value <= 0) {
                                                        error.add(String.format(LangID.getStringByID("calc_logbase", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    if(inner.compareTo(BigDecimal.ZERO) <= 0) {
                                                        error.add(String.format(LangID.getStringByID("calc_ln", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    elements.add(new Number(bigDecimal.divide(BigDecimal.valueOf(Math.log(Double.parseDouble(base))), MathContext.UNLIMITED)));
                                                } else {
                                                    int originalLength = error.size();

                                                    BigDecimal value = calculate(base, prefix + "(" + builder + ")", lang);

                                                    if(originalLength != error.size()) {
                                                        error.add(String.format(LangID.getStringByID("calc_unknownfunc", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    } else {
                                                        if(value.compareTo(BigDecimal.ZERO) <= 0) {
                                                            error.add(String.format(LangID.getStringByID("calc_logbase", lang), prefix + "(" + builder + ")"));

                                                            return new ArrayList<>();
                                                        }

                                                        elements.add(new Number(bigDecimal.divide(BigDecimal.valueOf(Math.log(value.doubleValue())), MathContext.UNLIMITED)));
                                                    }
                                                }
                                            } else if(prefix.startsWith("sqrt")) {
                                                String base = prefix.replaceAll("^sqrt", "");

                                                if(StaticStore.isNumeric(base)) {
                                                    BigDecimal value = new BigDecimal(base);

                                                    if(value.compareTo(BigDecimal.ZERO) == 0 && inner.compareTo(BigDecimal.ZERO) < 0) {
                                                        error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    double checkValue = Math.pow(inner.doubleValue(), BigDecimal.ONE.divide(value, MathContext.UNLIMITED).doubleValue());

                                                    if(Double.isNaN(checkValue)) {
                                                        error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    }

                                                    elements.add(new Number(BigDecimal.valueOf(checkValue)));
                                                } else {
                                                    int originalLength = error.size();

                                                    BigDecimal value = calculate(base, prefix + "(" + builder + ")", lang);

                                                    if(originalLength != error.size()) {
                                                        error.add(String.format(LangID.getStringByID("calc_unknownfunc", lang), prefix + "(" + builder + ")"));

                                                        return new ArrayList<>();
                                                    } else {
                                                        if(value.remainder(BigDecimal.valueOf(2)).compareTo(BigDecimal.ZERO) == 0 && inner.compareTo(BigDecimal.ZERO) < 0) {
                                                            error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                            return new ArrayList<>();
                                                        }

                                                        double checkValue = Math.pow(inner.doubleValue(), BigDecimal.ONE.divide(value, MathContext.UNLIMITED).doubleValue());

                                                        if(Double.isNaN(checkValue)) {
                                                            error.add(String.format(LangID.getStringByID("calc_sqrtbase", lang), prefix + "(" + builder + ")"));

                                                            return new ArrayList<>();
                                                        }

                                                        elements.add(new Number(BigDecimal.valueOf(checkValue)));
                                                    }
                                                }
                                            } else {
                                                int len = error.size();

                                                check = calculate(prefix, prefix + "(" + builder + ")", lang);

                                                if(len != error.size()) {
                                                    error.add(String.format(LangID.getStringByID("calc_unknownfunc", lang), prefix + "(" + builder + ")"));

                                                    return new ArrayList<>();
                                                }

                                                elements.add(new Number(check.multiply(inner)));
                                            }
                                    }
                                }
                        }

                        builder.setLength(0);
                    }

                    break;
                case '+':
                case '-':
                case '*':
                case '×':
                case 'x':
                case '/':
                case '÷':
                case '^':
                    if((builder.length() == 0 && i != 0 && equation.charAt(i - 1) != ')' && equation.charAt(i - 1) != '|') || i == equation.length() - 1) {
                        error.add(LangID.getStringByID("calc_alone", lang));

                        return new ArrayList<>();
                    }

                    if(builder.length() == 0 && i == 0 && equation.charAt(i) == '-') {
                        builder.append(equation.charAt(i));

                        continue;
                    }

                    if(builder.length() != 0) {
                        prefix = builder.toString();

                        if(StaticStore.isNumeric(prefix)) {
                            elements.add(new Number(prefix));
                        } else {
                            switch (prefix) {
                                case "pi":
                                case "π":
                                    elements.add(new Number(Math.PI));

                                    break;
                                case "e":
                                    elements.add(new Number(Math.E));

                                    break;
                                default:
                                    if(prefix.endsWith("!")) {
                                        String filtered = prefix.replaceAll("!$", "");

                                        int originalSize = error.size();

                                        BigDecimal valD = calculate(filtered, null, lang);

                                        if(originalSize != error.size()) {
                                            error.add(String.format(LangID.getStringByID("calc_notnum", lang), prefix));

                                            return new ArrayList<>();
                                        } else {
                                            if(valD.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                                                error.add(String.format(LangID.getStringByID("calc_factnum", lang), prefix));

                                                return new ArrayList<>();
                                            } else if(valD.compareTo(BigDecimal.ZERO) < 0) {
                                                error.add(String.format(LangID.getStringByID("calc_factrange", lang), prefix));

                                                return new ArrayList<>();
                                            } else {
                                                elements.add(new Number(factorial(valD.toBigInteger())));
                                            }
                                        }
                                    } else if(prefix.matches("\\d+p\\d+") || prefix.matches("\\d+c\\d+")) {
                                        String[] data;

                                        if(prefix.matches("\\d+p\\d+")) {
                                            data = builder.toString().split("p");
                                        } else {
                                            data = builder.toString().split("c");
                                        }

                                        if(data.length != 2) {
                                            error.add(String.format(LangID.getStringByID("calc_npcrparam", lang), 2, data.length, Arrays.toString(data)));

                                            return new ArrayList<>();
                                        }

                                        for(int j = 0; j < data.length; j++) {
                                            if(StaticStore.isNumeric(data[j])) {
                                                long valL = Long.parseLong(data[j]);
                                                double valD = Double.parseDouble(data[j]);

                                                if(valL != valD) {
                                                    error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang),  prefix, data[j]));

                                                    return new ArrayList<>();
                                                } else if(valL < 0) {
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

                                        if(n.compareTo(r) < 0) {
                                            error.add(String.format(LangID.getStringByID("calc_npcrsize", lang), prefix, data[0], data[1]));
                                        }

                                        if (prefix.matches("\\d+p\\d+")) {
                                            elements.add(new Number(nPr(n, r)));
                                        } else {
                                            elements.add(new Number(nCr(n, r)));
                                        }
                                    } else {
                                        double suffixCheck = checkSuffix(prefix);

                                        if(Double.isNaN(suffixCheck)) {
                                            int len = error.size();

                                            BigDecimal check = calculate(prefix, equation, lang);

                                            if(len != error.size()) {
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

                        builder.setLength(0);
                    }

                    Operator.TYPE operator;

                    switch (equation.charAt(i)) {
                        case '*':
                        case '×':
                        case 'x':
                            operator = Operator.TYPE.MULTIPLICATION;

                            break;
                        case '+':
                            operator = Operator.TYPE.ADDITION;

                            break;
                        case '-':
                            operator = Operator.TYPE.SUBTRACTION;

                            break;
                        case '/':
                        case '÷':
                            operator = Operator.TYPE.DIVISION;

                            break;
                        default:
                            operator = Operator.TYPE.SQUARE;
                    }

                    elements.add(new Operator(operator));

                    break;
                default:
                    builder.append(equation.charAt(i));
            }
        }

        if(builder.length() != 0) {
            String prefix = builder.toString();

            if(StaticStore.isNumeric(prefix)) {
                elements.add(new Number(prefix));
            } else {
                switch (prefix) {
                    case "pi":
                    case "π":
                        elements.add(new Number(Math.PI));

                        break;
                    case "e":
                        elements.add(new Number(Math.E));

                        break;
                    default:
                        if(prefix.endsWith("!")) {
                            String filtered = prefix.replaceAll("!$", "");

                            int originalSize = error.size();

                            BigDecimal valD = calculate(filtered, null, lang);

                            if(originalSize != error.size()) {
                                error.add(String.format(LangID.getStringByID("calc_notnum", lang), prefix));

                                return new ArrayList<>();
                            } else {
                                if(valD.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                                    error.add(String.format(LangID.getStringByID("calc_factnum", lang), prefix));

                                    return new ArrayList<>();
                                } else if(valD.compareTo(BigDecimal.ZERO) < 0) {
                                    error.add(String.format(LangID.getStringByID("calc_factrange", lang), prefix));

                                    return new ArrayList<>();
                                } else {
                                    elements.add(new Number(factorial(valD.toBigInteger())));
                                }
                            }
                        } else if(prefix.matches("\\d+p\\d+") || prefix.matches("\\d+c\\d+")) {
                            String[] data;

                            if(prefix.matches("\\d+p\\d+")) {
                                data = builder.toString().split("p");
                            } else {
                                data = builder.toString().split("c");
                            }

                            if(data.length != 2) {
                                error.add(String.format(LangID.getStringByID("calc_npcrparam", lang), 2, data.length, Arrays.toString(data)));

                                return new ArrayList<>();
                            }

                            for(int j = 0; j < data.length; j++) {
                                if(StaticStore.isNumeric(data[j])) {
                                    long valL = Long.parseLong(data[j]);
                                    double valD = Double.parseDouble(data[j]);

                                    if(valL != valD) {
                                        error.add(String.format(LangID.getStringByID("calc_npcrnoint", lang),  prefix, data[j]));

                                        return new ArrayList<>();
                                    } else if(valL < 0) {
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

                            if(n.compareTo(r) < 0) {
                                error.add(String.format(LangID.getStringByID("calc_npcrsize", lang), prefix + "(" + data[0] + ", " + data[1] + ")", data[0], data[1]));
                            }

                            if (prefix.matches("\\d+p\\d+")) {
                                elements.add(new Number(nPr(n, r)));
                            } else {
                                elements.add(new Number(nCr(n, r)));
                            }
                        } else {
                            double suffixCheck = checkSuffix(prefix);

                            if(Double.isNaN(suffixCheck)) {
                                int len = error.size();

                                BigDecimal check = calculate(prefix, equation, lang);

                                if(len != error.size()) {
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

        if(builder.length() != 0)
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

    private static BigDecimal nPr(BigInteger n, BigInteger r) {
        return factorial(n).divide(factorial(n.subtract(r)), MathContext.UNLIMITED);
    }

    private static BigDecimal nCr(BigInteger n, BigInteger r) {
        return factorial(n).divide(factorial(r).multiply(factorial(n.subtract(r))), MathContext.UNLIMITED);
    }

    private static BigDecimal factorial(BigInteger n) {
        if(n.compareTo(BigInteger.ZERO) < 0)
            return new BigDecimal("1");

        BigDecimal f = new BigDecimal("1");

        for(BigInteger i = new BigInteger("2"); i.compareTo(n) < 0; i = i.add(new BigInteger("1"))) {
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
}
