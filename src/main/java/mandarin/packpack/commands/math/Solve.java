package mandarin.packpack.commands.math;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.calculation.NumericalResult;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.SolutionHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Solve extends TimedConstraintCommand {
    private static final int numberOfElements = 5000;

    public Solve(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_SOLVE_ID, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();

        String[] commands = loader.getContent().split(" ", 2);

        if(commands.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("plot.failed.noFormula", lang), loader.getMessage(), a -> a);

            return;
        }

        String command = commands[1];

        Formula.ROOT ROOT = findAlgorithm(command);
        int iteration = getIteration(command);
        BigDecimal error = getErrorMargin(command);

        String formula = filterFormula(command);

        if(formula.contains("=")) {
            String[] side = formula.split("=");

            if(side.length > 2) {
                replyToMessageSafely(ch, LangID.getStringByID("solve.failed.invalidFormat", lang), loader.getMessage(), a -> a);

                return;
            }

            formula = side[0] + "- (" + side[1] + ")";
        }

        Formula f = new Formula(formula, 1, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), loader.getMessage(), a -> a);

            return;
        }

        BigDecimal[] range = getRange(command);

        if(range == null) {
            range = new BigDecimal[2];

            range[0] = new BigDecimal("-5");
            range[1] = new BigDecimal("5");
        }

        List<BigDecimal[]> targetRanges = new ArrayList<>();

        BigDecimal segment = range[1].subtract(range[0]).divide(BigDecimal.valueOf(numberOfElements), Equation.context);

        for(int i = 0; i < numberOfElements; i++) {
            BigDecimal s = range[0].add(range[1].subtract(range[0]).divide(BigDecimal.valueOf(numberOfElements), Equation.context).multiply(BigDecimal.valueOf(i)));
            BigDecimal e = range[0].add(range[1].subtract(range[0]).divide(BigDecimal.valueOf(numberOfElements), Equation.context).multiply(BigDecimal.valueOf(i + 1)));

            BigDecimal sy = f.substitute(s);

            if(!Equation.error.isEmpty() || sy == null) {
                Equation.error.clear();
                f.element.reset();

                continue;
            }

            BigDecimal ey = f.substitute(e);

            if(!Equation.error.isEmpty() || ey == null) {
                Equation.error.clear();
                f.element.reset();

                i++;

                continue;
            }

            int ssig = sy.signum();
            int esig = ey.signum();

            if(ssig * esig <= 0) {
                targetRanges.add(new BigDecimal[] {s, e});

                if(ssig * esig == 0) {
                    i++;
                }
            }
        }

        if(targetRanges.isEmpty()) {
            replyToMessageSafely(ch, String.format(LangID.getStringByID("solve.noRoot", lang), Equation.formatNumber(range[0]), Equation.formatNumber(range[1])), loader.getMessage(), a -> a);

            return;
        }

        if(iteration == -1 && error.compareTo(BigDecimal.ONE.negate()) == 0)
            error = Formula.minimumError;

        int success = 0;
        int fail = 0;

        List<NumericalResult> solutions = new ArrayList<>();
        String summary;

        List<Integer> fakeResults = new ArrayList<>();

        if(error.compareTo(BigDecimal.ONE.negate()) != 0) {
            for(int i = 0; i < targetRanges.size(); i++) {
                NumericalResult result = f.solveByError(targetRanges.get(i)[0], targetRanges.get(i)[1], error, ROOT, lang);

                if(result != null) {
                    if (result.value.compareTo(targetRanges.get(i)[0].subtract(segment)) < 0 || result.value.compareTo(targetRanges.get(i)[1].add(segment)) > 0) {
                        fakeResults.add(i);

                        continue;
                    }

                    solutions.add(result);

                    success++;
                } else {
                    Equation.error.clear();
                    Formula.error.clear();

                    fail++;
                }
            }

            List<BigDecimal[]> actualResults = new ArrayList<>();

            for (int i = 0; i < targetRanges.size(); i++) {
                if (!fakeResults.contains(i)) {
                    actualResults.add(targetRanges.get(i));
                }
            }

            targetRanges.clear();
            targetRanges.addAll(actualResults);

            summary = String.format(LangID.getStringByID("solve.success.error", lang), Equation.formatNumber(range[0]), Equation.formatNumber(range[1]), targetRanges.size(), success, fail, Equation.formatNumber(error), getAlgorithmName(ROOT));
        } else {
            for(int i = 0; i < targetRanges.size(); i++) {
                NumericalResult result = f.solveByIteration(targetRanges.get(i)[0], targetRanges.get(i)[1], iteration, ROOT, lang);

                if(result != null) {
                    if (result.value.compareTo(targetRanges.get(i)[0].subtract(segment)) < 0 || result.value.compareTo(targetRanges.get(i)[1].add(segment)) > 0) {
                        fakeResults.add(i);

                        continue;
                    }

                    solutions.add(result);

                    success++;
                } else {
                    Equation.error.clear();
                    Formula.error.clear();

                    fail++;
                }
            }

            List<BigDecimal[]> actualResults = new ArrayList<>();

            for (int i = 0; i < targetRanges.size(); i++) {
                if (!fakeResults.contains(i)) {
                    actualResults.add(targetRanges.get(i));
                }
            }

            targetRanges.clear();
            targetRanges.addAll(actualResults);

            summary = String.format(LangID.getStringByID("solve.success.iteration", lang), Equation.formatNumber(range[0]), Equation.formatNumber(range[1]), targetRanges.size(), success, fail, iteration, getAlgorithmName(ROOT));
        }

        if(!targetRanges.isEmpty() && success == 0) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), loader.getMessage(), a -> a);

            return;
        }

        StringBuilder sb = new StringBuilder("```ansi\n");

        List<String> data = accumulateListData(solutions, targetRanges);

        for(int i = 0; i < data.size(); i++) {
            sb.append(data.get(i));
            
            if(i < data.size() - 1)
                sb.append("\n\n");
            else
                sb.append("\n");
        }

        if(solutions.size() > 5) {
            int totalPage = solutions.size() / 5;

            if(solutions.size() % 5 != 0)
                totalPage++;

            sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
        }

        sb.append("```");

        replyToMessageSafely(ch, summary + sb, loader.getMessage(), a -> a.setComponents(getComponents(solutions)), msg -> {
            if(solutions.size() > 5) {
                StaticStore.putHolder(u.getId(), new SolutionHolder(loader.getMessage(), u.getId(), ch.getId(), msg, summary, targetRanges, solutions, lang));
            }
        });
    }

    private Formula.ROOT findAlgorithm(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-f", "-false" -> {
                    return Formula.ROOT.FALSE_POSITION;
                }
                case "-n", "-newton" -> {
                    return Formula.ROOT.NEWTON_RAPHSON;
                }
                case "-s", "-secant" -> {
                    return Formula.ROOT.SECANT;
                }
                case "-b", "-bisection" -> {
                    return Formula.ROOT.BISECTION;
                }
            }
        }

        return Formula.ROOT.SMART;
    }

    private int getIteration(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-i") || contents[i].equals("-iteration")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return Math.min(Formula.maximumIteration, Math.max(1, StaticStore.safeParseInt(contents[i + 1])));
            }
        }

        return -1;
    }

    private BigDecimal getErrorMargin(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-e") || contents[i].equals("-error")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return BigDecimal.ZERO.max(new BigDecimal(contents[i + 1]));
            }
        }

        return BigDecimal.ONE.negate();
    }

    private String filterFormula(String command) {
        command = command.replaceAll("-(f|false|n|newton|s|secant|b|bisection)", "");
        command = command.replaceAll("-(e|error|i|iteration)\\s+(\\S+)?", "");
        return command.replaceAll("-r(\\s+)?\\[.+?,.+?]", "");
    }

    private BigDecimal[] getRange(String command) {
        Pattern pattern = Pattern.compile("-r(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-r(\\s+)?", "").replaceAll("\\s", "");

            String[] removed = filtered.substring(1, filtered.length() - 1).split(",");

            if(removed.length == 2) {
                BigDecimal minimum = Equation.calculate(removed[0], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                BigDecimal maximum = Equation.calculate(removed[1], null, false, lang);

                if(!Equation.error.isEmpty()) {
                    Equation.error.clear();

                    return null;
                }

                return new BigDecimal[] {minimum.min(maximum), maximum.max(minimum)};
            }
        }

        return null;
    }

    private String getAlgorithmName(Formula.ROOT ROOT) {
        return switch (ROOT) {
            case NEWTON_RAPHSON -> LangID.getStringByID("calculator.algorithm.solve.newtonRaphson", lang);
            case FALSE_POSITION -> LangID.getStringByID("calculator.algorithm.solve.falsePosition", lang);
            case SECANT -> LangID.getStringByID("calculator.algorithm.solve.secant", lang);
            case BISECTION -> LangID.getStringByID("calculator.algorithm.solve.bisection", lang);
            case SMART -> LangID.getStringByID("calculator.algorithm.solve.auto", lang);
        };
    }

    public List<String> accumulateListData(List<NumericalResult> solutions, List<BigDecimal[]> targetRanges) {
        List<String> result = new ArrayList<>();

        for(int i = 0; i < 5; i++) {
            if(i >= solutions.size())
                break;

            NumericalResult solution = solutions.get(i);

            result.add(String.format(
                    LangID.getStringByID("solve.root", lang),
                    i + 1,
                    Equation.formatNumber(solution.value),
                    Equation.formatNumber(targetRanges.get(i)[0]),
                    Equation.formatNumber(targetRanges.get(i)[1]),
                    Equation.formatNumber(solution.error)
            ));
        }

        return result;
    }

    public List<ActionRow> getComponents(List<NumericalResult> solutions) {
        int totalPage = solutions.size() / 5;

        if(solutions.size() % 5 != 0)
            totalPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(solutions.size() > 5) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            rows.add(ActionRow.of(buttons));
        }

        return rows;
    }
}
