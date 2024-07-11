package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.modal.CustomRoleAssignHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CustomIDManagerHolder extends ComponentHolder {
    private final IDHolder holder;
    private final Guild g;

    private int page;

    public CustomIDManagerHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message msg, @NotNull IDHolder holder, @NotNull Guild g, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);

        this.holder = holder;
        this.g = g;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        message.editMessage(LangID.getStringByID("idset_expire", holder.config.lang))
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        if (lang == null)
            return;

        switch (event.getComponentId()) {
            case "customAdd" -> {
                TextInput name = TextInput.create("name", LangID.getStringByID("idset_name", lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("idset_nameplace", lang))
                        .setMaxLength(32)
                        .setRequired(true)
                        .build();

                TextInput role = TextInput.create("role", LangID.getStringByID("idset_role", lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("idset_roleplace", lang))
                        .setRequired(true)
                        .setMaxLength(32)
                        .build();

                Modal modal = Modal.create("custom", LangID.getStringByID("idset_customrole", lang))
                        .addActionRow(name)
                        .addActionRow(role)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new CustomRoleAssignHolder(getAuthorMessage(), channelID, message, () -> message.editMessage(getManagerText())
                        .setComponents(getManagerComponents())
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue(), holder, g, lang));
            }
            case "customRemove" -> {
                if (event instanceof StringSelectInteractionEvent e) {
                    for (String name : e.getValues()) {
                        holder.ID.remove(name);
                    }

                    event.deferReply(true)
                            .setContent(LangID.getStringByID("idset_customremoved", lang))
                            .queue();

                    int totalPage = holder.ID.size() / SearchHolder.PAGE_CHUNK;

                    if(holder.ID.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    page = Math.min(Math.max(0, page), totalPage - 1);

                    message.editMessage(getManagerText())
                            .setComponents(getManagerComponents())
                            .mentionRepliedUser(false)
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                }
            }
            case "back" -> {
                expired = true;

                event.deferEdit()
                        .setContent(getPreviousMessage())
                        .setComponents(getPreviousComponents())
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();

                StaticStore.putHolder(getAuthorMessage().getAuthor().getId(), new IDManagerHolder(getAuthorMessage(), channelID, message, holder, g, lang));
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

    private void applyResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getManagerText())
                .setComponents(getManagerComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private String getManagerText() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        StringBuilder builder = new StringBuilder(LangID.getStringByID("idset_managetitle", lang)).append("\n\n");

        int i = 0;

        for (String key : holder.ID.keySet()) {
            if (i < SearchHolder.PAGE_CHUNK * page) {
                i++;

                continue;
            }

            if (i == SearchHolder.PAGE_CHUNK * (page + 1))
                break;

            builder.append(i + 1).append(". ").append(key).append(" : ");

            String id = holder.ID.get(key);

            if (id == null) {
                builder.append("UNKNOWN");
            } else {
                builder.append("<@&").append(id).append("> [").append(id).append("]");
            }

            builder.append("\n");

            i++;
        }

        if(holder.ID.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = holder.ID.size() / SearchHolder.PAGE_CHUNK;

            if(holder.ID.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            builder.append("\n").append(LangID.getStringByID("formst_page", lang).formatted(page + 1, totalPage));
        }

        return builder.toString();
    }

    private String getPreviousMessage() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        String[] data = { "moderator", "member", "booster" };
        String[] ids = { holder.MOD, holder.MEMBER, holder.BOOSTER };

        StringBuilder result = new StringBuilder();

        for(int i = 0; i < data.length; i++) {
            result.append("**")
                    .append(LangID.getStringByID("idset_" + data[i], lang))
                    .append("** : ");

            if(ids[i] == null) {
                result.append(LangID.getStringByID(i == 1 ? "data_everyone" : "data_none", lang));
            } else {
                Role r = getRoleSafelyWithID(ids[i]);

                if (r == null) {
                    result.append(LangID.getStringByID(i == 1 ? "data_everyone" : "data_none", lang));
                } else {
                    result.append(r.getId())
                            .append(" [")
                            .append(r.getAsMention())
                            .append("]");
                }
            }

            if(i < data.length - 1) {
                result.append("\n\n");
            }
        }

        return result.toString();
    }

    private List<LayoutComponent> getManagerComponents() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        List<LayoutComponent> result = new ArrayList<>();

        Button b = Button.secondary("customAdd", LangID.getStringByID("idset_customadd", lang)).withEmoji(Emoji.fromUnicode("➕"));

        result.add(ActionRow.of(b));

        if (!holder.ID.isEmpty()) {
            List<SelectOption> options = new ArrayList<>();

            int i = 0;

            for(String name : holder.ID.keySet()) {
                if (i < SearchHolder.PAGE_CHUNK * page) {
                    i++;

                    continue;
                }

                if (i == SearchHolder.PAGE_CHUNK * (page + 1))
                    break;

                String id = holder.ID.get(name);

                if(id == null) {
                    options.add(SelectOption.of(name, name).withDescription(LangID.getStringByID("idset_none", lang)));
                } else {
                    Role r = getRoleSafelyWithID(id);

                    if(r == null) {
                        options.add(SelectOption.of(name, name).withDescription(String.format(LangID.getStringByID("idset_unknown", lang), id)));
                    } else {
                        options.add(SelectOption.of(name, name).withDescription(id));
                    }
                }

                i++;
            }

            result.add(ActionRow.of(StringSelectMenu.create("customRemove").addOptions(options).setRequiredRange(0, 20).setPlaceholder(LangID.getStringByID("idset_customremove", lang)).build()));
        }

        if (holder.ID.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = holder.ID.size() / SearchHolder.PAGE_CHUNK;

            if(holder.ID.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS));
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS));
            }

            if(page + 1 >= totalPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT));
            }

            if(totalPage > 10) {
                if(page + 10 >= totalPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT));
                }
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(ActionRow.of(Button.primary("back", LangID.getStringByID("button_back", lang))));

        return result;
    }

    private List<LayoutComponent> getPreviousComponents() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        List<LayoutComponent> result = new ArrayList<>();

        List<ActionComponent> pages = new ArrayList<>();

        pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
        pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));

        result.add(ActionRow.of(
                EntitySelectMenu.create("mod", EntitySelectMenu.SelectTarget.ROLE)
                        .setRequiredRange(1, 1)
                        .setPlaceholder(LangID.getStringByID("idset_modselect", lang))
                        .build()));

        result.add(ActionRow.of(
                EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.ROLE)
                        .setRequiredRange(0, 1)
                        .setPlaceholder(LangID.getStringByID("idset_memberselect", lang))
                        .build()));

        result.add(ActionRow.of(
                EntitySelectMenu.create("booster", EntitySelectMenu.SelectTarget.ROLE)
                        .setRequiredRange(0, 1)
                        .setPlaceholder(LangID.getStringByID("idset_boosterselect", lang))
                        .build()));

        result.add(ActionRow.of(pages));

        result.add(ActionRow.of(Button.primary("confirm", LangID.getStringByID("button_confirm", lang))));

        return result;
    }

    private Role getRoleSafelyWithID(String id) {
        if (id == null)
            return null;

        try {
            return g.getRoleById(id);
        } catch (Exception ignored) {
            return null;
        }
    }
}
