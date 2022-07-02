package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class EnemyAnimMessageHolder extends MessageHolder<MessageReceivedEvent> {
    private final ArrayList<Enemy> enemy;
    private final Message msg;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final int lang;
    private final boolean gif;
    private final boolean raw;
    private final boolean gifMode;

    private final String channelID;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public EnemyAnimMessageHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, int mode, int frame, boolean transparent, boolean debug, int lang, boolean isGif, boolean raw, boolean gifMode) {
        super(MessageReceivedEvent.class);

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
        this.gifMode = gifMode;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public int handleEvent(MessageReceivedEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel();

        if(!ch.getId().equals(channelID))
            return RESULT_STILL;

        String content = event.getMessage().getContentRaw();

        if(content.equals("n")) {
            if(20 * (page + 1) >= enemy.size())
                return RESULT_STILL;

            page++;

            edit();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            edit();

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
                            Guild g = event.getGuild();

                            try {
                                boolean result = EntityHandler.generateEnemyAnim(e, ch, g.getBoostTier().getKey(), mode, debug, frame, lang, raw, gifMode);

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
                        ch.sendMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))).queue();
                    }
                } else {
                    Member m = event.getMember();

                    if(m != null) {
                        try {
                            if(StaticStore.timeLimit.containsKey(m.getId()) && StaticStore.timeLimit.get(m.getId()).containsKey(StaticStore.COMMAND_ENEMYIMAGE_ID)) {
                                long time = StaticStore.timeLimit.get(m.getId()).get(StaticStore.COMMAND_ENEMYIMAGE_ID);

                                if(System.currentTimeMillis() - time > 10000) {
                                    EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                                    StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                                } else {
                                    ch.sendMessage(LangID.getStringByID("command_timelimit", lang).replace("_", DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))).queue();
                                }
                            } else if(StaticStore.timeLimit.containsKey(m.getId())) {
                                EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                                StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                            } else {
                                EntityHandler.generateEnemyImage(e, ch, mode, frame, transparent, debug, lang);

                                Map<String, Long> memberLimit = new HashMap<>();

                                memberLimit.put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());

                                StaticStore.timeLimit.put(m.getId(), memberLimit);
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            msg.delete().queue();

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            msg.editMessage(LangID.getStringByID("formst_cancel", lang)).queue();

            expired = true;

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

                    edit();

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
                m.delete().queue();
        }
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.editMessage(LangID.getStringByID("formst_expire", lang)).queue();
    }

    public void edit() {
        String check;

        if(enemy.size() <= 20)
            check = "";
        else if(page == 0)
            check = LangID.getStringByID("formst_next", lang);
        else if((page + 1) * 20 >= enemy.size())
            check = LangID.getStringByID("formst_pre", lang);
        else
            check = LangID.getStringByID("formst_nexpre", lang);

        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

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

        msg.editMessage(sb.toString()).queue();
    }
}
