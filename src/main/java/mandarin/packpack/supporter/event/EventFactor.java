package mandarin.packpack.supporter.event;

import common.util.stage.MapColc;
import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventFactor {
    public static EventDate END = new EventDate(20300101);
    public static String NOMAX = "99.99.99";
    public static String currentGlobalVersion = "100201";
    public static String currentJapaneseVersion = "100300";

    public static final int EN = 0;
    public static final int JP = 3;
    public static final int KR = 2;
    public static final int TW = 1;

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 4;
    public static final int WEDNESDAY = 8;
    public static final int THURSDAY = 16;
    public static final int FRIDAY = 32;
    public static final int SATURDAY = 64;
    public static final int WEEKEND = 65;

    public static final int WEEKLY = 0;
    public static final int MONTHLY = 1;

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

    public static final List<Integer> CYCLONE = Arrays.asList(1015, 1039, 1066, 1122, 1172, 118, 1189, 1193, 1198);
    public static final List<Integer> CYCLONE2 = Arrays.asList(1014, 1016, 1043, 1096, 1157, 1169, 1176, 1187, 1195, 1203);
    public static final List<Integer> BUILDERBLITZ = Arrays.asList(1150, 1151, 1152);
    public static final List<Integer> XPBLITZ = Arrays.asList(1028, 1059, 1600, 1155);

    public static final String GACHAURL = "https://ponos.s3.dualstack.ap-northeast-1.amazonaws.com/information/appli/battlecats/gacha/rare/en/R___.html";
    public static final String ANNOUNCEURL = "https://nyanko-announcement.ponosgames.com/v1/notices?platform=google&clientVersion=VVVVVV&countryCode=LL&clearedStageJapan=100&clearedStageFuture=100&clearedUniverse=100&clientTime=DDDDDDDDD&timeDifference=1";

    public static boolean isXPBlitz(StageSchedule schedule) {
        if(schedule.sections.isEmpty())
            return false;

        if(schedule.stages.size() != 1)
            return false;

        int id = stageToInteger(schedule.stages.get(0));

        if(id == -1 || !XPBLITZ.contains(id))
            return false;

        EventSection section = schedule.sections.get(0);

        return !section.days.isEmpty() && !section.times.isEmpty();
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
}
