package mandarin.packpack.supporter.server.holder.message;

import common.CommonStatic;
import common.io.assets.UpdateCheck;
import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FileAnalyzerHolder extends MessageHolder {
    private static final int INVALID = -3, FAILED = -2, READY = -1, SUCCESS = 1;

    protected final Message msg;
    protected final File container;

    private final Map<String, File> resultFiles = new HashMap<>();

    private final List<String> requiredFiles;
    private final List<Integer> fileDownloaded = new ArrayList<>();

    public FileAnalyzerHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, File container, List<String> requiredFiles, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.msg = message;
        this.container = container;

        this.requiredFiles = requiredFiles;

        for(int i = 0; i < requiredFiles.size(); i++) {
            fileDownloaded.add(READY);
        }

        if(!checkAttachments(author, false)) {
            StaticStore.putHolder(author.getAuthor().getId(), this);

            registerAutoExpiration(FIVE_MIN);
        }
    }

    @Override
    public STATUS onReceivedEvent(MessageReceivedEvent event) {
        MessageChannel ch = event.getChannel();

        if(!ch.getId().equals(channelID))
            return STATUS.WAIT;

        Message m = event.getMessage();

        if(m.getContentRaw().equals("c")) {
            msg.editMessage(LangID.getStringByID("statAnalyzer.canceled", lang)).queue();

            StaticStore.deleteFile(container, true);

            m.delete().queue();

            return STATUS.FINISH;
        } else if(checkAttachments(m, true)) {
            return STATUS.FINISH;
        }

        return STATUS.WAIT;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        msg.editMessage(LangID.getStringByID("ui.search.expired", lang))
                .mentionRepliedUser(false)
                .queue();
    }

    public boolean hasValidFileFormat(File result) throws Exception {
        String name = result.getName();

        if(name.endsWith(".png")) {
            return AnimMixer.validPng(result);
        } else if(name.endsWith(".maanim")) {
            return AnimMixer.validMaanim(result);
        }

        return true;
    }

    public abstract void perform(Map<String, File> fileMap) throws Exception;

    public void registerMoreFile(String... files) {
        for(int i = 0; i < files.length; i++) {
            requiredFiles.add(files[i]);
            fileDownloaded.add(READY);
        }
    }

    private boolean checkAttachments(Message m, boolean delete) {
        if(!m.getAttachments().isEmpty()) {
            for(Message.Attachment a : m.getAttachments()) {
                if(requiredFiles.contains(a.getFileName())) {
                    downloadFile(a);
                }
            }

            if(delete)
                m.delete().queue();

            edit();

            if(allDownloaded()) {
                RecordableThread t = new RecordableThread(() -> {
                    perform(resultFiles);

                    StaticStore.executorHandler.postDelayed(1000, this::releaseFiles);
                }, e -> StaticStore.logger.uploadErrorLog(e, "E/FileAnalyzerHolder::checkAttachments - Error happened while trying to perform file analyzing"));

                t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + getAuthorMessage().getContentRaw());
                t.start();

                return true;
            }
        }

        return false;
    }

    private void downloadFile(Message.Attachment attachment) {
        try {
            UpdateCheck.Downloader downloader = StaticStore.getDownloader(attachment, container);

            int index = requiredFiles.indexOf(attachment.getFileName());

            if(downloader != null) {
                downloader.run(d -> {});

                File result = new File(container, attachment.getFileName());

                if(result.exists()) {
                    if(hasValidFileFormat(result)) {
                        fileDownloaded.set(index, SUCCESS);

                        resultFiles.put(attachment.getFileName(), result);
                    } else {
                        fileDownloaded.set(index, INVALID);

                        if(!result.delete()) {
                            StaticStore.logger.uploadLog("W/FileAnalyzerHolder::downloadFile - Failed to delete file : "+result.getAbsolutePath());
                        }
                    }
                } else {
                    fileDownloaded.set(index, FAILED);
                }
            } else {
                fileDownloaded.set(index, FAILED);
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FileAnalyzerHolder::downloadFile - Error happened while trying to download file");
        }
    }

    private void edit() {
        StringBuilder builder = new StringBuilder("- Required File List -\n\n");

        for(int i = 0; i < requiredFiles.size(); i++) {
            String status = switch (fileDownloaded.get(i)) {
                case FAILED -> "Failed";
                case READY -> "Ready";
                default -> "Done";
            };

            builder.append(requiredFiles.get(i))
                    .append(" : ")
                    .append(status);

            if(i < requiredFiles.size() - 1) {
                builder.append("\n");
            }
        }

        msg.editMessage(builder.toString()).queue();
    }

    private boolean allDownloaded() {
        for(int i = 0; i < fileDownloaded.size(); i++) {
            if(fileDownloaded.get(i) != SUCCESS)
                return false;
        }

        return true;
    }

    private void releaseFiles() {
        StaticStore.deleteFile(container, true);
    }
}
