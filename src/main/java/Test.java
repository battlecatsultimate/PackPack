import mandarin.packpack.supporter.calculation.Formula;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {

    public static void main(String[] args) throws Exception {
        String f = "2*x";

        Formula formula = new Formula(f, 1, 0);

        Formula.error.clear();

        System.out.println("Formula : " + f);
        System.out.println(formula.element.printTree(""));
        System.out.println("f(2) = " + formula.substitute(new BigDecimal("2")));

        System.out.printf("%2$d, %1$s will be offline%n", "Due to bug fix", 123123);

        Map<String, String> map = new HashMap<>();

        for(int i = 0; i < 50; i++) {
            map.put(String.valueOf(i), String.valueOf(i));
        }

        System.out.println(map);

        while(map.size() > SelectMenu.OPTIONS_MAX_AMOUNT) {
            String[] keys = map.keySet().toArray(new String[0]);

            map.remove(keys[keys.length - 1]);
        }

        System.out.println(map);
    }
}
