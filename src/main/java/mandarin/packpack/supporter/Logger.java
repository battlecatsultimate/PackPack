package mandarin.packpack.supporter;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.AllowedMentions;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class Logger {
    private static final String[] errorMessages = {
            "Fix me",
            "I literally had seizure while doing job",
            "Exception happened",
            "Help me",
            "Bruh"
    };

    private final GatewayDiscordClient client;

    public Logger(GatewayDiscordClient client) {
        this.client = client;
    }

    private MessageChannel getLoggingChannel() {
        if(StaticStore.loggingChannel.isBlank())
            return null;

        Channel ch = client.getChannelById(Snowflake.of(StaticStore.loggingChannel)).block();

        if(ch instanceof MessageChannel)
            return (MessageChannel) ch;
        else
            return null;
    }

    public void uploadErrorLog(Exception e, String message, File... files) {
        MessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        String errMessage = errorMessages[StaticStore.random.nextInt(errorMessages.length)] +
                "\n\nMessage : " + message + "\n\n----- StackTrace -----\n\n```java\n" +
                ExceptionUtils.getStackTrace(e)+"\n```";

        if(errMessage.length() >= 2000) {
            errMessage = errMessage.substring(0, 1997) + "...";
        }

        createMessageWithNoPingsWithFile(ch, errMessage, files);
    }

    public void uploadLog(String content) {
        MessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        createMessageWithNoPings(ch, content);
    }

    private void createMessageWithNoPings(MessageChannel ch, String content) {
        ch.createMessage(m -> {
            m.setContent(content);
            m.setAllowedMentions(AllowedMentions.builder().build());
        }).subscribe();
    }

    private void createMessageWithNoPingsWithFile(MessageChannel ch, String content, File... files) {
        ArrayList<FileInputStream> fis = new ArrayList<>();

        for(File f : files) {
            if(f.exists()) {
                try {
                    FileInputStream fi = new FileInputStream(f);
                    fis.add(fi);
                } catch (FileNotFoundException e) {
                    uploadErrorLog(e, "Failed to open file input stream while uploading error log : "+f.getAbsolutePath());
                    e.printStackTrace();
                    return;
                }
            }
        }

        ch.createMessage(m -> {
            m.setContent(content);
            m.setAllowedMentions(AllowedMentions.builder().build());
            for(int i = 0; i < files.length; i++) {
                m.addFile(files[i].getName(), fis.get(i));
            }
        }).subscribe(null, (e) -> {
            for(FileInputStream fi : fis) {
                try {
                    fi.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }, () -> {
            for(FileInputStream fi : fis) {
                try {
                    fi.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
