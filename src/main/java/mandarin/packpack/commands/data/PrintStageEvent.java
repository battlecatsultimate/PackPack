package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class PrintStageEvent extends ConstraintCommand {
    public PrintStageEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
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

        ArrayList<String> result = StaticStore.event.printStageEvent(getLocale(getContent(event)), lang, isFull(getContent(event)), isRaw(getContent(event)), now, t);

        if(result.isEmpty()) {
            ch.sendMessage(LangID.getStringByID("chevent_noup", lang)).queue();

            return;
        }

        boolean goWithFile = false;

        for(int k = 0; k < result.size(); k++) {
            if(result.get(k).length() >= 1950) {
                goWithFile = true;
                break;
            }
        }

        if(goWithFile) {
            StringBuilder total = new StringBuilder(LangID.getStringByID("event_stage", holder.config.lang).replace("**", "")).append("\n");

            for(int k = 0; k < result.size(); k++) {
                total.append(result.get(k).replace("```scss\n", "").replace("```", ""));

                if(k < result.size() - 1)
                    total.append("\n");
            }

            File temp = new File("./temp");

            if(!temp.exists() && !temp.mkdirs()) {
                StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());
                return;
            }

            File res = StaticStore.generateTempFile(temp, "event", ".txt", false);

            if(res == null) {
                return;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

            writer.write(total.toString());

            writer.close();

            sendMessageWithFile(ch, LangID.getStringByID("printstage_toolong", holder.config.lang), res, "stageAndEvent.txt");
        } else {
            for(int k = 0; k < result.size(); k++) {
                StringBuilder merge = new StringBuilder();

                if(k == 0) {
                    merge.append(LangID.getStringByID("event_stage", holder.config.lang)).append("\n\n");
                } else {
                    merge.append("** **\n");
                }

                while(merge.length() < 2000) {
                    if(k >= result.size())
                        break;

                    if(result.get(k).length() + merge.length() >= 2000) {
                        k--;
                        break;
                    }

                    merge.append(result.get(k));

                    if(k < result.size() - 1) {
                        merge.append("\n");
                    }

                    k++;
                }

                ch.sendMessage(merge.toString())
                        .setAllowedMentions(new ArrayList<>())
                        .queue();
            }
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
