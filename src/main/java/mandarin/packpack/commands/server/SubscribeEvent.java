package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.guild.ConfigEventVersionSelectHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class SubscribeEvent extends ConstraintCommand {
    public SubscribeEvent(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        replyToMessageSafely(ch, getContents(), loader.getMessage(), a -> a.setComponents(getComponents()), msg ->
                StaticStore.putHolder(loader.getUser().getId(), new ConfigEventVersionSelectHolder(loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, holder, lang))
        );
    }

    private String getContents() {
        return LangID.getStringByID("serverConfig.channel.documentation.title", lang) + "\n" +
                LangID.getStringByID("serverConfig.channel.documentation.eventData.title", lang).formatted(Emoji.fromUnicode("🗓️")) + "\n" +
                LangID.getStringByID("serverConfig.eventData.versionSelect", lang);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        if (holder == null)
            return result;

        CommonStatic.Lang.Locale locale = holder.config.lang;

        if (locale == null)
            return result;

        String[] idPriority;

        switch (locale) {
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

        result.add(
                ActionRow.of(
                        Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}
