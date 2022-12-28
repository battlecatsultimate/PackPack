package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class ClearCache extends ConstraintCommand {
    public ClearCache(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Message res = registerConfirmButtons(ch.sendMessage("Are you sure you want to clear cache? This cannot be undone"), lang).complete();

        Member m = getMember(event);

        if(m != null) {
            StaticStore.putHolder(m.getId(), new ConfirmButtonHolder(res, getMessage(event), ch.getId(), () -> {
                StaticStore.imgur.clear();

                ch.sendMessage(LangID.getStringByID("clearcache_cleared", lang)).queue();
            }, lang));
        }
    }
}
