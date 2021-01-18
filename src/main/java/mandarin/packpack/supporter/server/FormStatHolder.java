package mandarin.packpack.supporter.server;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FormStatHolder {
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    private final ArrayList<Form> form;
    private final Message msg;

    private int page = 0;
    private boolean expired = false;

    private final boolean talent;
    private final boolean isFrame;
    private int lv;
    private final int lang;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public FormStatHolder(ArrayList<Form> form, Message msg, int param, int lv, int lang) {
        this.form = form;
        this.msg = msg;

        this.talent = (param & 2) > 0;
        this.isFrame = (param & 4) == 0;
        this.lv = lv;
        this.lang = lang;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                msg.edit(m -> {
                    m.setContent("Searching process expired...");

                    expired = true;

                    msg.getAuthor().ifPresent(u -> StaticStore.formHolder.remove(u.getId().asString()));
                }).subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(1));
    }

    public int handleEvent(MessageCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return RESULT_FAIL;
        }

        String content = event.getMessage().getContent();

        if(content.equals("n")) {
            if(20 * (page + 1) >= form.size())
                return RESULT_STILL;

            page++;

            msg.edit(m -> {
                String check;

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= form.size())
                        break;

                    Form f = form.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(form.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

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

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= form.size())
                        break;

                    Form f = form.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(form.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            MessageChannel ch = event.getMessage().getChannel().block();

            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= form.size())
                return RESULT_STILL;

            if(ch != null) {
                msg.delete().subscribe();

                if(lv > form.get(id).unit.max + form.get(id).unit.maxp)
                    lv = form.get(id).unit.max + form.get(id).unit.maxp;

                EntityHandler.showUnitEmb(form.get(id), ch, isFrame, talent, lv, lang);
            }

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

                    if(p < 0 || p * 20 >= form.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    msg.edit(m -> {
                        String check;

                        if(form.size() <= 20)
                            check = "";
                        else if(page == 0)
                            check = LangID.getStringByID("formst_next", lang);
                        else if((page + 1) * 20 >= form.size())
                            check = LangID.getStringByID("formst_pre", lang);
                        else
                            check = LangID.getStringByID("formst_nexpre", lang);

                        StringBuilder sb = new StringBuilder("```md\n").append(check);

                        for(int i = 20 * page; i < 20 * (page +1); i++) {
                            if(i >= form.size())
                                break;

                            Form f = form.get(i);

                            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                            if(MultiLangCont.get(f) != null)
                                fname += MultiLangCont.get(f);

                            sb.append(i+1).append(". ").append(fname).append("\n");
                        }

                        if(form.size() > 20)
                            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                        sb.append("```");

                        m.setContent(sb.toString());
                    }).subscribe();

                    cleaner.add(event.getMessage());
                }
            }
        }

        return RESULT_STILL;
    }

    public void clean() {
        for(Message m : cleaner) {
            if(m != null)
                m.delete().subscribe();
        }
    }
}
