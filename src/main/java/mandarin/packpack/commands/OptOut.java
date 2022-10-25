package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class OptOut extends ConstraintCommand {
    public OptOut(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Member me = getMember(event);

        if(me != null) {
            String id = me.getId();

            Message m = registerConfirmButtons(ch.sendMessage(LangID.getStringByID("optout_warn", lang)), lang).setMessageReference(getMessage(event)).mentionRepliedUser(false).complete();

            StaticStore.putHolder(id, new ConfirmButtonHolder(m, getMessage(event), ch.getId(), () -> {
                StaticStore.optoutMembers.add(id);

                StaticStore.spamData.remove(id);
                StaticStore.prefix.remove(id);
                StaticStore.timeZones.remove(id);

                ch.sendMessage(LangID.getStringByID("optout_success", lang)).setMessageReference(getMessage(event)).mentionRepliedUser(false).queue();
            }, lang));
        } else {
            ch.sendMessage(LangID.getStringByID("optout_nomem", lang)).setMessageReference(getMessage(event)).mentionRepliedUser(false).queue();
        }
    }
}
