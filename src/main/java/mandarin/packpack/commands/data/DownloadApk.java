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
import java.util.ArrayList;
import java.util.List;

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

        File googlePlay = new File("./googlePlay/cmd/googleplay");

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
        String packageName;

        switch (loc) {
            case LangID.JP:
                localeCode = "jp";
                packageName = "jp.co.ponos.battlecats";
                break;
            case LangID.ZH:
                localeCode = "tw";
                packageName = "jp.co.ponos.battlecatstw";
                break;
            case LangID.KR:
                localeCode = "kr";
                packageName = "jp.co.ponos.battlecatskr";
                break;
            default:
                localeCode = "en";
                packageName = "jp.co.ponos.battlecatsen";
                break;
        }

        File workspace = new File("./data/bc/"+localeCode.replace("tw", "zh")+"/workspace");

        if(workspace.exists()) {
            ch.sendMessage("Resetting workspace...").queue();

            StaticStore.deleteFile(workspace, true);
        }

        File[] googleFiles = googlePlay.listFiles();

        if(googleFiles == null) {
            ch.sendMessage("Something went wrong while resetting workspace...").queue();

            StaticStore.apkDownloading = false;

            return;
        }

        for(File f : googleFiles) {
            if(f.getName().endsWith(".apk")) {
                StaticStore.deleteFile(f, true);
            }
        }

        ch.sendMessage("Getting apk version code...").queue();

        ProcessBuilder builder = new ProcessBuilder("./googlePlay/cmd/googleplay/googleplay", "-a", packageName);
        builder.redirectErrorStream(true);

        Process pro = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

        String line;

        List<String> lines = new ArrayList<>();

        while((line = reader.readLine()) != null) {
            lines.add(line);
        }

        pro.waitFor();

        reader.close();

        String versionCode = null;

        for(int i = 0; i < lines.size(); i++) {
            if(lines.get(i).startsWith("Version Code:")) {
                String[] data = lines.get(i).split(": ");

                if(data.length != 2) {
                    ch.sendMessage("Failed to get version code, aborted downloading process").queue();

                    StaticStore.apkDownloading = false;

                    return;
                }

                ch.sendMessage("Version found, code is `" + data[1] + "`").queue();

                versionCode = data[1];

                break;
            }
        }

        if(versionCode == null) {
            ch.sendMessage("Failed to get version code, aborted downloading process").queue();

            StaticStore.apkDownloading = false;

            return;
        } else {
            ch.sendMessage("Downloading apk file...").queue();
        }

        builder = new ProcessBuilder("./googlePlay/cmd/googleplay/googleplay", "-a", packageName, "-v", versionCode, "-s");
        builder.redirectErrorStream(false);

        pro = builder.start();

        reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

        while((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        pro.waitFor();
        reader.close();

        File tempApkFolder = new File("./");

        File apkFile = null;
        File[] tempFolderList = tempApkFolder.listFiles();

        if(tempFolderList == null) {
            ch.sendMessage("Something went wrong while checking downloaded apk file...").queue();

            StaticStore.apkDownloading = false;

            return;
        }

        for(File f : tempFolderList) {
            System.out.println(f.getName());
            if(f.getName().endsWith(".apk")) {
                System.out.println(f.getName() +" | " + f.length());
                apkFile = f;

                break;
            }
        }

        if(apkFile == null) {
            ch.sendMessage("It seems that apk downloading was unsuccessful, analyzing process aborted").queue();

            StaticStore.apkDownloading = false;

            return;
        }

        File apk = new File("./data/bc/"+localeCode.replace("tw", "zh")+"/app.apk");

        if(!apkFile.renameTo(apk)) {
            ch.sendMessage("Failed to move downloaded apk file to proper place...").queue();

            StaticStore.apkDownloading = false;

            return;
        }

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
