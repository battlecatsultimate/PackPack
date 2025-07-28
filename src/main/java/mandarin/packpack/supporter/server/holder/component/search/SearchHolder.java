package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class SearchHolder extends ComponentHolder {
    public enum TextType {
        TEXT,
        LIST_LABEL,
        LIST_DESCRIPTION
    }

    protected int page = 0;

    protected final String keyword;
    protected int chunk;
    protected ConfigHolder.SearchLayout layout;

    public SearchHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String keyword, ConfigHolder.SearchLayout layout, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.keyword = keyword;
        this.chunk = layout.chunkSize;
        this.layout = layout;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onExpire() {
        message.editMessageComponents(TextDisplay.of(LangID.getStringByID("ui.search.expired", lang)))
                .setAllowedMentions(new ArrayList<>())
                .useComponentsV2()
                .mentionRepliedUser(false)
                .queue(null, e ->
                    StaticStore.logger.uploadErrorLog(e,
                            ("""
                            E/SearchHolder::onExpire - Failed to edit message for expiration
                            Holder = %s
                            Channel = <#%d> [%d]
                            Message ID = %d
                            Author = <@%s> [%s]""").formatted(this.getClass(), message.getChannel().getIdLong(), message.getChannel().getIdLong(), message.getIdLong(), userID, userID)
                    )
                );
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "prev10" -> page -= 10;
            case "prev" -> page--;
            case "next" -> page++;
            case "next10" -> page += 10;
            case "data" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                int index = StaticStore.safeParseInt(e.getValues().getFirst());

                finish(event, index);

                return;
            }
            case "cancel" -> {
                cancel(event);

                return;
            }
            default -> {
                if (StaticStore.isNumeric(event.getComponentId())) {
                    finish(event, StaticStore.safeParseInt(event.getComponentId()));
                }
            }
        }

        page = Math.max(0, Math.min(getTotalPage(getDataSize(), chunk) - 1, page));

        apply(event);
    }

    @Override
    public void clean() {

    }

    @Override
    public final void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        apply(event);
    }

    public abstract List<String> accumulateTextData(TextType textType);

    public abstract void onSelected(GenericComponentInteractionCreateEvent event);

    public abstract int getDataSize();

    public void finish(GenericComponentInteractionCreateEvent event, int index) {
        onSelected(event);

        end(true);
    }

    public void cancel(GenericComponentInteractionCreateEvent event) {
        event.deferEdit().setComponents(TextDisplay.of(LangID.getStringByID("ui.search.canceled", lang)))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();

        end(true);
    }

    public Container getComponents() {
        int totalPage = getTotalPage(getDataSize(), chunk);

        List<ContainerChildComponent> children = new ArrayList<>();
        List<String> data = accumulateTextData(TextType.TEXT);

        children.add(TextDisplay.of(LangID.getStringByID("ui.search.severalResult", lang).formatted(keyword, getDataSize())));
        children.add(Separator.create(true, Separator.Spacing.LARGE));

        switch (layout) {
            case FANCY_BUTTON -> {
                for (int i = 0; i < data.size(); i++) {
                    children.add(Section.of(Button.secondary(LangID.getStringByID("ui.button.select", lang), String.valueOf(page * chunk + i)), TextDisplay.of(data.get(i))));
                }
            }
            case FANCY_LIST -> {
                for (int i = 0; i < data.size(); i++) {
                    children.add(TextDisplay.of(data.get(i)));

                    if (i < data.size() - 1) {
                        children.add(Separator.create(false, Separator.Spacing.SMALL));
                    }
                }
            }
            case COMPACTED -> {
                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < data.size(); i++) {
                    builder.append(i + 1).append(". ").append(data.get(i));

                    if (i < data.size() - 1) {
                        builder.append("\n");
                    }
                }

                children.add(TextDisplay.of("```md\n" + builder + "\n```"));
            }
        }

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(TextDisplay.of(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage)));

        if(getDataSize() > chunk) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            children.add(ActionRow.of(buttons));
        }

        if (layout == ConfigHolder.SearchLayout.COMPACTED || layout == ConfigHolder.SearchLayout.FANCY_LIST) {
            List<SelectOption> options = new ArrayList<>();

            List<String> labels = accumulateTextData(TextType.LIST_LABEL);
            List<String> descriptions = accumulateTextData(TextType.LIST_DESCRIPTION);

            for(int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                String description;

                if (descriptions == null) {
                    description = null;
                } else {
                    description = descriptions.get(i);
                }

                SelectOption option = SelectOption.of(label, String.valueOf(page * chunk + i));

                String[] elements = label.split("\\\\\\\\");

                if(elements.length == 2 && elements[0].matches("<:\\S+?:\\d+>")) {
                    option = option.withEmoji(Emoji.fromFormatted(elements[0])).withLabel(elements[1]);
                }

                if (description != null)
                    option = option.withDescription(description);

                options.add(option);
            }

            children.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectList", lang)).build()));
        }

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return Container.of(children);
    }

    protected void apply(IMessageEditCallback event) {
        event.deferEdit()
                .setComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }
}
