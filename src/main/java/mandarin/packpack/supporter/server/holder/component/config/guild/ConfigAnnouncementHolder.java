package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.AnnouncementAdditionalMessageHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigAnnouncementHolder extends ServerConfigHolder {
    public ConfigAnnouncementHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> {
                if (!(event instanceof EntitySelectInteractionEvent e)) {
                    return;
                }

                if (e.getValues().isEmpty()) {
                    holder.announceChannel = null;

                    applyResult(event);

                    return;
                }

                holder.announceChannel = e.getValues().getFirst().getId();

                applyResult(event);
            }
            case "publish" -> {
                holder.publish = !holder.publish;

                applyResult(event);
            }
            case "additional" -> {
                TextInput input = TextInput.create("message", LangID.getStringByID("serverConfig.eventData.message", lang), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(LangID.getStringByID("serverConfig.eventData.typeAdditional", lang))
                        .setRequired(false)
                        .setRequiredRange(0, 500)
                        .build();

                Modal modal = Modal.create("additional", LangID.getStringByID("serverConfig.eventData.additionalMessage", lang))
                        .addComponents(ActionRow.of(input))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new AnnouncementAdditionalMessageHolder(getAuthorMessage(), userID, channelID, message, holder, lang));
            }
            case "back" -> goBack(event);
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    end(true);
                }, lang));
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.channel.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.channel.documentation.announcement.title", lang).formatted(Emoji.fromUnicode("📢"))).append("\n")
                .append(LangID.getStringByID("serverConfig.announcement.description", lang)).append("\n\n");

        String channel;

        if (holder.announceChannel == null) {
            channel = LangID.getStringByID("data.none", lang);
        } else {
            channel = "<#" + holder.announceChannel + ">";
        }

        String post;

        if (holder.publish) {
            post = LangID.getStringByID("ui.button.yes", lang);
        } else {
            post = LangID.getStringByID("ui.button.no", lang);
        }

        String additional;

        if (holder.announceMessage.isBlank()) {
            additional = LangID.getStringByID("serverConfig.eventData.info.content.none", lang);
        } else {
            additional = LangID.getStringByID("serverConfig.eventData.info.content.checkBelow", lang);
        }

        builder.append(LangID.getStringByID("serverConfig.announcement.info.channel", lang).formatted(channel)).append("\n")
                .append(LangID.getStringByID("serverConfig.announcement.info.publish", lang).formatted(post)).append("\n")
                .append(LangID.getStringByID("serverConfig.announcement.info.additionalMessage", lang).formatted(additional));

        if (!holder.announceMessage.isBlank()) {
            builder.append("\n\n```\n")
                    .append(LangID.getStringByID("serverConfig.eventData.info.content.indicator", lang))
                    .append("\n```\n")
                    .append(holder.announceMessage)
                    .append("\n\n```\n")
                    .append("=".repeat(LangID.getStringByID("serverConfig.eventData.info.content.indicator", lang).length()))
                    .append("\n```");

            if (holder.announceMessage.matches("")) {
                Emoji warn = Emoji.fromUnicode("⚠️");

                builder.append("\n")
                        .append(LangID.getStringByID("serverConfig.announcement.info.content.warning", lang).formatted(warn, warn));
            }
        }

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        EntitySelectMenu.Builder channelBuilder = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                .setPlaceholder(LangID.getStringByID("serverConfig.announcement.selectChannel", lang));

        if (holder.announceChannel != null) {
            EntitySelectMenu.DefaultValue value = EntitySelectMenu.DefaultValue.channel(holder.announceChannel);

            channelBuilder = channelBuilder.setDefaultValues(value);
        }

        result.add(ActionRow.of(
            channelBuilder.build()
        ));

        Emoji publishSwitch;

        if (holder.publish) {
            publishSwitch = EmojiStore.SWITCHON;
        } else {
            publishSwitch = EmojiStore.SWITCHOFF;
        }

        result.add(ActionRow.of(Button.secondary("publish", LangID.getStringByID("serverConfig.announcement.button.publish", lang)).withEmoji(publishSwitch)));
        result.add(ActionRow.of(Button.secondary("additional", LangID.getStringByID("serverConfig.announcement.button.additional", lang)).withEmoji(Emoji.fromUnicode("💬"))));

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
