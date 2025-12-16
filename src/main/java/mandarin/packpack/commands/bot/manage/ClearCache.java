package mandarin.packpack.commands.bot.manage;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class ClearCache extends ConstraintCommand {
    public ClearCache(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        List<MessageTopLevelComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("Are you sure you want to clear cache? This cannot be undone"));

        messageWithConfirmButtons(components, lang);

        replyToMessageSafely(ch, loader.getMessage(), components, msg ->
            StaticStore.putHolder(loader.getUser().getId(), new ConfirmButtonHolder(loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, lang, () -> {
                StaticStore.imgur.clear();

                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("clearCache.cleared", lang));
            }))
        );
    }
}
