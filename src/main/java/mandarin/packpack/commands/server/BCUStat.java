package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BCUStat extends Command {

    private final IDHolder holder;

    public BCUStat(int lang, IDHolder holder) {
        super(lang);

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

        DecimalFormat df = new DecimalFormat("#.##");

        AtomicBoolean error = new AtomicBoolean(false);

        AtomicReference<StringBuilder> result = new AtomicReference<>(new StringBuilder());

        AtomicReference<Long> allUsers = new AtomicReference<>(0L);

        getGuild(event).subscribe(g -> {
            g.getMembers()
                    .filter(m -> !m.isBot())
                    .count()
                    .subscribe(l -> {
                        result.get().append(LangID.getStringByID("bcustat_human", lang).replace("_", Long.toString(l)));
                        allUsers.set(l);
                    });

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> holder.PRE_MEMBER != null && StaticStore.rolesToString(m.getRoleIds()).contains(holder.PRE_MEMBER))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_prem", lang).replace("_", String.valueOf(l)).replace("=", df.format(l * 100.0 / allUsers.get()))));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> holder.MEMBER != null && StaticStore.rolesToString(m.getRoleIds()).contains(holder.MEMBER))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_mem", lang).replace("_", String.valueOf(l)).replace("=", df.format(l * 100.0 / allUsers.get()))));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> holder.BCU_PC_USER != null && StaticStore.rolesToString(m.getRoleIds()).contains(holder.BCU_PC_USER))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_pc", lang).replace("_", String.valueOf(l)).replace("=", df.format(l * 100.0 / allUsers.get()))));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> holder.BCU_ANDROID != null && StaticStore.rolesToString(m.getRoleIds()).contains(holder.BCU_ANDROID))
                    .count()
                    .subscribe(l -> result.get().append(LangID.getStringByID("bcustat_and", lang).replace("_", String.valueOf(l)).replace("=",df.format(l * 100.0 / allUsers.get()))));
        }, e -> {
            ch.createMessage(StaticStore.ERROR_MSG).subscribe();
            error.set(true);
        }, pause::resume);

        pause.pause(() -> {
            ch.createMessage(StaticStore.ERROR_MSG).subscribe();
            error.set(true);
        });

        if(!error.get())
            ch.createMessage(result.get().toString()).subscribe();
    }
}
