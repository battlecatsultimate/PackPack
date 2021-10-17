package mandarin.packpack.commands;

import common.pack.UserProfile;
import common.util.pack.Background;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {

    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected void doThing(MessageEvent event) throws Exception {
//        MessageChannel ch = getChannel(event);
//
//        if(ch == null)
//            return;
//
//        String[] list = getContent(event).replace("    ", "\t").split(" ", 2);
//
//        if(list.length >= 2) {
//            StageSchedule gacha = new StageSchedule(list[1]);
//
//            ch.createMessage(gacha.beautify()).subscribe();
//        }

        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event).block();

        if(ch == null || g == null)
            return;

        Message msg = createMessage(ch, m -> m.content(LangID.getStringByID("bg_prepare", lang)));

        if(msg == null)
            return;

        Background bg = UserProfile.getBCData().bgs.get(164);

        long start = System.currentTimeMillis();

        File result = ImageDrawing.drawBGAnimEffect(bg, msg, lang);

        long end = System.currentTimeMillis();

        if(result == null) {
            createMessage(ch, m -> m.content(LangID.getStringByID("bg_fail", lang)));
        } else if(result.length() >= (long) getBoosterFileLimit(g.getPremiumTier().getValue()) * 1024 * 1024) {
            createMessage(ch, m -> m.content(LangID.getStringByID("bg_toobig", lang).replace("_SSS_", getFileSize(result))));
        } else {
            FileInputStream fis = new FileInputStream(result);

            createMessage(ch, m -> {
                m.content(LangID.getStringByID("bg_animres", lang).replace("_SSS_", getFileSize(result)).replace("_TTT_", DataToString.df.format((end - start) / 1000.0)));
                m.addFile("result.mp4", fis);
            }, (e) -> {
                try {
                    fis.close();
                } catch (IOException ioException) {
                    StaticStore.logger.uploadErrorLog(e, "Failed to close stream");
                }

                if(result.exists() && !result.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                }
            } ,() -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(result.exists() && !result.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                }
            });
        }
    }

    @Override
    protected void setOptionalID(MessageEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }

    private static int getBoosterFileLimit(int level) {
        switch (level) {
            case 2:
                return 50;
            case 3:
                return 100;
            default:
                return 8;
        }
    }

    private static String getFileSize(File f) {
        String[] unit = {"B", "KB", "MB"};

        double size = f.length();

        for (String s : unit) {
            if (size < 1024) {
                return DataToString.df.format(size) + s;
            } else {
                size /= 1024.0;
            }
        }

        return DataToString.df.format(size)+unit[2];
    }
}