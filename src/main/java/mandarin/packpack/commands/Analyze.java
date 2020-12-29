package mandarin.packpack.commands;

import common.io.assets.UpdateCheck;
import common.pack.PackData;
import common.pack.UserProfile;
import common.util.stage.MapColc;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Analyze extends ConstraintCommand {
    public static final long INTERVAL = 1000;
    public static long NOW  = System.currentTimeMillis();

    private final DecimalFormat df = new DecimalFormat("#.##");
    private Message msg;

    public Analyze(ROLE role) {
        super(role);
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        try {
            Message msg = event.getMessage();
            MessageChannel ch = getChannel(event);

            if(ch == null)
                return;

            if(!msg.getAttachments().isEmpty()) {
                List<Attachment> attachments = new ArrayList<>(msg.getAttachments());

                Attachment at = attachments.get(0);

                if(at.getFilename().endsWith("pack.bcuzip")) {
                    this.msg = ch.createMessage("Downloading Pack... : 0%").block();

                    String url = at.getUrl();

                    File target = StaticStore.getDownPackFile();
                    File temp = StaticStore.getTempPackFile();

                    UpdateCheck.Downloader down = new UpdateCheck.Downloader(url, target, temp, "", false);

                    down.run(this::editMessage);

                    this.msg.edit(m -> m.setContent("Analyzing Pack...")).subscribe();

                    PackData.UserPack pack = UserProfile.readZipPack(target);
                    pack.load();

                    this.msg.delete().subscribe();

                    ch.createEmbed(emb -> {
                        emb.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
                        emb.setTitle(pack.desc.name);
                        emb.addField("ID", pack.desc.id, false);
                        emb.addField("Total number of Units", Integer.toString(pack.units.getList().size()), true);
                        emb.addField("Total number of Enemies", Integer.toString(pack.enemies.getList().size()), true);
                        emb.addField("Total number of Stages", Integer.toString(MapColc.get(pack.desc.id).maps.size()), true);
                        emb.addField("Total number of Backgrounds", Integer.toString(pack.bgs.getList().size()), true);
                        emb.addField("Total number of Music", Integer.toString(pack.musics.getList().size()), true);
                        emb.setDescription(at.getFilename()+" ("+getFileSize(target.length())+")");
                    }).subscribe();

                    this.msg = null;

                    UserProfile.unloadAllUserPacks();

                    if(target.exists()) {
                        target.delete();
                    }

                    if(temp.exists()) {
                        temp.delete();
                    }
                } else {
                    ch.createMessage("Please upload pack.bcuzip file").subscribe();
                }
            } else {
                ch.createMessage("Please attach pack.bcuzip file which will be analyzed").subscribe();
            }
        } catch (Exception e) {
            onFail(event, DEFAULT_ERROR);
        }
    }

    private void editMessage(double prog) {
        if(System.currentTimeMillis() - NOW < INTERVAL)
            return;

        NOW = System.currentTimeMillis();

        if(msg != null) {
            msg.edit(m -> m.setContent("Downloading Pack... : "+df.format(prog*100)+"%")).subscribe();
        }
    }

    private String getFileSize(double size) {
        String[] siz = {"B", "KB", "MB"};

        int index = 0;

        while(size / 1000 >= 1.0) {
            size /= 1000.0;
            index++;

            if(index == 2)
                return df.format(size)+siz[index];
        }

        return df.format(size)+siz[index];
    }
}


