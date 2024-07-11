package mandarin.packpack.commands.data;

import common.CommonStatic;
import common.battle.data.MaskUnit;
import common.pack.UserProfile;
import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import common.util.unit.Trait;
import common.util.unit.Unit;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.*;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public class TalentAnalyzer extends ConstraintCommand {
    private static final List<String> allParameters = List.of(
            "-s", "-second", "-uid", "-u", "-n", "-name", "-en", "-jp", "-tw", "-kr"
    );

    public TalentAnalyzer(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    public CustomTalent talent;
    private MaskUnit unit;

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : " + temp.getAbsolutePath());
            return;
        }

        MessageChannel ch = loader.getChannel();

        String command = loader.getContent();

        int uid = getUnitID(command);

        if(uid == -1) {
            ch.sendMessage(LangID.getStringByID("talanalyzer_uid", lang)).queue();
            return;
        }

        boolean isSecond = isSecond(command);

        String localeCode = getLocale(command);

        File workspace = new File("./data/bc/" + localeCode + "/workspace");

        if(!workspace.exists()) {
            ch.sendMessage("Couldn't find workspace folder, try to call `p!da [Locale]` first").queue();

            return;
        }

        if(!validateFile(workspace, uid, !isSecond)) {
            ch.sendMessage("Couldn't find sufficient data for unit : " + Data.trio(uid)).queue();

            return;
        }

        String name = getName(command);

        if(name == null)
            name = Data.trio(uid) + " - 002";

        name = String.format(LangID.getStringByID("talanalyzer_title", lang), name);

        String type;

        if(uid >= UserProfile.getBCData().units.size()) {
            type = DataToString.getRarity(((CustomMaskUnit) unit).rarity, lang);
        } else {
            Unit u = UserProfile.getBCData().units.get(uid);

            type = DataToString.getRarity(u.rarity, lang);
        }

        File talentImage = ImageDrawing.drawTalentImage(name, type, talent, lang);

        if(talentImage == null) {
            replyToMessageSafely(ch, LangID.getStringByID("talanalyzer_fail", lang), loader.getMessage(), a -> a);
        } else {
            sendMessageWithFile(ch, LangID.getStringByID("talanalyzer_success", lang), talentImage, loader.getMessage());
        }
    }

    private int getUnitID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-u") && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return -1;
    }

    private boolean isSecond(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-s") || contents[i].equals("-second"))
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

    private boolean validateFile(File workspace, int uid, boolean isFrame) throws Exception {
        FakeImage unitIcon;

        if(uid >= UserProfile.getBCData().units.size() || UserProfile.getBCData().units.get(uid).forms.length < 3) {
            File imageLocal = new File(workspace, "UnitLocal");

            if(!imageLocal.exists())
                return false;

            File icon = new File(imageLocal, "uni" + Data.trio(uid) + "_s00.png");

            if(!icon.exists())
                return false;

            unitIcon = ImageBuilder.builder.build(icon).getSubimage(9, 21, 110, 85);
        } else {
            Unit u = UserProfile.getBCData().units.get(uid);

            if(u == null)
                return false;

            if(u.forms.length < 3) {
                return false;
            }

            u.forms[2].anim.load();

            unitIcon = u.forms[2].anim.getUni().getImg().cloneImage();

            u.forms[2].anim.unload();
        }

        File dataLocal = new File(workspace, "DataLocal");
        File resLocal = new File(workspace, "resLocal");

        if(!dataLocal.exists() || !resLocal.exists())
            return false;

        File acquisition = new File(dataLocal, "SkillAcquisition.csv");

        if(!acquisition.exists())
            return false;

        BufferedReader skillReader = new BufferedReader(new FileReader(acquisition, StandardCharsets.UTF_8));

        skillReader.readLine();

        String line;
        String[] talentLine = null;

        boolean talentExist = false;

        while((line = skillReader.readLine()) != null && !line.isBlank()) {
            String[] data = line.split(",");

            if(StaticStore.isNumeric(data[0]) && StaticStore.safeParseInt(data[0]) == uid) {
                talentExist = true;
                talentLine = data;
                break;
            }
        }

        skillReader.close();

        if(!talentExist)
            return false;

        if(uid >= UserProfile.getBCData().units.size() || UserProfile.getBCData().units.get(uid).forms.length < 3) {
            generateCustomMaskUnit(workspace, uid);

            if(unit == null)
                return false;

            talent = new CustomTalent(talentLine, unit, unitIcon);
        } else {
            Unit u = UserProfile.getBCData().units.get(uid);

            unit = u.forms[2].du;

            talent = new CustomTalent(talentLine, u.forms[2].du, unitIcon);
        }

        if(StaticStore.isNumeric(talentLine[1])) {
            int traitID = StaticStore.safeParseInt(talentLine[1]);

            if(traitID != 0 && traitID < Data.TB_EVA) {
                List<Trait> traits = Trait.convertType(traitID);

                if(traits.size() == 1) {
                    talent.traitIcon = CommonStatic.getBCAssets().icon[3][traits.getFirst().id.id].getImg().cloneImage();
                }
            }
        }

        int talentSet = (talentLine.length - 2) / 14;

        for(int i = 0; i < talentSet; i++) {
            String desc = null;
            String title = null;

            int abilityID = StaticStore.safeParseInt(talentLine[2 + i * 14]);

            if(abilityID == 0)
                continue;

            if(abilityID >= Data.PC_CORRES.length || Data.PC_CORRES[abilityID][0] == -1) {
                File description = new File(resLocal, "SkillDescriptions.csv");

                if(!description.exists()) {
                    return false;
                }

                if(!StaticStore.isNumeric(talentLine[2 + i * 14 + 10]))
                    return false;

                int textID = StaticStore.safeParseInt(talentLine[2 + i * 14 + 10]);

                BufferedReader descReader = new BufferedReader(new FileReader(description, StandardCharsets.UTF_8));

                descReader.readLine();

                boolean textFound = false;

                while((line = descReader.readLine()) != null) {
                    String[] data = line.split(",");

                    if(StaticStore.isNumeric(data[0]) && StaticStore.safeParseInt(data[0]) == textID) {
                        textFound = true;

                        title = String.format(LangID.getStringByID("talanalyzer_dummy", lang), abilityID);

                        if(StaticStore.isNumeric(talentLine[2 + i * 14 + 1])) {
                            int maxLevel = StaticStore.safeParseInt(talentLine[2 + i * 14 + 1]);

                            if(maxLevel >= 2) {
                                title += " [1 ~ " + maxLevel + "]";
                            }
                        }

                        desc = data[1];

                        break;
                    }
                }

                descReader.close();

                if(!textFound)
                    return false;
            } else {
                title = DataToString.getTalentTitle(talentLine, i, lang);
                desc = DataToString.getTalentExplanation(talentLine, unit, i, isFrame, lang);
            }

            int[] levels = null;

            int levelID = StaticStore.safeParseInt(talentLine[2 + i * 14 + 11]);

            if(!DataToString.talentLevel.containsKey(levelID)) {
                File curve = new File(dataLocal, "SkillLevel.csv");

                if(!curve.exists())
                    return false;

                BufferedReader curveReader = new BufferedReader(new FileReader(curve));

                curveReader.readLine();

                while((line = curveReader.readLine()) != null && !line.isBlank()) {
                    String[] data = line.split(",");

                    if(StaticStore.isNumeric(data[0]) && StaticStore.safeParseInt(data[0]) == levelID) {
                        levels = new int[data.length - 1];

                        for(int j = 1; j < data.length; j++) {
                            if(StaticStore.isNumeric(data[j])) {
                                levels[j - 1] = StaticStore.safeParseInt(data[j]);
                            }
                        }

                        break;
                    }
                }

                curveReader.close();

                if(levels == null)
                    return false;
            } else {
                levels = DataToString.talentLevel.get(levelID);
            }

            List<Integer> levelCurve = new ArrayList<>();

            for(int j = 0; j < levels.length; j++) {
                levelCurve.add(levels[j]);
            }

            if(title == null || desc == null)
                return false;

            boolean immunity = false;

            if(desc.endsWith("<IMU>")) {
                immunity = true;
                desc = desc.replace("<IMU>", "").trim();
            }

            talent.talents.add(new TalentData(levelCurve, title, desc, grabTalentIcon(abilityID, immunity)));
        }

        return true;
    }

    private FakeImage grabTalentIcon(int abilityID, boolean immunity) throws Exception {
        if(abilityID >= Data.PC_CORRES.length || Data.PC_CORRES[abilityID][0] == -1) {
            File dummyIcon = new File("./data/bot/icons/unknownAbility.png");

            if(!dummyIcon.exists()) {
                throw new IllegalStateException("E/TalentAnalyzer::grabTalentIcon - Dummy ability icon not found");
            }

            return ImageBuilder.builder.build(dummyIcon);
        } else {
            int[] type = Data.PC_CORRES[abilityID];

            switch (type[0]) {
                case 0 -> {
                    return CommonStatic.getBCAssets().icon[1][type[1]].getImg().cloneImage();
                }
                case 1 -> {
                    return CommonStatic.getBCAssets().icon[0][(int) (Math.log(type[1]) / Math.log(2))].getImg().cloneImage();
                }
                case 2 -> {
                    return CommonStatic.getBCAssets().icon[4][type[1]].getImg().cloneImage();
                }
                case 3 -> {
                    if (immunity) {
                        return CommonStatic.getBCAssets().icon[1][type[1]].getImg().cloneImage();
                    } else {
                        return DataToString.resistantIcon.getOrDefault(type[1], null).cloneImage();
                    }
                }
                case 4 -> {
                    return CommonStatic.getBCAssets().icon[3][type[1]].getImg().cloneImage();
                }
                default ->
                        throw new IllegalStateException("E/TalentAnalyzer::grabTalentIcon - Invalid talent type ID : " + type[0]);
            }
        }
    }

    private void generateCustomMaskUnit(File workspace, int uid) throws Exception {
        if(!validateUnitFile(workspace, uid)) {
            return;
        }

        File dataLocal = new File(workspace, "DataLocal");
        File imageDataLocal = new File(workspace, "ImageDataLocal");

        File statFile = new File(dataLocal, "unit"+Data.trio(uid+1)+".csv");
        File levelFile = new File(dataLocal, "unitlevel.csv");
        File buyFile = new File(dataLocal, "unitbuy.csv");

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

        File maanim = new File(imageDataLocal, Data.trio(uid)+"_s02.maanim");

        VFile anim = VFile.getFile(maanim);

        if(anim == null) {
            statReader.close();

            return;
        }

        MaAnim ma = MaAnim.newIns(anim.getData());

        statReader.readLine();
        statReader.readLine();

        unit = new CustomMaskUnit(statReader.readLine().split(","), curve, ma, rare);

        statReader.close();
    }

    private boolean validateUnitFile(File workspace, int uID) throws Exception {
        File dataLocal = new File(workspace, "DataLocal");
        File imageDataLocal = new File(workspace, "ImageDataLocal");
        File unitLocal = new File(workspace, "UnitLocal");
        File imageLocal = new File(workspace, "ImageLocal");

        if(!dataLocal.exists() || !imageDataLocal.exists() || !unitLocal.exists() || !imageLocal.exists())
            return false;

        File buy = new File(dataLocal, "unitbuy.csv");

        if(!buy.exists())
            return false;

        int[][] trueForm = getTrueForm(new File(dataLocal, "unitbuy.csv"), uID);

        File atkMaanim = new File(imageDataLocal, Data.trio(uID)+"_s02.maanim");
        File uni = new File(unitLocal, "uni"+Data.trio(uID)+"_s00.png");

        if(!atkMaanim.exists() || !uni.exists())
            return false;

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

    private boolean abortAppending(String content, String... exception) {
        for(int i = 0; i < exception.length; i++) {
            if(content.equals(exception[i]))
                return false;
        }

        return allParameters.contains(content);
    }
}
