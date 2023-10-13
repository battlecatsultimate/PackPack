package mandarin.packpack.commands.data;

import common.pack.UserProfile;
import common.util.Data;
import common.util.unit.Combo;
import common.util.unit.Form;
import common.util.unit.Unit;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.CustomCombo;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComboAnalyzer extends ConstraintCommand {
    public ComboAnalyzer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    List<CustomCombo> combos = new ArrayList<>();

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : " + temp.getAbsolutePath());

            return;
        }

        MessageChannel ch = loader.getChannel();

        String command = loader.getContent();

        int cid = getComboID(command);
        String localeCode = getLocale(command);

        File workspace = new File("./data/bc/" + localeCode + "/workspace");

        if(!workspace.exists()) {
            ch.sendMessage("Couldn't find workspace folder, try to call `p!da [Locale]` first").queue();

            return;
        }

        if(!validateFiles(workspace, cid, localeCode) || combos.isEmpty()) {
            if(cid >= 0) {
                replyToMessageSafely(ch, LangID.getStringByID("comanalyzer_nosuch", lang), loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("comanalyzer_notfound", lang), loader.getMessage(), a -> a);
            }

            return;
        }

        File folder = StaticStore.generateTempFile(temp, "combo", "", true);

        if(folder == null || !folder.exists()) {
            StaticStore.logger.uploadLog("Can't create folder : " + (folder != null ? folder.getAbsolutePath() : "combo folder"));

            return;
        }

        List<File> images = new ArrayList<>();

        for(int i = 0; i < combos.size(); i++) {
            File image = ImageDrawing.drawComboImage(folder, combos.get(i));

            if(image != null)
                images.add(image);
        }

        int i = 0;

        while(i < images.size()) {
            MessageCreateAction action = ch.sendMessage("Analyzed combo image");

            long fileSize = 0;

            while (i < images.size() && fileSize + images.get(i).length() < 8 * 1024 * 1024) {
                action = action.addFiles(FileUpload.fromData(images.get(i)));

                fileSize += images.get(i).length();
                i++;
            }

            int finalIndex = i;

            action.queue(msg -> {
                if (finalIndex >= images.size()) {
                    StaticStore.deleteFile(folder, true);
                }
            });
        }
    }

    private int getComboID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-c") || contents[i].equals("-cid")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return -1;
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

    private boolean validateFiles(File workspace, int cid, String locale) throws Exception {
        File dataLocal = new File(workspace, "DataLocal");
        File resLocal = new File(workspace, "resLocal");
        File unitLocal = new File(workspace, "UnitLocal");

        if(!dataLocal.exists() || !resLocal.exists() || !unitLocal.exists())
            return false;

        if(cid >= 0) {
            CustomCombo combo = generateCombo(dataLocal, resLocal, unitLocal, cid, locale);

            if(combo == null)
                return false;
            else {
                combos.add(combo);

                return true;
            }
        } else {
            File comboData = new File(dataLocal, "NyancomboData.csv");

            if(!comboData.exists())
                return false;

            BufferedReader dataReader = new BufferedReader(new FileReader(comboData, StandardCharsets.UTF_8));

            String line;

            List<Integer> unknownIDs = new ArrayList<>();

            while((line = dataReader.readLine()) != null) {
                String[] data = line.split(",");

                if(data.length <= 2)
                    break;

                if(StaticStore.isNumeric(data[0]) && StaticStore.isNumeric(data[1])) {
                    int id = StaticStore.safeParseInt(data[0]);

                    if(StaticStore.safeParseInt(data[1]) != -1 && !existingCombo(id))
                        unknownIDs.add(id);
                }
            }

            dataReader.close();

            for(int i = 0; i < unknownIDs.size(); i++) {
                CustomCombo combo = generateCombo(dataLocal, resLocal, unitLocal, unknownIDs.get(i), locale);

                if(combo != null) {
                    combos.add(combo);
                }
            }

            return true;
        }
    }

    private CustomCombo generateCombo(File dataLocal, File resLocal, File unitLocal, int cid, String locale) throws Exception {
        File comboData = new File(dataLocal, "NyancomboData.csv");

        if(!comboData.exists())
            return null;

        BufferedReader dataReader = new BufferedReader(new FileReader(comboData, StandardCharsets.UTF_8));

        String line;

        String[] comboLine = null;
        int comboIndex = 0;

        while((line = dataReader.readLine()) != null) {
            String[] data = line.split(",");

            if(data.length == 2) {
                dataReader.close();

                return null;
            }

            if(StaticStore.isNumeric(data[0]) && StaticStore.safeParseInt(data[0]) == cid && StaticStore.isNumeric(data[1]) && StaticStore.safeParseInt(data[1]) != -1) {
                comboLine = data;
                break;
            }

            comboIndex++;
        }

        dataReader.close();

        if(comboLine == null)
            return null;

        String title = null;

        File comboName = new File(resLocal, "Nyancombo_" + getRawLocale(locale) + ".csv");

        if(!comboName.exists())
            title = String.format(LangID.getStringByID("comanalyzer_dummytitle", lang), comboLine[0]);
        else {
            BufferedReader nameReader = new BufferedReader(new FileReader(comboName, StandardCharsets.UTF_8));

            int index = 0;

            while((line = nameReader.readLine()) != null) {
                if(line.isBlank()) {
                    break;
                }

                if(index == comboIndex) {
                    String[] data = line.split(getSeparator(locale));

                    if(data.length < 1) {
                        title = String.format(LangID.getStringByID("comanalyzer_dummytitle", lang), comboLine[0]);
                    } else {
                        title = data[0];
                    }

                    break;
                }

                index++;
            }

            nameReader.close();
        }

        if (title == null) {
            title = String.format(LangID.getStringByID("comanalyzer_dummytitle", lang), comboLine[0]);
        }

        int formNumber = comboLine.length - 5;

        if(comboLine[comboLine.length - 1].isBlank())
            formNumber--;

        if(formNumber % 2 == 1) {
            StaticStore.logger.uploadLog("Invalid combo file format found : " + Arrays.toString(comboLine));

            return null;
        }

        formNumber /= 2;

        List<BufferedImage> icons = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for(int i = 0; i < formNumber; i++) {
            if(!StaticStore.isNumeric(comboLine[2 + i * 2]) || !StaticStore.isNumeric(comboLine[2 + i * 2 + 1]))
                return null;

            int uid = StaticStore.safeParseInt(comboLine[2 + i * 2]);
            int form = StaticStore.safeParseInt(comboLine[2 + i * 2 + 1]);

            if(uid == -1 || form == -1)
                continue;

            if(uid >= UserProfile.getBCData().units.size()) {
                File icon = new File(unitLocal, "uni" + Data.trio(uid) + "_" + getUnitCode(form) + "00.png");

                if(!icon.exists())
                    return null;

                icons.add(ImageIO.read(icon).getSubimage(9, 21, 110, 85));

                names.add(grabUnitNameFromFile(resLocal, uid, form, locale));
            } else {
                Unit u = UserProfile.getBCData().units.get(uid);

                if(u == null || form >= u.forms.length) {
                    File icon = new File(unitLocal, "uni" + Data.trio(uid) + "_" + getUnitCode(form) + "00.png");

                    if(!icon.exists())
                        return null;

                    icons.add(ImageIO.read(icon).getSubimage(9, 21, 110, 85));

                    names.add(grabUnitNameFromFile(resLocal, uid, form, locale));
                } else {
                    Form f = u.forms[form];

                    f.anim.load();

                    BufferedImage icon = (BufferedImage) f.anim.getUni().getImg().bimg();

                    if(icon == null)
                        return null;

                    icons.add(icon);

                    String name = StaticStore.safeMultiLangGet(f, lang);

                    if(name == null || name.isBlank())
                        name = f.names.toString();

                    if(name.isBlank())
                        name = Data.trio(uid) + " - " + Data.trio(form);

                    names.add(name);

                    f.anim.unload();
                }
            }
        }

        if(icons.isEmpty() || names.isEmpty())
            return null;

        int comboType;

        if(StaticStore.isNumeric(comboLine[2 + formNumber * 2]))
            comboType = StaticStore.safeParseInt(comboLine[2 + formNumber * 2]);
        else
            return null;

        String type = null;

        boolean unknownType = false;

        try {
            type = DataToString.getComboType(comboType, lang);
        } catch (IllegalStateException ignored) {
            unknownType = true;

            File comboTypeName = new File(resLocal, "Nyancombo1_" + getRawLocale(locale) + ".csv");

            if(!comboTypeName.exists())
                return null;

            BufferedReader typeReader = new BufferedReader(new FileReader(comboTypeName, StandardCharsets.UTF_8));

            int index = 0;

            while((line = typeReader.readLine()) != null) {
                if(line.isBlank()) {
                    typeReader.close();

                    return null;
                }

                if(index == comboIndex) {
                    String[] data = line.split(getSeparator(locale));

                    if(data.length < 1) {
                        typeReader.close();

                        return null;
                    }

                    type = data[0];

                    break;
                }

                index++;
            }

            typeReader.close();
        }

        if(type == null)
            return null;

        int comboLevel;

        if(StaticStore.isNumeric(comboLine[2 + formNumber * 2 + 1]))
            comboLevel = StaticStore.safeParseInt(comboLine[2 + formNumber * 2 + 1]);
        else
            return null;

        String level = DataToString.getComboLevel(comboLevel, lang);

        String desc = null;

        if(unknownType) {
            File descFile = new File(resLocal, "Nyancombo2_" + getRawLocale(locale) + ".csv");

            if(!descFile.exists())
                return null;

            BufferedReader descReader = new BufferedReader(new FileReader(descFile, StandardCharsets.UTF_8));

            int index = 0;

            while((line = descReader.readLine()) != null) {
                if(line.isBlank()) {
                    break;
                }

                if(index == comboLevel) {
                    String[] data = line.split(getSeparator(locale));

                    if(data.length < 1) {
                        desc = type + " Lv. " + (comboLevel + 1);
                    } else {
                        desc = type + " " + data[0];
                    }

                    break;
                }

                index++;
            }

            descReader.close();
        } else {
            desc = DataToString.getComboDescription(comboType, comboLevel, lang);
        }

        if(desc == null)
            desc = type + " Lv. " + (comboLevel + 1);

        return new CustomCombo(title, desc, icons, names, type, level);
    }

    private String grabUnitNameFromFile(File resLocal, int uid, int form, String locale) throws Exception {
        String name = null;

        File explanation = new File(resLocal, "Unit_Explanation" + (uid + 1) + "_" + getRawLocale(locale) + ".csv");

        if(!explanation.exists())
            name = Data.trio(uid) + " - " + Data.trio(form);
        else {
            BufferedReader explanationReader = new BufferedReader(new FileReader(explanation, StandardCharsets.UTF_8));

            int index = 0;
            String line;

            while((line = explanationReader.readLine()) != null) {
                if(line.isBlank()) {
                    break;
                }

                if(index == form) {
                    String[] data = line.split(getSeparator(locale));

                    if(data.length < 1) {
                        name = Data.trio(uid) + " - " + Data.trio(form);
                    } else {
                        name = data[0];
                    }

                    break;
                }

                index++;
            }

            explanationReader.close();

            if(name == null)
                name = Data.trio(uid) + " - " + Data.trio(form);
        }

        return name;
    }

    private boolean existingCombo(int cid) {
        for(Combo c : UserProfile.getBCData().combos) {
            if(StaticStore.isNumeric(c.name) && StaticStore.safeParseInt(c.name) == cid && c.show != -1) {
                return true;
            }
        }

        return false;
    }

    private String getRawLocale(String locale) {
        return switch (locale) {
            case "en" -> "en";
            case "jp" -> "ja";
            case "kr" -> "ko";
            case "zh" -> "tw";
            default ->
                    throw new IllegalStateException("E/ComboAnalyzer::getRawLocale - Unknown locale provided : " + locale);
        };
    }

    private static String getUnitCode(int ind) {
        return switch (ind) {
            case 0 -> "f";
            case 1 -> "c";
            case 2 -> "s";
            default -> String.valueOf(ind);
        };
    }

    private static String getSeparator(String locale) {
        if(locale.equals("jp"))
            return ",";
        else
            return "\\|";
    }
}
