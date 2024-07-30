package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigPermissionUserSelectHolder extends ServerConfigHolder {
    public ConfigPermissionUserSelectHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "user" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                Guild g = event.getGuild();

                if (g == null)
                    return;

                String id = e.getValues().getFirst().getId();

                if (id.equals(userID)) {
                    event.deferReply().setEphemeral(true).setAllowedMentions(new ArrayList<>()).setContent(LangID.getStringByID("serverConfig.permissionBan.user.cantBan.reason.self", lang)).queue();

                    return;
                }

                User u = e.getMentions().getUsers().getFirst();

                if (u.isBot()) {
                    event.deferReply().setEphemeral(true).setAllowedMentions(new ArrayList<>()).setContent(LangID.getStringByID("serverConfig.permissionBan.user.cantBan.reason.bot", lang)).queue();

                    return;
                }

                connectTo(event, new ConfigPermissionUserPermissionHolder(getAuthorMessage(), channelID, message, holder, backup, g, id, lang));
            }
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end();
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

                    end();
                }, lang));
            }
            case "back" -> goBack(event);
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
                LangID.getStringByID("serverConfig.permission.documentation.permissionBan.title", lang).formatted(Emoji.fromUnicode("🔧")) + "\n" +
                LangID.getStringByID("serverConfig.permissionBan.description", lang);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(
                EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                        .setPlaceholder(LangID.getStringByID("serverConfig.permissionBan.selectUser", lang))
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
