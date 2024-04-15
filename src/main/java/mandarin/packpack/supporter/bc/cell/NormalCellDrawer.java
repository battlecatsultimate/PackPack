package mandarin.packpack.supporter.bc.cell;

import common.system.fake.FakeImage;
import mandarin.packpack.supporter.lwjgl.GLGraphics;
import mandarin.packpack.supporter.lwjgl.opengl.model.FontModel;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class NormalCellDrawer implements CellDrawer {
    private final String[] names;
    private final String[] contents;
    private final FakeImage[] icons;
    private final boolean[] fitToText;

    public float h = -1;
    public float ch = -1;
    public float ih = -1;
    public float w = -1;
    public float uw = -1;
    public float offset = -1;

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
    }

    public NormalCellDrawer(@Nonnull String[] names, @Nonnull String[] contents, @Nonnull FakeImage[] icons, @Nonnull boolean[] fitToText) {
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
    }

    @Override
    public void initialize(FontModel nameFont, FontModel contentFont, int targetWidth) {
        getHeightAndOffset(nameFont, contentFont);
        getWidth(nameFont, contentFont);
    }

    private void getWidth(FontModel nameFont, FontModel contentFont) {
        float uw = 0;

        for(int i = 0; i < names.length; i++) {
            float cw = contentFont.textWidth(contents[i]);

            if(icons != null && icons[i] != null) {
                if(fitToText[i]) {
                    double ratio = 1.0 * ch / icons[i].getHeight();

                    cw += (int) (icons[i].getWidth() * ratio);
                } else {
                    cw *= icons[i].getWidth();
                }
            }

            uw = Math.max(uw, Math.max(nameFont.textWidth(names[i]), cw));
        }

        this.uw = uw;

        int w = 0;

        for(int i = 0; i < names.length; i++) {
            w = (int) (w + uw);

            if(i < names.length - 1) {
                w += lineOffset * 2;
            }
        }

        this.w = w;
    }

    private void getHeightAndOffset(FontModel nameFont, FontModel contentFont) {
        float rh = 0;
        float off = 0;
        float rih = 0;
        float rch = 0;

        for(int i = 0; i < names.length; i++) {
            float[] nRect = nameFont.measureDimension(names[i]);
            float[] cRect = contentFont.measureDimension(contents[i]);

            rh = Math.max(Math.round(nRect[3] + textMargin + cRect[3]), rh);
            rch = Math.max(Math.round(cRect[3]), rch);

            if(icons != null && icons[i] != null) {
                rih = Math.max(fitToText[i] ? rch : icons[i].getHeight(), ih);
            } else {
                rih = Math.max(rch, ih);
            }

            off = Math.max(Math.round(nRect[3] + textMargin), off);
        }

        h = rh;
        ch = rch;
        ih = rih;
        offset = off;
    }

    @Override
    public void draw(GLGraphics g, int x, int y, int uw, int offset, int h , FontModel nameFont, FontModel contentFont) {
        g.setColor(191, 191, 191, 255);
        g.setFontModel(nameFont);

        float rx = x;

        for(int i = 0; i < names.length; i++) {
            g.drawText(names[i], rx, y, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

            rx += uw + lineOffset * 2;
        }

        rx = x + uw;

        g.setStroke(lineStroke, GLGraphics.LineEndMode.ROUND);

        for(int i = 0; i < contents.length - 1; i++) {
            g.drawLine(rx + lineOffset, y + lineMargin, rx + lineOffset, y + Math.max(h, ih) - lineMargin);

            rx += uw + lineOffset * 2;
        }

        rx = x;

        g.setColor(238, 238, 238, 255);
        g.setFontModel(contentFont);

        for(int i = 0; i < contents.length; i++) {
            if(icons != null && icons[i] != null) {
                float icw;
                float ich;

                if(fitToText[i]) {
                    ich = ch;

                    double ratio = 1.0 * ich / icons[i].getHeight();

                    icw = (int) (icons[i].getWidth() * ratio);
                } else {
                    icw = icons[i].getWidth();
                    ich = icons[i].getHeight();
                }

                g.drawImage(icons[i], rx, y + offset, icw, ich);

                g.drawText(contents[i], rx + icw + iconMargin, y + offset + (ih - ch) / 2, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
            } else {
                g.drawText(contents[i], rx, y + offset, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
            }

            rx += uw + lineOffset * 2;
        }
    }

    public boolean isSingleData() {
        return names.length == 1;
    }
}
