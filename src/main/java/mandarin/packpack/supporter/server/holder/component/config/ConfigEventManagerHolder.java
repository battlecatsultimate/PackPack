package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.EventAdditionalMessageHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
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

public class ConfigEventManagerHolder extends ServerConfigHolder {
    private final CommonStatic.Lang.Locale locale;

    public ConfigEventManagerHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, CommonStatic.Lang.Locale lang, CommonStatic.Lang.Locale locale) {
        super(author, channelID, message, holder, backup, lang);

        this.locale = locale;
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                if (e.getValues().isEmpty()) {
                    holder.eventMap.remove(locale);

                    applyResult(event);
                } else {
                    String id = e.getValues().getFirst().getId();

                    Guild g = event.getGuild();

                    if (g == null)
                        return;

                    TextChannel channel = g.getTextChannelById(id);

                    if (channel == null)
                        return;

                    if (!channel.canTalk()) {
                        event.deferReply()
                                .setContent(LangID.getStringByID("serverConfig.eventData.cantTalk", lang))
                                .setAllowedMentions(new ArrayList<>())
                                .setEphemeral(true)
                                .queue();

                        return;
                    }

                    holder.eventMap.put(locale, id);

                    applyResult(event);
                }
            }
            case "additional" -> {
                TextInput input = TextInput.create("message", LangID.getStringByID("serverConfig.eventData.message", lang), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(LangID.getStringByID("serverConfig.eventData.typeAdditional", lang))
                        .setRequired(false)
                        .setRequiredRange(0, 500)
                        .build();

                Modal modal = Modal.create("additional", LangID.getStringByID("serverConfig.eventData.additionalMessage", lang))
                        .addActionRow(input)
                        .build();

                event.replyModal(modal).queue();

                connectTo(new EventAdditionalMessageHolder(getAuthorMessage(), channelID, message, holder, lang, locale));
            }
            case "sort" -> {
                holder.eventRaw = !holder.eventRaw;

                applyResult(event);
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

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
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
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
        applyResult(event);
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) {
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
        Emoji emoji;
        String bcVersion;

        switch (locale) {
            case EN -> {
                emoji = Emoji.fromUnicode("🇺🇸");
                bcVersion = LangID.getStringByID("serverConfig.eventData.version.en", lang);
            }
            case JP -> {
                emoji = Emoji.fromUnicode("🇯🇵");
                bcVersion = LangID.getStringByID("serverConfig.eventData.version.jp", lang);
            }
            case ZH -> {
                emoji = Emoji.fromUnicode("🇹🇼");
                bcVersion = LangID.getStringByID("serverConfig.eventData.version.tw", lang);
            }
            case KR -> {
                emoji = Emoji.fromUnicode("🇰🇷");
                bcVersion = LangID.getStringByID("serverConfig.eventData.version.kr", lang);
            }
            default -> throw new IllegalStateException("E/ConfigEventManagerHolder::getComponents - Unknown locale type %s found".formatted(locale));
        }

        String sort;

        if (holder.eventRaw) {
            sort = LangID.getStringByID("serverConfig.eventData.sortMethod.raw", lang);
        } else {
            sort = LangID.getStringByID("serverConfig.eventData.sortMethod.date", lang);
        }

        String channel;

        if (holder.eventMap.containsKey(locale)) {
            channel = "<#" + holder.eventMap.get(locale) + ">";
        } else {
            channel = LangID.getStringByID("data.none", lang);
        }

        String additional;

        if (holder.eventMessage.containsKey(locale)) {
            additional = LangID.getStringByID("serverConfig.eventData.info.content.checkBelow", lang);
        } else {
            additional = LangID.getStringByID("serverConfig.eventData.info.content.none", lang);
        }

        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.channel.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.channel.documentation.eventData.title", lang).formatted(Emoji.fromUnicode("🗓️"))).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.description", lang)).append("\n\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.version", lang).formatted(emoji.getFormatted(), bcVersion)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.sortMethod", lang).formatted(sort)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.channel", lang).formatted(channel)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.additionalMessage", lang).formatted(additional));

        if (holder.eventMessage.containsKey(locale)) {
            String message = holder.eventMessage.get(locale);

            builder.append("\n\n```\n")
                    .append(LangID.getStringByID("serverConfig.eventData.info.content.indicator", lang))
                    .append("\n```\n")
                    .append(message)
                    .append("\n\n```\n")
                    .append("=".repeat(LangID.getStringByID("serverConfig.eventData.info.content.indicator", lang).length()))
                    .append("\n```");

            if (message.matches("(.+)?(<@&?\\d+>|@everyone|@here)(.+)?")) {
                Emoji warning = Emoji.fromUnicode("⚠️");

                builder.append("\n\n").append(LangID.getStringByID("serverConfig.eventData.info.content.warning", lang).formatted(warning, warning));
            }
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        EntitySelectMenu.Builder channelBuilder = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL).setChannelTypes(ChannelType.TEXT);

        if (holder.eventMap.containsKey(locale)) {
            String channelID = holder.eventMap.get(locale);

            channelBuilder = channelBuilder.setDefaultValues(EntitySelectMenu.DefaultValue.channel(channelID)).setRequiredRange(0, 1);
        }

        String sortButton;

        if (holder.eventRaw) {
            sortButton = LangID.getStringByID("serverConfig.eventData.sortMethod.raw", lang);
        } else {
            sortButton = LangID.getStringByID("serverConfig.eventData.sortMethod.date", lang);
        }

        result.add(ActionRow.of(channelBuilder.build()));
        result.add(ActionRow.of(Button.secondary("additional", LangID.getStringByID("serverConfig.eventData.setAdditional", lang)).withEmoji(Emoji.fromUnicode("💬"))));
        result.add(ActionRow.of(Button.secondary("sort", LangID.getStringByID("serverConfig.eventData.setSort", lang).formatted(sortButton)).withEmoji(Emoji.fromUnicode("⚖️"))));

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
