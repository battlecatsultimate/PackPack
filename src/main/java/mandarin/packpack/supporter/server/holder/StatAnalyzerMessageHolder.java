package mandarin.packpack.supporter.server.holder;

import common.io.assets.UpdateCheck;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.MaAnim;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.cell.AbilityData;
import mandarin.packpack.supporter.bc.cell.CellData;
import mandarin.packpack.supporter.bc.CustomMaskUnit;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.cell.FlagCellData;
import mandarin.packpack.supporter.lang.LangID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class StatAnalyzerMessageHolder extends MessageHolder<MessageCreateEvent> {
    private enum FILE {
        STAT(-1),
        LEVEL(-1),
        BUY(-1),
        ANIM(0),
        ICON(0);

        int index;

        FILE(int ind) {
            index = ind;
        }

        public void setIndex(int ind) {
            index = ind;
        }
    }

    private final List<CellData> cellData;
    private final List<AbilityData> procData;
    private final List<FlagCellData> abilityData;
    private final List<FlagCellData> traitData;
    private final int lang;
    private final Message msg;
    private final String channelID;
    private final File container;
    private final int uID;
    private final boolean isSecond;
    private final int lv;
    private final String[] name;

    private boolean statDone = false;
    private boolean levelDone = false;
    private boolean buyDone = false;
    private final boolean[] animDone;
    private final boolean[] iconDone;

    private final AtomicReference<String> stat;
    private final AtomicReference<String> level = new AtomicReference<>("LEVEL_CURVE (unitlevel.csv) : -");
    private final AtomicReference<String> buy = new AtomicReference<>("BUY (unitbuy.csv) : -");
    private final List<AtomicReference<String>> anim = new ArrayList<>();
    private final List<AtomicReference<String>> icon = new ArrayList<>();

    public StatAnalyzerMessageHolder(Message msg, Message author, int uID, int len, boolean isSecond, List<CellData> cellData, List<AbilityData> procData, List<FlagCellData> abilityData, List<FlagCellData> traitData, String channelID, File container, int lv, String[] name, int lang) throws Exception {
        super(MessageCreateEvent.class);

        this.cellData = cellData;
        this.abilityData = abilityData;
        this.procData = procData;
        this.traitData = traitData;
        this.lang = lang;
        this.msg = msg;
        this.channelID = channelID;
        this.container = container;
        this.uID = uID;
        this.isSecond = isSecond;
        this.lv = lv;
        this.name = name;

        animDone = new boolean[len];
        iconDone = new boolean[len];

        for(int i = 0; i < len; i++) {
            anim.add(new AtomicReference<>(getMaanimTitle(i) + "-"));
            icon.add(new AtomicReference<>(getIconTitle(i) + "-"));
        }

        stat = new AtomicReference<>("STAT (unit"+Data.trio(uID+1)+".csv) : -");

        AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

        if(!msg.getAttachments().isEmpty()) {
            for(Attachment a : msg.getAttachments()) {
                if(a.getFilename().equals("unit"+ Data.trio(uID+1)+".csv") && !statDone) {
                    downloadAndValidate("STAT (unit"+ Data.trio(uID+1)+".csv) : ", a, FILE.STAT, now);
                } else if(a.getFilename().equals("unitlevel.csv") && !levelDone) {
                    downloadAndValidate("LEVEL (unitlevel.csv) : ", a, FILE.LEVEL, now);
                } else if(a.getFilename().endsWith("02.maanim")) {
                    int index = getIndexOfMaanim(a.getFilename());

                    if(index != -1) {
                        FILE.ANIM.setIndex(index);

                        downloadAndValidate(getMaanimTitle(index), a, FILE.ANIM, now);
                    }
                } else if(a.getFilename().endsWith(".png") && getIndexOfIcon(a.getFilename()) != -1) {
                    int index = getIndexOfIcon(a.getFilename());

                    FILE.ICON.setIndex(index);

                    downloadAndValidate(getIconTitle(index), a, FILE.ICON, now);
                } else if(a.getFilename().equals("unitbuy.csv") && !buyDone) {
                    downloadAndValidate("BUY (unitbuy.csv) : ", a, FILE.BUY, now);
                }
            }
        }

        if(allDone()) {
            new Thread(() -> {
                Guild g = msg.getGuild().block();

                if(g == null) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            StaticStore.deleteFile(container, true);
                        }
                    }, 1000);

                    return;
                }

                try {
                    CustomMaskUnit[] units = new CustomMaskUnit[anim.size()];

                    File statFile = new File(container, "unit"+Data.trio(uID+1)+".csv");
                    File levelFile = new File(container, "unitlevel.csv");
                    File buyFile = new File(container, "unitbuy.csv");

                    if(!statFile.exists()) {
                        StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Stat file isn't existing even though code finished validation : "+statFile.getAbsolutePath());

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                StaticStore.deleteFile(container, true);
                            }
                        }, 1000);

                        return;
                    }

                    if(!levelFile.exists()) {
                        StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Level curve file isn't existing even though code finished validation : "+levelFile.getAbsolutePath());

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                StaticStore.deleteFile(container, true);
                            }
                        }, 1000);

                        return;
                    }

                    if(!buyFile.exists()) {
                        StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Unit buy file isn't existing even though code finished validation : "+buyFile.getAbsolutePath());

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                StaticStore.deleteFile(container, true);
                            }
                        }, 1000);

                        return;
                    }

                    BufferedReader statReader = new BufferedReader(new FileReader(statFile, StandardCharsets.UTF_8));
                    BufferedReader levelReader = new BufferedReader(new FileReader(levelFile, StandardCharsets.UTF_8));
                    BufferedReader buyReader = new BufferedReader(new FileReader(buyFile, StandardCharsets.UTF_8));

                    int count = 0;

                    while(count < uID) {
                        levelReader.readLine();
                        buyReader.readLine();
                        count++;
                    }

                    String[] curve = levelReader.readLine().split(",");
                    String[] rare = buyReader.readLine().split(",");

                    levelReader.close();
                    buyReader.close();

                    for(int i = 0; i < units.length; i++) {
                        File maanim = new File(container, getMaanimFileName(i));

                        if(!maanim.exists()) {
                            StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Maanim file isn't existing even though code finished validation : "+maanim.getAbsolutePath());
                            statReader.close();

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    StaticStore.deleteFile(container, true);
                                }
                            }, 1000);

                            return;
                        }

                        VFile vf = VFile.getFile(maanim);

                        if(vf == null) {
                            StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Failed to generate vFile of maanim : "+maanim.getAbsolutePath());
                            statReader.close();

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    StaticStore.deleteFile(container, true);
                                }
                            }, 1000);

                            return;
                        }

                        MaAnim ma = MaAnim.newIns(vf.getData());

                        units[i] = new CustomMaskUnit(statReader.readLine().split(","), curve, ma, rare);
                    }

                    statReader.close();

                    EntityHandler.generateStatImage(msg.getChannel().block(), cellData, procData, abilityData, traitData, units, name, container, lv, !isSecond, uID, lang);

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            StaticStore.deleteFile(container, true);
                        }
                    }, 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    StaticStore.logger.uploadErrorLog(e, "Failed to generate CustomMaksUnit data");

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            StaticStore.deleteFile(container, true);
                        }
                    }, 1000);
                }
            }).start();
        } else {
            author.getAuthor().ifPresent(u -> StaticStore.putHolder(u.getId().asString(), StatAnalyzerMessageHolder.this));

            registerAutoFinish(this, msg, author, lang, "animanalyze_expire", TimeUnit.MINUTES.toMillis(5));
        }
    }

    @Override
    public int handleEvent(MessageCreateEvent event) {
        try {
            if(expired) {
                StaticStore.logger.uploadLog("Expired Holder : "+this.getClass().getName());
                return RESULT_FAIL;
            }

            System.out.println("1");

            MessageChannel ch = event.getMessage().getChannel().block();

            if(ch == null)
                return RESULT_STILL;

            System.out.println("2");

            if(!ch.getId().asString().equals(channelID))
                return RESULT_STILL;

            System.out.println("3");

            AtomicReference<Long> now = new AtomicReference<>(System.currentTimeMillis());

            Message m = event.getMessage();

            if(!m.getAttachments().isEmpty()) {
                for(Attachment a : m.getAttachments()) {
                    if(a.getFilename().equals("unit"+Data.trio(uID+1)+".csv") && !statDone) {
                        downloadAndValidate("STAT (" + "unit" + Data.trio(uID+1) + ".csv" + ") : ", a, FILE.STAT, now);
                    } else if(a.getFilename().equals("unitlevel.csv") && !levelDone) {
                        downloadAndValidate("LEVEL (unitlevel.csv) : ", a, FILE.LEVEL, now);
                    } else if(a.getFilename().endsWith("02.maanim")) {
                        int index = getIndexOfMaanim(a.getFilename());

                        if(index != -1) {
                            FILE.ANIM.setIndex(index);

                            downloadAndValidate(getMaanimTitle(index), a, FILE.ANIM, now);
                        }
                    } else if(a.getFilename().endsWith(".png") && getIndexOfIcon(a.getFilename()) != -1) {
                        int index = getIndexOfIcon(a.getFilename());

                        FILE.ICON.setIndex(index);

                        downloadAndValidate(getIconTitle(index), a, FILE.ICON, now);
                    } else if(a.getFilename().equals("unitbuy.csv") && !buyDone) {
                        downloadAndValidate("BUY (unitbuy.csv) : ", a, FILE.BUY, now);
                    }
                }

                m.delete().subscribe();

                if(allDone()) {
                    new Thread(() -> {
                        Guild g = msg.getGuild().block();

                        if(g == null) {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    StaticStore.deleteFile(container, true);
                                }
                            }, 1000);

                            return;
                        }

                        try {
                            CustomMaskUnit[] units = new CustomMaskUnit[anim.size()];

                            File statFile = new File(container, "unit"+Data.trio(uID+1)+".csv");
                            File levelFile = new File(container, "unitlevel.csv");
                            File buyFile = new File(container, "unitbuy.csv");

                            if(!statFile.exists()) {
                                StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Stat file isn't existing even though code finished validation : "+statFile.getAbsolutePath());

                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        StaticStore.deleteFile(container, true);
                                    }
                                }, 1000);

                                return;
                            }

                            if(!levelFile.exists()) {
                                StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Level curve file isn't existing even though code finished validation : "+levelFile.getAbsolutePath());

                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        StaticStore.deleteFile(container, true);
                                    }
                                }, 1000);

                                return;
                            }

                            if(!buyFile.exists()) {
                                StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Unit buy file isn't existing even though code finished validation : "+buyFile.getAbsolutePath());

                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        StaticStore.deleteFile(container, true);
                                    }
                                }, 1000);

                                return;
                            }

                            BufferedReader statReader = new BufferedReader(new FileReader(statFile, StandardCharsets.UTF_8));
                            BufferedReader levelReader = new BufferedReader(new FileReader(levelFile, StandardCharsets.UTF_8));
                            BufferedReader buyReader = new BufferedReader(new FileReader(buyFile, StandardCharsets.UTF_8));

                            int count = 0;

                            while(count < uID) {
                                levelReader.readLine();
                                buyReader.readLine();
                                count++;
                            }

                            String[] curve = levelReader.readLine().split(",");
                            String[] rare = buyReader.readLine().split(",");

                            levelReader.close();
                            buyReader.close();

                            for(int i = 0; i < units.length; i++) {
                                File maanim = new File(container, getMaanimFileName(i));

                                if(!maanim.exists()) {
                                    StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Maanim file isn't existing even though code finished validation : "+maanim.getAbsolutePath());
                                    statReader.close();

                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            StaticStore.deleteFile(container, true);
                                        }
                                    }, 1000);

                                    return;
                                }

                                VFile vf = VFile.getFile(maanim);

                                if(vf == null) {
                                    StaticStore.logger.uploadLog("E/StatAnalyzerMessageHolder | Failed to generate vFile of maanim : "+maanim.getAbsolutePath());
                                    statReader.close();

                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            StaticStore.deleteFile(container, true);
                                        }
                                    }, 1000);

                                    return;
                                }

                                MaAnim ma = MaAnim.newIns(vf.getData());

                                units[i] = new CustomMaskUnit(statReader.readLine().split(","), curve, ma, rare);
                            }

                            statReader.close();

                            EntityHandler.generateStatImage(msg.getChannel().block(), cellData, procData, abilityData, traitData, units, name, container, lv, !isSecond, uID, lang);

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    StaticStore.deleteFile(container, true);
                                }
                            }, 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                            StaticStore.logger.uploadErrorLog(e, "Failed to generate CustomMaksUnit data");

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    StaticStore.deleteFile(container, true);
                                }
                            }, 1000);
                        }
                    }).start();
                }

            } else if(m.getContent().equals("c")) {
                Command.editMessage(msg, me -> me.content(wrap(LangID.getStringByID("animanalyze_cancel", lang))));

                StaticStore.deleteFile(container, true);

                return RESULT_FINISH;
            }
        } catch (Exception e) {
            e.printStackTrace();
            StaticStore.logger.uploadErrorLog(e, "Failed to perform StatAnalyzerHolder");
        }

        return RESULT_STILL;
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

    private boolean allDone() {
        for(int i = 0; i < animDone.length; i++) {
            if(!animDone[i] || !iconDone[i])
                return false;
        }

        return statDone && levelDone && buyDone;
    }

    private void downloadAndValidate(String prefix, Attachment attachment, FILE type, AtomicReference<Long> now) throws Exception {
        UpdateCheck.Downloader down = StaticStore.getDownloader(attachment, container);

        if(down != null) {
            AtomicReference<String> reference = getReference(type);

            down.run(d -> {
                String p = prefix;

                if(d == 1.0) {
                    p += "VALIDATING...";
                } else {
                    p += DataToString.df.format(d * 100.0) + "%";
                }

                reference.set(p);

                if(System.currentTimeMillis() - now.get() >= 1500) {
                    now.set(System.currentTimeMillis());

                    edit();
                }
            });

            File res = new File(container, attachment.getFilename());

            if(res.exists()) {
                if(validFile(type, res)) {
                    reference.set(prefix+"SUCCESS");

                    switch (type) {
                        case STAT:
                            statDone = true;
                            break;
                        case LEVEL:
                            levelDone = true;
                            break;
                        case BUY:
                            buyDone = true;
                            break;
                        case ANIM:
                            animDone[FILE.ANIM.index] = true;
                            break;
                        case ICON:
                            iconDone[FILE.ICON.index] = true;
                            break;
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

    private AtomicReference<String> getReference(FILE type) {
        switch (type) {
            case STAT:
                return stat;
            case LEVEL:
                return level;
            case BUY:
                return buy;
            case ANIM:
                return anim.get(type.index);
            case ICON:
                return icon.get(type.index);
        }

        throw new IllegalStateException("Invalid file type : "+type);
    }

    private boolean validFile(FILE type, File file) throws Exception {
        switch (type) {
            case STAT:
                if(!file.getName().equals("unit"+Data.trio(uID+1)+".csv"))
                    return false;

                BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));

                int count = 0;

                while(reader.readLine() != null) {
                    count++;
                }

                reader.close();

                return count >= 3 && count < 5;
            case LEVEL:
                if(!file.getName().equals("unitlevel.csv"))
                    return false;

                reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));

                count = 0;
                String line;

                while((line = reader.readLine()) != null) {
                    count++;

                    if(count == uID && !line.isBlank())
                        return true;
                }

                reader.close();

                return false;
            case BUY:
                if(!file.getName().equals("unitbuy.csv"))
                    return false;

                reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));

                count = 0;

                while((line = reader.readLine()) != null) {
                    count++;

                    if(count == uID && !line.isBlank())
                        return true;
                }

                reader.close();

                return false;
            case ANIM:
                return AnimMixer.validMaanim(file);
            case ICON:
                return AnimMixer.validPng(file);
        }

        return false;
    }

    private int getIndexOfMaanim(String fileName) {
        if(fileName.endsWith("f02.maanim"))
            return 0;
        else if(fileName.endsWith("c02.maanim"))
            return 1;
        else if(fileName.endsWith("s02.maanim"))
            return 2;
        else
            return -1;
    }

    private int getIndexOfIcon(String fileName) {
        if(fileName.endsWith("f00.png"))
            return 0;
        else if(fileName.endsWith("c00.png"))
            return 1;
        else if(fileName.endsWith("s00.png"))
            return 2;
        else
            return -1;
    }

    private String getMaanimTitle(int ind) {
        switch (ind) {
            case 0:
                return "MAANIM F ATK : ";
            case 1:
                return "MAANIM C ATK : ";
            case 2:
                return "MAANIM S ATK : ";
            default:
                return "MAANIM " + ind + " ATK : ";
        }
    }

    private String getIconTitle(int ind) {
        switch (ind) {
            case 0:
                return "ICON F : ";
            case 1:
                return "ICON C : ";
            case 2:
                return "ICON S : ";
            default:
                return "ICON " + ind + " : ";
        }
    }

    private String getMaanimFileName(int ind) {
        switch (ind) {
            case 0:
                return Data.trio(uID)+"_f02.maanim";
            case 1:
                return Data.trio(uID)+"_c02.maanim";
            case 2:
                return Data.trio(uID)+"_s02.maanim";
            default:
                return Data.trio(uID)+"_"+ind+"02.maanim";
        }
    }

    private void edit() {
        StringBuilder content = new StringBuilder(stat.get()+"\n"+level.get()+"\n"+buy.get()+"\n");

        for(int i = 0; i < anim.size(); i++) {
            content.append(anim.get(i).get()).append("\n");
        }

        for(int i = 0; i < icon.size(); i++) {
            content.append(icon.get(i).get());

            if(i < icon.size() - 1)
                content.append("\n");
        }

        Command.editMessage(msg, m -> m.content(wrap(content.toString())));
    }
}
