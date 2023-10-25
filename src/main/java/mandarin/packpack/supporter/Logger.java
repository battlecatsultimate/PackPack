package mandarin.packpack.supporter;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Logger {
    private static final String[] errorMessages = {
            "Fix me",
            "I literally had seizure while doing job",
            "Exception happened",
            "Help me",
            "Bruh"
    };
    private static final String separatorHalf = "==========";
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");

    public final static List<String> logMessages = new ArrayList<>();

    public static void writeLog() {
        if (logMessages.isEmpty())
            return;

        File logFile = new File("./data/log.txt");

        try {
            if (!logFile.exists() && !logFile.createNewFile())
                return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (FileWriter fileWriter = new FileWriter(logFile, StandardCharsets.UTF_8, true)) {
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter writer = new PrintWriter(bufferedWriter);

            for (int i = 0; i < logMessages.size(); i++) {
                writer.println(logMessages.get(i));
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logMessages.clear();
    }

    @Nullable
    private ShardManager client;

    public Logger() {
        client = null;

        logMessages.add("");
        logMessages.add(separatorHalf + " BOT STARTED " + getTimeStamp() + " " + separatorHalf);
        logMessages.add("");
    }

    public void assignClient(ShardManager client) {
        this.client = client;
    }

    @Nullable
    private GuildMessageChannel getLoggingChannel() {
        if (client == null)
            return null;

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

        logMessages.add(separatorHalf + " EXCEPTION HAPPENED " + separatorHalf);
        logMessages.add(getTimeStamp() + " - " + message);
        logMessages.add("");
        logMessages.add(ExceptionUtils.getStackTrace(e));
        logMessages.add(separatorHalf + "====================" + separatorHalf);

        GuildMessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        String errMessage = errorMessages[StaticStore.random.nextInt(errorMessages.length)] +
                "\n\n" + message + "\n\n----- StackTrace -----\n\n```java\n" +
                ExceptionUtils.getStackTrace(e)+"\n```";

        if(errMessage.length() >= 2000) {
            errMessage = errMessage.substring(0, 1993) + "...\n```";
        }

        try {
            File temp = new File("temp");

            if(!temp.exists() && !temp.mkdirs()) {
                uploadLog("Failed to create folder : "+temp.getAbsolutePath());

                createMessageWithNoPingsWithFile(ch, errMessage, null, files);

                return;
            }

            File log = StaticStore.generateTempFile(temp, "log", ".txt", false);

            if(log == null) {
                return;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(log));

            writer.write(ExceptionUtils.getStackTrace(e));

            writer.close();

            File[] fs = new File[1 + files.length];

            for(int i = 0; i < fs.length; i++) {
                if(i == 0) {
                    fs[i] = log;
                } else {
                    fs[i] = files[i - 1];
                }
            }

            createMessageWithNoPingsWithFile(ch, errMessage, () -> {
                if(log.exists() && !log.delete()) {
                    uploadLog("Failed to delete file : "+log.getAbsolutePath());
                }
            }, fs);
        } catch (Exception err) {
            createMessageWithNoPingsWithFile(ch, errMessage, null, files);
        }
    }

    public void uploadLog(String content) {
        logMessages.add(getTimeStamp() + " - " + content);

        System.out.println(content);

        GuildMessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        createMessageWithNoPings(ch, content);
    }

    public void uploadLogWithPing(String content) {
        System.out.println(content);

        GuildMessageChannel ch = getLoggingChannel();

        if(ch == null)
            return;

        ch.sendMessage(content).queue();
    }

    public static void addLog(String content) {
        String[] segments = content.split("\n");

        for (int i = 0; i < segments.length; i++) {
            String line;

            if (i == 0) {
                line = getTimeStamp() + " - " + content;
            } else {
                line = " ".repeat(24) + content;
            }

            logMessages.add(line);
            System.out.println(line);
        }
    }

    private void createMessageWithNoPings(GuildMessageChannel ch, String content) {
        ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private void createMessageWithNoPingsWithFile(GuildMessageChannel ch, String content, Runnable run, File... files) {
        for(File f : files) {
            if(!f.exists()) {
                throw new IllegalStateException("File doesn't exist : "+f.getAbsolutePath());
            }
        }

        MessageCreateAction action = ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>());

        for(int i = 0; i < files.length; i++) {
            action = action.addFiles(FileUpload.fromData(files[i], files[i].getName()));
        }

        action.queue(m -> {
            if(run != null)
                run.run();
        }, e -> {
            if(run != null)
                run.run();
        });
    }

    private static String getTimeStamp() {
        Date currentTime = new Date(System.currentTimeMillis());

        return "[" + format.format(currentTime) + "]";
    }
}
