package mandarin.packpack.commands.math;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Differentiate extends ConstraintCommand {
    public Differentiate(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ", 2);

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("differentiate.failed.noFormulaValue", lang), loader.getMessage(), a -> a);

            return;
        }

        String command = contents[1];

        String v = findValue(command);

        if(v == null || v.isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("differentiate.failed.noValue", lang), loader.getMessage(), a -> a);

            return;
        }

        BigDecimal value = Equation.calculate(v, null, false, lang);

        if(!Equation.error.isEmpty()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("differentiate.failed.calculationFailed", lang)), loader.getMessage(), a -> a);

            return;
        }

        String s = getStep(command);

        BigDecimal step;

        if(s == null || s.isBlank()) {
            step = Formula.H;
        } else {
            step = Equation.calculate(s, null, false, lang);
        }

        if(!Equation.error.isEmpty()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("differentiate.failed.stepFailed", lang)), loader.getMessage(), a -> a);

            return;
        }

        if(step.compareTo(BigDecimal.ZERO) == 0) {
            replyToMessageSafely(ch, LangID.getStringByID("differentiate.failed.zeroStep", lang), loader.getMessage(), a -> a);

            return;
        }

        String f = filterFormula(command).trim();

        if(f.isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("differentiate.failed.noFormula", lang), loader.getMessage(), a -> a);

            return;
        }

        Formula formula = new Formula(f, 1, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), loader.getMessage(), a -> a);

            return;
        }

        formula.substitute(value);

        if(formula.element.isCritical()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("differentiate.failed.invalidPoint", lang)), loader.getMessage(), a -> a);

            return;
        }

        BigDecimal result = formula.differentiate(value, step, getSnap(command), lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), loader.getMessage(), a -> a);

            return;
        }

        replyToMessageSafely(ch, String.format(LangID.getStringByID("differentiate.success", lang), Equation.formatNumber(value), Equation.formatNumber(result), getAlgorithmName(getSnap(command)), Equation.formatNumber(step)), loader.getMessage(), a -> a);
    }

    private String findValue(String command) {
        Pattern pattern = Pattern.compile("-v(alue)?(\\s+)?\\[.+?]");

        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().split("\\[")[1];

            return filtered.substring(0, filtered.length() - 1);
        }

        return null;
    }

    private String filterFormula(String command) {
        return command.replaceAll("-v(alue)?(\\s+)?\\[.+?]", "").replaceAll("-(f|front|c|center|b|back)", "").replaceAll("-s(tep)?(\\s+)?\\[.+?]", "");
    }

    private Formula.SNAP getSnap(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-f", "-front" -> {
                    return Formula.SNAP.FRONT;
                }
                case "-c", "-center" -> {
                    return Formula.SNAP.CENTER;
                }
                case "-b", "-back" -> {
                    return Formula.SNAP.BACK;
                }
            }
        }

        return Formula.SNAP.CENTER;
    }

    private String getStep(String command) {
        Pattern pattern = Pattern.compile("-s(tep)?(\\s+)?\\[.+?]");

        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().split("\\[")[1];

            return filtered.substring(0, filtered.length() - 1);
        }

        return null;
    }

    private String getAlgorithmName(Formula.SNAP snap) {
        return switch (snap) {
            case BACK -> LangID.getStringByID("calculator.algorithm.differentiate.backward", lang);
            case CENTER -> LangID.getStringByID("calculator.algorithm.differentiate.central", lang);
            case FRONT -> LangID.getStringByID("calculator.algorithm.differentiate.forward", lang);
        };
    }
}
