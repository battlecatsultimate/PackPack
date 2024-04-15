package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelPermissionRoleHolder extends ComponentHolder {
    private final Message msg;

    private final IDHolder holder;
    private final Guild g;

    private int page = 0;

    public ChannelPermissionRoleHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull Guild g, @NotNull IDHolder holder) {
        super(author, channelID, message.getId());

        msg = message;

        this.holder = holder;
        this.g = g;

        registerAutoFinish(this, message, holder.config.lang, "chperm_expire", FIVE_MIN);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        msg.editMessage(LangID.getStringByID("chperm_expire", holder.config.lang))
                .setComponents()
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        int lang = holder.config.lang;

        switch (event.getComponentId()) {
            case "role" -> {
                if (!(event instanceof StringSelectInteractionEvent))
                    return;

                if (((StringSelectInteractionEvent) event).getValues().isEmpty())
                    return;

                expired = true;

                String value;

                if (((StringSelectInteractionEvent) event).getValues().getFirst().equals(holder.MEMBER)) {
                    value = holder.MEMBER;
                } else {
                    value = ((StringSelectInteractionEvent) event).getValues().getFirst();
                }

                event.deferEdit()
                        .setContent(getChannelSelectorMessage(value))
                        .setComponents(getChannelSelector(value))
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();

                StaticStore.putHolder(userID, new ChannelPermissionHolder(getAuthorMessage(), channelID, msg, g, holder, value));
            }
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("chperm_confirmed", lang))
                        .setComponents()
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();

                expired = true;
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
                .setContent(getRoleSelectorMessage())
                .setComponents(getRoleSelector())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private String getRoleSelectorMessage() {
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
            if (i < SearchHolder.PAGE_CHUNK * page) {
                i++;

                continue;
            }

            if (i == SearchHolder.PAGE_CHUNK * (page + 1)) {
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

            builder.append("\n").append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page + 1)).replace("-", String.valueOf(totalPage)));
        }

        return builder.toString();
    }

    private String getChannelSelectorMessage(String roleId) {
        int lang = holder.config.lang;

        StringBuilder builder = new StringBuilder(LangID.getStringByID("chperm_chdesc", lang))
                .append("\n\n");

        List<String> channels = holder.channel.get(roleId);

        if (channels == null) {
            builder.append(LangID.getStringByID("chperm_all", lang));
        } else {
            if (channels.isEmpty()) {
                builder.append(LangID.getStringByID("chperm_none", lang));
            } else {
                int i = 0;

                for (String id : channels) {
                    if (i == SearchHolder.PAGE_CHUNK)
                        break;

                    builder.append(i + 1).append(". ");

                    GuildChannel channel = g.getGuildChannelById(id);

                    if (channel == null) {
                        builder.append("UNKNOWN");
                    } else {
                        builder.append(channel.getAsMention());
                    }

                    builder.append(" [").append(id).append("]\n");

                    i++;
                }

                if(channels.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = channels.size() / SearchHolder.PAGE_CHUNK;

                    if(holder.ID.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    builder.append("\n").append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage)));
                }
            }
        }

        return builder.toString();
    }

    public List<LayoutComponent> getRoleSelector() {
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
            if (i < SearchHolder.PAGE_CHUNK * page) {
                i++;

                continue;
            }

            if (i == SearchHolder.PAGE_CHUNK * (page + 1))
                break;

            if (key.equals(holder.MEMBER)) {
                options.add(SelectOption.of(LangID.getStringByID("idset_member", lang), LangID.getStringByID("idset_member", lang) + " : " + holder.MEMBER).withDescription(holder.MEMBER));
            } else {
                options.add(SelectOption.of(key, key).withDescription(roleMap.get(key)));
            }

            i++;
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("role").addOptions(options).setPlaceholder(LangID.getStringByID("chperm_roleselect", lang)).build()
        ));

        if (roleMap.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = roleMap.size() / SearchHolder.PAGE_CHUNK;

            if(roleMap.size() % SearchHolder.PAGE_CHUNK != 0)
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

        result.add(ActionRow.of(Button.primary("confirm", LangID.getStringByID("button_confirm", lang))));

        return result;
    }

    private List<LayoutComponent> getChannelSelector(String roleId) {
        int lang = holder.config.lang;

        List<LayoutComponent> result = new ArrayList<>();

        result.add(
                ActionRow.of(
                        Button.secondary("allow", LangID.getStringByID("chperm_allow", lang)),
                        Button.secondary("disallow", LangID.getStringByID("chperm_disallow",  lang))
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("addChannel", EntitySelectMenu.SelectTarget.CHANNEL)
                                .setChannelTypes(ChannelType.TEXT, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                                .setMaxValues(25)
                                .setPlaceholder(LangID.getStringByID("chperm_challow", lang))
                                .build()
                )
        );

        List<String> channels = holder.channel.get(roleId);

        if (channels != null && !channels.isEmpty()) {
            List<SelectOption> channelOptions = new ArrayList<>();

            int i = 0;

            for (String id : channels) {
                if (i == SearchHolder.PAGE_CHUNK)
                    break;

                GuildChannel channel = g.getGuildChannelById(id);

                if (channel != null) {
                    channelOptions.add(SelectOption.of(channel.getName(), id).withDescription(id));
                } else {
                    channelOptions.add(SelectOption.of("UNKNOWN", id).withDescription(id));
                }

                i++;
            }

            result.add(
                    ActionRow.of(
                            StringSelectMenu.create("removeChannel")
                                    .addOptions(channelOptions)
                                    .setPlaceholder(LangID.getStringByID("chperm_chdisallow", lang))
                                    .setMaxValues(25)
                                    .build()
                    )
            );

            if (channels.size() > SearchHolder.PAGE_CHUNK) {
                List<ActionComponent> pages = new ArrayList<>();

                if (channels.size() > SearchHolder.PAGE_CHUNK * 10) {
                    pages.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
                }

                pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
                pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));

                if (channels.size() > SearchHolder.PAGE_CHUNK * 10) {
                    pages.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).asDisabled());
                }

                result.add(ActionRow.of(pages));
            }
        }

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("button_back", lang)),
                        Button.primary("confirm", LangID.getStringByID("button_confirm", lang))
                )
        );

        return result;
    }
}
