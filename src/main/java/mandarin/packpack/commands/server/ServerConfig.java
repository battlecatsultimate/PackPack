package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.guild.ConfigCategoryHolder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig extends ConstraintCommand {
    public ServerConfig(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        if (holder == null)
            return;

        replyToMessageSafely(loader.getChannel(), LangID.getStringByID("serverConfig.category.title", lang), loader.getMessage(), a -> a.setComponents(getComponents()), msg ->
                StaticStore.putHolder(loader.getMember().getId(), new ConfigCategoryHolder(loader.getMessage(), loader.getMember().getId(), loader.getChannel().getId(), msg, holder, lang))
        );
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

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
