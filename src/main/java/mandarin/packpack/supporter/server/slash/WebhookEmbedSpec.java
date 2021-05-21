package mandarin.packpack.supporter.server.slash;

import discord4j.discordjson.json.*;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebhookEmbedSpec {
    private final ImmutableWebhookExecuteRequest.Builder builder;
    private final ImmutableEmbedData.Builder embedBuilder = EmbedData.builder();

    public WebhookEmbedSpec(ImmutableWebhookExecuteRequest.Builder builder) {
        this.builder = builder;
    }

    public void setImage(@NotNull String url) {
        embedBuilder.image(EmbedImageData.builder().url(url).build());
    }

    public void addField(@NotNull String key, @NotNull String value, boolean inline) {
        if(key.isBlank()) {
            key = "None";
        }

        if(value.isBlank()) {
            value = "None";
        }

        embedBuilder.addField(EmbedFieldData.builder()
                .name(key)
                .value(value)
                .inline(inline)
                .build()
        );
    }

    public void setColor(@NotNull Color c) {
        embedBuilder.color(c.getRGB());
    }

    public void setTitle(@NotNull String title) {
        embedBuilder.title(title);
    }

    public void setDescription(@NotNull String description) {
        embedBuilder.description(description);
    }

    public void setAuthor(@NotNull String name, @Nullable String url, @Nullable String iconUrl) {
        ImmutableEmbedAuthorData.Builder authorBuilder = EmbedAuthorData.builder();

        authorBuilder.name(name);

        if(url != null)
            authorBuilder.url(url);

        if(iconUrl != null)
            authorBuilder.iconUrl(iconUrl);

        embedBuilder.author(authorBuilder.build());
    }

    public void setThumbnail(@NotNull String url) {
        embedBuilder.thumbnail(EmbedThumbnailData.builder().url(url).build());
    }

    public void setFooter(@Nullable String text, @Nullable String iconUrl) {
        if(text == null && iconUrl == null)
            return;

        ImmutableEmbedFooterData.Builder footerBuilder = EmbedFooterData.builder();

        if(text != null)
            footerBuilder.text(text);

        if(iconUrl != null)
            footerBuilder.iconUrl(iconUrl);

        embedBuilder.footer(footerBuilder.build());
    }

    public void apply() {
        builder.addEmbed(embedBuilder.build());
    }
}
