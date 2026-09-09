package mandarin.packpack.supporter.server.holder.component.booster;

import common.CommonStatic;
import kotlin.Unit;
import mandarin.packpack.commands.server.BoosterRole;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.BoosterRoleColorHolder;
import mandarin.packpack.supporter.server.holder.modal.BoosterRoleModalHolder;
import mandarin.packpack.supporter.server.holder.modal.BoosterRoleNameModalHolder;
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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.managers.RoleManager;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class BoosterRoleModifyHolder extends ComponentHolder {
    private static final int ROLE_COLOR_IMAGE_WIDTH = 100;
    private static final int ROLE_COLOR_IMAGE_HEIGHT = 50;

    private static final String GUILD_FEATURE_GRADIENT_ROLE_COLOR = "ENHANCED_ROLE_COLORS";
    private static final String GUILD_FEATURE_ROLE_ICON = "ROLE_ICONS";

    private final Member targetMember;
    private final Guild g;

    private final Role role;

    private final BoosterHolder boosterHolder;
    private final BoosterData boosterData;

    @Nullable
    private final String roleIconURL;

    @Nullable
    private File roleIcon;
    private String roleName;

    private int roleColor1;
    private int roleColor2 = -1;

    private int roleColorOld1;
    private int roleColorOld2 = -1;

    @Nullable
    private File roleColorImage;

    public BoosterRoleModifyHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull Member targetMember, @NotNull Guild g, @NotNull Role role, @NotNull BoosterHolder boosterHolder, @NotNull BoosterData boosterData, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.targetMember = targetMember;
        this.g = g;

        this.role = role;

        this.boosterHolder = boosterHolder;
        this.boosterData = boosterData;

        RoleIcon icon = role.getIcon();

        if (icon != null) {
            roleIconURL = icon.getIconUrl();
        } else {
            roleIconURL = null;
        }

        roleName = role.getName();

        roleColor1 = role.getColors().getPrimaryRaw();
        roleColorOld1 = role.getColors().getPrimaryRaw();

        if (role.getColors().isGradient()) {
            roleColor2 = role.getColors().getSecondaryRaw();
            roleColorOld2 = role.getColors().getSecondaryRaw();
        }
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) throws Exception {
        switch (event.getComponentId()) {
            case "name" -> {
                TextInput input = TextInput.create("name", TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("boosterRole.name.field", lang))
                        .setRequiredRange(2, RichCustomEmoji.EMOJI_NAME_MAX_LENGTH)
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("name", LangID.getStringByID("boosterRole.name.title", lang))
                        .addComponents(TextDisplay.of(LangID.getStringByID("boosterRole.name.description", lang)))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.name.label", lang), input))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new BoosterRoleNameModalHolder(getAuthorMessage(), userID, channelID, message, role, lang));
            }
            case "image" -> {
                AttachmentUpload upload = AttachmentUpload.create("roleIcon")
                        .setRequired(true)
                        .setRequiredRange(1, 1)
                        .build();

                Modal modal = Modal.create("role", LangID.getStringByID("boosterRole.upload.title", lang))
                        .addComponents(TextDisplay.of(LangID.getStringByID("boosterRole.upload.description", lang)))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.upload.file", lang), upload))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new BoosterRoleModalHolder(getAuthorMessage(), userID, channelID, message, g, lang));
            }
            case "color1" -> {
                Modal modal = Modal.create("color", LangID.getStringByID("boosterRole.color.title.primary", lang))
                        .addComponents(TextDisplay.of(LangID.getStringByID("boosterRole.color.description", lang)))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.hexCode", lang), TextInput.create("hexCode", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputHexCode", lang)).setMaxLength(6).setRequired(false).build()))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.red", lang), TextInput.create("red", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputRed", lang)).setMaxLength(3).setRequired(false).build()))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.green", lang), TextInput.create("green", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputGreen", lang)).setMaxLength(3).setRequired(false).build()))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.blue", lang), TextInput.create("blue", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputBlue", lang)).setMaxLength(3).setRequired(false).build()))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new BoosterRoleColorHolder(getAuthorMessage(), userID, channelID, message, true, lang));
            }
            case "color2" -> {
                Modal modal = Modal.create("color", LangID.getStringByID("boosterRole.color.title.gradient", lang))
                        .addComponents(TextDisplay.of(LangID.getStringByID("boosterRole.color.description", lang)))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.hexCode", lang), TextInput.create("hexCode", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputHexCode", lang)).setMaxLength(6).setRequired(false).build()))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.red", lang), TextInput.create("red", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputRed", lang)).setMaxLength(3).setRequired(false).build()))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.green", lang), TextInput.create("green", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputGreen", lang)).setMaxLength(3).setRequired(false).build()))
                        .addComponents(Label.of(LangID.getStringByID("boosterRole.color.blue", lang), TextInput.create("blue", TextInputStyle.SHORT).setPlaceholder(LangID.getStringByID("boosterRole.color.inputBlue", lang)).setMaxLength(3).setRequired(false).build()))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new BoosterRoleColorHolder(getAuthorMessage(), userID, channelID, message, false, lang));
            }
            case "removeGradient" -> {
                roleColor2 = -1;

                applyResult(event);
            }
            case "removeImage" -> {
                if (roleIcon != null) {
                    StaticStore.deleteFile(roleIcon, true);

                    roleIcon = null;
                }

                applyResult(event);
            }
            case "change" -> {
                RoleManager action = role.getManager();

                if (roleColor1 != roleColor2 && roleColor2 != -1) {
                    action = action.setGradientColors(roleColor1, roleColor2);
                } else {
                    action = action.setColor(roleColor1);
                }

                if (roleIcon != null) {
                    action = action.setIcon(Icon.from(roleIcon));
                }

                action.setName(roleName)
                        .queue(_ -> {
                            event.deferReply()
                                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.modify.success", lang).formatted(targetMember.getIdLong(), role.getIdLong(), role.getIdLong())))
                                    .useComponentsV2()
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .setEphemeral(true)
                                    .queue();

                            if (roleIcon != null) {
                                StaticStore.deleteFile(roleIcon, true);
                            }

                            if (roleColorImage != null) {
                                StaticStore.deleteFile(roleColorImage, true);
                            }

                            goBack();
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/BoosterRoleCreateHolder::onEvent - Failed to modify role");

                            event.deferReply()
                                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.failedModify", lang)))
                                    .useComponentsV2()
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .setEphemeral(true)
                                    .queue();

                            if (roleIcon != null) {
                                StaticStore.deleteFile(roleIcon, true);
                            }

                            if (roleColorImage != null) {
                                StaticStore.deleteFile(roleColorImage, true);
                            }

                            goBack();
                        });
            }
            case "delete" -> {
                registerPopUp(event, LangID.getStringByID("boosterRole.modify.delete", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    role.delete().queue();
                    boosterData.removeRole();

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
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("boosterRole.modify.cancel", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    if (roleIcon != null)
                        StaticStore.deleteFile(roleIcon, true);

                    if (roleColorImage != null)
                        StaticStore.deleteFile(roleColorImage, true);

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
        if (roleColorImage != null)
            StaticStore.deleteFile(roleColorImage, true);

        if (roleIcon != null)
            StaticStore.deleteFile(roleIcon, true);
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child, Object... additional) throws Exception {
        if (additional.length != 0) {
            if (additional[0] instanceof File iconFile) {
                roleIcon = iconFile;
            } else if (additional[0] instanceof String textData) {
                String[] data = textData.split("\n");

                if (data.length != 2) {
                    StaticStore.logger.uploadLog("W/BoosterRoleCreateHolder::onBack - Failed to get proper string data : " + textData);
                } else {
                    switch (data[0]) {
                        case BoosterRole.SEGMENT_NAME -> roleName = data[1].trim();
                        case BoosterRole.SEGMENT_COLOR1 -> roleColor1 = Math.clamp(StaticStore.safeParseInt(data[1]), 0, 0xFFFFFF);
                        case BoosterRole.SEGMENT_COLOR2 -> roleColor2 = Math.clamp(StaticStore.safeParseInt(data[1]), 0, 0xFFFFFF);
                    }
                }
            }
        }

        applyResult(event);
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) throws Exception {
        if (roleColorImage == null || roleColor1 != roleColorOld1 || roleColor2 != roleColorOld2) {
            if (roleColorImage != null) {
                StaticStore.deleteFile(roleColorImage, true);
            }

            roleColorImage = renderRoleColorImage(roleColor1, roleColor2);

            roleColorOld1 = roleColor1;
            roleColorOld2 = roleColor2;
        }

        event.deferEdit()
                .setComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private Container getComponents() {
        List<ContainerChildComponent> children = new ArrayList<>();

        children.add(TextDisplay.of("## " + LangID.getStringByID("boosterRole.modify.title", lang)));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        if (g.getFeatures().contains(GUILD_FEATURE_ROLE_ICON)) {
            if (roleIcon == null) {
                if (roleIconURL != null) {
                    children.add(MediaGallery.of(MediaGalleryItem.fromUrl(roleIconURL)));

                    children.add(Separator.create(false, Separator.Spacing.SMALL));
                }
            } else {
                children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(roleIcon, "roleIcon.png"))));

                children.add(Separator.create(false, Separator.Spacing.SMALL));
            }
        }

        children.add(TextDisplay.of(LangID.getStringByID("boosterRole.modify.member", lang).formatted(targetMember.getIdLong(), targetMember.getIdLong())));
        children.add(TextDisplay.of(LangID.getStringByID("boosterRole.modify.name", lang).formatted(roleName)));

        Color c1 = new Color(roleColor1);

        if (g.getFeatures().contains(GUILD_FEATURE_GRADIENT_ROLE_COLOR) && roleColor2 != -1) {
            Color c2 = new Color(roleColor2);

            children.add(TextDisplay.of(LangID.getStringByID("boosterRole.modify.color.first", lang).formatted(String.format("%06X", roleColor1), c1.getRed(), c1.getGreen(), c1.getBlue())));
            children.add(TextDisplay.of(LangID.getStringByID("boosterRole.modify.color.second", lang).formatted(String.format("%06X", roleColor2), c2.getRed(), c2.getGreen(), c2.getBlue())));
        } else {
            children.add(TextDisplay.of(LangID.getStringByID("boosterRole.modify.color.first", lang).formatted(String.format("%06X", roleColor1), c1.getRed(), c1.getGreen(), c1.getBlue())));
        }

        if (roleColorImage != null) {
            children.add(TextDisplay.of(LangID.getStringByID("boosterRole.modify.colorPreview", lang)));

            children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(roleColorImage, "roleColor.png"))));
        }

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(ActionRow.of(net.dv8tion.jda.api.components.buttons.Button.secondary("name", LangID.getStringByID("boosterRole.modify.changeName", lang)).withEmoji(Emoji.fromUnicode("🏷️"))));

        if (g.getFeatures().contains(GUILD_FEATURE_ROLE_ICON)) {
            children.add(ActionRow.of(
                    net.dv8tion.jda.api.components.buttons.Button.secondary("image", LangID.getStringByID("boosterRole.modify.changeFile", lang)).withEmoji(EmojiStore.PNG),
                    net.dv8tion.jda.api.components.buttons.Button.secondary("removeImage", LangID.getStringByID("boosterRole.modify.removeFile", lang)).withEmoji(Emoji.fromUnicode("🗑️")).withDisabled(roleIcon == null)
            ));
        }

        if (g.getFeatures().contains(GUILD_FEATURE_GRADIENT_ROLE_COLOR)) {
            children.add(ActionRow.of(
                    net.dv8tion.jda.api.components.buttons.Button.secondary("color1", LangID.getStringByID("boosterRole.modify.changeColor.first", lang)),
                    net.dv8tion.jda.api.components.buttons.Button.secondary("color2", LangID.getStringByID("boosterRole.modify.changeColor.second", lang)),
                    net.dv8tion.jda.api.components.buttons.Button.secondary("removeGradient", LangID.getStringByID("boosterRole.modify.changeColor.removeGradient", lang)).withDisabled(roleColor2 == -1)
            ));
        } else {
            children.add(ActionRow.of(net.dv8tion.jda.api.components.buttons.Button.secondary("color1", LangID.getStringByID("boosterRole.modify.changeColor.single", lang))));
        }

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.primary("change", LangID.getStringByID("boosterRole.modify.change", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("delete", LangID.getStringByID("ui.button.delete", lang)).withEmoji(Emoji.fromUnicode("🗑️"))
        ));

        return Container.of(children).withAccentColor(c1);
    }

    @Nullable
    private File renderRoleColorImage(int color1, int color2) throws Exception {
        File temp = new File("./temp");

        if (!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/BoosterRoleCreateHolder::renderRoleColorImage - Failed to create temp folder");

            return null;
        }

        File imageFile = StaticStore.generateTempFile(temp, "roleColorImage", "png", false);

        if (imageFile == null) {
            StaticStore.logger.uploadLog("W/BoosterRoleCreateHolder::renderRoleColorImage - Failed to create image file");

            return null;
        }

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(ROLE_COLOR_IMAGE_WIDTH, ROLE_COLOR_IMAGE_HEIGHT, temp, connector -> {
            connector.queue(g -> {
                Color c1 = new Color(color1);

                if (color1 != color2 && color2 != -1) {
                    Color c2 = new Color(color2);

                    g.gradRoundRect(0f, 0f, ROLE_COLOR_IMAGE_WIDTH, ROLE_COLOR_IMAGE_HEIGHT, 0f, 0f, new int[] { c1.getRed(), c1.getGreen(), c1.getBlue() }, ROLE_COLOR_IMAGE_WIDTH, 0f, new int[] { c2.getRed(), c2.getGreen(), c2.getBlue() }, 10f, 10f);
                } else {
                    g.setColor(c1.getRed(), c1.getGreen(), c1.getBlue());

                    g.fillRoundRect(0f, 0f, ROLE_COLOR_IMAGE_WIDTH, ROLE_COLOR_IMAGE_HEIGHT, 10f, 10f);
                }

                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        }, _ -> imageFile, () -> {
            waiter.countDown();

            return Unit.INSTANCE;
        });

        waiter.await();
        return imageFile;
    }
}
