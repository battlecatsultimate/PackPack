package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChannelPermissionHolder extends ComponentHolder {
    private final Message msg;

    private final Guild g;
    private final IDHolder holder;

    private final String roleId;

    private int page = 0;

    public ChannelPermissionHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull Guild g, @NotNull IDHolder holder, @NotNull String roleId) {
        super(author, channelID, message.getId());

        msg = message;

        this.g = g;
        this.holder = holder;

        this.roleId = roleId;

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
            case "addChannel" -> {
                if (!(event instanceof EntitySelectInteractionEvent ee))
                    return;

                List<IMentionable> mentionables = ee.getValues();

                List<String> channels = holder.channel.computeIfAbsent(roleId, k -> new ArrayList<>());

                for (IMentionable id : mentionables) {
                    if (!channels.contains(id.getId()))
                        channels.add(id.getId());
                }

                event.deferReply()
                        .setContent(LangID.getStringByID("chperm_added", lang))
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "removeChannel" -> {
                if (!(event instanceof StringSelectInteractionEvent ee))
                    return;

                List<String> values = ee.getValues();

                List<String> channels = holder.channel.computeIfAbsent(roleId, k -> new ArrayList<>());

                for (String id : values) {
                    channels.remove(id);
                }

                event.deferReply()
                        .setContent(LangID.getStringByID("chperm_removed", lang))
                        .setEphemeral(true)
                        .queue();

                int totalPage = channels.size() / SearchHolder.PAGE_CHUNK;

                if(holder.ID.size() % SearchHolder.PAGE_CHUNK != 0)
                    totalPage++;

                page = Math.min(Math.max(0, page), totalPage - 1);

                applyResult();
            }
            case "allow" -> {
                holder.channel.remove(roleId);

                page = 0;

                event.deferReply()
                        .setContent(LangID.getStringByID("chperm_allowed", lang))
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "disallow" -> {
                List<String> channels = holder.channel.computeIfAbsent(roleId, k -> new ArrayList<>());

                channels.clear();

                page = 0;

                event.deferReply()
                        .setContent(LangID.getStringByID("chperm_disallowed", lang))
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "back" -> {
                expired = true;

                goBack(event);
            }
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("chperm_confirmed", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
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

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getChannelSelectorMessage())
                .setComponents(getChannelSelector())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private void applyResult() {
        msg.editMessage(getChannelSelectorMessage())
                .setComponents(getChannelSelector())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private String getChannelSelectorMessage() {
        int lang = holder.config.lang;

        StringBuilder builder = new StringBuilder(String.format(LangID.getStringByID("chperm_chdesc", lang), roleId, roleId))
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
                    if (i < SearchHolder.PAGE_CHUNK * page) {
                        i++;

                        continue;
                    }

                    if (i == SearchHolder.PAGE_CHUNK * (page + 1))
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

    private List<LayoutComponent> getChannelSelector() {
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
                                .setDefaultValues(new ArrayList<>())
                                .setPlaceholder(LangID.getStringByID("chperm_challow", lang))
                                .build()
                )
        );

        List<String> channels = holder.channel.get(roleId);

        if (channels != null && !channels.isEmpty()) {
            List<SelectOption> channelOptions = new ArrayList<>();

            int i = 0;

            for (String id : channels) {
                if (i < SearchHolder.PAGE_CHUNK * page) {
                    i++;

                    continue;
                }

                if (i == SearchHolder.PAGE_CHUNK * (page + 1))
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
                int totalPage = channels.size() / SearchHolder.PAGE_CHUNK;

                if(channels.size() % SearchHolder.PAGE_CHUNK != 0)
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
