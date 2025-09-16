package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.pack.Soul;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SoulImage extends TimedConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    public SoulImage(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, "soulimage", false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        int id = findSoulID(loader.getContent());

        if(id == -1) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("soul.failed.noParameter", lang));

            return;
        }

        int soulLen = UserProfile.getBCData().souls.size();

        if(id >= soulLen) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("soul.failed.outOfRange", lang).formatted(soulLen - 1));

            disableTimer();

            return;
        }

        Soul s = UserProfile.getBCData().souls.get(id);

        if(s == null) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("soul.failed.noSoul", lang));

            disableTimer();

            return;
        }

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/SoulImage::doSomething - Failed to create folder : " + temp.getAbsolutePath());

            disableTimer();

            return;
        }

        int param = checkParameters(loader.getContent());
        int frame = getFrame(loader.getContent());

        s.anim.load();

        EAnimD<?> anim = s.anim.getEAnim(AnimU.UType.SOUL);

        File img = ImageDrawing.drawAnimImage(anim, frame, 1f, (param & PARAM_TRANSPARENT) > 0, (param & PARAM_DEBUG) > 0);

        s.anim.unload();

        if(img != null) {
            List<ContainerChildComponent> children = new ArrayList<>();

            String title = LangID.getStringByID("data.soul", lang) + " [" + Data.trio(s.getID().id) + "]";

            children.add(TextDisplay.of("## " + title));
            children.add(TextDisplay.of(LangID.getStringByID("soulImage.frame", lang).formatted(frame)));

            children.add(Separator.create(true, Separator.Spacing.LARGE));

            children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(img))));

            replyToMessageSafely(ch, loader.getMessage(), unused -> StaticStore.deleteFile(img, true), e -> {
                StaticStore.logger.uploadErrorLog(e, "E/SoulImage::doSomething - Failed to upload soul image");

                StaticStore.deleteFile(img, true);
            }, Container.of(children));
        } else {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("soulImage.failed.unknown", lang));

            disableTimer();
        }
    }

    private int getFrame(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-f") || msg[i].equals("-fr")) {
                if(i < msg.length - 1 && StaticStore.isNumeric(msg[i+1])) {
                    return StaticStore.safeParseInt(msg[i+1]);
                }
            }
        }

        return 0;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for(int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-t" -> {
                        if ((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
                        } else {
                            break label;
                        }
                    }
                    case "-d", "-debug" -> {
                        if ((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                    }
                    case "-f", "-fr" -> {
                        if (i < pureMessage.length - 1 && StaticStore.isNumeric(pureMessage[i + 1])) {
                            i++;
                        } else {
                            break label;
                        }
                    }
                }
            }
        }

        return result;
    }

    private int findSoulID(String content) {
        String[] contents = content.split(" ");

        boolean frame = false;

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-f") || contents[i].equals("-fr")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]) && !frame) {
                frame = true;

                i++;
            } else if(StaticStore.isNumeric(contents[i]))
                return StaticStore.safeParseInt(contents[i]);
        }

        return -1;
    }
}
