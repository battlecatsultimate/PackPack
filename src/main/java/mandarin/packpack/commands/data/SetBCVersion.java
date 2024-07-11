package mandarin.packpack.commands.data;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class SetBCVersion extends ConstraintCommand {
    public SetBCVersion(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length != 3) {
            String versions = "BCEN : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(CommonStatic.Lang.Locale.EN))) +
                    "\n" +
                    "BCTW : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(CommonStatic.Lang.Locale.ZH))) +
                    "\n" +
                    "BCKR : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(CommonStatic.Lang.Locale.KR))) +
                    "\n" +
                    "BCJP : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(CommonStatic.Lang.Locale.JP)));

            ch.sendMessage("Format : p!sbv [Locale] [Version]\n\n"+versions).queue();

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            ch.sendMessage("Locale must be numeric! 0 : En | 2 : Tw | 3 : Kr | 4 : Jp").queue();

            return;
        }

        int loc = StaticStore.safeParseInt(contents[1]);

        if(loc < 0 || loc > 4) {
            ch.sendMessage("Locale must be range in 0 ~ 4! 0 : En | 2 : Tw | 3 : Kr | 4 : Jp").queue();

            return;
        }

        if(!StaticStore.isNumeric(contents[2])) {
            ch.sendMessage("Version must be numeric! Example : 11.0.1 for 110001").queue();

            return;
        }

        CommonStatic.Lang.Locale locale = switch (loc) {
            case 0 -> CommonStatic.Lang.Locale.EN;
            case 1 -> CommonStatic.Lang.Locale.ZH;
            case 2 -> CommonStatic.Lang.Locale.KR;
            case 3 -> CommonStatic.Lang.Locale.JP;
            default -> throw new IllegalStateException("E/SetBCVersion::doSomething - Unexpected value : " + loc);
        };

        int ver = StaticStore.safeParseInt(contents[2]);

        String version = convertVersion(ver);
        String localeCode;

        switch (locale) {
            case EN -> {
                localeCode = "BCEN";
                StaticStore.englishVersion = String.valueOf(ver);
            }
            case ZH -> {
                localeCode = "BCTW";
                StaticStore.taiwaneseVersion = String.valueOf(ver);
            }
            case KR -> {
                localeCode = "BCKR";
                StaticStore.koreanVersion = String.valueOf(ver);
            }
            default -> {
                localeCode = "BCJP";
                StaticStore.japaneseVersion = String.valueOf(ver);
            }
        }

        ch.sendMessage("Set "+localeCode+" version to "+version+"!").queue();
    }

    private String convertVersion(int version) {
        int main = version / 10000;

        version -= main * 10000;

        int major = version / 100;
        int minor = version % 100;

        return main+"."+major+"."+minor;
    }
}
