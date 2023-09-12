package mandarin.packpack.supporter;

import common.pack.Context;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.awt.FIBI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FontStageImageGenerator implements ImageGenerator{
    private final Font font;
    private final float strokeWidth;

    public static boolean valid(Font font, String message) {
        for(int i = 0; i < message.length(); i++) {
            String str = Character.toString(message.charAt(i));

            if(str.isBlank())
                continue;

            if(!font.canDisplay(message.charAt(i)))
                return false;
        }

        return true;
    }

    public FontStageImageGenerator(Font font, float strokeWidth) {
        this.font = font;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public File generateImage(String message, boolean isStage) {
        if(valid(message)) {
            AffineTransform affine = new AffineTransform();

            FontRenderContext frc = new FontRenderContext(affine, true, false);

            double w = generateWidth(message, frc);
            double[] h = generateHeight(message, frc, affine);

            BufferedImage img = new BufferedImage((int) (w + strokeWidth * 2 + xGap * 2), (int) (h[0] + h[1] + strokeWidth * 2 + yGap), BufferedImage.TYPE_INT_ARGB);

            FG2D g = new FG2D(img.getGraphics());

            g.setRenderingHint(3, 2);
            g.enableAntialiasing();

            double pad = 0.0;

            for(int i = 0; i < message.length(); i++) {
                String str = Character.toString(message.charAt(i));

                if(str.isBlank()) {
                    pad += space;
                    continue;
                }

                Shape outline = font.createGlyphVector(frc, str).getGlyphOutline(0);

                double[] offset = decideOffset(pad, h[0] + h[1], h[1]);
                double left = getLeftPoint(outline.getPathIterator(affine));

                offset[0] -= left - strokeWidth - xGap;
                offset[1] += strokeWidth;

                Path2D path = generatePath2D(offset, outline.getPathIterator(affine));

                g.drawFontOutline(path, strokeWidth * 2);

                pad += generateLetterWidth(str, frc) + 4;
            }

            pad = 0.0;

            for(int i = 0; i < message.length(); i++) {
                String str = Character.toString(message.charAt(i));

                if(str.isBlank()) {
                    pad += space;
                    continue;
                }

                Shape outline = font.createGlyphVector(frc, str).getGlyphOutline(0);

                double[] offset = decideOffset(pad, h[0] + h[1], h[1]);
                double left = getLeftPoint(outline.getPathIterator(affine));

                offset[0] -= left - strokeWidth - xGap;
                offset[1] += strokeWidth;

                Path2D path = generatePath2D(offset, outline.getPathIterator(affine));

                if(isStage) {
                    g.setGradient(0, 0, 0, (int) offset[1], new Color(255, 245, 0), new Color(236, 156, 0), 1f - 1f/3f);
                }

                g.fillPath2D(path);

                pad += generateLetterWidth(str, frc) + 4;
            }

            File f = StaticStore.generateTempFile(new File("./temp/"), "Result", ".png", false);

            if(f == null)
                return null;

            g.dispose();

            try {
                ImageIO.write(img, "PNG", f);

                return f;
            } catch (IOException e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to write png data : "+f.getAbsolutePath());
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private BufferedImage generateBufferedImage(String message, boolean isStage) {
        if(valid(message)) {
            AffineTransform affine = new AffineTransform();

            FontRenderContext frc = new FontRenderContext(affine, true, false);

            double w = generateWidth(message, frc);
            double[] h = generateHeight(message, frc, affine);

            BufferedImage img = new BufferedImage((int) (w + strokeWidth * 2 + xGap * 2), (int) (h[0] + h[1] + strokeWidth * 2 + yGap * 2), BufferedImage.TYPE_INT_ARGB);

            FG2D g = new FG2D(img.getGraphics());

            g.setRenderingHint(3, 2);
            g.enableAntialiasing();

            double pad = 0.0;

            for(int i = 0; i < message.length(); i++) {
                String str = Character.toString(message.charAt(i));

                if(str.isBlank()) {
                    pad += space;
                    continue;
                }

                Shape outline = font.createGlyphVector(frc, str).getGlyphOutline(0);

                double[] offset = decideOffset(pad, h[0] + h[1], h[1]);
                double left = getLeftPoint(outline.getPathIterator(affine));

                offset[0] -= left - strokeWidth - xGap;
                offset[1] += strokeWidth;

                Path2D path = generatePath2D(offset, outline.getPathIterator(affine));

                g.drawFontOutline(path, strokeWidth * 2);

                pad += generateLetterWidth(str, frc) + 4;
            }

            pad = 0.0;

            for(int i = 0; i < message.length(); i++) {
                String str = Character.toString(message.charAt(i));

                if(str.isBlank()) {
                    pad += space;
                    continue;
                }

                Shape outline = font.createGlyphVector(frc, str).getGlyphOutline(0);

                double[] offset = decideOffset(pad, h[0] + h[1], h[1]);
                double left = getLeftPoint(outline.getPathIterator(affine));

                offset[0] -= left - strokeWidth - xGap;
                offset[1] += strokeWidth + 1;

                Path2D path = generatePath2D(offset, outline.getPathIterator(affine));

                if(isStage) {
                    g.setGradient(0, 0, 0, (int) offset[1], new Color(255, 245, 0), new Color(236, 156, 0), 1f - 1f/3f);
                }

                g.fillPath2D(path);

                pad += generateLetterWidth(str, frc) + 4;
            }

            g.dispose();

            return img;
        } else {
            return null;
        }
    }

    @Override
    public File generateRealImage(String message, boolean isStage) {
        BufferedImage img = generateBufferedImage(message, isStage);

        if(img == null)
            return null;

        BufferedImage real = new BufferedImage(256, 64, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(real.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        if(!isStage) {
            float ratio = 55f / img.getHeight();

            BufferedImage scaled = new BufferedImage((int) (img.getWidth() * ratio), 55, BufferedImage.TYPE_INT_ARGB);
            FG2D sg = new FG2D(scaled.getGraphics());

            sg.setRenderingHint(3, 1);
            sg.enableAntialiasing();

            sg.drawImage(FIBI.build(img), 0, 0, scaled.getWidth(), scaled.getHeight());

            if(scaled.getWidth() > 253)
                ratio = 253f / scaled.getWidth();
            else
                ratio = 1f;

            g.drawImage(FIBI.build(scaled), 128 - (scaled.getWidth() * ratio / 2), 32 - 55f/2, scaled.getWidth() * ratio, scaled.getHeight());

            g.dispose();
            sg.dispose();

            File f = StaticStore.generateTempFile(new File("./temp/"), "Result", ".png", false);

            if(f == null)
                return null;

            try {
                ImageIO.write(real, "PNG", f);
            } catch (IOException e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to write png file : "+f.getAbsolutePath());
                e.printStackTrace();
                return null;
            }

            return f;
        } else {
            float ratio = 42f / img.getHeight();

            BufferedImage scaled = new BufferedImage((int) (img.getWidth() * ratio), 42, BufferedImage.TYPE_INT_ARGB);
            FG2D sg = new FG2D(scaled.getGraphics());

            sg.setRenderingHint(3, 1);
            sg.enableAntialiasing();

            sg.drawImage(FIBI.build(img), 0, 0, scaled.getWidth(), scaled.getHeight());

            if(scaled.getWidth() > 228)
                ratio = 228f / scaled.getWidth();
            else
                ratio = 1f;

            g.drawImage(FIBI.build(scaled), 3, 2, scaled.getWidth() * ratio, scaled.getHeight());

            g.dispose();
            sg.dispose();

            File f = new File("./temp/Result.png");

            try {
                Context.check(f);
            } catch (IOException e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to check file : "+f.getAbsolutePath());
                e.printStackTrace();
                return null;
            }

            try {
                ImageIO.write(real, "PNG", f);
            } catch (IOException e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to write png file : "+f.getAbsolutePath());
                e.printStackTrace();
                return null;
            }

            return f;
        }
    }

    @Override
    public ArrayList<String> getInvalids(String message) {
        ArrayList<String> res = new ArrayList<>();

        for(int i = 0; i < message.length(); i++) {
            String str = Character.toString(message.charAt(i));

            if(str.isBlank())
                continue;

            if(!font.canDisplay(message.charAt(i)))
                res.add(str);
        }

        return res;
    }

    private boolean valid(String message) {
        for(int i = 0; i < message.length(); i++) {
            if(message.charAt(i) == ' ')
                continue;

            if(!font.canDisplay(message.charAt(i)))
                return false;
        }

        return true;
    }

    private double[] getAscendDescend(PathIterator path) {
        double[] d = new double[6];

        double descend = 0;
        double ascend = 0;

        while(!path.isDone()) {
            path.currentSegment(d);

            descend = Math.min(d[1] * -1.0, descend);
            ascend = Math.max(d[1] * -1.0, ascend);

            if(!path.isDone())
                path.next();
        }

        return new double[] {ascend, descend};
    }

    private double generateWidth(String message, FontRenderContext frc) {
        double w = 0.0;

        for(int i = 0; i < message.length(); i++) {
            String str = Character.toString(message.charAt(i));

            if(str.isBlank()) {
                w += space;
                continue;
            }

            GlyphVector glyph = font.createGlyphVector(frc, str);

            w += glyph.getVisualBounds().getWidth() + 4;
        }

        return w - 4;
    }

    private double[] generateHeight(String message, FontRenderContext frc, AffineTransform aff) {
        GlyphVector glyph = font.createGlyphVector(frc, message);

        double[] res = new double[2];

        for(int i = 0; i < message.length(); i++) {
            Shape outline = glyph.getGlyphOutline(i);

            PathIterator path = outline.getPathIterator(aff);

            double[] result = getAscendDescend(path);

            res[0] = Math.max(res[0], result[0]);
            res[1] = Math.min(res[1], result[1]);
        }

        res[1] *= -1.0;

        return res;
    }

    private double[] decideOffset(double padding, double h, double base) {
        return new double[] {padding, h - base};
    }

    private double generateLetterWidth(String str, FontRenderContext frc) {
        GlyphVector glyph = font.createGlyphVector(frc, str);

        return glyph.getVisualBounds().getWidth();
    }

    private double getLeftPoint(PathIterator path) {
        double res = Double.MAX_VALUE;

        double[] d = new double[6];

        while(!path.isDone()) {
            path.currentSegment(d);

            res = Math.min(res, d[0]);

            if(!path.isDone())
                path.next();
        }

        return res;
    }

    private Path2D generatePath2D(double[] offset, PathIterator path) {
        Path2D path2D = new Path2D.Double();

        double[] d = new double[6];

        while(!path.isDone()) {
            switch (path.currentSegment(d)) {
                case PathIterator.SEG_MOVETO -> path2D.moveTo(d[0] + offset[0], d[1] + offset[1]);
                case PathIterator.SEG_LINETO -> path2D.lineTo(d[0] + offset[0], d[1] + offset[1]);
                case PathIterator.SEG_QUADTO ->
                        path2D.quadTo(d[0] + offset[0], d[1] + offset[1], d[2] + offset[0], d[3] + offset[1]);
                case PathIterator.SEG_CUBICTO ->
                        path2D.curveTo(d[0] + offset[0], d[1] + offset[1], d[2] + offset[0], d[3] + offset[1], d[4] + offset[0], d[5] + offset[1]);
                case PathIterator.SEG_CLOSE -> path2D.closePath();
            }

            if(!path.isDone())
                path.next();
        }

        return path2D;
    }
}
