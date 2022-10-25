package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;

public class Prefix extends ConstraintCommand {
    private static final int ERR_CANT_FIND_MEMBER = 0;

    public Prefix(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        String[] list = getContent(event).split(" ");

        if(list.length == 2) {
            if(list[1] == null || list[1].isBlank()) {
                ch.sendMessage(LangID.getStringByID("prefix_space", lang)).setMessageReference(getMessage(event)).mentionRepliedUser(false).queue();
                return;
            }

            Member m = getMember(event);

            if(m != null) {
                StaticStore.prefix.put(m.getId(), list[1]);

                String result = LangID.getStringByID("prefix_set", lang).replace("_", list[1]);

                if(result.length() < 2000) {
                    ch.sendMessage(result)
                            .setAllowedMentions(new ArrayList<>())
                            .setMessageReference(getMessage(event))
                            .mentionRepliedUser(false)
                            .queue();
                } else {
                    ch.sendMessage(LangID.getStringByID("prefix_setnone", lang)).setMessageReference(getMessage(event)).mentionRepliedUser(false).queue();
                }
            }
        } else if(list.length == 1) {
            ch.sendMessage(LangID.getStringByID("prefix_argu", lang)).setMessageReference(getMessage(event)).mentionRepliedUser(false).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("prefix_tooarg", lang)).setMessageReference(getMessage(event)).mentionRepliedUser(false).queue();
        }
    }

    @Override
    public void onFail(GenericMessageEvent event, int error) {
        StaticStore.executed--;

        MessageChannel ch = getChannel(event);

        switch (error) {
            case DEFAULT_ERROR:
                ch.sendMessage("`INTERNAL_ERROR`").queue();
                break;
            case ERR_CANT_FIND_MEMBER:
                ch.sendMessage("Couldn't get member info").queue();
                break;
        }
    }
}
