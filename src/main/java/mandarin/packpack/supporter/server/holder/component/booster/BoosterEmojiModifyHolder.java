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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BoosterEmojiModifyHolder extends ComponentHolder {
    private final Member targetMember;
    private final Guild g;

    private final BoosterHolder boosterHolder;
    private final BoosterData boosterData;

    private final RichCustomEmoji targetEmoji;

    private File emojiFile;
    private String emojiName;

    public BoosterEmojiModifyHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull Member targetMember, @NotNull Guild g, @NotNull BoosterHolder boosterHolder, @NotNull BoosterData boosterData, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.targetMember = targetMember;
        this.g = g;

        this.boosterHolder = boosterHolder;
        this.boosterData = boosterData;

        targetEmoji = g.getEmojiById(boosterData.getEmoji());
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

                connectTo(new BoosterEmojiNameModalHolder(getAuthorMessage(), userID, channelID, message, targetEmoji, lang));
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
            case "change" -> {
                if (emojiFile != null) {
                    targetEmoji.delete().queue(_ -> {
                        try {
                            g.createEmoji(emojiName == null ? targetEmoji.getName() : emojiName, Icon.from(emojiFile)).queue(emoji -> {
                                if (boosterData.getRole() != -1L) {
                                    boosterData.setEmoji(emoji.getIdLong());
                                } else {
                                    boosterHolder.serverBooster.put(targetMember.getIdLong(), new BoosterData(emoji.getIdLong(), BoosterData.INITIAL.EMOJI));
                                }

                                event.deferReply()
                                        .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.modify.changed", lang).formatted(targetMember.getIdLong(), emoji.getFormatted(), emoji.getName(), emoji.getIdLong())))
                                        .useComponentsV2()
                                        .setAllowedMentions(new ArrayList<>())
                                        .mentionRepliedUser(false)
                                        .setEphemeral(true)
                                        .queue();

                                goBack();
                            }, e -> {
                                StaticStore.logger.uploadErrorLog(e, "E/BoosterEmojiModifyHolder::onEvent - Failed to create emoji");

                                event.deferReply()
                                        .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.failedReplace", lang)))
                                        .useComponentsV2()
                                        .setAllowedMentions(new ArrayList<>())
                                        .mentionRepliedUser(false)
                                        .setEphemeral(true)
                                        .queue();
                            });
                        } catch (IOException e) {
                            StaticStore.logger.uploadErrorLog(e, "E/BoosterEmojiModifyHolder::onEvent - Failed to get emoji file");

                            event.deferReply()
                                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.failedEmojiFile", lang)))
                                    .useComponentsV2()
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .setEphemeral(true)
                                    .queue();
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/BoosterEmojiModifyHolder::onEvent - Failed to delete emoji");

                        event.deferReply()
                                .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.failedReplace", lang)))
                                .useComponentsV2()
                                .setAllowedMentions(new ArrayList<>())
                                .mentionRepliedUser(false)
                                .setEphemeral(true)
                                .queue();
                    });
                } else {
                    targetEmoji.getManager().setName(emojiName).queue(_ -> {
                        event.deferReply()
                                .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.modify.changed", lang).formatted(targetMember.getIdLong(), targetEmoji.getFormatted(), emojiName, targetEmoji.getIdLong())))
                                .useComponentsV2()
                                .setAllowedMentions(new ArrayList<>())
                                .mentionRepliedUser(false)
                                .setEphemeral(true)
                                .queue();

                        goBack();
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/BoosterEmojiModifyHolder::onEvent - Failed to change name of the emoji");

                        event.deferReply()
                                .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.failedModifyName", lang)))
                                .useComponentsV2()
                                .setAllowedMentions(new ArrayList<>())
                                .mentionRepliedUser(false)
                                .setEphemeral(true)
                                .queue();
                    });
                }
            }
            case "back" -> goBack(event);
            case "delete" -> {
                registerPopUp(event, LangID.getStringByID("boosterEmoji.modify.delete", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    targetEmoji.delete().queue();
                    boosterData.removeEmoji();

                    if (boosterData.getEmoji() == -1L && boosterData.getRole() == -1L)
                        boosterHolder.serverBooster.remove(targetMember.getIdLong());

                    e.deferReply()
                            .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.modify.deleteSuccess", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .setEphemeral(true)
                            .queue();

                    goBack();
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

        children.add(TextDisplay.of("## " + LangID.getStringByID("boosterEmoji.modify.title", lang)));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(TextDisplay.of(LangID.getStringByID("boosterEmoji.modify.emoji", lang).formatted(targetEmoji.getFormatted(), targetEmoji.getName(), targetEmoji.getIdLong())));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        if (emojiFile != null) {
            children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(emojiFile, "emoji." + FilenameUtils.getExtension(emojiFile.getName())))));

            children.add(Separator.create(false, Separator.Spacing.SMALL));
        }

        children.add(TextDisplay.of(LangID.getStringByID("boosterEmoji.modify.member", lang).formatted(targetMember.getIdLong(), targetMember.getIdLong())));

        String actualEmojiName = emojiName == null ? targetEmoji.getName() : emojiName;

        children.add(TextDisplay.of(LangID.getStringByID("boosterEmoji.modify.name", lang).formatted(actualEmojiName)));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(ActionRow.of(Button.secondary("name", LangID.getStringByID("boosterEmoji.modify.changeName", lang)).withEmoji(Emoji.fromUnicode("🏷️"))));
        children.add(ActionRow.of(Button.secondary("image", LangID.getStringByID("boosterEmoji.modify.changeFile", lang)).withEmoji(EmojiStore.PNG)));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.primary("change", LangID.getStringByID("boosterEmoji.modify.change", lang)).withEmoji(EmojiStore.CHECK).withDisabled(emojiName == null && emojiFile == null),
                Button.danger("delete", LangID.getStringByID("ui.button.delete", lang)).withEmoji(Emoji.fromUnicode("🗑️"))
        ));

        return Container.of(children);
    }
}
