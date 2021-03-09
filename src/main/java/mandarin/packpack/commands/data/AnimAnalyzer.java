package mandarin.packpack.commands.data;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.AnimHolder;
import mandarin.packpack.supporter.server.IDHolder;

import java.io.File;
import java.util.Locale;

public class AnimAnalyzer extends ConstraintCommand {
    private static final int PARAM_DEBUG = 2;
    private static final int PARAM_RAW = 4;

    public AnimAnalyzer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        int anim = getAnimNumber(getContent(event));

        StringBuilder message = new StringBuilder("PNG : -\nIMGCUT : -\nMAMODEL : -\n");

        for(int i = 0; i < anim; i++) {
            message.append("MAANIM ").append(i).append(" : -");

            if(i < anim - 1)
                message.append("\n");
        }

        Message m = ch.createMessage(message.toString()).block();

        if(m == null)
            return;

        int param = checkParam(getContent(event));

        boolean debug = (PARAM_DEBUG & param) > 0;
        boolean raw = (PARAM_RAW & param) > 0;

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File container = new File("./temp", StaticStore.findFileName(temp, "anim", ""));

        if(!container.exists()) {
            boolean res = container.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        Message msg = getMessage(event);

        if(msg != null)
            new AnimHolder(msg, m, lang, ch.getId().asString(), container, debug, ch, raw, anim);
    }

    private int checkParam(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for (String s : pureMessage) {
                switch (s) {
                    case "-r":
                    case "-raw":
                        if ((result & PARAM_RAW) == 0) {
                            result |= PARAM_RAW;
                        } else {
                            break label;
                        }
                        break;
                    case "-d":
                    case "-debug":
                        if ((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                        break;
                }
            }
        }

        return result;
    }

    private int getAnimNumber(String message) {
        String[] contents = message.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-anim") || (contents[i].equals("-a"))) && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                return Math.min(5, Math.max(1, StaticStore.safeParseInt(contents[i + 1])));
            }
        }

        return 1;
    }
}
