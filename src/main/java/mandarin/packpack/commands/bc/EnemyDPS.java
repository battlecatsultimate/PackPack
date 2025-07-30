package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.search.EnemyDPSHolder;
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
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class EnemyDPS extends TimedConstraintCommand {
    private final ConfigHolder config;

    public EnemyDPS(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_ENEMYDPS_ID, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() throws Exception {
        super.prepare();

        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String name;
        int magnification;

        if (loader.fromMessage) {
            String[] segments = loader.getContent().split(" ");

            StringBuilder removeMistake = new StringBuilder();

            for(int i = 0; i < segments.length; i++) {
                if(segments[i].matches("-m(\\d+(,|%,|%$)?)+")) {
                    removeMistake.append("-m ").append(segments[i].replace("-m", ""));
                } else {
                    removeMistake.append(segments[i]);
                }

                if(i < segments.length - 1)
                    removeMistake.append(" ");
            }

            String command = removeMistake.toString();

            name = filterCommand(command);
            magnification = handleMagnification(command);
        } else {
            name = loader.getOptions().getOption("name", "");
            magnification = loader.getOptions().getOption("magnification", 100);
        }

        if(name.isBlank()) {
            if (loader.fromMessage) {
                replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("formStat.fail.noName", lang)));
            } else {
                replyToMessageSafely(loader.getInteractionEvent(), TextDisplay.of(LangID.getStringByID("formStat.fail.noName", lang)));
            }
        } else {
            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(name, lang);

            if(enemies.size() == 1) {
                Message m = loader.getNullableMessage();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getUser().getId(), TreasureHolder.global);

                Object sender = loader.fromMessage ? ch : loader.getInteractionEvent();

                EntityHandler.showEnemyDPS(sender, m, enemies.getFirst(), treasure, magnification, false, false, lang);
            } else if(enemies.isEmpty()) {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(name))));
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), TextDisplay.of(LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(name))));
                }
            } else {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, loader.getMessage(), msg -> {
                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getUser().getId(), TreasureHolder.global);

                        StaticStore.putHolder(loader.getUser().getId(), new EnemyDPSHolder(enemies, loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, name, config.searchLayout, treasure, magnification, lang));
                    }, getComponents(enemies.size(), name, enemies));
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), msg -> {
                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getUser().getId(), TreasureHolder.global);

                        StaticStore.putHolder(loader.getUser().getId(), new EnemyDPSHolder(enemies, loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, name, config.searchLayout, treasure, magnification, lang));
                    }, getComponents(enemies.size(), name, enemies));
                }
            }
        }
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isExtra = false;
        boolean isLevel = false;
        boolean isCompact = false;

        StringBuilder command = new StringBuilder();

        for(int i = 1; i < content.length; i++) {
            boolean written = false;

            switch (content[i]) {
                case "-s" -> {
                    if (!isSec)
                        isSec = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-e", "-extra" -> {
                    if (!isExtra)
                        isExtra = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-m" -> {
                    if (!isLevel && i < content.length - 1) {
                        String text = getLevelText(content, i + 1);

                        if (text.contains(" ")) {
                            i += text.split(" ").length;
                        } else if (msg.endsWith(text)) {
                            i++;
                        }

                        isLevel = true;
                    } else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-c", "-compact" -> {
                    if (!isCompact)
                        isCompact = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                default -> {
                    command.append(content[i]);
                    written = true;
                }
            }

            if(written && i < content.length - 1) {
                command.append(" ");
            }
        }

        if(command.toString().isBlank())
            return "";

        return command.toString().trim();
    }

    private int handleMagnification(String msg) {
        if(msg.contains("-m")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if(content[i].equals("-m") && i != content.length - 1 && StaticStore.isNumeric(content[i + 1])) {
                    return StaticStore.safeParseInt(content[i + 1]);
                }
            }
        }

        return 100;
    }

    private String getLevelText(String[] trial, int index) {
        StringBuilder sb = new StringBuilder();

        for(int i = index; i < trial.length; i++) {
            sb.append(trial[i]);

            if(i != trial.length - 1)
                sb.append(" ");
        }

        StringBuilder fin = new StringBuilder();

        boolean commaStart = false;
        boolean beforeSpace = false;
        boolean percentage = false;
        boolean numberStart = false;
        int numberLetter = 0;
        int commaAdd = 0;

        for(int i = 0; i < sb.length(); i++) {
            if(sb.charAt(i) == ',') {
                if(!commaStart && commaAdd <= 1) {
                    commaStart = true;
                    numberStart = false;
                    commaAdd++;
                    fin.append(sb.charAt(i));
                    numberLetter++;
                } else {
                    break;
                }
            } else if(sb.charAt(i) == ' ') {
                beforeSpace = true;
                numberLetter = 0;
                fin.append(sb.charAt(i));
            } else if(sb.charAt(i) == '%') {
                if(!percentage && numberStart) {
                    percentage = true;
                    numberStart = false;
                    fin.append(sb.charAt(i));
                    numberLetter++;
                } else {
                    break;
                }
            } else {
                if(Character.isDigit(sb.charAt(i))) {
                    numberStart = true;
                    commaStart = false;
                    percentage = false;
                    fin.append(sb.charAt(i));
                    numberLetter++;
                } else if(beforeSpace) {
                    numberLetter = 0;
                    break;
                } else {
                    break;
                }

                beforeSpace = false;
            }

            if(i == sb.length() - 1)
                numberLetter = 0;
        }

        String result = fin.toString();

        result = result.substring(0, result.length() - numberLetter);

        return result;
    }

    private List<String> accumulateData(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < ConfigHolder.SearchLayout.COMPACTED.chunkSize; i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String ename;

            if(e.id != null) {
                ename = Data.trio(e.id.id)+" ";
            } else {
                ename = " ";
            }

            String name = StaticStore.safeMultiLangGet(e, lang);

            if(name != null)
                ename += name;

            data.add(ename);
        }

        return data;
    }

    public List<String> accumulateTextData(List<Enemy> enemies, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(enemies.size(), config.searchLayout.chunkSize); i++) {
            Enemy e = enemies.get(i);

            if (e.id == null)
                continue;

            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(e.id.id);

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(e.id.id) + "`";

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(e.id.id);
                        }

                        text += " " + name;
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(e, lang);

                    if (text == null) {
                        text = Data.trio(e.id.id);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(e.id.id);
            }

            data.add(text);
        }

        return data;
    }

    private String getSearchKeyword(String name) {
        String result = name;

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }

    private Container getComponents(int dataSize, String keyword, List<Enemy> enemies) {
        int totalPage = Holder.getTotalPage(dataSize, config.searchLayout.chunkSize);

        List<ContainerChildComponent> children = new ArrayList<>();
        List<String> data = accumulateTextData(enemies, SearchHolder.TextType.TEXT);

        children.add(TextDisplay.of(LangID.getStringByID("ui.search.severalResult", lang).formatted(keyword, dataSize)));
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

            List<String> labels = accumulateTextData(enemies, SearchHolder.TextType.LIST_LABEL);
            List<String> descriptions = accumulateTextData(enemies, SearchHolder.TextType.LIST_DESCRIPTION);

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
