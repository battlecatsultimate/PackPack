package mandarin.packpack.supporter;

import discord4j.common.util.Snowflake;

import java.util.Set;

public class StaticStore {
    public static int ratingChannel = 0;

    public static boolean checkingBCU = false;

    public static final String ERROR_MSG = "`INTERNAL_ERROR`";

    public static final String MEMBER_ID = "632835571655507968";
    public static final String PRE_MEMBER_ID = "490940081738350592";
    public static final String MUTED = "791305076819230732";
    public static final String BCU_PC_USER_ID = "490940151501946880";
    public static final String BCU_ANDROId_USER_ID = "787391428916543488";

    public static final String BOT_COMMANDS = "508042127352266755";
    public static final String GET_ACCESS = "632836623931015185";

    public static String rolesToString(Set<Snowflake> roles) {
        StringBuilder builder = new StringBuilder();

        for(Snowflake role : roles) {
            builder.append(role.asString()).append(", ");
        }

        return builder.toString();
    }
}
