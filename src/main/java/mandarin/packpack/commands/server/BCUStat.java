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
            System.out.println(result.get());

            Long human = g.getMembers()
                    .filter(m -> !m.isBot())
                    .count()
                    .block();

            if(human == null)
                return;

            result.get().append(LangID.getStringByID("bcustat_human", lang).replace("_", Long.toString(human)));
            allUsers.set(human);

            Long member = g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> holder.MEMBER != null && StaticStore.rolesToString(m.getRoleIds()).contains(holder.MEMBER))
                    .count()
                    .block();

            if(member == null)
                return;

            result.get().append(LangID.getStringByID("bcustat_mem", lang).replace("_", String.valueOf(member)).replace("=", df.format(member * 100.0 / allUsers.get())));

            for(String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                if(id == null)
                    continue;

                Long c = g.getMembers()
                        .filter(m -> !m.isBot())
                        .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(id))
                        .count()
                        .block();

                if(c == null)
                    continue;

                result.get().append(LangID.getStringByID("bcustat_role", lang).replace("_MMM_", String.valueOf(c)).replace("=", df.format(c * 100.0 / allUsers.get())).replace("_NNN_", limitName(name)));

                System.out.println(result.get());
            }
        }, e -> {
            StaticStore.logger.uploadErrorLog(e, "Error during perform BCUStat command");
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

    private String limitName(String name) {
        if(name.length() > 20) {
            return name.substring(0, 17) + "...";
        }

        return name;
    }
}
