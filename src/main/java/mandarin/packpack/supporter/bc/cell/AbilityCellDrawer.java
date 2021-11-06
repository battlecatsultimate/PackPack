package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.awt.FG2D;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class AbilityCellDrawer implements CellDrawer {
    private static final int abilityMargin = 54;

    private final String name;
    private final String[] contents;

    public int h = -1;
    public int w = -1;
    public int offset = -1;
    public int uh = -1;

    public int xNameOffset = -1;
    public int yNameOffset = -1;

    public final int[] xTextOffset;
    public final int[] yTextOffset;

    public AbilityCellDrawer(@NotNull String name, @NotNull String[] contents) {
        this.name = name;
        this.contents = contents;

        this.yTextOffset = new int[contents.length];
        this.xTextOffset = new int[contents.length];
    }

    @Override
    public void initialize(Font nameFont, Font contentFont, FontMetrics nfm, FontMetrics cfm) {
        getWidth(cfm);
        getHeightAndOffset(nameFont, contentFont, nfm.getFontRenderContext(), cfm.getFontRenderContext());
    }

    private void getWidth(FontMetrics contentFont) {
        for(int i = 0; i < contents.length; i++) {
            w = Math.max(w, contentFont.stringWidth(contents[i]));
        }
    }

    private void getHeightAndOffset(Font nameFont, Font contentFont, FontRenderContext nfrc, FontRenderContext cfrc) {
        int rh = 0;

        GlyphVector ngv = nameFont.createGlyphVector(nfrc, name);

        Rectangle2D nRect = ngv.getPixelBounds(null, 0, 0);

        rh += (int) Math.round(nRect.getHeight()+textMargin);

        offset = rh;

        for(int i = 0; i < contents.length; i++) {
            GlyphVector cgv = contentFont.createGlyphVector(cfrc, contents[i]);

            Rectangle2D cRect = cgv.getPixelBounds(null, 0, 0);

            yTextOffset[i] = (int) Math.round(cRect.getY());
            xTextOffset[i] = (int) Math.round(cRect.getX());

            uh = (int) Math.max(uh, cRect.getHeight());
        }

        for(int i = 0; i < contents.length; i++) {
            rh += uh;

            if(i < contents.length - 1)
                rh += abilityMargin;
        }

        h = rh;
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
            g.drawText(contents[i], x - xTextOffset[i], ry + offset - yTextOffset[i]);
            ry += abilityMargin + uh;
        }
    }
}
