package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Save extends ConstraintCommand {
    public Save(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        Message msg = ch.sendMessage(LangID.getStringByID("save_save", lang)).complete();

        if(msg != null) {
            StaticStore.saveServerInfo();

            msg.editMessage(LangID.getStringByID("save_done", lang)).queue();
        } else {
            onFail(event, DEFAULT_ERROR);
        }
    }
}
