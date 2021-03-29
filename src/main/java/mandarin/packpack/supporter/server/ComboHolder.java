package mandarin.packpack.supporter.server;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
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

public class ComboHolder extends Holder<MessageCreateEvent> {
    private final ArrayList<Combo> combo;
    private final Message msg;
    private final Message fMsg;
    private final String channelID;

    private final int lang;

    private int page = 0;
    private boolean expired = false;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public ComboHolder(ArrayList<Combo> combo, Message author, Message msg, Message fMsg, String channelID, int lang) {
        super(MessageCreateEvent.class);

        this.combo = combo;
        this.msg = msg;
        this.fMsg = fMsg;
        this.channelID = channelID;

        this.lang = lang;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), ComboHolder.this));

                if(fMsg != null)
                    fMsg.delete().subscribe();

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
            if(20 * (page + 1) >= combo.size())
                return RESULT_STILL;

            page++;

            msg.edit(m -> {
                String check;

                if(combo.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= combo.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
                    if(i >= combo.size())
                        break;

                    Combo c = combo.get(i);

                    String comboName = Data.trio(c.name) + " ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.getStatic().COMNAME.getCont(c.name) != null)
                        comboName += MultiLangCont.getStatic().COMNAME.getCont(c.name);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(comboName).append("\n");
                }

                if(combo.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(combo.size()/20 + 1)));

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

                if(combo.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= combo.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
                    if(i >= combo.size())
                        break;

                    Combo c = combo.get(i);

                    String comboName = Data.trio(c.name) + " ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.getStatic().COMNAME.getCont(c.name) != null)
                        comboName += MultiLangCont.getStatic().COMNAME.getCont(c.name);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(comboName).append("\n");
                }

                if(combo.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(combo.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= combo.size())
                return RESULT_STILL;

            msg.delete().subscribe();

            if(fMsg != null)
                fMsg.delete().subscribe();

            try {
                EntityHandler.showComboEmbed(ch, combo.get(id), lang);

                event.getMember().ifPresent(m -> StaticStore.timeLimit.put(m.getId().asString(), System.currentTimeMillis()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            if(fMsg != null)
                fMsg.delete().subscribe();

            msg.edit(m -> {
                m.setContent(LangID.getStringByID("formst_cancel" ,lang));
                expired = true;
            }).subscribe();

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

                    msg.edit(m -> {
                        String check;

                        if(combo.size() <= 20)
                            check = "";
                        else if(page == 0)
                            check = LangID.getStringByID("formst_next", lang);
                        else if((page + 1) * 20 >= combo.size())
                            check = LangID.getStringByID("formst_pre", lang);
                        else
                            check = LangID.getStringByID("formst_nexpre", lang);

                        StringBuilder sb = new StringBuilder("```md\n").append(check);

                        for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
                            if(i >= combo.size())
                                break;

                            Combo c = combo.get(i);

                            String comboName = Data.trio(c.name) + " ";

                            int oldConfig = CommonStatic.getConfig().lang;
                            CommonStatic.getConfig().lang = lang;

                            if(MultiLangCont.getStatic().COMNAME.getCont(c.name) != null)
                                comboName += MultiLangCont.getStatic().COMNAME.getCont(c.name);

                            CommonStatic.getConfig().lang = oldConfig;

                            sb.append(i+1).append(". ").append(comboName).append("\n");
                        }

                        if(combo.size() > 20)
                            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(combo.size()/20 + 1)));

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

        cleaner.clear();
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        if(fMsg != null)
            fMsg.delete().subscribe();

        msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();
    }
}
