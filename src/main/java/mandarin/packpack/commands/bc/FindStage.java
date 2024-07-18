package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.CastleList;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
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
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import mandarin.packpack.supporter.server.holder.component.search.FindStageMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.component.search.StageEnemyMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class FindStage extends TimedConstraintCommand {
    public enum MONTHLY {
        ALL,
        EOC,
        ITF1,
        ITF2,
        ITF3,
        COTC1,
        COTC2,
        COTC3,
        SOL,
        CYCLONE
    }

    private static final int PARAM_SECOND = 2;
    private static final int PARAM_EXTRA = 4;
    private static final int PARAM_COMPACT = 8;
    private static final int PARAM_OR = 16;
    private static final int PARAM_AND = 32;
    private static final int PARAM_BOSS = 64;
    private static final int PARAM_MONTHLY = 128;
    private static final int PARAM_FRAME = 256;

    private final ConfigHolder config;

    public FindStage(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, ConfigHolder config, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_FINDSTAGE_ID, false);

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

        String[] segments = loader.getContent().split(" ");

        StringBuilder removeMistake = new StringBuilder();

        for(int i = 0; i < segments.length; i++) {
            if(segments[i].matches("-lv(l)?(\\d+(,)?)+")) {
                removeMistake.append("-lv ").append(segments[i].replace("-lvl", "").replace("-lv", ""));
            } else {
                removeMistake.append(segments[i]);
            }

            if(i < segments.length - 1)
                removeMistake.append(" ");
        }

        String command = removeMistake.toString();

        String enemyName = getEnemyName(command);

        int param = checkParameters(command);
        int star = getLevel(command);
        int music = getMusic(command);
        int castle = getCastle(command);
        int background = getBackground(command);

        boolean isFrame;

        if ((param & PARAM_SECOND) > 0)
            isFrame = false;
        else if ((param & PARAM_FRAME) > 0)
            isFrame = true;
        else
            isFrame = config.useFrame;

        boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
        boolean isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);
        boolean orOperate = (param & PARAM_OR) > 0 && (param & PARAM_AND) == 0;
        boolean hasBoss = (param & PARAM_BOSS) > 0;
        boolean monthly = (param & PARAM_MONTHLY) > 0;

        if(enemyName.isBlank() && music < 0 && castle < 0 && background < 0 && !hasBoss) {
            replyToMessageSafely(ch, LangID.getStringByID("fstage_noparam", lang), loader.getMessage(), a -> a);

            return;
        }

        if(background >= 0 && UserProfile.getBCData().bgs.get(background) == null) {
            replyToMessageSafely(ch, LangID.getStringByID("fstage_bg", lang), loader.getMessage(), a -> a);

            return;
        }

        if(music >= 0 && UserProfile.getBCData().musics.get(music) == null) {
            replyToMessageSafely(ch, LangID.getStringByID("fstage_music", lang), loader.getMessage(), a -> a);

            return;
        }

        ArrayList<CastleList> castleLists = new ArrayList<>(CastleList.defset());

        if(castle >= 0 && castle >= castleLists.getFirst().size()) {
            replyToMessageSafely(ch, LangID.getStringByID("fstage_castle", lang), loader.getMessage(), a -> a);

            return;
        }

        List<List<Enemy>> enemySequences = new ArrayList<>();
        List<Enemy> filterEnemy = new ArrayList<>();
        StringBuilder enemyList = new StringBuilder();

        String[] names = enemyName.split("/");

        if(names.length > 5) {
            replyToMessageSafely(ch, LangID.getStringByID("fstage_toomany", lang), loader.getMessage(), a -> a);
            disableTimer();

            return;
        }

        if(!enemyName.isBlank()) {
            for(int i = 0; i < names.length; i++) {
                if(names[i].trim().isBlank()) {
                    replyToMessageSafely(ch, LangID.getStringByID("fstage_noname", lang), loader.getMessage(), a -> a);
                    disableTimer();

                    return;
                }

                ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(names[i].trim(), lang);

                if(enemies.isEmpty()) {
                    replyToMessageSafely(ch, LangID.getStringByID("enemyst_noenemy", lang).replace("_", names[i].trim()), loader.getMessage(), a -> a);
                    disableTimer();

                    return;
                } else if(enemies.size() == 1) {
                    filterEnemy.add(enemies.getFirst());

                    String n = StaticStore.safeMultiLangGet(enemies.getFirst(), lang);

                    if(n == null || n.isBlank()) {
                        n = Data.trio(Objects.requireNonNull(enemies.getFirst().id).id);
                    }

                    enemyList.append(n).append(", ");
                } else {
                    enemySequences.add(enemies);
                }
            }
        }

        if(enemySequences.isEmpty()) {
            ArrayList<Stage> stages = EntityFilter.findStage(filterEnemy, music, background, castle, hasBoss, orOperate, monthly);

            if(stages.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("fstage_nost", lang), loader.getMessage(), a -> a);

                disableTimer();
            } else if(stages.size() == 1) {
                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getMessage().getAuthor().getId(), TreasureHolder.global);

                EntityHandler.showStageEmb(stages.getFirst(), ch, loader.getMessage(), isFrame, isExtra, isCompact, star, treasure, lang, result -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new StageInfoButtonHolder(stages.getFirst(), msg, result, ch.getId(), isCompact, lang));
                });
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("fstage_several", lang)).append("```md\n");

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = stages.size() / SearchHolder.PAGE_CHUNK;

                    if(stages.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                createMonthlyMessage(ch, loader.getMessage(), sb.toString(), accumulateStage(stages, false), stages, stages.size(), monthly, res -> {
                    User u = loader.getUser();

                    TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                    StaticStore.putHolder(u.getId(), new FindStageMessageHolder(stages, monthly ? accumulateCategory(stages) : null, loader.getMessage(), res, ch.getId(), star, treasure, isFrame, isExtra, isCompact, lang));
                });

                disableTimer();
            }
        } else {
            StringBuilder sb = new StringBuilder();

            if(!enemyList.isEmpty()) {
                sb.append(LangID.getStringByID("fstage_selected", lang).replace("_", enemyList.toString().replaceAll(", $", "")));
            }

            sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

            List<Enemy> enemies = enemySequences.getFirst();

            List<String> data = accumulateEnemy(enemies);

            for(int i = 0; i < data.size(); i++) {
                sb.append(i+1).append(". ").append(data.get(i)).append("\n");
            }

            if(enemies.size() > SearchHolder.PAGE_CHUNK) {
                int totalPage = enemies.size() / SearchHolder.PAGE_CHUNK;

                if(enemies.size() % SearchHolder.PAGE_CHUNK != 0)
                    totalPage++;

                sb.append(LangID.getStringByID("formst_page", lang).formatted(1, totalPage)).append("\n");
            }

            sb.append("```");

            registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), enemies.size(), data, lang).queue(res -> {
                User u = loader.getUser();

                Message msg = loader.getMessage();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                StaticStore.putHolder(u.getId(), new StageEnemyMessageHolder(enemySequences, filterEnemy, enemyList, msg, res, ch.getId(), isFrame, isExtra, isCompact, orOperate, hasBoss, monthly, star, treasure, background, castle, music, lang));
            });
        }
    }

    private String getEnemyName(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2)
            return "";

        StringBuilder result = new StringBuilder();

        boolean second = false;
        boolean frame = false;
        boolean level = false;
        boolean background = false;
        boolean or = false;
        boolean and = false;
        boolean castle = false;
        boolean music = false;
        boolean boss = false;
        boolean monthly = false;
        boolean extra = false;
        boolean compact = false;
        boolean itf = false;
        boolean cotc = false;

        for(int i = 1; i < contents.length; i++) {
            if(contents[i].equals("-lv") && !level) {
                if(i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    level = true;
                    i++;
                } else {
                    result.append(contents[i]);

                    if(i < contents.length - 1) {
                        result.append(" ");
                    }
                }
            } else if(!second && contents[i].equals("-s")) {
                second = true;
            } else if (!frame && contents[i].matches("-f(r)?")) {
                frame = true;
            } else if(!background && (contents[i].equals("-bg") || contents[i].equals("-background")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                background = true;
                i++;
            } else if(!and && (contents[i].equals("-a") || contents[i].equals("-and"))) {
                and = true;
            } else if(!or && (contents[i].equals("-o") || contents[i].equals("-or"))) {
                or = true;
            } else if(!castle && (contents[i].equals("-cs") || contents[i].equals("-castle")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                castle = true;
                i++;
            } else if(!music && (contents[i].equals("-ms") || contents[i].equals("-music")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                music = true;
                i++;
            } else if(!boss && (contents[i].equals("-b") || contents[i].equals("-boss"))) {
                boss = true;
            } else if(!monthly && (contents[i].equals("-m") || contents[i].equals("-monthly"))) {
                monthly = true;
            } else if (!extra && (contents[i].equals("-e") || contents[i].equals("-extra"))) {
                extra = true;
            } else if (!compact && (contents[i].equals("-c") || contents[i].equals("-compact"))) {
                compact = true;
            } else if (!itf && contents[i].matches("^-i(tf)?\\d$")) {
                itf = true;
            } else if (!cotc && contents[i].matches("^-c(otc)?\\d$")) {
                cotc = true;
            } else {
                result.append(contents[i]);

                if(i < contents.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString();
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
                        } else
                            break label;
                    }
                    case "-c", "-compact" -> {
                        if ((result & PARAM_COMPACT) == 0) {
                            result |= PARAM_COMPACT;
                        } else
                            break label;
                    }
                    case "-o", "-or" -> {
                        if ((result & PARAM_OR) == 0) {
                            result |= PARAM_OR;
                        } else
                            break label;
                    }
                    case "-a", "-and" -> {
                        if ((result & PARAM_AND) == 0) {
                            result |= PARAM_AND;
                        } else
                            break label;
                    }
                    case "-b", "-boss" -> {
                        if ((result & PARAM_BOSS) == 0) {
                            result |= PARAM_BOSS;
                        } else {
                            break label;
                        }
                    }
                    case "-m", "-monthly" -> {
                        if ((result & PARAM_MONTHLY) == 0) {
                            result |= PARAM_MONTHLY;
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

    private int getLevel(String command) {
        int level = 0;

        if(command.contains("-lv")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if(contents[i].equals("-lv") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    level = StaticStore.safeParseInt(contents[i+1]);
                    break;
                }
            }
        }

        return level;
    }

    private int getBackground(String command) {
        int bg = -1;

        if (command.contains("-bg") || command.contains("-background")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if((contents[i].equals("-bg") || contents[i].equals("-background")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                    bg = StaticStore.safeParseInt(contents[i + 1]);
                    break;
                }
            }
        }

        return bg;
    }

    private int getCastle(String command) {
        int castle = -1;

        if (command.contains("-cs") || command.contains("-castle")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if((contents[i].equals("-cs") || contents[i].equals("-castle")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                    castle = StaticStore.safeParseInt(contents[i + 1]);
                    break;
                }
            }
        }

        return castle;
    }

    private int getMusic(String command) {
        int music = -1;

        if (command.contains("-ms") || command.contains("-music")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if((contents[i].equals("-ms") || contents[i].equals("-music")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                    music = StaticStore.safeParseInt(contents[i + 1]);
                    break;
                }
            }
        }

        return music;
    }
    
    private List<String> accumulateEnemy(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();
        
        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String ename = e.id == null ? "UNKNOWN " : Data.trio(e.id.id)+" ";

            if(MultiLangCont.get(e, lang) != null)
                ename += MultiLangCont.get(e, lang);

            data.add(ename);
        }
        
        return data;
    }
    
    private List<String> accumulateStage(List<Stage> stage, boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= stage.size())
                break;

            Stage st = stage.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(onText) {
                if(mc != null) {
                    String mcn = MultiLangCont.get(mc, lang);

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            }

            String stmn = MultiLangCont.get(stm, lang);

            if(stm.id != null) {
                if(stmn == null || stmn.isBlank())
                    stmn = Data.trio(stm.id.id);
            } else {
                if(stmn == null || stmn.isBlank())
                    stmn = "Unknown";
            }

            name += stmn+" - ";

            String stn = MultiLangCont.get(st, lang);

            if(st.id != null) {
                if(stn == null || stn.isBlank())
                    stn = Data.trio(st.id.id);
            } else {
                if(stn == null || stn.isBlank())
                    stn = "Unknown";
            }

            name += stn;

            data.add(name);
        }

        return data;
    }

    private void createMonthlyMessage(MessageChannel ch, Message reference, String content, List<String> data, List<Stage> stages, int size, boolean monthly, Consumer<Message> onSuccess) {
        int totPage = size / SearchHolder.PAGE_CHUNK;

        if(size % SearchHolder.PAGE_CHUNK != 0)
            totPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(size > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:\\S+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectData", lang)).build()));

        if(monthly) {
            List<SelectOption> categories = new ArrayList<>();

            List<MONTHLY> category = accumulateCategory(stages);

            categories.add(SelectOption.of(LangID.getStringByID("data.all", lang), "all"));

            for(int i = 0; i < category.size(); i++) {
                String name = category.get(i).name().toLowerCase(Locale.ENGLISH);

                categories.add(SelectOption.of(LangID.getStringByID("data_" + name, lang), name));
            }

            rows.add(ActionRow.of(StringSelectMenu.create("category").addOptions(categories).setPlaceholder(LangID.getStringByID("fstage_category", lang)).build()));
        }

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));

        replyToMessageSafely(ch, content, reference, a -> a.setComponents(rows), onSuccess);
    }

    private List<MONTHLY> accumulateCategory(List<Stage> stages) {
        List<MONTHLY> category = new ArrayList<>();

        for(int i = 0; i < stages.size(); i++) {
            StageMap map = stages.get(i).getCont();

            if(map == null || map.id == null)
                continue;

            MapColc mc = map.getCont();

            if(mc == null)
                continue;

            switch (mc.getSID()) {
                case "000003" -> {
                    switch (map.id.id) {
                        case 3 -> addIfNone(category, MONTHLY.ITF1);
                        case 4 -> addIfNone(category, MONTHLY.ITF2);
                        case 5 -> addIfNone(category, MONTHLY.ITF3);
                        case 6 -> addIfNone(category, MONTHLY.COTC1);
                        case 7 -> addIfNone(category, MONTHLY.COTC2);
                        case 8 -> addIfNone(category, MONTHLY.COTC3);
                        case 9 -> addIfNone(category, MONTHLY.EOC);
                    }
                }
                case "000001" -> addIfNone(category, MONTHLY.CYCLONE);
                case "000000" -> addIfNone(category, MONTHLY.SOL);
            }
        }

        return category;
    }

    private <T> void addIfNone(List<T> data, T element) {
        if(!data.contains(element)) {
            data.add(element);
        }
    }
}
