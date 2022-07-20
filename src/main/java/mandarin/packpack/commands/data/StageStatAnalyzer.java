package mandarin.packpack.commands.data;

import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.cell.CellData;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StageStatAnalyzer extends ConstraintCommand {
    private static final List<String> allParameters = List.of(
            "-s", "-second", "-code", "-m", "-mid", "-s", "-l", "-len", "-n", "-name", "-lv", "-c", "-cell", "-apk",
            "-en", "-jp", "-tw", "-kr"
    );

    public StageStatAnalyzer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        File container = StaticStore.generateTempFile(temp, "stat", "", true);

        if(container == null) {
            return;
        }

        MessageChannel ch = getChannel(event);
        Message author = getMessage(event);

        if(ch == null || author == null)
            return;

        String command = getContent(event);

        int mid = getMID(command);
        int len = getLen(command);
        String code = getCode(command);

        if(mid == -1) {
            ch.sendMessage(LangID.getStringByID("stanalyzer_sid", lang)).queue();

            return;
        }

        if(code == null) {
            ch.sendMessage(LangID.getStringByID("stanalyzer_code", lang)).queue();

            return;
        }

        int level = getLevel(command);
        boolean isSecond = isSecond(command);
        boolean isApk = isApk(command);
        String[] name = getName(command);

        if(name == null) {
            name = new String[len];

            for(int i = 0; i < len; i++) {
                name[i] = code + " - " + Data.trio(mid) + " - " + Data.trio(i);
            }
        } else if(name.length != len) {
            int nLen = name.length;

            ch.sendMessage(LangID.getStringByID("stat_name", lang).replace("_RRR_", len+"").replace("_PPP_", nLen+"")).queue();

            return;
        }

        String cell = getCellData(command);

        ArrayList<CellData> cellData = new ArrayList<>();

        String warnings = "";

        if(cell != null) {
            warnings += parseCellData(cellData, cell) + "\n\n";
        }

        warnings = warnings.replaceAll("\n{3,}", "\n\n").strip();

        if(!warnings.isBlank()) {
            ch.sendMessage(warnings).queue();
        }

        if(isApk) {
            String localeCode = getLocale(command);

            File workspace = new File("./data/bc/"+localeCode+"/workspace");

            if(!workspace.exists()) {
                ch.sendMessage("Couldn't find workspace folder, try to call `p!da [Locale]` first").queue();

                return;
            }

            if(!validateFile(workspace, code, mid)) {
                ch.sendMessage("Couldn't find sufficient data for unit code : "+code+ " - "+mid).queue();

                return;
            }

            File dataLocal = new File(workspace, "DataLocal");
            File imageLocal = new File(workspace, "ImageLocal");
        }
    }

    private int getMID(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-s") || contents[i].equals("-sid")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return -1;
    }

    private String getCode(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-co") || contents[i].equals("-code")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return contents[i + 1].toUpperCase(Locale.ENGLISH);
            }
        }

        return null;
    }

    private int getLen(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-l") || contents[i].equals("-len")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return Math.min(3, Math.max(1, StaticStore.safeParseInt(contents[i + 1])));
            }
        }

        return 1;
    }

    private int getLevel(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-lv") && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return 30;
    }

    private boolean isSecond(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-s") || contents[i].equals("-second"))
                return true;
        }

        return false;
    }

    private boolean isApk(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-apk"))
                return true;
        }

        return false;
    }

    private String getCellData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-c") || contents[i].equals("-cell") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(abortAppending(contents[j], "-c", "-cell"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
    }

    private String[] getName(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-n") || contents[i].equals("-name") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(abortAppending(contents[j], "-n", "-name"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                String[] n = builder.toString().split(",");

                for(int j = 0; j < n.length; j++) {
                    n[j] = n[j].strip();
                }

                return n;
            }
        }

        return null;
    }

    private String parseCellData(List<CellData> result, String data) {
        if(data == null)
            return "W : Input was null";

        String[] contents = data.split(" ");

        int bracket = 0;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].contains("{"))
                bracket += StringUtils.countMatches(contents[i], '{');

            if(contents[i].contains("}"))
                bracket -= StringUtils.countMatches(contents[i], '}');
        }

        if(bracket != 0) {
            if(bracket > 0) {
                return "E : Opened number of bracket = "+bracket;
            } else {
                return "E : Over-closed number of bracket = "+(-bracket);
            }
        }

        StringBuilder res = new StringBuilder();

        Pattern p = Pattern.compile("\\{(.+?)?}");
        Matcher m = p.matcher(data);

        Pattern gp = Pattern.compile("\\[(.+?)?]");

        int i = 0;

        while(m.find()) {
            String group = m.group(1);

            Matcher gm = gp.matcher(group);

            List<String> arrays = new ArrayList<>();

            while(gm.find()) {
                arrays.add(gm.group(1));
            }

            if(arrays.size() != 1) {
                if(arrays.size() > 1) {
                    res.append("W : Overfed array data in cell No.").append(i + 1).append("\n");
                } else {
                    res.append("W : Lacking array data in cell No.").append(i + 1).append("\n");
                }

                i++;

                continue;
            }

            String[] ind = arrays.get(0).split(",");

            int[] indexes = new int[ind.length];

            boolean invalidNumber = false;

            for(int j = 0; j < ind.length; j++) {
                if(StaticStore.isNumeric(ind[j].strip())) {
                    indexes[j] = StaticStore.safeParseInt(ind[j].strip());
                } else {
                    invalidNumber = true;
                    break;
                }
            }

            if(invalidNumber) {
                res.append("W : Invalid index array data at cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(arrays.get(0))
                        .append("\n");

                i++;

                continue;
            }

            String[] rest = group.replaceAll("\\["+arrays.get(0)+"]( +?)?,", "").split(",");

            if(rest.length != 4) {
                res.append("W : Insufficient data is provided at cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(rest.length)
                        .append("\n");

                i++;

                continue;
            }

            String isTime = rest[1].replace(",", "").strip();

            if(!isTime.startsWith("t") && !isTime.startsWith("f")) {
                res.append("W : isTimeUnit is not a boolean type at cell No.")
                        .append(i + 1)
                        .append(" -> isTimeUnit = ")
                        .append(isTime)
                        .append("\n");

                i++;

                continue;
            }

            boolean isTimeUnit = isTime.startsWith("t");

            String name = rest[0].replace(",", "").strip();

            String one = rest[2].replace(",", "").strip();

            if(!one.startsWith("t") && !one.startsWith("f")) {
                res.append("W : oneLine is not a boolean type at cell No.")
                        .append(i + 1)
                        .append(" -> oneLine = ")
                        .append(one)
                        .append("\n");

                i++;

                continue;
            }

            boolean oneLine = one.startsWith("t");

            String ignore = rest[1].replace(",", "").strip();

            if(!ignore.startsWith("t") && !ignore.startsWith("f")) {
                res.append("W : ignoreZero is not a boolean type at cell No.")
                        .append(i + 1)
                        .append(" -> ignoreZero = ")
                        .append(ignore)
                        .append("\n");

                i++;

                continue;
            }

            boolean ignoreZero = ignore.startsWith("t");

            result.add(new CellData(name, oneLine, indexes, isTimeUnit, ignoreZero));

            i++;
        }

        return res.substring(0, Math.max(0, res.length() - 1));
    }

    private boolean abortAppending(String content, String... exception) {
        for(int i = 0; i < exception.length; i++) {
            if(content.equals(exception[i]))
                return false;
        }

        return allParameters.contains(content);
    }

    private int getStageLength(File mapData) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(mapData, StandardCharsets.UTF_8));

        reader.readLine();
        reader.readLine();

        int len = 0;
        String line;

        while((line = reader.readLine()) != null) {
            if(!line.isBlank())
                len++;
            else
                return len;
        }

        return len;
    }

    private String getLocale(String content) {
        if(content.contains("-en"))
            return "en";
        else if(content.contains("-tw"))
            return "zh";
        else if(content.contains("-kr"))
            return "kr";
        else
            return "jp";
    }

    private boolean validateFile(File workspace, String code, int mID) throws Exception {
        File dataLocal = new File(workspace, "DataLocal");
        File imageLocal = new File(workspace, "ImageLocal");

        if(!dataLocal.exists() || !imageLocal.exists())
            return false;

        File mapData = new File(dataLocal, "MapStageData" + code + "_" + Data.trio(mID) + ".csv");

        if(!mapData.exists())
            return false;

        int len = getStageLength(mapData);

        for(int i = 0; i < len; i++) {
            File stageData = new File(dataLocal, "stage" + code + Data.trio(mID) + "_" + Data.duo(i) + ".csv");

            if(!stageData.exists())
                return false;
        }

        File mapOption = new File(dataLocal, "Map_option.csv");
        File stageOption = new File(dataLocal, "Stage_option.csv");
        File exGroup = new File(dataLocal, "EX_group.csv");
        File exLottery = new File(dataLocal, "EX_lottery.csv");
        File characterGroup = new File(dataLocal, "Charagroup.csv");

        return exists(mapOption, stageOption, exGroup, exLottery, characterGroup);
    }

    private boolean exists(File... files) {
        for(int i = 0; i < files.length; i++) {
            if(!files[i].exists())
                return false;
        }

        return true;
    }
}
