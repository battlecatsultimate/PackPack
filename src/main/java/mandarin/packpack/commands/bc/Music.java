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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
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
                StaticStore.logger.uploadLog("Can't create file : " + file.getAbsolutePath());

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

            event.deferReply()
                    .setAllowedMentions(new ArrayList<>())
                    .setContent(LangID.getStringByID("music.uploaded", lang).replace("_", Data.trio(ms.id.id)))
                    .addFiles(FileUpload.fromData(file, Data.trio(ms.id.id) + ".ogg"))
                    .queue(m -> {
                        if(file.exists() && !file.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+file.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/Music::performButton - Failed to upload music");

                        if(file.exists() && !file.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+file.getAbsolutePath());
                        }
                    });
        }
    }

    private static final String NOT_NUMBER = "notNumber";
    private static final String OUT_RANGE = "outRange";
    private static final String ARGUMENT = "parameters";

    private common.util.stage.Music ms;

    public Music(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(10), false);
    }

    public Music(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, String mainID, common.util.stage.Music ms) {
        super(role, lang, id, mainID, 0, false);
        this.ms = ms;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    protected void setOptionalID(CommandLoader loader) {
        if(ms == null) {
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
        } else {
            if(ms.id != null) {
                optionalID = Data.trio(ms.id.id);
            }
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
        if(ms != null && ms.id != null) {
            File file = new File("./temp/", Data.trio(ms.id.id)+".ogg");

            if(!file.exists() && !file.createNewFile()) {
                StaticStore.logger.uploadLog("Can't create file : "+file.getAbsolutePath());

                return;
            }

            FileOutputStream fos = new FileOutputStream(file);
            InputStream ins = ms.data.getStream();

            int l;
            byte[] buffer = new byte[65535];

            while((l = ins.read(buffer)) > 0) {
                fos.write(buffer, 0, l);
            }

            ins.close();
            fos.close();

            MessageChannel ch = loader.getChannel();

            sendMessageWithFile(ch, LangID.getStringByID("music.uploaded", lang).replace("_", optionalID), file, optionalID + ".ogg", loader.getMessage());
        } else if(ms == null) {
            int id = StaticStore.safeParseInt(optionalID);

            common.util.stage.Music m = UserProfile.getBCData().musics.get(id);

            File file = new File("./temp/", Data.trio(id)+".ogg");

            if(!file.exists() && !file.createNewFile()) {
                StaticStore.logger.uploadLog("Can't create file : "+file.getAbsolutePath());

                return;
            }

            FileOutputStream fos = new FileOutputStream(file);
            InputStream ins = m.data.getStream();

            int l;
            byte[] buffer = new byte[65535];

            while((l = ins.read(buffer)) > 0) {
                fos.write(buffer, 0, l);
            }

            ins.close();
            fos.close();

            MessageChannel ch = loader.getChannel();

            sendMessageWithFile(ch, LangID.getStringByID("music.uploaded", lang).replace("_", optionalID), file, optionalID + ".ogg", loader.getMessage());
        }
    }

    @Override
    protected void onAbort(CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        switch (optionalID) {
            case NOT_NUMBER -> ch.sendMessage(LangID.getStringByID("music.fail.notNumber", lang)).queue();
            case OUT_RANGE ->
                    ch.sendMessage(LangID.getStringByID("music.fail.outOfRange", lang).replace("_", String.valueOf(UserProfile.getBCData().musics.getList().size() - 1))).queue();
            case ARGUMENT -> ch.sendMessage(LangID.getStringByID("music.fail.noParameter", lang)).queue();
        }
    }
}
