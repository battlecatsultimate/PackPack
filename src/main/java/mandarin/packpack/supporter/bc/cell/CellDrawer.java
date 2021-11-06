package mandarin.packpack.supporter.bc.cell;

import mandarin.packpack.supporter.awt.FG2D;

import java.awt.*;

public interface CellDrawer {
    int lineOffset = 144;
    float lineStroke = 6f;
    int lineMargin = 16;
    int textMargin = 48;

    void initialize(Font nameFont, Font contentFont, FontMetrics nfm, FontMetrics cfm);

    void draw(FG2D g, int x, int y, int uw, int offset, int h, Font nameFont, Font contentFont);
}
