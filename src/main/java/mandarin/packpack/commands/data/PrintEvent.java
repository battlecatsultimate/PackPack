package mandarin.packpack.commands.data;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PrintEvent extends ConstraintCommand {
    public PrintEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        int loc = getLocale(getContent(event));
        int l = followServerLocale(getContent(event)) ? holder.serverLocale : lang;
        boolean full = isFull(getContent(event));

        String gacha = StaticStore.event.printGachaEvent(loc, l , full);
        String item = StaticStore.event.printItemEvent(loc, l, full);
        List<String> stage = StaticStore.event.printStageEvent(loc, l, full);

        if(gacha.isBlank() && item.isBlank() && stage.isEmpty()) {
            createMessage(ch, m -> m.content(LangID.getStringByID("chevent_noup", lang)));
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
                    ch.createMessage(MessageCreateSpec.builder().content(LangID.getStringByID("event_loc"+loc, l)).build()).subscribe();
                }

                boolean goWithFile = false;

                for(int k = 0; k < stage.size(); k++) {
                    if(stage.get(k).length() >= 1950) {
                        goWithFile = true;
                        break;
                    }
                }

                if(goWithFile) {
                    MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

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

                    File res = new File(temp, StaticStore.findFileName(temp, "event", ".txt"));

                    if(!res.exists() && !res.createNewFile()) {
                        StaticStore.logger.uploadLog("Failed to create file : "+res.getAbsolutePath());
                        return;
                    }

                    BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

                    writer.write(total.toString());

                    writer.close();

                    FileInputStream fis = new FileInputStream(res);

                    builder.content((wasDone ? "** **\n" : "") + LangID.getStringByID("printstage_toolong", l))
                            .addFile(MessageCreateFields.File.of("event.txt", fis));

                    ch.createMessage(builder.build()).subscribe(null, (e) -> {
                        StaticStore.logger.uploadErrorLog(e, "Failed to perform uploading stage event data");

                        try {
                            fis.close();
                        } catch (IOException ex) {
                            StaticStore.logger.uploadErrorLog(ex, "Failed close stream while uploading stage event data");
                        }

                        if(res.exists() && !res.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                        }
                    }, () -> {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            StaticStore.logger.uploadErrorLog(e, "Failed close stream while uploading stage event data");
                        }

                        if(res.exists() && !res.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                        }
                    });
                } else {
                    for(int k = 0; k < stage.size(); k++) {
                        MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

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

                        builder.content(merge.toString());
                        builder.allowedMentions(AllowedMentions.builder().build());

                        ch.createMessage(builder.build()).subscribe();
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
                    ch.createMessage(MessageCreateSpec.builder().content(LangID.getStringByID("event_loc"+loc, l)).build()).subscribe();
                }

                MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

                builder.allowedMentions(AllowedMentions.builder().build());

                if(result.length() >= 1980) {
                    File temp = new File("./temp");

                    if(!temp.exists() && !temp.mkdirs()) {
                        StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());
                        return;
                    }

                    File res = new File(temp, StaticStore.findFileName(temp, "event", ".txt"));

                    if(!res.exists() && !res.createNewFile()) {
                        StaticStore.logger.uploadLog("Failed to create file : "+res.getAbsolutePath());
                        return;
                    }

                    BufferedWriter writer = new BufferedWriter(new FileWriter(res, StandardCharsets.UTF_8));

                    writer.write(result);

                    writer.close();

                    FileInputStream fis = new FileInputStream(res);

                    String lID;

                    if(j == EventFactor.GATYA) {
                        lID = "printgacha_toolong";
                    } else {
                        lID = "printitem_toolong";
                    }

                    builder.content((wasDone ? "** **\n" : "") + LangID.getStringByID(lID, l))
                            .addFile(MessageCreateFields.File.of("event.txt", fis));

                    ch.createMessage(builder.build()).subscribe(null, (e) -> {
                        StaticStore.logger.uploadErrorLog(e, "Failed to perform uploading stage event data");

                        try {
                            fis.close();
                        } catch (IOException ex) {
                            StaticStore.logger.uploadErrorLog(ex, "Failed close stream while uploading stage event data");
                        }

                        if(res.exists() && !res.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                        }
                    }, () -> {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            StaticStore.logger.uploadErrorLog(e, "Failed close stream while uploading stage event data");
                        }

                        if(res.exists() && !res.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+res.getAbsolutePath());
                        }
                    });
                } else {
                    builder.content((wasDone ? "** **\n" : "") + result);

                    ch.createMessage(builder.build()).subscribe();
                }
            }
        }

        if(done) {
            ch.createMessage(MessageCreateSpec.builder().content(LangID.getStringByID("event_warning", holder.serverLocale)).build()).subscribe();
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
}
