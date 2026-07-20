package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import common.io.assets.UpdateCheck;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.holder.component.booster.BoosterEmojiCreateHolder;
import mandarin.packpack.supporter.server.holder.component.booster.BoosterEmojiListHolder;
import mandarin.packpack.supporter.server.holder.component.booster.BoosterEmojiModifyHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BoosterEmojiModalHolder extends ModalHolder {
    private final Member targetMember;
    private final Guild g;

    private final BoosterHolder boosterHolder;

    public BoosterEmojiModalHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull Member targetMember, @NotNull Guild g, @NotNull BoosterHolder boosterHolder, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.targetMember = targetMember;
        this.g = g;

        this.boosterHolder = boosterHolder;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) throws Exception {
        if (!event.getModalId().equals("emoji"))
            return;

        List<Message.Attachment> attachments = getAttachmentFromMap(event.getValues(), "emojiFile");

        if (attachments.isEmpty()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.noAttachment", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        Message.Attachment attachment = attachments.getFirst();

        if (!attachment.getFileName().matches(".+\\.(png|jpg|gif)$")) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.notSupported", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (attachment.getFileName().matches(".+\\.(png|jpg)$") && g.getEmojis().stream().filter(e -> !e.isAnimated()).count() >= g.getMaxEmojis()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.noSlot", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        } else if (g.getEmojis().stream().filter(CustomEmoji::isAnimated).count() >= g.getMaxEmojis()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.noSlot", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (attachment.getSize() >= 256L * 1024L) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.tooLarge", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        UpdateCheck.Downloader downloader = StaticStore.getDownloader(attachment, new File("./temp"));

        if (downloader == null) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.noDownloader", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        downloader.run(_ -> {});

        File downloadedFile = downloader.target;

        if (!downloadedFile.exists()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.noFile", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        if (parent instanceof BoosterEmojiCreateHolder || parent instanceof BoosterEmojiModifyHolder) {
            goBack(event, downloadedFile);
        } else if (parent instanceof BoosterEmojiListHolder) {
            parent.connectTo(event, new BoosterEmojiCreateHolder(getAuthorMessage(), userID, channelID, message, targetMember, g, boosterHolder, lang, downloadedFile));
        }
    }

    @Override
    public void clean() {

    }
}
