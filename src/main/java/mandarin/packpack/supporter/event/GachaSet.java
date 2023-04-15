package mandarin.packpack.supporter.event;

import common.pack.UserProfile;
import common.system.files.VFile;
import common.util.unit.Unit;
import mandarin.packpack.supporter.StaticStore;

import java.util.*;

public class GachaSet {
    public static Map<Integer, GachaSet> gachaSet = new HashMap<>();

    public static void initialize() {
        VFile vf = VFile.get("./org/data/GatyaDataSetR1.csv");

        if(vf == null)
            return;

        Queue<String> q = vf.getData().readLine();

        String line;

        int i = 0;

        while((line = q.poll()) != null) {
            if(i < 100) {
                i++;

                continue;
            }

            gachaSet.put(i, new GachaSet(line));
            i++;
        }
    }

    public final int[] data;
    public Map<Integer, List<Unit>> buffUnits = new HashMap<>();

    public GachaSet(String line) {
        String[] data = line.split(",");

        int length = 0;

        while(StaticStore.safeParseInt(data[length]) != -1) {
            length++;
        }

        this.data = new int[length];

        for(int i = 0; i < this.data.length; i++) {
            this.data[i] = StaticStore.safeParseInt(data[i]);
        }

        findBuffedUnits();
    }

    private void findBuffedUnits() {
        List<Integer> units = new ArrayList<>();
        Map<Integer, Integer> collection = new HashMap<>();

        for(int i = 0; i < data.length; i++) {
            if(units.contains(data[i])) {
                int buff = collection.getOrDefault(data[i], 1) + 1;

                collection.put(data[i], buff);
            } else {
                units.add(data[i]);
            }
        }

        for(int id : collection.keySet()) {
            Unit u = UserProfile.getBCData().units.get(id);

            if(u == null)
                continue;

            if(buffUnits.containsKey(collection.get(id))) {
                buffUnits.get(collection.get(id)).add(u);

                buffUnits.get(collection.get(id)).sort(Comparator.comparingInt(un -> un.id.id));
            } else {
                List<Unit> unitList = new ArrayList<>();

                unitList.add(u);

                buffUnits.put(collection.get(id), unitList);
            }
        }
    }
}
