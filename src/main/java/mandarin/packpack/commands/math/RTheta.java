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

public class RTheta extends TimedConstraintCommand {
    public RTheta(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_RTHETA_ID, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("plot.failed.noFormula", lang));

            return;
        }

        BigDecimal[] xRange = getXRange(loader.getContent());
        BigDecimal[] yRange = getYRange(loader.getContent());
        BigDecimal[] tRange = getTRange(loader.getContent());
        BigDecimal[] rRange = getRRange(loader.getContent());

        String f = filterFormula(loader.getContent());

        String[] test = f.split("=");

        if(test.length > 2) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("plot.failed.invalidFormat", lang));

            return;
        }

        Formula formula = new Formula(f, 2, lang);

        if(!Formula.error.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), Formula.getErrorMessage());

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

        if(yRange == null) {
            yRange = xRange.clone();
        }

        if(tRange == null) {
            tRange = new BigDecimal[] {
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(Math.PI * 2)
            };
        }

        BigDecimal best = xRange[0].abs().max(xRange[1].abs()).max(yRange[0].abs()).max(yRange[1].abs()).multiply(BigDecimal.valueOf(2).sqrt(Equation.context));

        if(rRange == null || rRange[0].compareTo(rRange[1]) == 0) {
            rRange = new BigDecimal[] {
                    best.negate(),
                    best
            };
        }

        double[] tr = new double[2];
        double[] rr = new double[2];

        for(int i = 0; i < tr.length; i++) {
            tr[i] = tRange[i].min(BigDecimal.valueOf(Double.MAX_VALUE)).max(BigDecimal.valueOf(-Double.MAX_VALUE)).doubleValue();
            rr[i] = rRange[i].min(BigDecimal.valueOf(Double.MAX_VALUE)).max(BigDecimal.valueOf(-Double.MAX_VALUE)).doubleValue();
        }

        Object[] plots = ImageDrawing.plotRThetaGraph(formula, xRange, yRange, rr, tr, lang);

        if(plots == null) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("plot.failed.noImage", lang));
        } else {
            List<ContainerChildComponent> components = new ArrayList<>();

            components.add(TextDisplay.of("## " + LangID.getStringByID("rPlot.result", lang)));
            components.add(Separator.create(true, Separator.Spacing.LARGE));
            components.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData((File) plots[0], "plot.png"))));
            components.add(Separator.create(true, Separator.Spacing.LARGE));
            components.add(TextDisplay.of(plots[1] + "\n" + plots[2] + "\n\n" + plots[3] + "\n" + plots[4]));

            replyToMessageSafely(ch, loader.getMessage(), msg -> StaticStore.deleteFile((File) plots[0], true), e -> {
                StaticStore.logger.uploadErrorLog(e, "E/RThetaPlot::doSomething - Failed to send plot image file");

                StaticStore.deleteFile((File) plots[0], true);
            }, Container.of(components));
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

    private BigDecimal[] getRRange(String command) {
        Pattern pattern = Pattern.compile("-rr(\\s+)?\\[.+?,.+?]");
        Matcher matcher = pattern.matcher(command);

        if(matcher.find()) {
            String filtered = matcher.group().replaceAll("-rr(\\s+)?", "").replaceAll("\\s", "");

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

    private String filterFormula(String command) {
        String removePrefix = command.split(" ", 2)[1];

        return removePrefix.replaceAll("-(r|ratio)\\s", "").replaceAll("-[xytr]r(\\s+)?\\[.+?,.+?]", "");
    }
}
