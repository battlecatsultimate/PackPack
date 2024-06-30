package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.ConfigChannelRoleSelectHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChannelPermission extends ConstraintCommand {
    public ChannelPermission(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        List<String> roles = new ArrayList<>();

        if (holder.MEMBER != null) {
            roles.add("MEMBER|" + holder.MEMBER);
        }

        if (holder.BOOSTER != null) {
            roles.add("BOOSTER|" + holder.BOOSTER);
        }

        roles.addAll(holder.ID.values());

        replyToMessageSafely(ch, getContents(roles), loader.getMessage(), a -> a.setComponents(getComponents(roles)), msg ->
            StaticStore.putHolder(loader.getUser().getId(), new ConfigChannelRoleSelectHolder(loader.getMessage(), ch.getId(), msg, holder, lang))
        );
    }

    private String getContents(List<String> roles) {
        if (holder == null)
            return "";

        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("sercon_permission", lang)).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannel", lang).formatted(Emoji.fromUnicode("📜"))).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannelrole", lang)).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannellist", lang)).append("\n");

        if (roles.isEmpty()) {
            builder.append(LangID.getStringByID("sercon_permissionchannelnorole", lang));
        } else {
            int size = Math.min(roles.size(), SearchHolder.PAGE_CHUNK);

            for (int i = 0; i < size; i++) {
                String id = roles.get(i);
                boolean isCustom = true;

                builder.append(i + 1).append(". ");

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("sercon_permissionrolemember", lang));
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("sercon_permissionrolelbooster", lang));
                } else {
                    final String finalID = id;

                    String foundRoleName = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(finalID);
                    }).findAny().orElse("UNKNOWN");

                    builder.append(LangID.getStringByID("sercon_permissionrolecustom", lang).formatted(foundRoleName));
                }

                builder.append("<@&").append(id).append("> [").append(id).append("]");

                if (isCustom) {
                    builder.append(" <").append(LangID.getStringByID("sercon_permissionrolecustomtype", lang)).append(">");
                }

                if (i < size - 1) {
                    builder.append("\n");
                }
            }
        }

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            builder.append("\n").append(LangID.getStringByID("formst_page", lang).formatted(1, totalPage));
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents(List<String> roles) {
        List<LayoutComponent> result = new ArrayList<>();

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
                    label = LangID.getStringByID("sercon_permissionrolemembertype", lang);
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    value = id;
                    label = LangID.getStringByID("sercon_permissionrolelboostertype", lang);
                } else {
                    value = id;

                    label = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(value);
                    }).findAny().orElse("UNKNOWN") + " <" + LangID.getStringByID("sercon_permissionrolecustomtype", lang) + ">";
                }

                roleOptions.add(SelectOption.of(label, value).withDescription(id));
            }
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("role")
                        .addOptions(roleOptions)
                        .setPlaceholder(LangID.getStringByID("sercon_permissionchannelselect", lang))
                        .setDisabled(roles.isEmpty())
                        .build()
        ));

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(
                ActionRow.of(
                        Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
