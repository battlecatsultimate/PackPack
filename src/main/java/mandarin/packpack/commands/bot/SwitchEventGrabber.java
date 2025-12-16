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
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.components.buttons.Button;
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

        List<ContainerChildComponent> components = new ArrayList<>();

        for (int i = 0; i < EventFactor.supportedVersions.length; i++) {
            CommonStatic.Lang.Locale locale = EventFactor.supportedVersions[i];

            boolean newWay = EventFileGrabber.newWay.getOrDefault(locale, false);

            String name = switch (locale) {
                case EN -> "BCEN";
                case ZH -> "BCTW";
                case JP -> "BCJP";
                case KR -> "BCKR";
                default -> throw new IllegalStateException(("E/SwitchEventGrabber::parseMessage - Unknown supported version : %s".formatted(locale)));
            };

            String isNew = newWay ? "New Way" : "Old Way";

            Emoji e = newWay ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

            components.add(Section.of(Button.secondary(name.toLowerCase(Locale.ENGLISH), isNew).withEmoji(e), TextDisplay.of(name + " : " + isNew)));

            if (i < EventFactor.supportedVersions.length - 1) {
                components.add(Separator.create(false, Separator.Spacing.SMALL));
            }
        }

        components.add(Separator.create(true, Separator.Spacing.LARGE));
        components.add(ActionRow.of(Button.primary("confirm", "Confirm").withEmoji(EmojiStore.CHECK)));

        replyToMessageSafely(ch, loader.getMessage(), msg -> {
            User u = loader.getUser();

            StaticStore.putHolder(u.getId(), new EventGrabberHolder(loader.getMessage(), u.getId(), ch.getId(), msg, lang));
        }, Container.of(components));

    }
}
