package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.EventGrabberHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SwitchEventGrabber extends ConstraintCommand {
    public SwitchEventGrabber(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if (ch == null)
            return;

        Message m = getRepliedMessageSafely(ch, parseMessage(), getMessage(event), this::registerComponent);
        User u = getUser(event);

        if (m != null && u != null) {
            StaticStore.putHolder(u.getId(), new EventGrabberHolder(getMessage(event), ch.getId(), m));
        }
    }

    private MessageCreateAction registerComponent(MessageCreateAction action) {
        List<LayoutComponent> layouts = new ArrayList<>();

        for(int i = 0; i < 4; i ++) {
            boolean newWay = EventFileGrabber.newWay.get(i);

            String name = switch (i) {
                case EventFactor.EN -> "BCEN : ";
                case EventFactor.ZH -> "BCTW : ";
                case EventFactor.JP -> "BCJP : ";
                case EventFactor.KR -> "BCKR : ";
                default -> "UNKNOWN : ";
            };

            String isNew = newWay ? "New Way" : "Old Way";

            Emoji e = newWay ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

            layouts.add(ActionRow.of(Button.secondary(name.replace(" : ", "").toLowerCase(Locale.ENGLISH), name + isNew).withEmoji(e)));
        }

        layouts.add(ActionRow.of(Button.primary("confirm", "Confirm")));

        return action.setComponents(layouts);
    }

    private String parseMessage() {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < 4; i++) {
            boolean newWay = EventFileGrabber.newWay.get(i);

            String name = switch (i) {
                case EventFactor.EN -> "BCEN : ";
                case EventFactor.ZH -> "BCTW : ";
                case EventFactor.JP -> "BCJP : ";
                case EventFactor.KR -> "BCKR : ";
                default -> "UNKNOWN : ";
            };

            String isNew = newWay ? "New Way" : "Old Way";

            builder.append(name).append(isNew);

            if (i < 3) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
