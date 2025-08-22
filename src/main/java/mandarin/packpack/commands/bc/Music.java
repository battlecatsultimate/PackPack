package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.GlobalTimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Music extends GlobalTimedConstraintCommand {
    public static void performButton(ButtonInteractionEvent event, common.util.stage.Music ms) throws Exception {
        Interaction interaction = event.getInteraction();

        CommonStatic.Lang.Locale lang = CommonStatic.Lang.Locale.EN;

        User u = interaction.getUser();

        if(StaticStore.config.containsKey(u.getId())) {
            lang =  StaticStore.config.get(u.getId()).lang;
        }

        if(ms != null && ms.id != null) {
            File file = new File("./temp/", Data.trio(ms.id.id) + ".ogg");

            if (!file.exists() && !file.createNewFile()) {
                StaticStore.logger.uploadLog("W/Music::performButton - Can't create file : " + file.getAbsolutePath());

                return;
            }

            FileOutputStream fos = new FileOutputStream(file);
            InputStream ins = ms.data.getStream();

            int l;
            byte[] buffer = new byte[65535];

            while ((l = ins.read(buffer)) > 0) {
                fos.write(buffer, 0, l);
            }

            ins.close();
            fos.close();

            List<ContainerChildComponent> children = new ArrayList<>();

            children.add(TextDisplay.of("## " + LangID.getStringByID("music.title", lang).formatted(Data.trio(ms.id.id))));
            children.add(TextDisplay.of(LangID.getStringByID("music.uploaded", lang).formatted(Data.trio(ms.id.id))));

            children.add(Separator.create(true, Separator.Spacing.LARGE));

            children.add(FileDisplay.fromFile(FileUpload.fromData(file, Data.trio(ms.id.id) + ".ogg")));

            Container container = Container.of(children);

            event.deferReply()
                    .setComponents(container)
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .queue(unused -> StaticStore.deleteFile(file, true), e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/Music::performButton - Failed to upload music");

                        StaticStore.deleteFile(file, true);
                    });
        }
    }

    private static final String NOT_NUMBER = "notNumber";
    private static final String OUT_RANGE = "outRange";
    private static final String ARGUMENT = "parameters";

    public Music(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(10), false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    protected void setOptionalID(CommandLoader loader) {
        String[] command = loader.getContent().split(" ");

        if(command.length == 2) {
            if (StaticStore.isNumeric(command[1])) {
                int id = StaticStore.safeParseInt(command[1]);

                if(id < 0 || id >= UserProfile.getBCData().musics.getList().size()) {
                    optionalID = OUT_RANGE;
                    return;
                }

                optionalID = Data.trio(StaticStore.safeParseInt(command[1]));
            } else {
                optionalID = NOT_NUMBER;
            }
        } else {
            optionalID = ARGUMENT;
        }
    }

    @Override
    protected void prepareAborts() {
        aborts.add(OUT_RANGE);
        aborts.add(NOT_NUMBER);
        aborts.add(ARGUMENT);
    }

    @Override
    protected void doThing(CommandLoader loader) throws Exception {
        int id = StaticStore.safeParseInt(optionalID);

        common.util.stage.Music music = UserProfile.getBCData().musics.get(id);

        File file = new File("./temp/", Data.trio(music.id.id)+".ogg");

        if(!file.exists() && !file.createNewFile()) {
            StaticStore.logger.uploadLog("Can't create file : "+file.getAbsolutePath());

            return;
        }

        FileOutputStream fos = new FileOutputStream(file);
        InputStream ins = music.data.getStream();

        int l;
        byte[] buffer = new byte[65535];

        while((l = ins.read(buffer)) > 0) {
            fos.write(buffer, 0, l);
        }

        ins.close();
        fos.close();

        List<ContainerChildComponent> children = new ArrayList<>();

        children.add(TextDisplay.of("## " + LangID.getStringByID("music.title", lang).formatted(Data.trio(music.id.id))));
        children.add(TextDisplay.of(LangID.getStringByID("music.uploaded", lang).formatted(Data.trio(music.id.id))));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(FileDisplay.fromFile(FileUpload.fromData(file, Data.trio(music.id.id) + ".ogg")));

        Container container = Container.of(children);

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), unused -> StaticStore.deleteFile(file, true), e -> {
            StaticStore.logger.uploadErrorLog(e, "E/Music::performButton - Failed to upload music");

            StaticStore.deleteFile(file, true);
        }, container);
    }

    @Override
    protected void onAbort(CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Message msg = loader.getMessage();

        switch (optionalID) {
            case NOT_NUMBER -> replyToMessageSafely(ch, msg, TextDisplay.of(LangID.getStringByID("music.fail.notNumber", lang)));
            case OUT_RANGE -> replyToMessageSafely(ch, msg, TextDisplay.of(LangID.getStringByID("music.fail.outOfRange", lang).formatted(UserProfile.getBCData().musics.getList().size() - 1)));
            case ARGUMENT -> replyToMessageSafely(ch, msg, TextDisplay.of(LangID.getStringByID("music.fail.noParameter", lang)));
        }
    }
}
