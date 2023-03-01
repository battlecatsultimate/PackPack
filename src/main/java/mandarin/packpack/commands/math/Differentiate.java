package mandarin.packpack.commands.math;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Differentiate extends ConstraintCommand {
    public Differentiate(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ", 2);

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("diff_noforval", lang), getMessage(event), a -> a);
        }

        String command = contents[1];

        String v = findValue(command);

        if(v == null || v.isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("diff_novalue", lang), getMessage(event), a -> a);

            return;
        }

        BigDecimal value = Equation.calculate(v, null, false, lang);

        if(!Equation.error.isEmpty()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("diff_valuefail", lang)), getMessage(event), a -> a);

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
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("diff_stepfail", lang)), getMessage(event), a -> a);

            return;
        }

        if(step.compareTo(BigDecimal.ZERO) == 0) {
            replyToMessageSafely(ch, LangID.getStringByID("diff_stepzero", lang), getMessage(event), a -> a);

            return;
        }

        String f = filterFormula(command).trim();

        if(f.isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("diff_noformula", lang), getMessage(event), a -> a);

            return;
        }

        Formula formula = new Formula(f, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), getMessage(event), a -> a);

            return;
        }

        Equation.calculate(formula.substitute(value.toPlainString(), lang), null, false, lang);

        if(!Equation.error.isEmpty()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("diff_cant", lang)), getMessage(event), a -> a);

            return;
        }

        BigDecimal result = formula.differentiate(value, step, getSnap(command), lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), getMessage(event), a -> a);

            return;
        }

        replyToMessageSafely(ch, String.format(LangID.getStringByID("diff_success", lang), Equation.formatNumber(value), Equation.formatNumber(result), getAlgorithmName(getSnap(command)), Equation.formatNumber(step)), getMessage(event), a -> a);
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
                case "-f":
                case "-front":
                    return Formula.SNAP.FRONT;
                case "-c":
                case "-center":
                    return Formula.SNAP.CENTER;
                case "-b":
                case "-back":
                    return Formula.SNAP.BACK;
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
        switch (snap) {
            case BACK:
                return LangID.getStringByID("calc_back", lang);
            case CENTER:
                return LangID.getStringByID("calc_center", lang);
            case FRONT:
                return LangID.getStringByID("calc_front", lang);
            default:
                throw new IllegalStateException("Invalid snapping algorithm : " + snap);
        }
    }
}
