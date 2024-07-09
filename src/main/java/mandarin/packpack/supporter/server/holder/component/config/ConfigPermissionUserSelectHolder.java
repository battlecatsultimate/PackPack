package mandarin.packpack.supporter.server.holder.component.config;

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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigPermissionUserSelectHolder extends ServerConfigHolder {
    public ConfigPermissionUserSelectHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
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
                    event.deferReply().setEphemeral(true).setAllowedMentions(new ArrayList<>()).setContent(LangID.getStringByID("sercon_permissionuserself", lang)).queue();

                    return;
                }

                User u = e.getMentions().getUsers().getFirst();

                if (u.isBot()) {
                    event.deferReply().setEphemeral(true).setAllowedMentions(new ArrayList<>()).setContent(LangID.getStringByID("sercon_permissionuserbot", lang)).queue();

                    return;
                }

                connectTo(event, new ConfigPermissionUserPermissionHolder(getAuthorMessage(), channelID, message, holder, backup, g, id, lang));
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

    @Override
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event, @NotNull Holder child) {
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
        return LangID.getStringByID("sercon_permission", lang) + "\n" +
                LangID.getStringByID("sercon_permissionmanage", lang).formatted(Emoji.fromUnicode("🔧")) + "\n" +
                LangID.getStringByID("sercon_permissiondeactivateuser", lang);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(
                EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                        .setPlaceholder(LangID.getStringByID("sercon_permissiondeactivateselect", lang))
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
