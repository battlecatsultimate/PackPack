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
import java.util.Objects;

public class ConfigEventVersionSelectHolder extends ServerConfigHolder {
    public ConfigEventVersionSelectHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    public ConfigEventVersionSelectHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "en" -> connectTo(event, new ConfigEventManagerHolder(getAuthorMessage(), channelID, message, holder, backup, lang, CommonStatic.Lang.Locale.EN));
            case "jp" -> connectTo(event, new ConfigEventManagerHolder(getAuthorMessage(), channelID, message, holder, backup, lang, CommonStatic.Lang.Locale.JP));
            case "tw" -> connectTo(event, new ConfigEventManagerHolder(getAuthorMessage(), channelID, message, holder, backup, lang, CommonStatic.Lang.Locale.ZH));
            case "kr" -> connectTo(event, new ConfigEventManagerHolder(getAuthorMessage(), channelID, message, holder, backup, lang, CommonStatic.Lang.Locale.KR));
            case "back" -> goBack(event);
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
        return LangID.getStringByID("serverConfig.channel.documentation.title", lang) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.eventData.title", lang).formatted(Emoji.fromUnicode("🗓️")) + "\n" +
                LangID.getStringByID("serverConfig.eventData.versionSelect", lang);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        String[] idPriority;

        CommonStatic.Lang.Locale language = Objects.requireNonNull(holder.config.lang);

        switch (language) {
            case ZH -> idPriority = new String[] { "tw", "jp", "en", "kr" };
            case KR -> idPriority = new String[] { "kr", "jp", "en", "tw" };
            case JP -> idPriority = new String[] { "jp", "en", "tw", "kr" };
            default -> idPriority = new String[] { "en", "jp", "tw", "kr" };
        }

        for (String id : idPriority) {
            Emoji emoji;
            String label;

            switch (id) {
                case "en" -> {
                    emoji = Emoji.fromUnicode("🇺🇸");
                    label = LangID.getStringByID("serverConfig.eventData.version.en", lang);
                }
                case "jp" -> {
                    emoji = Emoji.fromUnicode("🇯🇵");
                    label = LangID.getStringByID("serverConfig.eventData.version.jp", lang);
                }
                case "tw" -> {
                    emoji = Emoji.fromUnicode("🇹🇼");
                    label = LangID.getStringByID("serverConfig.eventData.version.tw", lang);
                }
                case "kr" -> {
                    emoji = Emoji.fromUnicode("🇰🇷");
                    label = LangID.getStringByID("serverConfig.eventData.version.kr", lang);
                }
                default -> throw new IllegalStateException("E/ConfigEventVersionSelectHolder::getComponents - Unknown locale type %s found".formatted(id));
            }

            result.add(ActionRow.of(Button.secondary(id, label).withEmoji(emoji)));
        }

        if (parent != null) {
            result.add(
                    ActionRow.of(
                            Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                            Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                            Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                    )
            );
        } else {
            result.add(
                    ActionRow.of(
                            Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                            Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                    )
            );
        }

        return result;
    }
}
