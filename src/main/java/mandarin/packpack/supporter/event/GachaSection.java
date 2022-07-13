package mandarin.packpack.supporter.event;

import java.util.ArrayList;
import java.util.List;

public class GachaSection {
    public enum ADDITIONAL {
        SHARD,
        GRANDON,
        NENEKO,
        LUCKY,
        REINFORCE,
        STEP
    }

    public GachaSchedule.TYPE gachaType;
    public int gachaID;
    public double[] rarityChances = new double[5];
    public int[] rarityGuarantees = new int[5];
    public int addition;
    public int additionalMask;
    public int requiredCatFruit;
    public int index;

    public List<ADDITIONAL> additional = new ArrayList<>();

    public String message;
}
