package mandarin.packpack.supporter.bc;

import common.util.stage.Stage;
import common.util.stage.StageMap;

public class CustomStage extends Stage {
    public final CustomStageMap parent;

    public CustomStage(CustomStageMap parent) {
        this.parent = parent;
    }

    @Override
    public StageMap getCont() {
        return parent;
    }
}
