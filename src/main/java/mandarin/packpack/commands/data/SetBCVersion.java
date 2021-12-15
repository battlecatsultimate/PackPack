package mandarin.packpack.commands.data;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class SetBCVersion extends ConstraintCommand {
    public SetBCVersion(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

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

            createMessage(ch, m -> m.content("Format : p!sbv [Locale] [Version]\n\n"+versions));
            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            createMessage(ch, m -> m.content("Locale must be numeric! 0 : En | 2 : Tw | 3 : Kr | 4 : Jp"));
            return;
        }

        int loc = StaticStore.safeParseInt(contents[1]);

        if(loc < 0 || loc > 4) {
            createMessage(ch, m -> m.content("Locale must be range in 0 ~ 4! 0 : En | 2 : Tw | 3 : Kr | 4 : Jp"));
            return;
        }

        if(!StaticStore.isNumeric(contents[2])) {
            createMessage(ch, m -> m.content("Version must be numeric! Example : 11.0.1 for 110001"));
            return;
        }

        int ver = StaticStore.safeParseInt(contents[2]);

        String version = convertVersion(ver);
        String locale;

        switch (loc) {
            case LangID.EN:
                locale = "BCEN";
                StaticStore.englishVersion = "" + ver;
                break;
            case LangID.ZH:
                locale = "BCTW";
                StaticStore.taiwaneseVersion = "" + ver;
                break;
            case LangID.KR:
                locale = "BCKR";
                StaticStore.koreanVersion = "" + ver;
                break;
            default:
                locale = "BCJP";
                StaticStore.japaneseVersion = "" + ver;
        }

        createMessage(ch, m -> m.content("Set "+locale+" version to "+version+"!"));
    }

    private String convertVersion(int version) {
        int main = version / 10000;

        version -= main * 10000;

        int major = version / 100;
        int minor = version % 100;

        return main+"."+major+"."+minor;
    }
}
