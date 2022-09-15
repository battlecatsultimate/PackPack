package mandarin.packpack.commands.data;

import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.system.files.FDFile;
import common.util.anim.ImgCut;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.FontStageImageGenerator;
import mandarin.packpack.supporter.ImageGenerator;
import mandarin.packpack.supporter.StageImageGenerator;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class StageImage extends ConstraintCommand {
    private static final int PARAM_REAL = 2;
    private static final int PARAM_FORCE = 4;

    public StageImage(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    private File f;

    private int startIndex = 1;

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        try {
            MessageChannel ch = getChannel(event);

            String [] list = getContent(event).split(" ");

            if(list.length >= 2) {
                File temp = new File("./temp");

                if(!temp.exists()) {
                    boolean res = temp.mkdirs();

                    if(!res) {
                        System.out.println("Can't create folder : "+temp.getAbsolutePath());
                        return;
                    }
                }

                int param = checkParameters(getContent(event));

                ImageGenerator generator;
                String[] messages = getContent(event).split(" ", startIndex+1);

                if(messages.length <= startIndex) {
                    ch.sendMessage(LangID.getStringByID("stimg_more", lang).replace("_", holder.serverPrefix)).queue();
                    return;
                }

                String message = messages[startIndex];

                if((param & PARAM_FORCE) > 0 || !canImage(message)) {
                    File fon = new File("./data/ForceFont.otf");
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fon).deriveFont(102f);

                    generator = new FontStageImageGenerator(font, 12f);
                } else {
                    ImgCut ic = ImgCut.newIns(new FDFile(new File("./data/stage/st/Font.imgcut")));
                    FakeImage img = ImageBuilder.builder.build(new File("./data/stage/st/Font.png"));

                    generator = new StageImageGenerator(ic.cut(img));
                }

                if((param & PARAM_REAL) > 0) {
                    f = generator.generateRealImage(message, true);
                } else {
                    f = generator.generateImage(message, true);
                }

                handleLast(message, ch, generator);
            } else {
                ch.sendMessage(LangID.getStringByID("stimg_argu", lang).replace("_", holder.serverPrefix)).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
            onFail(event, DEFAULT_ERROR);
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ROOT).split(" ");

            for(String str : pureMessage) {
                if(str.equals("-r")) {
                    if((result & PARAM_REAL) == 0) {
                        result |= PARAM_REAL;
                        startIndex++;
                    } else
                        break;
                } else if(str.equals("-f")) {
                    if((result & PARAM_FORCE) == 0) {
                        result |= PARAM_FORCE;
                        startIndex++;
                    } else
                        break;
                } else
                    break;
            }
        }

        return result;
    }

    private void handleLast(String message, MessageChannel ch, ImageGenerator generator) {
        if(f != null) {
            ch.sendMessage(LangID.getStringByID("stimg_result", lang))
                    .addFiles(FileUpload.fromData(f, f.getName()))
                    .queue(m -> {
                        if(f.exists() && !f.delete()) {
                            StaticStore.logger.uploadLog("Can't delete file : "+f.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/StageImage::handleLast - Error happened while trying to upload stage image");

                        if(f.exists() && !f.delete()) {
                            StaticStore.logger.uploadLog("Can't delete file : "+f.getAbsolutePath());
                        }
                    });
        } else {
            ArrayList<String> invalid = generator.getInvalids(message);

            if(invalid.isEmpty()) {
                ch.sendMessage(LangID.getStringByID("stimg_wrong", lang)).queue();
            } else {
                StringBuilder builder = new StringBuilder(LangID.getStringByID("stimg_letter", lang));

                for(int i = 0; i < invalid.size(); i++) {
                    if(i == invalid.size() -1) {
                        builder.append(invalid.get(i));
                    } else {
                        builder.append(invalid.get(i)).append(", ");
                    }
                }

                ch.sendMessage(builder.toString()).queue();
            }
        }
    }

    private boolean canImage(String message) {
        for(int i = 0; i < message.length(); i++) {
            String str = Character.toString(message.charAt(i));

            if(str.equals(" "))
                continue;

            if(!StageImageGenerator.contains(str)) {
                return false;
            }
        }

        return true;
    }
}
