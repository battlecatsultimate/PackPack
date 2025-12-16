package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class Save extends ConstraintCommand {
    public Save(ROLE role, CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("save.saving", lang), msg -> {
            StaticStore.saveServerInfo();

            msg.editMessageComponents(TextDisplay.of(LangID.getStringByID("save.done", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .queue();
        }, e -> {
            StaticStore.logger.uploadErrorLog(e, "E/Save::doSomething - Failed to send saving notification mesage");

            onFail(loader, DEFAULT_ERROR);
        });
    }
}
