package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.guild.ConfigRoleRegistrationHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class IDSet extends ConstraintCommand {

    public IDSet(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();
        Guild g = loader.getGuild();

        if(holder == null)
            return;

        replyToMessageSafely(ch, getContents(), loader.getMessage(), a -> a.setComponents(getComponents(g)), msg ->
            StaticStore.putHolder(u.getId(), new ConfigRoleRegistrationHolder(loader.getMessage(), u.getId(), ch.getId(), msg, holder, lang))
        );
    }

    private String getContents() {
        if (holder == null)
            return "";

        String moderatorRole;

        if (holder.moderator == null) {
            moderatorRole = LangID.getStringByID("serverConfig.general.role.anyManager", lang);
        } else {
            moderatorRole = "<@&" + holder.moderator + ">";
        }

        StringBuilder builder = new StringBuilder(LangID.getStringByID("serverConfig.general.role.documentation.title", lang).formatted(EmojiStore.ROLE.getFormatted()))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.description", lang))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.moderator.title", lang).formatted(EmojiStore.MODERATOR.getFormatted(), moderatorRole))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.moderator.description", lang))
                .append("\n");

        String memberRole;

        if (holder.member == null) {
            memberRole = "@everyone";
        } else {
            memberRole = "<@&" + holder.member + ">";
        }

        builder.append(LangID.getStringByID("serverConfig.general.role.documentation.member.title", lang).formatted(EmojiStore.MEMBER.getFormatted(), memberRole))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.member.description", lang))
                .append("\n");

        String boosterRole;

        if (holder.booster == null) {
            boosterRole = LangID.getStringByID("data.none", lang);
        } else {
            boosterRole = "<@&" + holder.booster + ">";
        }

        builder.append(LangID.getStringByID("serverConfig.general.role.documentation.booster.title", lang).formatted(EmojiStore.BOOSTER.getFormatted(), boosterRole))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.booster.description", lang));

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents(Guild g) {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        if (holder == null)
            return result;

        List<Role> roles = g.getRoles();

        EntitySelectMenu.DefaultValue moderator = roles
                .stream()
                .filter(r -> r.getId().equals(holder.moderator))
                .findAny()
                .map(role -> EntitySelectMenu.DefaultValue.role(role.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue member = roles
                .stream()
                .filter(r -> r.getId().equals(holder.member))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue booster = roles
                .stream()
                .filter(r -> r.getId().equals(holder.booster))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("moderator", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(moderator == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { moderator })
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.ROLE)
                                .setPlaceholder(LangID.getStringByID("serverConfig.general.role.selectNone.everyone", lang))
                                .setDefaultValues(member == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { member })
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("booster", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(booster == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { booster })
                                .setPlaceholder(LangID.getStringByID("serverConfig.general.role.selectNone.none", lang))
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        Button.secondary("custom", LangID.getStringByID("serverConfig.general.role.custom", lang)).withEmoji(Emoji.fromUnicode("⚙️"))
                )
        );

        result.add(
                ActionRow.of(
                        Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
