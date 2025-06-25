package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.modal.PrefixBanHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigPrefixBanHolder extends ServerConfigHolder {
    private int page = 0;

    public ConfigPrefixBanHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "assign" -> {
                TextInput input = TextInput.create("prefix", LangID.getStringByID("serverConfig.prefixBan.modal.field", lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("serverConfig.prefixBan.modal.description", lang))
                        .setMaxLength(SelectOption.LABEL_MAX_LENGTH)
                        .build();

                Modal modal = Modal.create("prefixBan", LangID.getStringByID("serverConfig.prefixBan.modal.title", lang))
                        .addActionRow(input)
                        .build();

                event.replyModal(modal).queue();

                connectTo(new PrefixBanHolder(getAuthorMessage(), userID, channelID, message, lang, holder));
            }
            case "prefix" -> {
                if (!(event instanceof StringSelectInteractionEvent e)) {
                    return;
                }

                String value = e.getValues().getFirst();

                if (!StaticStore.isNumeric(value)) {
                    return;
                }

                int index = StaticStore.safeParseInt(value);

                holder.bannedPrefix.remove(index);

                applyResult(event);
            }
            case "disallow" -> {
                holder.disableCustomPrefix = !holder.disableCustomPrefix;

                applyResult(event);
            }
            case "prev10" -> {
                page -= 10;

                applyResult(event);
            }
            case "prev" -> {
                page--;

                applyResult(event);
            }
            case "next" -> {
                page++;

                applyResult(event);
            }
            case "next10" -> {
                page += 10;

                applyResult(event);
            }
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
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull Holder child) {
        applyResult();
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult() {
        message.editMessage(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.permission.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.permission.documentation.prefixBan.title", lang).formatted(Emoji.fromUnicode("📋"))).append("\n")
                .append(LangID.getStringByID("serverConfig.prefixBan.description", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.prefixBan.prefixList.list", lang)).append("\n```\n");

        if (holder.disableCustomPrefix) {
            builder.append(LangID.getStringByID("serverConfig.prefixBan.prefixList.all", lang)).append("\n```");
        } else if (holder.bannedPrefix.isEmpty()) {
            builder.append(LangID.getStringByID("serverConfig.prefixBan.prefixList.no", lang)).append("\n```");
        } else {
            for (int i = page * SearchHolder.PAGE_CHUNK; i < Math.min((page + 1) * SearchHolder.PAGE_CHUNK, holder.bannedPrefix.size()); i++) {
                builder.append(i + 1).append(". ").append(holder.bannedPrefix.get(i)).append("\n");
            }

            builder.append("```");
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(
                Button.secondary("assign", LangID.getStringByID("serverConfig.prefixBan.assign", lang)).withEmoji(Emoji.fromUnicode("❌"))
                        .withDisabled(holder.disableCustomPrefix)
        ));

        List<SelectOption> prefixOptions = new ArrayList<>();

        if (holder.bannedPrefix.isEmpty()) {
            prefixOptions.add(SelectOption.of("a", "a"));
        } else {
            for (int i = page * SearchHolder.PAGE_CHUNK; i < Math.min((page + 1) * SearchHolder.PAGE_CHUNK, holder.bannedPrefix.size()); i++) {
                String label = holder.bannedPrefix.get(i);

                if (label.length() >= SelectOption.LABEL_MAX_LENGTH) {
                    label = label.substring(0, SelectOption.LABEL_MAX_LENGTH);
                }

                prefixOptions.add(SelectOption.of(label, String.valueOf(i)));
            }
        }

        String placeholder;

        if (holder.bannedPrefix.isEmpty()) {
            placeholder = LangID.getStringByID("serverConfig.prefixBan.noPrefix", lang);
        } else {
            placeholder = LangID.getStringByID("serverConfig.prefixBan.remove", lang);
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("prefix")
                        .addOptions(prefixOptions)
                        .setPlaceholder(placeholder)
                        .setDisabled(holder.disableCustomPrefix || holder.bannedPrefix.isEmpty())
                        .build()
        ));

        if (holder.bannedPrefix.size() > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            int totalPage = getTotalPage(holder.bannedPrefix.size());

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            result.add(ActionRow.of(buttons));
        }

        Emoji disallowed;

        if (holder.disableCustomPrefix) {
            disallowed = EmojiStore.SWITCHON;
        } else {
            disallowed = EmojiStore.SWITCHOFF;
        }

        result.add(ActionRow.of(Button.secondary("disallow", LangID.getStringByID("serverConfig.prefixBan.disallow", lang)).withEmoji(disallowed)));

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
