package mandarin.packpack.commands.data;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateFields;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PrintGachaEvent extends ConstraintCommand {
    public PrintGachaEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        boolean now = isNow(getContent(event));
        int t = 0;

        if(now) {
            Optional<Member> m = getMember(event);

            if(m.isPresent()) {
                t = StaticStore.timeZones.getOrDefault(m.get().getId().asString(), 0);

                String content;

                if(t >= 0)
                    content = "+" + t;
                else
                    content = "" + t;

                createMessage(ch, me -> me.content(LangID.getStringByID("printevent_time", lang).replace("_", content)));
            }
        }

        String result = StaticStore.event.printGachaEvent(getLocale(getContent(event)), lang, isFull(getContent(event)), isRaw(getContent(event)), now, t);

        if(result.isBlank()) {
            createMessage(ch, m -> m.content(LangID.getStringByID("chevent_noup", lang)));
            return;
        }

        if(result.length() >= 2000) {
            File temp = new File("./temp");

            if(!temp.exists() && !temp.mkdirs()) {
                StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());
                return;
            }

            File res = new File(temp, StaticStore.findFileName(temp, "gachaEvent", ".txt"));

            if(!res.exists() && !res.createNewFile()) {
                StaticStore.logger.uploadLog("Failed to create file : "+res.getAbsolutePath());
                return;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

            writer.write(result);

            writer.close();

            FileInputStream fis = new FileInputStream(res);

            createMessage(ch, m -> {
                m.content(LangID.getStringByID("printgacha_toolong", lang));
                m.addFile(MessageCreateFields.File.of("gachaEvent.txt", fis));
            }, (e) -> {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform PrintStageEvent while uploading sale event result");

                try {
                    fis.close();
                } catch (IOException ex) {
                    StaticStore.logger.uploadErrorLog(ex, "Failed to perform PrintStageEvent while uploading sale event result");
                }

                if(res.exists() && !res.delete()) {
                    StaticStore.logger.uploadLog("Faield to delte file : "+res.getAbsolutePath());
                }
            }, () -> {
                try {
                    fis.close();
                } catch (IOException e) {
                    StaticStore.logger.uploadErrorLog(e, "Failed to perform PrintStageEvent while uploading sale event result");
                }

                if(res.exists() && !res.delete()) {
                    StaticStore.logger.uploadLog("Faield to delte file : "+res.getAbsolutePath());
                }
            });
        } else {
            createMessage(ch, m -> m.content(result));
        }
    }

    private int getLang() {
        if(lang >= 1 && lang < 4) {
            return lang;
        } else {
            return 0;
        }
    }

    private int getLocale(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-en":
                    return LangID.EN;
                case "-tw":
                    return LangID.ZH;
                case "-kr":
                    return LangID.KR;
                case "-jp":
                    return LangID.JP;
            }
        }

        return getLang();
    }

    private boolean isFull(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-f") || contents[i].equals("-full"))
                return true;
        }

        return false;
    }

    private boolean isRaw(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-r") || contents[i].equals("-raw"))
                return true;
        }

        return false;
    }

    private boolean isNow(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if (contents[i].equals("-n") || contents[i].equals("-now")) {
                return true;
            }
        }

        return false;
    }
}
