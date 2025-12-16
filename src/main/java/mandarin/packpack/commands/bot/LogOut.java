package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.lwjgl.opengl.RenderSessionManager;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LogOut extends ConstraintCommand {
    public LogOut(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        if (!loader.getUser().getId().equals(StaticStore.MANDARIN_SMELL) && !StaticStore.maintainers.contains(loader.getUser().getId())) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("bot.denied.reason.noPermission.developer", lang));

            return;
        }

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), "Format : `p!lo -b/-u/-a/-m/-p`\n\n-b : Bug fix\n-u : Update\n-a : API Update\n-m : Maintenance\n-p : Permanent out of service");

            return;
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("Are you sure that you want to turn off the bot?"));

        containerWithConfirmButtons(components, lang);

        replyToMessageSafely(ch, loader.getMessage(), msg -> {
            User u = loader.getUser();

            StaticStore.putHolder(u.getId(), new ConfirmButtonHolder(loader.getMessage(), u.getId(), ch.getId(), msg, lang, () -> {
                String self = ch.getJDA().getSelfUser().getAsMention();

                String code = switch (contents[1]) {
                    case "-b" -> "bot.status.offline.reason.bug";
                    case "-a" -> "bot.status.offline.reason.api";
                    case "-p" -> "bot.status.offline.reason.endOfService";
                    case "-m" -> "bot.status.offline.reason.maintenance";
                    default -> "bot.status.offline.reason.update";
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

                            String fullMessage = String.format(LangID.getStringByID("bot.status.offline.format", id.config.lang), self, LangID.getStringByID(code, id.config.lang));

                            CountDownLatch countDown = new CountDownLatch(1);

                            ((MessageChannel) c).sendMessageComponents(TextDisplay.of(fullMessage))
                                    .useComponentsV2()
                                    .setAllowedMentions(new ArrayList<>())
                                    .queue(res -> countDown.countDown(), e -> countDown.countDown());

                            countDown.await();
                        }
                    } catch (Exception ignored) {
                    }
                }

                try {
                    CountDownLatch countDown = new CountDownLatch(1);

                    replyToMessageSafely(ch, loader.getMessage(), "Good bye!", unused -> countDown.countDown(), e -> countDown.countDown());

                    countDown.await();
                } catch (Exception ignored) {

                }

                StaticStore.saver.cancel();
                StaticStore.saver.purge();

                StaticStore.executorHandler.release();

                StaticStore.safeClose = true;

                StaticStore.saveServerInfo();

                client.shutdown();

                StaticStore.renderManager.setReleaseFlag(true);
                RenderSessionManager.Companion.terminate();

                LogManager.shutdown();
            }));
        }, Container.of(components));
    }
}
