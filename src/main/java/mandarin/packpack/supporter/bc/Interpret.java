package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.battle.data.MaskAtk;
import common.battle.data.MaskEnemy;
import common.battle.data.MaskEntity;
import common.pack.Identifier;
import common.util.Data;
import common.util.lang.Formatter;
import common.util.lang.ProcLang;
import common.util.unit.Trait;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Interpret extends Data {
    public static final String[] TRAIT = {
            "data.traits.red", "data.traits.floating", "data.traits.black", "data.traits.metal", "data.traits.angel", "data.traits.alien", "data.traits.zombie",
            "data.traits.aku", "data.traits.relic", "data.traits.white", "data.traits.evaAngel", "data.traits.witch", "data.traits.colossus", "data.traits.behemoth", "data.traits.sage",
            "data.traits.base"
    };

    public static final String[] TRAITICON = {
            "T_RED", "T_FLOAT", "T_BLACK", "T_METAL", "T_ANGEL", "T_ALIEN", "T_ZOMBIE", "T_AKU", "T_RELIC", "T_WHITE",
            "EVA", "WITCH", "COLOSSUS", "BEHEMOTH", "SAGE", "BASE"
    };

    public static final String[] ABIS = {
            "data.abilities.strong", "data.abilities.resistant", "data.abilities.massiveDamage", "data.abilities.targetsOnly", "data.abilities.metal", "data.abilities.waveBlocker",
            "data.abilities.immunities.sniperKb", "data.abilities.immunities.stopTime", "data.abilities.ghost", "data.abilities.zombieKiller", "data.abilities.witchKiller", "data.abilities.selfDestruct",
            "data.abilities.immunities.themeChange", "data.abilities.evaAngelKiller", "data.abilities.immunities.bossWave", "data.abilities.insanelyTough", "data.abilities.insaneDamage", "data.abilities.colossusSlayer",
            "data.abilities.soulStrike", "data.abilities.counterSurge", "data.abilities.sageSlayer"
    };

    public static final String[] PROCIND = {
            "WEAK", "STOP", "SLOW", "KB", "WARP", "CURSE", "IMUATK", "STRONG", "LETHAL", "ATKBASE",
            "CRIT", "METALKILL", "BREAK", "SHIELDBREAK", "SATK", "BOUNTY", "MINIWAVE", "WAVE",
            "MINIVOLC", "VOLC", "BLAST", "SPIRIT", "BSTHUNT", "IMUWEAK", "IMUSTOP", "IMUSLOW",
            "IMUKB", "IMUWAVE", "IMUVOLC", "IMUWARP", "IMUCURSE", "IMUPOIATK", "POIATK",
            "DEMONSHIELD", "DEATHSURGE", "BURROW", "REVIVE", "SNIPER", "SEAL", "TIME", "SUMMON",
            "MOVEWAVE", "THEME", "POISON", "BOSS", "ARMOR", "SPEED", "COUNTER", "DMGCUT", "DMGCAP",
            "CRITI", "IMUSEAL", "IMUPOI", "IMUSUMMON", "IMUMOVING", "IMUARMOR", "IMUSPEED"
    };

    public static final int[] P_INDEX = {
            P_WEAK, P_STOP, P_SLOW, P_KB, P_WARP, P_CURSE, P_IMUATK, P_STRONG, P_LETHAL, P_ATKBASE,
            P_CRIT, P_METALKILL, P_BREAK, P_SHIELDBREAK, P_SATK, P_BOUNTY, P_MINIWAVE, P_WAVE,
            P_MINIVOLC, P_VOLC, P_BLAST, P_SPIRIT, P_BSTHUNT, P_IMUWEAK, P_IMUSTOP, P_IMUSLOW,
            P_IMUKB, P_IMUWAVE, P_IMUVOLC, P_IMUWARP, P_IMUCURSE, P_IMUPOIATK, P_POIATK,
            P_DEMONSHIELD, P_DEATHSURGE, P_BURROW, P_REVIVE, P_SNIPER, P_SEAL, P_TIME, P_SUMMON,
            P_MOVEWAVE, P_THEME, P_POISON, P_BOSS, P_ARMOR, P_COUNTER, P_DMGCUT, P_DMGCUT, P_SPEED,
            P_CRITI, P_IMUSEAL, P_IMUPOI, P_IMUSUMMON, P_IMUMOVING, P_IMUARMOR, P_IMUSPEED
    };

    public static String getTrait(List<Trait> traits, int star, boolean icon, CommonStatic.Lang.Locale lang) {
        StringBuilder res = new StringBuilder();

        for(int i = 0; i < traits.size(); i++) {
            Trait trait = traits.get(i);

            if(trait.id.pack.equals(Identifier.DEF)) {
                if(icon) {
                    res.append(getTraitEmoji(TRAITICON[trait.id.id], lang));
                }

                if(trait.id.id == 5 && star == 1) {
                    res.append(LangID.getStringByID(TRAIT[trait.id.id], lang))
                            .append(" (")
                            .append(LangID.getStringByID("data.traits.starred", lang))
                            .append("), ");
                } else {
                    res.append(LangID.getStringByID(TRAIT[trait.id.id], lang))
                            .append(", ");
                }
            } else {
                res.append(trait.name)
                        .append(", ");
            }
        }

        return res.toString();
    }

    public static boolean isType(MaskEntity du, int type) {
        int[][] raw = du.rawAtkData();

        return switch (type) {
            case 0 -> !du.isRange();
            case 1 -> du.isRange();
            case 2 -> du.isLD();
            case 3 -> raw.length > 1;
            case 4 -> du.isOmni();
            default -> false;
        };
    }

    public static ArrayList<String> getAbi(MaskEntity mu, boolean icon, CommonStatic.Lang.Locale lang, List<Trait> traits, TreasureHolder holder) {
        ArrayList<String> l = new ArrayList<>();

        for(int i = 0; i < ABIS.length; i++) {
            if(((mu.getAbi() >> i) & 1) > 0) {
                String ab = (icon ? getAbilityEmoji(ABIS[i]) : "") + LangID.getStringByID(ABIS[i], lang);

                if(ab.startsWith("Imu."))
                    ab = ab.replace("Imu.", "Immune to");
                else
                    switch (i) {
                        case 0 -> {
                            if(traits != null && holder != null) {
                                ab += String.format(LangID.getStringByID("data.abilities.description.strong.withTreasure", lang), DataToString.df.format(holder.getStrongHealthMultiplier(traits)), DataToString.df.format(holder.getStrongAttackMultiplier(traits)));
                            } else {
                                ab += LangID.getStringByID("data.abilities.description.strong.normal", lang);
                            }
                        }
                        case 1 -> {
                            if(traits != null && holder != null) {
                                ab += String.format(LangID.getStringByID("data.abilities.description.resistant.withTreasure", lang), DataToString.df.format(holder.getResistHealthMultiplier(traits)));
                            } else {
                                ab += LangID.getStringByID("data.abilities.description.resistant.normal", lang);
                            }
                        }
                        case 2 -> {
                            if(traits != null && holder != null) {
                                ab += String.format(LangID.getStringByID("data.abilities.description.massiveDamage.withTreasure", lang), DataToString.df.format(holder.getMassiveAttackMultiplier(traits)));
                            } else {
                                ab += LangID.getStringByID("data.abilities.description.massiveDamage.normal", lang);
                            }
                        }
                        case 10 -> ab += LangID.getStringByID("data.abilities.description.witchKiller", lang);
                        case 13 -> ab += LangID.getStringByID("data.abilities.description.evaAngelKiller", lang);
                        case 15 -> {
                            if(traits != null && holder != null) {
                                ab += String.format(LangID.getStringByID("data.abilities.description.insanelyTough.withTreasure", lang), DataToString.df.format(holder.getInsaneResistHealthMultiplier(traits)));
                            } else {
                                ab += LangID.getStringByID("data.abilities.description.insanelyTough.normal", lang);
                            }
                        }
                        case 16 -> {
                            if(traits != null && holder != null) {
                                ab += String.format(LangID.getStringByID("data.abilities.description.insaneDamage.withTreasure", lang), DataToString.df.format(holder.getInsaneMassiveAttackMultiplier(traits)));
                            } else {
                                ab += LangID.getStringByID("data.abilities.description.insaneDamage.normal", lang);
                            }
                        }
                        case 17 -> ab += LangID.getStringByID("data.abilities.description.colossusSlayer", lang);
                        case 20 -> ab += LangID.getStringByID("data.abilities.description.sageSlayer", lang);
                    }

                if(!l.contains(ab))
                    l.add(ab);
            }
        }

        return l;
    }

    public static ArrayList<String> getProc(MaskEntity du, boolean useSecond, boolean icon, CommonStatic.Lang.Locale lang, double multi, double amulti, boolean treasure, List<Trait> traits, Function<List<Trait>, Double> function) {
        ArrayList<String> l = new ArrayList<>();
        ArrayList<Integer> id = new ArrayList<>();

        MaskAtk mr = du.getRepAtk();
        Formatter.Context c = new Formatter.Context(du instanceof MaskEnemy, useSecond, new double[] { multi, amulti }, treasure, traits, function);

        for(int i = 0; i < PROCIND.length; i++) {
            if(isValidProc(i, mr)) {
                String f = ProcLang.getWithLang(lang).get(PROCIND[i]).format;

                Object proc = getProcObject(i, mr);

                String ans = (icon ? getProcEmoji(PROCIND[i], proc) : "") + Formatter.format(f, proc, c);

                if(!l.contains(ans)) {
                    if(id.contains(i)) {
                        if(isEnglish(lang)) {
                            ans += " ["+getNumberAttack(getNumberExtension(1), lang)+"]";
                        } else {
                            ans += " ["+LangID.getStringByID("data_nthatk", lang).replace("_", String.valueOf(1))+"]";
                        }
                    }

                    l.add(ans);
                    id.add(i);
                }
            }
        }

        for(int j = 0; j < du.getAtkCount(); j++) {
            MaskAtk ma = du.getAtkModel(j);

            for(int i = 0; i < PROCIND.length; i++) {
                if(isValidProc(i, ma)) {
                    String f = ProcLang.getWithLang(lang).get(PROCIND[i]).format;

                    Object proc = getProcObject(i, ma);

                    String ans = (icon ? getProcEmoji(PROCIND[i], proc) : "") + Formatter.format(f, proc, c);

                    if(!l.contains(ans)) {
                        if(id.contains(i)) {
                            if(isEnglish(lang)) {
                                ans += " ["+getNumberAttack(getNumberExtension(1), lang)+"]";
                            } else {
                                ans += " ["+LangID.getStringByID("data_nthatk", lang).replace("_", String.valueOf(1))+"]";
                            }
                        }

                        l.add(ans);
                        id.add(i);
                    }
                }
            }
        }

        return l;
    }

    private static Object getProcObject(int ind, MaskAtk atk) {
        if(ind >= 0 && ind < P_INDEX.length)
            return atk.getProc().getArr(P_INDEX[ind]);
        else
            return atk.getProc().KB;
    }

    private static boolean isValidProc(int ind, MaskAtk atk) {
        if(ind >= 0 && ind < P_INDEX.length) {
            return atk.getProc().getArr(P_INDEX[ind]).exists();
        } else
            return false;
    }

    private static boolean isEnglish(CommonStatic.Lang.Locale lang) {
        return lang == CommonStatic.Lang.Locale.EN;
    }

    @SuppressWarnings("unused")
    private static String getNumberAttack(String pre, CommonStatic.Lang.Locale lang) {
        return pre + " Attack";
    }

    @SuppressWarnings("SameParameterValue")
    private static String getNumberExtension(int n) {
        if (n == 11 || n == 12 || n == 13)
            return "th";

        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private static String getProcEmoji(String code, Object proc) {
        if(proc instanceof Proc.IMU && ((Proc.IMU) proc).mult < 100) {
            code = code.replace("IMU", "RES");
        } else if(proc instanceof Proc.IMUAD && ((Proc.IMUAD) proc).mult < 100) {
            code = code.replace("IMU", "RES");
        } else if(proc instanceof Proc.WAVEI && ((Proc.WAVEI) proc).mult < 100) {
            code = code.replace("IMU", "RES");
        }

        Emoji emoji = EmojiStore.ABILITY.get(code);

        if(emoji != null) {
            return emoji.getFormatted() + " ";
        } else {
            return "";
        }
    }

    private static String getAbilityEmoji(String code) {
        Emoji emoji = EmojiStore.ABILITY.get(code);

        if(emoji != null) {
            return emoji.getFormatted() + " ";
        } else {
            return "";
        }
    }

    private static String getTraitEmoji(String code, CommonStatic.Lang.Locale lang) {
        Emoji emoji;

        if(code.startsWith("T_")) {
            emoji = EmojiStore.TRAIT.getCont(code, lang);
        } else {
            emoji = EmojiStore.ABILITY.get(code);
        }

        if(emoji == null)
            return "";
        else
            return emoji.getFormatted() + " ";
    }
}
