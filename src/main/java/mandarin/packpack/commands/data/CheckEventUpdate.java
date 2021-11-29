package mandarin.packpack.commands.data;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.PackBot;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class CheckEventUpdate extends ConstraintCommand {
    private final GatewayDiscordClient gate;

    public CheckEventUpdate(ROLE role, int lang, IDHolder id, GatewayDiscordClient gate) {
        super(role, lang, id);

        this.gate = gate;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Message msg = createMessage(ch, m -> m.content(LangID.getStringByID("chevent_check", lang)));

        boolean[][] result = StaticStore.event.checkUpdates();

        String res = parseResult(result);

        if(res.isBlank()) {
            editMessage(msg, m -> m.content(wrap(LangID.getStringByID("chevent_noup", lang))));
        } else {
            editMessage(msg, m -> m.content(wrap(LangID.getStringByID("chevent_done", lang) + "\n\n" + res)));

            PackBot.notifyEvent(gate, result);
        }
    }

    private String parseResult(boolean[][] result) {
        StringBuilder r = new StringBuilder();

        for(int i = 0; i < result.length; i++) {
            for(int j = 0; j < result[i].length; j++) {
                if(result[i][j]) {
                    r.append(getLocale(i)).append(" : ").append(getFile(j)).append("\n");
                }
            }
        }

        String res = r.toString();

        if(!res.isBlank()) {
            return res.substring(0, res.length() - 1);
        } else {
            return "";
        }
    }

    private String getLocale(int loc) {
        switch (loc) {
            case EventFactor.EN:
                return "en";
            case EventFactor.ZH:
                return "tw";
            case EventFactor.KR:
                return "kr";
            default:
                return "jp";
        }
    }

    private String getFile(int f) {
        switch (f) {
            case EventFactor.GATYA:
                return "gatya";
            case EventFactor.ITEM:
                return "item";
            default:
                return "sale";
        }
    }
}
