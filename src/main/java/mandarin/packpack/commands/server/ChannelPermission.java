package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.guild.ConfigChannelRoleSelectHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class ChannelPermission extends ConstraintCommand {
    public ChannelPermission(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        List<String> roles = new ArrayList<>();

        if (holder.member != null) {
            roles.add("MEMBER|" + holder.member);
        }

        if (holder.booster != null) {
            roles.add("BOOSTER|" + holder.booster);
        }

        roles.addAll(holder.ID.values());

        replyToMessageSafely(ch, getContents(roles), loader.getMessage(), a -> a.setComponents(getComponents(roles)), msg ->
            StaticStore.putHolder(loader.getUser().getId(), new ConfigChannelRoleSelectHolder(loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, holder, lang))
        );
    }

    private String getContents(List<String> roles) {
        if (holder == null)
            return "";

        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.permission.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.permission.documentation.channelPermission.title", lang).formatted(Emoji.fromUnicode("📜"))).append("\n")
                .append(LangID.getStringByID("serverConfig.channelPermission.description", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.channelPermission.assignedRole", lang)).append("\n");

        if (roles.isEmpty()) {
            builder.append(LangID.getStringByID("serverConfig.channelPermission.noRoleAssigned", lang));
        } else {
            int size = Math.min(roles.size(), SearchHolder.PAGE_CHUNK);

            for (int i = 0; i < size; i++) {
                String id = roles.get(i);
                boolean isCustom = true;

                builder.append(i + 1).append(". ");

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.member.text", lang));
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.booster.text", lang));
                } else {
                    final String finalID = id;

                    String foundRoleName = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(finalID);
                    }).findAny().orElse("UNKNOWN");

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.custom.text", lang).formatted(foundRoleName));
                }

                builder.append("<@&").append(id).append("> [").append(id).append("]");

                if (isCustom) {
                    builder.append(" <").append(LangID.getStringByID("serverConfig.channelPermission.role.custom.type", lang)).append(">");
                }

                if (i < size - 1) {
                    builder.append("\n");
                }
            }
        }

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            builder.append("\n").append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage));
        }

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents(List<String> roles) {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        if (holder == null)
            return result;

        List<SelectOption> roleOptions = new ArrayList<>();

        if (roles.isEmpty()) {
            roleOptions.add(SelectOption.of("A", "A"));
        } else {
            int size = Math.min(roles.size(), SearchHolder.PAGE_CHUNK);

            for (int i = 0; i < size; i++) {
                String id = roles.get(i);
                String label;
                String value;

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");
                    value = id;
                    label = LangID.getStringByID("serverConfig.channelPermission.role.member.type", lang);
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    value = id;
                    label = LangID.getStringByID("serverConfig.channelPermission.role.booster.type", lang);
                } else {
                    value = id;

                    label = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(value);
                    }).findAny().orElse("UNKNOWN") + " <" + LangID.getStringByID("serverConfig.channelPermission.role.custom.type", lang) + ">";
                }

                roleOptions.add(SelectOption.of(label, value).withDescription(id));
            }
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("role")
                        .addOptions(roleOptions)
                        .setPlaceholder(LangID.getStringByID("serverConfig.channelPermission.selectRole", lang))
                        .setDisabled(roles.isEmpty())
                        .build()
        ));

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(
                ActionRow.of(
                        Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
