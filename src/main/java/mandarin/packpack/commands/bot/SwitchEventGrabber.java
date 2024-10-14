package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.EventGrabberHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SwitchEventGrabber extends ConstraintCommand {
    public SwitchEventGrabber(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        replyToMessageSafely(ch, parseMessage(), loader.getMessage(), this::registerComponent, m -> {
            User u = loader.getUser();

            StaticStore.putHolder(u.getId(), new EventGrabberHolder(loader.getMessage(), u.getId(), ch.getId(), m, lang));
        });

    }

    private MessageCreateAction registerComponent(MessageCreateAction action) {
        List<LayoutComponent> layouts = new ArrayList<>();

        for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
            boolean newWay = EventFileGrabber.newWay.get(locale);

            String name = switch (locale) {
                case EN -> "BCEN : ";
                case ZH -> "BCTW : ";
                case JP -> "BCJP : ";
                case KR -> "BCKR : ";
                default -> throw new IllegalStateException(("E/SwitchEventGrabber::parseMessage - Unknown supported version : %s".formatted(locale)));
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

        for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
            boolean newWay = EventFileGrabber.newWay.get(locale);

            String name = switch (locale) {
                case EN -> "BCEN : ";
                case ZH -> "BCTW : ";
                case JP -> "BCJP : ";
                case KR -> "BCKR : ";
                default -> throw new IllegalStateException(("E/SwitchEventGrabber::parseMessage - Unknown supported version : %s".formatted(locale)));
            };

            String isNew = newWay ? "New Way" : "Old Way";

            builder.append(name).append(isNew);

            if (locale != CommonStatic.Lang.Locale.JP) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
