package mandarin.packpack.supporter.bc;

import common.battle.data.MaskUnit;
import common.battle.data.PCoin;
import common.system.fake.FakeImage;

import java.util.ArrayList;
import java.util.List;

public class CustomTalent extends PCoin {
    public final int unitID;
    public final List<TalentData> talents = new ArrayList<>();
    public final FakeImage icon;
    public FakeImage traitIcon = null;

    public CustomTalent(String[] data, MaskUnit du, int unitID, FakeImage icon) {
        super(data, du);

        this.unitID = unitID;
        this.icon = icon;
    }
}
