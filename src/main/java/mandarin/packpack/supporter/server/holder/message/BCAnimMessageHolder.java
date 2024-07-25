package mandarin.packpack.supporter.server.holder.message;

import common.CommonStatic;
import common.io.assets.UpdateCheck;
import common.system.files.VFile;
import common.util.anim.ImgCut;
import common.util.anim.MaAnim;
import common.util.anim.MaModel;
import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BCAnimMessageHolder extends MessageHolder {
    private enum FILE {
        PNG(-1),
        IMGCUT(-1),
        MAMODEL(-1),
        ANIM(0);

        int index;

        FILE(int ind) {
            index = ind;
        }

        public void setIndex(int ind) {
            index = ind;
        }
    }

    private final AnimMixer mixer;
    private final boolean performance;
    private final File container;

    private boolean pngDone = false;
    private boolean cutDone = false;
    private boolean modelDone = false;

    private final AtomicReference<String> png = new AtomicReference<>("PNG : -");
    private final AtomicReference<String> mamodel = new AtomicReference<>("MAMODEL : -");
    private final AtomicReference<String> imgcut = new AtomicReference<>("IMGCUT : -");
    private final ArrayList<AtomicReference<String>> maanim = new ArrayList<>();

    public BCAnimMessageHolder(@Nonnull Message author, @Nonnull Message target, boolean performance, CommonStatic.Lang.Locale lang, @Nonnull String channelID, File container, MessageChannel ch, boolean zombie) throws Exception {
        super(author, channelID, target, lang);
        
        this.performance = performance;
        this.container = container;

        int len = zombie ? 7 : 4;

        mixer = new AnimMixer(len);

        for(int i  = 0; i < len; i++) {
            maanim.add(new AtomicReference<>(getMaanimTitle(i) + "-"));
        }

        AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

        if(!author.getAttachments().isEmpty()) {
            for(Message.Attachment a : author.getAttachments()) {
                if(a.getFileName().endsWith(".png") && mixer.png == null) {
                    downloadAndValidate("PNG : ", a, FILE.PNG, now);
                } else if(a.getFileName().endsWith(".imgcut") && mixer.imgCut == null) {
                    downloadAndValidate("IMGCUT : ", a, FILE.IMGCUT, now);
                } else if(a.getFileName().endsWith(".mamodel") && mixer.model == null) {
                    downloadAndValidate("MAMODEL : ", a, FILE.MAMODEL, now);
                } else if(a.getFileName().endsWith(".maanim")) {
                    int index = getIndexFromFileName(a.getFileName());

                    if(index != -1) {
                        FILE.ANIM.setIndex(index);

                        downloadAndValidate(getMaanimTitle(index), a, FILE.ANIM, now);
                    }
                }
            }
        }

        if(pngDone && cutDone && modelDone && animAllDone()) {
            RecordableThread t = new RecordableThread(() -> {
                Guild g;

                if(ch instanceof GuildChannel) {
                    g = author.getGuild();
                } else {
                    g = null;
                }

                EntityHandler.generateBCAnim(ch, g == null ? 0 : g.getBoostTier().getKey(), mixer, performance, lang, () -> {
                    StaticStore.canDo.put("gif", new TimeBoolean(false, TimeUnit.MINUTES.toMillis(1)));

                    StaticStore.executorHandler.postDelayed(60000, () -> {
                        System.out.println("Remove Process : gif");

                        StaticStore.canDo.put("gif", new TimeBoolean(true));
                    });

                    StaticStore.executorHandler.postDelayed(1000, () -> StaticStore.deleteFile(container, true));
                }, () ->
                    StaticStore.executorHandler.postDelayed(1000, () -> StaticStore.deleteFile(container, true))
                );

                StaticStore.executorHandler.postDelayed(1000, () -> StaticStore.deleteFile(container, true));
            }, e -> StaticStore.logger.uploadErrorLog(e, "E/BCAnimMessageHolder::constructor - Failed to generate animation"));

            t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + getAuthorMessage().getContentRaw());
            t.start();
        } else {
            StaticStore.putHolder(author.getAuthor().getId(), this);

            registerAutoFinish(this, target, "animationAnalyze.expired", TimeUnit.MINUTES.toMillis(5));
        }
    }

    @Override
    public STATUS onReceivedEvent(MessageReceivedEvent event) {
        try {
            if(expired) {
                System.out.println("Expired!!");
                return STATUS.FAIL;
            }

            MessageChannel ch = event.getMessage().getChannel();

            if(!ch.getId().equals(channelID))
                return STATUS.WAIT;

            AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

            Message m = event.getMessage();

            if(!m.getAttachments().isEmpty()) {
                for(Message.Attachment a : m.getAttachments()) {
                    if(a.getFileName().endsWith(".png") && mixer.png == null) {
                        downloadAndValidate("PNG : ", a, FILE.PNG, now);
                    } else if(a.getFileName().endsWith(".imgcut") && mixer.imgCut == null) {
                        downloadAndValidate("IMGCUT : ", a, FILE.IMGCUT, now);
                    } else if(a.getFileName().endsWith(".mamodel") && mixer.model == null) {
                        downloadAndValidate("MAMODEL : ", a, FILE.MAMODEL, now);
                    } else if(a.getFileName().endsWith(".maanim")) {
                        int index = getIndexFromFileName(a.getFileName());

                        if(index != -1) {
                            FILE.ANIM.setIndex(index);

                            downloadAndValidate(getMaanimTitle(index), a, FILE.ANIM, now);
                        }
                    }
                }

                m.delete().queue();

                if(pngDone && cutDone && modelDone && animAllDone()) {
                    RecordableThread t = new RecordableThread(() -> {
                        Guild g;

                        if(ch instanceof GuildChannel) {
                            g = event.getGuild();
                        } else {
                            g = null;
                        }

                        EntityHandler.generateBCAnim(ch, g == null ? 0 : g.getBoostTier().getKey(), mixer, performance, lang, () -> {
                            StaticStore.canDo.put("gif", new TimeBoolean(false, TimeUnit.MINUTES.toMillis(1)));

                            StaticStore.executorHandler.postDelayed(60000, () -> {
                                System.out.println("Remove Process : gif");

                                StaticStore.canDo.put("gif", new TimeBoolean(true));
                            });

                            StaticStore.executorHandler.postDelayed(1000, () -> StaticStore.deleteFile(container, true));
                        }, () ->
                            StaticStore.executorHandler.postDelayed(1000, () -> StaticStore.deleteFile(container, true))
                        );
                    }, e -> StaticStore.logger.uploadErrorLog(e, "E/BCAnimMessageHolder::onReceivedEvent - Failed to generate animation"));

                    t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + getAuthorMessage().getContentRaw());
                    t.start();

                    return STATUS.FINISH;
                }
            } else if(m.getContentRaw().equals("c")) {
                message.editMessage(LangID.getStringByID("animationAnalyze.canceled", lang)).queue();

                StaticStore.deleteFile(container, true);

                return STATUS.FINISH;
            }

            return STATUS.WAIT;
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/BCAnimMessageHolder::onReceivedEvent - Failed to handle BC animation generator");

            return STATUS.WAIT;
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        message.editMessage(LangID.getStringByID("ui.search.expired", lang))
                .mentionRepliedUser(false)
                .queue();
    }

    private int getIndexFromFileName(String fileName) {
        if(fileName.endsWith("zombie00.maanim")) {
            return 4;
        } else if(fileName.endsWith("zombie01.maanim")) {
            return 5;
        } else if(fileName.endsWith("zombie02.maanim")) {
            return 6;
        } else if (fileName.endsWith("00.maanim")) {
            return 0;
        } else if(fileName.endsWith("01.maanim")) {
            return 1;
        } else if(fileName.endsWith("02.maanim")) {
            return 2;
        } else if(fileName.endsWith("03.maanim")){
            return 3;
        }

        return -1;
    }

    private String getMaanimTitle(int index) {
        return switch (index) {
            case 0 -> "MAANIM WALKING : ";
            case 1 -> "MAANIM IDLE : ";
            case 2 -> "MAANIM ATTACK : ";
            case 3 -> "MAANIM HITBACK : ";
            case 4 -> "MAANIM BURROW DOWN : ";
            case 5 -> "MAANIM BURROW MOVE : ";
            case 6 -> "MAANIM BURROW UP : ";
            default -> "MAANIM " + index + " : ";
        };
    }

    private void edit() {
        StringBuilder content = new StringBuilder(png.get()+"\n"+imgcut.get()+"\n"+mamodel.get()+"\n");

        for(int i = 0; i < maanim.size(); i++) {
            content.append(maanim.get(i).get());

            if(i < maanim.size() - 1) {
                content.append("\n");
            }
        }

        message.editMessage(content.toString()).queue();
    }

    private boolean validFile(FILE fileType, File file) throws Exception {
        return switch (fileType) {
            case PNG -> AnimMixer.validPng(file);
            case IMGCUT -> mixer.validImgCut(file);
            case MAMODEL -> mixer.validMamodel(file);
            default -> AnimMixer.validMaanim(file);
        };
    }

    private AtomicReference<String> getReference(FILE fileType) {
        return switch (fileType) {
            case PNG -> png;
            case IMGCUT -> imgcut;
            case MAMODEL -> mamodel;
            default -> maanim.get(fileType.index);
        };
    }

    private void downloadAndValidate(String prefix, Message.Attachment attachment, FILE fileType, AtomicReference<Long> now) throws Exception {
        UpdateCheck.Downloader down = StaticStore.getDownloader(attachment, container);

        if(down != null) {
            AtomicReference<String> reference = getReference(fileType);

            down.run(d -> {
                String p = prefix;

                if(d == 1.0) {
                    p += "VALIDATING...";
                } else {
                    p += DataToString.df.format(d * 100.0) +"%";
                }

                reference.set(p);

                if(System.currentTimeMillis() - now.get() >= 1500) {
                    now.set(System.currentTimeMillis());

                    edit();
                }
            });

            File res = new File(container, attachment.getFileName());

            if(res.exists()) {
                if(validFile(fileType, res)) {
                    reference.set(prefix+"SUCCESS");

                    switch (fileType) {
                        case PNG -> {
                            pngDone = true;
                            mixer.buildPng(res);
                        }
                        case IMGCUT -> {
                            cutDone = true;
                            VFile cutFile = VFile.getFile(res);
                            if (cutFile != null) {
                                mixer.imgCut = ImgCut.newIns(cutFile.getData());
                            }
                        }
                        case MAMODEL -> {
                            modelDone = true;
                            VFile modelFile = VFile.getFile(res);
                            if (modelFile != null) {
                                mixer.model = MaModel.newIns(modelFile.getData());
                            }
                        }
                        default -> {
                            VFile animationFile = VFile.getFile(res);
                            if (animationFile != null) {
                                mixer.anim[fileType.index] = MaAnim.newIns(animationFile.getData(), false);
                            }
                        }
                    }
                } else {
                    reference.set(prefix+"INVALID");
                }
            } else {
                reference.set(prefix+"DOWN FAILED");
            }

            edit();
        }
    }

    private boolean animAllDone() {
        for(int i = 0; i < mixer.anim.length; i++) {
            if(mixer.anim[i] == null)
                return false;
        }

        return true;
    }
}
