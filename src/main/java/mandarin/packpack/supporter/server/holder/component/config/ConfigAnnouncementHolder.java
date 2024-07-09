package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.AnnouncementAdditionalMessageHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigAnnouncementHolder extends ServerConfigHolder {
    public ConfigAnnouncementHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> {
                if (!(event instanceof EntitySelectInteractionEvent e)) {
                    return;
                }

                if (e.getValues().isEmpty()) {
                    holder.ANNOUNCE = null;

                    applyResult(event);

                    return;
                }

                holder.ANNOUNCE = e.getValues().getFirst().getId();

                applyResult(event);
            }
            case "publish" -> {
                holder.publish = !holder.publish;

                applyResult(event);
            }
            case "additional" -> {
                TextInput input = TextInput.create("message", LangID.getStringByID("sercon_channeladditionalinput", lang), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(LangID.getStringByID("sercon_channeladditionalinputplace", lang))
                        .setRequired(true)
                        .setMaxLength(500)
                        .build();

                Modal modal = Modal.create("additional", LangID.getStringByID("sercon_channeladditionalmodal", lang))
                        .addActionRow(input)
                        .build();

                event.replyModal(modal).queue();

                connectTo(new AnnouncementAdditionalMessageHolder(getAuthorMessage(), channelID, message, holder));
            }
            case "back" -> goBack(event);
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("sercon_done", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("sercon_cancelask", lang), lang);

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("sercon_cancel", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    expired = true;
                }, lang));
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull ModalInteractionEvent event, @NotNull Holder child) {
        applyResult(event);
    }

    private void applyResult(ModalInteractionEvent event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("sercon_channeltitle", lang)).append("\n")
                .append(LangID.getStringByID("sercon_channelanntit", lang).formatted(Emoji.fromUnicode("📢"))).append("\n")
                .append(LangID.getStringByID("sercon_channelannmanagedesc", lang)).append("\n\n");

        String channel;

        if (holder.ANNOUNCE == null) {
            channel = LangID.getStringByID("data_none", lang);
        } else {
            channel = "<#" + holder.ANNOUNCE + ">";
        }

        String post;

        if (holder.publish) {
            post = LangID.getStringByID("button_yes", lang);
        } else {
            post = LangID.getStringByID("button_no", lang);
        }

        String additional;

        if (holder.announceMessage == null) {
            additional = LangID.getStringByID("sercon_channelnone", lang);
        } else {
            additional = LangID.getStringByID("sercon_channelcontents", lang);
        }

        builder.append(LangID.getStringByID("sercon_channelannchannel", lang).formatted(channel)).append("\n")
                .append(LangID.getStringByID("sercon_channelannpost", lang).formatted(post)).append("\n")
                .append(LangID.getStringByID("sercon_channelannadditional", lang).formatted(additional));

        if (holder.announceMessage != null) {
            builder.append("\n\n```\n")
                    .append(LangID.getStringByID("sercon_channelmessage", lang))
                    .append("\n```\n")
                    .append(holder.announceMessage)
                    .append("\n\n```\n")
                    .append("=".repeat(LangID.getStringByID("sercon_channelmessage", lang).length()))
                    .append("\n```");

            if (holder.announceMessage.matches("")) {
                Emoji warn = Emoji.fromUnicode("⚠️");

                builder.append("\n")
                        .append(LangID.getStringByID("sercon_channelannwarn", lang).formatted(warn, warn));
            }
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        EntitySelectMenu.Builder channelBuilder = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT)
                .setPlaceholder(LangID.getStringByID("sercon_channelannselect", lang));

        if (holder.ANNOUNCE != null) {
            EntitySelectMenu.DefaultValue value = EntitySelectMenu.DefaultValue.channel(holder.ANNOUNCE);

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

        result.add(ActionRow.of(Button.secondary("publish", LangID.getStringByID("sercon_channelannpostbutotn", lang)).withEmoji(publishSwitch)));
        result.add(ActionRow.of(Button.secondary("additional", LangID.getStringByID("sercon_channelannmodal", lang)).withEmoji(Emoji.fromUnicode("💬"))));

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
