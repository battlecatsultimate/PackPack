package mandarin.packpack.commands.data;

import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.CustomMaskUnit;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.cell.AbilityData;
import mandarin.packpack.supporter.bc.cell.CellData;
import mandarin.packpack.supporter.bc.cell.FlagCellData;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.message.StatAnalyzerMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatAnalyzer extends ConstraintCommand {
    private static final List<String> allParameters = List.of(
            "-s", "-second", "-lv", "-uid", "-u", "-n", "-name", "-l", "-len", "-p", "-proc", "-a", "-ability", "-t", 
            "-trait", "-c", "-cell", "-apk", "-en", "-jp", "-tw", "-kr"
    );

    public StatAnalyzer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
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
        Message author = loader.getMessage();

        String command = loader.getContent();

        int uid = getUID(command);

        if(uid == -1) {
            ch.sendMessage(LangID.getStringByID("stat_uid", lang)).queue();

            return;
        }

        int level = getLevel(command);
        int len = getLen(command);
        boolean isSecond = isSecond(command);
        boolean isApk = isApk(command);
        String[] name = getName(command);

        if(name == null) {
            name = new String[len];

            for(int i = 0; i < name.length; i++) {
                name[i] = Data.trio(uid)+" - "+Data.trio(i);
            }
        } else if(name.length != len) {
            int nLen = name.length;

            ch.sendMessage(LangID.getStringByID("stat_name", lang).replace("_RRR_", String.valueOf(len)).replace("_PPP_", String.valueOf(nLen))).queue();

            return;
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

            if(!validateFile(workspace, uid, len)) {
                ch.sendMessage("Couldn't find sufficient data for unit code : "+uid).queue();

                return;
            }

            File dataLocal = new File(workspace, "DataLocal");
            File imageDataLocal = new File(workspace, "ImageDataLocal");
            File unitLocal = new File(workspace, "UnitLocal");
            File imageLocal = new File(workspace, "ImageLocal");

            CustomMaskUnit[] data = new CustomMaskUnit[len];

            String[] code = {"f", "c", "s"};

            File statFile = new File(dataLocal, "unit"+Data.trio(uid+1)+".csv");
            File levelFile = new File(dataLocal, "unitlevel.csv");
            File buyFile = new File(dataLocal, "unitbuy.csv");

            int[] egg = getEggData(buyFile, uid);
            int[][] trueForm = getTrueForm(buyFile, uid);

            BufferedReader statReader = new BufferedReader(new FileReader(statFile, StandardCharsets.UTF_8));
            BufferedReader levelReader = new BufferedReader(new FileReader(levelFile, StandardCharsets.UTF_8));
            BufferedReader buyReader = new BufferedReader(new FileReader(buyFile, StandardCharsets.UTF_8));

            int count = 0;

            while(count < uid) {
                levelReader.readLine();
                buyReader.readLine();
                count++;
            }

            String[] curve = levelReader.readLine().split(",");
            String[] rare = buyReader.readLine().split(",");

            levelReader.close();
            buyReader.close();

            System.gc();

            for(int i = 0; i < data.length; i++) {
                File maanim;

                if(egg != null && i < egg.length && egg[i] != -1) {
                    maanim = new File(imageDataLocal, Data.trio(egg[i])+"_m02.maanim");
                } else {
                    maanim = new File(imageDataLocal, Data.trio(uid)+"_"+code[i]+"02.maanim");
                }

                VFile anim = VFile.getFile(maanim);

                if(anim == null) {
                    ch.sendMessage("Something went wrong while analyzing maanim data").queue();

                    statReader.close();

                    return;
                }

                MaAnim ma = MaAnim.newIns(anim.getData());

                data[i] = new CustomMaskUnit(statReader.readLine().split(","), curve, ma, rare);
            }

            statReader.close();

            EntityHandler.generateStatImage(ch, cellData, procData, abilData, traitData, data, name, unitLocal, imageLocal, level, !isSecond, egg, trueForm, false, uid, lang);

        } else {
            List<String> requiredFiles = new ArrayList<>();

            requiredFiles.add("unit"+Data.trio(uid + 1)+".csv");
            requiredFiles.add("unitlevel.csv");
            requiredFiles.add("unitbuy.csv");

            StringBuilder sb = new StringBuilder("- Required File List -\n\n");

            for(int i = 0; i < requiredFiles.size(); i++) {
                sb.append(requiredFiles.get(i))
                        .append(" : Ready");

                if(i < requiredFiles.size() - 1) {
                    sb.append("\n");
                }
            }

            String[] finalName = name;

            ch.sendMessage(sb.toString()).queue(msg -> {
                if(msg == null)
                    return;

                new StatAnalyzerMessageHolder(msg, author, uid, len, isSecond, cellData, procData, abilData, traitData, ch.getId(), container, level, finalName, lang, requiredFiles);
            });
        }
    }

    private int getUID(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-u") || contents[i].equals("-uid")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return -1;
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

    private int getLen(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-l") || contents[i].equals("-len")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return Math.min(3, Math.max(1, StaticStore.safeParseInt(contents[i + 1])));
            }
        }

        return 1;
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

            String[] ind = arrays.get(0).split(",");
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
                        .append(arrays.get(0))
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

            String[] rest = group.replace("["+arrays.get(0)+"]", "").split("\\["+arrays.get(1)+"]");

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

    private boolean validateFile(File workspace, int uID, int len) throws Exception {
        File dataLocal = new File(workspace, "DataLocal");
        File imageDataLocal = new File(workspace, "ImageDataLocal");
        File unitLocal = new File(workspace, "UnitLocal");
        File imageLocal = new File(workspace, "ImageLocal");

        if(!dataLocal.exists() || !imageDataLocal.exists() || !unitLocal.exists() || !imageLocal.exists())
            return false;

        File buy = new File(dataLocal, "unitbuy.csv");

        if(!buy.exists())
            return false;

        String[] codes = {"f", "c", "s"};
        int[] egg = getEggData(new File(dataLocal, "unitbuy.csv"), uID);
        int[][] trueForm = getTrueForm(new File(dataLocal, "unitbuy.csv"), uID);

        for(int i = 0; i < len; i++) {
            File atkMaanim;
            File icon;

            if(egg != null && i < egg.length && egg[i] != -1) {
                atkMaanim = new File(imageDataLocal, Data.trio(egg[i])+"_m02.maanim");
                icon = new File(unitLocal, "uni"+Data.trio(egg[i])+"_m"+Data.duo(i)+".png");
            } else {
                atkMaanim = new File(imageDataLocal, Data.trio(uID)+"_"+codes[i]+"02.maanim");
                icon = new File(unitLocal, "uni"+Data.trio(uID)+"_"+codes[i]+"00.png");
            }

            if(!atkMaanim.exists() || !icon.exists())
                return false;
        }

        if(trueForm != null) {
            for(int i = 0; i < trueForm.length; i++) {
                if(trueForm[i][0] == -1)
                    continue;

                File icon = new File(imageLocal, "gatyaitemD_"+trueForm[i][0]+"_f.png");

                if(!icon.exists()) {
                    VFile vf = VFile.get("./org/page/catfruit/gatyaitemD_"+trueForm[i][0]+"_f.png");

                    if(vf == null) {
                        return false;
                    }
                }
            }
        }

        File stat = new File(dataLocal, "unit"+Data.trio((uID+1))+".csv");

        if(!stat.exists())
            return false;

        File level = new File(dataLocal, "unitlevel.csv");

        return level.exists();
    }

    private int[] getEggData(File unitBuy, int uID) throws Exception {
        if(!unitBuy.exists())
            return null;

        BufferedReader reader = new BufferedReader(new FileReader(unitBuy, StandardCharsets.UTF_8));

        int count = 0;
        String line;

        while((line = reader.readLine()) != null) {
            if(count == uID && !line.isBlank()) {
                reader.close();

                String[] data = line.split(",");

                int firstEgg = StaticStore.safeParseInt(data[data.length - 2]);
                int secondEgg = StaticStore.safeParseInt(data[data.length - 1]);

                if(firstEgg != -1 || secondEgg != -1)
                    return new int[] {firstEgg, secondEgg};

                return null;
            }

            count++;
        }

        reader.close();

        return null;
    }

    private int[][] getTrueForm(File unitBuy, int uID) throws Exception {
        if(!unitBuy.exists())
            return null;

        BufferedReader reader = new BufferedReader(new FileReader(unitBuy, StandardCharsets.UTF_8));

        int count = 0;
        String line;

        while((line = reader.readLine()) != null) {
            if(count == uID && !line.isBlank()) {
                reader.close();

                String[] data = line.split(",");

                if(StaticStore.safeParseInt(data[25]) != 0) {
                    int len = 0;

                    while(len != 6 && StaticStore.safeParseInt(data[25 + 2 * len + 2]) != 0) {
                        len++;
                    }

                    int[][] trueForm = new int[len][2];

                    for(int i = 0; i < len; i++) {
                        //Unnecessary calculation is for organizing stuffs

                        trueForm[i][0] = StaticStore.safeParseInt(data[25 + 2 * i + 1]);
                        trueForm[i][1] = StaticStore.safeParseInt(data[25 + 2 * i + 2]);
                    }

                    return trueForm;
                }

                return null;
            }

            count++;
        }

        reader.close();

        return null;
    }
}
