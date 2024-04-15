package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.FormButtonHolder;
import mandarin.packpack.supporter.server.holder.component.search.FormStatMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.message.FormReactionSlashMessageHolder;
import mandarin.packpack.supporter.server.slash.SlashOption;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FormStat extends ConstraintCommand {
    public static void performInteraction(GenericCommandInteractionEvent event) {
        Interaction interaction = event.getInteraction();

        if(event.getOptions().isEmpty()) {
            StaticStore.logger.uploadLog("E/FormStat::performInteraction - Options are absent!");
            return;
        }

        int lang = LangID.EN;

        User u = interaction.getUser();

        if(StaticStore.config.containsKey(u.getId())) {
            lang = StaticStore.config.get(u.getId()).lang;

            if(lang == -1) {
                if(interaction.getGuild() == null) {
                    lang = LangID.EN;
                } else {
                    IDHolder idh = StaticStore.idHolder.get(interaction.getGuild().getId());

                    if(idh == null) {
                        lang = LangID.EN;
                    } else {
                        lang = idh.config.lang;
                    }
                }
            }
        }

        IDHolder holder;
        Guild g = event.getGuild();

        if(g == null) {
            holder = null;
        } else {
            holder = StaticStore.idHolder.get(g.getId());
        }

        List<OptionMapping> options = event.getOptions();

        String name = SlashOption.getStringOption(options, "name", "");
        boolean frame = SlashOption.getBooleanOption(options, "frame", true);
        boolean talent = SlashOption.getBooleanOption(options, "talent", false);
        boolean extra = SlashOption.getBooleanOption(options, "extra", false);
        boolean treasure = SlashOption.getBooleanOption(options, "treasure", false);
        Level lv = prepareLevels(options);

        Form f = EntityFilter.pickOneForm(name, lang);

        int finalLang = lang;

        if(f == null) {
            event.deferReply()
                    .setAllowedMentions(new ArrayList<>())
                    .setContent(LangID.getStringByID("formst_specific", finalLang))
                    .queue();

            return;
        }

        try {
            ConfigHolder config;

            config = StaticStore.config.getOrDefault(u.getId(), StaticStore.defaultConfig);

            TreasureHolder t = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

            EntityHandler.performUnitEmb(f, event, config, frame, talent, extra, lv, treasure, t, finalLang, m -> {
                if(m != null && (!(m.getChannel() instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE))) {
                    StaticStore.putHolder(
                            u.getId(),
                            new FormReactionSlashMessageHolder(m, f, u.getId(), m.getChannel().getId(), config, frame && config.useFrame, talent, extra || config.extra, lv, treasure, t, finalLang)
                    );
                }
            });
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FormStat::performInteraction - Failed to show unit embed");
        }
    }

    private static Level prepareLevels(List<OptionMapping> options) {
        ArrayList<Integer> levels = new ArrayList<>();

        for(int i = 0; i < 7; i++) {
            if(i == 0)
                levels.add(SlashOption.getIntOption(options, "level", -1));
            else
                levels.add(SlashOption.getIntOption(options, "talent_level_"+i, -1));
        }

        Level lv = new Level(0);

        if(!levels.isEmpty())
            lv.setLevel(levels.getFirst());
        else
            lv.setLevel(-1);

        if(levels.size() > 1) {
            boolean allEmpty = true;

            for(int i = 1; i < levels.size(); i++) {
                if (levels.get(i) > 0) {
                    allEmpty = false;
                    break;
                }
            }

            if(!allEmpty) {
                int[] t = new int[levels.size() - 1];

                for(int i = 1; i < levels.size(); i++) {
                    t[i - 1] = Math.max(0, levels.get(i));
                }

                lv.setTalents(t);
            }
        }

        return lv;
    }

    private static final int PARAM_TALENT = 2;
    private static final int PARAM_SECOND = 4;
    private static final int PARAM_EXTRA = 8;
    private static final int PARAM_COMPACT = 16;
    private static final int PARAM_TRUE_FORM = 32;
    private static final int PARAM_TREASURE = 64;
    private static final int PARAM_FRAME = 128;

    private final ConfigHolder config;

    public FormStat(ROLE role, int lang, IDHolder holder, ConfigHolder config) {
        super(role, lang, holder, false);

        if(config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
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

        String[] list = loader.getContent().split(" ",2);
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
        int param = checkParameters(command);

        Level lv = handleLevel(command);

        boolean isFrame;

        if ((param & PARAM_SECOND) > 0)
            isFrame = false;
        else if ((param & PARAM_FRAME) > 0)
            isFrame = true;
        else
            isFrame = config.useFrame;

        boolean talent = (param & PARAM_TALENT) > 0 || lv.getTalents().length > 0;
        boolean extra = (param & PARAM_EXTRA) > 0 || config.extra;
        boolean compact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);
        boolean isTrueForm = (param & PARAM_TRUE_FORM) > 0 || config.trueForm;
        boolean isTreasure = (param & PARAM_TREASURE) > 0 || config.treasure;

        if(list.length == 1 || filterCommand(command).isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("formst_noname", lang), loader.getMessage(), a -> a);
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(filterCommand(command), isTrueForm, lang);

            if (forms.size() == 1) {
                Form f = forms.getFirst();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getMessage().getAuthor().getId(), TreasureHolder.global);

                EntityHandler.showUnitEmb(f, ch, loader.getMessage(), config, isFrame, talent, extra, isTrueForm, f.fid >= 2, lv, isTreasure, treasure, lang, true, compact, result -> {
                    User u = loader.getUser();

                    Message author = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new FormButtonHolder(forms.getFirst(), author, result, config, isFrame, talent, extra, compact, isTreasure, treasure, lv, lang, ch.getId()));
                });
            } else if (forms.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("formst_nounit", lang).replace("_", getSearchKeyword(loader.getContent())), loader.getMessage(), a -> a);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", getSearchKeyword(command)));

                sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateListData(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = forms.size() / SearchHolder.PAGE_CHUNK;

                    if(forms.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                    StaticStore.putHolder(u.getId(), new FormStatMessageHolder(forms, msg, config, holder, res, ch.getId(), param, lv, treasure, lang));
                });
            }
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            label:
            for(String str : pureMessage) {
                switch (str) {
                    case "-t" -> {
                        if ((result & PARAM_TALENT) == 0) {
                            result |= PARAM_TALENT;
                        } else
                            break label;
                    }
                    case "-s" -> {
                        if ((result & PARAM_SECOND) == 0) {
                            result |= PARAM_SECOND;
                        } else
                            break label;
                    }
                    case "-f", "-fr" -> {
                        if ((result & PARAM_FRAME) == 0) {
                            result |= PARAM_FRAME;
                        } else {
                            break label;
                        }
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
                    case "-tf", "-trueform" -> {
                        if ((result & PARAM_TRUE_FORM) == 0) {
                            result |= PARAM_TRUE_FORM;
                        } else
                            break label;
                    }
                    case "-tr", "-treasure" -> {
                        if ((result & PARAM_TREASURE) == 0) {
                            result |= PARAM_TREASURE;
                        } else
                            break label;
                    }
                }
            }
        }

        return result;
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isFrame = false;
        boolean isLevel = false;
        boolean isTalent = false;
        boolean isExtra = false;
        boolean isCompact = false;
        boolean isTrueForm = false;
        boolean isTreasure = false;

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
                case "-t" -> {
                    if (!isTalent)
                        isTalent = true;
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
                case "-lv", "-lvl" -> {
                    if (!isLevel && i < content.length - 1) {
                        String text = getLevelText(content, i + 1);

                        if (text.contains(" ")) {
                            i += getLevelText(content, i + 1).split(" ").length;
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
                case "-tf", "-trueform" -> {
                    if (!isTrueForm)
                        isTrueForm = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-tr", "-treasure" -> {
                    if (!isTreasure)
                        isTreasure = true;
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

        if(command.toString().isBlank()) {
            return "";
        }

        return command.toString().trim();
    }

    private Level handleLevel(String msg) {
        if(msg.contains("-lv")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if((content[i].equals("-lv") || content[i].equals("-lvl")) && i != content.length -1) {
                    String[] trial = getLevelText(content, i+1).replace(" ", "").split(",");

                    int length = 0;

                    for (String s : trial) {
                        if (StaticStore.isNumeric(s))
                            length++;
                        else
                            break;
                    }

                    if(length == 0) {
                        Level res = new Level(0);

                        res.setLevel(-1);

                        return res;
                    }
                    else {
                        ArrayList<Integer> lv = new ArrayList<>();

                        for (int j = 0; j < length; j++) {
                            lv.add(StaticStore.safeParseInt(trial[j]));
                        }

                        Level level = new Level(0);

                        if(!lv.isEmpty())
                            level.setLevel(lv.getFirst());
                        else
                            level.setLevel(-1);

                        if(lv.size() > 1) {
                            int[] t = new int[lv.size() - 1];

                            for(int j = 1; j < lv.size(); j++) {
                                t[j - 1] = lv.get(j);
                            }

                            level.setTalents(t);
                        }

                        return level;
                    }
                }
            }
        } else {
            Level res = new Level(0);

            res.setLevel(-1);

            return res;
        }

        Level res = new Level(0);

        res.setLevel(-1);

        return res;
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
        int numberLetter = 0;
        int commaAdd = 0;

        for(int i = 0; i < sb.length(); i++) {
            if(sb.charAt(i) == ',') {
                if(!commaStart && commaAdd <= 8) {
                    commaStart = true;
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
            } else {
                if(Character.isDigit(sb.charAt(i))) {
                    commaStart = false;
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

    private List<String> accumulateListData(List<Form> forms) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= forms.size())
                break;

            Form f = forms.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            String name = StaticStore.safeMultiLangGet(f, lang);

            if(name != null)
                fname += name;

            data.add(fname);
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
