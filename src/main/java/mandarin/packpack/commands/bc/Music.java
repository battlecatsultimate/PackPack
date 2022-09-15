package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.GlobalTimedConstraintCommand;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
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

        int lang = LangID.EN;

        if(interaction.getMember() != null) {
            Member m = interaction.getMember();

            if(StaticStore.config.containsKey(m.getId())) {
                lang =  StaticStore.config.get(m.getId()).lang;
            }
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
                    .setContent(LangID.getStringByID("music_upload", lang).replace("_", Data.trio(ms.id.id)))
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
    private static final String ARGUMENT = "arguments";

    private common.util.stage.Music ms;

    public Music(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(10));
    }

    public Music(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID, common.util.stage.Music ms) {
        super(role, lang, id, mainID, 0);
        this.ms = ms;
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    private final Pauser waiter = new Pauser();

    @Override
    protected void setOptionalID(GenericMessageEvent event) {
        if(ms == null) {
            String[] command = getContent(event).split(" ");

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
    protected void doThing(GenericMessageEvent event) throws Exception {
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

            MessageChannel ch = getChannel(event);

            if(ch != null) {
                ch.sendMessage(LangID.getStringByID("music_upload", lang).replace("_", optionalID))
                        .addFiles(FileUpload.fromData(file, optionalID+".ogg"))
                        .queue(m -> {
                            waiter.resume();

                            if(file.exists() && !file.delete()) {
                                StaticStore.logger.uploadLog("Can't delete file : "+file.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/Music - Failed to upload music");

                            waiter.resume();

                            if(file.exists() && !file.delete()) {
                                StaticStore.logger.uploadLog("Can't delete file : "+file.getAbsolutePath());
                            }
                        });

                waiter.pause(() -> onFail(event, DEFAULT_ERROR));
            }

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

            MessageChannel ch = getChannel(event);

            if(ch != null) {
                ch.sendMessage(LangID.getStringByID("music_upload", lang).replace("_", optionalID))
                        .addFiles(FileUpload.fromData(file, optionalID+".ogg"))
                        .queue(msg -> {
                            waiter.resume();

                            if(file.exists() && !file.delete()) {
                                StaticStore.logger.uploadLog("Can't delete file : "+file.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/Music - Failed to upload music");

                            waiter.resume();

                            if(file.exists() && !file.delete()) {
                                StaticStore.logger.uploadLog("Can't delete file : "+file.getAbsolutePath());
                            }
                        });

                waiter.pause(() -> onFail(event, DEFAULT_ERROR));
            }
        }
    }

    @Override
    protected void onAbort(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch != null) {
            switch (optionalID) {
                case NOT_NUMBER:
                    ch.sendMessage(LangID.getStringByID("music_number", lang)).queue();
                    break;
                case OUT_RANGE:
                    ch.sendMessage(LangID.getStringByID("music_outrange", lang).replace("_", String.valueOf(UserProfile.getBCData().musics.getList().size() - 1))).queue();
                    break;
                case ARGUMENT:
                    ch.sendMessage(LangID.getStringByID("music_argu", lang)).queue();
                    break;
            }
        }
    }
}
