package mandarin.packpack.supporter;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.ArrayList;

public class Logger {
    private static final String[] errorMessages = {
            "Fix me",
            "I literally had seizure while doing job",
            "Exception happened",
            "Help me",
            "Bruh"
    };

    private final JDA client;

    public Logger(JDA client) {
        this.client = client;
    }

    private GuildMessageChannel getLoggingChannel() {
        if(StaticStore.loggingChannel.isBlank())
            return null;

        GuildChannel ch = client.getGuildChannelById(StaticStore.loggingChannel);

        if(ch instanceof GuildMessageChannel)
            return (GuildMessageChannel) ch;
        else
            return null;
    }

    public void uploadErrorLog(Throwable e, String message, File... files) {
        e.printStackTrace();

        GuildMessageChannel ch = getLoggingChannel();

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

        GuildMessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        createMessageWithNoPings(ch, content);
    }

    private void createMessageWithNoPings(GuildMessageChannel ch, String content) {
        ch.sendMessage(content)
                .allowedMentions(new ArrayList<>())
                .queue();
    }

    private void createMessageWithNoPingsWithFile(GuildMessageChannel ch, String content, File... files) {
        for(File f : files) {
            if(!f.exists()) {
                throw new IllegalStateException("File doesn't exist : "+f.getAbsolutePath());
            }
        }

        MessageAction action = ch.sendMessage(content)
                .allowedMentions(new ArrayList<>());

        for(int i = 0; i < files.length; i++) {
            action = action.addFile(files[i], files[i].getName());
        }

        action.queue();
    }
}
