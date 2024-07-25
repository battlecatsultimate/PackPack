package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
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

    public ConfigChannelManageHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, String role, CommonStatic.Lang.Locale lang) {
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
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
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

        String roleMention;

        if (role.equals("Member")) {
            roleMention = "@everyone";
        } else {
            roleMention = "<@&" + role + ">";
        }

        builder.append(LangID.getStringByID("serverConfig.permission.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.permission.documentation.channelPermission.title", lang).formatted(Emoji.fromUnicode("📜"))).append("\n")
                .append(LangID.getStringByID("serverConfig.channelPermission.role.description", lang).formatted(roleMention)).append("\n")
                .append(LangID.getStringByID("serverConfig.channelPermission.role.allowedChannels", lang)).append("\n");

        if (channels != null) {
            if (channels.isEmpty()) {
                builder.append(LangID.getStringByID("serverConfig.channelPermission.role.noChannels", lang));
            } else {
                int size = Math.min(channels.size() , (page + 1) * SearchHolder.PAGE_CHUNK);

                for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
                    builder.append(i + 1).append(". <#").append(channels.get(i)).append("> [").append(channels.get(i)).append("]");

                    if (i < size - 1)
                        builder.append("\n");
                }

                if (channels.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = (int) Math.ceil(channels.size() * 1.0 / SearchHolder.PAGE_CHUNK);

                    builder.append("\n").append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage));
                }
            }
        } else {
            builder.append(LangID.getStringByID("serverConfig.channelPermission.role.allChannels", lang));
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        if (channels != null && channels.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(channels.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(ActionRow.of(
                EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                        .setChannelTypes(ChannelType.TEXT, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD, ChannelType.FORUM)
                        .setPlaceholder(LangID.getStringByID("serverConfig.channelPermission.role.selectChannel", lang))
                        .setRequiredRange(1, StringSelectMenu.OPTIONS_MAX_AMOUNT)
                        .build()
        ));

        result.add(ActionRow.of(
                Button.secondary("all", LangID.getStringByID("serverConfig.channelPermission.role.allowAll", lang)).withEmoji(Emoji.fromUnicode("🪄")).withDisabled(channels == null),
                Button.secondary("no", LangID.getStringByID("serverConfig.channelPermission.role.disallowAll", lang)).withEmoji(Emoji.fromUnicode("❌")).withDisabled(channels != null && channels.isEmpty())
        ));

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
