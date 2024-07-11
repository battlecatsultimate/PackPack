package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BoosterPin extends ConstraintCommand {
    public BoosterPin(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_MANAGE);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Member m = loader.getMember();

        if(holder == null)
            return;

        if(holder.BOOSTER == null) {
            return;
        }

        if(!StaticStore.rolesToString(m.getRoles()).contains(holder.BOOSTER)) {
            return;
        }

        if(!holder.boosterPin) {
            replyToMessageSafely(ch, LangID.getStringByID("boostpin_noperm", lang), loader.getMessage(), a -> a);

            return;
        }

        if(holder.boosterAll || !holder.boosterPinChannel.isEmpty()) {
            boolean pinAllowed;

            if (holder.boosterAll) {
                pinAllowed = true;
            } else {
                if(ch instanceof ThreadChannel t) {
                    pinAllowed = holder.boosterPinChannel.contains(t.getParentChannel().getId());
                } else {
                    pinAllowed = holder.boosterPinChannel.contains(ch.getId());
                }
            }

            if(!pinAllowed) {
                replyToMessageSafely(ch, LangID.getStringByID("boostpin_nothere", lang), loader.getMessage(), a -> a);

                return;
            }
        }

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            if(loader.getMessage().getReferencedMessage() != null) {
                pinMessage(loader, loader.getMessage().getReferencedMessage());
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("boostpin_id", lang), loader.getMessage(), a -> a);
            }
        } else {
            RestAction<Message> action = obtainMessage(contents[1], ch);

            if (action == null) {
                pinMessage(loader, null);
            } else {
                action.queue(msg -> pinMessage(loader, msg));
            }
        }


    }

    private void pinMessage(CommandLoader loader, Message msg) {
        MessageChannel ch = loader.getChannel();

        if(msg == null) {
            replyToMessageSafely(ch, LangID.getStringByID("boostpin_nomsg", lang), loader.getMessage(), a -> a);

            return;
        }

        if(msg.isPinned()) {
            msg.unpin().queue();

            replyToMessageSafely(ch, String.format(LangID.getStringByID("boostpin_unpin", lang), msg.getJumpUrl()), loader.getMessage(), a -> a);
        } else {
            msg.pin().queue();

            replyToMessageSafely(ch, String.format(LangID.getStringByID("boostpin_pin", lang), msg.getJumpUrl()), loader.getMessage(), a -> a);
        }
    }

    private RestAction<Message> obtainMessage(String id, MessageChannel ch) {
        if(id.startsWith("http")) {
            String[] segments = id.split("/");

            if(!StaticStore.isNumeric(segments[segments.length - 1])) {
                return null;
            }

            try {
                return ch.retrieveMessageById(segments[segments.length - 1]);
            } catch (Exception ignored) {
                return null;
            }
        } else if(StaticStore.isNumeric(id)) {
            try {
                return ch.retrieveMessageById(id);
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }
}
