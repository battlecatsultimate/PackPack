package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigStatusHolder extends ServerConfigHolder {
    public ConfigStatusHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "assign" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                String id = e.getValues().getFirst().getId();

                if (holder.status.contains(id)) {
                    event.deferReply()
                            .setContent(LangID.getStringByID("sercon_channelstatusalready", lang))
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
                .append(LangID.getStringByID("sercon_channelstatustit", lang).formatted(Emoji.fromUnicode("📡"))).append("\n")
                .append(LangID.getStringByID("sercon_channelstatusmanagedesc", lang)).append("\n\n")
                .append(LangID.getStringByID("sercon_channelstatuslist", lang)).append("\n\n");

        if (holder.status.isEmpty()) {
            builder.append("```\n").append(LangID.getStringByID("sercon_channelstatusno", lang)).append("\n```");
        } else {
            for (int i = 0; i < holder.status.size(); i++) {
                builder.append(i + 1).append("<#").append(holder.status.get(i)).append("> [").append(holder.status.get(i)).append("]");

                if (i < holder.status.size() - 1) {
                    builder.append("\n");
                }
            }
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents(GenericComponentInteractionCreateEvent event) {
        List<LayoutComponent> result = new ArrayList<>();

        Guild g = event.getGuild();

        if (g == null)
            return result;

        result.add(ActionRow.of(
                EntitySelectMenu.create("assign", EntitySelectMenu.SelectTarget.CHANNEL)
                        .setChannelTypes(ChannelType.TEXT)
                        .setPlaceholder(LangID.getStringByID("sercon_channelstatusassign", lang).formatted(SelectMenu.OPTIONS_MAX_AMOUNT))
                        .setDisabled(holder.status.size() >= SelectMenu.OPTIONS_MAX_AMOUNT)
                        .setRequiredRange(1, 1)
                        .build()
        ));

        List<SelectOption> options = new ArrayList<>();
        String placeHolder;

        if (holder.status.isEmpty()) {
            options.add(SelectOption.of("A", "A"));
            placeHolder = LangID.getStringByID("sercon_channelstatusnochannel", lang);
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

            placeHolder = LangID.getStringByID("sercon_channelstatusremove", lang);
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
                        Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
