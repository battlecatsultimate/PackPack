package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HasRolePageHolder extends ComponentHolder {
    private final List<Member> members;
    private final Role role;

    private int page = 0;

    public HasRolePageHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull List<Member> members, @NotNull Role role, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, lang);

        this.members = members;
        this.role = role;

        registerAutoFinish(this, message, "hasrole_close", FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
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
            case "close" ->
                    event.deferEdit()
                            .setContent(LangID.getStringByID("hasrole_close", lang))
                            .setComponents()
                            .setEmbeds()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        message.editMessage(LangID.getStringByID("hasrole_close", lang))
                .setEmbeds()
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setComponents(getComponents())
                .setEmbeds(getEmbed())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private MessageEmbed getEmbed() {
        StringBuilder builder = new StringBuilder();

        if (members.size() == 1) {
            builder.append(LangID.getStringByID("hasrole_numbersingle", lang).formatted(role.getAsMention()));
        } else {
            builder.append(LangID.getStringByID("hasrole_number", lang).formatted(members.size(), role.getAsMention()));
        }

        builder.append("\n\n");

        int size = Math.min(members.size(), (page + 1) * SearchHolder.PAGE_CHUNK);

        for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
            Member m = members.get(i);

            builder.append(i + 1).append(". ").append(m.getUser().getName()).append(" ").append(m.getAsMention()).append(" [").append(m.getId()).append("]");

            if (i < size - 1) {
                builder.append("\n");
            }
        }

        if (members.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(members.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            builder.append("\n\n").append(LangID.getStringByID("hasrole_page", lang).formatted(1, totalPage));
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(LangID.getStringByID("hasrole_title", lang).formatted(role.getName()));

        embed.setDescription(builder.toString());

        return embed.build();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        if (members.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(members.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            List<Button> buttons = new ArrayList<>();

            if (totalPage > 10) {
                buttons.add(Button.secondary("prev10", LangID.getStringByID("search_prev10", lang)).withEmoji(EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());

            buttons.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));

            if (totalPage > 10) {
                buttons.add(Button.secondary("next10", LangID.getStringByID("search_next10", lang)).withEmoji(EmojiStore.TWO_NEXT));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(ActionRow.of(Button.danger("close", LangID.getStringByID("button_close", lang)).withEmoji(EmojiStore.CROSS)));

        return result;
    }
}
