package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import common.util.unit.Form;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ComboFormMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;

    private final String cName;
    private final String fName;

    public ComboFormMessageHolder(ArrayList<Form> form, Message author, Message msg, String channelID, int lang, String cName, String fName) {
        super(msg, author, channelID, lang);

        this.form = form;

        this.cName = cName;
        this.fName = fName;

        registerAutoFinish(this, msg, author, lang, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            if(MultiLangCont.get(f, lang) != null)
                fname += MultiLangCont.get(f, lang);

            data.add(fname);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        try {
            Form f = form.get(id);

            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(f, cName);

            if(combos.isEmpty()) {
                msg.delete().queue();

                createMessageWithNoPings(ch, LangID.getStringByID("combo_noname", lang).replace("_", validateKeyword(getSearchKeywords(fName, cName, lang))));
            } else if(combos.size() == 1) {
                User u = event.getUser();

                if(StaticStore.timeLimit.containsKey(u.getId())) {
                    StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());
                } else {
                    Map<String, Long> memberLimit = new HashMap<>();

                    memberLimit.put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());

                    StaticStore.timeLimit.put(u.getId(), memberLimit);
                }

                msg.delete().queue();

                try {
                    EntityHandler.showComboEmbed(ch, getAuthorMessage(), combos.get(0), lang);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateCombo(combos);

                for(int i = 0; i < data.size() ; i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(combos.size() > PAGE_CHUNK) {
                    int totalPage = combos.size() / PAGE_CHUNK;

                    if(combos.size() % PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = Command.registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), combos.size(), data, lang).complete();
                String formName = StaticStore.safeMultiLangGet(form.get(id), lang);

                if(formName == null || formName.isBlank())
                    formName = form.get(id).names.toString();

                if(formName.isBlank())
                    formName = Data.trio(form.get(id).unit.id.id) +" - " + Data.trio(form.get(id).fid);

                msg.editMessage(LangID.getStringByID("combo_selected", lang).replace("_", formName)).mentionRepliedUser(false).setComponents().queue();

                if(res != null) {
                    User u = event.getUser();

                    StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, getAuthorMessage(), res, msg, ch.getId(), lang));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
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

    private List<String> accumulateCombo(List<Combo> combos) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < PAGE_CHUNK ; i++) {
            if(i >= combos.size())
                break;

            Combo c = combos.get(i);

            String comboName = Data.trio(Integer.parseInt(c.name));

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                comboName += " " + MultiLangCont.getStatic().COMNAME.getCont(c);

            CommonStatic.getConfig().lang = oldConfig;

            comboName += " | " + DataToString.getComboType(c, lang) + " ";

            if(c.forms.length == 1) {
                comboName += LangID.getStringByID("combo_slot", lang);
            } else {
                comboName += String.format(LangID.getStringByID("combo_slots", lang), c.forms.length);
            }

            data.add(comboName);
        }

        return data;
    }

    private String validateKeyword(String keyword) {
        if(keyword.length() > 1500)
            return keyword.substring(0, 1500) + "...";
        else
            return keyword;
    }
}
