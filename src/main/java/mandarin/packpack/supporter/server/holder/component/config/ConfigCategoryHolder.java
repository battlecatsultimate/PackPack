package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigCategoryHolder extends ServerConfigHolder {
    public ConfigCategoryHolder(Message author, String channelID, Message message, @NotNull IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, lang);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "general" -> connectTo(event, new ConfigGeneralHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
            case "command" -> connectTo(event, new ConfigCommandHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
            case "channel" -> connectTo(event, new ConfigChannelHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
            case "permission" -> connectTo(event, new ConfigPermissionHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
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
        }
    }

    @Override
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event, @NotNull Holder child) {
        event.deferEdit()
                .setContent(LangID.getStringByID("serverConfig.category.title", lang))
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(
                ActionRow.of(
                        Button.secondary("general", LangID.getStringByID("serverConfig.category.general", lang)).withEmoji(Emoji.fromUnicode("🎛️")),
                        Button.secondary("command", LangID.getStringByID("serverConfig.category.command", lang)).withEmoji(Emoji.fromUnicode("📟"))
                )
        );

        result.add(
                ActionRow.of(
                        Button.secondary("permission", LangID.getStringByID("serverConfig.category.permission", lang)).withEmoji(Emoji.fromUnicode("📖")),
                        Button.secondary("channel", LangID.getStringByID("serverConfig.category.channel", lang)).withEmoji(Emoji.fromUnicode("💬"))
                )
        );

        result.add(ActionRow.of(
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
