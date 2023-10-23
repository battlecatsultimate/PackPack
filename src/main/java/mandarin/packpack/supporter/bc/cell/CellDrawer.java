package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.lwjgl.GLGraphics;
import mandarin.packpack.supporter.lwjgl.opengl.model.FontModel;

public interface CellDrawer {
    int lineOffset = 72;
    float lineStroke = 3f;
    int lineMargin = 8;
    int textMargin = 24;
    int iconMargin = 24;

    void initialize(FontModel nameFont, FontModel contentFont, int targetWidth);

    void draw(GLGraphics g, int x, int y, int uw, int offset, int h, FontModel nameFont, FontModel contentFont);
}
