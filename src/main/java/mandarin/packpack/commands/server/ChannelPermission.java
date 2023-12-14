package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ChannelPermissionRoleHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelPermission extends ConstraintCommand {
    public ChannelPermission(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        replyToMessageSafely(ch, getRoleSelectorMessage(), loader.getMessage(), a -> a.setComponents(getRoleSelector()), m ->
            StaticStore.putHolder(loader.getUser().getId(), new ChannelPermissionRoleHolder(loader.getMessage(), ch.getId(), m, loader.getGuild(), holder))
        );
    }

    private String getRoleSelectorMessage() {
        if (holder == null)
            return "";

        int lang = holder.config.lang;

        StringBuilder builder = new StringBuilder(LangID.getStringByID("chperm_desc", lang))
                .append("\n\n");

        Map<String, String> roleMap = new HashMap<>();

        if (holder.MEMBER != null)
            roleMap.put(holder.MEMBER, holder.MEMBER);

        for (String key : holder.ID.keySet()) {
            roleMap.put(key, holder.ID.get(key));
        }

        int i  = 0;

        for (String key : roleMap.keySet()) {
            if (i == SearchHolder.PAGE_CHUNK) {
                break;
            }

            builder.append(i + 1).append(". ");

            if (key.equals(holder.MEMBER)) {
                builder.append(LangID.getStringByID("idset_member", lang));
            } else {
                builder.append(key);
            }

            String id;

            if (key.equals(holder.MEMBER))
                id = holder.MEMBER;
            else
                id = holder.ID.get(key);

            if (id == null) {
                builder.append(" : UNKNOWN\n");
            } else {
                builder.append(" : ")
                        .append("<@&")
                        .append(roleMap.get(key))
                        .append("> [")
                        .append(roleMap.get(key))
                        .append("]\n");
            }

            i++;
        }

        if(roleMap.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = roleMap.size() / SearchHolder.PAGE_CHUNK;

            if(holder.ID.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            builder.append("\n").append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage)));
        }

        return builder.toString();
    }

    private List<LayoutComponent> getRoleSelector() {
        if (holder == null)
            return new ArrayList<>();

        int lang = holder.config.lang;

        List<LayoutComponent> result = new ArrayList<>();

        Map<String, String> roleMap = new HashMap<>();

        if (holder.MEMBER != null)
            roleMap.put(holder.MEMBER, holder.MEMBER);

        for (String key : holder.ID.keySet()) {
            roleMap.put(key, holder.ID.get(key));
        }

        List<SelectOption> options = new ArrayList<>();

        int i = 0;

        for (String key : roleMap.keySet()) {
            if (i == SearchHolder.PAGE_CHUNK)
                break;

            if (key.equals(holder.MEMBER)) {
                options.add(SelectOption.of(LangID.getStringByID("idset_member", lang), LangID.getStringByID("idset_member", lang) + " : " + holder.MEMBER).withDescription(holder.MEMBER));
            } else {
                options.add(SelectOption.of(key, key).withDescription(roleMap.get(key)));
            }

            i++;
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("role").addOptions(options).build()
        ));

        if (roleMap.size() > SearchHolder.PAGE_CHUNK) {
            List<ActionComponent> pages = new ArrayList<>();

            if (roleMap.size() > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));

            if (roleMap.size() > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).asDisabled());
            }

            result.add(ActionRow.of(pages));
        }

        result.add(ActionRow.of(Button.primary("confirm", LangID.getStringByID("button_confirm", lang))));

        return result;
    }
}
