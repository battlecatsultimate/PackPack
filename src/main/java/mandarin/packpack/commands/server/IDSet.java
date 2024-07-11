package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.ConfigRoleRegistrationHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class IDSet extends ConstraintCommand {

    public IDSet(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();
        Guild g = loader.getGuild();

        if(holder == null)
            return;

        replyToMessageSafely(ch, getContents(), loader.getMessage(), a -> a.setComponents(getComponents(g)), msg ->
            StaticStore.putHolder(u.getId(), new ConfigRoleRegistrationHolder(loader.getMessage(), ch.getId(), msg, holder, lang))
        );
    }

    private String getContents() {
        if (holder == null)
            return "";

        StringBuilder builder = new StringBuilder(LangID.getStringByID("sercon_roletit", lang).formatted(EmojiStore.ROLE.getFormatted()))
                .append("\n")
                .append(LangID.getStringByID("sercon_roledesc", lang))
                .append("\n")
                .append(LangID.getStringByID("sercon_rolemod", lang).formatted(EmojiStore.MODERATOR.getFormatted(), "<@&" + holder.MOD + ">"))
                .append("\n")
                .append(LangID.getStringByID("sercon_rolemoddesc", lang))
                .append("\n");

        String memberRole;

        if (holder.MEMBER == null) {
            memberRole = "@everyone";
        } else {
            memberRole = "<@&" + holder.MEMBER + ">";
        }

        builder.append(LangID.getStringByID("sercon_rolemem", lang).formatted(EmojiStore.MEMBER.getFormatted(), memberRole))
                .append("\n")
                .append(LangID.getStringByID("sercon_rolememdesc", lang))
                .append("\n");

        String boosterRole;

        if (holder.BOOSTER == null) {
            boosterRole = LangID.getStringByID("data_none", lang);
        } else {
            boosterRole = "<@&" + holder.BOOSTER + ">";
        }

        builder.append(LangID.getStringByID("sercon_roleboo", lang).formatted(EmojiStore.BOOSTER.getFormatted(), boosterRole))
                .append("\n")
                .append(LangID.getStringByID("sercon_roleboodesc", lang));

        return builder.toString();
    }

    private List<LayoutComponent> getComponents(Guild g) {
        List<LayoutComponent> result = new ArrayList<>();

        if (holder == null)
            return result;

        EntitySelectMenu.DefaultValue moderator = g.getRoles()
                .stream()
                .filter(r -> r.getId().equals(holder.MOD))
                .findAny()
                .map(role -> EntitySelectMenu.DefaultValue.role(role.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue member = g.getRoles()
                .stream()
                .filter(r -> r.getId().equals(holder.MEMBER))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue booster = g.getRoles()
                .stream()
                .filter(r -> r.getId().equals(holder.BOOSTER))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("moderator", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(moderator)
                                .setRequiredRange(1, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.ROLE)
                                .setPlaceholder(LangID.getStringByID("sercon_roleevery", lang))
                                .setDefaultValues(member == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { member })
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("booster", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(booster == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { booster })
                                .setPlaceholder(LangID.getStringByID("sercon_rolenone", lang))
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        Button.secondary("custom", LangID.getStringByID("sercon_custom", lang)).withEmoji(Emoji.fromUnicode("⚙️"))
                )
        );

        result.add(
                ActionRow.of(
                        Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
