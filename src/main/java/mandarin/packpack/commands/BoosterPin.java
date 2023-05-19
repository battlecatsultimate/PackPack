package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class BoosterPin extends ConstraintCommand {
    public BoosterPin(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_MANAGE);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Member m = getMember(event);

        if(ch == null || m == null || holder == null)
            return;

        if(holder.BOOSTER == null) {
            return;
        }

        if(!StaticStore.rolesToString(m.getRoles()).contains(holder.BOOSTER)) {
            return;
        }

        if(!holder.boosterPin) {
            replyToMessageSafely(ch, LangID.getStringByID("boostpin_noperm", lang), getMessage(event), a -> a);

            return;
        }

        if(!holder.boosterPinChannel.isEmpty()) {
            boolean pinAllowed;

            if(ch instanceof ThreadChannel t) {
                pinAllowed = holder.boosterPinChannel.contains(t.getParentChannel().getId());
            } else {
                pinAllowed = holder.boosterPinChannel.contains(ch.getId());
            }

            if(!pinAllowed) {
                replyToMessageSafely(ch, LangID.getStringByID("boostpin_nothere", lang), getMessage(event), a -> a);

                return;
            }
        }

        Message msg;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            if(getMessage(event).getReferencedMessage() != null) {
                msg = getMessage(event).getReferencedMessage();
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("boostpin_id", lang), getMessage(event), a -> a);

                return;
            }
        } else {
            msg = obtainMessage(contents[1], ch);
        }

        if(msg == null) {
            replyToMessageSafely(ch, LangID.getStringByID("boostpin_nomsg", lang), getMessage(event), a -> a);

            return;
        }

        if(msg.isPinned()) {
            msg.unpin().queue();

            replyToMessageSafely(ch, String.format(LangID.getStringByID("boostpin_unpin", lang), msg.getJumpUrl()), getMessage(event), a -> a);
        } else {
            msg.pin().queue();

            replyToMessageSafely(ch, String.format(LangID.getStringByID("boostpin_pin", lang), msg.getJumpUrl()), getMessage(event), a -> a);
        }
    }

    private Message obtainMessage(String id, MessageChannel ch) {
        if(id.startsWith("http")) {
            String[] segments = id.split("/");

            if(!StaticStore.isNumeric(segments[segments.length - 1])) {
                return null;
            }

            try {
                return ch.retrieveMessageById(segments[segments.length - 1]).complete();
            } catch (Exception ignored) {
                return null;
            }
        } else if(StaticStore.isNumeric(id)) {
            try {
                return ch.retrieveMessageById(id).complete();
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }
}
