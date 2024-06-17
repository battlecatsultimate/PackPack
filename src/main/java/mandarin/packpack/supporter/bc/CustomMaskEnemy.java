package mandarin.packpack.supporter.bc;

import common.battle.data.DataEnemy;
import common.util.anim.MaAnim;

public class CustomMaskEnemy extends DataEnemy {
    public final String[] data;
    public final MaAnim anim;

    public CustomMaskEnemy(String[] data, MaAnim anim) {
        super(null);

        this.data = data;
        this.anim = anim;

        fillData(data);
    }

    @Override
    public int getAnimLen() {
        return anim.len + 1;
    }
}
