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
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigPermissionHolder extends ServerConfigHolder {
    public ConfigPermissionHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> connectTo(event, new ConfigChannelRoleSelectHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
            case "scamDetector.action.ban" -> connectTo(event, new ConfigUserBanHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
            case "manage" -> connectTo(event, new ConfigPermissionUserSelectHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
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

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
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
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
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
        return LangID.getStringByID("serverConfig.permission.documentation.title", lang) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.channelPermission.title", lang).formatted(Emoji.fromUnicode("📜")) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.channelPermission.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.commandBan.title", lang).formatted(Emoji.fromUnicode("🔨")) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.commandBan.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.permissionBan.title", lang).formatted(Emoji.fromUnicode("🔧")) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.permissionBan.description", lang);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(Button.secondary("channel", LangID.getStringByID("serverConfig.permission.button.channelPermission", lang)).withEmoji(Emoji.fromUnicode("📜"))));
        result.add(ActionRow.of(Button.secondary("scamDetector.action.ban", LangID.getStringByID("serverConfig.permission.button.commandBan", lang)).withEmoji(Emoji.fromUnicode("🔨"))));
        result.add(ActionRow.of(Button.secondary("manage", LangID.getStringByID("serverConfig.permission.button.permissionPan", lang)).withEmoji(Emoji.fromUnicode("🔧"))));

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
