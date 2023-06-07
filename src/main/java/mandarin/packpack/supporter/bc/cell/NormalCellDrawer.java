package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.awt.FG2D;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class NormalCellDrawer implements CellDrawer {
    private final String[] names;
    private final String[] contents;
    private final BufferedImage[] icons;
    private final boolean[] fitToText;

    public int h = -1;
    public int ch = -1;
    public int ih = -1;
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
        this.icons = null;
        this.fitToText = null;
        this.yTextOffset = new int[names.length][2];
        this.xTextOffset = new int[names.length][2];
    }

    public NormalCellDrawer(@Nonnull String[] names, @Nonnull String[] contents, @Nonnull BufferedImage[] icons, @Nonnull boolean[] fitToText) {
        if(names.length != contents.length || names.length != icons.length || names.length != fitToText.length) {
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
        this.icons = icons;
        this.fitToText = fitToText;
        this.yTextOffset = new int[names.length][2];
        this.xTextOffset = new int[names.length][2];
    }

    @Override
    public void initialize(Font nameFont, Font contentFont, FontMetrics nfm, FontMetrics cfm, int targetWidth) {
        getHeightAndOffset(nameFont, contentFont, nfm.getFontRenderContext(), cfm.getFontRenderContext());
        getWidth(nfm, cfm);
    }

    private void getWidth(FontMetrics bigFont, FontMetrics smallFont) {
        int uw = 0;

        for(int i = 0; i < names.length; i++) {
            int cw = smallFont.stringWidth(contents[i]);

            if(icons != null && icons[i] != null) {
                if(fitToText[i]) {
                    double ratio = 1.0 * ch / icons[i].getHeight();

                    cw += (int) (icons[i].getWidth() * ratio);
                } else {
                    cw *= icons[i].getWidth();
                }
            }

            uw = Math.max(uw, Math.max(bigFont.stringWidth(names[i]), cw));
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
        int rih = 0;
        int rch = 0;

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
            rch = (int) Math.max(Math.round(cRect.getHeight()), rch);

            if(icons != null && icons[i] != null) {
                rih = Math.max(fitToText[i] ? rch : icons[i].getHeight(), ih);
            } else {
                rih = Math.max(rch, ih);
            }

            off = (int) Math.max(Math.round(nRect.getHeight()+textMargin), off);
        }

        h = rh;
        ch = rch;
        ih = rih;
        offset = off;
    }

    @Override
    public void draw(FG2D g, int x, int y, int uw, int offset, int h , Font nameFont, Font contentFont) {
        g.setColor(191, 191, 191, 255);
        g.setFont(nameFont);

        int rx = x;

        for(int i = 0; i < names.length; i++) {
            g.drawText(names[i], rx - xTextOffset[i][0], y -yTextOffset[i][0]);
            rx += uw + lineOffset * 2;
        }

        rx = x + uw;

        g.setStroke(lineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for(int i = 0; i < contents.length - 1; i++) {
            g.drawLine(rx + lineOffset, y + lineMargin, rx + lineOffset, y + Math.max(h, ih) - lineMargin);

            rx += uw + lineOffset * 2;
        }

        rx = x;

        g.setColor(238, 238, 238, 255);
        g.setFont(contentFont);

        for(int i = 0; i < contents.length; i++) {
            if(icons != null && icons[i] != null) {
                int icw;
                int ich;

                if(fitToText[i]) {
                    ich = ch;

                    double ratio = 1.0 * ich / icons[i].getHeight();

                    icw = (int) (icons[i].getWidth() * ratio);
                } else {
                    icw = icons[i].getWidth();
                    ich = icons[i].getHeight();
                }

                g.drawImage(icons[i], rx, y + offset, icw, ich);

                g.drawText(contents[i], rx + icw + iconMargin - xTextOffset[i][1], y + offset + (ih - ch) / 2 - yTextOffset[i][1]);
            } else {
                g.drawText(contents[i], rx - xTextOffset[i][1], y + offset - yTextOffset[i][1]);
            }

            rx += uw + lineOffset * 2;
        }
    }

    public boolean isSingleData() {
        return names.length == 1;
    }
}
