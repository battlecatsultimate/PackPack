package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class OptOut extends ConstraintCommand {
    public OptOut(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        User u = loader.getUser();

        String id = u.getId();

        replyToMessageSafely(ch, LangID.getStringByID("optout_warn", lang), loader.getMessage(), a -> registerConfirmButtons(a, lang), m ->
            StaticStore.putHolder(id, new ConfirmButtonHolder(loader.getMessage(), m, ch.getId(), lang, () -> {
                StaticStore.optoutMembers.add(id);

                StaticStore.spamData.remove(id);
                StaticStore.prefix.remove(id);
                StaticStore.timeZones.remove(id);

                replyToMessageSafely(ch, LangID.getStringByID("optout_success", lang), loader.getMessage(), a -> a);
            }))
        );
    }
}
