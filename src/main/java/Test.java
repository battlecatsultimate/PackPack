import org.apache.commons.lang3.ArrayUtils;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    public static void main(String[] args) throws Exception {
        int[] a = {2, 2, 3, 4, 5, 6, 7, 7, 8, 9, 9, 9, 10, 1};

        System.out.println(containAll(a, 1,2,3));
    }

    private static boolean containAll(int[] data, int... ids) {
        for(int i = 0; i < ids.length; i++) {
            System.out.println(ids[i]);
            if(ArrayUtils.contains(data, ids[i]))
                return false;
        }

        return true;
    }
}
