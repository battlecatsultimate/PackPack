package mandarin.packpack.supporter.server.slash;

import discord4j.discordjson.json.ImmutableWebhookExecuteRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.WebhookMultipartRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class WebhookBuilder {
    private final ImmutableWebhookExecuteRequest.Builder builder = WebhookExecuteRequest.builder();
    private final ArrayList<Tuple2<String, InputStream>> files = new ArrayList<>();
    private final ArrayList<File> rawFiles = new ArrayList<>();

    private boolean jobDone = false;

    public void addEmbed(@NotNull Consumer<WebhookEmbedSpec> handler) {
        WebhookEmbedSpec spec = new WebhookEmbedSpec(builder);

        handler.accept(spec);

        spec.apply();
    }

    public void setContent(@NotNull String content) {
        builder.content(content);
    }

    public void addFile(@NotNull String fileName, @NotNull InputStream stream, @Nullable File file) {
        files.add(Tuples.of(fileName, stream));

        if(file != null)
            rawFiles.add(file);
    }

    public WebhookMultipartRequest build() {
        WebhookExecuteRequest executor = builder.build();

        if(!files.isEmpty()) {
            return new WebhookMultipartRequest(executor, files);
        } else {
            return new WebhookMultipartRequest(executor);
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
}
