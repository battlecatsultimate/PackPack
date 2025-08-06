package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.components.ActionComponent;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponentUnion;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent;
import net.dv8tion.jda.api.components.section.SectionContentComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class ComponentHolder extends Holder {
    public static List<MessageTopLevelComponent> expireButton(Message message, boolean removeButton) {
        List<MessageTopLevelComponent> components = new ArrayList<>();

        for (MessageTopLevelComponent component : message.getComponents()) {
            if (component instanceof Container container) {
                List<ContainerChildComponent> children = new ArrayList<>();

                for (ContainerChildComponent child : container.getComponents()) {
                    if (child instanceof Section section) {
                        SectionAccessoryComponent accessory = section.getAccessory();

                        if (accessory instanceof Button button) {
                            if (!removeButton) {
                                children.add(Section.of(button.asDisabled(), section.getContentComponents()));
                            } else {
                                boolean needSection = false;

                                List<ContainerChildComponent> separatedChildren = new ArrayList<>();

                                for (SectionContentComponent contentComponent : section.getContentComponents()) {
                                    if (!(contentComponent instanceof ContainerChildComponent containerChild)) {
                                        needSection = true;

                                        break;
                                    }

                                    separatedChildren.add(containerChild);
                                }

                                if (needSection) {
                                    Section.of(button.asDisabled(), section.getContentComponents());
                                } else {
                                    children.addAll(separatedChildren);
                                }
                            }
                        } else {
                            children.add(section);
                        }
                    } else if (child instanceof ActionRow row) {
                        if (!removeButton) {
                            List<ActionRowChildComponentUnion> actionRowChildren = new ArrayList<>();

                            for (ActionRowChildComponentUnion rowChild : row.getComponents()) {
                                if (rowChild instanceof ActionComponent actionComponent) {
                                    actionRowChildren.add((ActionRowChildComponentUnion) actionComponent.asDisabled());
                                } else {
                                    actionRowChildren.add(rowChild);
                                }
                            }

                            children.add(ActionRow.of(actionRowChildren));
                        } else {
                            if (!children.isEmpty() && children.getLast() instanceof Separator) {
                                children.removeLast();
                            }
                        }
                    } else {
                        children.add(child);
                    }
                }

                components.add(Container.of(children).withAccentColor(container.getAccentColor()).withDisabled(container.isDisabled()).withSpoiler(container.isSpoiler()));
            } else if (component instanceof ActionRow row) {
                if (!removeButton) {
                    List<ActionRowChildComponentUnion> children = new ArrayList<>();

                    for (ActionRowChildComponentUnion child : row.getComponents()) {
                        if (child instanceof ActionComponent actionComponent) {
                            children.add((ActionRowChildComponentUnion) actionComponent.asDisabled());
                        } else {
                            children.add(child);
                        }
                    }

                    components.add(ActionRow.of(children).withDisabled(row.isDisabled()));
                } else {
                    if (!components.isEmpty() && components.getLast() instanceof Separator) {
                        components.removeLast();
                    }
                }
            } else {
                components.add(component);
            }
        }

        return components;
    }

    public ComponentHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);
    }

    @Override
    public final STATUS handleEvent(Event event) {
        if(event instanceof GenericComponentInteractionCreateEvent componentEvent && canHandleEvent(componentEvent)) {
            onEvent(componentEvent);
        }

        return STATUS.FINISH;
    }

    @Override
    public final Type getType() {
        return Type.COMPONENT;
    }

    public abstract void onEvent(@Nonnull GenericComponentInteractionCreateEvent event);

    private boolean canHandleEvent(GenericComponentInteractionCreateEvent event) {
        return event.getChannel().getId().equals(channelID)
                && event.getMessage().getId().equals(message.getId())
                && event.getUser().getId().equals(userID);
    }
}
