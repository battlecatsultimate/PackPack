package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class SetBCVersion extends ConstraintCommand {
    public SetBCVersion(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length != 3) {
            String versions = "BCEN : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(0))) +
                    "\n" +
                    "BCTW : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(1))) +
                    "\n" +
                    "BCKR : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(2))) +
                    "\n" +
                    "BCJP : " +
                    convertVersion(StaticStore.safeParseInt(StaticStore.getVersion(3)));

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

        int ver = StaticStore.safeParseInt(contents[2]);

        String version = convertVersion(ver);
        String locale;

        switch (loc) {
            case LangID.EN -> {
                locale = "BCEN";
                StaticStore.englishVersion = String.valueOf(ver);
            }
            case LangID.ZH -> {
                locale = "BCTW";
                StaticStore.taiwaneseVersion = String.valueOf(ver);
            }
            case LangID.KR -> {
                locale = "BCKR";
                StaticStore.koreanVersion = String.valueOf(ver);
            }
            default -> {
                locale = "BCJP";
                StaticStore.japaneseVersion = String.valueOf(ver);
            }
        }

        ch.sendMessage("Set "+locale+" version to "+version+"!").queue();
    }

    private String convertVersion(int version) {
        int main = version / 10000;

        version -= main * 10000;

        int major = version / 100;
        int minor = version % 100;

        return main+"."+major+"."+minor;
    }
}
