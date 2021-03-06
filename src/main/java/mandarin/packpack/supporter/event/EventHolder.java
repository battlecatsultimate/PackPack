package mandarin.packpack.supporter.event;

import common.io.assets.UpdateCheck;
import mandarin.packpack.supporter.StaticStore;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EventHolder extends EventFactor {
    public Map<Integer, ArrayList<Integer>> yearly = new HashMap<>();
    public Map<Integer, ArrayList<Integer>> monthly = new HashMap<>();
    public Map<Integer, ArrayList<Integer>> weekly = new HashMap<>();
    public Map<Integer, ArrayList<Integer>> daily = new HashMap<>();

    public Map<Integer, ArrayList<StageSchedule>> stages = new HashMap<>();
    public Map<Integer, ArrayList<StageSchedule>> gachas = new HashMap<>();
    public Map<Integer, ArrayList<StageSchedule>> items = new HashMap<>();

    public Map<Integer, String> stageMD5 = new HashMap<>();
    public Map<Integer, String> gachaMD5 = new HashMap<>();
    public Map<Integer, String> itemMD5 = new HashMap<>();

    public void updateStage(File f, int locale, boolean init) throws Exception {
        if(!prepareFiles(locale))
            return;

        File file = new File("./data/event/"+getLocaleName(locale), "sale.tsv");

        boolean needToAnalyze = false;

        if(stageMD5.containsKey(locale)) {
            String md5 = StaticStore.fileToMD5(f);

            if(!stageMD5.get(locale).equals(md5)) {
                stageMD5.put(locale, md5);
                needToAnalyze = true;
            }
        } else {
            stageMD5.put(locale, StaticStore.fileToMD5(f));
            needToAnalyze = true;
        }

        if(!needToAnalyze)
            return;

        ArrayList<String> newLines;

        if(file.exists() && !init) {
            newLines = getOnlyNewLine(file, f);
        } else {
            newLines = new ArrayList<>();

            BufferedReader reader = new BufferedReader(new FileReader(f));

            String line = reader.readLine();

            if(line != null && line.contains("[start]")) {
                while((line = reader.readLine()) != null) {
                    if(line.contains("[end]"))
                        break;
                    else {
                        newLines.add(line);
                    }
                }
            }

            reader.close();
        }

        if(file.exists()) {
            boolean res = file.delete();

            if(!res)
                return;
        }

        boolean result = f.renameTo(file);

        if(!result)
            return;

        ArrayList<StageSchedule> stages = this.stages.containsKey(locale) ? this.stages.get(locale) : new ArrayList<>();

        ArrayList<Integer> daily = this.daily.containsKey(locale) ? this.daily.get(locale) : new ArrayList<>();
        ArrayList<Integer> weekly = this.weekly.containsKey(locale) ? this.weekly.get(locale) : new ArrayList<>();
        ArrayList<Integer> monthly = this.monthly.containsKey(locale) ? this.monthly.get(locale) : new ArrayList<>();
        ArrayList<Integer> yearly = this.yearly.containsKey(locale) ? this.yearly.get(locale) : new ArrayList<>();

        if(!stages.isEmpty()) {
            stages.clear();
        }

        for(String line : newLines) {
            stages.add(new StageSchedule(line));
        }

        for(StageSchedule schedule : stages) {
            if(schedule.type == StageSchedule.TYPE.DAILY) {
                schedule.stages.removeIf(sm -> {
                    int id = stageToInteger(sm);

                    boolean res = id != -1 && !daily.contains(id);

                    if(!res)
                        daily.add(id);

                    return !res;
                });
            } else if(schedule.type == StageSchedule.TYPE.WEEKLY) {
                schedule.stages.removeIf(sm -> {
                    int id = stageToInteger(sm);

                    boolean res = id != -1 && !weekly.contains(id);

                    if(!res)
                        weekly.add(id);

                    return !res;
                });
            } else if(schedule.type == StageSchedule.TYPE.MONTHLY) {
                schedule.stages.removeIf(sm -> {
                    int id = stageToInteger(sm);

                    boolean res = id != -1 && !monthly.contains(id);

                    if(!res)
                        monthly.add(id);

                    return !res;
                });
            } else if(schedule.type == StageSchedule.TYPE.YEARLY) {
                schedule.stages.removeIf(sm -> {
                    int id = stageToInteger(sm);

                    boolean res = id != -1 && !yearly.contains(id);

                    if(!res)
                        yearly.add(id);

                    return !res;
                });
            }
        }

        stages.removeIf(schedule -> schedule.unknownStages.isEmpty() && schedule.stages.isEmpty());

        this.stages.put(locale, stages);

        this.daily.put(locale, daily);
        this.weekly.put(locale, weekly);
        this.monthly.put(locale, monthly);
        this.yearly.put(locale, yearly);
    }

    public String printStageEvent(int locale) {
        ArrayList<StageSchedule> stages = this.stages.get(locale);

        if(stages == null || stages.isEmpty())
            return "";

        StringBuilder data = new StringBuilder();

        boolean[] analyzed = new boolean[stages.size()];

        ArrayList<String> normals = new ArrayList<>();
        ArrayList<String> dailys = new ArrayList<>();
        ArrayList<String> weeklys = new ArrayList<>();
        ArrayList<String> monthlys = new ArrayList<>();
        ArrayList<String> yearlys = new ArrayList<>();

        for(int i = 0; i < stages.size(); i++) {
            if(analyzed[i])
                continue;

            StageSchedule schedule = stages.get(i);

            if(onlyHoldMissions(schedule)) {
                analyzed[i] = true;
                continue;
            }

            if(isXPBlitz(schedule)) {
                if(i < stages.size() - 1) {
                    int connections = 0;
                    ArrayList<Integer> js = new ArrayList<>();

                    for(int j = i+1; j < stages.size(); j++) {
                        StageSchedule tempSchedule = stages.get(i);

                        if(isXPBlitzConnection(tempSchedule, schedule.date)) {
                            analyzed[j] = true;
                            js.add(j);
                            connections++;
                        }
                    }

                    if(connections == 3) {

                        appendProperly(schedule, manualSchedulePrint(schedule.date.dateStart, schedule.date.dateEnd, "XP Blitz!"), normals, dailys, weeklys, monthlys, yearlys);
                    } else {
                        if(connections != 0)
                            System.out.println("??? Something is wrong!");

                        for (Integer integer : js) {
                            analyzed[integer] = false;
                        }

                        appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                    }
                } else {
                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else if(isCycloneFestival(schedule)) {
                if(i < stages.size() - 1) {
                    boolean done = false;

                    for(int j = i + 1; j < stages.size(); j++) {
                        StageSchedule temp = stages.get(j);

                        if(isCycloneFestivalConnection(temp, schedule.date)) {
                            analyzed[j] = true;
                            done = true;
                            break;
                        }
                    }

                    if(!done) {
                        appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                    } else {
                        appendProperly(schedule, manualSchedulePrint(schedule.date.dateStart, schedule.date.dateEnd, "Cyclone Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                    }
                } else {
                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else if(isSecondCycloneFestival(schedule)) {
                if(i < stages.size() - 1) {
                    boolean done = false;

                    for(int j = i + 1; j < stages.size(); j++) {
                        StageSchedule temp = stages.get(j);

                        if(isSecondCycloneFestivalConnection(temp, schedule.date)) {
                            analyzed[j] = true;
                            done = true;
                            break;
                        }
                    }

                    if(!done) {
                        appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                    } else {
                        appendProperly(schedule, manualSchedulePrint(schedule.date.dateStart, schedule.date.dateEnd, "Cyclone Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                    }
                } else {
                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else if (isBuilderBlitz(schedule)) {
                appendProperly(schedule, manualSchedulePrint(schedule.date.dateStart, schedule.date.dateEnd, "Builders Blitz!"), normals, dailys, weeklys, monthlys, yearlys);
            } else if(isCatfruitFestival(schedule)) {
                appendProperly(schedule, manualSchedulePrint(schedule.date.dateStart, schedule.date.dateEnd, "Catfruit Festival!"), normals, dailys, weeklys, monthlys, yearlys);
            } else if(isCrazedFestival(schedule)) {
                if(i < stages.size() - 1) {
                    boolean secondCrazed = false;
                    boolean thirdCrazed = false;

                    ArrayList<Integer> id = new ArrayList<>();

                    EventDate start = schedule.date.dateStart;
                    EventDate end = null;

                    for(int j = i + 1; j < stages.size(); j++) {
                        StageSchedule temp = stages.get(j);

                        if(isSecondCrazedFestival(temp) && !secondCrazed) {
                            secondCrazed = true;
                            analyzed[j] = true;
                            id.add(j);
                        } else if(isThirdCrazedFestival(temp) && !thirdCrazed) {
                            thirdCrazed = true;
                            analyzed[j] = true;
                            end = temp.date.dateEnd;
                            id.add(j);
                        }
                    }

                    if(secondCrazed && thirdCrazed) {
                        appendProperly(schedule, manualSchedulePrint(start, end, "Crazed/Manic Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                    } else {
                        for(int j = 0; j < id.size(); j++) {
                            analyzed[j] = false;
                        }

                        appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                    }
                } else {
                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else if (isSecondCrazedFestival(schedule)) {
                if(i < stages.size() - 1) {
                    boolean firstCrazed = false;
                    boolean thirdCrazed = false;

                    ArrayList<Integer> id = new ArrayList<>();

                    EventDate start = null;
                    EventDate end = null;

                    for(int j = i + 1; j < stages.size(); j++) {
                        StageSchedule temp = stages.get(j);

                        if(isCrazedFestival(temp) && !firstCrazed) {
                            firstCrazed = true;
                            analyzed[j] = true;
                            start = temp.date.dateStart;
                            id.add(j);
                        } else if(isThirdCrazedFestival(temp) && !thirdCrazed) {
                            thirdCrazed = true;
                            analyzed[j] = true;
                            end = temp.date.dateEnd;
                            id.add(j);
                        }
                    }

                    if(firstCrazed && thirdCrazed) {
                        appendProperly(schedule, manualSchedulePrint(start, end, "Crazed/Manic Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                    } else {
                        for(int j = 0; j < id.size(); j++) {
                            analyzed[j] = false;
                        }

                        appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                    }
                } else {
                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else if (isThirdCrazedFestival(schedule)) {
                if(i < stages.size() - 1) {
                    boolean firstCrazed = false;
                    boolean secondCrazed = false;

                    ArrayList<Integer> id = new ArrayList<>();

                    EventDate start = null;
                    EventDate end = schedule.date.dateEnd;

                    for(int j = i + 1; j < stages.size(); j++) {
                        StageSchedule temp = stages.get(j);

                        if(isCrazedFestival(temp) && !firstCrazed) {
                            firstCrazed = true;
                            analyzed[j] = true;
                            start = temp.date.dateStart;
                            id.add(j);
                        } else if(isSecondCrazedFestival(temp) && !secondCrazed) {
                            secondCrazed = true;
                            analyzed[j] = true;
                            id.add(j);
                        }
                    }

                    if(firstCrazed && secondCrazed) {
                        appendProperly(schedule, manualSchedulePrint(start, end, "Crazed/Manic Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                    } else {
                        for(int j = 0; j < id.size(); j++) {
                            analyzed[j] = false;
                        }

                        appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                    }
                } else {
                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else if(isLilFestival(schedule)) {
            if(i < stages.size() - 1) {
                boolean secondLil = false;
                boolean thirdLil = false;

                ArrayList<Integer> id = new ArrayList<>();

                EventDate start = schedule.date.dateStart;
                EventDate end = null;

                for(int j = i + 1; j < stages.size(); j++) {
                    StageSchedule temp = stages.get(j);

                    if(isSecondLilFestival(temp) && !secondLil) {
                        secondLil = true;
                        analyzed[j] = true;
                        id.add(j);
                    } else if(isThirdLilFestival(temp) && !thirdLil) {
                        thirdLil = true;
                        analyzed[j] = true;
                        end = temp.date.dateEnd;
                        id.add(j);
                    }
                }

                if(secondLil && thirdLil) {
                    appendProperly(schedule, manualSchedulePrint(start, end, "Li'l Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                } else {
                    for(int j = 0; j < id.size(); j++) {
                        analyzed[j] = false;
                    }

                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else {
                appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
            }
        } else if (isSecondLilFestival(schedule)) {
            if(i < stages.size() - 1) {
                boolean firstLil = false;
                boolean thirdLil = false;

                ArrayList<Integer> id = new ArrayList<>();

                EventDate start = null;
                EventDate end = null;

                for(int j = i + 1; j < stages.size(); j++) {
                    StageSchedule temp = stages.get(j);

                    if(isLilFestival(temp) && !firstLil) {
                        firstLil = true;
                        analyzed[j] = true;
                        start = temp.date.dateStart;
                        id.add(j);
                    } else if(isThirdLilFestival(temp) && !thirdLil) {
                        thirdLil = true;
                        analyzed[j] = true;
                        end = temp.date.dateEnd;
                        id.add(j);
                    }
                }

                if(firstLil && thirdLil) {
                    appendProperly(schedule, manualSchedulePrint(start, end, "Li'l Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                } else {
                    for(int j = 0; j < id.size(); j++) {
                        analyzed[j] = false;
                    }

                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else {
                appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
            }
        } else if (isThirdLilFestival(schedule)) {
            if(i < stages.size() - 1) {
                boolean firstLil = false;
                boolean secondLil = false;

                ArrayList<Integer> id = new ArrayList<>();

                EventDate start = null;
                EventDate end = schedule.date.dateEnd;

                for(int j = i + 1; j < stages.size(); j++) {
                    StageSchedule temp = stages.get(j);

                    if(isLilFestival(temp) && !firstLil) {
                        firstLil = true;
                        analyzed[j] = true;
                        start = temp.date.dateStart;
                        id.add(j);
                    } else if(isSecondLilFestival(temp) && !secondLil) {
                        secondLil = true;
                        analyzed[j] = true;
                        id.add(j);
                    }
                }

                if(firstLil && secondLil) {
                    appendProperly(schedule, manualSchedulePrint(start, end, "Li'l Festival!"), normals, dailys, weeklys, monthlys, yearlys);
                } else {
                    for(int j = 0; j < id.size(); j++) {
                        analyzed[j] = false;
                    }

                    appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
                }
            } else {
                appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
            }
        } else {
                appendProperly(schedule, schedule.beautify(), normals, dailys, weeklys, monthlys, yearlys);
            }

            analyzed[i] = true;
        }

        if(!normals.isEmpty()) {
            data.append("```less\n");

            for (String normal : normals) {
                data.append(normal)
                        .append("\n");
            }

            data.append("```\n");
        }

        if(!dailys.isEmpty()) {
            data.append("New Daily Schedule Found\n\n");

            data.append("```less\n");

            for (String day : dailys) {
                data.append(day)
                        .append("\n");
            }

            data.append("```\n");
        }

        if(!weeklys.isEmpty()) {
            data.append("New Weekly Schedule Found\n\n");

            data.append("```less\n");

            for (String week : weeklys) {
                data.append(week)
                        .append("\n");
            }

            data.append("```\n");
        }

        if(!monthlys.isEmpty()) {
            data.append("New Monthly Schedule Found\n\n");

            data.append("```less\n");

            for (String month : monthlys) {
                data.append(month)
                        .append("\n");
            }

            data.append("```\n");
        }

        if(!yearlys.isEmpty()) {
            data.append("New Yearly Schedule Found\n\n");

            data.append("```less\n");

            for (String year : yearlys) {
                data.append(year)
                        .append("\n");
            }

            data.append("```\n");
        }

        return data.toString();
    }

    private ArrayList<String> getOnlyNewLine(File src, File newSrc) throws Exception {
        ArrayList<String> oldLines = new ArrayList<>();
        ArrayList<String> newLines = new ArrayList<>();

        BufferedReader oldReader = new BufferedReader(new FileReader(src));
        BufferedReader newReader = new BufferedReader(new FileReader(newSrc));

        String line = oldReader.readLine();

        if(line == null || !line.contains("[start]")) {
            newReader.close();
            oldReader.close();

            throw new IllegalStateException("Source file " + src.getAbsolutePath() + " doesn't have event data format!");
        }

        line = newReader.readLine();

        if(line == null || !line.contains("[start]")) {
            newReader.close();
            oldReader.close();

            throw new IllegalStateException("New source file " + newSrc.getAbsolutePath() + " doesn't have event data format!");
        }

        while((line = oldReader.readLine()) != null) {
            if(line.contains("[end]"))
                break;
            else {
                oldLines.add(line);
            }
        }

        while((line = newReader.readLine()) != null) {
            if(line.contains("[end]")) {
                break;
            } else {
                newLines.add(line);
            }
        }

        newReader.close();
        oldReader.close();

        newLines.removeAll(oldLines);

        return newLines;
    }

    private boolean prepareFiles(int locale) {
        File event = new File("./data/event/");

        if(!event.exists()) {
            boolean res = event.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+event.getAbsolutePath());
                return false;
            }
        }

        String lang = getLocaleName(locale);

        File loc = new File("./data/event/"+lang);

        if(!loc.exists()) {
            boolean res = loc.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+loc.getAbsolutePath());
                return false;
            }
        }

        return true;
    }

    private String getLocaleName(int locale) {
        if (locale == JP) {
            return "jp";
        }

        return "global";
    }

    private void appendProperly(StageSchedule schedule, String val, ArrayList<String> normal, ArrayList<String> daily, ArrayList<String> weekly, ArrayList<String> monthly, ArrayList<String> yearly) {
        if(schedule.type == null)
            normal.add(val);
        else {
            switch (schedule.type) {
                case DAILY:
                    daily.add(val);
                    break;
                case WEEKLY:
                    weekly.add(val);
                    break;
                case MONTHLY:
                    monthly.add(val);
                    break;
                case YEARLY:
                    yearly.add(val);
                    break;
            }
        }
    }

    public void initialize() throws Exception {
        String[] loc = {"jp", "global"};
        String[] file = {"sale.tsv", "gatya.tsv", "item.tsv"};

        for(int j = 0; j < loc.length; j++) {
            String locale = loc[j];

            for(int i = 0; i < file.length; i++) {
                File f = new File("./data/event/"+locale+"/"+file[i]);

                if(!f.exists())
                    continue;

                if(i == 0) {
                    updateStage(f, j, true);
                }
            }
        }
    }

    private String manualSchedulePrint(EventDate dateStart, EventDate dateEnd, String scheduleName) {
        StringBuilder result = new StringBuilder("[");

        if(dateStart.year != dateEnd.year || dateStart.year != currentYear) {
            result.append(dateStart.year)
                    .append(" ");
        }

        result.append(getMonth(dateStart.month))
                .append(" ")
                .append(dateStart.day)
                .append(getNumberExtension(dateStart.day))
                .append(" ~ ");

        if(dateEnd.equals(END)) {
            result.append("] ");
        } else {
            if(dateStart.year != dateEnd.year) {
                result.append(dateEnd.year)
                        .append(" ");
            }

            if(dateStart.month != dateEnd.month) {
                result.append(getMonth(dateEnd.month))
                        .append(" ");
            }

            result.append(dateEnd.day)
                    .append(getNumberExtension(dateEnd.day))
                    .append("] ");
        }

        result.append(scheduleName);

        return result.toString();
    }

    public boolean[][] checkUpdates() throws Exception {
        boolean[][] updates = new boolean[2][3];

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                return updates;
            }
        }

        String[] loc = {"jp", "en"};
        String[] file = {"sale.tsv", "gatya.tsv", "item.tsv"};

        for(int i = 0; i < loc.length; i++) {
            String locale = loc[i];

            for(int j = 0; j < file.length; j++) {
                String fi = file[j];
                File f = new File("./data/"+getLocaleName(i)+"/"+fi);

                String url = EVENTURL.replace("LL", locale).replace("FFFF", fi);

                if(f.exists()) {
                    String oldMD5 = StaticStore.fileToMD5(f);
                    String newMD5 = getMD5fromURL(url);

                    if(oldMD5 == null || !oldMD5.equals(newMD5)) {
                        updates[i][j] = true;

                        File temporary = new File("./temp", StaticStore.findFileName(temp, fi, ".tmp"));
                        File target = new File("./temp", StaticStore.findFileName(temp, fi.split("\\.")[0], ".tsv"));

                        UpdateCheck.Downloader down = StaticStore.getDownloader(url, target, temporary);

                        down.run(d -> {});

                        switch (j) {
                            case 0:
                                updateStage(target, i, false);
                                break;
                            case 1:

                        }
                    }
                } else {
                    updates[i][j] = true;

                    File temporary = new File("./temp", StaticStore.findFileName(temp, fi, ".tmp"));
                    File target = new File("./temp", StaticStore.findFileName(temp, fi.split("\\.")[0], ".tsv"));

                    UpdateCheck.Downloader down = StaticStore.getDownloader(url, target, temporary);

                    down.run(d -> {});

                    switch (j) {
                        case 0:
                            updateStage(target, i, false);
                            break;
                        case 1:

                    }
                }
            }
        }

        return updates;
    }

    private String getMD5fromURL(String url) throws Exception {
        URL u = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) u.openConnection();

        InputStream is = connection.getInputStream();

        byte[] buffer = new byte[2048];

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        int n;

        while((n = is.read(buffer)) != -1) {
            md5.update(buffer, 0, n);
        }

        is.close();

        byte[] result = md5.digest();

        BigInteger big = new BigInteger(1, result);

        String str = big.toString(16);

        return String.format("%32s", str).replace(' ', '0');
    }
}
