package mandarin.packpack.supporter;

import common.util.lang.MultiLangCont;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmojiStore {
    public static void initialize(JDA jda) {
        TWO_PREVIOUS = StaticStore.getEmoteWitNameAndID(jda, "TwoPrevious", 993716493458083900L, false, true);
        PREVIOUS = StaticStore.getEmoteWitNameAndID(jda, "Previous", 993716355817807883L, false, true);
        NEXT = StaticStore.getEmoteWitNameAndID(jda, "Next", 993716605450203146L, false, true);
        TWO_NEXT = StaticStore.getEmoteWitNameAndID(jda, "TwoNext", 993716631589113857L, false, true);
        CASTLE = StaticStore.getEmoteWitNameAndID(jda, "Castle", 993716694436544574L, false, true);
        BACKGROUND = StaticStore.getEmoteWitNameAndID(jda, "Background", 993716717123555408L, false, true);
        MUSIC = StaticStore.getEmoteWitNameAndID(jda, "Music", 993716741421158420L, false, true);
        MUSIC_BOSS = StaticStore.getEmoteWitNameAndID(jda, "MusicBoss", 993716761855787069L, false, true);
        CROWN_OFF = StaticStore.getEmoteWitNameAndID(jda, "CrownOff", 993716814389444709L, false, true);
        CROWN_ON = StaticStore.getEmoteWitNameAndID(jda, "CrownOn", 993716790813261884L, false, true);
        TREASURE_RADAR = StaticStore.getEmoteWitNameAndID(jda, "TreasureRadar", 993716433387261992L, false, true);
        UDP = StaticStore.getEmoteWitNameAndID(jda, "UDP", 993716659904847912L, false, true);
        NP = StaticStore.getEmoteWitNameAndID(jda, "NP", 1013168214143946823L, false, true);
        FILE = StaticStore.getEmoteWitNameAndID(jda, "File", 1021728205540954142L, false, true);
        FOLDER = StaticStore.getEmoteWitNameAndID(jda, "Folder", 1021730210741227581L, false, true);
        FOLDERUP = StaticStore.getEmoteWitNameAndID(jda, "FolderUp", 1021728258292731914L, false, true);
        PNG = StaticStore.getEmoteWitNameAndID(jda, "PNG", 1021728433870487572L, false, true);
        CSV = StaticStore.getEmoteWitNameAndID(jda, "CSV", 1021728377360633866L, false, true);
        TSV = StaticStore.getEmoteWitNameAndID(jda, "TSV", 1021728409262493706L, false, true);
        JSON = StaticStore.getEmoteWitNameAndID(jda, "JSON", 1021728486483836938L, false, true);
        INI = StaticStore.getEmoteWitNameAndID(jda, "INI", 1021728519195205663L, false, true);
        IMGCUT = StaticStore.getEmoteWitNameAndID(jda, "Imgcut", 1021728290895056936L, false, true);
        MAMODEL = StaticStore.getEmoteWitNameAndID(jda, "Mamodel", 1021728318510342184L, false, true);
        MAANIM = StaticStore.getEmoteWitNameAndID(jda, "Maanim", 1021728344632467517L, false, true);

        File iconData = new File("./data/abilityIcons.txt");

        if(iconData.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(iconData, StandardCharsets.UTF_8));

                String line;

                while((line = reader.readLine()) != null) {
                    if(line.isBlank())
                        break;

                    String[] data = line.split("\t");

                    if(data.length <= 3) {
                        putAbility(jda, data[0], data[1], Long.parseLong(data[2]));
                    } else {
                        if(data[0].startsWith("T_")) {
                            for(int i = 2; i < data.length; i++) {
                                String[] localeID = data[i].split("\\|");

                                if(localeID.length != 2)
                                    continue;

                                localeID[0] = localeID[0].toLowerCase(Locale.ENGLISH);

                                putTrait(jda, data[0], data[1], localeID[0], Long.parseLong(localeID[1]));
                            }
                        }
                    }
                }

                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static RichCustomEmoji TWO_PREVIOUS;
    public static RichCustomEmoji PREVIOUS;
    public static RichCustomEmoji NEXT;
    public static RichCustomEmoji TWO_NEXT;
    public static RichCustomEmoji CASTLE;
    public static RichCustomEmoji BACKGROUND;
    public static RichCustomEmoji MUSIC;
    public static RichCustomEmoji MUSIC_BOSS;
    public static RichCustomEmoji CROWN_OFF;
    public static RichCustomEmoji CROWN_ON;
    public static RichCustomEmoji TREASURE_RADAR;
    public static RichCustomEmoji UDP;
    public static RichCustomEmoji NP;
    public static RichCustomEmoji FILE;
    public static RichCustomEmoji FOLDER;
    public static RichCustomEmoji FOLDERUP;
    public static RichCustomEmoji PNG;
    public static RichCustomEmoji CSV;
    public static RichCustomEmoji TSV;
    public static RichCustomEmoji JSON;
    public static RichCustomEmoji INI;
    public static RichCustomEmoji IMGCUT;
    public static RichCustomEmoji MAMODEL;
    public static RichCustomEmoji MAANIM;

    public static Map<String, RichCustomEmoji> ABILITY = new HashMap<>();
    public static MultiLangCont<String, RichCustomEmoji> TRAIT = new MultiLangCont<>();

    private static void putAbility(JDA jda, String key, String name, long id) {
        RichCustomEmoji emoji = StaticStore.getEmoteWitNameAndID(jda, name, id, false, true);

        if(emoji == null) {
            System.out.println("W/EmojiStore::putAbility - Couldn't get Emoji : " + name + " (" + id + ")");

            return;
        }

        ABILITY.put(key, emoji);
    }

    private static void putTrait(JDA jda, String key, String name, String loc, long id) {
        String locale = name.replace("LOC", loc.toUpperCase(Locale.ENGLISH));

        RichCustomEmoji emoji = StaticStore.getEmoteWitNameAndID(jda, locale, id, false, true);

        if(emoji == null) {
            System.out.println("W/EmojiStore::putAbility - Couldn't get Emoji : " + locale + " (" + id + ")");

            return;
        }

        TRAIT.put(loc.equals("tw") ? "zh" : loc, key, emoji);
    }
}
