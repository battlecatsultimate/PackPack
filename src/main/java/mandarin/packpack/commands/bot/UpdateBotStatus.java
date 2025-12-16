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
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Failed to get shard manager from client...");

            return;
        }

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Updating status of top.gg...");

        BotListPlatformHandler.handleTopGG(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Updating status of discord bot list...");

        BotListPlatformHandler.handleDiscordBotList(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Updating status of Korean discord list...");

        BotListPlatformHandler.handleKoreanDiscordList(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Updating status of discord bot gg...");

        BotListPlatformHandler.handleDiscordBotGG(manager, loader.getClient().getSelfUser().getId(), true);

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Updating bot status message");

        StaticStore.updateStatus();

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Successfully updated bot status");
    }
}
