import mandarin.packpack.supporter.KoreanSeparater;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.GachaSchedule;
import mandarin.packpack.supporter.server.AnimHolder;

import java.io.File;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) throws Exception {
        System.out.println(KoreanSeparater.separate("안녕 abc 123 하세요"));
    }

    private static String[] getWords(String[] src, int numberOfWords) {
        int length;

        if(src.length % numberOfWords == 0)
            length = src.length / numberOfWords;
        else
            length = src.length / numberOfWords + 1;

        String[] result = new String[length];

        for(int i = 0; i < src.length; i += numberOfWords) {
            StringBuilder builder = new StringBuilder();

            for(int j = 0; j < numberOfWords; j++) {
                if(i + j < src.length) {
                    builder.append(src[i+j]);

                    if(j < numberOfWords - 1 && i+j < src.length - 1)
                        builder.append(" ");
                }
            }

            result[i/numberOfWords] = builder.toString();
        }

        return result;
    }
}
