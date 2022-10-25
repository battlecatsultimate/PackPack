package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class SearchHolder extends InteractionHolder<GenericComponentInteractionCreateEvent> {
    public static final int PAGE_CHUNK = 20;

    protected final Message msg;
    protected final String channelID;
    protected final String memberID;
    protected final int lang;

    protected int page = 0;

    public SearchHolder(@Nonnull Message msg, @Nonnull Message author, @Nonnull String channelID, int lang) {
        super(GenericComponentInteractionCreateEvent.class, author);

        this.msg = msg;
        this.channelID = channelID;
        this.memberID = author.getAuthor().getId();
        this.lang = lang;
    }

    @Override
    public int handleEvent(GenericComponentInteractionCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");

            return RESULT_FAIL;
        }

        MessageChannel ch = event.getChannel();

        if(!ch.getId().equals(channelID))
            return RESULT_STILL;

        Member member = event.getInteraction().getMember();

        if(member == null || !member.getId().equals(memberID))
            return RESULT_STILL;

        Message m = event.getMessage();

        if(!msg.getId().equals(m.getId()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.editMessage(LangID.getStringByID("formst_expire", lang))
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents()
                .queue();
    }

    @Override
    public void performInteraction(GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "prev10":
                page -= 10;

                break;
            case "prev":
                page--;

                break;
            case "next":
                page++;

                break;
            case "next10":
                page += 10;

                break;
            case "data":
                finish(event);

                return;
            case "cancel":
                cancel(event);

                return;
        }

        apply(event);
    }

    @Override
    public void clean() {

    }

    public abstract List<String> accumulateListData(boolean onText);

    public abstract void onSelected(GenericComponentInteractionCreateEvent event);

    public abstract int getDataSize();

    public void finish(GenericComponentInteractionCreateEvent event) {
        expired = true;

        StaticStore.removeHolder(event.getUser().getId(), this);

        onSelected(event);
    }

    public void cancel(GenericComponentInteractionCreateEvent event) {
        expired = true;

        StaticStore.removeHolder(event.getUser().getId(), this);

        event.deferEdit()
                .setContent(LangID.getStringByID("formst_cancel", lang))
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .complete();
    }

    protected String getPage() {
        StringBuilder sb = new StringBuilder("```md\n")
                .append(LangID.getStringByID("formst_pick", lang));

        List<String> data = accumulateListData(true);

        for(int i = 0; i < data.size(); i++) {
            sb.append(i + PAGE_CHUNK * page + 1)
                    .append(". ")
                    .append(data.get(i))
                    .append("\n");
        }

        if(getDataSize() > PAGE_CHUNK) {
            int totalPage = getDataSize() / PAGE_CHUNK;

            if(getDataSize() % PAGE_CHUNK != 0)
                totalPage++;

            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page + 1)).replace("-", String.valueOf(totalPage))).append("\n");
        }

        sb.append("```");

        return sb.toString();
    }

    public List<ActionRow> getComponents() {
        int totalPage = getDataSize() / PAGE_CHUNK;

        if(getDataSize() % PAGE_CHUNK != 0)
            totalPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(getDataSize() > PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), Emoji.fromCustom(EmojiStore.TWO_PREVIOUS)).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), Emoji.fromCustom(EmojiStore.TWO_PREVIOUS)));
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), Emoji.fromCustom(EmojiStore.PREVIOUS)).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), Emoji.fromCustom(EmojiStore.PREVIOUS)));
            }

            if(page + 1 >= totalPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), Emoji.fromCustom(EmojiStore.NEXT)).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), Emoji.fromCustom(EmojiStore.NEXT)));
            }

            if(totalPage > 10) {
                if(page + 10 >= totalPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), Emoji.fromCustom(EmojiStore.TWO_NEXT)).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), Emoji.fromCustom(EmojiStore.TWO_NEXT)));
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
                if(elements[0].matches("<:[^\\s]+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(page * PAGE_CHUNK + i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(page * PAGE_CHUNK + i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(page * PAGE_CHUNK + i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("search_list", lang)).build()));

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));

        return rows;
    }

    protected void apply(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getPage())
                .setComponents(getComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .complete();
    }
}
