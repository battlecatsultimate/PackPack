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
import mandarin.packpack.supporter.server.holder.component.search.EnemySpriteMessageHolder;
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
import java.util.ArrayList;
import java.util.List;

public class EnemySprite extends TimedConstraintCommand {
    private static final int PARAM_EDI = 2;

    private final ConfigHolder config;

    public EnemySprite(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_ENEMYSPRITE_ID, false);

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

        String[] contents = loader.getContent().split(" ");

        if (contents.length == 1) {
            replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("enemyImage.fail.noParameter", lang)));
        } else {
            String search = filterCommand(loader.getContent());

            if (search.isBlank()) {
                replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("enemyImage.fail.noParameter", lang)));

                return;
            }

            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(search, lang);

            if (enemies.isEmpty()) {
                replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(loader.getContent()))));

                disableTimer();
            } else if (enemies.size() == 1) {
                int param = checkParameter(loader.getContent());

                EntityHandler.generateEnemySprite(enemies.getFirst(), ch, loader.getMessage(), getModeFromParam(param), lang);
            } else {
                int param = checkParameter(loader.getContent());

                int mode = getModeFromParam(param);

                replyToMessageSafely(ch, loader.getMessage(), msg -> {
                    User u = loader.getUser();

                    StaticStore.putHolder(u.getId(), new EnemySpriteMessageHolder(enemies, loader.getMessage(), u.getId(), ch.getId(), msg, search, config.searchLayout, mode, lang));
                }, getComponents(enemies, enemies.size(), search));

                disableTimer();
            }
        }
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        int res = 1;

        for (String content : contents) {
            if ("-edi".equals(content)) {
                res |= PARAM_EDI;
                break;
            }
        }

        return res;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if (contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean edi = false;

        for (int i = 1; i < contents.length; i++) {
            boolean written = false;

            if (contents[i].equals("-edi")) {
                if (!edi) {
                    edi = true;
                } else {
                    result.append(contents[i]);
                    written = true;
                }
            } else {
                result.append(contents[i]);
                written = true;
            }

            if (written && i < contents.length - 1)
                result.append(" ");
        }

        return result.toString().trim();
    }

    private int getModeFromParam(int param) {
        if ((param & PARAM_EDI) > 0)
            return 3;
        else
            return 0;
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

    private String getSearchKeyword(String command) {
        String result = filterCommand(command);

        if (result == null)
            return "";

        if (result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
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
