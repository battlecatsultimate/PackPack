import java.util.Collections;
import java.util.LinkedList;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {

    public static void main(String[] args) throws Exception {
        LinkedList<String> list = new LinkedList<>();

        list.add("ABC");
        list.addLast("DEF");
        list.addLast("GHI");
        list.add(1, "XYZ");

        list.addLast(null);

        System.out.println(list);

        list.removeAll(Collections.singleton(null));

        System.out.println(list);
    }
}
