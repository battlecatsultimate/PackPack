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
import mandarin.packpack.supporter.server.holder.component.search.EnemyStatMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.slash.SlashOption;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnemyStat extends ConstraintCommand {
    public static void performInteraction(GenericCommandInteractionEvent interaction) {
        if(interaction.getOptions().isEmpty()) {
            StaticStore.logger.uploadLog("W/EnemyStat::performInteraction - Options are absent!");
            return;
        }

        CommonStatic.Lang.Locale lang = CommonStatic.Lang.Locale.EN;

        User u = interaction.getUser();

        if(StaticStore.config.containsKey(u.getId())) {
            lang =  StaticStore.config.get(u.getId()).lang;

            if(lang == null) {
                if(interaction.getGuild() == null) {
                    lang = CommonStatic.Lang.Locale.EN;
                } else {
                    IDHolder idh = StaticStore.idHolder.get(interaction.getGuild().getId());

                    if(idh == null) {
                        lang = CommonStatic.Lang.Locale.EN;
                    } else {
                        lang = idh.config.lang;
                    }
                }
            }
        }

        List<OptionMapping> options = interaction.getOptions();

        String name = SlashOption.getStringOption(options, "name", "");

        Enemy e = name.isBlank() ? null : EntityFilter.pickOneEnemy(name, lang);

        boolean frame = SlashOption.getBooleanOption(options, "frame", true);
        boolean extra = SlashOption.getBooleanOption(options, "extra", false);

        int[] magnification = {
                SlashOption.getIntOption(options, "magnification", 100),
                SlashOption.getIntOption(options, "atk_magnification", 100)
        };

        final CommonStatic.Lang.Locale finalLang = lang;

        if(e == null) {
            interaction.deferReply().setAllowedMentions(new ArrayList<>()).setContent(LangID.getStringByID("ui.search.moreSpecific", finalLang)).queue();
        } else {
            try {
                EntityHandler.performEnemyEmb(e, interaction, frame, extra, magnification, StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global), finalLang);
            } catch (Exception exception) {
                StaticStore.logger.uploadErrorLog(exception, "E/EnemyStat::performInteraction - Failed to show enemy embed");
            }
        }
    }

    private static final int PARAM_SECOND = 2;
    private static final int PARAM_EXTRA = 4;
    private static final int PARAM_COMPACT = 8;
    private static final int PARAM_FRAME = 16;

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
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ", 2);
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

        if(list.length == 1 || filterCommand(command).isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("formStat.fail.noName", lang), loader.getMessage(), a -> a);
        } else {
            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(filterCommand(command), lang);

            if(enemies.size() == 1) {
                int param = checkParameters(command);

                int[] magnification = handleMagnification(command);

                boolean isFrame;

                if ((param & PARAM_SECOND) > 0)
                    isFrame = false;
                else if ((param & PARAM_FRAME) > 0)
                    isFrame = true;
                else
                    isFrame = config.useFrame;

                boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
                boolean isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);

                Message m = loader.getMessage();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(m.getAuthor().getId(), TreasureHolder.global);

                EntityHandler.showEnemyEmb(enemies.getFirst(), ch, m, isFrame, isExtra, isCompact, magnification, treasure, lang);
            } else if(enemies.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("enemyStat.fail.noEnemy", lang).replace("_", getSearchKeyword(command)), loader.getMessage(), a -> a);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("ui.search.severalResult", lang).replace("_", getSearchKeyword(command)));

                sb.append("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateData(enemies);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(enemies.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = enemies.size() / SearchHolder.PAGE_CHUNK;

                    if(enemies.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, enemies.size(), data, lang), res -> {
                    int[] magnification = handleMagnification(command);

                    int param = checkParameters(command);

                    boolean isFrame;

                    if ((param & PARAM_SECOND) > 0)
                        isFrame = false;
                    else if ((param & PARAM_FRAME) > 0)
                        isFrame = true;
                    else
                        isFrame = config.useFrame;

                    boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
                    boolean isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);

                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                        StaticStore.putHolder(u.getId(), new EnemyStatMessageHolder(enemies, msg, res, ch.getId(), magnification, isFrame, isExtra, isCompact, treasure, lang));
                    }
                });
            }
        }
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isFrame = false;
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
                case "-f", "-fr" -> {
                    if (!isFrame)
                        isFrame = true;
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
                    case "-e", "-extra" -> {
                        if ((result & PARAM_EXTRA) == 0) {
                            result |= PARAM_EXTRA;
                        } else {
                            break label;
                        }
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
                        return new int[] {100};
                    else {
                        int[] lv = new int[length];

                        for (int j = 0; j < length; j++) {
                            if(trial[j].isBlank() || !StaticStore.isNumeric(trial[j].replace("%", ""))) {
                                lv[j] = 100;
                            } else {
                                lv[j] = StaticStore.safeParseInt(trial[j].replace("%", ""));
                            }
                        }

                        return lv;
                    }
                }
            }
        } else {
            return new int[] {100};
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

    private List<String> accumulateData(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
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

    private String getSearchKeyword(String command) {
        String result = filterCommand(command);

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }
}
