package mandarin.packpack.commands;

import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.system.files.FDFile;
import common.util.anim.ImgCut;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.ImageGenerator;
import mandarin.packpack.supporter.FontStageImageGenerator;
import mandarin.packpack.supporter.StageImageGenerator;
import mandarin.packpack.supporter.StaticStore;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class StageImage extends ConstraintCommand {
    private static final int PARAM_REAL = 2;
    private static final int PARAM_FORCE = 4;

    public StageImage(ROLE role) {
        super(role);
    }

    private FileInputStream fis;
    private File f;

    private int startIndex = 1;

    @Override
    public void doSomething(MessageCreateEvent event) {
        try {
            MessageChannel ch = getChannel(event);

            String [] list = getMessage(event).split(" ");

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

                ImageGenerator generator;
                String message = getMessage(event).split(" ", startIndex+1)[startIndex];

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
                ch.createMessage("You need one more argument!\nUsage : `"+ StaticStore.serverPrefix+"stageimg [Text]`").subscribe();
            }
        } catch (Exception e) {
            e.printStackTrace();
            onFail(event, DEFAULT_ERROR);
        }
    }

    @Override
    public void onSuccess(MessageCreateEvent event) {
        if(fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(f.exists()) {
            boolean res = f.delete();

            if(!res) {
                System.out.println("Can't delete file : "+f.getAbsolutePath());
            }
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ROOT).replace(" ", "");

            for(int i = 0; i < 2; i++) {
                if(pureMessage.startsWith("-r")) {
                    result |= PARAM_REAL;
                    startIndex++;

                    pureMessage = pureMessage.substring(2);
                } else if(pureMessage.startsWith("-f")) {
                    result |= PARAM_FORCE;
                    startIndex++;

                    pureMessage = pureMessage.substring(2);
                }
            }
        }

        return result;
    }

    private void handleLast(String message, MessageChannel ch, ImageGenerator generator) throws Exception {
        if(f != null) {
            fis = new FileInputStream(f);
            ch.createMessage(m -> {
                m.addFile(f.getName(),fis);
                m.setContent("Result : ");
            }).subscribe();
        } else {
            ArrayList<String> invalid = generator.getInvalids(message);

            if(invalid.isEmpty()) {
                ch.createMessage("Something went wrong while generating image").subscribe();
            } else {
                StringBuilder builder = new StringBuilder("Your message has invalid letters : ");

                for(int i = 0; i < invalid.size(); i++) {
                    if(i == invalid.size() -1) {
                        builder.append(invalid.get(i));
                    } else {
                        builder.append(invalid.get(i)).append(", ");
                    }
                }

                ch.createMessage(builder.toString()).subscribe();
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
