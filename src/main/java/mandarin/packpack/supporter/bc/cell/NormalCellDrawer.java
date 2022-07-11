package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.bc.cell.CellDrawer;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class NormalCellDrawer implements CellDrawer {
    private final String[] names;
    private final String[] contents;

    public int h = -1;
    public int w = -1;
    public int uw = -1;
    public int offset = -1;
    public final int[][] xTextOffset;
    public final int[][] yTextOffset;

    public NormalCellDrawer(@Nonnull String[] names, @Nonnull String[] contents) {
        if(names.length != contents.length) {
            throw new IllegalStateException("Name and content must be synchronized!\n\nName : "+ Arrays.toString(names)+"\nContents : "+Arrays.toString(contents));
        } else if(names.length == 0) {
            throw new IllegalStateException("Name and content must not be empty!");
        } else {
            for(int i = 0; i < names.length; i++) {
                if(names[i] == null || contents[i] == null) {
                    throw new IllegalStateException("Name and content array must not contain null!\n\nName : "+Arrays.toString(names)+"\nContents : "+Arrays.toString(contents));
                }
            }
        }

        this.names = names;
        this.contents = contents;
        this.yTextOffset = new int[names.length][2];
        this.xTextOffset = new int[names.length][2];
    }

    @Override
    public void initialize(Font nameFont, Font contentFont, FontMetrics nfm, FontMetrics cfm, int targetWidth) {
        getWidth(nfm, cfm);
        getHeightAndOffset(nameFont, contentFont, nfm.getFontRenderContext(), cfm.getFontRenderContext());
    }

    private void getWidth(FontMetrics bigFont, FontMetrics smallFont) {
        int uw = 0;

        for(int i = 0; i < names.length; i++) {
            uw = Math.max(uw, Math.max(bigFont.stringWidth(names[i]), smallFont.stringWidth(contents[i])));
        }

        this.uw = uw;

        int w = 0;

        for(int i = 0; i < names.length; i++) {
            w += uw;

            if(i < names.length - 1) {
                w += lineOffset * 2;
            }
        }

        this.w = w;
    }

    private void getHeightAndOffset(Font nameFont, Font contentFont, FontRenderContext nfrc, FontRenderContext cfrc) {
        int rh = 0;
        int off = 0;

        for(int i = 0; i < names.length; i++) {
            GlyphVector ngv = nameFont.createGlyphVector(nfrc, names[i]);
            GlyphVector cgv = contentFont.createGlyphVector(cfrc, contents[i]);

            Rectangle2D nRect = ngv.getPixelBounds(null, 0, 0);
            Rectangle2D cRect = cgv.getPixelBounds(null, 0, 0);

            yTextOffset[i][0] = (int) Math.round(nRect.getY());
            yTextOffset[i][1] = (int) Math.round(cRect.getY());

            xTextOffset[i][0] = (int) Math.round(nRect.getX());
            xTextOffset[i][1] = (int) Math.round(cRect.getX());

            rh = (int) Math.max(Math.round(nRect.getHeight()+textMargin+cRect.getHeight()), rh);
            off = (int) Math.max(Math.round(nRect.getHeight()+textMargin), off);
        }

        h = rh;
        offset = off;
    }

    @Override
    public void draw(FG2D g, int x, int y, int uw, int offset, int h , Font nameFont, Font contentFont) {
        g.setColor(191, 191, 191, 255);
        g.setFont(nameFont);

        int rx = x;

        for(int i = 0; i < names.length; i++) {
            g.drawText(names[i], rx - xTextOffset[i][0], Math.round(y -yTextOffset[i][0]));
            rx += uw + lineOffset * 2;
        }

        rx = uw;

        g.setStroke(lineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 0; i < contents.length - 1; i++) {
            g.drawLine(rx + lineOffset, y + lineMargin, rx + lineOffset, y + h - lineMargin);

            rx += uw + lineOffset * 2;
        }

        rx = x;

        g.setColor(238, 238, 238, 255);
        g.setFont(contentFont);

        for(int i = 0; i < contents.length; i++) {
            g.drawText(contents[i], rx - xTextOffset[i][1], Math.round(y + offset - yTextOffset[i][1]));
            rx += uw + lineOffset * 2;
        }
    }

    public boolean isSingleData() {
        return names.length == 1;
    }
}
