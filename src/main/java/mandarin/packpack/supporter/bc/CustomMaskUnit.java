package mandarin.packpack.supporter.bc;

import common.battle.data.DataUnit;
import common.util.anim.MaAnim;
import common.util.unit.UnitLevel;
import mandarin.packpack.supporter.StaticStore;

public class CustomMaskUnit extends DataUnit {
    public final String[] data;
    public final UnitLevel curve;
    public final MaAnim anim;
    public final int rarity, max, maxp;
    public final int[][] evo;

    public CustomMaskUnit(String[] data, String[] curve, MaAnim anim, String[] rare) {
        super(null, null, data);

        this.data = data;
        this.curve = new UnitLevel(toCurve(curve));
        this.anim = anim;
        this.rarity = Integer.parseInt(rare[13]);
        this.max = Integer.parseInt(rare[50]);
        this.maxp = Integer.parseInt(rare[51]);
        int et = Integer.parseInt(rare[23]);
        if (et >= 15000 && et < 17000) {
            evo = new int[6][2];

            evo[0][0] = Integer.parseInt(rare[27]);

            for (int i = 0; i < 5; i++) {
                evo[i + 1][0] = Integer.parseInt(rare[28 + i * 2]);
                evo[i + 1][1] = Integer.parseInt(rare[29 + i * 2]);
            }
        } else {
            evo = null;
        }
    }

    private int[] toCurve(String[] curve) {
        int[] result = new int[curve.length];

        for(int i = 0; i < curve.length; i++) {
            if(StaticStore.isNumeric(curve[i])) {
                result[i] = StaticStore.safeParseInt(curve[i]);
            }
        }

        return result;
    }

    @Override
    public int getAnimLen() {
        return anim.len;
    }
}
