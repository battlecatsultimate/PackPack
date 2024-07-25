package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import common.pack.UserProfile;
import common.util.Data;
import common.util.unit.Trait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TreasureHolder {
    public static TreasureHolder toData(JsonObject obj) {
        TreasureHolder holder = new TreasureHolder();

        if(obj.has("basic")) {
            holder.injectValuesIntoArray(holder.basic, obj.getAsJsonArray("basic"));
        }

        if(obj.has("eoc")) {
            holder.injectValuesIntoArray(holder.eoc, obj.getAsJsonArray("eoc"));
        }

        if(obj.has("itf")) {
            holder.injectValuesIntoArray(holder.itf, obj.getAsJsonArray("itf"));
        }

        if(obj.has("cotc")) {
            holder.injectValuesIntoArray(holder.cotc, obj.getAsJsonArray("cotc"));
        }

        return holder;
    }

    public static void initialize() {
        fullTraits.addAll(UserProfile.getBCData().traits.getList());

        for(int i = 0; i < basicMax.length; i++) {
            basicMax[i] = Data.MLV[basicIndex[i]];
        }

        for(int i = 0; i < eocMax.length; i++) {
            eocMax[i] = Data.MT[eocIndex[i]];
        }

        for(int i = 0; i < itfMax.length; i++) {
            if(i == 0) {
                itfMax[i] = 600;
            } else {
                itfMax[i] = 300;
            }
        }

        for(int i = 0; i < cotcMax.length; i++) {
            if(i == 0) {
                cotcMax[i] = 1500;
            } else if(i == cotcMax.length - 1) {
                cotcMax[i] = Data.MT[Data.T_XP2];
            } else {
                cotcMax[i] = 300;
            }
        }

        global.basic = basicMax.clone();
        global.eoc = eocMax.clone();
        global.itf = itfMax.clone();
        global.cotc = cotcMax.clone();
    }

    public static final List<Trait> fullTraits = new ArrayList<>();

    public static final int L_RESEARCH = 0;
    public static final int L_ACCOUNTANT = 1;
    public static final int L_STUDY = 2;
    public static final int[] basicIndex = {Data.LV_RES, Data.LV_ACC, Data.LV_XP};
    public static final int[] basicMax = new int[basicIndex.length];
    public static final String[] basicText = {"data.treasure.upgrades.research", "data.treasure.upgrades.accountant", "data.treasure.upgrades.study"};

    public static final int T_RESEARCH = 0;
    public static final int T_STUDY = 1;
    public static final int T_ACCOUNTANT = 2;
    public static final int T_HEALTH = 3;
    public static final int T_ATTACK = 4;
    public static final int[] eocIndex = {Data.T_RES, Data.T_XP1, Data.T_ACC, Data.T_DEF, Data.T_ATK};
    public static final int[] eocMax = new int[eocIndex.length];
    public static final String[] eocText = {"data.treasure.eoc.research.text", "data.treasure.eoc.study.text", "data.treasure.eoc.accountant.text", "data.treasure.eoc.health.text", "data.treasure.eoc.damage.text"};

    public static final int T_ITF_CRYSTAL = 0;
    public static final int T_BLACK = 1;
    public static final int T_RED = 2;
    public static final int T_FLOAT = 3;
    public static final int T_ANGEL = 4;
    public static final int[] itfTraitIndex = {-1, Data.TRAIT_BLACK, Data.TRAIT_RED, Data.TRAIT_FLOAT, Data.TRAIT_ANGEL};
    public static final int[] itfMax = new int[5];
    public static final String[] itfText = {"data.treasure.itf.crystal", "data.treasure.itf.black.text", "data.treasure.itf.red.text", "data.treasure.itf.floating.text", "data.treasure.itf.angel.text"};

    public static final int T_COTC_CRYSTAL = 0;
    public static final int T_METAL = 1;
    public static final int T_ZOMBIE = 2;
    public static final int T_ALIEN = 3;
    public static final int T_STUDY2 = 4;
    public static final int[] cotcMax = new int[5];
    public static final int[] cotcTraitIndex = {-1, Data.TRAIT_METAL, Data.TRAIT_ZOMBIE, Data.TRAIT_ALIEN};
    public static final String[] cotcText = {"data.treasure.cotc.crystal", "data.treasure.cotc.metal.text", "data.treasure.cotc.zombie.text", "data.treasure.cotc.alien.text", "data.treasure.cotc.study.text"};

    public int[] basic = new int[3];
    public int[] eoc = new int[5];
    public int[] itf = new int[5];
    public int[] cotc = new int[5];

    public static final TreasureHolder global = new TreasureHolder();

    public TreasureHolder() {
        System.arraycopy(basicMax, 0, basic, 0, basicIndex.length);

        System.arraycopy(eocMax, 0, eoc, 0, eocMax.length);

        System.arraycopy(itfMax, 0, itf, 0, itfMax.length);

        System.arraycopy(cotcMax, 0, cotc, 0, cotcMax.length);
    }

    public double getAtkMultiplier() {
        return 1 + eoc[T_ATTACK] * 0.005;
    }

    public double getHealthMultiplier() {
        return 1 + eoc[T_HEALTH] * 0.005;
    }

    public double getDropMultiplier() {
        return 0.95 + 0.05 * basic[L_ACCOUNTANT] + 0.005 * eoc[T_ACCOUNTANT];
    }

    public int getCooldown(int value) {
        return (int) Math.max(60, value - ((basic[L_RESEARCH] - 1) * 6 + eoc[T_RESEARCH] * 0.3));
    }

    public double getFruitMultiplier(List<Trait> traits) {
        double ans = 0;

        for(int i = 1; i < itfTraitIndex.length; i++) {
            if(traits.contains(fullTraits.get(itfTraitIndex[i])))
                ans = Math.max(ans, itf[i]);
        }

        for(int i = 1; i < cotcTraitIndex.length; i++) {
            if(traits.contains(fullTraits.get(cotcTraitIndex[i])))
                ans = Math.max(ans, cotc[i]);
        }

        return ans * 0.01;
    }

    public double getAbilityMultiplier(List<Trait> traits) {
        return 1.0 + 0.2 / 3 * getFruitMultiplier(traits);
    }

    public double getStrongHealthMultiplier(List<Trait> traits) {
        return 0.5 - 0.1 / 3 * getFruitMultiplier(traits);
    }

    public double getStrongAttackMultiplier(List<Trait> traits) {
        return 1.5 + 0.3 / 3 * getFruitMultiplier(traits);
    }

    public double getMassiveAttackMultiplier(List<Trait> traits) {
        return 3 + 1.0 / 3 * getFruitMultiplier(traits);
    }

    public double getResistHealthMultiplier(List<Trait> traits) {
        return 0.25 - 0.05 / 3 * getFruitMultiplier(traits);
    }

    public double getInsaneMassiveAttackMultiplier(List<Trait> traits) {
        return 5 + 1.0 / 3 * getFruitMultiplier(traits);
    }

    public double getInsaneResistHealthMultiplier(List<Trait> traits) {
        return 1.0 / 6 - 1.0 / 126 * getFruitMultiplier(traits);
    }

    public double getAlienMultiplier() {
        return 7 - itf[T_ITF_CRYSTAL] * 0.01;
    }

    public double getStarredAlienMultiplier() {
        return 16 - cotc[T_COTC_CRYSTAL] * 0.01;
    }

    public double getStudyMultiplier() {
        return 0.95 + basic[L_STUDY] * 0.05 + eoc[T_STUDY] * 0.005 + cotc[T_STUDY2] * 0.005;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        obj.add("basic", intArrayToJsonArray(basic));
        obj.add("eoc", intArrayToJsonArray(eoc));
        obj.add("itf", intArrayToJsonArray(itf));
        obj.add("cotc", intArrayToJsonArray(cotc));

        return obj;
    }

    public boolean differentFromGlobal() {
        return !equals(global);
    }

    public TreasureHolder copy() {
        TreasureHolder clone = new TreasureHolder();

        clone.basic = basic.clone();
        clone.eoc = eoc.clone();
        clone.itf = itf.clone();
        clone.cotc = cotc.clone();

        return clone;
    }

    private JsonArray intArrayToJsonArray(int[] array) {
        JsonArray arr = new JsonArray();

        for(int i = 0; i < array.length; i++) {
            arr.add(array[i]);
        }

        return arr;
    }

    private void injectValuesIntoArray(int[] target, JsonArray arr) {
        for(int i = 0; i < Math.min(target.length, arr.size()); i++) {
            target[i] = arr.get(i).getAsInt();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        TreasureHolder that = (TreasureHolder) o;

        return Arrays.equals(basic, that.basic) && Arrays.equals(eoc, that.eoc) && Arrays.equals(itf, that.itf) && Arrays.equals(cotc, that.cotc);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fullTraits);
        result = 31 * result + Arrays.hashCode(basic);
        result = 31 * result + Arrays.hashCode(eoc);
        result = 31 * result + Arrays.hashCode(itf);
        result = 31 * result + Arrays.hashCode(cotc);
        return result;
    }

    @Override
    public String toString() {
        return "TreasureData {\n" +
                "basic : " + Arrays.toString(basic) + ",\n" +
                "eoc : " + Arrays.toString(eoc) + ",\n" +
                "itf : " + Arrays.toString(itf) + ",\n" +
                "cotc : " + Arrays.toString(cotc) + "\n" +
                "}";
    }
}
