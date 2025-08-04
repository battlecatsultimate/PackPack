package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.GlobalTimedConstraintCommand;
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
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EnemyGif extends GlobalTimedConstraintCommand {
    private static final int PARAM_DEBUG = 2;
    private static final int PARAM_RAW = 4;
    private static final int PARAM_GIF = 8;
    private static final int PARAM_TRANSPARENT = 16;

    public static final List<Integer> forbidden = new ArrayList<>();

    static {
        int[] data = {
            564, 565, 566, 567, 568, 644
        };

        for(int d : data)
            forbidden.add(d);
    }

    private final ConfigHolder config;

    public EnemyGif(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(30), false);

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
    protected void doThing(CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();

        boolean isTrusted = StaticStore.contributors.contains(u.getId()) || u.getId().equals(StaticStore.MANDARIN_SMELL);

        String[] list = loader.getContent().split(" ");

        if(list.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("enemyImage.fail.noParameter", lang)));

            disableTimer();

            return;
        }

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/EnemyGif::doThing - Can't create folder : " + temp.getAbsolutePath());

            return;
        }

        String search = filterCommand(loader.getContent());

        if(search.isBlank()) {
            replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("enemyImage.fail.noParameter", lang)));

            disableTimer();

            return;
        }

        ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(search, lang);

        if(enemies.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(loader.getContent()))));

            disableTimer();
        } else if(enemies.size() == 1) {
            Enemy enemy = enemies.getFirst();

            int param = checkParameters(loader.getContent());
            int mode = getMode(loader.getContent());
            boolean debug = (param & PARAM_DEBUG) > 0;
            boolean raw = (param & PARAM_RAW) > 0;
            boolean gif = (param & PARAM_GIF) > 0;
            boolean transparent = (param & PARAM_TRANSPARENT) > 0;
            int frame = getFrame(loader.getContent());

            if(forbidden.contains(enemy.id.id)) {
                replyToMessageSafely(ch, loader.getMessage(), TextDisplay.of(LangID.getStringByID("data.animation.gif.dummy", lang)));

                return;
            }

            StringBuilder primary = new StringBuilder();

            if(raw && !isTrusted) {
                primary.append(LangID.getStringByID("data.animation.gif.ignoring", lang)).append("\n\n");
            }

            int boostLevel = 0;

            if (ch instanceof GuildChannel) {
                boostLevel = loader.getGuild().getBoostTier().getKey();
            }

            EntityHandler.generateEnemyAnim(enemy, ch, loader.getMessage(), primary, boostLevel, mode, transparent, debug, frame, lang, raw && isTrusted, gif, () -> {
                if(!StaticStore.conflictedAnimation.isEmpty()) {
                    StaticStore.logger.uploadLog("Warning - Bot generated animation while this animation is already cached\n\nCommand : " + loader.getContent());

                    StaticStore.conflictedAnimation.clear();
                }

                if(raw && isTrusted) {
                    StaticStore.logger.uploadLog("Generated mp4 by user " + u.getName() + " for enemy ID " + Data.trio(enemy.id.id) + " with mode of " + mode);

                    changeTime(TimeUnit.MINUTES.toMillis(1));
                }
            }, () -> {
                if(!StaticStore.conflictedAnimation.isEmpty()) {
                    StaticStore.logger.uploadLog("Warning - Bot generated animation while this animation is already cached\n\nCommand : " + loader.getContent());

                    StaticStore.conflictedAnimation.clear();
                }

                disableTimer();
            });
        } else {
            replyToMessageSafely(ch, loader.getMessage(), res -> {
                int param = checkParameters(loader.getContent());
                int mode = getMode(loader.getContent());
                int frame = getFrame(loader.getContent());

                boolean debug = (param & PARAM_DEBUG) > 0;
                boolean raw = (param & PARAM_RAW) > 0;
                boolean gif = (param & PARAM_GIF) > 0;
                boolean transparent = (param & PARAM_TRANSPARENT) > 0;

                StringBuilder primary = new StringBuilder();

                if(raw && !isTrusted) {
                    primary.append(LangID.getStringByID("data.animation.gif.ignoring", lang)).append("\n\n");
                }

                StaticStore.putHolder(u.getId(), new EnemyAnimMessageHolder(enemies, loader.getMessage(), u.getId(), ch.getId(), res, primary, search, config.searchLayout, mode, frame, transparent, debug, lang, true, raw && isTrusted, gif));
            }, getComponents(enemies, enemies.size(), search));

            disableTimer();
        }
    }

    @Override
    protected void setOptionalID(CommandLoader loader) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }

    private int getMode(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-m") || msg[i].equals("-mode")) {
                if(i < msg.length - 1) {
                    if(LangID.getStringByID("data.animation.mode.walk", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 0;
                    else if(LangID.getStringByID("data.animation.mode.idle", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 1;
                    else if(LangID.getStringByID("data.animation.mode.attack", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 2;
                    else if(LangID.getStringByID("data.animation.mode.kb", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 3;
                    else if(LangID.getStringByID("data.animation.mode.enter", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowDown", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowMove", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 5;
                    else if(LangID.getStringByID("data.animation.mode.burrowUp", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 6;
                } else {
                    return 0;
                }
            }
        }

        return 0;
    }

    private int getFrame(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-f") || msg[i].equals("-fr")) {
                if(i < msg.length - 1 && StaticStore.isNumeric(msg[i+1])) {
                    return StaticStore.safeParseInt(msg[i+1]);
                }
            }
        }

        return -1;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for(int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-d", "-debug" -> {
                        if ((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                    }
                    case "-r", "-raw" -> {
                        if ((result & PARAM_RAW) == 0) {
                            result |= PARAM_RAW;
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
                    case "-g", "-gif" -> {
                        if ((result & PARAM_GIF) == 0) {
                            result |= PARAM_GIF;
                        } else {
                            break label;
                        }
                    }
                    case "-t" -> {
                        if ((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
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

        if(contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean debug = false;
        boolean raw = false;

        boolean mode = false;
        boolean frame = false;
        boolean gif = false;
        boolean transparent = false;

        for(int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-debug", "-d" -> {
                    if (!debug) {
                        debug = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-r", "-raw" -> {
                    if (!raw) {
                        raw = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-mode", "-m" -> {
                    if (!mode && i < contents.length - 1) {
                        mode = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-fr", "-f" -> {
                    if (!frame && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                        frame = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-g", "-gif" -> {
                    if (!gif) {
                        gif = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-t" -> {
                    if (!transparent) {
                        transparent = true;
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

            if(written && i < contents.length - 1)
                result.append(" ");
        }

        return result.toString().trim();
    }

    private List<String> accumulateTextData(List<Enemy> enemies, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for (int i = 0; i < config.searchLayout.chunkSize; i++) {
            if (i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

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

    private String getSearchKeyword(String command) {
        String result = filterCommand(command);

        if(result == null)
            return "";

        if(result.length() > 1500)
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
