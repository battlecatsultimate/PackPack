package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.awt.FG2D;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

public class AbilityCellDrawer implements CellDrawer {
    private static final int abilityMargin = 54;
    private static final int lineSpace = 12;
    private static final int fixer = 15;

    private final String name;
    private final String[] contents;

    public int h = -1;
    public int w = -1;
    public int offset = -1;
    public int uh = -1;

    public int xNameOffset = -1;
    public int yNameOffset = -1;

    public int spaceDotMargin = 0;

    public final int[] xTextOffset;
    public final int[] yTextOffset;

    public AbilityCellDrawer(@NotNull String name, @NotNull String[] contents) {
        this.name = name;
        this.contents = contents;

        this.yTextOffset = new int[contents.length];
        this.xTextOffset = new int[contents.length];
    }

    @Override
    public void initialize(Font nameFont, Font contentFont, FontMetrics nfm, FontMetrics cfm, int targetWidth) {
        getWidth(cfm, targetWidth);
        getHeightAndOffset(nameFont, contentFont, nfm.getFontRenderContext(), cfm.getFontRenderContext());
    }

    private void getWidth(FontMetrics contentFont, int targetWidth) {
        for(int i = 0; i < contents.length; i++) {
            String[] data = contents[i].split(" ");

            int stackWidth = 0;

            StringBuilder realContent = new StringBuilder();
            boolean reachedBoundary = false;

            for(int j = 0; j < data.length; j++) {
                String realSegment = j < data.length - 1 ? data[j] + " " : data[j];

                int sw = contentFont.stringWidth(realSegment);

                if(stackWidth + sw > targetWidth) {
                    if(spaceDotMargin == 0) {
                        spaceDotMargin = contentFont.stringWidth(" · ");
                    }

                    stackWidth = contentFont.stringWidth( realSegment);

                    realContent.append("\n").append(realSegment);

                    reachedBoundary = true;
                } else {
                    stackWidth += sw;

                    realContent.append(realSegment);
                }
            }

            if(reachedBoundary)
                contents[i] = realContent.toString();

            w = Math.max(w, reachedBoundary ? targetWidth : contentFont.stringWidth(contents[i]));
        }
    }

    private void getHeightAndOffset(Font nameFont, Font contentFont, FontRenderContext nfrc, FontRenderContext cfrc) {
        int rh = 0;

        GlyphVector ngv = nameFont.createGlyphVector(nfrc, name);

        Rectangle2D nRect = ngv.getPixelBounds(null, 0, 0);

        rh += (int) Math.round(nRect.getHeight()+textMargin);

        offset = rh;

        for(int i = 0; i < contents.length; i++) {
            String[] segment = contents[i].split("\n");

            for(int j = 0; j < segment.length; j++) {
                GlyphVector cgv = contentFont.createGlyphVector(cfrc, segment[j]);

                Rectangle2D cRect = cgv.getPixelBounds(null, 0, 0);

                if(j == 0) {
                    yTextOffset[i] = (int) Math.round(cRect.getY());
                    xTextOffset[i] = (int) Math.round(cRect.getX());
                }

                uh = (int) Math.max(uh, cRect.getHeight());
            }
        }

        for(int i = 0; i < contents.length; i++) {
            String[] segment = contents[i].split("\n");

            rh += uh * segment.length + lineSpace * (segment.length - 1);

            if(i < contents.length - 1)
                rh += abilityMargin;
        }

        h = rh + fixer;
    }

    @Override
    public void draw(FG2D g, int x, int y, int uw, int offset, int h, Font nameFont, Font contentFont) {
        g.setColor(191, 191, 191, 255);
        g.setFont(nameFont);

        g.drawText(name, x - xNameOffset, y - yNameOffset);

        int ry = y;

        g.setColor(238, 238, 238, 255);
        g.setFont(contentFont);

        for(int i = 0; i < contents.length; i++) {
            String[] segment = contents[i].split("\n");

            for(int j = 0; j < segment.length; j++) {
                if(j == 0) {
                    g.drawText(segment[j], x - xTextOffset[i], ry + offset - yTextOffset[i]);
                } else {
                    g.drawText(segment[j], x + spaceDotMargin - xTextOffset[i], ry + offset - yTextOffset[i]);
                }

                ry += uh;

                if(j < segment.length - 1)
                    ry += lineSpace;
            }

            ry += abilityMargin;
        }
    }
}
