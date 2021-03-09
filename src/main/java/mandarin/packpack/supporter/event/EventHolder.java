package mandarin.packpack.supporter.event;

import mandarin.packpack.supporter.StaticStore;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;

public class EventHolder extends EventFactor {
    public static int currentYear;

    public ArrayList<Integer> yearly = new ArrayList<>();
    public ArrayList<Integer> monthly = new ArrayList<>();
    public ArrayList<Integer> weekly = new ArrayList<>();
    public ArrayList<Integer> daily = new ArrayList<>();

    public ArrayList<StageSchedule> stages = new ArrayList<>();
    public ArrayList<GachaSchedule> gachas = new ArrayList<>();
    public ArrayList<ItemSchedule> items = new ArrayList<>();

    public String stageMD5;
    public String gachaMD5;
    public String itemMD5;

    static {
        Calendar c = Calendar.getInstance();

        currentYear = c.get(Calendar.YEAR);
    }

    public void updateStage(File f, int locale) throws Exception {
        if(!prepareFiles(locale))
            return;

        File file = new File("./data/event/"+getLocaleName(locale), f.getName());

        boolean needToAnalyze = false;

        if(stageMD5 != null) {
            String md5 = StaticStore.fileToMD5(file);

            if(!stageMD5.equals(md5)) {
                stageMD5 = md5;
                needToAnalyze = true;
            }
        } else {
            stageMD5 = StaticStore.fileToMD5(file);
            needToAnalyze = true;
        }

        if(!needToAnalyze)
            return;

        ArrayList<String> newLines;

        if(file.exists()) {
            newLines = getOnlyNewLine(file, f);
        } else {
            newLines = new ArrayList<>();
        }

        boolean result = f.renameTo(file);

        if(!result)
            return;

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

                    return res;
                });
            } else if(schedule.type == StageSchedule.TYPE.WEEKLY) {
                schedule.stages.removeIf(sm -> {
                    int id = stageToInteger(sm);

                    boolean res = id != -1 && !weekly.contains(id);

                    if(!res)
                        weekly.add(id);

                    return res;
                });
            } else if(schedule.type == StageSchedule.TYPE.MONTHLY) {
                schedule.stages.removeIf(sm -> {
                    int id = stageToInteger(sm);

                    boolean res = id != -1 && !monthly.contains(id);

                    if(!res)
                        monthly.add(id);

                    return res;
                });
            } else if(schedule.type == StageSchedule.TYPE.YEARLY) {
                schedule.stages.removeIf(sm -> {
                    int id = stageToInteger(sm);

                    boolean res = id != -1 && !yearly.contains(id);

                    if(!res)
                        yearly.add(id);

                    return res;
                });
            }
        }

        StringBuilder data = new StringBuilder();

        boolean[] analzyed = new boolean[stages.size()];



        System.out.println(newLines);
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
        switch (locale) {
            case JP:
                return "jp";
            case KR:
                return  "kr";
            case TW:
                return "tw";
            default:
                return "en";
        }
    }
}
