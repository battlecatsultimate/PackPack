package mandarin.packpack.commands;

import common.io.assets.UpdateCheck;
import common.pack.PackData;
import common.pack.UserProfile;
import common.util.stage.MapColc;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

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
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        try {
            Message msg = getMessage(event);
            MessageChannel ch = getChannel(event);

            if(ch == null)
                return;

            if(msg == null)
                return;

            if(!msg.getAttachments().isEmpty()) {
                List<Message.Attachment> attachments = new ArrayList<>(msg.getAttachments());

                Message.Attachment at = attachments.get(0);

                if(at.getFileName().endsWith("pack.bcuzip")) {
                    this.msg = ch.sendMessage(LangID.getStringByID("analyz_down", lang).replace("_", String.valueOf(0))).complete();

                    String url = at.getUrl();

                    File target = StaticStore.getDownPackFile();
                    File temp = StaticStore.getTempPackFile();

                    UpdateCheck.Downloader down = new UpdateCheck.Downloader(target, temp, "", false, url);

                    down.run(this::editMessage);

                    this.msg.editMessage(LangID.getStringByID("analyz_pack", lang)).queue();

                    PackData.UserPack pack = UserProfile.readZipPack(target);

                    if(!canAdd(pack)) {
                        this.msg.editMessage(LangID.getStringByID("analyz_parent", lang)).queue();

                        return;
                    }

                    UserProfile.profile().pending.put(pack.desc.id, pack);

                    pack.load();

                    UserProfile.profile().packmap.put(pack.desc.id, pack);

                    this.msg.delete().queue();

                    EmbedBuilder builder = new EmbedBuilder();

                    builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                            .setTitle(pack.desc.name)
                            .addField("ID", pack.desc.id, false)
                            .addField(LangID.getStringByID("analyz_unit", lang), Integer.toString(pack.units.getList().size()), true)
                            .addField(LangID.getStringByID("analyz_enemy", lang), Integer.toString(pack.enemies.getList().size()), true)
                            .addField(LangID.getStringByID("analyz_stage", lang), Integer.toString(MapColc.get(pack.desc.id).maps.size()), true)
                            .addField(LangID.getStringByID("analyz_bg", lang), Integer.toString(pack.bgs.getList().size()), true)
                            .addField(LangID.getStringByID("analyz_music", lang), Integer.toString(pack.musics.getList().size()), true)
                            .setDescription(at.getFileName()+" ("+getFileSize(target.length())+")");

                    ch.sendMessageEmbeds(builder.build()).queue();

                    this.msg = null;

                    UserProfile.unloadAllUserPacks();

                    if(target.exists()) {
                        target.delete();
                    }

                    if(temp.exists()) {
                        temp.delete();
                    }
                } else {
                    ch.sendMessage(LangID.getStringByID("analyz_file", lang)).queue();
                }
            } else {
                ch.sendMessage(LangID.getStringByID("analyz_attach", lang)).queue();
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
            msg.editMessage(LangID.getStringByID("analyz_down", lang).replace("_", df.format(prog*100))).queue();
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


