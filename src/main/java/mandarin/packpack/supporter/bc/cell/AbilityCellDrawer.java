package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.lwjgl.GLGraphics;
import mandarin.packpack.supporter.lwjgl.opengl.model.FontModel;
import org.jetbrains.annotations.NotNull;

public class AbilityCellDrawer implements CellDrawer {
    private static final int abilityMargin = 27;
    private static final int lineSpace = 6;
    private static final float fixer = 7.5f;

    private final String name;
    private final String[] contents;

    public float h = -1;
    public float w = -1;
    public float offset = -1;
    public float uh = -1;

    public float spaceDotMargin = 0;

    public AbilityCellDrawer(@NotNull String name, @NotNull String[] contents) {
        this.name = name;
        this.contents = contents;
    }

    @Override
    public void initialize(FontModel nameFont, FontModel contentFont, int targetWidth) {
        if(spaceDotMargin == 0) {
            spaceDotMargin = contentFont.trueWidth(" · ");
        }

        getWidth(contentFont, targetWidth);
        getHeightAndOffset(nameFont, contentFont);
    }

    private void getWidth(FontModel contentFont, int targetWidth) {
        for(int i = 0; i < contents.length; i++) {
            String[] data = contents[i].split(" ");

            float preWidth = spaceDotMargin;

            StringBuilder realContent = new StringBuilder();
            StringBuilder sentence = new StringBuilder();

            boolean reachedBoundary = false;

            for(int j = 0; j < data.length; j++) {
                String realSegment = j < data.length - 1 ? data[j] + " " : data[j];

                float sw = preWidth + contentFont.textWidth(sentence + realSegment);

                if (sw > targetWidth) {
                    realContent.append(sentence).append("\n");

                    preWidth = 0f;
                    sentence = new StringBuilder();

                    reachedBoundary = true;
                }

                sentence.append(realSegment);
            }

            if (!sentence.isEmpty()) {
                realContent.append(sentence);
            }

            if(reachedBoundary) {
                contents[i] = realContent.toString();
            }

            w = Math.max(w, reachedBoundary ? targetWidth : contentFont.textWidth(contents[i]));
        }
    }

    private void getHeightAndOffset(FontModel nameFont, FontModel contentFont) {
        int rh = 0;

        float[] nRect = nameFont.measureDimension(name);

        rh += Math.round(nRect[3] + textMargin);

        offset = rh;

        for(int i = 0; i < contents.length; i++) {
            String[] segment = contents[i].split("\n");

            for(int j = 0; j < segment.length; j++) {
                float[] cRect = contentFont.measureDimension(segment[j]);

                uh = (int) Math.max(uh, cRect[3]);
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
    public void draw(GLGraphics g, int x, int y, int uw, int offset, int h, FontModel nameFont, FontModel contentFont) {
        g.setColor(191, 191, 191, 255);
        g.setFontModel(nameFont);

        g.drawText(name, x, y, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.MIDDLE);

        int ry = y;

        g.setColor(238, 238, 238, 255);
        g.setFontModel(contentFont);

        for(int i = 0; i < contents.length; i++) {
            String[] segment = contents[i].split("\n");

            for(int j = 0; j < segment.length; j++) {
                if(j == 0) {
                    g.drawText(segment[j], x, ry + offset, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
                } else {
                    g.drawText(segment[j], x + spaceDotMargin, ry + offset, GLGraphics.HorizontalSnap.RIGHT, GLGraphics.VerticalSnap.TOP);
                }

                ry += uh;

                if(j < segment.length - 1)
                    ry += lineSpace;
            }

            ry += abilityMargin;
        }
    }
}
