package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class SearchHolder extends ComponentHolder {
    public static final int PAGE_CHUNK = 20;

    protected int page = 0;
    protected int chunk = PAGE_CHUNK;

    public SearchHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onExpire() {
        message.editMessage(LangID.getStringByID("ui.search.expired", lang))
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents()
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
                finish(event);

                return;
            }
            case "cancel" -> {
                cancel(event);

                return;
            }
        }

        page = Math.max(0, Math.min(getTotalPage(getDataSize(), chunk) - 1, page));

        apply(event);
    }

    @Override
    public void clean() {

    }

    public abstract List<String> accumulateListData(boolean onText);

    public abstract void onSelected(GenericComponentInteractionCreateEvent event);

    public abstract int getDataSize();

    public void finish(GenericComponentInteractionCreateEvent event) {
        onSelected(event);

        end(true);
    }

    public void cancel(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(LangID.getStringByID("ui.search.canceled", lang))
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();

        end(true);
    }

    protected String getPage() {
        StringBuilder sb = new StringBuilder("```md\n")
                .append(LangID.getStringByID("ui.search.selectData", lang));

        List<String> data = accumulateListData(true);

        for(int i = 0; i < data.size(); i++) {
            sb.append(i + chunk * page + 1)
                    .append(". ")
                    .append(data.get(i))
                    .append("\n");
        }

        if(getDataSize() > chunk) {
            int totalPage = getDataSize() / chunk;

            if(getDataSize() % chunk != 0)
                totalPage++;

            sb.append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage)).append("\n");
        }

        sb.append("```");

        return sb.toString();
    }

    public List<ActionRow> getComponents() {
        int totalPage = getDataSize() / chunk;

        if(getDataSize() % chunk != 0)
            totalPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(getDataSize() > chunk) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS));
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS));
            }

            if(page + 1 >= totalPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));
            }

            if(totalPage > 10) {
                if(page + 10 >= totalPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
                }
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        List<String> data = accumulateListData(false);

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:\\S+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(page * chunk + i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(page * chunk + i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(page * chunk + i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectList", lang)).build()));

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return rows;
    }

    protected void apply(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getPage())
                .setComponents(getComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }
}
