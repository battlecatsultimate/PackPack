package mandarin.packpack.supporter.server;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class EnemyAnimHolder extends Holder {
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    private final ArrayList<Enemy> enemy;
    private final Message msg;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final int lang;
    private final boolean gif;
    private final boolean raw;

    private final String channelID;

    private int page = 0;
    private boolean expired = false;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public EnemyAnimHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, int mode, int frame, boolean transparent, boolean debug, int lang, boolean isGif, boolean raw) {
        this.enemy = enemy;
        this.msg = msg;
        this.channelID = channelID;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.lang = lang;
        this.gif = isGif;
        this.raw = raw;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), EnemyAnimHolder.this));

                msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(MessageCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID))
            return RESULT_STILL;

        String content = event.getMessage().getContent();

        if(content.equals("n")) {
            if(20 * (page + 1) >= enemy.size())
                return RESULT_STILL;

            page++;

            msg.edit(m -> {
                String check;

                if(enemy.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= enemy.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= enemy.size())
                        break;

                    Enemy e = enemy.get(i);

                    String fname = Data.trio(e.id.id) + " - ";

                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(e) != null)
                        fname += MultiLangCont.get(e);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                CommonStatic.getConfig().lang = oldConfig;

                if(enemy.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            msg.edit(m -> {
                String check;

                if(enemy.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= enemy.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= enemy.size())
                        break;

                    Enemy e = enemy.get(i);

                    String fname = Data.trio(e.id.id) + " - ";

                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(e) != null)
                        fname += MultiLangCont.get(e);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                CommonStatic.getConfig().lang = oldConfig;

                if(enemy.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= enemy.size())
                return RESULT_STILL;

            try {
                Enemy e = enemy.get(id);

                if(gif) {
                    TimeBoolean timeBoolean = StaticStore.canDo.get("gif");

                    if(timeBoolean == null || timeBoolean.canDo) {
                        new Thread(() -> {
                            try {
                                boolean result = EntityHandler.generateEnemyAnim(e, ch, mode, debug, frame, lang, raw);

                                if(result) {
                                    long time = raw ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.SECONDS.toMillis(30);

                                    StaticStore.canDo.put("gif", new TimeBoolean(false, time));

                                    Timer timer = new Timer();

                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            System.out.println("Remove Process : gif");
                                            StaticStore.canDo.put("gif", new TimeBoolean(true));
                                        }
                                    }, time);
                                }
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }).start();
                    } else {
                        ch.createMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))).subscribe();
                    }
                } else {
                    event.getMember().ifPresent(m -> {
                        try {
                            if(StaticStore.timeLimit.containsKey(m.getId().asString())) {
                                long time = StaticStore.timeLimit.get(m.getId().asString());

                                if(System.currentTimeMillis() - time > 10000) {
                                    EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                                    StaticStore.timeLimit.put(m.getId().asString(), System.currentTimeMillis());
                                } else {
                                    ch.createMessage(LangID.getStringByID("command_timelimit", lang).replace("_", DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))).subscribe();
                                }
                            } else {
                                EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                                StaticStore.timeLimit.put(m.getId().asString(), System.currentTimeMillis());
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            msg.delete().subscribe();

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            msg.edit(m -> {
                m.setContent(LangID.getStringByID("formst_cancel", lang));
                expired = true;
            }).subscribe();

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1])-1;

                    if(p < 0 || p * 20 >= enemy.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    msg.edit(m -> {
                        String check;

                        if(enemy.size() <= 20)
                            check = "";
                        else if(page == 0)
                            check = LangID.getStringByID("formst_next", lang);
                        else if((page + 1) * 20 >= enemy.size())
                            check = LangID.getStringByID("formst_pre", lang);
                        else
                            check = LangID.getStringByID("formst_nexpre", lang);

                        StringBuilder sb = new StringBuilder("```md\n").append(check);

                        for(int i = 20 * page; i < 20 * (page +1); i++) {
                            if(i >= enemy.size())
                                break;

                            Enemy e = enemy.get(i);

                            String fname = Data.trio(e.id.id) + " - ";

                            int oldConfig = CommonStatic.getConfig().lang;
                            CommonStatic.getConfig().lang = lang;

                            if(MultiLangCont.get(e) != null)
                                fname += MultiLangCont.get(e);

                            CommonStatic.getConfig().lang = oldConfig;

                            sb.append(i+1).append(". ").append(fname).append("\n");
                        }

                        if(enemy.size() > 20)
                            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

                        sb.append(LangID.getStringByID("formst_can", lang));
                        sb.append("```");

                        m.setContent(sb.toString());
                    }).subscribe();

                    cleaner.add(event.getMessage());
                }
            }
        }

        return RESULT_STILL;
    }

    @Override
    public void clean() {
        for(Message m : cleaner) {
            if(m != null)
                m.delete().subscribe();
        }
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();
    }
}
