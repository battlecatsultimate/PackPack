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
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import javax.annotation.Nonnull;

import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("tPlot.failed.noFunction.x", lang));

            return;
        }

        Formula fx = new Formula(xt, 1, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), Formula.getErrorMessage());

            return;
        }

        String yt = getYt(command);

        if(yt == null) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("tPlot.failed.noFunction.y", lang));

            return;
        }

        Formula fy = new Formula(yt, 1, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), Formula.getErrorMessage());

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
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("plot.failed.noImage", lang));
        } else {
            List<ContainerChildComponent> components = new ArrayList<>();

            components.add(TextDisplay.of("## " + LangID.getStringByID("tPlot.result", lang)));
            components.add(Separator.create(true, Separator.Spacing.LARGE));
            components.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData((File) plots[0], "plot.png"))));
            components.add(Separator.create(true, Separator.Spacing.LARGE));
            components.add(TextDisplay.of(plots[1] + "\n\n" + plots[2] + "\n" + plots[3]));

            replyToMessageSafely(ch, loader.getMessage(), msg -> StaticStore.deleteFile((File) plots[0], true), e -> {
                StaticStore.logger.uploadErrorLog(e, "E/TPlot::doSomething - Failed to send plot image file");

                StaticStore.deleteFile((File) plots[0], true);
            }, Container.of(components));
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
