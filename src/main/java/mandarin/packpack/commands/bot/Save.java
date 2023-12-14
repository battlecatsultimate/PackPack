package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class Save extends ConstraintCommand {
    public Save(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ch.sendMessage(LangID.getStringByID("save_save", lang)).queue( msg -> {
            if(msg != null) {
                StaticStore.saveServerInfo();

                msg.editMessage(LangID.getStringByID("save_done", lang)).queue();
            } else {
                onFail(loader, DEFAULT_ERROR);
            }
        });
    }
}
