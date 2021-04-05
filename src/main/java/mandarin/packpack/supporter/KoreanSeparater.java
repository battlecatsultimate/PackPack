package mandarin.packpack.supporter;

public class KoreanSeparater {
    private static final String[] INIT = {"ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ", "ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"};

    private static final String[] MIDDLE = {"ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅘ", "ㅙ","ㅚ","ㅛ","ㅜ","ㅝ","ㅞ","ㅟ","ㅠ","ㅡ","ㅢ","ㅣ"};

    private static final String[] LAST = {"","ㄱ","ㄲ","ㄳ","ㄴ","ㄵ","ㄶ","ㄷ","ㄹ","ㄺ","ㄻ","ㄼ", "ㄽ","ㄾ","ㄿ","ㅀ","ㅁ","ㅂ","ㅄ","ㅅ","ㅆ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"};

    private static final int BEGIN = 44032, END = 55203;

    public static String separate(String src) {
        StringBuilder result = new StringBuilder();

        for(int i = 0; i < src.length(); i++) {
            char ch = src.charAt(i);

            if(isKorean(ch)) {
                ch = (char) (ch - 0xAC00);

                char init = (char) (ch / 28 / 21);
                char middle = (char) ((ch) / 28 % 21);
                char last = (char) (ch % 28);

                result.append(INIT[init]).append(MIDDLE[middle]).append(LAST[last]);
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }

    private static boolean isKorean(char ch) {
        return ch >= BEGIN && ch <= END;
    }
}
