package mandarin.packpack.commands.bot;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;

public class LogOut extends ConstraintCommand {
    private final GatewayDiscordClient gate;

    public LogOut(ROLE role, int lang, IDHolder id, GatewayDiscordClient gate) {
        super(role, lang, id);

        this.gate = gate;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch != null) {
            Message msg = createMessage(ch, m -> {
                m.content("Are you sure that you want to turn off the bot?");
                registerConfirmButtons(m, 0);
            });

            getMember(event).ifPresent(m -> StaticStore.putHolder(m.getId().asString(), new ConfirmButtonHolder(msg, getMessage(event), ch.getId().asString(), m.getId().asString(), () -> {
                createMessage(ch, ms -> ms.content("Good bye!"));

                StaticStore.saver.cancel();
                StaticStore.saver.purge();
                StaticStore.saveServerInfo();
                gate.logout().subscribe();
            }, lang)));
        } else {
            StaticStore.saver.cancel();
            StaticStore.saver.purge();
            StaticStore.saveServerInfo();
            gate.logout().subscribe();
        }
    }
}
