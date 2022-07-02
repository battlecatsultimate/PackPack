package mandarin.packpack.commands.bc;

import common.system.files.VFile;
import common.util.Data;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.Interaction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Castle extends ConstraintCommand {
    public static void performButton(ButtonInteractionEvent event, CastleImg cs) throws Exception {
        Interaction interaction = event.getInteraction();

        int lang = LangID.EN;

        if(interaction.getGuild() != null) {
            String gID = interaction.getGuild().getId();

            if(gID.equals(StaticStore.BCU_KR_SERVER))
                lang = LangID.KR;
        }

        if(interaction.getMember() != null) {
            Member m = interaction.getMember();

            if(StaticStore.config.containsKey(m.getId())) {
                lang =  StaticStore.config.get(m.getId()).lang;
            }
        }

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());

            return;
        }

        File img = new File(temp, StaticStore.findFileName(temp, "castle", ".png"));

        if(!img.exists() && !img.createNewFile()) {
            StaticStore.logger.uploadLog("Can't create new file : "+img.getAbsolutePath());

            return;
        }

        if(cs != null) {
            int code;

            switch (cs.getCont().getSID()) {
                case "000001":
                    code = 1;
                    break;
                case "000002":
                    code = 2;
                    break;
                case "000003":
                    code = 3;
                    break;
                default:
                    code = 0;
            }

            BufferedImage castle;

            if (code == 1 && lang != LangID.JP) {
                VFile vf = VFile.get("./org/img/ec/ec" + Data.trio(cs.id.id) + "_" + getLocale(lang) + ".png");

                if (vf != null) {
                    castle = (BufferedImage) vf.getData().getImg().bimg();
                } else {
                    castle = (BufferedImage) cs.img.getImg().bimg();
                }
            } else {
                castle = (BufferedImage) cs.img.getImg().bimg();
            }

            ImageIO.write(castle, "PNG", img);

            String castleCode;

            if (code == 0)
                castleCode = "RC";
            else if (code == 1)
                castleCode = "EC";
            else if (code == 2)
                castleCode = "WC";
            else
                castleCode = "SC";

            event.deferReply()
                    .allowedMentions(new ArrayList<>())
                    .setContent(LangID.getStringByID("castle_result", lang).replace("_CCC_", castleCode).replace("_III_", Data.trio(cs.id.id)).replace("_BBB_", cs.boss_spawn+""))
                    .addFile(img, "result.png")
                    .queue(m -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/Castle::performButton - Failed to upload castle image");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    });
        }
    }

    private static String getLocale(int lang) {
        switch (lang) {
            case LangID.KR:
                return "ko";
            case LangID.ZH:
                return "tw";
            default:
                return "en";
        }
    }

    private static final int PARAM_RC = 2;
    private static final int PARAM_EC = 4;
    private static final int PARAM_WC = 8;
    private static final int PARAM_SC = 16;

    private CastleImg cs;

    public Castle(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    public Castle(ROLE role, int lang, IDHolder id, CastleImg cs) {
        super(role, lang, id);

        this.cs = cs;
    }

    private int startIndex = 1;

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
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

        if(cs != null) {
            int code;

            switch (cs.getCont().getSID()) {
                case "000001":
                    code = 1;
                    break;
                case "000002":
                    code = 2;
                    break;
                case "000003":
                    code = 3;
                    break;
                default:
                    code = 0;
            }

            BufferedImage castle;

            if(code == 1 && lang != LangID.JP) {
                System.out.println(cs.id.id);
                VFile vf = VFile.get("./org/img/ec/ec"+Data.trio(cs.id.id)+"_"+getLocale(lang)+".png");

                if(vf != null) {
                    castle = (BufferedImage) vf.getData().getImg().bimg();
                } else {
                    castle = (BufferedImage) cs.img.getImg().bimg();
                }
            } else {
                castle = (BufferedImage) cs.img.getImg().bimg();
            }

            ImageIO.write(castle, "PNG", img);

            int finalId = cs.id.id;

            String castleCode;

            if(code == 0)
                castleCode = "RC";
            else if(code == 1)
                castleCode = "EC";
            else if(code == 2)
                castleCode = "WC";
            else
                castleCode = "SC";

            ch.sendMessage(LangID.getStringByID("castle_result", lang).replace("_CCC_", castleCode).replace("_III_", Data.trio(finalId)).replace("_BBB_", cs.boss_spawn+""))
                    .addFile(img, "result.png")
                    .queue(m -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/Castle - Failed to upload castle image");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    });
        } else {
            String[] list = getContent(event).split(" ");

            if(list.length >= 2) {
                int param = checkParameters(getContent(event));

                String[] messages = getContent(event).split(" ", startIndex+1);

                if(messages.length <= startIndex) {
                    ch.sendMessage(LangID.getStringByID("castle_more", lang).replace("_", holder.serverPrefix)).queue();
                    return;
                }

                String msg = messages[startIndex];

                int id;

                if(StaticStore.isNumeric(msg)) {
                    id = StaticStore.safeParseInt(msg);
                } else {
                    ch.sendMessage(LangID.getStringByID("castle_number", lang)).queue();

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

                if(id < 0)
                    id = 0;

                CastleImg image = imgs.get(id);

                BufferedImage castle;

                if(code == 1 && lang != LangID.JP) {
                    VFile vf = VFile.get("./org/img/ec/ec"+Data.trio(image.id.id)+"_"+getLocale(lang)+".png");

                    if(vf != null) {
                        castle = (BufferedImage) vf.getData().getImg().bimg();
                    } else {
                        castle = (BufferedImage) image.img.getImg().bimg();
                    }
                } else {
                    castle = (BufferedImage) image.img.getImg().bimg();
                }

                ImageIO.write(castle, "PNG", img);

                String castleCode;

                if(code == 0)
                    castleCode = "RC";
                else if(code == 1)
                    castleCode = "EC";
                else if(code == 2)
                    castleCode = "WC";
                else
                    castleCode = "SC";

                ch.sendMessage(LangID.getStringByID("castle_result", lang).replace("_CCC_", castleCode).replace("_III_", Data.trio(id)).replace("_BBB_", image.boss_spawn+""))
                        .addFile(img, "result.png")
                        .queue(m -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/Castle - Failed to upload castle image");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });
            } else {
                ch.sendMessage(LangID.getStringByID("castle_argu", lang).replace("_", holder.serverPrefix)).queue();
            }
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
