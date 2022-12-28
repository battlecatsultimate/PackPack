package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.TasteApk;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DownloadApk extends ConstraintCommand {
    public DownloadApk(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
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

        if(StaticStore.apkDownloading) {
            ch.sendMessage("APK is downloading, wait for process to be done").queue();

            return;
        }

        StaticStore.apkDownloading = true;

        File googlePlay = new File("./googlePlay");

        if(!googlePlay.exists() || googlePlay.isFile()) {
            ch.sendMessage("Failed to find apk downloader scripts. Download process aborted").queue();

            StaticStore.apkDownloading = false;

            return;
        }

        Message m = getMessage(event);

        if(m == null) {
            StaticStore.apkDownloading = false;

            return;
        }

        int loc = getLocale(m.getContentRaw());

        String localeCode;

        switch (loc) {
            case LangID.JP:
                localeCode = "jp";
                break;
            case LangID.ZH:
                localeCode = "tw";
                break;
            case LangID.KR:
                localeCode = "kr";
                break;
            default:
                localeCode = "en";
                break;
        }

        File workspace = new File("./data/bc/"+localeCode.replace("tw", "zh")+"/workspace");

        if(workspace.exists()) {
            ch.sendMessage("Resetting workspace...").queue();

            StaticStore.deleteFile(workspace, true);
        }

        ch.sendMessage("Downloading apk files...").queue();

        ProcessBuilder builder = new ProcessBuilder(SystemUtils.IS_OS_WINDOWS ? "googlePlay/.venv/Scripts/python.exe" : "googlePlay/.venv/bin/python3", "googlePlay/downloader/"+localeCode+".py", "-e", StaticStore.GOOGLE_EMAIL, "-p", StaticStore.GOOGLE_APP);
        builder.redirectErrorStream(true);

        Process pro = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

        String line;

        while((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        pro.waitFor();

        File apk = new File("./data/bc/"+localeCode.replace("tw", "zh")+"/app.apk");

        if(!apk.exists()) {
            ch.sendMessage("It seems that apk downloading was unsuccessful, analyzing process aborted").queue();

            StaticStore.apkDownloading = false;

            return;
        }

        ch.sendMessage("Apk download successful, validating apk file...").queue();

        if(!TasteApk.isValidApk(apk)) {
            ch.sendMessage("Bot caught invalid format in apk file, check raw file manually, aborted").queue();

            StaticStore.apkDownloading = false;

            return;
        }

        ch.sendMessage("Validation successful, tasting apk file...").queue();

        String result = TasteApk.tasteApk(apk, ch, loc);

        if(result.isBlank()) {
            ch.sendMessage("Tasting apk file failed...").queue();
        } else {
            File log = generateLogFile(result);

            String content;

            if (result.contains("ABORTED")) {
                content = "Process aborted due to error during tasting";
            } else {
                content = "Process done successfully";
            }

            if(log != null)
                sendMessageWithFile(ch, content, log, "result.txt");
            else
                ch.sendMessage(content).queue();
        }

        StaticStore.apkDownloading = false;
    }

    private int getLocale(String content) {
        if(content.contains("-tw"))
            return LangID.ZH;
        else if(content.contains("-kr"))
            return LangID.KR;
        else if(content.contains("-jp"))
            return LangID.JP;
        else
            return LangID.EN;
    }

    private File generateLogFile(String result) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());

            return null;
        }

        File log = StaticStore.generateTempFile(temp, "result", ".txt", false);

        if(log == null) {
            return null;
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(log, StandardCharsets.UTF_8));

        writer.write(result);

        writer.close();

        return log;
    }

    @Override
    public void onFail(GenericMessageEvent event, int error) {
        super.onFail(event, error);

        StaticStore.apkDownloading = false;
    }
}
