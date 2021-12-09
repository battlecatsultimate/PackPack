package mandarin.packpack.supporter.event;

import mandarin.packpack.supporter.DateComparator;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.group.*;
import mandarin.packpack.supporter.lang.LangID;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHolder extends EventFactor {
    public Map<Integer, List<Integer>> yearly = new HashMap<>();
    public Map<Integer, List<Integer>> monthly = new HashMap<>();
    public Map<Integer, List<Integer>> weekly = new HashMap<>();
    public Map<Integer, List<Integer>> daily = new HashMap<>();

    public Map<Integer, List<StageSchedule>> stages = new HashMap<>();
    public Map<Integer, List<GachaSchedule>> gachas = new HashMap<>();
    public Map<Integer, List<ItemSchedule>> items = new HashMap<>();

    public Map<Integer, List<Integer>> stageCache = new HashMap<>();
    public Map<Integer, List<Integer>> gachaCache = new HashMap<>();
    public Map<Integer, List<Integer>> itemCache = new HashMap<>();

    public void updateStage(File f, int locale, boolean init) throws Exception {
        if(failedToPrepareFile(locale))
            return;

        File file = new File("./data/event/"+getLocaleName(locale), "sale.tsv");

        boolean needToAnalyze = false;

        if(!file.exists()) {
            needToAnalyze = true;
        } else {
            String newMd5 = StaticStore.fileToMD5(f);
            String md5 = StaticStore.fileToMD5(file);

            if(newMd5 != null && !newMd5.equals(md5))
                needToAnalyze = true;
        }

        if(!needToAnalyze) {
            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }

            return;
        }

        ArrayList<String> newLines;

        if(file.exists() && !init) {
            newLines = getOnlyNewLine(file, f, locale);
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

            if(stageCache.containsKey(locale)) {
                List<Integer> cache = stageCache.get(locale);

                ArrayList<String> filteredLines = new ArrayList<>();

                for(int i = 0; i < cache.size(); i++) {
                    filteredLines.add(newLines.get(cache.get(i)));
                }

                if(!filteredLines.isEmpty()) {
                    newLines = filteredLines;
                }
            }
        }

        if(!file.getAbsolutePath().equals(f.getAbsolutePath())) {
            if(file.exists()) {
                boolean res = file.delete();

                if(!res) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+file.getAbsolutePath());
                    return;
                }
            }

            boolean result = f.renameTo(file);

            if(!result) {
                StaticStore.logger.uploadLog("Failed to rename file\n"+"Src : "+f.getAbsolutePath()+"\nDst : "+file.getAbsolutePath());
                return;
            }
        }

        List<StageSchedule> stages = this.stages.containsKey(locale) ? this.stages.get(locale) : new ArrayList<>();

        List<Integer> daily = this.daily.containsKey(locale) ? this.daily.get(locale) : new ArrayList<>();
        List<Integer> weekly = this.weekly.containsKey(locale) ? this.weekly.get(locale) : new ArrayList<>();
        List<Integer> monthly = this.monthly.containsKey(locale) ? this.monthly.get(locale) : new ArrayList<>();
        List<Integer> yearly = this.yearly.containsKey(locale) ? this.yearly.get(locale) : new ArrayList<>();

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

    public void updateGacha(File f, int locale, boolean init) throws Exception {
        if(failedToPrepareFile(locale))
            return;

        File file = new File("./data/event/"+getLocaleName(locale), "gatya.tsv");

        boolean needToAnalyze = false;

        if(!file.exists()) {
            needToAnalyze = true;
        } else {
            String newMd5 = StaticStore.fileToMD5(f);
            String md5 = StaticStore.fileToMD5(file);

            if(newMd5 != null && !newMd5.equals(md5))
                needToAnalyze = true;
        }

        if(!needToAnalyze) {
            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }

            return;
        }

        ArrayList<String> newLines;

        if(file.exists() && !init) {
            newLines = getOnlyNewLine(file, f, locale);
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

            if(gachaCache.containsKey(locale)) {
                List<Integer> cache = gachaCache.get(locale);

                ArrayList<String> filteredLines = new ArrayList<>();

                for(int i = 0; i < cache.size(); i++) {
                    filteredLines.add(newLines.get(cache.get(i)));
                }

                if(!filteredLines.isEmpty()) {
                    newLines = filteredLines;
                }
            }
        }

        if(!file.getAbsolutePath().equals(f.getAbsolutePath())) {
            if(file.exists()) {
                boolean res = file.delete();

                if(!res) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+file.getAbsolutePath());
                    return;
                }
            }

            boolean result = f.renameTo(file);

            if(!result) {
                StaticStore.logger.uploadLog("Failed to rename file\n"+"Src : "+f.getAbsolutePath()+"\nDst : "+file.getAbsolutePath());
                return;
            }
        }

        List<GachaSchedule> g = gachas.containsKey(locale) ? gachas.get(locale) : new ArrayList<>();

        if(!g.isEmpty())
            g.clear();

        for(String line : newLines) {
            g.add(new GachaSchedule(line));
        }

        g.removeIf(ga -> ga.gacha.isEmpty());

        this.gachas.put(locale, g);
    }

    public void updateItem(File f, int locale, boolean init) throws Exception {
        if(failedToPrepareFile(locale))
            return;

        File file = new File("./data/event/"+getLocaleName(locale), "item.tsv");

        boolean needToAnalyze = false;

        if(!file.exists()) {
            needToAnalyze = true;
        } else {
            String newMd5 = StaticStore.fileToMD5(f);
            String md5 = StaticStore.fileToMD5(file);

            if(newMd5 != null && !newMd5.equals(md5))
                needToAnalyze = true;
        }

        if(!needToAnalyze) {
            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }

            return;
        }

        ArrayList<String> newLines;

        if(file.exists() && !init) {
            newLines = getOnlyNewLine(file, f, locale);
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

            if(itemCache.containsKey(locale)) {
                List<Integer> cache = itemCache.get(locale);

                ArrayList<String> filteredLines = new ArrayList<>();

                for(int i = 0; i < cache.size(); i++) {
                    filteredLines.add(newLines.get(cache.get(i)));
                }

                if(!filteredLines.isEmpty()) {
                    newLines = filteredLines;
                }
            }
        }

        if(!file.getAbsolutePath().equals(f.getAbsolutePath())) {
            if(file.exists()) {
                boolean res = file.delete();

                if(!res) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+file.getAbsolutePath());
                    return;
                }
            }

            boolean result = f.renameTo(file);

            if(!result) {
                StaticStore.logger.uploadLog("Failed to rename file\n"+"Src : "+f.getAbsolutePath()+"\nDst : "+file.getAbsolutePath());
                return;
            }
        }

        List<ItemSchedule> i = items.containsKey(locale) ? items.get(locale) : new ArrayList<>();

        if(!i.isEmpty())
            i.clear();

        for(String line : newLines) {
            i.add(new ItemSchedule(line));
        }

        this.items.put(locale, i);
    }

    public ArrayList<String> printStageEvent(int locale, int lang) {
        ArrayList<String> result = new ArrayList<>();

        List<StageSchedule> stages = this.stages.get(locale);

        if(stages == null || stages.isEmpty())
            return result;

        boolean[] analyzed = new boolean[stages.size()];

        ArrayList<String> normals = new ArrayList<>();
        ArrayList<String> dailys = new ArrayList<>();
        ArrayList<String> weeklys = new ArrayList<>();
        ArrayList<String> monthlys = new ArrayList<>();
        ArrayList<String> yearlys = new ArrayList<>();
        ArrayList<String> missions = new ArrayList<>();

        for(GroupHandler group : EventFactor.handlers)
            group.clear();

        for(StageSchedule schedule : stages) {
            for(GroupHandler group : EventFactor.handlers) {
                group.handleEvent(schedule);
            }
        }

        for(int i = 0; i < stages.size(); i++) {
            StageSchedule s = stages.get(i);

            for(GroupHandler group : EventFactor.handlers) {
                for(EventGroup event : group.getGroups()) {
                    if(event.finished) {
                        for(Schedule schedule : event.getSchedules()) {
                            if(schedule instanceof StageSchedule && s.equals(schedule)) {
                                analyzed[i] = true;
                                break;
                            }
                        }

                        if(event instanceof ContainedEventGroup) {
                            for(List<Schedule> schedules : ((ContainedEventGroup) event).getSubSchedules()) {
                                for(Schedule schedule : schedules) {
                                    if(schedule instanceof StageSchedule && s.equals(schedule)) {
                                        analyzed[i] = true;
                                        break;
                                    }
                                }

                                if(analyzed[i])
                                    break;
                            }
                        }
                    }

                    if(analyzed[i])
                        break;
                }

                if(analyzed[i])
                    break;
            }
        }

        for(GroupHandler handler : EventFactor.handlers) {
            for(EventGroup event : handler.getGroups()) {
                if(event.finished) {
                    if(handler instanceof NormalGroupHandler) {
                        StageSchedule start = (StageSchedule) event.schedules[0];

                        appendProperly(start, start.beautifyWithCustomName(LangID.getStringByID(event.name, lang), lang), normals, dailys, weeklys, monthlys, yearlys, missions);
                    } else if(handler instanceof SequenceGroupHandler) {
                        StageSchedule start = (StageSchedule) event.schedules[0];
                        StageSchedule end = (StageSchedule) event.schedules[event.schedules.length - 1];

                        appendProperly(start, manualSchedulePrint(start.date.dateStart, end.date.dateEnd, LangID.getStringByID(event.name, lang), lang), normals, dailys, weeklys, monthlys, yearlys, missions);
                    } else if(handler instanceof ContainedGroupHandler) {
                        StageSchedule primary = (StageSchedule) event.schedules[0];

                        appendProperly(primary, primary.beautifyWithCustomName(LangID.getStringByID(event.name, lang), lang), normals, dailys, weeklys, monthlys, yearlys, missions);
                    }
                }
            }
        }

        for(int i = 0; i < stages.size(); i++) {
            if (analyzed[i])
                continue;

            StageSchedule schedule = stages.get(i);

            if(schedule.isMission && isWeeklyAndMonthlyMission(schedule)) {
                analyzed[i] = true;
                continue;
            }

            appendProperly(schedule, schedule.beautify(lang), normals, dailys, weeklys, monthlys, yearlys, missions);

            analyzed[i] = true;
        }

        normals.sort(new DateComparator(lang));
        dailys.sort(new DateComparator(lang));
        weeklys.sort(new DateComparator(lang));
        monthlys.sort(new DateComparator(lang));
        yearlys.sort(new DateComparator(lang));

        if(!normals.isEmpty()) {
            StringBuilder data = new StringBuilder();

            data.append("```scss\n");

            for (String normal : normals) {
                data.append(normal)
                        .append("\n");
            }

            data.append("```");

            result.add(data.toString());
        }

        if(!dailys.isEmpty()) {
            StringBuilder data = new StringBuilder(LangID.getStringByID("printstage_daily", lang)).append("\n\n```scss\n");

            for (String day : dailys) {
                data.append(day)
                        .append("\n");
            }

            data.append("```");

            result.add(data.toString());
        }

        if(!weeklys.isEmpty()) {
            StringBuilder data = new StringBuilder(LangID.getStringByID("printstage_weekly", lang)).append("\n\n```scss\n");

            for (String week : weeklys) {
                data.append(week)
                        .append("\n");
            }

            data.append("```");

            result.add(data.toString());
        }

        if(!monthlys.isEmpty()) {
            StringBuilder data = new StringBuilder(LangID.getStringByID("printstage_monthly", lang)).append("\n\n```scss\n");

            for (String month : monthlys) {
                data.append(month)
                        .append("\n");
            }

            data.append("```");

            result.add(data.toString());
        }

        if(!yearlys.isEmpty()) {
            StringBuilder data = new StringBuilder(LangID.getStringByID("printstage_yearly", lang)).append("\n\n```scss\n");

            for (String year : yearlys) {
                data.append(year)
                        .append("\n");
            }

            data.append("```");

            result.add(data.toString());
        }

        if(!missions.isEmpty()) {
            StringBuilder data = new StringBuilder(LangID.getStringByID("event_mission", lang)).append("\n\n```scss\n");

            for (String mission : missions) {
                data.append(mission)
                        .append("\n\n");
            }

            data.append("```");

            result.add(data.toString());
        }

        return result;
    }

    public String printGachaEvent(int locale, int lang) {
        List<GachaSchedule> gachas = this.gachas.get(locale);

        StringBuilder data = new StringBuilder(LangID.getStringByID("event_gacha", lang)).append("\n\n");

        ArrayList<String> normals = new ArrayList<>();

        for(int i = 0; i < gachas.size(); i++) {
            GachaSchedule schedule = gachas.get(i);

            String beauty = schedule.beautify(lang);

            normals.add(beauty);
        }

        normals.sort(new DateComparator(lang));

        if(normals.isEmpty()) {
           return "";
        }

        data.append("```scss\n");

        for(int i = 0; i < normals.size(); i++) {
            data.append(normals.get(i)).append("\n");
        }

        data.append("\n");

        data.append(LangID.getStringByID("printgacha_g", lang))
                .append(" : ")
                .append(LangID.getStringByID("printgacha_gua", lang))
                .append(" | ")
                .append(LangID.getStringByID("printgacha_s", lang))
                .append(" : ")
                .append(LangID.getStringByID("printgacha_step", lang))
                .append(" | ")
                .append(LangID.getStringByID("printgacha_l", lang))
                .append(" : ")
                .append(LangID.getStringByID("printgacha_lucky", lang))
                .append(" | ")
                .append(LangID.getStringByID("printgacha_p", lang))
                .append(" : ")
                .append(LangID.getStringByID("printgacha_plat", lang))
                .append(" | ")
                .append(LangID.getStringByID("printgacha_n", lang))
                .append(" : ")
                .append(LangID.getStringByID("printgacha_neneko", lang))
                .append(" | ")
                .append(LangID.getStringByID("printgacha_gr", lang))
                .append(" : ")
                .append(LangID.getStringByID("printgacha_gran", lang))
                .append(" | ")
                .append(LangID.getStringByID("printgacha_r", lang))
                .append(" : ")
                .append(LangID.getStringByID("printgacha_rein", lang))
                .append("\n");

        data.append("```");

        return data.toString();
    }

    public String printItemEvent(int locale, int lang) {
        List<ItemSchedule> items = this.items.get(locale);

        StringBuilder data = new StringBuilder(LangID.getStringByID("event_item", lang)).append("\n\n");

        ArrayList<String> normals = new ArrayList<>();

        for(int i = 0; i < items.size(); i++) {
            ItemSchedule schedule = items.get(i);

            String beauty = schedule.beautify(lang);

            normals.add(beauty);
        }

        normals.sort(new DateComparator(lang));

        if(normals.isEmpty()) {
            return "";
        }

        data.append("```scss\n");

        for(int i = 0; i < normals.size(); i++) {
            data.append(normals.get(i)).append("\n");
        }

        data.append("```");

        return data.toString();
    }

    private ArrayList<String> getOnlyNewLine(File src, File newSrc, int loc) throws Exception {
        ArrayList<String> oldLines = new ArrayList<>();
        ArrayList<String> newLines = new ArrayList<>();

        List<Integer> newIndex = new ArrayList<>();

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

        for(int i = 0; i < newLines.size(); i++) {
            newIndex.add(i);
        }

        for(int i = 0; i < oldLines.size(); i++) {
            if(newLines.contains(oldLines.get(i))) {
                int index = newLines.indexOf(oldLines.get(i));
                newIndex.remove(Integer.valueOf(index));
            }
        }

        newLines.removeAll(oldLines);

        switch (newSrc.getName()) {
            case "gatya.tsv":
                gachaCache.put(loc, newIndex);
                break;
            case "item.tsv":
                itemCache.put(loc, newIndex);
                break;
            case "sale.tsv":
                stageCache.put(loc, newIndex);
                break;
        }

        return newLines;
    }

    private boolean failedToPrepareFile(int locale) {
        File event = new File("./data/event/");

        if(!event.exists()) {
            boolean res = event.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+event.getAbsolutePath());
                return true;
            }
        }

        String lang = getLocaleName(locale);

        File loc = new File("./data/event/"+lang);

        if(!loc.exists()) {
            boolean res = loc.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+loc.getAbsolutePath());
                return true;
            }
        }

        return false;
    }

    private String getLocaleName(int locale) {
        switch (locale) {
            case EN:
                return "en";
            case ZH:
                return "zh";
            case KR:
                return "kr";
            default:
                return "jp";
        }
    }

    private void appendProperly(StageSchedule schedule, String val, ArrayList<String> normal, ArrayList<String> daily, ArrayList<String> weekly, ArrayList<String> monthly, ArrayList<String> yearly, ArrayList<String> missions) {
        if(schedule.isMission)
            missions.add(val);
        else if(schedule.type == null)
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
        String[] loc = {"en", "zh", "kr", "jp"};
        String[] file = {"gatya.tsv", "item.tsv", "sale.tsv"};

        for(int j = 0; j < loc.length; j++) {
            String locale = loc[j];

            for(int i = 0; i < file.length; i++) {
                File f = new File("./data/event/"+locale+"/"+file[i]);

                if(!f.exists())
                    continue;


                switch (i) {
                    case GATYA:
                        updateGacha(f, j, true);
                        break;
                    case SALE:
                        updateStage(f, j, true);
                        break;
                    case ITEM:
                        updateItem(f, j, true);
                        break;
                }
            }
        }
    }

    private String manualSchedulePrint(EventDate dateStart, EventDate dateEnd, String scheduleName, int lang) {
        StringBuilder result = new StringBuilder("[");

        if(dateStart.year != dateEnd.year || dateStart.year != currentYear) {
            result.append(dateStart.year)
                    .append(" ");
        }

        result.append(getMonth(dateStart.month, lang))
                .append(" ")
                .append(dateStart.day)
                .append(getNumberExtension(dateStart.day, lang))
                .append(" ~ ");

        if(dateEnd.equals(END)) {
            result.append("] ");
        } else {
            if(dateStart.year != dateEnd.year) {
                result.append(dateEnd.year)
                        .append(" ");
            }

            if(dateStart.month != dateEnd.month) {
                result.append(getMonth(dateEnd.month, lang))
                        .append(" ");
            }

            result.append(dateEnd.day)
                    .append(getNumberExtension(dateEnd.day, lang))
                    .append("] ");
        }

        result.append(scheduleName);

        return result.toString();
    }

    public boolean[][] checkUpdates() throws Exception {
        boolean[][] updates = new boolean[4][3];

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                return updates;
            }
        }

        String[] file = {"gatya.tsv", "item.tsv", "sale.tsv"};

        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < file.length; j++) {
                String fi = file[j];
                File f = new File("./data/event/"+getLocaleName(i)+"/"+fi);

                String url = EventFileGrabber.getLink(i, j);

                if(url == null)
                    continue;

                if(f.exists()) {
                    String oldMD5 = StaticStore.fileToMD5(f);
                    String newMD5 = getMD5fromURL(url);

                    if(oldMD5 == null || !oldMD5.equals(newMD5)) {
                        updates[i][j] = true;

                        File temporary = new File("./temp", StaticStore.findFileName(temp, fi, ".tmp"));
                        File target = new File("./temp", StaticStore.findFileName(temp, fi.split("\\.")[0], ".tsv"));

                        if(!temporary.exists() && !temporary.createNewFile()) {
                            StaticStore.logger.uploadLog("Failed to create file : "+temporary.getAbsolutePath());
                            continue;
                        }

                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        connection.setRequestMethod("GET");

                        connection.connect();

                        FileOutputStream fos = new FileOutputStream(temporary);
                        InputStream ins = connection.getInputStream();

                        byte[] buffer = new byte[1024];
                        int n;

                        while((n = ins.read(buffer)) != -1) {
                            fos.write(buffer, 0, n);
                        }

                        fos.close();
                        ins.close();

                        connection.disconnect();

                        if(!temporary.renameTo(target)) {
                            StaticStore.logger.uploadLog("Failed to rename file\nSrc : "+temporary.getAbsolutePath()+"\nDst : "+target.getAbsolutePath());
                            continue;
                        }

                        switch (j) {
                            case SALE:
                                updateStage(target, i, false);
                                break;
                            case GATYA:
                                updateGacha(target, i, false);
                                break;
                            case ITEM:
                                updateItem(target, i, false);
                                break;
                            default:
                                if(f.exists() && !f.delete()) {
                                    StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                                    break;
                                }

                                if(!target.renameTo(f)) {
                                    StaticStore.logger.uploadLog("Failed to rename file\nSrc : "+target.getAbsolutePath()+"\nDst : "+f.getAbsolutePath());
                                }
                                break;
                        }
                    }
                } else {
                    updates[i][j] = true;

                    File temporary = new File("./temp", StaticStore.findFileName(temp, fi, ".tmp"));
                    File target = new File("./temp", StaticStore.findFileName(temp, fi.split("\\.")[0], ".tsv"));

                    if(!temporary.exists() && !temporary.createNewFile()) {
                        StaticStore.logger.uploadLog("Failed to create file : "+temporary.getAbsolutePath());
                        continue;
                    }

                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("GET");

                    connection.connect();

                    FileOutputStream fos = new FileOutputStream(temporary);
                    InputStream ins = connection.getInputStream();

                    byte[] buffer = new byte[1024];
                    int n;

                    while((n = ins.read(buffer)) != -1) {
                        fos.write(buffer, 0, n);
                    }

                    fos.close();
                    ins.close();

                    connection.disconnect();

                    if(!temporary.renameTo(target)) {
                        StaticStore.logger.uploadLog("Failed to rename file\nSrc : "+temporary.getAbsolutePath()+"\nDst : "+target.getAbsolutePath());
                        continue;
                    }

                    switch (j) {
                        case SALE:
                            updateStage(target, i, false);
                            break;
                        case GATYA:
                            updateGacha(target, i, false);
                            break;
                        case ITEM:
                            updateItem(target, i, false);
                            break;
                        default:
                            if(f.exists() && !f.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                                break;
                            }

                            if(!target.renameTo(f)) {
                                StaticStore.logger.uploadLog("Failed to rename file\nSrc : "+target.getAbsolutePath()+"\nDst : "+f.getAbsolutePath());
                            }
                            break;
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
