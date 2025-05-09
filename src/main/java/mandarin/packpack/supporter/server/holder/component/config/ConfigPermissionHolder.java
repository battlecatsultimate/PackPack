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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigPermissionHolder extends ServerConfigHolder {
    public ConfigPermissionHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "channel" -> connectTo(event, new ConfigChannelRoleSelectHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "command" -> connectTo(event, new ConfigUserBanHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "manage" -> connectTo(event, new ConfigPermissionUserSelectHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "prefix" -> connectTo(event, new ConfigPrefixBanHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
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
        return LangID.getStringByID("serverConfig.permission.documentation.title", lang) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.channelPermission.title", lang).formatted(Emoji.fromUnicode("📜")) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.channelPermission.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.commandBan.title", lang).formatted(Emoji.fromUnicode("🔨")) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.commandBan.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.permissionBan.title", lang).formatted(Emoji.fromUnicode("🔧")) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.permissionBan.description", lang) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.prefixBan.title", lang).formatted(Emoji.fromUnicode("📋")) + "\n" +
                LangID.getStringByID("serverConfig.permission.documentation.prefixBan.description", lang);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(Button.secondary("channel", LangID.getStringByID("serverConfig.permission.button.channelPermission", lang)).withEmoji(Emoji.fromUnicode("📜"))));
        result.add(ActionRow.of(Button.secondary("command", LangID.getStringByID("serverConfig.permission.button.commandBan", lang)).withEmoji(Emoji.fromUnicode("🔨"))));
        result.add(ActionRow.of(Button.secondary("manage", LangID.getStringByID("serverConfig.permission.button.permissionBan", lang)).withEmoji(Emoji.fromUnicode("🔧"))));
        result.add(ActionRow.of(Button.secondary("prefix", LangID.getStringByID("serverConfig.permission.button.prefixBan", lang)).withEmoji(Emoji.fromUnicode("📋"))));

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
