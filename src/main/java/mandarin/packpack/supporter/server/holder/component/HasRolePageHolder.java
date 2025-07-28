package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HasRolePageHolder extends ComponentHolder {
    private final List<Member> members;
    private final Role role;

    private int page = 0;

    public HasRolePageHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull List<Member> members, @Nonnull Role role, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.members = members;
        this.role = role;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
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
                            .setContent(LangID.getStringByID("hasRole.closed", lang))
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
    public void onExpire() {
        message.editMessage(LangID.getStringByID("hasRole.closed", lang))
                .setEmbeds()
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult(IMessageEditCallback event) {
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
            builder.append(LangID.getStringByID("hasRole.embed.number.single", lang).formatted(role.getAsMention()));
        } else {
            builder.append(LangID.getStringByID("hasRole.embed.number.plural", lang).formatted(members.size(), role.getAsMention()));
        }

        builder.append("\n\n");

        int size = Math.min(members.size(), (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize);

        for (int i = page * ConfigHolder.SearchLayout.COMPACTED.chunkSize; i < size; i++) {
            Member m = members.get(i);

            builder.append(i + 1).append(". ").append(m.getUser().getName()).append(" ").append(m.getAsMention()).append(" [").append(m.getId()).append("]");

            if (i < size - 1) {
                builder.append("\n");
            }
        }

        if (members.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            int totalPage = (int) Math.ceil(members.size() * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize);

            builder.append("\n\n").append(LangID.getStringByID("hasRole.embed.page", lang).formatted(1, totalPage));
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(LangID.getStringByID("hasRole.embed.title", lang).formatted(role.getName()));

        embed.setDescription(builder.toString());

        return embed.build();
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        if (members.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            int totalPage = (int) Math.ceil(members.size() * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize);

            List<Button> buttons = new ArrayList<>();

            if (totalPage > 10) {
                buttons.add(Button.secondary("prev10", LangID.getStringByID("ui.search.10Previous", lang)).withEmoji(EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());

            buttons.add(Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT));

            if (totalPage > 10) {
                buttons.add(Button.secondary("next10", LangID.getStringByID("ui.search.10Next", lang)).withEmoji(EmojiStore.TWO_NEXT));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(ActionRow.of(Button.danger("close", LangID.getStringByID("ui.button.close", lang)).withEmoji(EmojiStore.CROSS)));

        return result;
    }
}
