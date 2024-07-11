package mandarin.packpack.commands.math;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Plot extends TimedConstraintCommand {
    private static final int numberOfElements = 5000;

    public Plot(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMNAD_PLOT_ID, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("plot_formula", lang), loader.getMessage(), a -> a);

            return;
        }

        BigDecimal[] xRange = getXRange(loader.getContent());
        BigDecimal[] yRange = getYRange(loader.getContent());

        String f = filterFormula(loader.getContent());

        String[] test = f.split("=");

        if(test.length > 2) {
            replyToMessageSafely(ch, LangID.getStringByID("plot_invalid", lang), loader.getMessage(), a -> a);

            return;
        }

        Formula formula = new Formula(f, 2, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), loader.getMessage(), a -> a);

            return;
        }

        if(xRange == null) {
            xRange = new BigDecimal[2];

            xRange[0] = new BigDecimal("-5");
            xRange[1] = new BigDecimal("5");
        } else if(xRange[1].subtract(xRange[0]).compareTo(BigDecimal.ZERO) == 0) {
            xRange[0] = new BigDecimal("-5");
            xRange[1] = new BigDecimal("5");
        }

        if(formula.variable.size() == 1) {
            boolean yRangeUpdate = yRange == null;

            if(yRange == null) {
                yRange = new BigDecimal[2];

                yRange[0] = BigDecimal.ZERO;
                yRange[1] = BigDecimal.ZERO;
            }

            BigDecimal[][] coordinates = new BigDecimal[numberOfElements + 1][];

            for(int i = 0; i < numberOfElements + 1; i++) {
                BigDecimal[] c = new BigDecimal[2];

                c[0] = xRange[0].add(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(numberOfElements), Equation.context).multiply(BigDecimal.valueOf(i)));
                c[1] = formula.substitute(c[0]);

                if(!Equation.error.isEmpty() || c[1] == null) {
                    c[1] = null;

                    Equation.error.clear();
                } else if(yRangeUpdate) {
                    if(i == 0) {
                        yRange[0] = c[1];
                        yRange[1] = c[1];
                    } else {
                        yRange[0] = yRange[0].min(c[1]);
                        yRange[1] = yRange[1].max(c[1]);
                    }
                }

                coordinates[i] = c;
            }

            Object[] plots = ImageDrawing.plotGraph(coordinates, xRange, yRange, keepRatio(loader.getContent()), lang);

            if(plots == null) {
                replyToMessageSafely(ch, LangID.getStringByID("plot_fail", lang), loader.getMessage(), a -> a);
            } else {
                sendMessageWithFile(ch, (String) plots[1], (File) plots[0], "plot.png", loader.getMessage());
            }
        } else {
            if(yRange == null) {
                yRange = xRange.clone();
            }

            Object[] plots = ImageDrawing.plotXYGraph(formula, xRange, yRange, keepRatio(loader.getContent()), lang);

            if(plots == null) {
                replyToMessageSafely(ch, LangID.getStringByID("plot_fail", lang), loader.getMessage(), a -> a);
            } else {
                sendMessageWithFile(ch, (String) plots[1], (File) plots[0], "plot.png", loader.getMessage());
            }
        }
    }

    private BigDecimal[] getXRange(String command) {
        Pattern pattern = Pattern.compile("-xr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-xr(\\s+)?", "").replaceAll("\\s", "");

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

    private BigDecimal[] getYRange(String command) {
        Pattern pattern = Pattern.compile("-yr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-yr(\\s+)?", "").replaceAll("\\s", "");

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

    private boolean keepRatio(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-r") || contents[i].equals("-ratio")) {
                return true;
            }
        }

        return false;
    }

    private String filterFormula(String command) {
        String removePrefix = command.split(" ", 2)[1];

        return removePrefix.replaceAll("-(r|ratio)", "").replaceAll("-[xy]r(\\s+)?\\[.+?,.+?]", "");
    }
}
