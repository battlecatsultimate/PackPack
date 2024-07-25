package mandarin.packpack.commands.data;

import common.CommonStatic;
import mandarin.packpack.PackBot;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

public class CheckEventUpdate extends ConstraintCommand {
    public CheckEventUpdate(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ch.sendMessage(LangID.getStringByID("checkEvent.checking", lang)).queue( msg -> {
            try {
                boolean[][] result = StaticStore.event.checkUpdates();

                String res = parseResult(result);

                if(res.isBlank()) {
                    msg.editMessage(LangID.getStringByID("checkEvent.noEvent", lang)).queue();
                } else {
                    msg.editMessage(LangID.getStringByID("checkEvent.done", lang) + "\n\n" + res).queue();

                    ShardManager manager = ch.getJDA().getShardManager();

                    if (manager != null) {
                        PackBot.notifyEvent(manager, result);
                    }
                }
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/CheckEventUpdate::doSomething - Failed to check event data");
            }
        });
    }

    private String parseResult(boolean[][] result) {
        StringBuilder r = new StringBuilder();

        for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
            int index = ArrayUtils.indexOf(EventFactor.supportedVersions, locale);

            if (index == -1)
                continue;

            for(int j = 0; j < result[index].length; j++) {
                if(result[index][j]) {
                    r.append(locale.code).append(" : ").append(getFile(j)).append("\n");
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

    private String getFile(int f) {
        return switch (f) {
            case EventFactor.GATYA -> "gatya";
            case EventFactor.ITEM -> "item";
            default -> "sale";
        };
    }
}
