package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.slash.SlashOptionMap;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class
Background extends TimedConstraintCommand {
    public static void performButton(ButtonInteractionEvent event, common.util.pack.Background bg) throws Exception {
        Interaction interaction = event.getInteraction();

        CommonStatic.Lang.Locale lang = CommonStatic.Lang.Locale.EN;

        User u = interaction.getUser();

        if(StaticStore.config.containsKey(u.getId())) {
            lang =  StaticStore.config.get(u.getId()).lang;
        }

        File img = ImageDrawing.drawBGImage(bg, 960, 540, false);

        if(img != null) {
            List<ContainerChildComponent> children = new ArrayList<>();

            children.add(TextDisplay.of(LangID.getStringByID("background.result.title", lang)));

            children.add(Separator.create(true, Separator.Spacing.LARGE));

            children.add(TextDisplay.of(
                    LangID.getStringByID("background.result.id", lang).formatted(Data.trio(bg.id.id)) + "\n\n" +
                    LangID.getStringByID("background.result.size", lang).formatted(960, 540)
            ));

            children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(img))));

            Container container = Container.of(children);

            event.deferReply()
                    .setComponents(container)
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .queue(m -> StaticStore.deleteFile(img, true), e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/Background::performButton - Failed to upload bg image");

                        StaticStore.deleteFile(img, true);
                    });
        }
    }

    private common.util.pack.Background bg;

    public Background(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID, false);
    }

    public Background(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, long time, common.util.pack.Background bg) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID, false);

        this.bg = bg;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        if(bg != null) {
            File img = ImageDrawing.drawBGImage(bg, 960, 540, false);

            if(img != null) {
                List<ContainerChildComponent> children = new ArrayList<>();

                children.add(TextDisplay.of(
                        LangID.getStringByID("background.result.title", lang) + "\n" +
                                LangID.getStringByID("background.result.id", lang).formatted(Data.trio(bg.id.id)) + "\n\n" +
                                LangID.getStringByID("background.result.size", lang).formatted(960, 540)
                ));

                children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(img))));

                Container container = Container.of(children);

                replyToMessageSafely(loader.getChannel(), loader.getMessage(), container);
            }
        } else {
            int id;
            int width;
            int height;
            boolean effect;
            boolean animation;

            if (loader.fromMessage) {
                String[] msg = loader.getContent().split(" ");

                if(msg.length == 1) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("background.fail.noParameter", lang));

                    return;
                }

                id = getID(loader.getContent());

                if(id == -1) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("background.fail.noParameter", lang));

                    return;
                }

                width = Math.max(1, getWidth(loader.getContent()));
                height = Math.max(1, getHeight(loader.getContent()));
                effect = drawEffect(loader.getContent());
                animation = generateAnim(loader.getContent());
            } else {
                SlashOptionMap options = loader.getOptions();

                id = options.getOption("id", 0);
                width = options.getOption("width", 960);
                height = options.getOption("height", 540);
                effect = options.getOption("effect", false);
                animation = options.getOption("animation", false);
            }

            common.util.pack.Background bg = UserProfile.getBCData().bgs.getRaw(id);

            if(bg == null) {
                int[] size = getBGSize();

                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("background.fail.invalidID", lang).formatted(size[0], size[1]));

                return;
            }

            User u = loader.getUser();

            boolean isTrusted = StaticStore.contributors.contains(u.getId());

            String cache = StaticStore.imgur.get("BG - "+Data.trio(bg.id.id), false, true);

            if(animation && bg.effect != -1 && cache == null && isTrusted) {
                EntityHandler.generateBGAnim(loader, bg, lang);
            } else {
                if(animation && bg.effect != -1) {
                    if(cache != null) {
                        List<ContainerChildComponent> children = new ArrayList<>();

                        children.add(TextDisplay.of(LangID.getStringByID("background.result.title", lang)));

                        children.add(Separator.create(true, Separator.Spacing.LARGE));

                        children.add(TextDisplay.of(
                                LangID.getStringByID("background.result.id", lang).formatted(Data.trio(bg.id.id)) + "\n\n" +
                                        LangID.getStringByID("data.animation.gif.cached", lang).formatted(cache)
                        ));

                        Container container = Container.of(children);

                        children.add(MediaGallery.of(MediaGalleryItem.fromUrl(cache)));

                        replyToMessageSafely(ch, loader.getMessage(), container);

                        return;
                    } else {
                        ch.sendMessageComponents(TextDisplay.of(LangID.getStringByID("data.animation.background.ignore", lang))).useComponentsV2().queue();
                    }
                }

                if(effect && bg.effect != -1)
                    width = width * 5 / 6;

                File img = ImageDrawing.drawBGImage(bg, width, height, effect);

                if(img != null) {
                    List<ContainerChildComponent> children = new ArrayList<>();

                    children.add(TextDisplay.of(LangID.getStringByID("background.result.title", lang)));

                    children.add(Separator.create(true, Separator.Spacing.LARGE));

                    children.add(TextDisplay.of(
                            LangID.getStringByID("background.result.id", lang).formatted(Data.trio(bg.id.id)) + "\n\n" +
                                    LangID.getStringByID("background.result.size", lang).formatted(width, height)
                    ));

                    children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(img))));

                    Container container = Container.of(children);

                    if (loader.fromMessage) {
                        replyToMessageSafely(loader.getChannel(), loader.getMessage(), container);
                    } else {
                        loader.getInteractionEvent().deferReply()
                                .setComponents(container)
                                .useComponentsV2()
                                .setAllowedMentions(new ArrayList<>())
                                .mentionRepliedUser(false)
                                .queue();
                    }
                }
            }
        }
    }

    private int getWidth(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return 960;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-w") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                return Math.min(1920, StaticStore.safeParseInt(contents[i+1]));
            }
        }

        return 960;
    }

    private int getHeight(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return 540;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-h") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                return Math.min(1080, StaticStore.safeParseInt(contents[i+1]));
            }
        }

        return 540;
    }

    private boolean drawEffect(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return false;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-e") || contents[i].equals("-effect")) {
                return true;
            }
        }

        return false;
    }

    private boolean generateAnim(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return false;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-a") || contents[i].equals("-anim")) {
                return true;
            }
        }

        return false;
    }

    private int getID(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return -1;

        boolean height = false;
        boolean width = false;

        for(int i = 1; i < contents.length; i++) {
            if(contents[i].equals("-w") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                if(!width) {
                    i++;
                    width = true;
                }
            } else if(contents[i].equals("-h") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                if(!height) {
                    i++;
                    height = true;
                }
            } else if(StaticStore.isNumeric(contents[i])) {
                return StaticStore.safeParseInt(contents[i]);
            }
        }

        return -1;
    }

    private int[] getBGSize() {
        List<common.util.pack.Background> bgs = UserProfile.getBCData().bgs.getList();

        int[] res = {0, 0};

        for(int i = 0; i < bgs.size(); i++) {
            if(bgs.get(i).id.id < 1000)
                res[0] = Math.max(res[0], bgs.get(i).id.id);
            else
                res[1] = Math.max(res[1], bgs.get(i).id.id);
        }

        return res;
    }
}
