package mandarin.packpack.commands.data;

import mandarin.packpack.PackBot;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class CheckEventUpdate extends ConstraintCommand {
    public CheckEventUpdate(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        ch.sendMessage(LangID.getStringByID("chevent_check", lang)).queue( msg -> {
            try {
                boolean[][] result = StaticStore.event.checkUpdates();

                String res = parseResult(result);

                if(res.isBlank()) {
                    msg.editMessage(LangID.getStringByID("chevent_noup", lang)).queue();
                } else {
                    msg.editMessage(LangID.getStringByID("chevent_done", lang) + "\n\n" + res).queue();

                    PackBot.notifyEvent(ch.getJDA(), result);
                }
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/CheckEventUpdate::doSomething - Failed to check event data");
            }
        });
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
        return switch (loc) {
            case EventFactor.EN -> "en";
            case EventFactor.ZH -> "tw";
            case EventFactor.KR -> "kr";
            default -> "jp";
        };
    }

    private String getFile(int f) {
        return switch (f) {
            case EventFactor.GATYA -> "gatya";
            case EventFactor.ITEM -> "item";
            default -> "sale";
        };
    }
}
