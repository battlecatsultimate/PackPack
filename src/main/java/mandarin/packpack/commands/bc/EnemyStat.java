package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.EnemyButtonHolder;
import mandarin.packpack.supporter.server.holder.component.search.EnemyStatMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.slash.SlashOptionMap;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class EnemyStat extends ConstraintCommand {
    public static class EnemyStatConfig {
        public boolean isFrame;
        public boolean isCompact;
        public boolean showEnemyDescription;

        public int[] magnification;
    }

    private static final int PARAM_SECOND = 2;
    private static final int PARAM_COMPACT = 4;
    private static final int PARAM_FRAME = 8;

    private final ConfigHolder config;

    public EnemyStat(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, ConfigHolder config) {
        super(role, lang, id, false);

        if(config == null)
            this.config = id == null ? StaticStore.defaultConfig : id.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        EnemyStatConfig configData = new EnemyStatConfig();
        String enemyName;

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

            enemyName = filterCommand(command);

            int param = checkParameters(command);

            configData.magnification = handleMagnification(command);

            if ((param & PARAM_SECOND) > 0)
                configData.isFrame = false;
            else if ((param & PARAM_FRAME) > 0)
                configData.isFrame = true;
            else
                configData.isFrame = config.useFrame;

            configData.isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);
        } else {
            SlashOptionMap optionMap = loader.getOptions();

            enemyName = optionMap.getOption("name", "");

            configData.magnification = new int[] {
                    optionMap.getOption("magnification", 100),
                    optionMap.getOption("atk_magnification", 100)
            };

            configData.isFrame = optionMap.getOption("frame", config.useFrame);
            configData.isCompact = optionMap.getOption("compact", ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact));
        }

        configData.showEnemyDescription = config.showEnemyDescription;

        if (enemyName.isBlank()) {
            if (loader.fromMessage) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("formStat.fail.noName", lang));
            } else {
                replyToMessageSafely(loader.getInteractionEvent(), TextDisplay.of(LangID.getStringByID("formStat.fail.noName", lang)));
            }
        } else {
            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(enemyName, lang);

            if(enemies.size() == 1) {
                Object sender;

                if (loader.fromMessage) {
                    sender = ch;
                } else {
                    sender = loader.getInteractionEvent();
                }

                Message m = loader.getNullableMessage();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getUser().getId(), TreasureHolder.global);

                EntityHandler.showEnemyEmb(enemies.getFirst(), sender, m, treasure, configData, false, lang, msg -> StaticStore.putHolder(loader.getUser().getId(), new EnemyButtonHolder(m, loader.getUser().getId(), ch.getId(), msg, enemies.getFirst(), treasure, configData, lang)));
            } else if(enemies.isEmpty()) {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(enemyName)));
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(enemyName)));
                }
            } else {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, loader.getMessage(), msg -> {
                        User u = loader.getUser();

                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                        StaticStore.putHolder(u.getId(), new EnemyStatMessageHolder(enemies, loader.getMessage(), u.getId(), ch.getId(), msg, enemyName, config.searchLayout, treasure, configData, lang));
                    }, getSearchComponents(enemies.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(enemyName, enemies.size()), enemies, this::accumulateTextData, config.searchLayout, lang));
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), msg -> {
                        User u = loader.getUser();

                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                        StaticStore.putHolder(u.getId(), new EnemyStatMessageHolder(enemies, loader.getNullableMessage(), u.getId(), ch.getId(), msg, enemyName, config.searchLayout, treasure, configData, lang));
                    }, getSearchComponents(enemies.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(enemyName, enemies.size()), enemies, this::accumulateTextData, config.searchLayout, lang));
                }
            }
        }
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isFrame = false;
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
                case "-f", "-fr" -> {
                    if (!isFrame)
                        isFrame = true;
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

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            label:
            for(String str : pureMessage) {
                switch (str) {
                    case "-s" -> {
                        if ((result & PARAM_SECOND) == 0) {
                            result |= PARAM_SECOND;
                        } else
                            break label;
                    }
                    case "-c", "-compact" -> {
                        if ((result & PARAM_COMPACT) == 0) {
                            result |= PARAM_COMPACT;
                        } else {
                            break label;
                        }
                    }
                    case "-f", "-fr" -> {
                        if ((result & PARAM_FRAME) == 0) {
                            result |= PARAM_FRAME;
                        } else {
                            break label;
                        }
                    }
                }
            }
        }

        return result;
    }

    private int[] handleMagnification(String msg) {
        if(msg.contains("-m")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if(content[i].equals("-m") && i != content.length -1) {
                    String[] trial = getLevelText(content, i+1).replace(" ", "").split(",");

                    int length = 0;

                    for (String s : trial) {
                        if (StaticStore.isNumeric(s.replace("%", "")))
                            length++;
                        else
                            break;
                    }

                    if(length == 0)
                        return new int[] { 100, 100 };
                    else {
                        int[] lv = new int[2];

                        for (int j = 0; j < Math.min(2, length); j++) {
                            if(trial[j].isBlank() || !StaticStore.isNumeric(trial[j].replace("%", ""))) {
                                lv[j] = 100;
                            } else {
                                lv[j] = StaticStore.safeParseInt(trial[j].replace("%", ""));
                            }
                        }

                        if (length == 1) {
                            lv[1] = lv[0];
                        }

                        return lv;
                    }
                }
            }
        } else {
            return new int[] { 100, 100 };
        }

        return new int[] {100};
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

    private String getSearchKeyword(String name) {
        String result = name;

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }

    public List<String> accumulateTextData(List<Enemy> enemies, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for (int i = 0; i < Math.min(enemies.size(), config.searchLayout.chunkSize); i++) {
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
}
