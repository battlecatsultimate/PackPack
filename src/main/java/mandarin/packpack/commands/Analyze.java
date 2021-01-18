package mandarin.packpack.commands;

import common.io.assets.AssetLoader;
import common.io.assets.UpdateCheck;
import common.pack.PackData;
import common.pack.UserProfile;
import common.util.stage.MapColc;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

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

    public Analyze(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
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
                    this.msg = ch.createMessage(LangID.getStringByID("analyz_down", lang).replace("_", String.valueOf(0))).block();

                    String url = at.getUrl();

                    File target = StaticStore.getDownPackFile();
                    File temp = StaticStore.getTempPackFile();

                    UpdateCheck.Downloader down = new UpdateCheck.Downloader(url, target, temp, "", false);

                    down.run(this::editMessage);

                    this.msg.edit(m -> m.setContent(LangID.getStringByID("analyz_pack", lang))).subscribe();

                    PackData.UserPack pack = UserProfile.readZipPack(target);

                    if(!canAdd(pack)) {
                        this.msg.edit(m -> m.setContent(LangID.getStringByID("analyz_parent", lang))).subscribe();
                        return;
                    }

                    UserProfile.profile().pending.put(pack.desc.id, pack);

                    pack.load();

                    UserProfile.profile().packmap.put(pack.desc.id, pack);

                    this.msg.delete().subscribe();

                    ch.createEmbed(emb -> {
                        emb.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
                        emb.setTitle(pack.desc.name);
                        emb.addField("ID", pack.desc.id, false);
                        emb.addField(LangID.getStringByID("analyz_unit", lang), Integer.toString(pack.units.getList().size()), true);
                        emb.addField(LangID.getStringByID("analyz_enemy", lang), Integer.toString(pack.enemies.getList().size()), true);
                        emb.addField(LangID.getStringByID("analyz_stage", lang), Integer.toString(MapColc.get(pack.desc.id).maps.size()), true);
                        emb.addField(LangID.getStringByID("analyz_bg", lang), Integer.toString(pack.bgs.getList().size()), true);
                        emb.addField(LangID.getStringByID("analyz_music", lang), Integer.toString(pack.musics.getList().size()), true);
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
                    ch.createMessage(LangID.getStringByID("analyz_file", lang)).subscribe();
                }
            } else {
                ch.createMessage(LangID.getStringByID("analyz_attach", lang)).subscribe();
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
            msg.edit(m -> m.setContent(LangID.getStringByID("analyz_down", lang).replace("_", df.format(prog*100)))).subscribe();
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

    private boolean canAdd(PackData.UserPack pack) {
        return pack.desc.dependency.isEmpty();
    }
}


