package mandarin.packpack.supporter.server.holder;

import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.CustomMaskUnit;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.cell.AbilityData;
import mandarin.packpack.supporter.bc.cell.CellData;
import mandarin.packpack.supporter.bc.cell.FlagCellData;
import net.dv8tion.jda.api.entities.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class StatAnalyzerMessageHolder extends FileAnalyzerHolder {
    private final List<CellData> cellData;
    private final List<AbilityData> procData;
    private final List<FlagCellData> abilityData;
    private final List<FlagCellData> traitData;
    private final int uID;
    private final int len;
    private final boolean isSecond;
    private int[] egg = null;
    private int[][] trueForm = null;
    private final int lv;
    private final String[] name;

    public StatAnalyzerMessageHolder(Message msg, Message author, int uID, int len, boolean isSecond, List<CellData> cellData, List<AbilityData> procData, List<FlagCellData> abilityData, List<FlagCellData> traitData, String channelID, File container, int lv, String[] name, int lang, List<String> requiredFiles) {
        super(msg, author, channelID, container, requiredFiles, lang);

        this.cellData = cellData;
        this.abilityData = abilityData;
        this.procData = procData;
        this.traitData = traitData;
        this.uID = uID;
        this.len = len;
        this.isSecond = isSecond;
        this.lv = lv;
        this.name = name;
    }

    @Override
    public void perform(Map<String, File> fileMap) throws Exception {
        CustomMaskUnit[] units = new CustomMaskUnit[len];

        File statFile = fileMap.get("unit"+Data.trio(uID+1)+".csv");
        File levelFile = fileMap.get("unitlevel.csv");
        File buyFile = fileMap.get("unitbuy.csv");

        if(!statFile.exists()) {
            StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Stat file isn't existing even though code finished validation : "+statFile.getAbsolutePath());

            return;
        }

        if(!levelFile.exists()) {
            StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Level curve file isn't existing even though code finished validation : "+levelFile.getAbsolutePath());

            return;
        }

        if(!buyFile.exists()) {
            StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Unit buy file isn't existing even though code finished validation : "+buyFile.getAbsolutePath());

            return;
        }

        BufferedReader statReader = new BufferedReader(new FileReader(statFile, StandardCharsets.UTF_8));
        BufferedReader levelReader = new BufferedReader(new FileReader(levelFile, StandardCharsets.UTF_8));
        BufferedReader buyReader = new BufferedReader(new FileReader(buyFile, StandardCharsets.UTF_8));

        int count = 0;

        while(count < uID) {
            levelReader.readLine();
            buyReader.readLine();
            count++;
        }

        String[] curve = levelReader.readLine().split(",");
        String[] rare = buyReader.readLine().split(",");

        levelReader.close();
        buyReader.close();

        System.gc();

        for(int i = 0; i < units.length; i++) {
            File maanim = fileMap.get(getMaanimFileName(i));

            if(!maanim.exists()) {
                StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Maanim file isn't existing even though code finished validation : "+maanim.getAbsolutePath());
                statReader.close();

                return;
            }

            VFile vf = VFile.getFile(maanim);

            if(vf == null) {
                StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Failed to generate vFile of maanim : "+maanim.getAbsolutePath());
                statReader.close();

                return;
            }

            MaAnim ma = MaAnim.newIns(vf.getData());

            units[i] = new CustomMaskUnit(statReader.readLine().split(","), curve, ma, rare);
        }

        statReader.close();

        EntityHandler.generateStatImage(msg.getChannel(), cellData, procData, abilityData, traitData, units, name, container, lv, !isSecond, egg, trueForm, uID, lang);
    }

    @Override
    public boolean hasValidFileFormat(File result) throws Exception {
        String name = result.getName();

        if(name.endsWith(".png") || name.endsWith(".maanim"))
            return super.hasValidFileFormat(result);
        else if(name.equals("unit"+Data.trio(uID+1)+".csv")) {
            BufferedReader reader = new BufferedReader(new FileReader(result, StandardCharsets.UTF_8));

            int count = 0;

            while(reader.readLine() != null) {
                count++;
            }

            reader.close();

            return count >= 3 && count < 5;
        } else if(name.equals("unitlevel.csv")) {
            BufferedReader reader = new BufferedReader(new FileReader(result, StandardCharsets.UTF_8));

            int count = 0;
            String line;

            while((line = reader.readLine()) != null) {
                count++;

                if(count == uID && !line.isBlank()) {
                    reader.close();

                    return true;
                }
            }

            reader.close();
        } else if(name.equals("unitbuy.csv")) {
            BufferedReader reader = new BufferedReader(new FileReader(result, StandardCharsets.UTF_8));

            int count = 0;
            String line;

            while((line = reader.readLine()) != null) {
                if(count == uID && !line.isBlank()) {
                    reader.close();

                    String[] data = line.split(",");

                    int firstEgg = StaticStore.safeParseInt(data[data.length - 2]);
                    int secondEgg = StaticStore.safeParseInt(data[data.length - 1]);

                    if(firstEgg != -1)
                        registerMoreFile(Data.trio(firstEgg)+"_m02.maanim");
                    else
                        registerMoreFile(Data.trio(uID)+"_"+getUnitCode(0)+"02.maanim");

                    if(secondEgg != -1)
                        registerMoreFile(Data.trio(secondEgg)+"_m02.maanim");
                    else
                        registerMoreFile(Data.trio(uID)+"_"+getUnitCode(1)+"02.maanim");

                    for(int i = 2; i < len; i++) {
                        registerMoreFile(Data.trio(uID)+"_"+getUnitCode(i)+"02.maanim");
                    }

                    if(firstEgg != -1)
                        registerMoreFile("uni"+Data.trio(firstEgg)+"_m00.png");
                    else
                        registerMoreFile("uni"+Data.trio(uID)+"_"+getUnitCode(0)+"00.png");

                    if(secondEgg != -1)
                        registerMoreFile("uni"+Data.trio(secondEgg)+"_m01.png");
                    else
                        registerMoreFile("uni"+Data.trio(uID)+"_"+getUnitCode(1)+"00.png");

                    for(int i = 2; i < len; i++) {
                        registerMoreFile("uni"+Data.trio(uID)+"_"+getUnitCode(i)+"00.png");
                    }

                    if(firstEgg != -1 || secondEgg != -1)
                        egg = new int[] {firstEgg, secondEgg};

                    int len = 0;

                    while(len != 6 && StaticStore.safeParseInt(data[25 + 1 + 2 * len + 1]) != 0) {
                        len++;
                    }

                    trueForm = new int[len][2];

                    for(int i = 0; i < len; i++) {
                        //Unnecessary calculation is for organizing stuffs

                        int id = StaticStore.safeParseInt(data[25 + 2 * i + 1]);

                        trueForm[i][0] = id;
                        trueForm[i][1] = StaticStore.safeParseInt(data[25 + 2 * i + 2]);

                        if(id != -1) {
                            String catFruitName = "gatyaitemD_"+id+"_f.png";
                            VFile vf = VFile.get("./org/page/catfruit/"+catFruitName);

                            if(vf == null) {
                                registerMoreFile(catFruitName);
                            }
                        }
                    }

                    return true;
                }

                count++;
            }

            reader.close();
        }

        return false;
    }


    private String getMaanimFileName(int ind) {
        if(egg != null && ind < egg.length) {
            return Data.trio(egg[ind]) + "_m02.maanim";
        }

        switch (ind) {
            case 0:
                return Data.trio(uID)+"_f02.maanim";
            case 1:
                return Data.trio(uID)+"_c02.maanim";
            case 2:
                return Data.trio(uID)+"_s02.maanim";
            default:
                return Data.trio(uID)+"_"+ind+"02.maanim";
        }
    }

    private String getUnitCode(int ind) {
        switch (ind) {
            case 0:
                return "f";
            case 1:
                return "c";
            default:
                return "s";
        }
    }
}
