package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Prefix extends ConstraintCommand {
    private static final int ERR_CANT_FIND_MEMBER = 0;

    public Prefix(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        String[] list = getContent(event).split(" ");

        if(list.length == 2) {
            if(list[1] == null || list[1].isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("prefix_space", lang), getMessage(event), a -> a);

                return;
            }

            User u = getUser(event);

            if(u != null) {
                StaticStore.prefix.put(u.getId(), list[1]);

                String result = String.format(LangID.getStringByID("prefix_set", lang), list[1]);

                if(result.length() < 2000) {
                    replyToMessageSafely(ch, result, getMessage(event), a -> a);
                } else {
                    replyToMessageSafely(ch, LangID.getStringByID("prefix_setnone", lang), getMessage(event), a -> a);
                }
            }
        } else if(list.length == 1) {
            replyToMessageSafely(ch, LangID.getStringByID("prefix_tooarg", lang), getMessage(event), a -> a);
        } else {
            replyToMessageSafely(ch, LangID.getStringByID("prefix_tooarg", lang), getMessage(event), a -> a);
        }
    }

    @Override
    public void onFail(GenericMessageEvent event, int error) {
        StaticStore.executed--;

        MessageChannel ch = getChannel(event);

        switch (error) {
            case DEFAULT_ERROR:
            case SERVER_ERROR:
                ch.sendMessage("`INTERNAL_ERROR`").queue();
                break;
            case ERR_CANT_FIND_MEMBER:
                ch.sendMessage("Couldn't get member info").queue();
                break;
        }
    }
}
