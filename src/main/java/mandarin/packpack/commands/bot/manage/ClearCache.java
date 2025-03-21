package mandarin.packpack.commands.bot.manage;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class ClearCache extends ConstraintCommand {
    public ClearCache(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        registerConfirmButtons(ch.sendMessage("Are you sure you want to clear cache? This cannot be undone"), lang).queue(res -> {
            Member m = loader.getMember();

            StaticStore.putHolder(m.getId(), new ConfirmButtonHolder(loader.getMessage(), m.getId(), ch.getId(), res, lang, () -> {
                StaticStore.imgur.clear();

                ch.sendMessage(LangID.getStringByID("clearCache.cleared", lang)).queue();
            }));
        });
    }
}
