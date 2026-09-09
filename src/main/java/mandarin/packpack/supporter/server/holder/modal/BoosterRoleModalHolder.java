package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import common.io.assets.UpdateCheck;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BoosterRoleModalHolder extends ModalHolder {
    private final Guild g;

    public BoosterRoleModalHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull Guild g, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.g = g;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) throws Exception {
        if (!event.getModalId().equals("role"))
            return;

        List<Message.Attachment> attachments = getAttachmentFromMap(event.getValues(), "roleIcon");

        if (attachments.isEmpty()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.noAttachment", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        Message.Attachment attachment = attachments.getFirst();

        if (!attachment.getFileName().matches(".+\\.(png|jpg)$")) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.notSupported", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (g.getRoles().size() >= 250) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.noSlot", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (attachment.getSize() >= 256L * 1024L) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.tooLarge", lang)))
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
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.noDownloader", lang)))
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
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.noFile", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        goBack(event, downloadedFile);
    }

    @Override
    public void clean() {

    }
}
