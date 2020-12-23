package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BCUStat implements Command {

    @Override
    public void doSomething(MessageCreateEvent event) {
        Message msg = event.getMessage();
        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return;

        Pauser pause = new Pauser();
        DecimalFormat df = new DecimalFormat("#.##");

        AtomicBoolean error = new AtomicBoolean(false);

        AtomicReference<StringBuilder> result = new AtomicReference<>(new StringBuilder());

        AtomicReference<Long> allUsers = new AtomicReference<>(0L);

        event.getGuild().subscribe(g -> {
            g.getMembers()
                    .filter(m -> !m.isBot())
                    .count()
                    .subscribe(l -> {
                        result.get().append("There are ").append(l).append(" human in this server\n\n");
                        allUsers.set(l);
                    });

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(StaticStore.PRE_MEMBER_ID))
                    .count()
                    .subscribe(l -> result.get().append("There are ").append(l).append(" pre-members in this server. It takes about ").append(df.format(l * 100.0 / allUsers.get())).append("% of the total number of people.\n\n"));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(StaticStore.MEMBER_ID))
                    .count()
                    .subscribe(l -> result.get().append("There are ").append(l).append(" members in this server. It takes about ").append(df.format(l * 100.0 / allUsers.get())).append("% of the total number of people.\n\n"));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(StaticStore.BCU_PC_USER_ID))
                    .count()
                    .subscribe(l -> result.get().append("There are ").append(l).append(" BCU PC users in this server. It takes about ").append(df.format(l * 100.0 / allUsers.get())).append("% of the total number of people.\n\n"));

            g.getMembers()
                    .filter(m -> !m.isBot())
                    .filter(m -> StaticStore.rolesToString(m.getRoleIds()).contains(StaticStore.BCU_ANDROId_USER_ID))
                    .count()
                    .subscribe(l -> result.get().append("There are ").append(l).append(" BCU Android users in this server. It takes about ").append(df.format(l * 100.0 / allUsers.get())).append("% of the total number of people.\n\n"));
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
