package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfigChannelManageHolder extends ServerConfigHolder {
    private final String role;
    @Nullable
    private List<String> channels;

    private int page = 0;

    public ConfigChannelManageHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, String role, int lang) {
        super(author, channelID, message, holder, backup, lang);

        this.role = role;
        this.channels = holder.channel.get(role);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                if (channels == null)
                    channels = holder.channel.computeIfAbsent(role, k -> new ArrayList<>());

                List<String> ids = e.getValues().stream().map(ISnowflake::getId).toList();

                for (String id : ids) {
                    if (channels.contains(id))
                        channels.remove(id);
                    else
                        channels.add(id);
                }

                if (channels != null && page * SearchHolder.PAGE_CHUNK >= channels.size()) {
                    page = (int) Math.ceil(channels.size() * 1.0 / SearchHolder.PAGE_CHUNK) - 1;
                }

                applyResult(event);
            }
            case "all" -> {
                holder.channel.remove(role);
                channels = null;
                page = 0;

                applyResult(event);
            }
            case "no" -> {
                if (channels == null)
                    channels = holder.channel.computeIfAbsent(role, k -> new ArrayList<>());
                else
                    channels.clear();

                page = 0;

                applyResult(event);
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
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("sercon_done", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("sercon_cancelask", lang), lang);

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("sercon_cancel", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    expired = true;
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("sercon_permission", lang)).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannel", lang).formatted(Emoji.fromUnicode("📜"))).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannelmanagedesc", lang).formatted("<@&" + role + ">")).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannelallowedchannel", lang)).append("\n");

        if (channels != null) {
            if (channels.isEmpty()) {
                builder.append(LangID.getStringByID("sercon_permissionchannelno", lang));
            } else {
                int size = Math.min(channels.size() , (page + 1) * SearchHolder.PAGE_CHUNK);

                for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
                    builder.append(i + 1).append(". <#").append(channels.get(i)).append("> [").append(channels.get(i)).append("]");

                    if (i < size - 1)
                        builder.append("\n");
                }

                if (channels.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = (int) Math.ceil(channels.size() * 1.0 / SearchHolder.PAGE_CHUNK);

                    builder.append("\n").append(LangID.getStringByID("formst_page", lang).formatted(page + 1, totalPage));
                }
            }
        } else {
            builder.append(LangID.getStringByID("sercon_permissionchannelall", lang));
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        if (channels != null && channels.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(channels.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(ActionRow.of(
                EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                        .setChannelTypes(ChannelType.TEXT, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD, ChannelType.FORUM)
                        .setPlaceholder(LangID.getStringByID("sercon_permissionchannelchannelselect", lang))
                        .setRequiredRange(1, StringSelectMenu.OPTIONS_MAX_AMOUNT)
                        .build()
        ));

        result.add(ActionRow.of(
                Button.secondary("all", LangID.getStringByID("sercon_permissionchannelallow", lang)).withEmoji(Emoji.fromUnicode("🪄")).withDisabled(channels == null),
                Button.secondary("no", LangID.getStringByID("sercon_permissionchanneldisallow", lang)).withEmoji(Emoji.fromUnicode("❌")).withDisabled(channels != null && channels.isEmpty())
        ));

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
