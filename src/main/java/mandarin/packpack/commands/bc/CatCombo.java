package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.search.ComboFormMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.ComboMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

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

                replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("combo.failed.noCombo", lang).formatted(validateKeyword(getSearchKeywords(name, cName, lang)))));
            } else if(combos.size() == 1) {
                EntityHandler.showComboEmbed(ch, loader.getMessage(), combos.getFirst(), lang, false);
            } else {
                disableTimer();

                replyToMessageSafely(ch, loader.getMessage(), msg -> {
                    User u = loader.getUser();

                    StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, u.getId(), ch.getId(), msg, null, cName, config.searchLayout, lang));
                }, getContainer(combos.size(), null, cName, accumulateCombo(combos, SearchHolder.TextType.TEXT), accumulateCombo(combos, SearchHolder.TextType.LIST_LABEL), accumulateCombo(combos, SearchHolder.TextType.LIST_DESCRIPTION)));
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(name, false, lang);

            if(forms.isEmpty()) {
                disableTimer();

                replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("combo.failed.noCombo", lang).formatted(validateKeyword(getSearchKeywords(name, cName, lang)))));
            } else if(forms.size() == 1) {
                ArrayList<Combo> combos = EntityFilter.filterComboWithUnit(forms.getFirst(), cName, lang);

                if(combos.isEmpty()) {
                    disableTimer();

                    replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("combo.failed.noCombo", lang).formatted(validateKeyword(getSearchKeywords(name, cName, lang)))));
                } else if(combos.size() == 1) {
                    EntityHandler.showComboEmbed(ch, loader.getMessage(), combos.getFirst(), lang, false);
                } else {
                    disableTimer();

                    replyToMessageSafely(ch, loader.getMessage(), res -> {
                        if(res != null) {
                            User u = loader.getUser();

                            Message msg = loader.getMessage();

                            StaticStore.putHolder(u.getId(), new ComboMessageHolder(combos, msg, u.getId(), ch.getId(), res, name, cName, config.searchLayout, lang));
                        }
                    }, getContainer(combos.size(), name, cName, accumulateCombo(combos, SearchHolder.TextType.TEXT), accumulateCombo(combos, SearchHolder.TextType.LIST_LABEL), accumulateCombo(combos, SearchHolder.TextType.LIST_DESCRIPTION)));
                }
            } else {
                replyToMessageSafely(ch, loader.getMessage(), res -> {
                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        StaticStore.putHolder(u.getId(), new ComboFormMessageHolder(forms, msg, u.getId(), ch.getId(), res, lang, cName, name, config.searchLayout));
                    }
                }, getContainer(forms.size(), null, name, accumulateUnit(forms, SearchHolder.TextType.TEXT), accumulateUnit(forms, SearchHolder.TextType.LIST_LABEL), accumulateUnit(forms, SearchHolder.TextType.LIST_DESCRIPTION)));
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

    private Container getContainer(int dataSize, String unitName, String keyword, List<String> data, List<String> labels, List<String> descriptions) {
        int totalPage = Holder.getTotalPage(dataSize, config.searchLayout.chunkSize);

        List<ContainerChildComponent> children = new ArrayList<>();

        String summary;

        if (unitName == null) {
            summary = LangID.getStringByID("combo.search.comboOnly", lang).formatted(keyword, dataSize);
        } else {
            summary = LangID.getStringByID("combo.search.withUnit", lang).formatted(keyword, unitName, dataSize);
        }

        children.add(TextDisplay.of(summary));
        children.add(Separator.create(true, Separator.Spacing.LARGE));

        switch (config.searchLayout) {
            case FANCY_BUTTON -> {
                for (int i = 0; i < data.size(); i++) {
                    children.add(Section.of(Button.secondary(LangID.getStringByID("ui.button.select", lang), String.valueOf(i)), TextDisplay.of(data.get(i))));
                }
            }
            case FANCY_LIST -> {
                for (int i = 0; i < data.size(); i++) {
                    children.add(TextDisplay.of(data.get(i)));

                    if (i < data.size() - 1) {
                        children.add(Separator.create(false, Separator.Spacing.SMALL));
                    }
                }
            }
            case COMPACTED -> {
                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < data.size(); i++) {
                    builder.append(i + 1).append(". ").append(data.get(i));

                    if (i < data.size() - 1) {
                        builder.append("\n");
                    }
                }

                children.add(TextDisplay.of("```md\n" + builder + "\n```"));
            }
        }

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(TextDisplay.of(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)));

        if(dataSize > config.searchLayout.chunkSize) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            children.add(ActionRow.of(buttons));
        }

        if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED || config.searchLayout == ConfigHolder.SearchLayout.FANCY_LIST) {
            List<SelectOption> options = new ArrayList<>();

            for(int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                String description;

                if (descriptions == null) {
                    description = null;
                } else {
                    description = descriptions.get(i);
                }

                SelectOption option = SelectOption.of(label, String.valueOf(i));

                String[] elements = label.split("\\\\\\\\");

                if(elements.length == 2 && elements[0].matches("<:\\S+?:\\d+>")) {
                    option = option.withEmoji(Emoji.fromFormatted(elements[0])).withLabel(elements[1]);
                }

                if (description != null)
                    option = option.withDescription(description);

                options.add(option);
            }

            children.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectList", lang)).build()));
        }

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return Container.of(children);
    }
}
