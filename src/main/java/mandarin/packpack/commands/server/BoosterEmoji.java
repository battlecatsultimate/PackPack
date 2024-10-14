package mandarin.packpack.commands.server;

import common.CommonStatic;
import common.io.assets.UpdateCheck;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import javax.annotation.Nonnull;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class BoosterEmoji extends ConstraintCommand {
    public BoosterEmoji(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        if(holder == null)
            return;

        File temp = new File("./temp");

        if(!temp.exists() && temp.mkdirs()) {
            System.out.println("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        Guild g = loader.getGuild();
        MessageChannel ch = loader.getChannel();
        Message me = loader.getMessage();

        if(holder.booster == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boosterRole.failed.noRegisteredRole", lang));
            return;
        }

        String id = getID(loader.getContent());

        if(id == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boosterRole.failed.noMember", lang));
            return;
        }

        String name = getEmojiName(loader.getContent());

        if(name == null || name.isBlank()) {
            createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.noName", lang));
            return;
        }

        if(me.getAttachments().isEmpty()) {
            createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.noFile", lang));
            return;
        }

        Member m = g.getMemberById(id);

        if(m != null) {
            try {
                if(!StaticStore.rolesToString(m.getRoles()).contains(holder.booster)) {
                    createMessageWithNoPings(ch, LangID.getStringByID("boosterRole.failed.notBooster", lang).replace("_RRR_", holder.booster));
                    return;
                }

                if(StaticStore.boosterData.containsKey(g.getId())) {
                    BoosterHolder bHolder = StaticStore.boosterData.get(g.getId());

                    if(bHolder.serverBooster.containsKey(m.getId())) {
                        BoosterData data = bHolder.serverBooster.get(m.getId());

                        if(data.getRole() != null) {
                            createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.alreadyAssigned", lang));
                            return;
                        }
                    }
                }

                boolean gif = false;

                Message.Attachment selectedAttachment = null;

                for(Message.Attachment att : me.getAttachments()) {
                    if(att.getFileName().endsWith(".png") || att.getFileName().endsWith(".gif")) {
                        selectedAttachment = att;

                        if(att.getFileName().endsWith(".gif"))
                            gif = true;

                        if(att.getSize() >= 256 * 1024) {
                            createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.tooBig", lang));
                            return;
                        }

                        break;
                    }
                }

                if(selectedAttachment == null) {
                    createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.noFile", lang));

                    return;
                }

                final boolean finalGif = gif;
                final Message.Attachment att = selectedAttachment;

                ch.sendMessage(LangID.getStringByID("boosterEmoji.downloading", lang))
                        .setAllowedMentions(new ArrayList<>())
                        .queue(mes -> {
                            try {
                                if(mes == null)
                                    return;

                                File tempo = StaticStore.generateTempFile(temp, StaticStore.extractFileName(att.getFileName()), ".png.tmp", false);

                                if(tempo == null)
                                    return;

                                String url = att.getUrl();

                                File target = new File(temp, tempo.getName().replace(".tmp", ""));

                                UpdateCheck.Downloader down = new UpdateCheck.Downloader(target, tempo, "", false, url);

                                AtomicReference<Long> currentTime = new AtomicReference<>(System.currentTimeMillis());

                                down.run(p -> {
                                    long current = System.currentTimeMillis();

                                    if(current - currentTime.get() > 1500) {
                                        currentTime.set(current);

                                        mes.editMessage(LangID.getStringByID("boosterEmoji.downloading", lang).replace("-", DataToString.df.format(p * 100))).queue();
                                    }
                                });

                                mes.delete().queue();

                                if(target.exists() && att.getFileName().endsWith(".png") && !AnimMixer.validPng(target)) {
                                    createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.invalidImage", lang));

                                    if(target.exists() && !target.delete()) {
                                        StaticStore.logger.uploadLog("Failed to delete file : "+target.getAbsolutePath());
                                    }

                                    return;
                                } else if(!target.exists()) {
                                    createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.downloadFail", lang));

                                    return;
                                }

                                g.createEmoji(name, Icon.from(target)).queue(e -> {
                                    if(StaticStore.boosterData.containsKey(g.getId())) {
                                        BoosterHolder bHolder = StaticStore.boosterData.get(g.getId());

                                        if(bHolder.serverBooster.containsKey(m.getId())) {
                                            BoosterData data = bHolder.serverBooster.get(m.getId());

                                            int result = data.setEmoji(e.getId());

                                            if(result == BoosterData.ERR_ALREADY_EMOJI_SET) {
                                                createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.alreadyAssigned", lang));
                                            } else {
                                                createMessageWithNoPings(ch, LangID.getStringByID(finalGif ? "boosterEmoji.success.animated" : "boosterEmoji.success.default", lang).replace("_III_", e.getId()).replace("_MMM_", m.getId()).replace("_EEE_", name));
                                            }
                                        } else {
                                            BoosterData data = new BoosterData(e.getId(), BoosterData.INITIAL.EMOJI);

                                            bHolder.serverBooster.put(m.getId(), data);

                                            createMessageWithNoPings(ch, LangID.getStringByID(finalGif ? "boosterEmoji.success.animated" : "boosterEmoji.success.default", lang).replace("_III_", e.getId()).replace("_MMM_", m.getId()).replace("_EEE_", name));
                                        }
                                    } else {
                                        BoosterHolder bHolder = new BoosterHolder();

                                        BoosterData data = new BoosterData(e.getId(), BoosterData.INITIAL.EMOJI);

                                        bHolder.serverBooster.put(m.getId(), data);

                                        StaticStore.boosterData.put(g.getId(), bHolder);

                                        createMessageWithNoPings(ch, LangID.getStringByID(finalGif ? "boosterEmoji.success.animated" : "boosterEmoji.success.default", lang).replace("_III_", e.getId()).replace("_MMM_", m.getId()).replace("_EEE_", name));
                                    }
                                }, err -> {
                                    createMessageWithNoPings(ch, LangID.getStringByID("boosterEmoji.failed.unknown", lang));

                                    StaticStore.logger.uploadErrorLog(err, "E/BoosterEmoji::doSomething - Failed to create emote");
                                });
                            } catch (Exception e) {
                                StaticStore.logger.uploadErrorLog(e, "E/BoosterEmoji::doSomething - Failed to create emote");
                            }
                        });
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/BoosterEmoji::doSomething - Failed to handle booster emoji command");
            }
        }
    }

    private String getID(String message) {
        String[] content = message.split(" ");

        if(content.length < 2)
            return null;

        String id = content[1].replaceAll("<@!?", "").replace(">", "");

        if(StaticStore.isNumeric(id))
            return id;
        else
            return null;
    }

    private String getEmojiName(String message) {
        String[] content = message.split(" ", 3);

        if(content.length < 3)
            return null;

        if(content[2].isBlank())
            return null;

        return content[2].strip().replace(" ", "");
    }
}
