package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.EventFileGrabber;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventGrabberHolder extends ComponentHolder {
    private final Message msg;

    public EventGrabberHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message) {
        super(author, channelID, message.getId());

        msg = message;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        msg.editMessage("Event file grabbing config expired")
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents()
                .queue();
    }

    @Override
    public void onEvent(GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "bcen" -> {
                EventFileGrabber.newWay.put(EventFactor.EN, !EventFileGrabber.newWay.get(EventFactor.EN));

                performResult(event);
            }
            case "bctw" -> {
                EventFileGrabber.newWay.put(EventFactor.ZH, !EventFileGrabber.newWay.get(EventFactor.ZH));

                performResult(event);
            }
            case "bckr" -> {
                EventFileGrabber.newWay.put(EventFactor.KR, !EventFileGrabber.newWay.get(EventFactor.KR));

                performResult(event);
            }
            case "bcjp" -> {
                EventFileGrabber.newWay.put(EventFactor.JP, !EventFileGrabber.newWay.get(EventFactor.JP));

                performResult(event);
            }
            case "confirm" -> {
                expired = true;

                event.deferEdit()
                        .setContent("Applied")
                        .setComponents()
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();
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

    private List<LayoutComponent> registerComponent() {
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

        return layouts;
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
