package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.Data;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Castle extends ConstraintCommand {
    private static final int PARAM_RC = 2;
    private static final int PARAM_EC = 4;
    private static final int PARAM_WC = 8;
    private static final int PARAM_SC = 16;

    public Castle(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    private int startIndex = 1;

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getMessage(event).split(" ");

        if(list.length >= 2) {
            File temp = new File("./temp");

            if(!temp.exists()) {
                boolean res = temp.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder : "+temp.getAbsolutePath());
                    return;
                }
            }

            int param = checkParameters(getMessage(event));

            String[] messages = getMessage(event).split(" ", startIndex+1);

            if(messages.length <= startIndex) {
                ch.createMessage(LangID.getStringByID("castle_more", lang).replace("_", StaticStore.serverPrefix)).subscribe();
                return;
            }

            String msg = messages[startIndex];

            int id;

            if(StaticStore.isNumeric(msg)) {
                id = StaticStore.safeParseInt(msg);
            } else {
                ch.createMessage(LangID.getStringByID("castle_number", lang)).subscribe();
                return;
            }

            File img = new File(temp, StaticStore.findFileName(temp, "castle", ".png"));

            if(!img.exists()) {
                boolean res = img.createNewFile();

                if(!res) {
                    System.out.println("Can't create new file : "+img.getAbsolutePath());
                    return;
                }
            } else {
                return;
            }

            ArrayList<CastleList> castleLists = new ArrayList<>(CastleList.defset());

            List<CastleImg> imgs;
            int code;

            if((param & PARAM_EC) > 0) {
                imgs = castleLists.get(1).getList();
                code = 1;
            } else if((param & PARAM_WC) > 0) {
                imgs = castleLists.get(2).getList();
                code = 2;
            } else if((param & PARAM_SC) > 0) {
                imgs = castleLists.get(3).getList();
                code = 3;
            } else {
                imgs = castleLists.get(0).getList();
                code = 0;
            }

            if(id >= imgs.size())
                id = imgs.size() - 1;

            CastleImg image = imgs.get(id);

            BufferedImage castle = (BufferedImage) image.img.getImg().bimg();

            ImageIO.write(castle, "PNG", img);

            FileInputStream fis = new FileInputStream(img);

            int finalId = id;

            ch.createMessage(m -> {
                String castleCode;

                if(code == 0)
                    castleCode = "RC";
                else if(code == 1)
                    castleCode = "EC";
                else if(code == 2)
                    castleCode = "WC";
                else
                    castleCode = "SC";

                m.setContent(LangID.getStringByID("castle_result", lang).replace("_", castleCode).replace("|", Data.trio(finalId)));
                m.addFile("Result.png", fis);
            }).subscribe(null, null, () -> {
                try {
                    fis.close();

                    StaticStore.deleteFile(img, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            ch.createMessage(LangID.getStringByID("castle_argu", lang).replace("_", StaticStore.serverPrefix)).subscribe();
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.startsWith("-rc")) {
                    if((result & PARAM_RC) == 0) {
                        result |= PARAM_RC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-ec")) {
                    if((result & PARAM_EC) == 0) {
                        result |= PARAM_EC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-wc")) {
                    if((result & PARAM_WC) == 0) {
                        result |= PARAM_WC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-sc")) {
                    if((result & PARAM_SC) == 0) {
                        result |= PARAM_SC;
                        startIndex++;
                    }

                    break;
                } else {
                    break;
                }
            }
        }

        return result;
    }
}
