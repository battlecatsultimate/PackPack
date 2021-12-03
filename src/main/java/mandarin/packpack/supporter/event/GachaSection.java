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

    public int gachaID;
    public int[] rarityChances = new int[5];
    public int[] rarityGuarantees = new int[5];
    public int addition;
    public int additionalMask;
    public int requiredCatFruit;
    public int index;

    public List<ADDITIONAL> additional = new ArrayList<>();

    public String message;
}
