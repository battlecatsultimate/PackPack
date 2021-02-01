package mandarin.packpack.commands.bc;

import discord4j.core.event.domain.message.MessageCreateEvent;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.server.IDHolder;

public class Castle extends ConstraintCommand {
    private static final int PARAM_RC = 2;
    private static final int PARAM_EC = 4;
    private static final int PARAM_WC = 8;
    private static final int PARAM_SC = 16;

    public Castle(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    private int startIndex = 1;

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {

    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.startsWith("-rc")) {
                    if((result & PARAM_RC) == 0) {
                        result |= PARAM_RC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-ec")) {
                    if((result & PARAM_EC) == 0) {
                        result |= PARAM_EC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-wc")) {
                    if((result & PARAM_WC) == 0) {
                        result |= PARAM_WC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-sc")) {
                    if((result & PARAM_SC) == 0) {
                        result |= PARAM_SC;
                        startIndex++;
                    }

                    break;
                } else {
                    break;
                }
            }
        }

        return result;
    }
}
