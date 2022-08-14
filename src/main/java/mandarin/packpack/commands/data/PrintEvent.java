package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PrintEvent extends ConstraintCommand {
    public PrintEvent(ROLE role, int lang, IDHolder id) {
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

        int loc = getLocale(getContent(event));
        int l = followServerLocale(getContent(event)) ? holder.config.lang : lang;
        boolean full = isFull(getContent(event));
        boolean raw = isRaw(getContent(event));

        String gacha = StaticStore.event.printGachaEvent(loc, l , full, raw, false, 0);
        String item = StaticStore.event.printItemEvent(loc, l, full, raw, false, 0);
        List<String> stage = StaticStore.event.printStageEvent(loc, l, full, raw, false, 0);

        if(gacha.isBlank() && item.isBlank() && stage.isEmpty()) {
            ch.sendMessage(LangID.getStringByID("chevent_noup", lang)).queue();

            return;
        }

        boolean done = false;
        boolean eventDone = false;

        for(int j = 0; j < 3; j++) {
            if(j == EventFactor.SALE) {
                if(stage.isEmpty())
                    continue;

                boolean wasDone = done;

                done = true;

                if(!eventDone) {
                    eventDone = true;

                    ch.sendMessage(LangID.getStringByID("event_loc"+loc, l)).queue();
                }

                boolean goWithFile = false;

                for(int k = 0; k < stage.size(); k++) {
                    if(stage.get(k).length() >= 1950) {
                        goWithFile = true;
                        break;
                    }
                }

                if(goWithFile) {
                    StringBuilder total = new StringBuilder(LangID.getStringByID("event_stage", l).replace("**", "")).append("\n\n");

                    for(int k = 0; k < stage.size(); k++) {
                        total.append(stage.get(k).replace("```scss\n", "").replace("```", ""));

                        if(k < stage.size() - 1)
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

                    sendMessageWithFile(ch, (wasDone ? "** **\n" : "") + LangID.getStringByID("printstage_toolong", l), res, "event.txt");
                } else {
                    for(int k = 0; k < stage.size(); k++) {
                        StringBuilder merge = new StringBuilder();

                        if(k == 0) {
                            if(wasDone) {
                                merge.append("** **\n");
                            }

                            merge.append(LangID.getStringByID("event_stage", l)).append("\n\n");
                        } else {
                            merge.append("** **\n");
                        }

                        while(merge.length() < 2000) {
                            if(k >= stage.size())
                                break;

                            if(stage.get(k).length() + merge.length() >= 2000) {
                                k--;
                                break;
                            }

                            merge.append(stage.get(k));

                            if(k < stage.size() - 1) {
                                merge.append("\n");
                            }

                            k++;
                        }

                        ch.sendMessage(merge.toString())
                                .allowedMentions(new ArrayList<>())
                                .queue();
                    }
                }
            } else {
                String result;

                if(j == EventFactor.GATYA)
                    result = gacha;
                else
                    result = item;

                if(result.isBlank()) {
                    continue;
                }

                boolean wasDone = done;

                done = true;

                if(!eventDone) {
                    eventDone = true;

                    ch.sendMessage(LangID.getStringByID("event_loc"+loc, l)).queue();
                }

                if(result.length() >= 1980) {
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

                    writer.write(result);

                    writer.close();

                    String lID;

                    if(j == EventFactor.GATYA) {
                        lID = "printgacha_toolong";
                    } else {
                        lID = "printitem_toolong";
                    }

                    sendMessageWithFile(ch, (wasDone ? "** **\n" : "") + LangID.getStringByID(lID, l), res, "event.txt");
                } else {
                    ch.sendMessage((wasDone ? "** **\n" : "") + result)
                            .allowedMentions(new ArrayList<>())
                            .queue();
                }
            }
        }

        if(done) {
            ch.sendMessage(LangID.getStringByID("event_warning", holder.config.lang)).queue();
        }
    }

    private boolean followServerLocale(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-s") || contents[i].equals("-server")) {
                return true;
            }
        }

        return false;
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
}
