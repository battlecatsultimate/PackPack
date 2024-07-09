package mandarin.packpack.supporter.server.holder.component.config;

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

public class ConfigEventManagerHolder extends ServerConfigHolder {
    private final int locale;

    public ConfigEventManagerHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang, int locale) {
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
                                .setContent(LangID.getStringByID("sercon_channelcanttalk", lang))
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
                TextInput input = TextInput.create("message", LangID.getStringByID("sercon_channeladditionalinput", lang), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(LangID.getStringByID("sercon_channeladditionalinputplace", lang))
                        .setRequired(true)
                        .setMaxLength(500)
                        .build();

                Modal modal = Modal.create("additional", LangID.getStringByID("sercon_channeladditionalmodal", lang))
                        .addActionRow(input)
                        .build();

                event.replyModal(modal).queue();

                String localeCode;

                switch (locale) {
                    case LangID.JP -> localeCode = "jp";
                    case LangID.ZH -> localeCode = "tw";
                    case LangID.KR -> localeCode = "kr";
                    case LangID.EN -> localeCode = "en";
                    default -> throw new IllegalStateException("E/ConfigEventManagerHolder::onEvent - Unknown locale type %d".formatted(locale));
                }

                connectTo(new EventAdditionalMessageHolder(getAuthorMessage(), channelID, message, holder, localeCode));
            }
            case "sort" -> {
                holder.eventRaw = !holder.eventRaw;

                applyResult(event);
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
    public void onBack(@NotNull ModalInteractionEvent event, @NotNull Holder child) {
        applyResult(event);
    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
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
        Emoji emoji;
        String bcVersion;
        String localeCode;

        switch (locale) {
            case LangID.EN -> {
                emoji = Emoji.fromUnicode("🇺🇸");
                bcVersion = LangID.getStringByID("sercon_channeleventen", lang);
                localeCode = "en";
            }
            case LangID.JP -> {
                emoji = Emoji.fromUnicode("🇯🇵");
                bcVersion = LangID.getStringByID("sercon_channeleventjp", lang);
                localeCode = "jp";
            }
            case LangID.ZH -> {
                emoji = Emoji.fromUnicode("🇹🇼");
                bcVersion = LangID.getStringByID("sercon_channeleventtw", lang);
                localeCode = "zh";
            }
            case LangID.KR -> {
                emoji = Emoji.fromUnicode("🇰🇷");
                bcVersion = LangID.getStringByID("sercon_channeleventkr", lang);
                localeCode = "kr";
            }
            default -> throw new IllegalStateException("E/ConfigEventManagerHolder::getComponents - Unknown locale type %s found".formatted(locale));
        }

        String sort;

        if (holder.eventRaw) {
            sort = LangID.getStringByID("sercon_channeleventraw", lang);
        } else {
            sort = LangID.getStringByID("sercon_channeleventdate", lang);
        }

        String channel;

        if (holder.eventMap.containsKey(locale)) {
            channel = "<#" + holder.eventMap.get(locale) + ">";
        } else {
            channel = LangID.getStringByID("data_none", lang);
        }

        String additional;

        if (holder.eventMessage.containsKey(localeCode)) {
            additional = LangID.getStringByID("sercon_channelcontents", lang);
        } else {
            additional = LangID.getStringByID("sercon_channelnone", lang);
        }

        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("sercon_channeltitle", lang)).append("\n")
                .append(LangID.getStringByID("sercon_channeleventtit", lang).formatted(Emoji.fromUnicode("🗓️"))).append("\n")
                .append(LangID.getStringByID("sercon_channeleventmanagedesc", lang)).append("\n\n")
                .append(LangID.getStringByID("sercon_channeleventbcver", lang).formatted(emoji.getFormatted(), bcVersion)).append("\n")
                .append(LangID.getStringByID("sercon_channeleventsort", lang).formatted(sort)).append("\n")
                .append(LangID.getStringByID("sercon_channeleventchannel", lang).formatted(channel)).append("\n")
                .append(LangID.getStringByID("sercon_channeleventadditional", lang).formatted(additional));

        if (holder.eventMessage.containsKey(localeCode)) {
            String message = holder.eventMessage.get(localeCode);

            builder.append("\n\n```\n")
                    .append(LangID.getStringByID("sercon_channelmessage", lang))
                    .append("\n```\n")
                    .append(message)
                    .append("\n\n```\n")
                    .append("=".repeat(LangID.getStringByID("sercon_channeleventmessage", lang).length()))
                    .append("\n```");

            if (message.matches("(.+)?(<@&?\\d+>|@everyone|@here)(.+)?")) {
                Emoji warning = Emoji.fromUnicode("⚠️");

                builder.append("\n\n").append(LangID.getStringByID("sercon_channeleventwarn", lang).formatted(warning, warning));
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
            sortButton = LangID.getStringByID("sercon_channeleventraw", lang);
        } else {
            sortButton = LangID.getStringByID("sercon_channeleventdate", lang);
        }

        result.add(ActionRow.of(channelBuilder.build()));
        result.add(ActionRow.of(Button.secondary("additional", LangID.getStringByID("sercon_channeladditional", lang)).withEmoji(Emoji.fromUnicode("💬"))));
        result.add(ActionRow.of(Button.secondary("sort", LangID.getStringByID("sercon_channeleventsortbutton", lang).formatted(sortButton)).withEmoji(Emoji.fromUnicode("⚖️"))));

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
