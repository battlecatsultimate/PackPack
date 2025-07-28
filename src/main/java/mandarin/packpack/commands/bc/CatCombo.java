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
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.ComboFormMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.ComboMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CatCombo extends TimedConstraintCommand {
    private final ConfigHolder config;

    public CatCombo(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id) {
        super(role, lang, id, TimeUnit.SECONDS.toMillis(5), StaticStore.COMMAND_COMBO_ID, false);

        this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String name = getUnitName(loader.getContent());
        String cName = getComboName(loader.getContent());

        if(name == null || name.isBlank()) {
            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(null, cName, lang);

            if(combos.isEmpty()) {
                disableTimer();
                replyToMessageSafely(ch, LangID.getStringByID("combo.failed.noCombo", lang).replace("_", validateKeyword(getSearchKeywords(name, cName, lang))), loader.getMessage(), a -> a);
            } else if(combos.size() == 1) {
                EntityHandler.showComboEmbed(ch, loader.getMessage(), combos.getFirst(), lang, false);
            } else {
                disableTimer();

                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateCombo(combos);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(combos.size() > config.searchLayout.chunkSize) {
                    int totalPage = combos.size() / config.searchLayout.chunkSize;

                    if(combos.size() % config.searchLayout.chunkSize != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, combos.size(), data, lang), res -> {
                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, u.getId(), ch.getId(), res, cName, config.searchLayout, lang));
                    }
                });
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(name, false, lang);

            if(forms.isEmpty()) {
                disableTimer();
                replyToMessageSafely(ch, LangID.getStringByID("combo.failed.noCombo", lang).replace("_", validateKeyword(getSearchKeywords(name, cName, lang))), loader.getMessage(), a -> a);
            } else if(forms.size() == 1) {
                ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(forms.getFirst(), cName, lang);

                if(combos.isEmpty()) {
                    disableTimer();

                    replyToMessageSafely(ch, LangID.getStringByID("combo.failed.noCombo", lang).replace("_", validateKeyword(getSearchKeywords(name, cName, lang))), loader.getMessage(), a -> a);
                } else if(combos.size() == 1) {
                    EntityHandler.showComboEmbed(ch, loader.getMessage(), combos.getFirst(), lang, false);
                } else {
                    disableTimer();

                    StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                    List<String> data = accumulateCombo(combos);

                    for(int i = 0; i < data.size(); i++) {
                        sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                    }

                    if(combos.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
                        int totalPage = combos.size() / ConfigHolder.SearchLayout.COMPACTED.chunkSize;

                        if(combos.size() % ConfigHolder.SearchLayout.COMPACTED.chunkSize != 0)
                            totalPage++;

                        sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                    }

                    sb.append("```");

                    replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, combos.size(), data, lang), res -> {
                        if(res != null) {
                            User u = loader.getUser();

                            Message msg = loader.getMessage();

                            StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, u.getId(), ch.getId(), res, cName, config.searchLayout, lang));
                        }
                    });
                }
            } else {
                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateUnit(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
                    int totalPage = forms.size() / ConfigHolder.SearchLayout.COMPACTED.chunkSize;

                    if(forms.size() % ConfigHolder.SearchLayout.COMPACTED.chunkSize != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        StaticStore.putHolder(u.getId(), new ComboFormMessageHolder(forms, msg, u.getId(), ch.getId(), res, lang, cName, name, config.searchLayout));
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
            return "";
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
                return "";

            return builder.toString();
        }
    }

    private String getSearchKeywords(String fName, String cName, CommonStatic.Lang.Locale lang) {
        StringBuilder builder = new StringBuilder();

        if(cName != null) {
            builder.append(LangID.getStringByID("data.combo.combo", lang)).append(" : ").append(cName);
        }

        if(fName != null) {
            if(cName != null) {
                builder.append(", ");
            }

            builder.append(LangID.getStringByID("data.stage.limit.unit", lang)).append(" : ").append(fName);
        }

        return builder.toString();
    }

    private List<String> accumulateCombo(List<Combo> combos) {
        List<String> data = new ArrayList<>();
        for(int i = 0; i < ConfigHolder.SearchLayout.COMPACTED.chunkSize; i++) {
            if(i >= combos.size())
                break;

            Combo c = combos.get(i);

            String comboName = Data.trio(Integer.parseInt(c.name));

            if(MultiLangCont.getStatic().COMNAME.getCont(c, lang) != null)
                comboName += " " + MultiLangCont.getStatic().COMNAME.getCont(c, lang);

            comboName += " | " + DataToString.getComboType(c, lang) + " ";

            if(c.forms.length == 1) {
                comboName += LangID.getStringByID("combo.slot.singular", lang);
            } else {
                comboName += String.format(LangID.getStringByID("combo.slot.plural", lang), c.forms.length);
            }

            data.add(comboName);
        }

        return data;
    }

    private List<String> accumulateUnit(List<Form> forms) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < ConfigHolder.SearchLayout.COMPACTED.chunkSize; i++) {
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
