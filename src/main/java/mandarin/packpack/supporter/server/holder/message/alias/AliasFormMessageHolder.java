package mandarin.packpack.supporter.server.holder.message.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AliasFormMessageHolder extends SearchHolder {
    private final ArrayList<Form> forms;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    public AliasFormMessageHolder(ArrayList<Form> forms, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message msg, AliasHolder.MODE mode, CommonStatic.Lang.Locale lang, @Nonnull String keyword, @Nullable String aliasName) {
        super(author, userID, channelID, msg, keyword, ConfigHolder.SearchLayout.FANCY_LIST, lang);

        this.forms = forms;
        this.mode = mode;
        this.aliasName = aliasName;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page +1); i++) {
            if(i >= forms.size())
                break;

            Form f = forms.get(i);

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
        String fname = StaticStore.safeMultiLangGet(forms.get(index), lang);

        if(fname == null || fname.isBlank())
            fname = forms.get(index).names.toString();

        if(fname.isBlank())
            fname = Data.trio(forms.get(index).unit.id.id)+"-"+Data.trio(forms.get(index).fid);

        ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.FORM, lang, forms.get(index));

        switch (mode) {
            case GET -> {
                if (alias == null || alias.isEmpty()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.noAlias.unit", lang).formatted(fname)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
                } else {
                    StringBuilder result = new StringBuilder(LangID.getStringByID("alias.aliases.unit", lang).formatted(fname, alias.size()));
                    result.append("\n\n");

                    for (int i = 0; i < alias.size(); i++) {
                        String temp = "- " + alias.get(i);

                        if (result.length() + temp.length() > 1900) {
                            result.append("\n").append(LangID.getStringByID("alias.etc", lang));

                            break;
                        }

                        result.append(temp);

                        if (i < alias.size() - 1)
                            result.append("\n");
                    }

                    event.deferEdit()
                            .setComponents(TextDisplay.of(result.toString()))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
                }
            }
            case ADD -> {
                if (alias == null)
                    alias = new ArrayList<>();

                if (aliasName.isBlank()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.noName", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                if (alias.contains(aliasName)) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.contain", lang).formatted(fname)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                alias.add(aliasName);

                AliasHolder.FALIAS.put(lang, forms.get(index), alias);

                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("alias.added", lang).formatted(fname, aliasName)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                StaticStore.logger.uploadLog("Alias added\n\nUnit : " + fname + "\nAlias : " + aliasName + "\nBy : <@" + userID + ">");
            }
            case REMOVE -> {
                if (alias == null || alias.isEmpty()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.noAlias.unit", lang).formatted(fname)))
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                if (aliasName.isBlank()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.noName", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                if (!alias.contains(aliasName)) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.removeFail", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                alias.remove(aliasName);
                AliasHolder.FALIAS.put(lang, forms.get(index), alias);

                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("alias.removed", lang).formatted(fname, aliasName)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                StaticStore.logger.uploadLog("Alias removed\n\nUnit : " + fname + "\nAlias : " + aliasName + "\nBy : <@" + userID + ">");
            }
        }
    }

    @Override
    public int getDataSize() {
        return forms.size();
    }

    @Override
    public void onExpire() {
        message.editMessageComponents(TextDisplay.of(LangID.getStringByID("ui.search.expired", lang)))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }
}
