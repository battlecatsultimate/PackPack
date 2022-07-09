package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class PrintGachaEvent extends ConstraintCommand {
    public PrintGachaEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        boolean now = isNow(getContent(event));
        int t = 0;

        if(now) {
            Member m = getMember(event);

            if(m != null) {
                t = StaticStore.timeZones.getOrDefault(m.getId(), 0);

                String content;

                if(t >= 0)
                    content = "+" + t;
                else
                    content = "" + t;

                ch.sendMessage(LangID.getStringByID("printevent_time", lang).replace("_", content)).queue();
            }
        }

        String result = StaticStore.event.printGachaEvent(getLocale(getContent(event)), lang, isFull(getContent(event)), isRaw(getContent(event)), now, t);

        if(result.isBlank()) {
            ch.sendMessage(LangID.getStringByID("chevent_noup", lang)).queue();

            return;
        }

        if(result.length() >= 2000) {
            File temp = new File("./temp");

            if(!temp.exists() && !temp.mkdirs()) {
                StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());
                return;
            }

            File res = StaticStore.generateTempFile(temp, "gachaEvent", ".txt", false);

            if(res == null) {
                return;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

            writer.write(result);

            writer.close();

            sendMessageWithFile(ch, LangID.getStringByID("printgacha_toolong", lang), res, "gachaEvent.txt");
        } else {
            ch.sendMessage(result).queue();
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
