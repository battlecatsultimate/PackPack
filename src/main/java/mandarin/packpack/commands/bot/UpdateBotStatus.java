package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.BotListPlatformHandler;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

public class UpdateBotStatus extends ConstraintCommand {
    public UpdateBotStatus(ROLE role, CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        ShardManager manager = loader.getClient().getShardManager();

        if (manager == null) {
            replyToMessageSafely(loader.getChannel(), "Failed to get shard manager from client...", loader.getMessage(), a -> a);

            return;
        }

        replyToMessageSafely(loader.getChannel(), "Updating status of top.gg...", loader.getMessage(), a -> a);

        BotListPlatformHandler.handleTopGG(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), "Updating status of discord bot list...", loader.getMessage(), a -> a);

        BotListPlatformHandler.handleDiscordBotList(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), "Updating status of Korean discord list...", loader.getMessage(), a -> a);

        BotListPlatformHandler.handleKoreanDiscordList(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), "Updating status of discord bot gg...", loader.getMessage(), a -> a);

        BotListPlatformHandler.handleDiscordBotGG(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), "Updating bot status message", loader.getMessage(), a -> a);

        StaticStore.updateStatus();

        replyToMessageSafely(loader.getChannel(), "Successfully updated bot status", loader.getMessage(), a -> a);
    }
}
