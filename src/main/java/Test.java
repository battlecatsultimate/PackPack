import common.CommonStatic;
import common.system.P;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import mandarin.packpack.supporter.AssetDownloader;
import mandarin.packpack.supporter.PackContext;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.bc.*;
import mandarin.packpack.supporter.bc.cell.*;
import mandarin.packpack.supporter.lang.LangID;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    private static Font titleFont;
    private static Font typeFont;
    private static Font nameFont;
    private static Font contentFont;
    private static Font levelFont;

    private static final int statPanelMargin = 120;
    private static final int bgMargin = 80;
    private static final int nameMargin = 80;
    private static final int cornerRadius = 150;
    private static final int typeUpDownMargin = 28;
    private static final int typeLeftRightMargin = 66;
    private static final int levelMargin = 36;
    private static final int cellMargin = 110;

    static {
        File regular = new File("./data/NotoRegular.otf");
        File medium = new File("./data/NotoMedium.otf");

        try {
            titleFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(144f);
            typeFont = Font.createFont(Font.TRUETYPE_FONT, regular).deriveFont(96f);
            nameFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(63f);
            contentFont = Font.createFont(Font.TRUETYPE_FONT, regular).deriveFont(84f);
            levelFont = Font.createFont(Font.TRUETYPE_FONT, medium).deriveFont(96f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String command = "p!statanalyzer -s -uid 655 -len 2 -lv 50 -proc {[1,2,3], _0_% chance to do _1_ for _2_, [false, false, true], 4}, {[4, 5], _0_% chance to do other thing for _1_, [false, true], 5} -ability {25, New flag}, {26, Second new passive ability} -trait {27, New trait}, {29, More new trait} -cell {[40, 41, 42, 45], Poppo, true, true, false}, {[46], Fumo, true, false, true}";

        boolean isSecond = isSecond(command);
        String proc = getProcData(command);
        String abil = getAbilData(command);
        String trait = getTraitData(command);
        String cell = getCellData(command);

        ArrayList<AbilityData> procData = new ArrayList<>();
        ArrayList<FlagCellData> abilData = new ArrayList<>();
        ArrayList<FlagCellData> traitData = new ArrayList<>();
        ArrayList<CellData> cellData = new ArrayList<>();

        String warnings = parseProcData(procData, proc)+"\n\n";

        warnings += parseFlagCellData(abilData, abil, "ability") + "\n\n";

        warnings += parseFlagCellData(traitData, trait, "trait") + "\n\n";

        warnings += parseCellData(cellData, cell) + "\n\n";

        warnings = warnings.replaceAll("\n{3,}", "\n\n").strip();

        System.out.println(warnings);

        CommonStatic.ctx = new PackContext();
        CommonStatic.getConfig().ref = false;

        StaticStore.readServerInfo();

        AssetDownloader.checkAssetDownload();

        LangID.initialize();

        Canvas cv = new Canvas();

        CustomMaskUnit[] units = new CustomMaskUnit[2];

        File container = new File("./result");
        int uID = 619;

        File statFile = new File(container, "unit"+ Data.trio(uID+1)+".csv");
        File levelFile = new File(container, "unitlevel.csv");

        if(!statFile.exists()) {
            StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Stat file isn't existing even though code finished validation : "+statFile.getAbsolutePath());
            return;
        }

        if(!levelFile.exists()) {
            StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Level curve file isn't existing even though code finished validation : "+statFile.getAbsolutePath());
            return;
        }

        BufferedReader statReader = new BufferedReader(new FileReader(statFile, StandardCharsets.UTF_8));
        BufferedReader levelReader = new BufferedReader(new FileReader(levelFile, StandardCharsets.UTF_8));

        int count = 0;

        while(count < uID - 1) {
            levelReader.readLine();
            count++;
        }

        String[] curve = levelReader.readLine().split(",");

        for(int i = 0; i < units.length; i++) {
            File maanim = new File(container, getMaanimFileName(i, uID));

            if(!maanim.exists()) {
                StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Maanim file isn't existing even though code finished validation : "+maanim.getAbsolutePath());
                return;
            }

            VFile vf = VFile.getFile(maanim);

            if(vf == null) {
                StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Failed to generate vFile of maanim : "+maanim.getAbsolutePath());
                return;
            }

            MaAnim ma = MaAnim.newIns(vf.getData());

            units[i] = new CustomMaskUnit(statReader.readLine().split(","), curve, ma, curve);
        }

        List<List<CellDrawer>> cellGroup = new ArrayList<>();

        for(int i = 0; i < units.length; i++) {
            cellGroup.add(addCell(cellData, procData, abilData, traitData, units[i], 0, 30, false));
        }

        FontMetrics nfm = cv.getFontMetrics(nameFont);
        FontMetrics cfm = cv.getFontMetrics(contentFont);

        int uh = 0;
        int uw = 0;

        int ah = 0;
        int aw = 0;

        int offset = 0;

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellDrawer> group = cellGroup.get(i);

            for(int j = 0; j < group.size(); j++) {
                group.get(j).initialize(nameFont, contentFont, nfm, cfm);

                if(group.get(i) instanceof NormalCellDrawer)
                    offset = Math.max(((NormalCellDrawer) group.get(i)).offset, offset);
                else if(group.get(i) instanceof AbilityCellDrawer)
                    offset = Math.max(((AbilityCellDrawer) group.get(i)).offset, offset);

                if(j < group.size() - 1) {
                    int tempH = ((NormalCellDrawer) group.get(j)).h;
                    int tempUw = ((NormalCellDrawer) group.get(j)).uw;

                    uh = Math.max(tempH, uh);
                    uw = Math.max(tempUw, uw);
                } else {
                    int tempH = ((AbilityCellDrawer) group.get(j)).h;
                    int tempUw = ((AbilityCellDrawer) group.get(j)).w;

                    ah = Math.max(tempH, ah);
                    aw = Math.max(tempUw, aw);
                }
            }
        }

        if(aw > uw * 3 + CellDrawer.lineOffset * 4) {
            uw = (aw - CellDrawer.lineOffset * 4) / 3;
        }

        int h = 0;
        int w = uw * 3 + CellDrawer.lineOffset * 4;

        List<CellDrawer> group = cellGroup.get(1);

        for(int i = 0; i < group.size(); i++) {
            if(i < group.size() - 1) {
                h += uh;

                h += cellMargin;
            } else {
                h += ah + cellMargin;
            }
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(img.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        int x = 0;
        int y = 0;

        for(int i = 0; i < group.size(); i++) {
            group.get(i).draw(g, x, y, uw, offset, uh, nameFont, contentFont);

            y += uh + cellMargin;

            if(i == group.size() - 2)
                y += cellMargin;
        }

        FontMetrics bf = cv.getFontMetrics(titleFont);
        FontMetrics sf = cv.getFontMetrics(typeFont);
        FontMetrics lf = cv.getFontMetrics(levelFont);

        BufferedImage title = getUnitTitleImage(new File("./result/uni619_c00.png"), "Lilin", "Uber Rare", 30, bf, sf, lf);

        int finW = Math.max(title.getWidth(), img.getWidth() + statPanelMargin * 2) + bgMargin * 2;
        int finH = bgMargin * 5 + title.getHeight() + statPanelMargin * 2 + img.getHeight();

        BufferedImage result = new BufferedImage(finW, finH, BufferedImage.TYPE_INT_ARGB);
        FG2D rg = new FG2D(result.getGraphics());

        rg.setColor(50, 53, 59);
        rg.fillRect(0, 0, finW, finH);

        rg.setColor(24, 25, 28);
        rg.fillRoundRect(0, -cornerRadius, finW, cornerRadius + bgMargin * 6 + title.getHeight(), cornerRadius, cornerRadius);

        rg.setColor(64, 68, 75);
        rg.fillRoundRect(bgMargin, bgMargin * 4 + title.getHeight(), img.getWidth() + statPanelMargin * 2, img.getHeight() + statPanelMargin * 2, cornerRadius, cornerRadius);

        rg.drawImage(title, bgMargin, bgMargin * 2);
        rg.drawImage(img, bgMargin + statPanelMargin, bgMargin * 4 + title.getHeight() + statPanelMargin);

        File f = new File("./result/");

        if(!f.exists() && !f.mkdirs())
            return;

        String fileName = StaticStore.findFileName(f, "result", ".png");

        File image = new File(f.getAbsolutePath(), fileName);

        if(!image.exists() && !image.createNewFile()) {
            return;
        }

        ImageIO.write(result, "PNG", image);
    }

    public static List<CellDrawer> addCell(List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskUnit u, int lang, int lv, boolean isFrame) {
        List<CellDrawer> cells = new ArrayList<>();

        int[] lvs = {lv, 0, 0, 0, 0, 0};

        cells.add(new NormalCellDrawer(
                new String[] {"HP", "Hitbacks", "Speed"},
                new String[] {DataToString.getHP(u, u.curve, false, lvs), DataToString.getHitback(u, false, lvs), DataToString.getSpeed(u, false , lvs)}
        ));

        cells.add(new NormalCellDrawer(new String[] {"Attack"}, new String[] {DataToString.getTotalAtk(u.curve, u, false, lvs)}));

        cells.add(new NormalCellDrawer(
                new String[] {"DPS", "Attack Time", "Use Ability"},
                new String[] {DataToString.getDPS(u, u.curve, false, lvs), DataToString.getAtkTime(u, isFrame), DataToString.getAbilT(u, lang)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {"Pre-Atk", "Post-Atk", "TBA"},
                new String[] {DataToString.getPre(u, isFrame), DataToString.getPost(u, isFrame), DataToString.getTBA(u, isFrame)}
        ));

        StringBuilder trait = new StringBuilder(DataToString.getTrait(u, false, lvs, lang));

        for(int i = 0; i < traitData.size(); i++) {
            String t = traitData.get(i).dataToString(u.data);

            if(!t.isBlank()) {
                trait.append(", ").append(t);
            }
        }

        cells.add(new NormalCellDrawer(
                new String[] {"Trait"},
                new String[] {trait.toString()}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {"Attack Type", "Cost", "Range"},
                new String[] {DataToString.getSiMu(u, lang), DataToString.getCost(u, false, lvs), DataToString.getRange(u)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {"Cooldown"},
                new String[] {DataToString.getCD(u, isFrame, false, lvs)}
        ));

        List<List<CellData>> cellGroup = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            CellData d = data.get(i);

            List<CellData> group = new ArrayList<>();

            if(d.oneLine) {
                group.add(d);

                cellGroup.add(group);
            } else {
                int j = i;

                while(group.size() < 3 && !data.get(j).oneLine) {
                    group.add(data.get(j));

                    j++;

                    if(j >= data.size()) {
                        break;
                    }
                }

                j--;

                cellGroup.add(group);

                if(j > i) {
                    i = j;
                }
            }
        }

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellData> group = cellGroup.get(i);

            String[] names = new String[group.size()];
            String[] contents = new String[group.size()];

            for(int j = 0; j < group.size(); j++) {
                names[j] = group.get(j).name;
                String c = group.get(j).dataToString(u.data, isFrame);

                if(c.isBlank()) {
                    contents[j] = LangID.getStringByID("data_none", lang);
                } else {
                    contents[j] = c;
                }
            }

            cells.add(new NormalCellDrawer(names, contents));
        }

        List<String> abil = Interpret.getAbi(u, lang);

        for(int i = 0; i < abilData.size(); i++) {
            String a = abilData.get(i).dataToString(u.data);

            if(!a.isBlank()) {
                abil.add(a);
            }
        }

        abil.addAll(Interpret.getProc(u, !isFrame, lang, 1.0, 1.0));

        for(int i = 0; i < procData.size(); i++) {
            String p = procData.get(i).beautify(u.data, isFrame);

            if(!p.isBlank()) {
                abil.add(p);
            }
        }

        if(abil.isEmpty()) {
            cells.add(new NormalCellDrawer(new String[] {"Ability"}, new String[] {"None"}));
        } else {
            List<String> finalAbil = new ArrayList<>();

            for(int i = 0; i < abil.size(); i++) {
                finalAbil.add(" · " + abil.get(i));
            }

            cells.add(new AbilityCellDrawer("Ability", finalAbil.toArray(new String[0])));
        }

        return cells;
    }

    private static BufferedImage getUnitTitleImage(File icon, String name, String type, int lv, FontMetrics bfm, FontMetrics sfm, FontMetrics lfm) throws Exception {
        BufferedImage ic = ImageIO.read(icon).getSubimage(9, 21, 110, 85);

        FontRenderContext bfrc = bfm.getFontRenderContext();
        FontRenderContext sfrc = sfm.getFontRenderContext();
        FontRenderContext lfrc = lfm.getFontRenderContext();

        Rectangle2D nRect = titleFont.createGlyphVector(bfrc, name).getPixelBounds(null, 0, 0);
        Rectangle2D tRect = typeFont.createGlyphVector(sfrc, type).getPixelBounds(null, 0, 0);
        Rectangle2D lRect = levelFont.createGlyphVector(lfrc, "Lv. "+lv).getPixelBounds(null, 0, 0);

        int h = (int) Math.round(nRect.getHeight() + nameMargin + tRect.getHeight() + typeUpDownMargin * 2 + levelMargin + lRect.getHeight());

        int icw = (int) ((h - lRect.getHeight() - levelMargin) * 1.0 * ic.getWidth() / ic.getHeight());

        int w = icw + nameMargin + (int) Math.max(nRect.getWidth(), tRect.getWidth() + typeLeftRightMargin * 2);

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(result.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setColor(238, 238, 238, 255);
        g.setFont(titleFont);

        g.drawText(name, (int) (icw + nameMargin - nRect.getX()), (int) (-nRect.getY()));

        g.setFont(levelFont);

        g.drawText("Lv. "+lv, (int) ((icw - lRect.getWidth()) / 2 - lRect.getX()), (int) (h - lRect.getHeight() - lRect.getY()));

        g.setColor(88, 101, 242, 255);

        g.fillRoundRect(icw + nameMargin, (int) (nRect.getHeight() + nameMargin), (int) (typeLeftRightMargin * 2 + tRect.getWidth()), (int) (typeUpDownMargin * 2 + tRect.getHeight()), 36, 36);

        g.setColor(238, 238, 238, 255);
        g.setFont(typeFont);

        g.drawText(type, (int) (icw + nameMargin + typeLeftRightMargin - tRect.getX()), (int) (nRect.getHeight() + nameMargin + typeUpDownMargin - tRect.getY()));

        g.drawImage(ic, 0, 0, icw, h - lRect.getHeight() - levelMargin);

        return result;
    }

    private static String getMaanimFileName(int ind, int uID) {
        switch (ind) {
            case 0:
                return Data.trio(uID)+"_f02.maanim";
            case 1:
                return Data.trio(uID)+"_c02.maanim";
            case 2:
                return Data.trio(uID)+"_s02.maanim";
            default:
                return Data.trio(uID)+ind+"02.maanim";
        }
    }

    private static boolean isSecond(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-s") || contents[i].equals("-second"))
                return true;
        }

        return false;
    }

    private static String getTraitData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-t") || contents[i].equals("-trait") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(contents[j].equals("-p") || contents[j].equals("-proc") || contents[j].equals("-a") || contents[j].equals("-ability") || contents[j].equals("-c") || contents[j].equals("-cell"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
    }

    private static String getCellData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-c") || contents[i].equals("-cell") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(contents[j].equals("-p") || contents[j].equals("-proc") || contents[j].equals("-a") || contents[j].equals("-ability") || contents[j].equals("-t") || contents[j].equals("-trait"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
    }

    private static String getAbilData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-a") || contents[i].equals("-ability") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(contents[j].equals("-p") || contents[j].equals("-proc") || contents[j].equals("-t") || contents[j].equals("-trait") || contents[j].equals("-c") || contents[j].equals("-cell"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
    }

    private static String getProcData(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-p") || contents[i].equals("-proc") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(contents[j].equals("-t") || contents[j].equals("-trait") || contents[j].equals("-a") || contents[j].equals("-ability") || contents[j].equals("-c") || contents[j].equals("-cell"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                return builder.toString().strip();
            }
        }

        return null;
    }

    private static String parseProcData(List<AbilityData> result, String data) {
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

    private static String parseFlagCellData(List<FlagCellData> result, String data, String dataName) {
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

            String ind = content[i].strip();

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

            String name = contents[1].strip();

            result.add(new FlagCellData(name, index));
        }

        return res.substring(0, Math.max(res.length() - 1, 0));
    }

    private static String parseCellData(List<CellData> result, String data) {
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
}
