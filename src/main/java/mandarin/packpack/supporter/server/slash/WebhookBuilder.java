package mandarin.packpack.supporter.server.slash;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.util.EntityUtil;
import discord4j.discordjson.json.ImmutableWebhookExecuteRequest;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.MultipartRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebhookBuilder {
    private final ImmutableWebhookExecuteRequest.Builder builder = WebhookExecuteRequest.builder();
    private final ArrayList<Tuple2<String, InputStream>> files = new ArrayList<>();
    private final ArrayList<File> rawFiles = new ArrayList<>();
    private final ArrayList<ReactionEmoji> emojis = new ArrayList<>();
    public final ArrayList<BiConsumer<GatewayDiscordClient, MessageData>> postHandler = new ArrayList<>();

    private boolean jobDone = false;

    public void addEmbed(@NotNull Consumer<WebhookEmbedSpec> handler) {
        WebhookEmbedSpec spec = new WebhookEmbedSpec(builder);

        handler.accept(spec);

        spec.apply();
    }

    public void addReaction(@NotNull ReactionEmoji emoji) {
        if(!emojiExisting(emoji)) {
            emojis.add(emoji);
        }
    }

    public void setContent(@NotNull String content) {
        builder.content(content);
    }

    public void addFile(@NotNull String fileName, @NotNull InputStream stream, @Nullable File file) {
        files.add(Tuples.of(fileName, stream));

        if(file != null)
            rawFiles.add(file);
    }

    public void addPostHandler(@NotNull BiConsumer<GatewayDiscordClient, MessageData> consumer) {
        postHandler.add(consumer);
    }

    public MultipartRequest<WebhookExecuteRequest> build() {
        WebhookExecuteRequest executor = builder.build();

        if(!files.isEmpty()) {
            return MultipartRequest.ofRequestAndFiles(executor, files);
        } else {
            return MultipartRequest.ofRequest(executor);
        }
    }

    public void doAdditionalJob(GatewayDiscordClient gate, MessageData message) {
        if(!emojis.isEmpty()) {
            for(ReactionEmoji emoji : emojis) {
                gate.getRestClient().getChannelService()
                        .createReaction(message.channelId().asLong(), message.id().asLong(), EntityUtil.getEmojiString(emoji)).subscribe();
            }
        }

        if(!postHandler.isEmpty()) {
            for(BiConsumer<GatewayDiscordClient, MessageData> handler : postHandler) {
                handler.accept(gate, message);
            }
        }
    }

    public void finishJob(boolean delete) {
        if(jobDone)
            return;

        for(Tuple2<String, InputStream> file : files) {
            try {
                file.getT2().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(delete) {
            for(File f : rawFiles) {
                boolean res = f.delete();

                if(!res) {
                    System.out.println("Failed to delete file : "+f.getAbsolutePath());
                }
            }
        }

        jobDone = true;
    }

    private boolean emojiExisting(ReactionEmoji emoji) {
        if(emojis.isEmpty())
            return false;

        for(ReactionEmoji e : emojis) {
            if(e.equals(emoji))
                return true;
        }

        return false;
    }
}
