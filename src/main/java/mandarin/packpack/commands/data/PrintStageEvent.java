package mandarin.packpack.commands.data;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateFields;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PrintStageEvent extends ConstraintCommand {
    public PrintStageEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String result = StaticStore.event.printStageEvent(lang);

        if(result.length() >= 2000) {
            File temp = new File("./temp");

            if(!temp.exists() && !temp.mkdirs()) {
                StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());
                return;
            }

            File res = new File(temp, StaticStore.findFileName(temp, "saleEvent", ".txt"));

            if(!res.exists() && !res.createNewFile()) {
                StaticStore.logger.uploadLog("Failed to create file : "+res.getAbsolutePath());
                return;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

            writer.write(result);

            writer.close();

            FileInputStream fis = new FileInputStream(res);

            createMessage(ch, m -> {
                m.content(LangID.getStringByID("printstage_toolong", lang));
                m.addFile(MessageCreateFields.File.of("saleEvent.txt", fis));
            }, (e) -> {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform PrintStageEvent while uploading sale event result");

                try {
                    fis.close();
                } catch (IOException ex) {
                    StaticStore.logger.uploadErrorLog(ex, "Failed to perform PrintStageEvent while uploading sale event result");
                }
            }, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    StaticStore.logger.uploadErrorLog(e, "Failed to perform PrintStageEvent while uploading sale event result");
                }
            });
        } else {
            createMessage(ch, m -> m.content(result));
        }
    }
}
