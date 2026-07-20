package mandarin.packpack.supporter.server.holder.component.booster;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.modal.BoosterEmojiModalHolder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoosterEmojiListHolder extends ComponentHolder {
    private final Guild g;

    private final IDHolder holder;
    private final BoosterHolder boosterHolder;

    private int page;
    private List<Map.Entry<Long, BoosterData>> boosterData;

    public BoosterEmojiListHolder(@Nullable Message author, long userID, long channelID, @NotNull Guild g, @NotNull Message message, @NotNull IDHolder holder,  @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.g = g;

        this.holder = holder;

        boosterHolder = StaticStore.boosterData.computeIfAbsent(g.getIdLong(), _ -> new BoosterHolder());
        boosterData = boosterHolder.serverBooster.entrySet().stream().filter(e -> e.getValue().getEmoji() != -1L).toList();
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "emoji" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                Map.Entry<Long, BoosterData> entry = boosterData.get(StaticStore.safeParseInt(e.getValues().getFirst()));

                g.retrieveMember(UserSnowflake.fromId(entry.getKey())).queue(targetMember ->
                        connectTo(event, new BoosterEmojiModifyHolder(getAuthorMessage(), userID, channelID, message, targetMember, g, boosterHolder, entry.getValue(), lang)))
                ;
            }
            case "create" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                Member targetMember = e.getMentions().getMembers().getFirst();

                if (targetMember.getUser().isBot()) {
                    event.deferReply()
                            .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.noRobot", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .setEphemeral(true)
                            .queue();

                    return;
                }

                if (!targetMember.getRoles().stream().map(ISnowflake::getIdLong).toList().contains(holder.booster)) {
                    event.deferReply()
                            .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.notBooster", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .setEphemeral(true)
                            .queue();

                    return;
                }

                BoosterData data = boosterHolder.serverBooster.get(targetMember.getIdLong());

                if (data != null && data.getEmoji() != -1L) {
                    event.deferReply()
                            .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.alreadyAssigned", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .setEphemeral(true)
                            .queue();

                    return;
                }

                AttachmentUpload upload = AttachmentUpload.create("emojiFile")
                        .setRequired(true)
                        .setRequiredRange(1, 1)
                        .build();

                Modal modal = Modal.create("emoji", LangID.getStringByID("boosterEmoji.upload.title", lang))
                        .addComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.upload.description", lang)))
                        .addComponents(Label.of(LangID.getStringByID("boosterEmoji.upload.file", lang), upload))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new BoosterEmojiModalHolder(getAuthorMessage(), userID, channelID, message, targetMember, g, boosterHolder, lang));
            }
            case "close" -> {
                event.deferEdit().setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.closed", lang)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
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
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessageComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.expired", lang)))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) throws Exception {
        boosterData = boosterHolder.serverBooster.entrySet().stream().filter(e -> e.getValue().getEmoji() != -1L).toList();

        applyResult(event);
    }

    @Override
    public void onBack(@NotNull Holder child) throws Exception {
        boosterData = boosterHolder.serverBooster.entrySet().stream().filter(e -> e.getValue().getEmoji() != -1L).toList();

        applyResult();
    }

    private void applyResult() {
        boosterData = boosterHolder.serverBooster.entrySet().stream().filter(e -> e.getValue().getEmoji() != -1L).toList();

        message.editMessageComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult(IMessageEditCallback event) {
        boosterData = boosterHolder.serverBooster.entrySet().stream().filter(e -> e.getValue().getEmoji() != -1L).toList();

        event.deferEdit()
                .setComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private Container getComponents() {
        List<ContainerChildComponent> children = new ArrayList<>();

        children.add(TextDisplay.of(
                "## " + LangID.getStringByID("boosterEmoji.title", lang) + "\n" +
                        LangID.getStringByID("boosterEmoji.explanation", lang) + "\n\n" +
                        LangID.getStringByID("boosterEmoji.select", lang)
        ));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        int size = Math.min(boosterData.size(), (page + 1) * ConfigHolder.SearchLayout.FANCY_LIST.chunkSize);

        if (boosterData.isEmpty()) {
            children.add(TextDisplay.of("### " + LangID.getStringByID("boosterEmoji.noMember", lang)));
        } else {
            for (int i = page * ConfigHolder.SearchLayout.FANCY_LIST.chunkSize; i < size; i++) {
                Map.Entry<Long, BoosterData> entry = boosterData.get(i);
                Emoji emoji = g.getEmojiById(entry.getValue().getEmoji());

                if (emoji == null)
                    continue;

                children.add(TextDisplay.of(
                        LangID.getStringByID("boosterEmoji.list.member.text", lang).formatted(i, entry.getKey(), entry.getKey()) + "\n" +
                                "  " + LangID.getStringByID("boosterEmoji.list.emoji", lang).formatted(emoji.getFormatted(), emoji.getName(), entry.getValue().getEmoji())
                ));

                if (i < size - 1) {
                    children.add(Separator.create(false, Separator.Spacing.SMALL));
                }
            }
        }

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        if (boosterData.size() > ConfigHolder.SearchLayout.FANCY_LIST.chunkSize) {
            int totalPage = SearchHolder.getTotalPage(boosterData.size(), ConfigHolder.SearchLayout.FANCY_LIST.chunkSize);

            children.add(TextDisplay.of(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage)));

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

            children.add(Separator.create(false, Separator.Spacing.SMALL));
        }

        List<SelectOption> options = new ArrayList<>();

        if (size == 0) {
            options.add(SelectOption.of("A", "A"));
        } else {
            for (int i = page * ConfigHolder.SearchLayout.FANCY_LIST.chunkSize; i < size; i++) {
                Map.Entry<Long, BoosterData> entry = boosterData.get(i);
                Emoji emoji = g.getEmojiById(entry.getValue().getEmoji());

                if (emoji == null)
                    continue;

                User u = g.getJDA().getUserById(entry.getKey());

                if (u == null)
                    continue;

                options.add(SelectOption.of(LangID.getStringByID("boosterEmoji.list.member.selectMenu", lang).formatted(i + 1, u.getGlobalName(), entry.getKey()), String.valueOf(i)).withEmoji(emoji));
            }
        }

        children.add(ActionRow.of(StringSelectMenu.create("emoji").addOptions(options).setPlaceholder(LangID.getStringByID("boosterEmoji.list.edit", lang)).setRequiredRange(1, 1).setDisabled(boosterData.isEmpty()).build()));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        EntitySelectMenu memberMenu = EntitySelectMenu.create("create", EntitySelectMenu.SelectTarget.USER)
                .setRequiredRange(1, 1)
                .setPlaceholder(LangID.getStringByID("boosterEmoji.list.add", lang))
                .build();

        children.add(ActionRow.of(memberMenu));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(ActionRow.of(Button.danger("close", LangID.getStringByID("ui.button.close", lang)).withEmoji(EmojiStore.CROSS)));

        return Container.of(children);
    }
}
