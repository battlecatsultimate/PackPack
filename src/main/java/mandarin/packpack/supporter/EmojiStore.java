package mandarin.packpack.supporter;

import common.CommonStatic;
import common.util.lang.MultiLangCont;
import mandarin.card.supporter.CardData;
import mandarin.card.supporter.pack.CardPack;
import mandarin.packpack.supporter.server.data.ShardLoader;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EmojiStore {
    public static void initialize(ShardLoader loader) {
        UNKNOWN = Emoji.fromUnicode("❔");

        AIR = getEmoteWitNameAndID(loader, "Air", 1246011639325528144L);

        THREE_PREVIOUS = getEmoteWitNameAndID(loader, "ThreePrevious", 993716493458083900L);
        TWO_PREVIOUS = getEmoteWitNameAndID(loader, "TwoPrevious", 1174180530871337000L);
        PREVIOUS = getEmoteWitNameAndID(loader, "Previous", 993716355817807883L);
        NEXT = getEmoteWitNameAndID(loader, "Next", 993716605450203146L);
        TWO_NEXT = getEmoteWitNameAndID(loader, "TwoNext", 1174180482297114724L);
        THREE_NEXT = getEmoteWitNameAndID(loader, "ThreeNext", 993716631589113857L);
        CASTLE = getEmoteWitNameAndID(loader, "Castle", 993716694436544574L);
        BACKGROUND = getEmoteWitNameAndID(loader, "Background", 993716717123555408L);
        MUSIC = getEmoteWitNameAndID(loader, "Music", 993716741421158420L);
        MUSIC_BOSS = getEmoteWitNameAndID(loader, "MusicBoss", 993716761855787069L);
        CROWN_OFF = getEmoteWitNameAndID(loader, "CrownOff", 993716814389444709L);
        CROWN_ON = getEmoteWitNameAndID(loader, "CrownOn", 993716790813261884L);
        TREASURE_RADAR = getEmoteWitNameAndID(loader, "TreasureRadar", 993716433387261992L);
        UDP = getEmoteWitNameAndID(loader, "UDP", 993716659904847912L);
        NP = getEmoteWitNameAndID(loader, "NP", 1013168214143946823L);
        FILE = getEmoteWitNameAndID(loader, "File", 1021728205540954142L);
        FOLDER = getEmoteWitNameAndID(loader, "Folder", 1021730210741227581L);
        FOLDERUP = getEmoteWitNameAndID(loader, "FolderUp", 1021728258292731914L);
        PNG = getEmoteWitNameAndID(loader, "PNG", 1021728433870487572L);
        CSV = getEmoteWitNameAndID(loader, "CSV", 1021728377360633866L);
        TSV = getEmoteWitNameAndID(loader, "TSV", 1021728409262493706L);
        JSON = getEmoteWitNameAndID(loader, "JSON", 1021728486483836938L);
        INI = getEmoteWitNameAndID(loader, "INI", 1021728519195205663L);
        IMGCUT = getEmoteWitNameAndID(loader, "Imgcut", 1021728290895056936L);
        MAMODEL = getEmoteWitNameAndID(loader, "Mamodel", 1021728318510342184L);
        MAANIM = getEmoteWitNameAndID(loader, "Maanim", 1021728344632467517L);
        PAYPAL = getEmoteWitNameAndID(loader, "PayPal", 1088764164274663536L);
        CASHAPP = getEmoteWitNameAndID(loader, "CashApp", 1088764190505836564L);
        SWITCHON = getEmoteWitNameAndID(loader, "SwitchOn", 1105684864985993216L);
        SWITCHOFF = getEmoteWitNameAndID(loader, "SwitchOff", 1105684863236976691L);
        ORB = getEmoteWitNameAndID(loader, "Orb", 1105772389255614534L);
        DOGE = getEmoteWitNameAndID(loader, "Doge", 1105766783077584936L);
        SHIBALIEN = getEmoteWitNameAndID(loader, "Shibalien", 1105766785002774610L);
        SHIBALIENELITE = getEmoteWitNameAndID(loader, "Shibalien_Elite", 1105766780439380048L);
        GREENLINE = getEmoteWitNameAndID(loader, "Green_Line", 1140575224526536795L);
        REDDASHEDLINE = getEmoteWitNameAndID(loader, "Red_Dashed_Line", 1140575742082691172L);

        ROLE = getEmoteWitNameAndID(loader, "Role", 1237290096529641472L);
        MEMBER = getEmoteWitNameAndID(loader, "Member", 1237303379911508028L);
        MODERATOR = getEmoteWitNameAndID(loader, "Moderator", 1237303422290759711L);
        BOOSTER = getEmoteWitNameAndID(loader, "Booster", 1237303434915348521L);
        LANGUAGE = getEmoteWitNameAndID(loader, "Language", 1237306025753706546L);
        LEVEL = getEmoteWitNameAndID(loader, "Level", 1252169167977844767L);
        COMPRESS = getEmoteWitNameAndID(loader, "Compress", 1252183494738710528L);
        INFORMATION = getEmoteWitNameAndID(loader, "Information", 1252415073029328996L);

        CHECK = getEmoteWitNameAndID(loader, "Check", 1237662032560525332L);
        CROSS = getEmoteWitNameAndID(loader, "Cross", 1237662049031684136L);
        BACK = getEmoteWitNameAndID(loader, "Back", 1237685808354426921L);

        CAT = Emoji.fromCustom("Cat", 1268786079041851526L, false);

        CC = Emoji.fromCustom("CC", 1394900943996194917L, false);
        ECC = Emoji.fromCustom("ECC", 1394900930612166666L, false);

        File iconData = new File("./data/abilityIcons.txt");

        if(iconData.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(iconData, StandardCharsets.UTF_8));

                String line;

                while((line = reader.readLine()) != null) {
                    if(line.isBlank())
                        break;

                    String[] data = line.split("\t");

                    if (data[0].startsWith("PACK_")) {
                        String packName = switch (data[0].split("_")[1]) {
                            case "SMALL" -> "Small Pack";
                            case "LARGE" -> "Large Pack";
                            case "PREMIUM" -> "Premium Pack";
                            case "MYSTERY" -> "Mystery Pack";
                            case "LEGEND" -> "Legendary Pack";
                            case "EVA" -> "test";
                            default -> null;
                        };

                        if (packName == null)
                            continue;

                        CardPack pack = null;

                        for (CardPack p : CardData.INSTANCE.getCardPacks()) {
                            if (p.getUuid().contains(packName)) {
                                pack = p;

                                break;
                            }
                        }

                        if (pack != null) {
                            ArrayList<Emoji> emojis = PACK.computeIfAbsent(pack, k -> new ArrayList<>());

                            for (int i = 2; i < data.length; i++) {
                                String emojiName = data[1] + (i == 2 ? "" : Integer.toString(i - 1));

                                emojis.add(getEmoteWitNameAndID(loader, emojiName, Long.parseLong(data[i])));
                            }
                        }
                    } else if (data[0].startsWith("CARD_")) {
                        CardPack.CardType cardType = switch (data[0].split("_", 2)[1]) {
                            case "T1" -> CardPack.CardType.T1;
                            case "T2" -> CardPack.CardType.T2;
                            case "T2_SEASONAL" -> CardPack.CardType.SEASONAL;
                            case "T2_COLLAB" -> CardPack.CardType.COLLABORATION;
                            case "T3" -> CardPack.CardType.T3;
                            case "T4" -> CardPack.CardType.T4;
                            default -> null;
                        };

                        if (cardType == null)
                            continue;

                        ArrayList<Emoji> emojis = CARDS.computeIfAbsent(cardType, k -> new ArrayList<>());

                        for (int i = 2; i < data.length; i++) {
                            String emojiName = data[1] + (i == 2 ? "" : Integer.toString(i - 1));

                            emojis.add(getEmoteWitNameAndID(loader, emojiName, Long.parseLong(data[i])));
                        }
                    } else {
                        if(data.length <= 3) {
                            putAbility(loader, data[0], data[1], Long.parseLong(data[2]));
                        } else {
                            if(data[1].contains("LOC")) {
                                for(int i = 2; i < data.length; i++) {
                                    String[] localeID = data[i].split("\\|");

                                    if(localeID.length != 2)
                                        continue;

                                    localeID[0] = localeID[0].toLowerCase(Locale.ENGLISH);

                                    putTrait(loader, data[0], data[1], localeID[0], Long.parseLong(localeID[1]));
                                }
                            }
                        }
                    }
                }

                reader.close();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EmojiStore::initialize - Failed to load emoji data");
            }
        }
    }

    public static Emoji UNKNOWN;
    public static Emoji AIR;

    public static Emoji THREE_PREVIOUS;
    public static Emoji TWO_PREVIOUS;
    public static Emoji PREVIOUS;
    public static Emoji NEXT;
    public static Emoji TWO_NEXT;
    public static Emoji THREE_NEXT;
    public static Emoji CASTLE;
    public static Emoji BACKGROUND;
    public static Emoji MUSIC;
    public static Emoji MUSIC_BOSS;
    public static Emoji CROWN_OFF;
    public static Emoji CROWN_ON;
    public static Emoji TREASURE_RADAR;
    public static Emoji UDP;
    public static Emoji NP;
    public static Emoji FILE;
    public static Emoji FOLDER;
    public static Emoji FOLDERUP;
    public static Emoji PNG;
    public static Emoji CSV;
    public static Emoji TSV;
    public static Emoji JSON;
    public static Emoji INI;
    public static Emoji IMGCUT;
    public static Emoji MAMODEL;
    public static Emoji MAANIM;
    public static Emoji PAYPAL;
    public static Emoji CASHAPP;
    public static Emoji SWITCHON;
    public static Emoji SWITCHOFF;
    public static Emoji ORB;
    public static Emoji DOGE;
    public static Emoji SHIBALIEN;
    public static Emoji SHIBALIENELITE;
    public static Emoji GREENLINE;
    public static Emoji REDDASHEDLINE;

    public static Emoji ROLE;
    public static Emoji MEMBER;
    public static Emoji MODERATOR;
    public static Emoji BOOSTER;
    public static Emoji LANGUAGE;
    public static Emoji LEVEL;
    public static Emoji COMPRESS;
    public static Emoji INFORMATION;

    public static Emoji CHECK;
    public static Emoji CROSS;
    public static Emoji BACK;

    public static Emoji CAT;

    public static Emoji CC;
    public static Emoji ECC;

    public static final Map<String, Emoji> ABILITY = new HashMap<>();
    public static final MultiLangCont<String, Emoji> TRAIT = new MultiLangCont<>();
    public static final Map<CardPack, ArrayList<Emoji>> PACK = new HashMap<>();
    public static final Map<CardPack.CardType, ArrayList<Emoji>> CARDS = new HashMap<>();

    @Nullable
    public static Emoji getCardEmoji(@Nullable CardPack.CardType type) {
        if (type == null)
            return null;

        ArrayList<Emoji> emojiList = CARDS.computeIfAbsent(type, k -> new ArrayList<>());

        if (emojiList.isEmpty())
            return null;

        return emojiList.get(StaticStore.random.nextInt(emojiList.size()));
    }

    @Nullable
    public static Emoji getPackEmoji(@Nullable CardPack pack) {
        if (pack == null)
            return null;

        ArrayList<Emoji> emojiList = PACK.computeIfAbsent(pack, k -> new ArrayList<>());

        if (emojiList.isEmpty())
            return null;

        return emojiList.get(StaticStore.random.nextInt(emojiList.size()));
    }

    private static void putAbility(ShardLoader loader, String key, String name, long id) {
        Emoji emoji = getEmoteWitNameAndID(loader, name, id);

        if(emoji instanceof UnicodeEmoji) {
            System.out.println("W/EmojiStore::putAbility - Couldn't get Emoji : " + name + " (" + id + ")");

            return;
        }

        ABILITY.put(key, emoji);
    }

    private static void putTrait(ShardLoader loader, String key, String name, String loc, long id) {
        String locale = name.replace("LOC", loc.toUpperCase(Locale.ENGLISH));

        Emoji emoji = getEmoteWitNameAndID(loader, locale, id);

        if(emoji instanceof UnicodeEmoji) {
            System.out.println("W/EmojiStore::putAbility - Couldn't get Emoji : " + locale + " (" + id + ")");

            return;
        }

        CommonStatic.Lang.Locale foundLocale = null;

        if (loc.equals("tw")) {
            loc = "zh";
        }

        for (CommonStatic.Lang.Locale l : CommonStatic.Lang.Locale.values()) {
            if (l.code.equals(loc)) {
                foundLocale = l;
                break;
            }
        }

        if (foundLocale == null) {
            throw new IllegalStateException("E/EmojiStore::putTrait - Invalid locale : %s".formatted(loc));
        }

        TRAIT.put(foundLocale, key, emoji);
    }

    private static Emoji getEmoteWitNameAndID(ShardLoader loader, String name, long id) {
        List<RichCustomEmoji> emotes = new ArrayList<>();

        for (Guild g : loader.emojiArchives) {
            emotes.addAll(g.getEmojisByName(name, false));
        }

        emotes.addAll(loader.supportServer.getEmojisByName(name, false));

        if(emotes.isEmpty()) {
            return Emoji.fromCustom(name, id, false);
        }

        for(RichCustomEmoji e : emotes) {
            if(e.getIdLong() == id && !e.isAnimated())
                return e;
        }

        return Emoji.fromCustom(name, id, false);
    }
}
