import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.GachaSchedule;
import mandarin.packpack.supporter.server.AnimHolder;

import java.io.File;

public class Test {
    public static void main(String[] args) throws Exception {

    }

    private static String quad(int n) {
        if(n < 10) {
            return "000"+n;
        } else if(n < 100) {
            return "00"+n;
        } else if(n < 1000) {
            return "0"+n;
        } else {
            return ""+n;
        }
    }
}
