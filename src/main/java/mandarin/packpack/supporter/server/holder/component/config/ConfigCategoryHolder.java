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
                        .setContent(LangID.getStringByID("sercon_done", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("sercon_cancelask", lang));

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
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event, @NotNull Holder child) {
        event.deferEdit()
                .setContent(LangID.getStringByID("sercon_category", lang))
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(
                ActionRow.of(
                        Button.secondary("general", LangID.getStringByID("sercon_general", lang)).withEmoji(Emoji.fromUnicode("🎛️")),
                        Button.secondary("command", LangID.getStringByID("sercon_command", lang)).withEmoji(Emoji.fromUnicode("📟"))
                )
        );

        result.add(
                ActionRow.of(
                        Button.secondary("permission", LangID.getStringByID("sercon_perm", lang)).withEmoji(Emoji.fromUnicode("📖")),
                        Button.secondary("channel", LangID.getStringByID("sercon_channel", lang)).withEmoji(Emoji.fromUnicode("💬"))
                )
        );

        result.add(ActionRow.of(
                Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
