package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class ServerPrefix extends ConstraintCommand {
    public ServerPrefix(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        String[] list = getMessage(event).split(" ");

        if(list.length == 2) {
            if(list[1] == null || list[1].isBlank()) {
                ch.createMessage(LangID.getStringByID("prefix_space", lang));
                return;
            }

            StaticStore.serverPrefix = list[1];

            ch.createMessage(LangID.getStringByID("serverpre_set", lang).replace("_", StaticStore.serverPrefix)).subscribe();
        } else if(list.length == 1) {
            ch.createMessage(LangID.getStringByID("prefix_argu", lang)).subscribe();
        } else {
            ch.createMessage(LangID.getStringByID("prefix_tooag", lang)).subscribe();
        }
    }
}
