package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
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

public class ComboFormMessageHolder extends MessageHolder<MessageReceivedEvent> {
    private final ArrayList<Form> form;
    private final Message msg;
    private final String channelID;

    private final int lang;
    private final String cName;
    private final String fName;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public ComboFormMessageHolder(ArrayList<Form> form, Message author, Message msg, String channelID, int lang, String cName, String fName) {
        super(MessageReceivedEvent.class);

        this.form = form;
        this.msg = msg;
        this.channelID = channelID;

        this.lang = lang;
        this.cName = cName;
        this.fName = fName;

        registerAutoFinish(this, msg, author, lang, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(MessageReceivedEvent event) {
        if(expired) {
            System.out.println("Expired at ComboFormHolder!!!");
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

            try {
                Form f = form.get(id);

                ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(f, cName);

                if(combos.isEmpty()) {
                    msg.delete().queue();

                    expired = true;

                    cleaner.add(event.getMessage());

                    createMessageWithNoPings(ch, LangID.getStringByID("combo_noname", lang).replace("_", getSearchKeywords(fName, cName, lang)));

                    return RESULT_FINISH;
                } else if(combos.size() == 1) {
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

                    msg.delete().queue();

                    try {
                        EntityHandler.showComboEmbed(ch, combos.get(0), lang);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    expired = true;

                    cleaner.add(event.getMessage());

                    return RESULT_FINISH;
                } else {
                    String check;

                    if(combos.size() <= 20)
                        check = "";
                    else
                        check = LangID.getStringByID("formst_next", lang);

                    StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

                    for(int i = 0; i < 20 ; i++) {
                        if(i >= combos.size())
                            break;

                        Combo c = combos.get(i);

                        String comboName = Data.trio(Integer.parseInt(c.name)) + " ";

                        int oldConfig = CommonStatic.getConfig().lang;
                        CommonStatic.getConfig().lang = lang;

                        if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                            comboName += MultiLangCont.getStatic().COMNAME.getCont(c) + " | " + DataToString.getComboType(c, lang);

                        CommonStatic.getConfig().lang = oldConfig;

                        sb.append(i+1).append(". ").append(comboName).append("\n");
                    }

                    if(combos.size() > 20)
                        sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(combos.size()/20 + 1)));

                    sb.append(LangID.getStringByID("formst_can", lang));
                    sb.append("```");



                    Message res = ch.sendMessage(sb.toString())
                            .allowedMentions(new ArrayList<>())
                            .complete();

                    String formName = StaticStore.safeMultiLangGet(form.get(id), lang);

                    if(formName == null || formName.isBlank())
                        formName = form.get(id).names.toString();

                    if(formName.isBlank())
                        formName = Data.trio(form.get(id).unit.id.id) +" - " + Data.trio(form.get(id).fid);

                    msg.editMessage(LangID.getStringByID("combo_selected", lang).replace("_", formName)).queue();

                    if(res != null) {
                        Member m = event.getMember();

                        if(m != null) {
                            StaticStore.removeHolder(m.getId(), ComboFormMessageHolder.this);

                            StaticStore.putHolder(m.getId(), new ComboMessageHolder(combos, event.getMessage(), res, msg, ch.getId(), lang));
                        }
                    }

                    expired = true;

                    cleaner.add(event.getMessage());

                    clean();

                    return RESULT_STILL;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            if(m != null) {
                m.delete().queue();
            }
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

    private String getSearchKeywords(String fName, String cName, int lang) {
        StringBuilder builder = new StringBuilder();

        if(cName != null) {
            builder.append(LangID.getStringByID("data_combo", lang)).append(" : `").append(cName).append("`");
        }

        if(fName != null) {
            if(cName != null) {
                builder.append(", ");
            }

            builder.append(LangID.getStringByID("data_unit", lang)).append(" : `").append(fName).append("`");
        }

        return builder.toString();
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
