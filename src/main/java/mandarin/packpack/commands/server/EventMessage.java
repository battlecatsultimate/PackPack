package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class EventMessage extends ConstraintCommand {
    public EventMessage(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null || holder == null)
            return;

        String[] contents = getContent(event).split(" ", 3);

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("eventmes_noloc", lang), getMessage(event), a -> a);

            return;
        }

        String loc = getLocale(contents[1]);

        if(loc == null) {
            replyToMessageSafely(ch, LangID.getStringByID("eventmes_invalidloc", lang), getMessage(event), a -> a);

            return;
        }

        String message;

        if(contents.length < 3) {
            message = "";
        } else {
            message = contents[2];
        }

        if(message.length() >= 2000) {
            replyToMessageSafely(ch, LangID.getStringByID("eventmes_toolong", lang), getMessage(event), a -> a);

            return;
        }

        if(Pattern.compile("(<@(&)?\\d+>|@everyone|@here)").matcher(message).find()) {
            Member m = getMember(event);
            Message msg = getRepliedMessageSafely(ch, LangID.getStringByID("eventmes_mention", lang), getMessage(event), a -> registerConfirmButtons(a, lang));

            if(m != null) {
                StaticStore.putHolder(m.getId(), new ConfirmButtonHolder(getMessage(event), msg, ch.getId(), () -> {
                    if(message.isBlank()) {
                        if(holder.eventMessage.containsKey(loc)) {
                            holder.eventMessage.remove(loc);

                            replyToMessageSafely(ch, LangID.getStringByID("eventmes_removed", lang), getMessage(event), a -> a);
                        } else {
                            replyToMessageSafely(ch, LangID.getStringByID("eventmes_noempty", lang), getMessage(event), a -> a);
                        }
                    } else {
                        holder.eventMessage.put(loc, message);

                        replyToMessageSafely(ch, LangID.getStringByID("eventmes_added", lang), getMessage(event), a -> a);
                    }
                } ,lang));
            }
        } else {
            if(message.isBlank()) {
                if(holder.eventMessage.containsKey(loc)) {
                    holder.eventMessage.remove(loc);

                    replyToMessageSafely(ch, LangID.getStringByID("eventmes_removed", lang), getMessage(event), a -> a);
                } else {
                    replyToMessageSafely(ch, LangID.getStringByID("eventmes_noempty", lang), getMessage(event), a -> a);
                }
            } else {
                holder.eventMessage.put(loc, message);

                replyToMessageSafely(ch, LangID.getStringByID("eventmes_added", lang), getMessage(event), a -> a);
            }
        }
    }

    private String getLocale(String loc) {
        return switch (loc) {
            case "-en" -> "en";
            case "-tw" -> "tw";
            case "-kr" -> "kr";
            case "-jp" -> "jp";
            default -> null;
        };
    }
}
