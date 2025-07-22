package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigStatusHolder extends ServerConfigHolder {
    public ConfigStatusHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "assign" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                Guild g = event.getGuild();

                if (g == null)
                    return;

                String id = e.getValues().getFirst().getId();

                TextChannel channel = g.getTextChannelById(id);

                if (channel == null)
                    return;

                if (holder.status.contains(id)) {
                    event.deferReply()
                            .setContent(LangID.getStringByID("serverConfig.status.alreadyAssigned", lang))
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();

                    return;
                }

                if (!channel.canTalk()) {
                    event.deferReply()
                            .setContent(LangID.getStringByID("serverConfig.eventData.cantTalk", lang))
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();

                    return;
                }

                holder.status.add(id);

                applyResult(event);
            }
            case "remove" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                int index = StaticStore.safeParseInt(e.getValues().getFirst());

                holder.status.remove(index);

                applyResult(event);
            }
            case "back" -> goBack(event);
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    end(true);
                }, lang));
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents(event))
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.channel.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.channel.documentation.status.title", lang).formatted(Emoji.fromUnicode("📡"))).append("\n")
                .append(LangID.getStringByID("serverConfig.status.description", lang)).append("\n\n")
                .append(LangID.getStringByID("serverConfig.status.channelList", lang)).append("\n\n");

        if (holder.status.isEmpty()) {
            builder.append("```\n").append(LangID.getStringByID("serverConfig.status.noChannel", lang)).append("\n```");
        } else {
            for (int i = 0; i < holder.status.size(); i++) {
                builder.append(i + 1).append(". <#").append(holder.status.get(i)).append("> [").append(holder.status.get(i)).append("]");

                if (i < holder.status.size() - 1) {
                    builder.append("\n");
                }
            }
        }

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents(IMessageEditCallback event) {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        Guild g = event.getGuild();

        if (g == null)
            return result;

        result.add(ActionRow.of(
                EntitySelectMenu.create("assign", EntitySelectMenu.SelectTarget.CHANNEL)
                        .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                        .setPlaceholder(LangID.getStringByID("serverConfig.status.assignChannel", lang).formatted(SelectMenu.OPTIONS_MAX_AMOUNT))
                        .setDisabled(holder.status.size() >= SelectMenu.OPTIONS_MAX_AMOUNT)
                        .setRequiredRange(1, 1)
                        .build()
        ));

        List<SelectOption> options = new ArrayList<>();
        String placeHolder;

        if (holder.status.isEmpty()) {
            options.add(SelectOption.of("A", "A"));
            placeHolder = LangID.getStringByID("serverConfig.status.noRemovableChannel", lang);
        } else {
            for (int i = 0; i < holder.status.size(); i++) {
                TextChannel channel = g.getTextChannelById(holder.status.get(i));

                String name;

                if (channel == null) {
                    name = (i + 1) + ". UNKNOWN";
                } else {
                    name = (i + 1) + ". ";

                    String channelName;

                    if (channel.getName().length() >= 50) {
                        channelName = channel.getName().substring(0, 47) + "...";
                    } else {
                        channelName = channel.getName();
                    }

                    name += channelName;
                }

                options.add(SelectOption.of(name, String.valueOf(i)).withDescription(holder.status.get(i)));
            }

            placeHolder = LangID.getStringByID("serverConfig.status.removeChannel", lang);
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("remove")
                        .addOptions(options)
                        .setPlaceholder(placeHolder)
                        .setDisabled(holder.status.isEmpty())
                        .setRequiredRange(1, 1)
                        .build()
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
