package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.EventDataConfigHolder;
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

public class ConfigEventManagerHolder extends ServerConfigHolder {
    private final CommonStatic.Lang.Locale locale;

    public ConfigEventManagerHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang, CommonStatic.Lang.Locale locale) {
        super(author, userID, channelID, message, holder, backup, lang);

        this.locale = locale;
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                if (e.getValues().isEmpty()) {
                    holder.eventData.computeIfAbsent(locale, l -> new EventDataConfigHolder()).channelID = -1L;

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

                    holder.eventData.computeIfAbsent(locale, l -> new EventDataConfigHolder()).channelID = channel.getIdLong();

                    applyResult(event);
                }
            }
            case "newVersionChannel" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                if (e.getValues().isEmpty()) {
                    holder.eventData.computeIfAbsent(locale, l -> new EventDataConfigHolder()).newVersionChannelID = -1L;

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

                    holder.eventData.computeIfAbsent(locale, l -> new EventDataConfigHolder()).newVersionChannelID = channel.getIdLong();

                    applyResult(event);
                }
            }
            case "newAdditional", "additional" -> {
                boolean forEventData = event.getComponentId().equals("additional");

                TextInput input = TextInput.create("message", LangID.getStringByID("serverConfig.eventData.message", lang), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(LangID.getStringByID("serverConfig.eventData.typeAdditional." + (forEventData ? "eventData" : "newVersion"), lang))
                        .setRequired(false)
                        .setRequiredRange(0, 300)
                        .build();

                Modal modal = Modal.create("additional", LangID.getStringByID("serverConfig.eventData.additionalMessage", lang))
                        .addComponents(ActionRow.of(input))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new EventAdditionalMessageHolder(getAuthorMessage(), userID, channelID, message, holder.eventData.computeIfAbsent(locale, k -> new EventDataConfigHolder()), lang, forEventData));
            }
            case "sort" -> {
                EventDataConfigHolder config = holder.eventData.computeIfAbsent(locale, k -> new EventDataConfigHolder());

                config.eventRaw = !config.eventRaw;

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
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
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
        EventDataConfigHolder config = holder.eventData.getOrDefault(locale, new EventDataConfigHolder());

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

        if (config.eventRaw) {
            sort = LangID.getStringByID("serverConfig.eventData.sortMethod.raw", lang);
        } else {
            sort = LangID.getStringByID("serverConfig.eventData.sortMethod.date", lang);
        }

        String channel;

        if (config.channelID != -1L) {
            channel = "<#" + config.channelID + ">";
        } else {
            channel = LangID.getStringByID("data.none", lang);
        }

        String newVersion;

        if (config.newVersionChannelID != -1L) {
            newVersion = "<#" + config.newVersionChannelID + ">";
        } else {
            newVersion = LangID.getStringByID("data.none", lang);
        }

        String additional;

        if (!config.eventMessage.isBlank()) {
            additional = LangID.getStringByID("serverConfig.eventData.info.content.checkBelow", lang);
        } else {
            additional = LangID.getStringByID("serverConfig.eventData.info.content.none", lang);
        }

        String newAdditional;

        if (!config.newVersionMessage.isBlank()) {
            newAdditional = LangID.getStringByID("serverConfig.eventData.info.content.checkBelow", lang);
        } else {
            newAdditional = LangID.getStringByID("serverConfig.eventData.info.content.none", lang);
        }

        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.channel.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.channel.documentation.eventData.title", lang).formatted(Emoji.fromUnicode("🗓️"))).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.description", lang)).append("\n\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.version", lang).formatted(emoji.getFormatted(), bcVersion)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.sortMethod", lang).formatted(sort)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.channel.event", lang).formatted(channel)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.channel.newVersion", lang).formatted(newVersion)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.additionalMessage.eventData", lang).formatted(additional)).append("\n")
                .append(LangID.getStringByID("serverConfig.eventData.info.additionalMessage.newVersion", lang).formatted(newAdditional));

        boolean warn = false;

        if (!config.eventMessage.isBlank()) {
            builder.append("\n\n").append(LangID.getStringByID("serverConfig.eventData.info.content.indicator.eventData", lang)).append("\n")
                    .append(config.eventMessage);

            warn |= config.eventMessage.matches("(.+)?(<@&?\\d+>|@everyone|@here)(.+)?");
        }

        if (!config.newVersionMessage.isBlank()) {
            if (config.eventMessage.isBlank()) {
                builder.append("\n");
            }

            builder.append("\n").append(LangID.getStringByID("serverConfig.eventData.info.content.indicator.newVersion", lang)).append("\n")
                    .append(config.newVersionMessage);

            warn |= config.newVersionMessage.matches("(.+)?(<@&?\\d+>|@everyone|@here)(.+)?");
        }

        if (warn) {
            Emoji warning = Emoji.fromUnicode("⚠️");

            builder.append("\n\n").append(LangID.getStringByID("serverConfig.eventData.info.content.warning", lang).formatted(warning, warning));
        }

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents() {
        EventDataConfigHolder config = holder.eventData.getOrDefault(locale, new EventDataConfigHolder());

        List<MessageTopLevelComponent> result = new ArrayList<>();

        EntitySelectMenu.Builder newVersionChannelBuilder = EntitySelectMenu.create("newVersionChannel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                .setPlaceholder(LangID.getStringByID("serverConfig.eventData.info.select.newVersion", lang));

        if (config.newVersionChannelID != -1L) {
            newVersionChannelBuilder = newVersionChannelBuilder.setDefaultValues(EntitySelectMenu.DefaultValue.channel(String.valueOf(config.newVersionChannelID))).setRequiredRange(0, 1);
        }

        EntitySelectMenu.Builder channelBuilder = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                .setPlaceholder(LangID.getStringByID("serverConfig.eventData.info.select.event", lang));

        if (config.channelID != -1L) {
            channelBuilder = channelBuilder.setDefaultValues(EntitySelectMenu.DefaultValue.channel(String.valueOf(config.channelID))).setRequiredRange(0, 1);
        }

        String sortButton;

        if (config.eventRaw) {
            sortButton = LangID.getStringByID("serverConfig.eventData.sortMethod.raw", lang);
        } else {
            sortButton = LangID.getStringByID("serverConfig.eventData.sortMethod.date", lang);
        }

        result.add(ActionRow.of(channelBuilder.build()));
        result.add(ActionRow.of(newVersionChannelBuilder.build()));

        result.add(ActionRow.of(
                Button.secondary("additional", LangID.getStringByID("serverConfig.eventData.setAdditional.eventData", lang)).withEmoji(Emoji.fromUnicode("💬")),
                Button.secondary("newAdditional", LangID.getStringByID("serverConfig.eventData.setAdditional.newVersion", lang)).withEmoji(Emoji.fromUnicode("💬"))
        ));

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
