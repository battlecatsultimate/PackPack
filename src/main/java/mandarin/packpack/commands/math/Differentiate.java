package mandarin.packpack.commands.math;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("differentiate.failed.noFormulaValue", lang));

            return;
        }

        String command = contents[1];

        String v = findValue(command);

        if(v == null || v.isBlank()) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("differentiate.failed.noValue", lang));

            return;
        }

        BigDecimal value = Equation.calculate(v, null, false, lang);

        if(!Equation.error.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), Equation.getErrorMessage(LangID.getStringByID("differentiate.failed.calculationFailed", lang)));

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
            replyToMessageSafely(ch, loader.getMessage(), Equation.getErrorMessage(LangID.getStringByID("differentiate.failed.stepFailed", lang)));

            return;
        }

        if(step.compareTo(BigDecimal.ZERO) == 0) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("differentiate.failed.zeroStep", lang));

            return;
        }

        String f = filterFormula(command).trim();

        if(f.isBlank()) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("differentiate.failed.noFormula", lang));

            return;
        }

        Formula formula = new Formula(f, 1, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), Formula.getErrorMessage());

            return;
        }

        formula.substitute(value);

        if(formula.element.isCritical()) {
            replyToMessageSafely(ch, loader.getMessage(), Equation.getErrorMessage(LangID.getStringByID("differentiate.failed.invalidPoint", lang)));

            return;
        }

        BigDecimal result = formula.differentiate(value, step, getSnap(command), lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), Formula.getErrorMessage());

            return;
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## " + LangID.getStringByID("differentiate.result", lang)));
        components.add(Separator.create(true, Separator.Spacing.LARGE));
        components.add(TextDisplay.of(
                LangID.getStringByID("differentiate.success.description", lang) + "\n\n" +
                LangID.getStringByID("differentiate.success.formula", lang).formatted(Equation.formatNumber(value), Equation.formatNumber(result))
        ));
        components.add(Separator.create(true, Separator.Spacing.LARGE));
        components.add(TextDisplay.of(
                LangID.getStringByID("differentiate.success.algorithm", lang).formatted(getAlgorithmName(getSnap(command))) + "\n" +
                        LangID.getStringByID("differentiate.success.step", lang).formatted(Equation.formatNumber(step))
        ));

        replyToMessageSafely(ch, loader.getMessage(), Container.of(components));
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
