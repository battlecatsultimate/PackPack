package mandarin.packpack.supporter.calculation;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Formula {
    public static List<String> error = new ArrayList<>();

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
