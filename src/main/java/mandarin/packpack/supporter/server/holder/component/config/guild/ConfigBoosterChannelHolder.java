package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigBoosterChannelHolder extends ServerConfigHolder {
    private int page = 0;

    public ConfigBoosterChannelHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                Guild g = event.getGuild();

                if (g == null)
                    return;

                List<GuildChannel> channels = e.getMentions().getChannels();

                List<String> reasons = new ArrayList<>();

                for (GuildChannel channel : channels) {
                    if (holder.boosterPinChannel.contains(channel.getId())) {
                        holder.boosterPinChannel.remove(channel.getId());
                    } else {
                        boolean invalid = false;

                        if (channel instanceof GuildMessageChannel m) {
                            if (!m.canTalk()) {
                                reasons.add(channel.getAsMention() + " : " + LangID.getStringByID("serverConfig.boosterPin.problem.cantTalk", lang));
                                invalid = true;
                            } else if (!g.getSelfMember().hasPermission(m, Permission.MESSAGE_MANAGE)) {
                                reasons.add(channel.getAsMention() + " : " + LangID.getStringByID("serverConfig.boosterPin.problem.noPermission", lang));
                                invalid = true;
                            }
                        }

                        if (!invalid) {
                            holder.boosterPinChannel.add(channel.getId());
                        }
                    }
                }

                if (!reasons.isEmpty()) {
                    StringBuilder problem = new StringBuilder(LangID.getStringByID("serverConfig.boosterPin.registerProblem", lang)).append("\n\n");

                    for (String reason : reasons) {
                        problem.append("- ").append(reason).append("\n");
                    }

                    event.deferReply()
                            .setContent(problem.toString())
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();
                }

                applyResult(event);
            }
            case "enable" -> {
                holder.boosterPin = !holder.boosterPin;

                applyResult(event);
            }
            case "all" -> {
                holder.boosterAll = !holder.boosterAll;

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
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
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
                .append(LangID.getStringByID("serverConfig.channel.documentation.boosterPin.title", lang).formatted(Emoji.fromUnicode("📌"))).append("\n")
                .append(LangID.getStringByID("serverConfig.boosterPin.description", lang)).append("\n\n");


        if (holder.boosterPin) {
            builder.append(LangID.getStringByID("serverConfig.boosterPin.allowedChannels", lang)).append("\n\n");

            if (holder.boosterAll) {
                builder.append(LangID.getStringByID("serverConfig.boosterPin.allChannels", lang));
            } else {
                if (holder.boosterPinChannel.isEmpty()) {
                    builder.append(LangID.getStringByID("serverConfig.boosterPin.noChannels", lang));
                } else {
                    int size = Math.min(holder.boosterPinChannel.size(), (page + 1) * SearchHolder.PAGE_CHUNK);

                    for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
                        String channel = holder.boosterPinChannel.get(i);

                        builder.append(i + 1).append(". ").append("<#").append(channel).append("> [").append(channel).append("]");

                        if (i < size - 1) {
                            builder.append("\n");
                        }
                    }
                }
            }
        } else {
            builder.append(LangID.getStringByID("serverConfig.boosterPin.featureDisabled", lang));
        }

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents(IMessageEditCallback event) {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        if (holder.boosterPin) {
            Guild g = event.getGuild();

            if (g == null)
                return result;

            boolean channelNotManageable = holder.boosterAll;

            result.add(ActionRow.of(
                    EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                            .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                            .setPlaceholder(LangID.getStringByID("serverConfig.boosterPin.selectChannel", lang))
                            .setRequiredRange(1, EntitySelectMenu.OPTIONS_MAX_AMOUNT)
                            .setDisabled(channelNotManageable)
                            .build()
            ));

            if (holder.boosterPinChannel.size() > SearchHolder.PAGE_CHUNK) {
                List<Button> buttons = new ArrayList<>();

                int totalPage = (int) Math.ceil(holder.boosterPinChannel.size() * 1.0 / SearchHolder.PAGE_CHUNK);

                if(totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0 || channelNotManageable));
                }

                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0 || channelNotManageable));
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage || channelNotManageable));

                if(totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage || channelNotManageable));
                }

                result.add(ActionRow.of(buttons));
            }

            Emoji allChannelSwitch;

            if (holder.boosterAll) {
                allChannelSwitch = EmojiStore.SWITCHON;
            } else {
                allChannelSwitch = EmojiStore.SWITCHOFF;
            }

            result.add(
                    ActionRow.of(
                            Button.secondary("enable", LangID.getStringByID("serverConfig.boosterPin.allowBooster", lang)).withEmoji(EmojiStore.SWITCHON),
                            Button.secondary("all", LangID.getStringByID("serverConfig.boosterPin.allowAll", lang)).withEmoji(allChannelSwitch)
                    )
            );
        } else {
            result.add(
                    ActionRow.of(
                            Button.secondary("enable", LangID.getStringByID("serverConfig.boosterPin.allowBooster", lang)).withEmoji(EmojiStore.SWITCHOFF)
                    )
            );
        }

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
