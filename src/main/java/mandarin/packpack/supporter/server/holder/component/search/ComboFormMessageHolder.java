package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Combo;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
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
        super(author, userID, channelID, message, fName, layout, lang);

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
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + " ";

                        if (StaticStore.safeMultiLangGet(f, lang) != null) {
                            text += StaticStore.safeMultiLangGet(f, lang);
                        }
                    } else {
                        text = "`" + Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + "` ";

                        String formName = StaticStore.safeMultiLangGet(f, lang);

                        if (formName == null || formName.isBlank()) {
                            formName = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
                        }

                        text += "**" + formName + "**";
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
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        MessageChannel ch = event.getChannel();

        try {
            Form f = form.get(index);

            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(f, cName, lang);

            if(combos.isEmpty()) {
                message.delete().queue();

                createMessageWithNoPings(ch, LangID.getStringByID("combo.failed.noCombo", lang).formatted(validateKeyword(getSearchKeywords(fName, cName, lang))));
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
                    EntityHandler.generateComboEmbed(event, getAuthorMessage(), combos.getFirst(), lang, true);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/ComboFormMessageHolder::onSelected - Failed to upload combo embed");
                }
            } else {
                String unitName = StaticStore.safeMultiLangGet(f, lang);

                if (unitName == null || unitName.isBlank()) {
                    unitName = Data.trio(f.uid.id) + " - " + Data.trio(f.fid);
                }

                connectTo(event, new ComboMessageHolder(combos, getAuthorMessage(), userID, channelID, message, unitName, cName, layout, lang));
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

        if(cName != null && !cName.isBlank()) {
            builder.append(LangID.getStringByID("data.combo.combo", lang)).append(" : ").append(cName);
        }

        if(fName != null && !fName.isBlank()) {
            if(cName != null && !cName.isBlank()) {
                builder.append(", ");
            }

            builder.append(LangID.getStringByID("data.stage.limit.unit", lang)).append(" : ").append(fName);
        }

        return builder.toString();
    }

    private String validateKeyword(String keyword) {
        if(keyword.length() > 1500)
            return keyword.substring(0, 1500) + "...";
        else
            return keyword;
    }
}
