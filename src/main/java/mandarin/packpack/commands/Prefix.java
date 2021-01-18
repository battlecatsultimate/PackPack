package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class Prefix extends ConstraintCommand {
    private static final int ERR_CANT_FIND_MEMBER = 0;

    public Prefix(ROLE role, int lang, IDHolder holder) {
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

            event.getMember().ifPresentOrElse(m -> {
                StaticStore.prefix.put(m.getId().asString(), list[1]);

                ch.createMessage(LangID.getStringByID("prefix_set", lang).replace("_", list[1])).subscribe();
            }, () -> onFail(event, ERR_CANT_FIND_MEMBER));
        } else if(list.length == 1) {
            ch.createMessage(LangID.getStringByID("prefix_argu", lang)).subscribe();
        } else {
            ch.createMessage(LangID.getStringByID("prefix_tooarg", lang)).subscribe();
        }
    }

    @Override
    public void onFail(MessageCreateEvent event, int error) {
        MessageChannel ch = getChannel(event);

        switch (error) {
            case DEFAULT_ERROR:
                ch.createMessage("`INTERNAL_ERROR`").subscribe();
                break;
            case ERR_CANT_FIND_MEMBER:
                ch.createMessage("Couldn't get member info").subscribe();
                break;
        }
    }
}
