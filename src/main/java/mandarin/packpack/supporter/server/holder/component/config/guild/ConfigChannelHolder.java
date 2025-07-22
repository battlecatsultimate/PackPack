package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigChannelHolder extends ServerConfigHolder {

    public ConfigChannelHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "event" -> connectTo(event, new ConfigEventVersionSelectHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "announcement" -> connectTo(event, new ConfigAnnouncementHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "status" -> connectTo(event, new ConfigStatusHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "booster" -> connectTo(event, new ConfigBoosterChannelHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
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
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        return LangID.getStringByID("serverConfig.channel.documentation.title", lang) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.eventData.title", lang).formatted(Emoji.fromUnicode("🗓️")) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.eventData.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.announcement.title", lang).formatted(Emoji.fromUnicode("📢")) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.announcement.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.status.title", lang).formatted(Emoji.fromUnicode("📡")) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.status.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.boosterPin.title", lang).formatted(Emoji.fromUnicode("📌")) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.boosterPin.description", lang);
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        result.add(ActionRow.of(Button.secondary("event", LangID.getStringByID("serverConfig.channel.button.eventData", lang)).withEmoji(Emoji.fromUnicode("🗓️"))));
        result.add(ActionRow.of(Button.secondary("announcement", LangID.getStringByID("serverConfig.channel.button.announcement", lang)).withEmoji(Emoji.fromUnicode("📢"))));
        result.add(ActionRow.of(Button.secondary("status", LangID.getStringByID("serverConfig.channel.button.status", lang)).withEmoji(Emoji.fromUnicode("📡"))));
        result.add(ActionRow.of(Button.secondary("booster", LangID.getStringByID("serverConfig.channel.button.boosterPin", lang)).withEmoji(Emoji.fromUnicode("📌"))));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
