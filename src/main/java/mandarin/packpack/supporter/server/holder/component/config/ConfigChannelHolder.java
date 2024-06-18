package mandarin.packpack.supporter.server.holder.component.config;

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

public class ConfigChannelHolder extends ServerConfigHolder {

    public ConfigChannelHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "event" -> {

            }
            case "announcement" -> {

            }
            case "status" -> {

            }
            case "booster" -> {

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

    @Override
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event, Holder child) {
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
        return LangID.getStringByID("sercon_channeltitle", lang) + "\n" +
                LangID.getStringByID("sercon_channeldesc", lang) + "\n" +
                LangID.getStringByID("sercon_channeleventtit", lang).formatted(Emoji.fromUnicode("🗓️")) + "\n" +
                LangID.getStringByID("sercon_channeleventdesc", lang) + "\n" +
                LangID.getStringByID("sercon_channelanntit", lang).formatted(Emoji.fromUnicode("📢")) + "\n" +
                LangID.getStringByID("sercon_channelanndesc", lang) + "\n" +
                LangID.getStringByID("sercon_channelstatustit", lang).formatted(Emoji.fromUnicode("📡")) + "\n" +
                LangID.getStringByID("sercon_channelstatusdesc", lang) + "\n" +
                LangID.getStringByID("sercon_channelboosttit", lang).formatted(Emoji.fromUnicode("📌")) + "\n" +
                LangID.getStringByID("sercon_channelboostdesc", lang);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(Button.secondary("event", LangID.getStringByID("sercon_channeleventbutton", lang)).withEmoji(Emoji.fromUnicode("🗓️"))));
        result.add(ActionRow.of(Button.secondary("announcement", LangID.getStringByID("sercon_channelannbutton", lang)).withEmoji(Emoji.fromUnicode("📢"))));
        result.add(ActionRow.of(Button.secondary("status", LangID.getStringByID("sercon_channelstatusbutton", lang)).withEmoji(Emoji.fromUnicode("📡"))));
        result.add(ActionRow.of(Button.secondary("booster", LangID.getStringByID("sercon_channelboostbutton", lang)).withEmoji(Emoji.fromUnicode("📌"))));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
