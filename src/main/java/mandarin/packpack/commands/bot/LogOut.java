package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;

public class LogOut extends ConstraintCommand {
    public LogOut(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        JDA client = event.getJDA();

        if(ch != null) {
            String[] contents = getContent(event).split(" ");

            if(contents.length < 2) {
                replyToMessageSafely(ch, "Format : `p!lo -b/-u/-a/-p`\n\n-b : Bug fix\n-u : Update\n-a : API Update\n-p : Permanent out of service", getMessage(event), a -> a);

                return;
            }

            Message msg = registerConfirmButtons(ch.sendMessage("Are you sure that you want to turn off the bot?"), 0).complete();

            User u = getUser(event);

            if(u != null) {
                StaticStore.putHolder(u.getId(), new ConfirmButtonHolder(msg, getMessage(event), ch.getId(), () -> {
                    String self = client.getSelfUser().getAsMention();
                    String code;

                    switch (contents[1]) {
                        case "-b":
                            code = "bot_bug";
                            break;
                        case "-a":
                            code = "bot_api";
                            break;
                        case "-p":
                            code = "bot_end";
                            break;
                        default:
                            code = "bot_update";
                    }

                    for(String key : StaticStore.idHolder.keySet()) {
                        try {
                            IDHolder id = StaticStore.idHolder.get(key);

                            if(id == null || id.STATUS == null)
                                continue;

                            Guild guild = client.getGuildById(key);

                            if(guild == null)
                                continue;

                            GuildChannel channel = guild.getGuildChannelById(id.STATUS);

                            if(!(channel instanceof MessageChannel) || !((MessageChannel) channel).canTalk())
                                continue;

                            String fullMessage = String.format(LangID.getStringByID("bot_offline", id.config.lang), self, LangID.getStringByID(code, id.config.lang));

                            ((MessageChannel) channel).sendMessage(fullMessage)
                                    .setAllowedMentions(new ArrayList<>())
                                    .complete();
                        } catch (Exception ignored) {}
                    }

                    ch.sendMessage("Good bye!").complete();

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
