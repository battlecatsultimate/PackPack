package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.Data;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.InteractionData;
import discord4j.discordjson.json.MemberData;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.GlobalTimedConstraintCommand;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.slash.SlashBuilder;
import mandarin.packpack.supporter.server.slash.WebhookBuilder;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class Music extends GlobalTimedConstraintCommand {
    public static WebhookBuilder getInteractionWebhook(InteractionData interaction, common.util.stage.Music ms) throws Exception {
        int lang = LangID.EN;

        if(!interaction.guildId().isAbsent()) {
            String gID = interaction.guildId().get();

            if(gID.equals(StaticStore.BCU_KR_SERVER))
                lang = LangID.KR;
        }

        if(!interaction.member().isAbsent()) {
            MemberData m = interaction.member().get();

            if(StaticStore.locales.containsKey(m.user().id().asString())) {
                lang =  StaticStore.locales.get(m.user().id().asString());
            }
        }

        if(ms != null && ms.id != null) {
            File file = new File("./temp/", Data.trio(ms.id.id) + ".ogg");

            if (!file.exists()) {
                boolean res = file.createNewFile();

                if (!res) {
                    System.out.println("Can't create file : " + file.getAbsolutePath());
                    return null;
                }
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

            FileInputStream fis = new FileInputStream(file);

            int finalLang = lang;

            return SlashBuilder.getWebhookRequest(w -> {
                w.setContent(LangID.getStringByID("music_upload", finalLang).replace("_", Data.trio(ms.id.id)));
                w.addFile(Data.trio(ms.id.id) + ".ogg", fis, file);
            });
        }

        return null;
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

    private final Pauser waiter = new Pauser();

    @Override
    protected void setOptionalID(MessageEvent event) {
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
    protected void doThing(MessageEvent event) throws Exception {
        if(ms != null && ms.id != null) {
            File file = new File("./temp/", Data.trio(ms.id.id)+".ogg");

            if(!file.exists()) {
                boolean res = file.createNewFile();

                if(!res) {
                    System.out.println("Can't create file : "+file.getAbsolutePath());
                    return;
                }
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
                FileInputStream fis = new FileInputStream(file);

                createMessage(ch, msg -> {
                    msg.content(LangID.getStringByID("music_upload", lang).replace("_", optionalID));
                    msg.addFile(optionalID+".ogg", fis);
                }, () -> {
                    waiter.resume();

                    try {
                        if(file.exists()) {
                            fis.close();

                            boolean res = file.delete();

                            if(!res) {
                                System.out.println("Can't delete file : "+file.getAbsolutePath());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                waiter.pause(() -> onFail(event, DEFAULT_ERROR));
            }

        } else if(ms == null) {
            int id = StaticStore.safeParseInt(optionalID);

            common.util.stage.Music m = UserProfile.getBCData().musics.get(id);

            File file = new File("./temp/", Data.trio(id)+".ogg");

            if(!file.exists()) {
                boolean res = file.createNewFile();

                if(!res) {
                    System.out.println("Can't create file : "+file.getAbsolutePath());
                    return;
                }
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
                FileInputStream fis = new FileInputStream(file);

                createMessage(ch, msg -> {
                    msg.content(LangID.getStringByID("music_upload", lang).replace("_", optionalID));
                    msg.addFile(optionalID+".ogg", fis);
                }, () -> {
                    waiter.resume();

                    try {
                        if(file.exists()) {
                            fis.close();

                            boolean res = file.delete();

                            if(!res) {
                                System.out.println("Can't delete file : "+file.getAbsolutePath());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                waiter.pause(() -> onFail(event, DEFAULT_ERROR));
            }
        }
    }

    @Override
    protected void onAbort(MessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch != null) {
            switch (optionalID) {
                case NOT_NUMBER:
                    ch.createMessage(LangID.getStringByID("music_number", lang)).subscribe();
                    break;
                case OUT_RANGE:
                    ch.createMessage(LangID.getStringByID("music_outrange", lang).replace("_", String.valueOf(UserProfile.getBCData().musics.getList().size() - 1))).subscribe();
                    break;
                case ARGUMENT:
                    ch.createMessage(LangID.getStringByID("music_argu", lang));
                    break;
            }
        }
    }
}
