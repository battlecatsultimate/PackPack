package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.ConfigCategoryHolder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig extends ConstraintCommand {
    public ServerConfig(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        if (holder == null)
            return;

        replyToMessageSafely(loader.getChannel(), LangID.getStringByID("sercon_category", lang), loader.getMessage(), a -> a.setComponents(getComponents()), msg ->
                StaticStore.putHolder(loader.getMember().getId(), new ConfigCategoryHolder(loader.getMessage(), loader.getChannel().getId(), msg, holder, lang))
        );
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
