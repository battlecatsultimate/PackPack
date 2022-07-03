package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;

public class FormStatMessageHolder extends MessageHolder<MessageReceivedEvent> {
    private final ArrayList<Form> form;
    private final Message msg;
    private final String channelID;
    private final ConfigHolder config;

    private int page = 0;

    private final boolean talent;
    private final boolean isFrame;
    private final boolean extra;
    private final ArrayList<Integer> lv;
    private final int lang;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public FormStatMessageHolder(ArrayList<Form> form, Message author, ConfigHolder config, Message msg, String channelID, int param, ArrayList<Integer> lv, int lang) {
        super(MessageReceivedEvent.class);

        this.form = form;
        this.msg = msg;
        this.channelID = channelID;
        this.config = config;

        this.talent = (param & 2) > 0;
        this.isFrame = (param & 4) == 0 && config.useFrame;
        this.extra = (param & 8) > 0 || config.extra;
        this.lv = lv;
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
            if(20 * (page + 1) >= form.size())
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

            if(id < 0 || id >= form.size())
                return RESULT_STILL;

            msg.delete().queue();

            if(lv.get(0) > form.get(id).unit.max + form.get(id).unit.maxp)
                lv.set(0, form.get(id).unit.max + form.get(id).unit.maxp);
            else if(lv.get(0) <= 0) {
                if(form.get(id).unit.rarity == 0)
                    lv.set(0, 110);
                else
                    lv.set(0, 30);
            }

            try {
                Message result = EntityHandler.showUnitEmb(form.get(id), ch, config, isFrame, talent, extra, lv, lang, true);

                if(result != null) {
                    Member m = event.getMember();

                    if(m != null) {
                        StaticStore.removeHolder(m.getId(), FormStatMessageHolder.this);

                        StaticStore.putHolder(m.getId(), new FormButtonHolder(form.get(id), event.getMessage(), result, config, isFrame, talent, extra, lv, lang, channelID, m.getId()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

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

                    if(p < 0 || p * 20 >= form.size()) {
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

    private void edit() {
        String check;

        if(form.size() <= 20)
            check = "";
        else if(page == 0)
            check = LangID.getStringByID("formst_next", lang);
        else if((page + 1) * 20 >= form.size())
            check = LangID.getStringByID("formst_pre", lang);
        else
            check = LangID.getStringByID("formst_nexpre", lang);

        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

        for(int i = 20 * page; i < 20 * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(f) != null)
                fname += MultiLangCont.get(f);

            CommonStatic.getConfig().lang = oldConfig;

            sb.append(i+1).append(". ").append(fname).append("\n");
        }

        if(form.size() > 20)
            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

        sb.append(LangID.getStringByID("formst_can", lang));
        sb.append("```");

        msg.editMessage(sb.toString()).queue();
    }
}
