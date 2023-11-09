package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lwjgl.opengl.RenderSessionManager;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogOut extends ConstraintCommand {
    public LogOut(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();
        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, "Format : `p!lo -b/-u/-a/-p`\n\n-b : Bug fix\n-u : Update\n-a : API Update\n-p : Permanent out of service", loader.getMessage(), a -> a);

            return;
        }

        registerConfirmButtons(ch.sendMessage("Are you sure that you want to turn off the bot?"), 0).queue( msg -> {
            User u = loader.getUser();

            StaticStore.putHolder(u.getId(), new ConfirmButtonHolder(loader.getMessage(), msg, ch.getId(), () -> {
                String self = ch.getJDA().getSelfUser().getAsMention();

                String code = switch (contents[1]) {
                    case "-b" -> "bot_bug";
                    case "-a" -> "bot_api";
                    case "-p" -> "bot_end";
                    default -> "bot_update";
                };

                for (String key : StaticStore.idHolder.keySet()) {
                    try {
                        IDHolder id = StaticStore.idHolder.get(key);

                        if (id == null || id.status.isEmpty())
                            continue;

                        Guild guild = client.getGuildById(key);

                        if (guild == null)
                            continue;

                        for (int i = 0; i < id.status.size(); i++) {
                            GuildChannel c = guild.getGuildChannelById(id.status.get(i));

                            if (!(c instanceof MessageChannel) || !((MessageChannel) c).canTalk())
                                continue;

                            String fullMessage = String.format(LangID.getStringByID("bot_offline", id.config.lang), self, LangID.getStringByID(code, id.config.lang));

                            AtomicBoolean running = new AtomicBoolean(true);

                            ((MessageChannel) c).sendMessage(fullMessage)
                                    .setAllowedMentions(new ArrayList<>())
                                    .queue(res -> running.set(false), e -> running.set(false));

                            while (true) {
                                if (!running.get())
                                    break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                AtomicBoolean running = new AtomicBoolean(true);

                ch.sendMessage("Good bye!").queue(res -> running.set(false), e -> running.set(false));

                while (true) {
                    if (!running.get())
                        break;
                }

                StaticStore.saver.cancel();
                StaticStore.saver.purge();

                StaticStore.safeClose = true;

                StaticStore.saveServerInfo();

                client.shutdown();

                StaticStore.renderManager.renderSessionManager.closeAll();
                RenderSessionManager.Companion.terminate();

                System.exit(0);
            }, lang));
        });
    }
}
