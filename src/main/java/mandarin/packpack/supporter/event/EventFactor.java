package mandarin.packpack.supporter.event;

import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.group.ContainedGroupHandler;
import mandarin.packpack.supporter.event.group.GroupHandler;
import mandarin.packpack.supporter.event.group.NormalGroupHandler;
import mandarin.packpack.supporter.event.group.SequenceGroupHandler;

import java.util.*;

public class EventFactor {
    static {
        Calendar c = Calendar.getInstance();

        currentYear = c.get(Calendar.YEAR);
    }

    public static int currentYear;

    public static EventDate END = new EventDate(20300101);
    public static String NOMAX = "99.99.99";
    public static String currentGlobalVersion = "100300";
    public static String currentJapaneseVersion = "100300";

    public static final int GLOBAL = 0;
    public static final int JP = 1;

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 4;
    public static final int WEDNESDAY = 8;
    public static final int THURSDAY = 16;
    public static final int FRIDAY = 32;
    public static final int SATURDAY = 64;
    public static final int WEEKEND = 65;

    public static final int SPEEDUP = 0;
    public static final int TREASURE = 1;
    public static final int RICHCAT = 2;
    public static final int CATCPU = 3;
    public static final int CATJOB = 4;
    public static final int CATSNIPER = 5;

    public static final int XPAPACK = 6;
    public static final int XPBPACK = 7;
    public static final int XPCPACK = 8;
    public static final int XPDPACK = 9;
    public static final int XPEPACK = 10;

    public static final int ITEM = 50;

    public static final int EOC = 100;
    public static final int ITF = 101;
    public static final int HALF = 102;
    public static final int RESET = 103;
    public static final int COTC = 104;

    public static final int DRINKA = 700;
    public static final int DRINKB = 701;
    public static final int DRINKC = 702;

    public static final GroupHandler CYCLONE = new NormalGroupHandler(
            Arrays.asList(
                    Arrays.asList(1015, 1039, 1066, 1122, 1172, 118, 1189, 1193, 1198, 1247),
                    Arrays.asList(1014, 1016, 1043, 1096, 1157, 1169, 1176, 1187, 1195, 1203)
            ),
            "Cyclone Festival!"
    );

    public static final GroupHandler BUILDERBLITZ = new NormalGroupHandler(
            Collections.singletonList(
                    Arrays.asList(1150, 1151, 1152)
            ),
            "Builders Blitz!"
    );

    public static final GroupHandler XPBLITZ = new NormalGroupHandler(
            Arrays.asList(
                    List.of(1028),
                    List.of(1059),
                    List.of(1124),
                    List.of(1155)
            ),
            "XP Blitz!"
    );

    public static final GroupHandler CATFRUITFESTIVAL = new NormalGroupHandler(
            Collections.singletonList(
                    Arrays.asList(1097, 1098, 1099, 1100, 1101)
            ),
            "Catfruit Festival!"
    );

    public static final GroupHandler CRAZEDFESTIVAL = new SequenceGroupHandler(
            Arrays.asList(
                    Arrays.asList(1017, 1020, 1023, 1102, 1105, 1108),
                    Arrays.asList(1018, 1021, 1024, 1103, 1106, 1109),
                    Arrays.asList(1019, 1022, 1025, 1104, 1107, 1110)
            ),
            "Crazed/Manic Festival!"
    );

    public static final GroupHandler LILFESTIVAL = new SequenceGroupHandler(
            Arrays.asList(
                    Arrays.asList(1130, 1133, 1136),
                    Arrays.asList(1131, 1134, 1137),
                    Arrays.asList(1132, 1135, 1138)
            ),
            "Li'l Festival!"
    );

    public static final GroupHandler METALFESTIVAL = new ContainedGroupHandler(
            Arrays.asList(
                    Arrays.asList(1006, 1007),
                    List.of(1078)
            ),
            "Metal Festival!"
    );

    public static final List<GroupHandler> handlers = Arrays.asList(
            CYCLONE, BUILDERBLITZ, XPBLITZ, CATFRUITFESTIVAL, CRAZEDFESTIVAL, LILFESTIVAL, METALFESTIVAL
    );

    public static final String GACHAURL = "https://ponos.s3.dualstack.ap-northeast-1.amazonaws.com/information/appli/battlecats/gacha/rare/en/R___.html";
    public static final String ANNOUNCEURL = "https://nyanko-announcement.ponosgames.com/v1/notices?platform=google&clientVersion=VVVVVV&countryCode=LL&clearedStageJapan=100&clearedStageFuture=100&clearedUniverse=100&clientTime=DDDDDDDDD&timeDifference=1";
    public static final String EVENTURL = "https://bc-seek.godfat.org/seek/LL/FFFF";

    public static boolean onlyHoldMissions(StageSchedule schedule) {
        if(!schedule.stages.isEmpty())
            return false;

        for(String id : schedule.unknownStages) {
            int realID = StaticStore.safeParseInt(id);

            if(realID > 9999 || realID < 8000)
                return false;
        }

        return true;
    }

    public static int stageToInteger(StageMap map) {
        if(map == null || map.id == null)
            return -1;

        MapColc mc = map.getCont();

        if(mc == null)
            return -1;

        if(!StaticStore.isNumeric(mc.getSID()))
            return -1;

        int mcid = StaticStore.safeParseInt(mc.getSID()) * 1000;

        return mcid + map.id.id;
    }

    public static String getMonth(int mon) {
        switch (mon) {
            case 1:
                return "January";
            case 2:
                return "February";
            case 3:
                return "March";
            case 4:
                return "April";
            case 5:
                return "May";
            case 6:
                return "June";
            case 7:
                return "July";
            case 8:
                return "August";
            case 9:
                return "September";
            case 10:
                return "October";
            case 11:
                return "November";
            case 12:
                return "December";
            default:
                return "Unknown Month "+mon;
        }
    }

    public static String getNumberExtension(int num) {
        if(num != 11 && num % 10 == 1)
            return "st";
        else if(num != 12 && num % 10 == 2)
            return "nd";
        else if(num != 13 && num % 10 == 3)
            return "rd";
        else
            return "th";
    }
}
