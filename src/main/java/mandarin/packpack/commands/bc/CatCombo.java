package mandarin.packpack.commands.bc;

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
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.ComboFormMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.ComboMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CatCombo extends TimedConstraintCommand {
    public CatCombo(ConstraintCommand.ROLE role, int lang, IDHolder id) {
        super(role, lang, id, TimeUnit.SECONDS.toMillis(5), StaticStore.COMMAND_COMBO_ID, false);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String name = getUnitName(loader.getContent());
        String cName = getComboName(loader.getContent());

        if(name == null || name.isBlank()) {
            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(null, cName);

            if(combos.isEmpty()) {
                disableTimer();
                replyToMessageSafely(ch, LangID.getStringByID("combo_noname", lang).replace("_", validateKeyword(getSearchKeywords(name, cName, lang))), loader.getMessage(), a -> a);
            } else if(combos.size() == 1) {
                EntityHandler.showComboEmbed(ch, loader.getMessage(), combos.get(0), lang);
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

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, combos.size(), data, lang), res -> {
                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, res, null, ch.getId(), lang));
                    }
                });
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(name, false, lang);

            if(forms.isEmpty()) {
                disableTimer();
                replyToMessageSafely(ch, LangID.getStringByID("combo_noname", lang).replace("_", validateKeyword(getSearchKeywords(name, cName, lang))), loader.getMessage(), a -> a);
            } else if(forms.size() == 1) {
                ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(forms.get(0), cName);

                if(combos.isEmpty()) {
                    disableTimer();

                    replyToMessageSafely(ch, LangID.getStringByID("combo_noname", lang).replace("_", validateKeyword(getSearchKeywords(name, cName, lang))), loader.getMessage(), a -> a);
                } else if(combos.size() == 1) {
                    EntityHandler.showComboEmbed(ch, loader.getMessage(), combos.get(0), lang);
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

                    replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, combos.size(), data, lang), res -> {
                        if(res != null) {
                            User u = loader.getUser();

                            Message msg = loader.getMessage();

                            StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, res, null, ch.getId(), lang));
                        }
                    });
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

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        StaticStore.putHolder(u.getId(), new ComboFormMessageHolder(forms, msg, res, ch.getId(), lang, cName, name));
                    }
                });
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

            String comboName = Data.trio(Integer.parseInt(c.name));

            if(MultiLangCont.getStatic().COMNAME.getCont(c, lang) != null)
                comboName += " " + MultiLangCont.getStatic().COMNAME.getCont(c, lang);

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

    private List<String> accumulateUnit(List<Form> forms) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= forms.size())
                break;

            Form f = forms.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            if(MultiLangCont.get(f, lang) != null)
                fname += MultiLangCont.get(f, lang);

            data.add(fname);
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
