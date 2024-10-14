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
import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TPlot extends TimedConstraintCommand {
    private static final int numberOfElements = 5000;

    public TPlot(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_TPLOT_ID, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String command = loader.getContent();

        String xt = getXt(command);

        if(xt == null) {
            replyToMessageSafely(ch, LangID.getStringByID("tPlot.failed.noFunction.x", lang), loader.getMessage(), a -> a);

            return;
        }

        Formula fx = new Formula(xt, 1, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), loader.getMessage(), a -> a);

            return;
        }

        String yt = getYt(command);

        if(yt == null) {
            replyToMessageSafely(ch, LangID.getStringByID("tPlot.failed.noFunction.y", lang), loader.getMessage(), a -> a);

            return;
        }

        Formula fy = new Formula(yt, 1, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, Formula.getErrorMessage(), loader.getMessage(), a -> a);

            return;
        }

        BigDecimal[] tRange = getTRange(command);

        if(tRange == null || tRange[0].compareTo(tRange[1]) == 0) {
            tRange = new BigDecimal[2];

            tRange[0] = new BigDecimal("-10");
            tRange[1] = new BigDecimal("10");
        }

        BigDecimal[][] coordinates = new BigDecimal[numberOfElements + 1][];

        BigDecimal[] xRange = new BigDecimal[2];
        BigDecimal[] yRange = new BigDecimal[2];

        xRange[0] = BigDecimal.ZERO;
        xRange[1] = BigDecimal.ONE;

        yRange[0] = BigDecimal.ZERO;
        yRange[1] = BigDecimal.ONE;

        for(int t = 0; t < numberOfElements + 1; t++) {
            BigDecimal[] c = new BigDecimal[2];

            BigDecimal tc = tRange[0].add(tRange[1].subtract(tRange[0]).divide(BigDecimal.valueOf(numberOfElements), Equation.context).multiply(BigDecimal.valueOf(t)));

            c[0] = fx.substitute(tc);

            if(!Equation.error.isEmpty() || c[0] == null) {
                c[0] = null;
                c[1] = null;

                coordinates[t] = c;

                Equation.error.clear();

                continue;
            }

            c[1] = fy.substitute(tc);

            if(!Equation.error.isEmpty() || c[1] == null) {
                c[0] = null;
                c[1] = null;

                Equation.error.clear();
            } else {
                if(t == 0) {
                    xRange[0] = c[0];
                    xRange[1] = c[0];

                    yRange[0] = c[1];
                    yRange[1] = c[1];
                } else {
                    xRange[0] = c[0].min(xRange[0]);
                    xRange[1] = c[0].max(xRange[1]);

                    yRange[0] = c[1].min(yRange[0]);
                    yRange[1] = c[1].max(yRange[1]);
                }
            }

            coordinates[t] = c;
        }

        Object[] plots = ImageDrawing.plotTGraph(coordinates, xRange, yRange, tRange, keepRatio(loader.getContent()), lang);

        if(plots == null) {
            replyToMessageSafely(ch, LangID.getStringByID("plot.failed.noImage", lang), loader.getMessage(), a -> a);
        } else {
            sendMessageWithFile(ch, (String) plots[1], (File) plots[0], "plot.png", loader.getMessage());
        }
    }

    private String getXt(String command) {
        String[] contents = command.replaceAll("-tr(\\s+)?\\[.+?,.+?]", "").split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-x") || contents[i].equals("-xt")) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(contents[j].equals("-r") || contents[j].equals("-ratio"))
                        continue;

                    if(!contents[j].equals("-y") && !contents[j].equals("-yt")) {
                        builder.append(contents[j]);

                        if(j < contents.length - 1)
                            builder.append(" ");
                    } else {
                        break;
                    }
                }

                return builder.isEmpty() ? null : builder.toString();
            }
        }

        return null;
    }

    private String getYt(String command) {
        String[] contents = command.replaceAll("-tr(\\s+)?\\[.+?,.+?]", "").split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-y") || contents[i].equals("-yt")) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(contents[j].equals("-r") || contents[j].equals("-ratio"))
                        continue;

                    if(!contents[j].equals("-x") && !contents[j].equals("-xt")) {
                        builder.append(contents[j]);

                        if(j < contents.length - 1)
                            builder.append(" ");
                    } else {
                        break;
                    }
                }

                return builder.isEmpty() ? null : builder.toString();
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

    private BigDecimal[] getTRange(String command) {
        Pattern pattern = Pattern.compile("-tr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-tr(\\s+)?", "").replaceAll("\\s", "");

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
}
