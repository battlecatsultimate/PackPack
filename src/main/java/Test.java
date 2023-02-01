import common.pack.UserProfile;
import common.util.unit.Unit;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    public static void main(String[] args) throws Exception {
        ImageExtracter.initialize();

        for(Unit u : UserProfile.getBCData().units) {
            System.out.println(u.forms.length);
        }
    }
}
