package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
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
                        .setContent("Applied")
                        .setComponents()
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();

                end(true);
            }
        }
    }

    private void performResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(parseMessage())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents(registerComponent())
                .queue();
    }

    private List<MessageTopLevelComponent> registerComponent() {
        List<MessageTopLevelComponent> layouts = new ArrayList<>();

        for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
            boolean newWay = EventFileGrabber.newWay.get(locale);

            String name = switch (locale) {
                case EN -> "BCEN : ";
                case ZH -> "BCTW : ";
                case JP -> "BCJP : ";
                case KR -> "BCKR : ";
                default -> "";
            };

            String isNew = newWay ? "New Way" : "Old Way";

            Emoji e = newWay ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

            layouts.add(ActionRow.of(Button.secondary(name.replace(" : ", "").toLowerCase(Locale.ENGLISH), name + isNew).withEmoji(e)));
        }

        layouts.add(ActionRow.of(Button.primary("confirm", "Confirm")));

        return layouts;
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
                default -> "";
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
