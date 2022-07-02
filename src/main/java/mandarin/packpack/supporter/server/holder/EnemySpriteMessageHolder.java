package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;

public class EnemySpriteMessageHolder extends MessageHolder<MessageReceivedEvent> {
    private final ArrayList<Enemy> enemy;
    private final Message msg;
    private final String channelID;

    private final int mode;
    private final int lang;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public EnemySpriteMessageHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, int mode, int lang) {
        super(MessageReceivedEvent.class);

        this.enemy = enemy;
        this.msg = msg;
        this.channelID = channelID;
        this.mode = mode;
        this.lang = lang;

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

                EntityHandler.getEnemySprite(e, ch, mode, lang);
            } catch (Exception e) {
                e.printStackTrace();
            }

            msg.delete().queue();

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            msg.editMessage(LangID.getStringByID("formst_cancel" ,lang)).queue();

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if (StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1]) - 1;

                    if (p < 0 || p * 20 >= enemy.size()) {
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

        cleaner.clear();
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.editMessage(LangID.getStringByID("formst_expire", lang)).queue();
    }

    private void edit() {
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

        for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
            if(i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String ename = Data.trio(e.id.id)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(e) != null)
                ename += MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            sb.append(i+1).append(". ").append(ename).append("\n");
        }

        if(enemy.size() > 20)
            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

        sb.append(LangID.getStringByID("formst_can", lang));
        sb.append("```");

        msg.editMessage(sb.toString()).queue();
    }
}
