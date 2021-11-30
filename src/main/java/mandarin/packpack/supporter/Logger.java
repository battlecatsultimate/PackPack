package mandarin.packpack.supporter;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.Command;
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

    public void uploadErrorLog(Throwable e, String message, File... files) {
        e.printStackTrace();

        MessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        String errMessage = errorMessages[StaticStore.random.nextInt(errorMessages.length)] +
                "\n\nMessage : " + message + "\n\n----- StackTrace -----\n\n```java\n" +
                ExceptionUtils.getStackTrace(e)+"\n```";

        if(errMessage.length() >= 2000) {
            errMessage = errMessage.substring(0, 1993) + "...\n```";
        }

        createMessageWithNoPingsWithFile(ch, errMessage, files);
    }

    public void uploadLog(String content) {
        System.out.println(content);

        MessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        createMessageWithNoPings(ch, content);
    }

    private void createMessageWithNoPings(MessageChannel ch, String content) {
        Command.createMessage(ch, m -> {
            m.content(content);
            m.allowedMentions(AllowedMentions.builder().build());
        });
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

        Command.createMessage(ch, m -> {
            m.content(content);
            m.allowedMentions(AllowedMentions.builder().build());
            for(int i = 0; i < files.length; i++) {
                m.addFile(files[i].getName(), fis.get(i));
            }
        }, (e) -> {
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
