package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class LogOut extends ConstraintCommand {
    public LogOut(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        JDA client = event.getJDA();

        if(ch != null) {
            Message msg = registerConfirmButtons(ch.sendMessage("Are you sure that you want to turn off the bot?"), 0).complete();

            User u = getUser(event);

            if(u != null) {
                StaticStore.putHolder(u.getId(), new ConfirmButtonHolder(msg, getMessage(event), ch.getId(), () -> {
                    ch.sendMessage("Good bye!").queue();

                    StaticStore.saver.cancel();
                    StaticStore.saver.purge();
                    StaticStore.saveServerInfo();
                    client.shutdown();
                    System.exit(0);
                }, lang));
            }
        } else {
            StaticStore.saver.cancel();
            StaticStore.saver.purge();
            StaticStore.saveServerInfo();
            client.shutdown();
            System.exit(0);
        }
    }
}
