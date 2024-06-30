package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigBoosterChannelHolder extends ServerConfigHolder {
    private int page = 0;

    public ConfigBoosterChannelHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
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
                                reasons.add(channel.getAsMention() + " : " + LangID.getStringByID("sercon_channelboostnosee", lang));
                                invalid = true;
                            } else if (!g.getSelfMember().hasPermission(m, Permission.MESSAGE_MANAGE)) {
                                reasons.add(channel.getAsMention() + " : " + LangID.getStringByID("sercon_channelboostnoperm", lang));
                                invalid = true;
                            }
                        }

                        if (!invalid) {
                            holder.boosterPinChannel.add(channel.getId());
                        }
                    }
                }

                if (!reasons.isEmpty()) {
                    StringBuilder problem = new StringBuilder(LangID.getStringByID("sercon_channelboosterproblem", lang)).append("\n\n");

                    for (String reason : reasons) {
                        problem.append("- ").append(reason).append("\n");
                    }

                    event.deferReply()
                            .setContent(problem.toString())
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
                .setComponents(getComponents(event))
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("sercon_channeltitle", lang)).append("\n")
                .append(LangID.getStringByID("sercon_channelboosttit", lang).formatted(Emoji.fromUnicode("📌"))).append("\n")
                .append(LangID.getStringByID("sercon_channelboostmanagedesc", lang)).append("\n\n");


        if (holder.boosterPin) {
            builder.append(LangID.getStringByID("sercon_channelboostallowed", lang)).append("\n\n");

            if (holder.boosterAll) {
                builder.append(LangID.getStringByID("sercon_channelboostallchannel", lang));
            } else {
                if (holder.boosterPinChannel.isEmpty()) {
                    builder.append(LangID.getStringByID("sercon_channelboostnochannel", lang));
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
            builder.append(LangID.getStringByID("sercon_channelboostdisable", lang));
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents(GenericComponentInteractionCreateEvent event) {
        List<LayoutComponent> result = new ArrayList<>();

        if (holder.boosterPin) {
            Guild g = event.getGuild();

            if (g == null)
                return result;

            boolean channelNotManageable = holder.boosterAll;

            result.add(ActionRow.of(
                    EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                            .setChannelTypes(ChannelType.TEXT)
                            .setPlaceholder(LangID.getStringByID("sercon_channelboostallow", lang))
                            .setRequiredRange(1, EntitySelectMenu.OPTIONS_MAX_AMOUNT)
                            .setDisabled(channelNotManageable)
                            .build()
            ));

            if (holder.boosterPinChannel.size() > SearchHolder.PAGE_CHUNK) {
                List<Button> buttons = new ArrayList<>();

                int totalPage = (int) Math.ceil(holder.boosterPinChannel.size() * 1.0 / SearchHolder.PAGE_CHUNK);

                if(totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0 || channelNotManageable));
                }

                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0 || channelNotManageable));
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage || channelNotManageable));

                if(totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage || channelNotManageable));
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
                            Button.secondary("enable", LangID.getStringByID("sercon_channelboostenable", lang)).withEmoji(EmojiStore.SWITCHON),
                            Button.secondary("all", LangID.getStringByID("sercon_channelboostall", lang)).withEmoji(allChannelSwitch)
                    )
            );
        } else {
            result.add(
                    ActionRow.of(
                            Button.secondary("enable", LangID.getStringByID("sercon_channelboostenable", lang)).withEmoji(EmojiStore.SWITCHOFF)
                    )
            );
        }

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
