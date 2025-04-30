package mandarin.packpack.supporter.event;

import common.CommonStatic;
import common.system.files.VFile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.group.ContainedGroupHandler;
import mandarin.packpack.supporter.event.group.GroupHandler;
import mandarin.packpack.supporter.event.group.NormalGroupHandler;
import mandarin.packpack.supporter.event.group.SequenceGroupHandler;
import mandarin.packpack.supporter.lang.LangID;

import java.util.*;

public class EventFactor {
    public enum SCHEDULE {
        NORMAL,
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
        MISSION
    }

    static {
        Calendar c = Calendar.getInstance();

        currentYear = c.get(Calendar.YEAR);
    }

    public static int currentYear;

    public static final EventDate END = new EventDate(20300101, false, null, false);
    public static final String NOMAX = "99.99.99";

    public static final CommonStatic.Lang.Locale[] supportedVersions = { CommonStatic.Lang.Locale.EN, CommonStatic.Lang.Locale.ZH, CommonStatic.Lang.Locale.KR, CommonStatic.Lang.Locale.JP };

    public static final int GATYA = 0;
    public static final int ITEM = 1;
    public static final int SALE = 2;

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 4;
    public static final int WEDNESDAY = 8;
    public static final int THURSDAY = 16;
    public static final int FRIDAY = 32;
    public static final int SATURDAY = 64;
    public static final int WEEKEND = 65;

    public static final int REWARDNORMAL = 0;
    public static final int REWARDUNIT = 1;
    public static final int REWARDTRUE = 2;

    public static final Map<Integer, int[]> missionReward = new HashMap<>();
    public static final Map<Integer, int[]> newUnits = new HashMap<>();

    public static final GroupHandler CYCLONE = new ContainedGroupHandler(
            Arrays.asList(
                    Arrays.asList(1014, 1016, 1043, 1096, 1157, 1169, 1176, 1187, 1195, 1203),
                    Arrays.asList(1015, 1039, 1066, 1122, 1172, 1185, 1189, 1193, 1198, 1247)
            ),
            "event.group.cycloneFestival", false
    );

    public static final GroupHandler BUILDERBLITZ = new NormalGroupHandler(
            Collections.singletonList(
                    Arrays.asList(1150, 1151, 1152)
            ),
            "event.group.buildersFestival", false
    );

    public static final GroupHandler XPBLITZ = new NormalGroupHandler(
            Arrays.asList(
                    List.of(1028),
                    List.of(1059),
                    List.of(1124),
                    List.of(1155)
            ),
            "event.group.xpFestival", false
    );

    public static final GroupHandler CATFRUITFESTIVAL = new NormalGroupHandler(
            Collections.singletonList(
                    Arrays.asList(1097, 1098, 1099, 1100, 1101)
            ),
            "event.group.catFruitFestival", false
    );

    public static final GroupHandler CRAZEDFESTIVAL = new SequenceGroupHandler(
            Arrays.asList(
                    Arrays.asList(1017, 1020, 1023, 1102, 1105, 1108),
                    Arrays.asList(1018, 1021, 1024, 1103, 1106, 1109),
                    Arrays.asList(1019, 1022, 1025, 1104, 1107, 1110)
            ),
            "event.group.crazedFestival", false
    );

    public static final GroupHandler LILFESTIVAL = new SequenceGroupHandler(
            Arrays.asList(
                    Arrays.asList(1130, 1133, 1136),
                    Arrays.asList(1131, 1134, 1137),
                    Arrays.asList(1132, 1135, 1138)
            ),
            "event.group.lilFestival", false
    );

    public static final GroupHandler METALFESTIVAL = new ContainedGroupHandler(
            Arrays.asList(
                    Arrays.asList(1006, 1007),
                    List.of(1078)
            ),
            "event.group.metalFestival", false
    );

    public static final GroupHandler GAMATOTOXP = new NormalGroupHandler(
            List.of(
                    Arrays.asList(5000, 5001, 5002)
            ),
            "event.group.xpHarvest", true
    );

    public static final GroupHandler ITEMEXDISCOUNT = new NormalGroupHandler(
            List.of(
                    Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 51, 700, 701, 702)
            ),
            "event.group.halfDiscount", true
    );

    public static final GroupHandler EOCHALFENERGY = new NormalGroupHandler(
            List.of(
                    Arrays.asList(3000, 3001, 3002)
            ),
            "event.group.eocEnergy", true
    );

    public static final GroupHandler ITFHALFENERGY = new NormalGroupHandler(
            List.of(
                    Arrays.asList(3003, 3004, 3005)
            ),
            "event.group.itfEnergy", true
    );

    public static final GroupHandler COTCHALFENERGY = new NormalGroupHandler(
            List.of(
                    Arrays.asList(3006, 3007, 3008)
            ),
            "event.group.cotcEnergy", true
    );

    public static final GroupHandler HALFENERGY = new NormalGroupHandler(
            List.of(
                    Arrays.asList(102, 3000, 3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008)
            ),
            "event.group.halfEnergy", true
    );

    public static final GroupHandler GAMATOTOGRANDXP = new NormalGroupHandler(
            List.of(
                    Arrays.asList(5008, 5009, 5010)
            ),
            "event.group.grandXpHarvest" , true
    );

    public static final GroupHandler ITEMDISCOUNT = new NormalGroupHandler(
            List.of(
                    Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 700, 701, 702)
            ),
            "event.group.itemDiscount", true
    );

    public static final GroupHandler CATFRUITFESTIVALJP = new SequenceGroupHandler(
            Arrays.asList(
                    Arrays.asList(1097, 1098, 1099),
                    Arrays.asList(1100, 1101)
            ),
            "event.group.catFruitFestival", false
    );

    public static final GroupHandler CRAZEDFESTIVALJP = new SequenceGroupHandler(
            Arrays.asList(
                    Arrays.asList(1017, 1020, 1023, 1103, 1106, 1109),
                    Arrays.asList(1018, 1021, 1024, 1104, 1107, 1110),
                    Arrays.asList(1019, 1022, 1025, 1102, 1105, 1108)
            ),
            "event.group.crazedFestival", false
    );

    public static final GroupHandler ALLSTARSTAGES = new SequenceGroupHandler(
            Arrays.asList(
                    List.of(1033),
                    List.of(1034),
                    List.of(1035),
                    List.of(1070),
                    List.of(1079),
                    List.of(1146)
            ),
            "event.group.allStars", false
    );

    public static final List<GroupHandler> handlers = Arrays.asList(
            CYCLONE, BUILDERBLITZ, XPBLITZ, CATFRUITFESTIVAL, CRAZEDFESTIVAL, LILFESTIVAL, METALFESTIVAL, GAMATOTOXP,
            ITEMEXDISCOUNT, EOCHALFENERGY, ITFHALFENERGY, COTCHALFENERGY, HALFENERGY, GAMATOTOGRANDXP, ITEMDISCOUNT,
            CATFRUITFESTIVALJP, CRAZEDFESTIVALJP, ALLSTARSTAGES
    );

    public static final String GACHAURL = "https://ponos.s3.dualstack.ap-northeast-1.amazonaws.com/information/appli/battlecats/gacha/rare_LLL_R_ID_.html";
    public static final String ANNOUNCEURL = "https://nyanko-announcement.ponosgames.com/v1/notices?platform=google&clientVersion=VVVVVV&countryCode=LL&clearedStageJapan=300&clearedStageFuture=300&clearedUniverse=300&clientTime=DDDDDDDDD&timeDifference=1";

    public static void readMissionReward() {
        VFile vf = VFile.get("./org/data/Mission_Data.csv");

        if(vf == null)
            return;

        Queue<String> q = vf.getData().readLine();

        q.poll();

        String line;

        while((line = q.poll()) != null) {
            String[] data = line.split(",");

            if(data.length < 7)
                continue;

            int missionID = StaticStore.safeParseInt(data[0]);

            int[] missionData = new int[] {
                    StaticStore.safeParseInt(data[4]),
                    StaticStore.safeParseInt(data[5]),
                    StaticStore.safeParseInt(data[6])
            };

            missionReward.put(missionID, missionData);
        }
    }

    public static boolean onlyHoldMissions(StageSchedule schedule) {
        if(!schedule.stages.isEmpty())
            return false;

        for(String id : schedule.unknownStages) {
            int realID = StaticStore.safeParseInt(id);

            if((realID < 8000 || realID >= 10000) && (realID < 15000 || realID >= 16000) && (realID < 17000 || realID >= 18000))
                return false;
        }

        return true;
    }

    public static boolean isWeeklyAndMonthlyMission(StageSchedule schedule) {
        if(!schedule.isMission)
            return false;

        for(String id : schedule.unknownStages) {
            int realID = StaticStore.safeParseInt(id);

            if((realID >= 8000 && realID < 9000) || (realID >= 17000 && realID < 18000))
                return true;
        }

        return false;
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

    public static String getMonth(int mon, CommonStatic.Lang.Locale lang) {
        return switch (mon) {
            case 1 -> LangID.getStringByID("date.monthOfYear.january", lang);
            case 2 -> LangID.getStringByID("date.monthOfYear.february", lang);
            case 3 -> LangID.getStringByID("date.monthOfYear.march", lang);
            case 4 -> LangID.getStringByID("date.monthOfYear.april", lang);
            case 5 -> LangID.getStringByID("date.monthOfYear.may", lang);
            case 6 -> LangID.getStringByID("date.monthOfYear.june", lang);
            case 7 -> LangID.getStringByID("date.monthOfYear.july", lang);
            case 8 -> LangID.getStringByID("date.monthOfYear.august", lang);
            case 9 -> LangID.getStringByID("date.monthOfYear.september", lang);
            case 10 -> LangID.getStringByID("date.monthOfYear.october", lang);
            case 11 -> LangID.getStringByID("date.monthOfYear.november", lang);
            case 12 -> LangID.getStringByID("date.monthOfYear.december", lang);
            default -> "Unknown Month " + mon;
        };
    }

    public String getWhichDay(int data, CommonStatic.Lang.Locale lang) {
        return switch (data) {
            case MONDAY -> LangID.getStringByID("date.dayOfWeek.monday", lang);
            case TUESDAY -> LangID.getStringByID("date.dayOfWeek.tuesday", lang);
            case WEDNESDAY -> LangID.getStringByID("date.dayOfWeek.wednesday", lang);
            case THURSDAY -> LangID.getStringByID("date.dayOfWeek.thursday", lang);
            case FRIDAY -> LangID.getStringByID("date.dayOfWeek.friday", lang);
            case SATURDAY -> LangID.getStringByID("date.dayOfWeek.saturday", lang);
            case SUNDAY -> LangID.getStringByID("date.dayOfWeek.sunday", lang);
            case WEEKEND -> LangID.getStringByID("date.dayOfWeek.weekend", lang);
            default -> "Unknown day " + data;
        };
    }

    public static String getNumberWithDayFormat(int num, CommonStatic.Lang.Locale lang) {
        switch (lang) {
            case EN -> {
                if (num != 11 && num % 10 == 1)
                    return "st";
                else if (num != 12 && num % 10 == 2)
                    return "nd";
                else if (num != 13 && num % 10 == 3)
                    return "rd";
                else
                    return "th";
            }
            case KR -> {
                return "일";
            }
            case ZH, JP -> {
                return "日";
            }
            default -> {
                return "";
            }
        }
    }

    @SuppressWarnings("unused")
    public static String getNumberExtension(int num, CommonStatic.Lang.Locale lang) {
        if(lang == CommonStatic.Lang.Locale.EN) {
            if(num != 11 && num % 10 == 1)
                return "st";
            else if(num != 12 && num % 10 == 2)
                return "nd";
            else if(num != 13 && num % 10 == 3)
                return "rd";
            else
                return "th";
        } else {
            return "";
        }
    }

    public static String beautifyVersion(String ver) {
        String[] numbers = ver.split("\\.");

        StringBuilder result = new StringBuilder();

        for(int i = 0; i < numbers.length; i++) {
            if(i == 0){
                result.append(numbers[i]).append(".");
            } else if(StaticStore.safeParseInt(numbers[i]) != 0){
                result.append(numbers[i]).append(".");
            } else {
                break;
            }
        }

        return result.substring(0, result.length() - 1);
    }

    public static int getVersionNumber(String ver) {
        String[] numbers = ver.split("\\.");

        int v = 0;

        for(int i = numbers.length - 1; i >= 0; i--) {
            v = (int) (v + StaticStore.safeParseInt(numbers[i]) * Math.pow(10, (numbers.length - 1 - i) * 2));
        }

        return v;
    }

    public static String beautifyItem(CommonStatic.Lang.Locale lang, int itemID, int itemAmount) {
        if (itemID >= 11000 && itemID < 12000) {
            MapColc mc = MapColc.get(Data.hex(11));

            if(mc != null) {
                StageMap map = mc.maps.get(itemID % 11000);

                if(map != null) {
                    Stage st = map.list.get(0);

                    if(st != null) {
                        String stageName = StaticStore.safeMultiLangGet(st, lang);

                        if(stageName != null && !stageName.isBlank()) {
                            return stageName;
                        } else {
                            return LangID.getStringByID("event.item.rankingDojo", lang).replace("_", Data.trio(itemID - 11000));
                        }
                    } else {
                        return LangID.getStringByID("event.item.rankingDojo", lang).replace("_", Data.trio(itemID - 11000));
                    }
                } else {
                    return LangID.getStringByID("event.item.rankingDojo", lang).replace("_", Data.trio(itemID - 11000));
                }
            } else {
                return LangID.getStringByID("event.item.rankingDojo", lang).replace("_", Data.trio(itemID - 11000));
            }
        }

        String item = MultiLangCont.getServerDrop(itemID, lang);

        if(item == null || item.isBlank()) {
            String id = "event.item."+itemID;

            item = LangID.getStringByIDSuppressed(id, lang);

            if (item.equals(id)) {
                if(itemID >= 800 && itemID < 900) {
                    item = LangID.getStringByID("event.dummy.sale", lang).replace("_", String.valueOf(itemID));
                } else if((itemID >= 900 && itemID < 1000) || (itemID >= 35000 && itemID < 36000)) {
                    item = LangID.getStringByID("event.dummy.stamp", lang).replace("_", String.valueOf(itemID));
                } else {
                    item = LangID.getStringByID("event.dummy.item", lang).replace("_", String.valueOf(itemID));
                }
            }
        }

        if(itemID >= 300) {
            return item;
        }

        char c = item.charAt(item.length() - 1);

        if(itemAmount > 1 && lang == CommonStatic.Lang.Locale.EN && ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || Character.isDigit(c)) && !item.endsWith("XP") && !item.endsWith("s") && !item.endsWith("Choco"))
            item = getPlural(item);

        if(itemID == 202 || itemID == 203) {
            return LangID.getStringByID("event.format.ticket", lang).replace("_NNN_", String.valueOf(itemAmount)).replace("_III_", item);
        } else if(itemID == 201) {
            return LangID.getStringByID("event.format.xp", lang).replace("_NNN_", String.valueOf(itemAmount)).replace("_III_", item);
        } else {
            return LangID.getStringByID("event.format.default", lang).replace("_NNN_", String.valueOf(itemAmount)).replace("_III_", item);
        }
    }

    public static String beautifyGameItem(CommonStatic.Lang.Locale lang, int itemID, int itemAmount) {
        String item = MultiLangCont.getStatic().RWNAME.getCont(itemID, lang);

        if (item == null || item.isBlank()) {
            item = LangID.getStringByID("event.dummy.item", lang).replace("_", String.valueOf(itemID));
        }

        char c = item.charAt(item.length() - 1);

        if (itemAmount > 1 && lang == CommonStatic.Lang.Locale.EN && ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || Character.isDigit(c)) && !item.endsWith("XP") && !item.endsWith("s") && !item.endsWith("Choco"))
            item = getPlural(item);

        if (itemID == 11 || itemID == 12 || itemID == 20 || itemID == 21) {
            return LangID.getStringByID("event.format.ticket", lang).replace("_NNN_", String.valueOf(itemAmount)).replace("_III_", item);
        } else if (itemID == 6) {
            return LangID.getStringByID("event.format.xp", lang).replace("_NNN_", String.valueOf(itemAmount)).replace("_III_", item);
        } else {
            return LangID.getStringByID("event.format.default", lang).replace("_NNN_", String.valueOf(itemAmount)).replace("_III_", item);
        }
    }

    public static String getPlural(String item) {
        if (item.endsWith("ch") || item.endsWith("sh") || item.endsWith("s") || item.endsWith("x") || item.endsWith("z")) {
            return item + "es";
        } else {
            return item + "s";
        }
    }

    public static String getGachaCodeExplanation(CommonStatic.Lang.Locale lang) {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("event.gachaCode.guaranteed.code", lang))
                .append(" : ")
                .append(LangID.getStringByID("event.gachaCode.guaranteed.fullName", lang));

        GachaSection.ADDITIONAL[] additionalList = Arrays.stream(GachaSection.ADDITIONAL.values()).sorted((a0, a1) -> {
            if (a0 == null && a1 == null)
                return 0;

            if (a0 == null)
                return -1;

            if (a1 == null)
                return 1;

            return a0.name().compareTo(a1.name());
        }).toArray(GachaSection.ADDITIONAL[]::new);

        for (int i = 0; i < additionalList.length; i++) {
            builder.append(" | ");

            GachaSection.ADDITIONAL additional = additionalList[i];

            switch (additional) {
                case SHARD -> builder.append(LangID.getStringByID("event.gachaCode.platinumShard.code", lang))
                        .append(" : ")
                        .append(LangID.getStringByID("event.gachaCode.platinumShard.fullName", lang));
                case GRANDON -> builder.append(LangID.getStringByID("event.gachaCode.grandon.code", lang))
                        .append(" : ")
                        .append(LangID.getStringByID("event.gachaCode.grandon.fullName", lang));
                case NENEKO -> builder.append(LangID.getStringByID("event.gachaCode.nenekoGang.code", lang))
                        .append(" : ")
                        .append(LangID.getStringByID("event.gachaCode.nenekoGang.fullName", lang));
                case LUCKY -> builder.append(LangID.getStringByID("event.gachaCode.luckyTicket.code", lang))
                        .append(" : ")
                        .append(LangID.getStringByID("event.gachaCode.luckyTicket.fullName", lang));
                case REINFORCE -> builder.append(LangID.getStringByID("event.gachaCode.reinforcement.code", lang))
                        .append(" : ")
                        .append(LangID.getStringByID("event.gachaCode.reinforcement.fullName", lang));
                case STEP -> builder.append(LangID.getStringByID("event.gachaCode.stepUp.code", lang))
                        .append(" : ")
                        .append(LangID.getStringByID("event.gachaCode.stepUp.fullName", lang));
                case CAPSULE_5 -> builder.append(LangID.getStringByID("event.gachaCode.5capsules.code", lang))
                        .append(" : ")
                        .append(LangID.getStringByID("event.gachaCode.5capsules.fullName", lang));
            }
        }

        return builder.toString();
    }
}
