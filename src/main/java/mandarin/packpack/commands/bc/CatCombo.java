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
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
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

        String unitName = getUnitName(loader.getContent());
        String comboName = getComboName(loader.getContent());

        if(unitName == null || unitName.isBlank()) {
            ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(null, comboName, lang);

            if(combos.isEmpty()) {
                disableTimer();

                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("combo.failed.noCombo", lang).formatted(validateKeyword(getSearchKeywords(unitName, comboName, lang))));
            } else if(combos.size() == 1) {
                EntityHandler.generateComboEmbed(ch, loader.getMessage(), combos.getFirst(), lang, false);
            } else {
                disableTimer();

                replyToMessageSafely(ch, loader.getMessage(), msg -> {
                    User u = loader.getUser();

                    StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, u.getId(), ch.getId(), msg, null, comboName, config.searchLayout, lang));
                }, getSearchComponents(combos.size(), LangID.getStringByID("combo.search.comboOnly", lang).formatted(comboName, combos.size()), combos, this::accumulateCombo, config.searchLayout, lang));
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(unitName, false, lang);

            if(forms.isEmpty()) {
                disableTimer();

                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("combo.failed.noCombo", lang).formatted(validateKeyword(getSearchKeywords(unitName, comboName, lang))));
            } else if(forms.size() == 1) {
                ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(forms.getFirst(), comboName, lang);

                if(combos.isEmpty()) {
                    disableTimer();

                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("combo.failed.noCombo", lang).formatted(validateKeyword(getSearchKeywords(unitName, comboName, lang))));
                } else if(combos.size() == 1) {
                    EntityHandler.generateComboEmbed(ch, loader.getMessage(), combos.getFirst(), lang, false);
                } else {
                    disableTimer();

                    replyToMessageSafely(ch, loader.getMessage(), res -> {
                        if(res != null) {
                            User u = loader.getUser();

                            Message msg = loader.getMessage();

                            StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, u.getId(), ch.getId(), res, unitName, comboName, config.searchLayout, lang));
                        }
                    }, getSearchComponents(combos.size(), LangID.getStringByID("combo.search.withUnit", lang).formatted(comboName, unitName, combos.size()), combos, this::accumulateCombo, config.searchLayout, lang));
                }
            } else {
                replyToMessageSafely(ch, loader.getMessage(), res -> {
                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        StaticStore.putHolder(u.getId(), new ComboFormMessageHolder(forms, msg, u.getId(), ch.getId(), res, lang, comboName, unitName, config.searchLayout));
                    }
                }, getSearchComponents(forms.size(), LangID.getStringByID("combo.search.withUnit", lang).formatted(comboName, unitName, forms.size()), forms, this::accumulateUnit, config.searchLayout, lang));
            }
        }
    }

    private String getUnitName(String message) {
        String[] contents = message.split(" +-u +", 2);

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

    private List<String> accumulateCombo(List<Combo> combos, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(combos.size(), config.searchLayout.chunkSize); i++) {
            Combo c = combos.get(i);

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(StaticStore.safeParseInt(c.name));

                        if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                            text += " " + MultiLangCont.getStatic().COMNAME.getCont(c, lang);

                        text += " | " + DataToString.getComboType(c, lang) + " ";

                        if(c.forms.length == 1) {
                            text += LangID.getStringByID("combo.slot.singular", lang);
                        } else {
                            text += String.format(LangID.getStringByID("combo.slot.plural", lang), c.forms.length);
                        }
                    } else {
                        text = "`" + Data.trio(StaticStore.safeParseInt(c.name)) + "`";

                        String comboName = StaticStore.safeMultiLangGet(c, lang);

                        if (comboName == null || comboName.isBlank())
                            comboName = Data.trio(StaticStore.safeParseInt(c.name));

                        text += " **" + comboName;

                        String slots;

                        if(c.forms.length == 1) {
                            slots = LangID.getStringByID("combo.slot.singular", lang);
                        } else {
                            slots = LangID.getStringByID("combo.slot.plural", lang).formatted(c.forms.length);
                        }

                        text += " " + slots + "**\n-# " + DataToString.getComboType(c, lang);
                    }
                }
                case LIST_LABEL -> {
                    text = Data.trio(StaticStore.safeParseInt(c.name));

                    String comboName = StaticStore.safeMultiLangGet(c, lang);

                    if (comboName != null && !comboName.isBlank())
                        text += " " + comboName;

                    String slots;

                    if(c.forms.length == 1) {
                        slots = LangID.getStringByID("combo.slot.singular", lang);
                    } else {
                        slots = LangID.getStringByID("combo.slot.plural", lang).formatted(c.forms.length);
                    }

                    text += " " + slots;
                }
                case LIST_DESCRIPTION -> text = DataToString.getComboType(c, lang);
            }

            data.add(text);
        }

        return data;
    }

    private List<String> accumulateUnit(List<Form> forms, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(forms.size(), config.searchLayout.chunkSize); i++) {
            if(i >= forms.size())
                break;

            Form f = forms.get(i);

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
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

    private String validateKeyword(String keyword) {
        if(keyword.length() > 1500)
            return keyword.substring(0, 1500) + "...";
        else
            return keyword;
    }
}
