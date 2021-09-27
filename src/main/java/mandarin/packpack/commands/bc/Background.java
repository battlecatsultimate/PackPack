package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.Data;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Background extends TimedConstraintCommand {
    private common.util.pack.Background bg;

    public Background(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID);
    }

    public Background(ConstraintCommand.ROLE role, int lang, IDHolder id, long time, common.util.pack.Background bg) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID);

        this.bg = bg;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        if(bg != null) {
            File img = ImageDrawing.drawBGImage(bg, 960, 520);

            if(img != null) {
                FileInputStream fis = new FileInputStream(img);

                createMessage(ch, m -> {
                    m.content(LangID.getStringByID("bg_result", lang).replace("_", Data.trio(bg.id.id)).replace("WWW", 960+"").replace("HHH", 520+""));
                    m.addFile("bg.png", fis);
                }, () -> {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Can't delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            }
        } else {
            String[] msg = getContent(event).split(" ");

            if(msg.length == 1) {
                ch.createMessage(LangID.getStringByID("bg_more", lang)).subscribe();
            } else {
                int id = getID(getContent(event));

                if(id == -1) {
                    ch.createMessage(LangID.getStringByID("bg_more", lang)).subscribe();
                    return;
                } else if(id < 0 || id >= UserProfile.getBCData().bgs.getList().size()) {
                    ch.createMessage(LangID.getStringByID("bg_invalid", lang).replace("_", (UserProfile.getBCData().bgs.getList().size()-1)+"")).subscribe();
                    return;
                }

                int w = Math.max(1, getWidth(getContent(event)));
                int h = Math.max(1, getHeight(getContent(event)));

                common.util.pack.Background bg = UserProfile.getBCData().bgs.getList().get(id);

                File img = ImageDrawing.drawBGImage(bg, w, h);

                if(img != null) {
                    FileInputStream fis = new FileInputStream(img);

                    createMessage(ch, m -> {
                        m.content(LangID.getStringByID("bg_result", lang).replace("_", Data.trio(id)).replace("WWW", w+"").replace("HHH", h+""));
                        m.addFile("bg.png", fis);
                    }, () -> {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if(img.exists()) {
                            boolean res = img.delete();

                            if(!res) {
                                System.out.println("Can't delete file : "+img.getAbsolutePath());
                            }
                        }
                    });
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
            return 520;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-h") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                return Math.min(1080, StaticStore.safeParseInt(contents[i+1]));
            }
        }

        return 520;
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
}
