package mandarin.packpack.supporter.server.holder;

import common.io.assets.UpdateCheck;
import common.system.files.VFile;
import common.util.anim.ImgCut;
import common.util.anim.MaAnim;
import common.util.anim.MaModel;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BCAnimMessageHolder extends MessageHolder<MessageCreateEvent> {
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
    private final Message msg;
    private final int lang;
    private final String channelID;
    private final File container;

    private boolean pngDone = false;
    private boolean cutDone = false;
    private boolean modelDone = false;

    private final AtomicReference<String> png = new AtomicReference<>("PNG : -");
    private final AtomicReference<String> mamodel = new AtomicReference<>("MAMODEL : -");
    private final AtomicReference<String> imgcut = new AtomicReference<>("IMGCUT : -");
    private final ArrayList<AtomicReference<String>> maanim = new ArrayList<>();

    public BCAnimMessageHolder(Message msg, Message target, int lang, String channelID, File container, MessageChannel ch, boolean zombie) throws Exception {
        super(MessageCreateEvent.class);

        this.msg = target;
        this.lang = lang;
        this.channelID = channelID;
        this.container = container;

        int len = zombie ? 7 : 4;

        mixer = new AnimMixer(len);

        for(int i  = 0; i < len; i++) {
            maanim.add(new AtomicReference<>(getMaanimTitle(i) + "-"));
        }

        AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

        if(!msg.getAttachments().isEmpty()) {
            for(Attachment a : msg.getAttachments()) {
                if(a.getFilename().endsWith(".png") && mixer.png == null) {
                    downloadAndValidate("PNG : ", a, FILE.PNG, now);
                } else if(a.getFilename().endsWith(".imgcut") && mixer.imgCut == null) {
                    downloadAndValidate("IMGCUT : ", a, FILE.IMGCUT, now);
                } else if(a.getFilename().endsWith(".mamodel") && mixer.model == null) {
                    downloadAndValidate("MAMODEL : ", a, FILE.MAMODEL, now);
                } else if(a.getFilename().endsWith(".maanim")) {
                    int index = getIndexFromFileName(a.getFilename());

                    if(index != -1) {
                        FILE.ANIM.setIndex(index);

                        downloadAndValidate(getMaanimTitle(index), a, FILE.ANIM, now);
                    }
                }
            }
        }

        if(pngDone && cutDone && modelDone && animAllDone()) {
            new Thread(() -> {
                Guild g = msg.getGuild().block();

                if(g == null)
                    return;

                try {
                    boolean result = EntityHandler.generateBCAnim(ch, g.getPremiumTier().getValue(), mixer, lang);

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            StaticStore.deleteFile(container, true);
                        }
                    }, 1000);

                    if(result) {
                        StaticStore.canDo.put("gif", new TimeBoolean(false, TimeUnit.MINUTES.toMillis(1)));

                        Timer timer = new Timer();

                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("Remove Process : gif");
                                StaticStore.canDo.put("gif", new TimeBoolean(true));
                            }
                        }, TimeUnit.MINUTES.toMillis(1));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        StaticStore.deleteFile(container, true);
                    }
                }, 1000);
            }).start();
        } else {
            msg.getAuthor().ifPresent(u -> StaticStore.putHolder(u.getId().asString(), this));

            registerAutoFinish(this, target, msg, lang, "animanalyze_expire", TimeUnit.MINUTES.toMillis(5));
        }
    }

    @Override
    public int handleEvent(MessageCreateEvent event) {
        try {
            if(expired) {
                System.out.println("Expired!!");
                return RESULT_FAIL;
            }

            MessageChannel ch = event.getMessage().getChannel().block();

            if(ch == null)
                return RESULT_STILL;

            if(!ch.getId().asString().equals(channelID))
                return RESULT_STILL;

            AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

            Message m = event.getMessage();

            if(!m.getAttachments().isEmpty()) {
                for(Attachment a : m.getAttachments()) {
                    if(a.getFilename().endsWith(".png") && mixer.png == null) {
                        System.out.println("PNG");
                        downloadAndValidate("PNG : ", a, FILE.PNG, now);
                    } else if(a.getFilename().endsWith(".imgcut") && mixer.imgCut == null) {
                        downloadAndValidate("IMGCUT : ", a, FILE.IMGCUT, now);
                    } else if(a.getFilename().endsWith(".mamodel") && mixer.model == null) {
                        downloadAndValidate("MAMODEL : ", a, FILE.MAMODEL, now);
                    } else if(a.getFilename().endsWith(".maanim")) {
                        int index = getIndexFromFileName(a.getFilename());

                        if(index != -1) {
                            FILE.ANIM.setIndex(index);

                            downloadAndValidate(getMaanimTitle(index), a, FILE.ANIM, now);
                        }
                    }
                }

                m.delete().subscribe();

                if(pngDone && cutDone && modelDone && animAllDone()) {
                    new Thread(() -> {
                        Guild g = event.getGuild().block();

                        if(g == null)
                            return;

                        try {
                            boolean result = EntityHandler.generateBCAnim(ch, g.getPremiumTier().getValue(), mixer, lang);

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    StaticStore.deleteFile(container, true);
                                }
                            }, 1000);

                            if(result) {
                                StaticStore.canDo.put("gif", new TimeBoolean(false, TimeUnit.MINUTES.toMillis(1)));

                                Timer timer = new Timer();

                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        System.out.println("Remove Process : gif");
                                        StaticStore.canDo.put("gif", new TimeBoolean(true));
                                    }
                                }, TimeUnit.MINUTES.toMillis(1));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();

                    return RESULT_FINISH;
                }
            } else if(m.getContent().equals("c")) {
                Command.editMessage(msg, mes -> mes.content(wrap(LangID.getStringByID("animanalyze_cancel", lang))));

                StaticStore.deleteFile(container, true);

                return RESULT_FINISH;
            }

            return RESULT_STILL;
        } catch (Exception e) {
            e.printStackTrace();
            return RESULT_STILL;
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID("formst_expire", lang))));
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
        switch (index) {
            case 0:
                return "MAANIM WALKING : ";
            case 1:
                return "MAANIM IDLE : ";
            case 2:
                return "MAANIM ATTACK : ";
            case 3:
                return "MAANIM HITBACK : ";
            case 4:
                return "MAANIM BURROW DOWN : ";
            case 5:
                return "MAANIM BURROW MOVE : ";
            case 6:
                return "MAANIM BURROW UP : ";
            default:
                return "MAANIM "+index+" : ";
        }
    }

    private void edit() {
        StringBuilder content = new StringBuilder(png.get()+"\n"+imgcut.get()+"\n"+mamodel.get()+"\n");

        for(int i = 0; i < maanim.size(); i++) {
            content.append(maanim.get(i).get());

            if(i < maanim.size() - 1) {
                content.append("\n");
            }
        }

        Command.editMessage(msg, m -> m.content(wrap(content.toString())));
    }

    private boolean validFile(FILE fileType, File file) throws Exception {
        switch (fileType) {
            case PNG:
                return AnimMixer.validPng(file);
            case IMGCUT:
                return mixer.validImgCut(file);
            case MAMODEL:
                return mixer.validMamodel(file);
            default:
                return AnimMixer.validMaanim(file);
        }
    }

    private AtomicReference<String> getReference(FILE fileType) {
        switch (fileType) {
            case PNG:
                return png;
            case IMGCUT:
                return imgcut;
            case MAMODEL:
                return mamodel;
            default:
                return maanim.get(fileType.index);
        }
    }

    private void downloadAndValidate(String prefix, Attachment attachment, FILE fileType, AtomicReference<Long> now) throws Exception {
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

            File res = new File(container, attachment.getFilename());

            if(res.exists()) {
                if(validFile(fileType, res)) {
                    reference.set(prefix+"SUCCESS");

                    switch (fileType) {
                        case PNG:
                            pngDone = true;

                            mixer.png = ImageIO.read(res);
                            break;
                        case IMGCUT:
                            cutDone = true;

                            VFile vf = VFile.getFile(res);

                            if(vf != null) {
                                mixer.imgCut = ImgCut.newIns(vf.getData());
                            }
                            break;
                        case MAMODEL:
                            modelDone = true;

                            vf = VFile.getFile(res);

                            if(vf != null) {
                                mixer.model = MaModel.newIns(vf.getData());
                            }
                            break;
                        default:
                            vf = VFile.getFile(res);

                            if(vf != null) {
                                mixer.anim[fileType.index] = MaAnim.newIns(vf.getData());
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
