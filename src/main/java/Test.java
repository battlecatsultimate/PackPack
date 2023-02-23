import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.Formula;
import mandarin.packpack.supporter.calculation.Matrix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    private static final double indicatorLengthRatio = 0.025;

    public static void main(String[] args) throws Exception {
        String formula = "x^3 - 3x^2 - 2x + 4";

        Formula f = new Formula(formula, 0);

        int len = 2000;

        BigDecimal[][] coordinates = new BigDecimal[len + 1][];

        BigDecimal[] xRange = new BigDecimal[2];
        BigDecimal[] yRange = new BigDecimal[2];

        xRange[0] = BigDecimal.valueOf(-4);
        xRange[1] = BigDecimal.valueOf(4);

        yRange[0] = BigDecimal.ZERO;
        yRange[1] = BigDecimal.ZERO;

        for(int i = 0; i < len + 1; i++) {
            BigDecimal[] c = new BigDecimal[2];

            c[0] = xRange[0].add(xRange[1].subtract(xRange[0]).divide(BigDecimal.valueOf(len), Equation.context).multiply(BigDecimal.valueOf(i)));
            c[1] = Equation.calculate(f.substitute(c[0].toString(), 0), null, false, 0);

            if(!Equation.error.isEmpty()) {
                c[1] = null;

                Equation.error.clear();
            } else {
                yRange[0] = yRange[0].min(c[1]);
                yRange[1] = yRange[1].max(c[1]);
            }

            coordinates[i] = c;
        }

        BigDecimal xWidth = xRange[1].subtract(xRange[0]);
        BigDecimal yWidth = yRange[1].subtract(yRange[0]);

        BigDecimal width;

        if(yWidth.divide(xWidth, Equation.context).compareTo(BigDecimal.valueOf(1.5)) > 0) {
            width = xWidth;
        } else {
            width = yWidth.multiply(new BigDecimal("1.2"));
        }

        int dimension = 1024;

        BufferedImage result = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        g.setColor(255, 255, 255, 255);
        g.fillRect(0, 0, dimension, dimension);

        BigDecimal centerX = xRange[0].add(width.divide(BigDecimal.valueOf(2), Equation.context));
        BigDecimal centerY = yRange[0].add(yWidth.divide(BigDecimal.valueOf(2), Equation.context))
                .subtract(width.divide(BigDecimal.valueOf(2), Equation.context));

        drawGuideline(g, dimension, xRange, centerX, centerY);

        drawGraph(g, coordinates, width, dimension, centerX, centerY);

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs())
            return;

        File image = StaticStore.generateTempFile(temp, "image", ".png", false);

        if(image == null)
            return;

        ImageIO.write(result, "PNG", image);
    }

    private static Matrix perform2ndSpline(BigDecimal[][] coordinates) {
        int n = coordinates.length;

        Matrix a = new Matrix(2 * (n - 1), 2 * (n - 1));
        Matrix b = new Matrix(2 * (n - 1), 1);

        for(int i = 1; i < n; i++) {
            a.setValue(i - 1, (i - 1) * 2, coordinates[i][0].subtract(coordinates[i - 1][0]));
            a.setValue(i - 1, (i - 1) * 2 + 1, coordinates[i][0].subtract(coordinates[i - 1][0]).pow(2));

            b.setValue(i - 1, 0, coordinates[i][1].subtract(coordinates[i - 1][1]));
        }

        for(int i = 1; i < n - 1; i++) {
            a.setValue(i + n - 2, (i - 1) * 2, BigDecimal.ONE);
            a.setValue(i + n - 2, (i - 1) * 2 + 1, coordinates[i][0].subtract(coordinates[i - 1][0]).multiply(BigDecimal.valueOf(2)));
            a.setValue(i + n - 2, i * 2, BigDecimal.ONE.negate());
        }

        a.setValue(2 * (n - 1) - 1, 1, BigDecimal.ONE);

        return Matrix.solvePolynomial(a, b, Matrix.ALGORITHM.GAUSSIAN);
    }

    private static void drawGuideline(FG2D g, int w, BigDecimal[] xRange, BigDecimal centerX, BigDecimal centerY) {
        BigDecimal width = xRange[1].subtract(xRange[0]);

        int xLine = convertCoordinateToPixel(BigDecimal.ZERO, width, w, centerX, true);
        int yLine = convertCoordinateToPixel(BigDecimal.ZERO, width, w, centerY, false);

        g.setColor(0, 0, 0, 255);
        g.setStroke(1.5f);

        g.drawLine(xLine, 0, xLine, w);
        g.drawLine(0, yLine, w, yLine);

        BigDecimal segment = width.divide(BigDecimal.TEN, Equation.context);

        int scale = (int) - (Math.round(Math.log10(segment.doubleValue())) + 0.5 - 0.5 * Math.signum(segment.doubleValue()));

        if (scale >= 0) {
            segment = segment.round(new MathContext(1, RoundingMode.HALF_EVEN));
        } else {
            segment = segment.divide(BigDecimal.TEN.pow(-scale), Equation.context).round(new MathContext(1, RoundingMode.HALF_EVEN)).multiply(BigDecimal.TEN.pow(-scale));
        }

        int i = 0;

        while(true) {
            BigDecimal position = segment.multiply(BigDecimal.valueOf(i + 1)).subtract(width.divide(BigDecimal.valueOf(2), Equation.context).round(new MathContext(0, RoundingMode.HALF_EVEN)));

            if(position.compareTo(xRange[1]) > 0)
                break;

            int xPosition = convertCoordinateToPixel(position.add(centerX), width, w, centerX, true);
            int yPosition = convertCoordinateToPixel(position.add(centerY), width, w, centerY, false);

            if(xPosition != 0) {
                g.setColor(0, 0, 0, 255);
                g.setStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPosition, (int) Math.round(yLine - w * indicatorLengthRatio / 2.0), xPosition, (int) Math.round(yLine + w * indicatorLengthRatio / 2.0));

                g.setColor(0, 0, 0, 64);
                g.setStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(xPosition, 0, xPosition, w);
            }

            if(yPosition != 0) {
                g.setColor(0, 0, 0, 255);
                g.setStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine((int) Math.round(xLine - w * indicatorLengthRatio / 2.0), yPosition, (int) Math.round(xLine + w * indicatorLengthRatio / 2.0), yPosition);

                g.setColor(0, 0, 0, 64);
                g.setStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

                g.drawLine(0, yPosition, w, yPosition);
            }

            i++;
        }
    }

    private static void drawGraph(FG2D g, BigDecimal[][] coordinates, BigDecimal width, int w, BigDecimal centerX, BigDecimal centerY) {
        g.setColor(217, 65, 68, 255);
        g.setStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 0; i < coordinates.length - 1; i++) {
            if(coordinates[i][1] == null || coordinates[i + 1][1] == null)
                continue;

            int x0 = convertCoordinateToPixel(coordinates[i][0], width, w, centerX, true);
            int x1 = convertCoordinateToPixel(coordinates[i + 1][0], width, w, centerX, true);

            int y0 = convertCoordinateToPixel(coordinates[i][1], width, w, centerY, false);
            int y1 = convertCoordinateToPixel(coordinates[i + 1][1], width, w, centerY, false);

            double angle = Math.abs(Math.toDegrees(Math.atan2(coordinates[i + 1][1].subtract(coordinates[i][1]).doubleValue(), coordinates[i + 1][0].subtract(coordinates[i][0]).doubleValue())));

            if(angle > 89.999) {
                continue;
            }

            g.drawLine(x0, y0, x1, y1);
        }
    }

    private static int pickSplineIndex(BigDecimal[][] coordinates, BigDecimal x) {
        for(int i = 0; i < coordinates.length - 1; i++) {
            if(x.compareTo(coordinates[i][0]) >= 0 && x.compareTo(coordinates[i + 1][0]) <= 0)
                return i;
        }

        return -1;
    }

    private static int convertCoordinateToPixel(BigDecimal coordinate, BigDecimal range, int width, BigDecimal center, boolean x) {
        if(x) {
            return coordinate.subtract(center).add(range.divide(BigDecimal.valueOf(2), Equation.context)).divide(range, Equation.context).multiply(BigDecimal.valueOf(width)).round(new MathContext(0, RoundingMode.HALF_EVEN)).intValue();
        } else {
            return range.divide(BigDecimal.valueOf(2), Equation.context).subtract(coordinate.subtract(center)).divide(range, Equation.context).multiply(BigDecimal.valueOf(width)).round(new MathContext(0, RoundingMode.HALF_EVEN)).intValue();
        }
    }
}
