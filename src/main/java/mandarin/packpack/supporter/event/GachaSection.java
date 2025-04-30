package mandarin.packpack.supporter.event;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class GachaSection {
    public enum ADDITIONAL {
        SHARD,
        GRANDON,
        NENEKO,
        LUCKY,
        REINFORCE,
        STEP,
        CAPSULE_5
    }

    public GachaSchedule.TYPE gachaType;
    public int gachaID;
    public final double[] rarityChances = new double[5];
    public final int[] rarityGuarantees = new int[5];
    public int addition;
    public int additionalMask;
    public int requiredCatFruit;
    public int index;

    public final List<ADDITIONAL> additional = new ArrayList<>();

    public String message;
}
