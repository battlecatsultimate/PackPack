package mandarin.packpack.supporter.server.holder.component.search;

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
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComboFormMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;

    private final String cName;
    private final String fName;

    public ComboFormMessageHolder(ArrayList<Form> form, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, CommonStatic.Lang.Locale lang, String cName, String fName, ConfigHolder.SearchLayout layout) {
        super(author, userID, channelID, message, fName,  layout, lang);

        this.form = form;

        this.cName = cName;
        this.fName = fName;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String text = null;

            switch (textType) {
                case TEXT -> {
                    text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + " ";

                    if (StaticStore.safeMultiLangGet(f, lang) != null) {
                        text += StaticStore.safeMultiLangGet(f, lang);
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(f, lang);

                    if (text == null) {
                        text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
            }

            data.add(text);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        try {
            Form f = form.get(id);

            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(f, cName, lang);

            if(combos.isEmpty()) {
                message.delete().queue();

                createMessageWithNoPings(ch, LangID.getStringByID("combo.failed.noCombo", lang).replace("_", validateKeyword(getSearchKeywords(fName, cName, lang))));
            } else if(combos.size() == 1) {
                User u = event.getUser();

                if(StaticStore.timeLimit.containsKey(u.getId())) {
                    StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());
                } else {
                    Map<String, Long> memberLimit = new HashMap<>();

                    memberLimit.put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());

                    StaticStore.timeLimit.put(u.getId(), memberLimit);
                }

                try {
                    EntityHandler.showComboEmbed(event, getAuthorMessage(), combos.getFirst(), lang, true);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/ComboFormMessageHolder::onSelected - Failed to upload combo embed");
                }
            } else {
                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateCombo(combos);

                for(int i = 0; i < data.size() ; i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(combos.size() > chunk) {
                    int totalPage = getTotalPage(getDataSize(), chunk);

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                Command.registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), combos.size(), data, lang).queue(res -> {
                    String formName = StaticStore.safeMultiLangGet(form.get(id), lang);

                    if(formName == null || formName.isBlank())
                        formName = form.get(id).names.toString();

                    if(formName.isBlank())
                        formName = Data.trio(form.get(id).unit.id.id) +" - " + Data.trio(form.get(id).fid);

                    message.editMessage(LangID.getStringByID("combo.selected", lang).replace("_", formName)).mentionRepliedUser(false).setComponents().queue();

                    if(res != null) {
                        User u = event.getUser();

                        StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, getAuthorMessage(), userID, ch.getId(), res, cName, layout, lang));
                    }
                });
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/ComboFormMessageHolder::onSelected - Failed to handle combo embed holder");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }

    private String getSearchKeywords(String fName, String cName, CommonStatic.Lang.Locale lang) {
        StringBuilder builder = new StringBuilder();

        if(cName != null) {
            builder.append(LangID.getStringByID("data.combo.combo", lang)).append(" : `").append(cName).append("`");
        }

        if(fName != null) {
            if(cName != null) {
                builder.append(", ");
            }

            builder.append(LangID.getStringByID("data.stage.limit.unit", lang)).append(" : `").append(fName).append("`");
        }

        return builder.toString();
    }

    private List<String> accumulateCombo(List<Combo> combos) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < chunk ; i++) {
            if(i >= combos.size())
                break;

            Combo c = combos.get(i);

            String comboName = Data.trio(Integer.parseInt(c.name));

            if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
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

    private String validateKeyword(String keyword) {
        if(keyword.length() > 1500)
            return keyword.substring(0, 1500) + "...";
        else
            return keyword;
    }
}
