package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ComboFormMessageHolder;
import mandarin.packpack.supporter.server.holder.ComboMessageHolder;
import mandarin.packpack.supporter.server.holder.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CatCombo extends TimedConstraintCommand {
    public CatCombo(ConstraintCommand.ROLE role, int lang, IDHolder id) {
        super(role, lang, id, TimeUnit.SECONDS.toMillis(5), StaticStore.COMMAND_COMBO_ID);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        String name = getUnitName(getContent(event));
        String cName = getComboName(getContent(event));

        if(name == null || name.isBlank()) {
            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(null, cName);

            if(combos.isEmpty()) {
                disableTimer();
                replyToMessageSafely(ch, LangID.getStringByID("combo_noname", lang).replace("_", getSearchKeywords(name, cName, lang)), getMessage(event), a -> a);
            } else if(combos.size() == 1) {
                EntityHandler.showComboEmbed(ch, getMessage(event), combos.get(0), lang);
            } else {
                disableTimer();

                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateCombo(combos);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(combos.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = combos.size() / SearchHolder.PAGE_CHUNK;

                    if(combos.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, combos.size(), data, lang));

                if(res != null) {
                    Member m = getMember(event);

                    if(m != null) {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(m.getId(), new ComboMessageHolder(combos, msg, res, null, ch.getId(), lang));
                        }
                    }
                }
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(name, lang);

            if(forms.isEmpty()) {
                disableTimer();
                replyToMessageSafely(ch, LangID.getStringByID("combo_noname", lang).replace("_", getSearchKeywords(name, cName, lang)), getMessage(event), a -> a);
            } else if(forms.size() == 1) {
                ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(forms.get(0), cName);

                if(combos.isEmpty()) {
                    disableTimer();

                    replyToMessageSafely(ch, LangID.getStringByID("combo_noname", lang).replace("_", getSearchKeywords(name, cName, lang)), getMessage(event), a -> a);
                } else if(combos.size() == 1) {
                    EntityHandler.showComboEmbed(ch, getMessage(event), combos.get(0), lang);
                } else {
                    disableTimer();

                    StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang));

                    List<String> data = accumulateCombo(combos);

                    for(int i = 0; i < data.size(); i++) {
                        sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                    }

                    if(combos.size() > SearchHolder.PAGE_CHUNK) {
                        int totalPage = combos.size() / SearchHolder.PAGE_CHUNK;

                        if(combos.size() % SearchHolder.PAGE_CHUNK != 0)
                            totalPage++;

                        sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                    }

                    sb.append("```");

                    Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, combos.size(), data, lang));

                    if(res != null) {
                        Member m = getMember(event);

                        if(m != null) {
                            Message msg = getMessage(event);

                            if(msg != null) {
                                StaticStore.putHolder(m.getId(), new ComboMessageHolder(combos, msg, res, null, ch.getId(), lang));
                            }
                        }
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateUnit(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = forms.size() / SearchHolder.PAGE_CHUNK;

                    if(forms.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, forms.size(), data, lang));

                if(res != null) {
                    Member m = getMember(event);

                    if(m != null) {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(m.getId(), new ComboFormMessageHolder(forms, msg, res, ch.getId(), lang, cName, name));
                        }
                    }
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

    private List<String> accumulateCombo(List<Combo> combos) {
        List<String> data = new ArrayList<>();
        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= combos.size())
                break;

            Combo c = combos.get(i);

            String comboName = Data.trio(Integer.parseInt(c.name)) + " ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                comboName += MultiLangCont.getStatic().COMNAME.getCont(c) + " | " + DataToString.getComboType(c, lang);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(comboName);
        }

        return data;
    }

    private List<String> accumulateUnit(List<Form> forms) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= forms.size())
                break;

            Form f = forms.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(f) != null)
                fname += MultiLangCont.get(f);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(fname);
        }

        return data;
    }
}
