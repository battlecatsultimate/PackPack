package mandarin.packpack.commands.data;

import common.CommonStatic;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.CustomMaskEnemy;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.cell.AbilityData;
import mandarin.packpack.supporter.bc.cell.CellData;
import mandarin.packpack.supporter.bc.cell.FlagCellData;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.message.EnemyStatAnalyzerHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnemyStatAnalyzer extends ConstraintCommand {
    private static final List<String> allParameters = List.of(
            "-s", "-second", "-m", "-eid", "-e", "-n", "-name", "-p", "-proc", "-a", "-ability", "-t",
            "-trait", "-c", "-cell", "-apk", "-en", "-jp", "-tw", "-kr"
    );

    public EnemyStatAnalyzer(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        File container = StaticStore.generateTempFile(temp, "stat", "", true);

        if(container == null) {
            return;
        }

        MessageChannel ch = loader.getChannel();

        String command = loader.getContent();

        int eid = getEID(command);

        if(eid == -1) {
            ch.sendMessage(LangID.getStringByID("stat_eid", lang)).queue();

            return;
        }

        int magnification = getMagnification(command);
        boolean isSecond = isSecond(command);
        boolean isApk = isApk(command);
        String name = getName(command);

        if(name == null) {
            name = Data.trio(eid);
        }

        String proc = getProcData(command);
        String abil = getAbilData(command);
        String trait = getTraitData(command);
        String cell = getCellData(command);

        ArrayList<AbilityData> procData = new ArrayList<>();
        ArrayList<FlagCellData> abilData = new ArrayList<>();
        ArrayList<FlagCellData> traitData = new ArrayList<>();
        ArrayList<CellData> cellData = new ArrayList<>();

        String warnings = "";

        if(proc != null) {
            warnings = parseProcData(procData, proc)+"\n\n";
        }

        if(abil != null) {
            warnings += parseFlagCellData(abilData, abil, "ability") + "\n\n";
        }

        if(trait != null) {
            warnings += parseFlagCellData(traitData, trait, "trait") + "\n\n";
        }

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

            if(!validateFile(workspace, eid)) {
                ch.sendMessage("Couldn't find sufficient data for enemy code : "+eid).queue();

                return;
            }

            File dataLocal = new File(workspace, "DataLocal");
            File imageDataLocal = new File(workspace, "ImageDataLocal");
            File imageLocal = new File(workspace, "ImageLocal");

            File statFile = new File(dataLocal, "t_unit.csv");

            BufferedReader statReader = new BufferedReader(new FileReader(statFile, StandardCharsets.UTF_8));

            int count = -2;

            while(count < eid) {
                statReader.readLine();
                count++;
            }

            File maanim = new File(imageDataLocal, Data.trio(eid)+"_e02.maanim");

            VFile anim = VFile.getFile(maanim);

            if(anim == null) {
                ch.sendMessage("Something went wrong while analyzing maanim data").queue();

                statReader.close();

                return;
            }

            MaAnim ma = MaAnim.newIns(anim.getData());

            CustomMaskEnemy data = new CustomMaskEnemy(statReader.readLine().split(","), ma);

            statReader.close();

            EntityHandler.generateEnemyStatImage(ch, cellData, procData, abilData, traitData, data, name, imageLocal, magnification, !isSecond, eid, lang);
        } else {
            List<String> requiredFiles = new ArrayList<>();

            requiredFiles.add("t_unit.csv");
            requiredFiles.add(Data.trio(eid)+"_e02.maanim");
            requiredFiles.add("enemy_icon_"+Data.trio(eid)+".png");

            StringBuilder sb = new StringBuilder("- Required File List -\n\n");

            for(int i = 0; i < requiredFiles.size(); i++) {
                sb.append(requiredFiles.get(i))
                        .append(" : Ready");

                if(i < requiredFiles.size() - 1) {
                    sb.append("\n");
                }
            }

            String finalName = name;

            ch.sendMessage(sb.toString()).queue(msg -> {
                if(msg == null)
                    return;

                new EnemyStatAnalyzerHolder(msg, loader.getMessage(), ch.getId(), container, requiredFiles, cellData, procData, abilData, traitData, eid, isSecond, magnification, finalName, lang);
            });
        }
    }

    private int getEID(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-e") || contents[i].equals("-eid")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return -1;
    }

    private int getMagnification(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-m") && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return 100;
    }

    private boolean isSecond(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-s") || contents[i].equals("-second"))
                return true;
        }

        return false;
    }

    private String getTraitData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-t") || contents[i].equals("-trait") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(abortAppending(contents[j], "-t", "-trait"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
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

    private String getAbilData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-a") || contents[i].equals("-ability") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(abortAppending(contents[j], "-a", "-ability"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
    }

    private String getProcData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-p") || contents[i].equals("-proc") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(abortAppending(contents[j], "-p", "-proc"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
    }

    private String getName(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-n") || contents[i].equals("-name") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(abortAppending(contents[j], "-n", "-name"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString();
            }
        }

        return null;
    }

    private String parseProcData(List<AbilityData> result, String data) {
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

            if(arrays.size() != 2) {
                if(arrays.size() > 2) {
                    res.append("W : Overfed array data in proc cell No.").append(i + 1).append("\n");
                } else {
                    res.append("W : Lacking array data in proc cell No.").append(i + 1).append("\n");
                }

                i++;

                continue;
            }

            String[] ind = arrays.getFirst().split(",");
            String[] isTime = arrays.get(1).split(",");

            if(isTime.length != ind.length) {
                res.append("W : Not synchronized index array and isTimeUnit array at proc cell No.")
                        .append(i + 1)
                        .append(" -> index = ")
                        .append(ind.length)
                        .append(" | isTimeUnit = ")
                        .append(isTime.length)
                        .append("\n");

                i++;

                continue;
            }

            int[] indexes = new int[ind.length];
            boolean[] isTimeUnit = new boolean[isTime.length];

            boolean invalidNumber = false;
            boolean invalidBoolean = false;

            for(int j = 0; j < ind.length; j++) {
                if(StaticStore.isNumeric(ind[j].strip())) {
                    indexes[j] = StaticStore.safeParseInt(ind[j].strip());
                } else {
                    invalidNumber = true;
                    break;
                }

                String b = isTime[j].strip();

                if(!b.startsWith("t") && !b.startsWith("f")) {
                    invalidBoolean = true;
                    break;
                } else {
                    isTimeUnit[j] = b.startsWith("t");
                }
            }


            if(invalidNumber) {
                res.append("W : Invalid index array data at proc cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(arrays.getFirst())
                        .append("\n");

                i++;

                continue;
            }

            if(invalidBoolean) {
                res.append("W : Invalid index array data at proc cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(arrays.get(1))
                        .append("\n");

                i++;

                continue;
            }

            String[] rest = group.replace("["+arrays.getFirst()+"]", "").split("\\["+arrays.get(1)+"]");

            if(rest.length != 2) {
                res.append("W : Insufficient data is provided at proc cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(rest.length)
                        .append("\n");

                i++;

                continue;
            }

            String format = rest[0].strip();

            format = format.substring(1, format.length() - 1).strip();

            String ignore = rest[1].replace(",", "").strip();

            if(StaticStore.isNumeric(ignore)) {
                int ignoreIndex = StaticStore.safeParseInt(ignore);

                boolean invalidIndex = true;

                for(int j = 0; j < indexes.length; j++) {
                    if(indexes[j] == ignoreIndex) {
                        invalidIndex = false;
                        break;
                    }
                }

                if(invalidIndex) {
                    res.append("W : Invalid ignoreIndex value at proc cell No.")
                            .append(i+1)
                            .append(" -> Arrays = ")
                            .append(Arrays.toString(indexes))
                            .append(" | IgnoreIndex = ")
                            .append(ignoreIndex)
                            .append("\n");

                    i++;

                    continue;
                }

                result.add(new AbilityData(indexes, format, isTimeUnit, ignoreIndex));
            } else {
                res.append("W : ignoreIndex isn't number -> ")
                        .append(ignore)
                        .append("\n");

                i++;

                continue;
            }

            i++;
        }

        return res.substring(0, Math.max(0, res.length() - 1));
    }

    private String parseFlagCellData(List<FlagCellData> result, String data, String dataName) {
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

        int i = 0;

        while(m.find()) {
            String[] content = m.group(1).split(",");

            if(content.length > 2) {
                res.append("W : Overfed data at ")
                        .append(dataName)
                        .append(" cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(contents.length);

                i++;

                continue;
            } else if(content.length < 2) {
                res.append("W : Insufficient data at ")
                        .append(dataName)
                        .append(" cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(contents.length)
                        .append("\n");

                i++;

                continue;
            }

            String ind = content[0].strip();

            if(!StaticStore.isNumeric(ind)) {
                res.append("W : Index is not a number at ")
                        .append(dataName)
                        .append(" cell No.")
                        .append(i + 1)
                        .append(" -> ")
                        .append(ind)
                        .append("\n");

                i++;

                continue;
            }

            int index = StaticStore.safeParseInt(ind);

            String name = content[1].strip();

            result.add(new FlagCellData(name, index));

            i++;
        }

        return res.substring(0, Math.max(res.length() - 1, 0));
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

            String[] ind = arrays.getFirst().split(",");

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
                        .append(arrays.getFirst())
                        .append("\n");

                i++;

                continue;
            }

            String[] rest = group.replaceAll("\\["+arrays.getFirst()+"]( +?)?,", "").split(",");

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

    private boolean isApk(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-apk"))
                return true;
        }

        return false;
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

    private boolean validateFile(File workspace, int eid) throws Exception {
        File dataLocal = new File(workspace, "DataLocal");
        File imageDataLocal = new File(workspace, "ImageDataLocal");
        File imageLocal = new File(workspace, "ImageLocal");

        if(!dataLocal.exists() || !imageDataLocal.exists() || !imageLocal.exists())
            return false;

        File stat = new File(dataLocal, "t_unit.csv");

        if(!stat.exists())
            return false;

        BufferedReader reader = new BufferedReader(new FileReader(stat, StandardCharsets.UTF_8));

        int i = -2;
        String line;

        boolean found = false;

        while((line = reader.readLine()) != null) {
            if(i == eid && !line.isBlank()) {
                reader.close();
                found = true;

                break;
            } else {
                i++;
            }
        }

        if(!found)
            return false;

        File atkAnim = new File(imageDataLocal, Data.trio(eid)+"_e02.maanim");
        File icon = new File(imageLocal, "enemy_icon_"+Data.trio(eid)+".png");

        return atkAnim.exists() && icon.exists();
    }
}
