package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;

import java.util.concurrent.atomic.AtomicReference;

public class CheckBCU implements Command {
    @Override
    public void doSomething(MessageCreateEvent event) {
        Message msg = event.getMessage();
        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return;

        if(StaticStore.checkingBCU) {
            ch.createMessage("I'm already performing this command! Wait for me to finish this");
        } else {
            StaticStore.checkingBCU = true;

            Pauser pause = new Pauser();

            AtomicReference<StringBuilder> both = new AtomicReference<>(new StringBuilder("BOTH : "));
            AtomicReference<StringBuilder> none = new AtomicReference<>(new StringBuilder("NONE : "));

            event.getGuild()
                    .subscribe(g -> g.getMembers()
                        .filter(m -> !StaticStore.rolesToString(m.getRoleIds()).contains(StaticStore.MUTED))
                        .subscribe(m -> {
                            boolean pre = false;
                            boolean mem = false;

                            String role = StaticStore.rolesToString(m.getRoleIds());

                            if(role.contains(StaticStore.PRE_MEMBER_ID))
                                pre = true;

                            if(role.contains(StaticStore.MEMBER_ID))
                                mem = true;

                            if (!pre && !mem)
                                none.get().append(m.getUsername()).append(", ");

                            if (pre && mem)
                                both.get().append(m.getUsername()).append(", ");
                        }, e -> ch.createMessage(StaticStore.ERROR_MSG).subscribe(), pause::resume));

            pause.pause(() -> onFail(event));

            ch.createMessage(both.get().substring(0, both.get().length()-2)+"\n"+none.get().substring(0, none.get().length()-2)).subscribe();

            StaticStore.checkingBCU = false;
        }
    }
}
