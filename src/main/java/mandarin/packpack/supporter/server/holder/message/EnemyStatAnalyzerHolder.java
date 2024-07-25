package mandarin.packpack.supporter.server.holder.message;

import common.CommonStatic;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import mandarin.packpack.supporter.bc.CustomMaskEnemy;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.cell.AbilityData;
import mandarin.packpack.supporter.bc.cell.CellData;
import mandarin.packpack.supporter.bc.cell.FlagCellData;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class EnemyStatAnalyzerHolder extends FileAnalyzerHolder {
    private final List<CellData> cellData;
    private final List<AbilityData> procData;
    private final List<FlagCellData> abilityData;
    private final List<FlagCellData> traitData;
    private final int eID;
    private final boolean isSecond;
    private final int m;
    private final String name;

    public EnemyStatAnalyzerHolder(@Nonnull Message msg, @Nonnull Message author, String channelID, File container, List<String> requiredFiles, List<CellData> cellData, List<AbilityData> procData, List<FlagCellData> abilityData, List<FlagCellData> traitData, int eID, boolean isSecond, int m, String name, CommonStatic.Lang.Locale lang) {
        super(msg, author, channelID, container, requiredFiles, lang);

        this.cellData = cellData;
        this.procData = procData;
        this.abilityData = abilityData;
        this.traitData = traitData;
        this.eID = eID;
        this.isSecond = isSecond;
        this.m = m;
        this.name = name;
    }

    @Override
    public void perform(Map<String, File> fileMap) throws Exception {
        File statFile = fileMap.get("t_unit.csv");

        BufferedReader statReader = new BufferedReader(new FileReader(statFile, StandardCharsets.UTF_8));

        int count = -2;

        while(count < eID) {
            statReader.readLine();
            count++;
        }

        File maanim = fileMap.get(Data.trio(eID)+"_e02.maanim");

        VFile anim = VFile.getFile(maanim);

        if(anim == null) {
            msg.getChannel().sendMessage("Something went wrong while analyzing maanim data").queue();

            statReader.close();

            return;
        }

        MaAnim ma = MaAnim.newIns(anim.getData(), false);

        CustomMaskEnemy data = new CustomMaskEnemy(statReader.readLine().split(","), ma);

        statReader.close();

        EntityHandler.generateEnemyStatImage(msg.getChannel(), cellData, procData, abilityData, traitData, data, name, container, m, !isSecond, eID, lang);
    }

    @Override
    public boolean hasValidFileFormat(File result) throws Exception {
        String name = result.getName();

        if(name.endsWith(".png") || name.endsWith("maanim"))
            return super.hasValidFileFormat(result);
        else if(name.equals("t_unit.csv")) {
            BufferedReader reader = new BufferedReader(new FileReader(result));

            int count = -2;

            while(reader.readLine() != null) {
                if(count == eID) {
                    reader.close();

                    return true;
                } else {
                    count++;
                }
            }

            reader.close();
        }

        return false;
    }
}
