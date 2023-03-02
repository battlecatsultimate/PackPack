package mandarin.packpack.commands.math;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
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

public class Integrate extends TimedConstraintCommand {
    public Integrate(ConstraintCommand.ROLE role, int lang, @Nullable IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_INTEGRATE_ID, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ", 2);

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("int_forran", lang), getMessage(event), a -> a);

            return;
        }

        String command = contents[1];

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

        String[] r = findRange(command);

        if(r == null) {
            replyToMessageSafely(ch, LangID.getStringByID("int_norange", lang), getMessage(event), a -> a);

            return;
        }

        if(r.length > 2) {
            replyToMessageSafely(ch, LangID.getStringByID("int_toorange", lang), getMessage(event), a -> a);

            return;
        }

        BigDecimal[] range = new BigDecimal[2];

        range[0] = Equation.calculate(r[0], null, false, lang);

        if(!Equation.error.isEmpty()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("int_rangefail", lang)), getMessage(event), a -> a);

            return;
        }

        range[1] = Equation.calculate(r[1], null, false, lang);

        if(!Equation.error.isEmpty()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("int_rangefail", lang)), getMessage(event), a -> a);

            return;
        }

        String s = findSection(command);

        int section;

        if(s == null) {
            section = Formula.maximumSections;
        } else {
            BigDecimal trial = Equation.calculate(s, null, false, lang);

            if(!Equation.error.isEmpty()) {
                replyToMessageSafely(ch, Equation.getErrorMessage(LangID.getStringByID("int_sectionfail", lang)), getMessage(event), a -> a);

                return;
            }

            section = Math.min(trial.intValue(), Formula.maximumSections);
        }

        if(section <= 0) {
            replyToMessageSafely(ch, LangID.getStringByID("int_sectionzero", lang), getMessage(event), a -> a);

            return;
        }

        Formula.INTEGRATION algorithm = findAlgorithm(command);

        BigDecimal result = formula.integrate(range[0], range[1], section, algorithm, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Equation.getErrorMessage(Formula.getErrorMessage()), getMessage(event), a -> a);
        } else {
            replyToMessageSafely(ch, String.format(LangID.getStringByID("int_success", lang), Equation.formatNumber(result), Equation.formatNumber(range[0]), Equation.formatNumber(range[1]), section, getAlgorithmName(algorithm)), getMessage(event), a -> a);
        }
    }

    private String[] findRange(String command) {
        Pattern pattern = Pattern.compile("-r(ange)?(\\s+)?\\[.+?,.+?]");

        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().split("\\[")[1];

            return filtered.substring(0, filtered.length() - 1).split(",");
        }

        return null;
    }

    private String findSection(String command) {
        Pattern pattern = Pattern.compile("-s(ection)?(\\s+)?\\[.+?]");

        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().split("\\[")[1];

            return filtered.substring(0, filtered.length() - 1);
        }

        return null;
    }

    private String filterFormula(String command) {
        return command.replaceAll("-r(ange)?(\\s+)?\\[.+?,.+?]", "").replaceAll("-(t|trapezoidal|si|simpson|s38|simpson38|b|boole)", "").replaceAll("-s(ection)?(\\s+)?\\[.+?]", "");
    }

    private Formula.INTEGRATION findAlgorithm(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-t":
                case "-trapezoidal":
                    return Formula.INTEGRATION.TRAPEZOIDAL;
                case "-si":
                case "-simpson":
                    return Formula.INTEGRATION.SIMPSON;
                case "-s38":
                case "-simpson38":
                    return Formula.INTEGRATION.SIMPSON38;
                case "-b":
                case "-boole":
                    return Formula.INTEGRATION.BOOLE;
            }
        }

        return Formula.INTEGRATION.SMART;
    }

    private String getAlgorithmName(Formula.INTEGRATION algorithm) {
        switch (algorithm) {
            case TRAPEZOIDAL:
                return LangID.getStringByID("calc_trapezoidal", lang);
            case SIMPSON:
                return LangID.getStringByID("calc_simpson", lang);
            case SIMPSON38:
                return LangID.getStringByID("calc_simpson38", lang);
            case BOOLE:
                return LangID.getStringByID("calc_boole", lang);
            case SMART:
                return LangID.getStringByID("calc_newtoncotes", lang);
            default:
                throw new IllegalStateException("Invalid algorithm : " + algorithm);
        }
    }
}
