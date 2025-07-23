package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.HasRolePageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HasRole extends ConstraintCommand {
    public HasRole(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(loader.getChannel(), LangID.getStringByID("hasRole.failed.noID", lang), loader.getMessage(), a -> a);

            return;
        }

        Role role = getRole(contents, loader.getGuild());

        if (role == null) {
            replyToMessageSafely(loader.getChannel(), LangID.getStringByID("hasRole.failed.noRole", lang), loader.getMessage(), a -> a);

            return;
        }

        loader.getGuild().findMembersWithRoles(role).onSuccess(members -> {
            members.sort((m1, m2) -> {
                if (m1 == null && m2 == null)
                    return 0;

                if (m1 == null)
                    return -1;

                if (m2 == null)
                    return 1;

                return m1.getUser().getEffectiveName().compareTo(m2.getUser().getEffectiveName());
            });

            replyToMessageSafely(loader.getChannel(), "", loader.getMessage(), a -> {
                a.setComponents(getComponents(members));
                a.addEmbeds(getEmbed(members, role));

                return a;
            }, msg -> StaticStore.putHolder(loader.getMember().getId(), new HasRolePageHolder(loader.getMessage(), loader.getMember().getId(), loader.getChannel().getId(), msg, members, role, lang)));
        });
    }

    private Role getRole(String[] contents, Guild g) {
        List<Role> roles = g.getRoles();

        for (int i = 0; i < contents.length; i++) {
            if (StaticStore.isNumeric(contents[i])) {
                long id = StaticStore.safeParseLong(contents[i]);

                Optional<Role> role = roles.stream().filter(r -> r.getIdLong() == id).findAny();

                if (role.isPresent()) {
                    return role.get();
                }
            } else if (contents[i].matches("<@&\\d+>")) {
                long id = StaticStore.safeParseLong(contents[i].replace("<@&", "").replace(">", ""));

                Optional<Role> role = roles.stream().filter(r -> r.getIdLong() == id).findAny();

                if (role.isPresent()) {
                    return role.get();
                }
            }
        }

        return null;
    }

    private MessageEmbed getEmbed(List<Member> members, Role role) {
        StringBuilder builder = new StringBuilder();

        if (members.size() == 1) {
            builder.append(LangID.getStringByID("hasRole.embed.number.single", lang).formatted(role.getAsMention()));
        } else {
            builder.append(LangID.getStringByID("hasRole.embed.number.plural", lang).formatted(members.size(), role.getAsMention()));
        }

        builder.append("\n\n");

        int size = Math.min(members.size(), SearchHolder.PAGE_CHUNK);

        for (int i = 0; i < size; i++) {
            Member m = members.get(i);

            builder.append(i + 1).append(". ").append(m.getUser().getName()).append(" ").append(m.getAsMention()).append(" [").append(m.getId()).append("]");

            if (i < size - 1) {
                builder.append("\n");
            }
        }

        if (members.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(members.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            builder.append("\n\n").append(LangID.getStringByID("hasRole.embed.page", lang).formatted(1, totalPage));
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(LangID.getStringByID("hasRole.embed.title", lang).formatted(role.getName()));

        embed.setDescription(builder.toString());

        return embed.build();
    }

    private List<MessageTopLevelComponent> getComponents(List<Member> members) {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        if (members.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(members.size() * 1.0 / SearchHolder.PAGE_CHUNK);

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
