package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventGrabberHolder extends ComponentHolder {
    public EventGrabberHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessage("Event file grabbing config expired")
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents()
                .queue();
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "bcen" -> {
                EventFileGrabber.newWay.put(CommonStatic.Lang.Locale.EN, !EventFileGrabber.newWay.get(CommonStatic.Lang.Locale.EN));

                performResult(event);
            }
            case "bctw" -> {
                EventFileGrabber.newWay.put(CommonStatic.Lang.Locale.ZH, !EventFileGrabber.newWay.get(CommonStatic.Lang.Locale.ZH));

                performResult(event);
            }
            case "bckr" -> {
                EventFileGrabber.newWay.put(CommonStatic.Lang.Locale.KR, !EventFileGrabber.newWay.get(CommonStatic.Lang.Locale.KR));

                performResult(event);
            }
            case "bcjp" -> {
                EventFileGrabber.newWay.put(CommonStatic.Lang.Locale.JP, !EventFileGrabber.newWay.get(CommonStatic.Lang.Locale.JP));

                performResult(event);
            }
            case "confirm" -> {
                event.deferEdit()
                        .setComponents(TextDisplay.of("Applied"))
                        .useComponentsV2()
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();

                end(true);
            }
        }
    }

    private void performResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setComponents(registerComponent())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<MessageTopLevelComponent> registerComponent() {
        List<MessageTopLevelComponent> layouts = new ArrayList<>();

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

        layouts.add(Container.of(components));

        return layouts;
    }
}
