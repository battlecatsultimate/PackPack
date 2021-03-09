package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.concurrent.atomic.AtomicReference;

public class CheckBCU implements Command {
    private final int lang;
    private final IDHolder holder;

    public CheckBCU(int lang, IDHolder holder) {
        this.lang = lang;
        this.holder = holder;
    }

    @Override
    public void doSomething(MessageEvent event) {
        Message msg = getMessage(event);

        if(msg == null)
            return;

        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return;

        if(StaticStore.checkingBCU) {
            ch.createMessage(LangID.getStringByID("chbcu_perform", lang)).subscribe();
        } else {
            StaticStore.checkingBCU = true;

            AtomicReference<StringBuilder> both = new AtomicReference<>(new StringBuilder("BOTH : "));
            AtomicReference<StringBuilder> none = new AtomicReference<>(new StringBuilder("NONE : "));

            getGuild(event)
                    .subscribe(g -> g.getMembers()
                        .filter(m -> holder.MUTED != null && !StaticStore.rolesToString(m.getRoleIds()).contains(holder.MUTED))
                        .subscribe(m -> {
                            boolean pre = false;
                            boolean mem = false;

                            String role = StaticStore.rolesToString(m.getRoleIds());

                            if(holder.PRE_MEMBER != null && role.contains(holder.PRE_MEMBER))
                                pre = true;

                            if(holder.MEMBER != null && role.contains(holder.MEMBER))
                                mem = true;

                            if (!pre && !mem)
                                none.get().append(m.getUsername()).append(", ");

                            if (pre && mem)
                                both.get().append(m.getUsername()).append(", ");
                        }, e -> ch.createMessage(StaticStore.ERROR_MSG).subscribe(), pause::resume));

            pause.pause(() -> onFail(event, DEFAULT_ERROR));

            ch.createMessage(both.get().substring(0, both.get().length()-2)+"\n"+none.get().substring(0, none.get().length()-2)).subscribe();

            StaticStore.checkingBCU = false;
        }
    }
}
