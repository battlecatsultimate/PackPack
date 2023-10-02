package mandarin.packpack.supporter.bc;

import common.battle.data.MaskUnit;
import common.battle.data.PCoin;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomTalent extends PCoin {
    public final int unitID;
    public final List<TalentData> talents = new ArrayList<>();
    public final BufferedImage icon;
    public BufferedImage traitIcon = null;

    public CustomTalent(String[] data, MaskUnit du, int unitID, BufferedImage icon) {
        super(data, du);

        this.unitID = unitID;
        this.icon = icon;
    }
}
