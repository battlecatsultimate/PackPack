package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ComboMessageHolder extends MessageHolder<MessageReceivedEvent> {
    private final ArrayList<Combo> combo;
    private final Message msg;
    private final Message fMsg;
    private final String channelID;

    private final int lang;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public ComboMessageHolder(ArrayList<Combo> combo, Message author, Message msg, Message fMsg, String channelID, int lang) {
        super(MessageReceivedEvent.class);

        this.combo = combo;
        this.msg = msg;
        this.fMsg = fMsg;
        this.channelID = channelID;

        this.lang = lang;

        registerAutoFinish(this, msg, author, lang, TimeUnit.MINUTES.toMillis(5), () -> {
            if(fMsg != null)
                fMsg.delete().queue();
        });
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
            if(20 * (page + 1) >= combo.size())
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

            if(id < 0 || id >= combo.size())
                return RESULT_STILL;

            msg.delete().queue();

            if(fMsg != null)
                fMsg.delete().queue();

            try {
                EntityHandler.showComboEmbed(ch, combo.get(id), lang);

                Member m = event.getMember();

                if(m != null) {
                    if(StaticStore.timeLimit.containsKey(m.getId())) {
                        StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());
                    } else {
                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(m.getId(), memberLimit);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            if(fMsg != null)
                fMsg.delete().queue();

            msg.editMessage(LangID.getStringByID("formst_cancel" ,lang)).queue();

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if (StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1]) - 1;

                    if (p < 0 || p * 20 >= combo.size()) {
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

        if(fMsg != null)
            fMsg.delete().queue();

        msg.editMessage(LangID.getStringByID("formst_expire", lang)).queue();
    }

    private void edit() {
        String check;

        if(combo.size() <= 20)
            check = "";
        else if(page == 0)
            check = LangID.getStringByID("formst_next", lang);
        else if((page + 1) * 20 >= combo.size())
            check = LangID.getStringByID("formst_pre", lang);
        else
            check = LangID.getStringByID("formst_nexpre", lang);

        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

        for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
            if(i >= combo.size())
                break;

            Combo c = combo.get(i);

            String comboName = Data.trio(Integer.parseInt(c.name)) + " ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                comboName += MultiLangCont.getStatic().COMNAME.getCont(c) + " | " + DataToString.getComboType(c, lang);

            CommonStatic.getConfig().lang = oldConfig;

            sb.append(i+1).append(". ").append(comboName).append("\n");
        }

        if(combo.size() > 20)
            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(combo.size()/20 + 1)));

        sb.append(LangID.getStringByID("formst_can", lang));
        sb.append("```");

        msg.editMessage(sb.toString()).queue();
    }
}
