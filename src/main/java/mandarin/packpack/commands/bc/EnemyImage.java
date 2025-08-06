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
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.search.EnemyAnimMessageHolder;
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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnemyImage extends TimedConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    private final ConfigHolder config;

    public EnemyImage(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_ENEMYIMAGE_ID, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ");

        if (list.length >= 2) {
            File temp = new File("./temp");

            if (!temp.exists()) {
                boolean res = temp.mkdirs();

                if (!res) {
                    System.out.println("Can't create folder : " + temp.getAbsolutePath());
                    return;
                }
            }

            String search = filterCommand(loader.getContent());

            if (search.isBlank()) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("enemyImage.fail.noParameter", lang));

                return;
            }

            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(search, lang);

            if (enemies.isEmpty()) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(loader.getContent())));

                disableTimer();
            } else if (enemies.size() == 1) {
                int param = checkParameters(loader.getContent());
                int mode = getMode(loader.getContent());
                int frame = getFrame(loader.getContent());

                EntityHandler.generateEnemyImage(enemies.getFirst(), ch, loader.getMessage(), (param & PARAM_DEBUG) > 0, (param & PARAM_TRANSPARENT) > 0, frame, mode, lang);
            } else {
                replyToMessageSafely(ch, loader.getMessage(), msg -> {
                    int param = checkParameters(loader.getContent());
                    int mode = getMode(loader.getContent());
                    int frame = getFrame(loader.getContent());

                    User u = loader.getUser();

                    StaticStore.putHolder(u.getId(), new EnemyAnimMessageHolder(enemies, loader.getMessage(), u.getId(), ch.getId(), msg, new StringBuilder(), search, config.searchLayout, mode, frame, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0), lang, false, false, false));
                }, getComponents(enemies, enemies.size(), search));

                disableTimer();
            }
        } else {
            ch.sendMessage(LangID.getStringByID("enemyImage.fail.noParameter", lang)).queue();
            disableTimer();
        }
    }

    private int getMode(String message) {
        String[] msg = message.split(" ");

        for (int i = 0; i < msg.length; i++) {
            if (msg[i].equals("-m") || msg[i].equals("-mode")) {
                if (i < msg.length - 1) {
                    if (LangID.getStringByID("data.animation.mode.walk", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 0;
                    else if (LangID.getStringByID("data.animation.mode.idle", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 1;
                    else if (LangID.getStringByID("data.animation.mode.attack", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 2;
                    else if (LangID.getStringByID("data.animation.mode.kb", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 3;
                    else if (LangID.getStringByID("data.animation.mode.enter", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if (LangID.getStringByID("data.animation.mode.burrowDown", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if (LangID.getStringByID("data.animation.mode.burrowMove", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 5;
                    else if (LangID.getStringByID("data.animation.mode.burrowUp", lang).toLowerCase(Locale.ENGLISH).contains(msg[i + 1].toLowerCase(Locale.ENGLISH)))
                        return 6;
                } else {
                    return 0;
                }
            }
        }

        return 0;
    }

    private int getFrame(String message) {
        String[] msg = message.split(" ");

        for (int i = 0; i < msg.length; i++) {
            if (msg[i].equals("-f") || msg[i].equals("-fr")) {
                if (i < msg.length - 1 && StaticStore.isNumeric(msg[i + 1])) {
                    return StaticStore.safeParseInt(msg[i + 1]);
                }
            }
        }

        return 0;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if (msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for (int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-t" -> {
                        if ((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
                        } else {
                            break label;
                        }
                    }
                    case "-d", "-debug" -> {
                        if ((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                    }
                    case "-f", "-fr" -> {
                        if (i < pureMessage.length - 1 && StaticStore.isNumeric(pureMessage[i + 1])) {
                            i++;
                        } else {
                            break label;
                        }
                    }
                    case "-m", "-mode" -> {
                        if (i < pureMessage.length - 1) {
                            i++;
                        } else {
                            break label;
                        }
                    }
                }
            }
        }

        return result;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if (contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean debug = false;
        boolean trans = false;

        boolean mode = false;
        boolean frame = false;

        for (int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-t" -> {
                    if (!trans) {
                        trans = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-d", "-debug" -> {
                    if (!debug) {
                        debug = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-m", "-mode" -> {
                    if (!mode && i < contents.length - 1) {
                        mode = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-f", "-fr" -> {
                    if (!frame && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                        frame = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                default -> {
                    result.append(contents[i]);
                    written = true;
                }
            }

            if (written && i < contents.length - 1) {
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    private String getSearchKeyword(String command) {
        String result = filterCommand(command);

        if (result == null)
            return "";

        if (result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }

    private List<String> accumulateTextData(List<Enemy> enemies, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for (int i = 0; i < Math.min(enemies.size(), config.searchLayout.chunkSize); i++) {
            Enemy e = enemies.get(i);

            if (e.id == null)
                continue;

            String text = null;

            switch (textType) {
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

    private Container getComponents(List<Enemy> enemies, int dataSize, String keyword) {
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

        if (dataSize > config.searchLayout.chunkSize) {
            List<Button> buttons = new ArrayList<>();

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            children.add(ActionRow.of(buttons));
        }

        if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED || config.searchLayout == ConfigHolder.SearchLayout.FANCY_LIST) {
            List<SelectOption> options = new ArrayList<>();

            List<String> labels = accumulateTextData(enemies, SearchHolder.TextType.LIST_LABEL);
            List<String> descriptions = accumulateTextData(enemies, SearchHolder.TextType.LIST_DESCRIPTION);

            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                String description;

                description = descriptions.get(i);

                SelectOption option = SelectOption.of(label, String.valueOf(i));

                String[] elements = label.split("\\\\\\\\");

                if (elements.length == 2 && elements[0].matches("<:\\S+?:\\d+>")) {
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
