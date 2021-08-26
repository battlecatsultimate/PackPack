package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.ComboFormHolder;
import mandarin.packpack.supporter.server.holder.ComboHolder;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CatCombo extends TimedConstraintCommand {
    public CatCombo(ConstraintCommand.ROLE role, int lang, IDHolder id) {
        super(role, lang, id, TimeUnit.SECONDS.toMillis(5), StaticStore.COMMAND_COMBO_ID);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        String name = getUnitName(getContent(event));
        String cName = getComboName(getContent(event));

        if(name == null || name.isBlank()) {
            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(null, cName);

            if(combos.isEmpty()) {
                disableTimer();
                createMessageWithNoPings(ch, LangID.getStringByID("combo_noname", lang).replace("_", getSearchKeywords(name, cName, lang)));
            } else if(combos.size() == 1) {
                EntityHandler.showComboEmbed(ch, combos.get(0), lang);
            } else {
                disableTimer();

                String check;

                if(combos.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 0; i < 20 ; i++) {
                    if(i >= combos.size())
                        break;

                    Combo c = combos.get(i);

                    String comboName = Data.trio(c.name) + " ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.getStatic().COMNAME.getCont(c.name) != null)
                        comboName += MultiLangCont.getStatic().COMNAME.getCont(c.name)+" "+ DataToString.getComboType(c, lang);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(comboName).append("\n");
                }

                if(combos.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(combos.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                Message res = getMessageWithNoPings(ch, sb.toString());

                if(res != null) {
                    getMember(event).ifPresent(m -> {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(m.getId().asString(), new ComboHolder(combos, msg, res, null, ch.getId().asString(), lang));
                        }
                    });
                }
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(name, lang);

            if(forms.isEmpty()) {
                disableTimer();
                createMessageWithNoPings(ch, LangID.getStringByID("combo_noname", lang).replace("_", getSearchKeywords(name, cName, lang)));
            } else if(forms.size() == 1) {
                ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(forms.get(0), cName);

                if(combos.isEmpty()) {
                    disableTimer();

                    createMessageWithNoPings(ch, LangID.getStringByID("combo_noname", lang).replace("_", getSearchKeywords(name, cName, lang)));
                } else if(combos.size() == 1) {
                    EntityHandler.showComboEmbed(ch, combos.get(0), lang);
                } else {
                    disableTimer();

                    String check;

                    if(combos.size() <= 20)
                        check = "";
                    else
                        check = LangID.getStringByID("formst_next", lang);

                    StringBuilder sb = new StringBuilder("```md\n").append(check);

                    for(int i = 0; i < 20 ; i++) {
                        if(i >= combos.size())
                            break;

                        Combo c = combos.get(i);

                        String comboName = Data.trio(c.name) + " ";

                        int oldConfig = CommonStatic.getConfig().lang;
                        CommonStatic.getConfig().lang = lang;

                        if(MultiLangCont.getStatic().COMNAME.getCont(c.name) != null)
                            comboName += MultiLangCont.getStatic().COMNAME.getCont(c.name)+ " " + DataToString.getComboType(c, lang);

                        CommonStatic.getConfig().lang = oldConfig;

                        sb.append(i+1).append(". ").append(comboName).append("\n");
                    }

                    if(combos.size() > 20)
                        sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(combos.size()/20 + 1)));

                    sb.append(LangID.getStringByID("formst_can", lang));
                    sb.append("```");

                    Message res = getMessageWithNoPings(ch, sb.toString());

                    if(res != null) {
                        getMember(event).ifPresent(m -> {
                            Message msg = getMessage(event);

                            if(msg != null) {
                                StaticStore.putHolder(m.getId().asString(), new ComboHolder(combos, msg, res, null, ch.getId().asString(), lang));
                            }
                        });
                    }
                }
            } else {
                String check;

                if(forms.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 0; i < 20; i++) {
                    if(i >= forms.size())
                        break;

                    Form f = forms.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(forms.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(forms.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                Message res = getMessageWithNoPings(ch, sb.toString());

                if(res != null) {
                    getMember(event).ifPresent(m -> {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(m.getId().asString(), new ComboFormHolder(forms, msg, res, ch.getId().asString(), lang, cName, name));
                        }
                    });
                }
            }
        }
    }

    private String getUnitName(String message) {
        String[] contents = message.split("[ ]+-u[ ]+", 2);

        if(contents.length <= 1)
            return null;
        else
            return contents[1].isBlank() ? null : contents[1];
    }

    private String getComboName(String message) {
        String[] contents = message.split(" ");

        if(contents.length <= 1)
            return null;
        else {
            StringBuilder builder = new StringBuilder();

            for(int i = 1; i < contents.length; i++) {
                if(i != 1)
                    builder.append(" ");

                if(!contents[i].equals("-u")) {
                    builder.append(contents[i]);
                } else {
                    break;
                }
            }

            if(builder.toString().isBlank())
                return null;

            return builder.toString();
        }
    }

    private String getSearchKeywords(String fName, String cName, int lang) {
        StringBuilder builder = new StringBuilder();

        if(cName != null) {
            builder.append(LangID.getStringByID("data_combo", lang)).append(" : ").append(cName);
        }

        if(fName != null) {
            if(cName != null) {
                builder.append(", ");
            }

            builder.append(LangID.getStringByID("data_unit", lang)).append(" : ").append(fName);
        }

        return builder.toString();
    }
}
