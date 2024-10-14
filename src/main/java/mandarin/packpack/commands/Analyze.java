package mandarin.packpack.commands;

import common.CommonStatic;
import common.io.assets.UpdateCheck;
import common.pack.PackData;
import common.pack.UserProfile;
import common.util.stage.MapColc;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Analyze extends ConstraintCommand {
    public static final long INTERVAL = 1000;
    public static long NOW  = System.currentTimeMillis();

    private final DecimalFormat df = new DecimalFormat("#.##");

    public Analyze(ROLE role, CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        try {
            Message msg = loader.getMessage();
            MessageChannel ch = loader.getChannel();

            if(!msg.getAttachments().isEmpty()) {
                List<Message.Attachment> attachments = new ArrayList<>(msg.getAttachments());

                Message.Attachment at = attachments.getFirst();

                if(at.getFileName().endsWith("pack.bcuzip")) {
                    ch.sendMessage(LangID.getStringByID("analyze.downloading", lang).replace("_", String.valueOf(0))).queue(message -> {
                        try {
                            String url = at.getUrl();

                            File target = StaticStore.getDownPackFile();
                            File temp = StaticStore.getTempPackFile();

                            UpdateCheck.Downloader down = new UpdateCheck.Downloader(target, temp, "", false, url);

                            down.run(progression -> editMessage(msg, progression));

                            message.editMessage(LangID.getStringByID("analyze.analyzing", lang)).queue();

                            PackData.UserPack pack = UserProfile.readZipPack(target);

                            if(!canAdd(pack)) {
                                message.editMessage(LangID.getStringByID("analyze.failed", lang)).queue();

                                return;
                            }

                            UserProfile.profile().pending.put(pack.desc.id, pack);

                            pack.load();

                            UserProfile.profile().packmap.put(pack.desc.id, pack);

                            message.delete().queue();

                            EmbedBuilder builder = new EmbedBuilder();

                            builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                                    .setTitle(pack.desc.name)
                                    .addField("ID", pack.desc.id, false)
                                    .addField(LangID.getStringByID("analyze.embed.unit", lang), Integer.toString(pack.units.getList().size()), true)
                                    .addField(LangID.getStringByID("analyze.embed.enemy", lang), Integer.toString(pack.enemies.getList().size()), true)
                                    .addField(LangID.getStringByID("analyze.embed.stage", lang), Integer.toString(MapColc.get(pack.desc.id).maps.size()), true)
                                    .addField(LangID.getStringByID("analyze.embed.background", lang), Integer.toString(pack.bgs.getList().size()), true)
                                    .addField(LangID.getStringByID("analyze.embed.music", lang), Integer.toString(pack.musics.getList().size()), true)
                                    .setDescription(at.getFileName()+" ("+getFileSize(target.length())+")");

                            ch.sendMessageEmbeds(builder.build()).queue();

                            UserProfile.unloadAllUserPacks();

                            if(target.exists()) {
                                target.delete();
                            }

                            if(temp.exists()) {
                                temp.delete();
                            }
                        } catch (Exception e) {
                            StaticStore.logger.uploadErrorLog(e, "E/Analyze::doSomething - Failed to analyze pack");
                        }
                    });
                } else {
                    ch.sendMessage(LangID.getStringByID("analyze.fail.incorrectFile", lang)).queue();
                }
            } else {
                ch.sendMessage(LangID.getStringByID("analyze.fail.noFile", lang)).queue();
            }
        } catch (Exception e) {
            onFail(loader, DEFAULT_ERROR);
        }
    }

    private void editMessage(Message msg, double prog) {
        if(System.currentTimeMillis() - NOW < INTERVAL)
            return;

        NOW = System.currentTimeMillis();

        if(msg != null) {
            msg.editMessage(LangID.getStringByID("analyze.downloading", lang).replace("_", df.format(prog*100))).queue();
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


