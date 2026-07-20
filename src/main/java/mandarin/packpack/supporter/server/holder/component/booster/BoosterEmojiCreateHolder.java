package mandarin.packpack.supporter.server.holder.component.booster;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.BoosterEmojiModalHolder;
import mandarin.packpack.supporter.server.holder.modal.BoosterEmojiNameModalHolder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BoosterEmojiCreateHolder extends ComponentHolder {
    private final Member targetMember;
    private final Guild g;

    private final BoosterHolder boosterHolder;

    private File emojiFile;
    private String emojiName;

    public BoosterEmojiCreateHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull Member targetMember, @NotNull Guild g, @NotNull BoosterHolder boosterHolder, @NotNull CommonStatic.Lang.Locale lang, @NotNull File emojiFile) {
        super(author, userID, channelID, message, lang);

        this.targetMember = targetMember;
        this.g = g;

        this.boosterHolder = boosterHolder;

        this.emojiFile = emojiFile;

        int i = 0;
        List<RichCustomEmoji> emojis = g.getEmojis();

        while(true) {
            emojiName = "emoji" + i;

            if (emojis.stream().anyMatch(e -> e.getName().equals(emojiName))) {
                i++;
            } else {
                break;
            }
        }
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) throws Exception {
        switch (event.getComponentId()) {
            case "name" -> {
                TextInput input = TextInput.create("name", TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("boosterEmoji.name.field", lang))
                        .setRequiredRange(2, RichCustomEmoji.EMOJI_NAME_MAX_LENGTH)
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("name", LangID.getStringByID("boosterEmoji.name.title", lang))
                        .addComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.name.description", lang)))
                        .addComponents(Label.of(LangID.getStringByID("boosterEmoji.name.label", lang), input))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new BoosterEmojiNameModalHolder(getAuthorMessage(), userID, channelID, message, lang));
            }
            case "image" -> {
                AttachmentUpload upload = AttachmentUpload.create("emojiFile")
                        .setRequired(true)
                        .setRequiredRange(1, 1)
                        .build();

                Modal modal = Modal.create("emoji", LangID.getStringByID("boosterEmoji.upload.title", lang))
                        .addComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.upload.description", lang)))
                        .addComponents(Label.of(LangID.getStringByID("boosterEmoji.upload.file", lang), upload))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new BoosterEmojiModalHolder(getAuthorMessage(), userID, channelID, message, targetMember, g, boosterHolder, lang));
            }
            case "create" -> g.createEmoji(emojiName, Icon.from(emojiFile)).queue(emoji -> {
                boosterHolder.serverBooster.put(targetMember.getIdLong(), new BoosterData(emoji.getIdLong(), BoosterData.INITIAL.EMOJI));

                event.deferReply()
                        .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.create.success", lang).formatted(targetMember.getIdLong(), emoji.getFormatted(), emoji.getIdLong())))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .setEphemeral(true)
                        .queue();

                goBack();
            }, e -> {
                StaticStore.logger.uploadErrorLog(e, "E/BoosterEmojiCreateHolder::onEvent - Failed to add booster emoji");

                StaticStore.deleteFile(emojiFile, true);

                event.deferReply()
                        .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.failedAdd", lang)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .setEphemeral(true)
                        .queue();

                goBack();
            });
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("boosterEmoji.create.cancel", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    StaticStore.deleteFile(emojiFile, true);

                    goBack(e);
                }, lang));
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {

    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child, Object... additional) throws Exception {
        if (additional.length > 0) {
            switch (additional[0]) {
                case File file -> {
                    StaticStore.deleteFile(emojiFile, true);
                    emojiFile = file;
                }
                case String name -> emojiName = name;
                default -> {}
            }
        }

        applyResult(event);
    }

    @Override
    public void onConnected(Holder parent, Object... additional) throws Exception {
        if (additional.length > 0 && additional[0] instanceof File file) {
            StaticStore.deleteFile(emojiFile, true);
            emojiFile = file;
        }

        applyResult();
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult() {
        message.editMessageComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private Container getComponents() {
        List<ContainerChildComponent> children = new ArrayList<>();

        children.add(TextDisplay.of("## " + LangID.getStringByID("boosterEmoji.create.title", lang)));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(emojiFile, "emoji." + FilenameUtils.getExtension(emojiFile.getName())))));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(TextDisplay.of(LangID.getStringByID("boosterEmoji.create.member", lang).formatted(targetMember.getIdLong(), targetMember.getIdLong())));
        children.add(TextDisplay.of(LangID.getStringByID("boosterEmoji.create.name", lang).formatted(emojiName)));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(ActionRow.of(Button.secondary("name", LangID.getStringByID("boosterEmoji.create.changeName", lang)).withEmoji(Emoji.fromUnicode("🏷️"))));
        children.add(ActionRow.of(Button.secondary("image", LangID.getStringByID("boosterEmoji.create.changeFile", lang)).withEmoji(EmojiStore.PNG)));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(ActionRow.of(
                Button.success("create", LangID.getStringByID("boosterEmoji.create.create", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return Container.of(children);
    }
}
