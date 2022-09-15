package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Equation {
    public static List<String> error = new ArrayList<>();
    public static DecimalFormat df = new DecimalFormat("#.######");

    private static final String[] suffix = { "k", "m", "b", "t" };

    public static double calculate(String equation, String parent) {
        if(equation.equals(parent)) {
            error.add("calc_notnum | " + equation);

            return 0;
        }

        List<Element> elements = parse(equation);

        if(elements.isEmpty())
            return 0;

        List<Element> filtered = new ArrayList<>();

        //Remove squares
        for(int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);

            if(e instanceof Operator && ((Operator) e).type == Operator.TYPE.SQUARE) {
                if(i == 0 || i == elements.size() - 1 || !(elements.get(i - 1) instanceof Number) || !(elements.get(i + 1) instanceof Number)) {
                    StaticStore.logger.uploadLog("W/Equation::calculate - Invalid equation format in square process : " + equation + "\n\nData : " + elements);

                    continue;
                }

                filtered.add(new Number(((Operator) e).calculate((Number) filtered.get(filtered.size() - 1), (Number) elements.get(i + 1))));
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

                filtered.add(new Number(((Operator) e).calculate((Number) filtered.get(filtered.size() - 1), (Number) elements.get(i + 1))));
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

                filtered.add(new Number(((Operator) e).calculate((Number) filtered.get(filtered.size() - 1), (Number) elements.get(i + 1))));
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

            error.add("calc_fail");

            return 0;
        } else {
            return ((Number) elements.get(0)).value;
        }
    }

    public static String getErrorMessage(int lang) {
        StringBuilder builder = new StringBuilder();

        List<String> realError = new ArrayList<>();

        for(String e : error) {
            if(!realError.contains(e))
                realError.add(e);
        }

        for(int i = 0; i < realError.size(); i++) {
            String[] data = realError.get(i).split(" \\| ");

            if(data.length == 1) {
                builder.append(LangID.getStringByID(data[0], lang));
            } else {
                builder.append(LangID.getStringByID(data[0], lang).replace("_", data[1]));
            }

            if(i < realError.size() - 1) {
                builder.append("\n\n");
            }
        }

        error.clear();

        return builder.toString();
    }

    private static List<Element> parse(String equation) {
        if(openedBracket(equation)) {
            error.add("calc_opened");

            return new ArrayList<>();
        }

        equation = equation.replaceAll("[\\[|{]", "(").replaceAll("[]|}]", ")").replaceAll("\\)\\(", ")*(");

        List<Element> elements = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < equation.length(); i++) {
            switch (equation.charAt(i)) {
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
                        double inner = calculate(builder.toString(), equation);

                        if(StaticStore.isNumeric(prefix)) {
                            elements.add(new Number(StaticStore.safeParseInt(prefix) * inner));
                        } else {
                            switch (prefix) {
                                case "sin":
                                    elements.add(new Number(Math.sin(inner)));

                                    break;
                                case "cos":
                                    elements.add(new Number(Math.cos(inner)));

                                    break;
                                case "tan":
                                    if(inner % (Math.PI / 2) == 0) {
                                        error.add("calc_tan | tan(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.tan(inner)));

                                    break;
                                case "csc":
                                    double check = Math.sin(inner);

                                    if(check == 0) {
                                        error.add("calc_csc | csc(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(1.0 / check));

                                    break;
                                case "sec":
                                    check = Math.cos(inner);

                                    if(check == 0) {
                                        error.add("calc_sec | sec(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(1.0 / check));

                                    break;
                                case "cot":
                                    if(inner % Math.PI == 0) {
                                        error.add("calc_cot | cot(" + builder + ")");
                                    } else if(inner % (Math.PI / 2) == 0) {
                                        elements.add(new Number(0));
                                    } else {
                                        elements.add(new Number(1.0 / Math.tan(inner)));
                                    }

                                    break;
                                case "ln":
                                case "loge":
                                    if(inner <= 0) {
                                        error.add("calc_ln | " + prefix + "(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.log(inner)));

                                    break;
                                case "log":
                                    if(inner <= 0) {
                                        error.add("calc_ln | log(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.log10(inner)));

                                    break;
                                case "sqrt":
                                case "root":
                                case "sqrt2":
                                    if(inner < 0) {
                                        error.add("calc_sqrt | " + prefix + "(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.sqrt(inner)));

                                    break;
                                case "exp":
                                    elements.add(new Number(Math.pow(Math.E, inner)));

                                    break;
                                case "arcsin":
                                case "asin":
                                    if(inner < -1 || inner > 1) {
                                        error.add("calc_arcsin | " + prefix + "(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.asin(inner)));

                                    break;
                                case "arccos":
                                case "acos":
                                    if(inner < -1 || inner > 1) {
                                        error.add("calc_arccos | " + prefix + "(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.acos(inner)));

                                    break;
                                case "arctan":
                                case "atan":
                                    elements.add(new Number(Math.atan(inner)));

                                    break;
                                case "arccsc":
                                case "acsc":
                                    if(-1 < inner && inner < 1) {
                                        error.add("calc_arccsc | " + prefix + "(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.asin(1.0 / inner)));

                                    break;
                                case "arcsec":
                                case "asec":
                                    if(-1 < inner && inner < 1) {
                                        error.add("calc_arcsec | " + prefix + "(" + builder + ")");

                                        return new ArrayList<>();
                                    }

                                    elements.add(new Number(Math.acos(1.0 / inner)));

                                    break;
                                case "arccot":
                                case "acot":
                                    if (inner == 0) {
                                        elements.add(new Number(Math.PI / 2));
                                    } else {
                                        elements.add(new Number(Math.atan(1.0 / inner)));
                                    }

                                    break;
                                default:
                                    if(prefix.startsWith("log")) {
                                        String base = prefix.replaceAll("^log", "");

                                        if(StaticStore.isNumeric(base)) {
                                            double value = Double.parseDouble(base);

                                            if(value <= 0) {
                                                error.add("calc_logbase | " + prefix + "(" + builder + ")");

                                                return new ArrayList<>();
                                            }
                                            elements.add(new Number(Math.log(inner) / Math.log(Double.parseDouble(base))));
                                        } else {
                                            int originalLength = error.size();

                                            double value = calculate(base, prefix + "(" + builder + ")");

                                            if(originalLength != error.size()) {
                                                error.add("calc_unknownfunc | " + prefix + "(" + builder + ")");

                                                return new ArrayList<>();
                                            } else {
                                                if(value <= 0) {
                                                    error.add("calc_logbase | " + prefix + "(" + builder + ")");

                                                    return new ArrayList<>();
                                                }

                                                elements.add(new Number(Math.log(inner) / Math.log(value)));
                                            }
                                        }
                                    } else if(prefix.startsWith("sqrt")) {
                                        String base = prefix.replaceAll("^sqrt", "");

                                        if(StaticStore.isNumeric(base)) {
                                            double value = Double.parseDouble(base);

                                            if(value % 2 == 0) {
                                                error.add("calc_sqrtbase | " + prefix + "(" + builder + ")");

                                                return new ArrayList<>();
                                            }

                                            check = Math.pow(inner, 1.0 / value);

                                            if(Double.isNaN(check)) {
                                                error.add("calc_sqrtbase | " + prefix + "(" + builder + ")");

                                                return new ArrayList<>();
                                            }

                                            elements.add(new Number(check));
                                        } else {
                                            int originalLength = error.size();

                                            double value = calculate(base, prefix + "(" + builder + ")");

                                            if(originalLength != error.size()) {
                                                error.add("calc_unknownfunc | " + prefix + "(" + builder + ")");

                                                return new ArrayList<>();
                                            } else {
                                                if(value % 2 == 0) {
                                                    error.add("calc_sqrtbase | " + prefix + "(" + builder + ")");

                                                    return new ArrayList<>();
                                                }

                                                check = Math.pow(inner, 1.0 / value);

                                                if(Double.isNaN(check)) {
                                                    error.add("calc_sqrtbase | " + prefix + "(" + builder + ")");

                                                    return new ArrayList<>();
                                                }

                                                elements.add(new Number(check));
                                            }
                                        }
                                    } else {
                                        int len = error.size();

                                        check = calculate(prefix, prefix + "(" + builder + ")");

                                        if(len != error.size()) {
                                            error.add("calc_unknownfunc | " + prefix + "(" + builder + ")");

                                            return new ArrayList<>();
                                        }

                                        elements.add(new Number(check * inner));
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
                    if((builder.length() == 0 && i != 0 && equation.charAt(i - 1) != ')') || i == equation.length() - 1) {
                        error.add("calc_alone");

                        return new ArrayList<>();
                    }

                    if(builder.length() == 0 && i == 0 && equation.charAt(i) == '-') {
                        builder.append(equation.charAt(i));

                        continue;
                    }

                    if(builder.length() != 0) {
                        prefix = builder.toString();

                        if(StaticStore.isNumeric(prefix)) {
                            elements.add(new Number(Double.parseDouble(prefix)));
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
                                    double suffixCheck = checkSuffix(prefix);

                                    if(Double.isNaN(suffixCheck)) {
                                        int len = error.size();

                                        double check = calculate(prefix, equation);

                                        if(len != error.size()) {
                                            error.add("calc_notnum | " + prefix);

                                            return new ArrayList<>();
                                        } else {
                                            elements.add(new Number(check));
                                        }
                                    } else {
                                        elements.add(new Number(suffixCheck));
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
                elements.add(new Number(Double.parseDouble(prefix)));
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
                        double suffixCheck = checkSuffix(prefix);

                        if(Double.isNaN(suffixCheck)) {
                            int len = error.size();

                            double check = calculate(prefix, equation);

                            if(len != error.size()) {
                                error.add("calc_notnum | " + prefix);

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

        return elements;
    }

    private static boolean openedBracket(String raw) {
        int open = 0;

        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '(')
                open++;
            else if (raw.charAt(i) == ')')
                open--;
        }

        return open != 0;
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
}
