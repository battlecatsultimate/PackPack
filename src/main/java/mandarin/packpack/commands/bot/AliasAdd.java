package mandarin.packpack.commands.bot;

import common.CommonStatic;
import common.util.Data;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.message.alias.AliasEnemyMessageHolder;
import mandarin.packpack.supporter.server.holder.message.alias.AliasFormMessageHolder;
import mandarin.packpack.supporter.server.holder.message.alias.AliasStageMessageHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AliasAdd extends ConstraintCommand {
    private final ConfigHolder config;

    public AliasAdd(ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id) {
        super(role, lang, id, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();

        AliasHolder.TYPE type = getType(loader.getContent());

        if(type == AliasHolder.TYPE.UNSPECIFIED) {
            ch.sendMessage(LangID.getStringByID("alias.failed.noType", lang)).queue();
            return;
        }

        switch (type) {
            case FORM -> {
                String unitName = getName(loader.getContent());

                if (unitName.isBlank()) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.failed.noAlias.unit", lang));

                    return;
                }

                ArrayList<Form> forms = EntityFilter.findUnitWithName(unitName, false, lang);

                if (forms.isEmpty()) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("formStat.fail.noUnit", lang).formatted(validateName(unitName)));
                } else if (forms.size() == 1) {
                    String fname = StaticStore.safeMultiLangGet(forms.getFirst(), lang);

                    if (fname == null || fname.isBlank())
                        fname = forms.getFirst().names.toString();

                    if (fname.isBlank())
                        fname = Data.trio(Objects.requireNonNull(forms.getFirst().unit.id).id) + "-" + Data.trio(forms.getFirst().fid);

                    ArrayList<String> alias = AliasHolder.getAlias(type, lang, forms.getFirst());

                    if (alias == null)
                        alias = new ArrayList<>();

                    String aliasName = getAliasName(loader.getContent());

                    if (aliasName.isBlank()) {
                        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.failed.noName", lang));

                        return;
                    }

                    if (alias.contains(aliasName)) {
                        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.contain", lang).formatted(fname));

                        return;
                    }

                    alias.add(aliasName);

                    AliasHolder.FALIAS.put(lang, forms.getFirst(), alias);

                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.added", lang).formatted(fname, aliasName));

                    StaticStore.logger.uploadLog("Alias added\n\nUnit : " + fname + "\nAlias : " + aliasName + "\nBy : " + u.getAsMention());
                } else {
                    replyToMessageSafely(ch, loader.getMessage(), msg ->
                                    StaticStore.putHolder(u.getId(), new AliasFormMessageHolder(forms, loader.getMessage(), u.getId(), ch.getId(), msg, AliasHolder.MODE.ADD, lang, unitName, getAliasName(loader.getContent())))
                            , getSearchComponents(forms.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(unitName, forms.size()), forms, this::accumulateFormName, config.searchLayout, lang)
                    );
                }
            }
            case ENEMY -> {
                String enemyName = getName(loader.getContent());

                if (enemyName.isBlank()) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.failed.noAlias.enemy", lang));
                    return;
                }

                ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(enemyName, lang);

                if (enemies.isEmpty()) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(validateName(enemyName)));
                } else if (enemies.size() == 1) {
                    String eName = StaticStore.safeMultiLangGet(enemies.getFirst(), lang);

                    if (eName == null || eName.isBlank())
                        eName = enemies.getFirst().names.toString();

                    if (eName.isBlank())
                        eName = Data.trio(Objects.requireNonNull(enemies.getFirst().id).id);

                    ArrayList<String> alias = AliasHolder.getAlias(type, lang, enemies.getFirst());

                    if (alias == null)
                        alias = new ArrayList<>();

                    String aliasName = getAliasName(loader.getContent());

                    if (aliasName.isBlank()) {
                        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.failed.noName", lang));

                        return;
                    }

                    if (alias.contains(aliasName)) {
                        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.contain", lang).formatted(eName));

                        return;
                    }

                    alias.add(aliasName);

                    AliasHolder.EALIAS.put(lang, enemies.getFirst(), alias);

                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.added", lang).formatted(eName, aliasName));

                    StaticStore.logger.uploadLog("Alias added\n\nEnemy : " + eName + "\nAlias : " + aliasName + "\nBy : " + u.getAsMention());
                } else {
                    replyToMessageSafely(ch, loader.getMessage(), msg ->
                                    StaticStore.putHolder(u.getId(), new AliasEnemyMessageHolder(enemies, loader.getMessage(), u.getId(), ch.getId(), msg, AliasHolder.MODE.ADD, lang, enemyName, getAliasName(loader.getContent()))),
                            getSearchComponents(enemies.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(enemyName, enemies.size()), enemies, this::accumulateEnemyName, config.searchLayout, lang)
                    );
                }
            }
            case STAGE -> {
                String[] names = generateStageNameSeries(loader.getContent());

                if (names[0] == null && names[1] == null && names[2] == null) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.failed.noAlias.stage", lang));

                    return;
                }

                ArrayList<Stage> stages = EntityFilter.findStageWithName(names, lang);

                String summary = "";

                if (stages.isEmpty() && names[0] == null && names[1] == null) {
                    stages = EntityFilter.findStageWithMapName(names[2], lang);

                    if (!stages.isEmpty()) {
                        summary += LangID.getStringByID("stageInfo.smartSearch", lang) + "\n\n";
                    }
                }

                if (stages.isEmpty()) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("stageInfo.fail.noResult", lang).formatted(generateSearchName(names)));
                } else if (stages.size() == 1) {
                    String stName = StaticStore.safeMultiLangGet(stages.getFirst(), lang);

                    if (stName == null || stName.isBlank())
                        stName = stages.getFirst().name;

                    if (stName == null || stName.isBlank())
                        stName = stages.getFirst().getCont().getSID() +
                                "-" +
                                Data.trio(Objects.requireNonNull(stages.getFirst().getCont().id).id) +
                                "-" +
                                Data.trio(Objects.requireNonNull(stages.getFirst().id).id);

                    ArrayList<String> alias = AliasHolder.getAlias(type, lang, stages.getFirst());

                    if (alias == null)
                        alias = new ArrayList<>();

                    String aliasName = getAliasName(loader.getContent());

                    if (aliasName.isBlank()) {
                        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.failed.noName", lang));

                        return;
                    }

                    if (alias.contains(aliasName)) {
                        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("alias.contain", lang).formatted(stName));

                        return;
                    }

                    alias.add(aliasName);

                    AliasHolder.SALIAS.put(lang, stages.getFirst(), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias.added", lang).formatted(stName, aliasName));

                    StaticStore.logger.uploadLog("Alias added\n\nStage : " + stName + "\nAlias : " + aliasName + "\nBy : " + u.getAsMention());
                } else {
                    ArrayList<Stage> finalStages = stages;

                    summary += LangID.getStringByID("stageInfo.several", lang).formatted(generateSearchName(names), stages.size());

                    System.out.println(summary);

                    final String finalSummary = summary;

                    replyToMessageSafely(ch, loader.getMessage(), msg ->
                                    StaticStore.putHolder(u.getId(), new AliasStageMessageHolder(finalStages, loader.getMessage(), u.getId(), ch.getId(), msg, AliasHolder.MODE.ADD, lang, generateSearchName(names), getAliasName(loader.getContent()), finalSummary))
                            , getSearchComponents(stages.size(), finalSummary, stages, this::accumulateStageName, config.searchLayout, lang)
                    );
                }
            }
        }
    }

    public AliasHolder.TYPE getType(String message) {
        String[] contents = message.split(" ");

        for(String content : contents) {
            switch (content) {
                case "-stage", "-st", "-s" -> {
                    return AliasHolder.TYPE.STAGE;
                }
                case "-form", "-f" -> {
                    return AliasHolder.TYPE.FORM;
                }
                case "-enemy", "-e" -> {
                    return AliasHolder.TYPE.ENEMY;
                }
            }
        }

        return AliasHolder.TYPE.UNSPECIFIED;
    }

    private String getName(String content) {
        String[] contents = content.split(" ");

        if(contents.length < 3)
            return "";

        StringBuilder result = new StringBuilder();

        for(int i = 2; i < contents.length; i++) {
            if(contents[i].equals("->"))
                break;

            result.append(contents[i]);

            if(i < contents.length - 1)
                result.append(" ");
        }

        String res = result.toString();

        if(res.endsWith(" ")) {
            return res.substring(0, res.length() - 1);
        }

        return res;
    }

    private String generateSearchName(String[] names) {
        String result = "";

        if(names[0] != null) {
            result += LangID.getStringByID("stageInfo.mapCollection", lang).replace("_", names[0])+", ";
        }

        if(names[1] != null) {
            result += LangID.getStringByID("stageInfo.stageMap", lang).replace("_", names[1])+", ";
        }

        if(names[2] != null) {
            result += LangID.getStringByID("stageInfo.stage", lang).replace("_", names[2]);
        }

        if(result.endsWith(", "))
            result = result.substring(0, result.length() -2);

        return result;
    }

    private String[] generateStageNameSeries(String command) {
        String[] res = {null, null, null};

        String[] segments = command.split(" ", 2);

        if(segments.length == 2) {
            String[] contents = segments[1].split(" ");

            //Find MapColc Name

            int mcIndex = -1;
            int stmIndex = -1;

            if (segments[1].contains("-mc")) {
                mcIndex = 0;

                for(String content : contents) {
                    if(content.equals("-mc"))
                        break;

                    mcIndex++;
                }
            }

            if(segments[1].contains("-stm")) {
                stmIndex = 0;

                for(String content : contents) {
                    if(content.equals("-stm"))
                        break;

                    stmIndex++;
                }
            }

            if(segments[1].contains("-mc")) {
                res[0] = getMapColcName(contents, mcIndex, stmIndex);

                if(res[0].isBlank())
                    res[0] = null;
            }

            //Find StageMap Name

            if(segments[1].contains("-stm")) {
                res[1] = getStageMapName(contents, stmIndex, mcIndex);

                if(res[1].isBlank())
                    res[1] = null;
            }

            res[2] = getStageName(contents);

            if(res[2].isBlank())
                res[2] = null;
        }

        return res;
    }

    private String getMapColcName(String[] contents, int start, int stm) {
        StringBuilder mc = new StringBuilder();

        boolean stmDone = stm < start;

        for(int i = start+1; i < contents.length; i++) {
            if(contents[i].equals("-stm") && !stmDone)
                break;
            else if(contents[i].equals("->"))
                break;
            else
                mc.append(contents[i]).append(" ");
        }

        String res = mc.toString();

        if(res.endsWith(" "))
            res = res.substring(0, res.length() - 1);

        return res;
    }

    private String getStageMapName(String[] contents, int start, int mc) {
        StringBuilder stm = new StringBuilder();

        boolean mcDone = mc < start;

        for (int i = start + 1; i < contents.length; i++) {
            if (contents[i].equals("-mc") && !mcDone)
                break;
            else if(contents[i].equals("->"))
                break;
            else
                stm.append(contents[i]).append(" ");
        }

        String res = stm.toString();

        if(res.endsWith(" "))
            res = res.substring(0, res.length() - 1);

        return res;
    }

    private String getStageName(String[] contents) {
        boolean isType = false;

        StringBuilder st = new StringBuilder();

        label:
        for (String content : contents) {
            switch (content) {
                case "-mc":
                case "-stm":
                case "->":
                    break label;
                case "-s":
                    if (!isType) {
                        isType = true;
                    } else {
                        st.append(content).append(" ");
                    }
                    break;
                default:
                    st.append(content).append(" ");
                    break;
            }
        }

        String res = st.toString();

        if(res.endsWith(" "))
            res = res.substring(0, res.length() - 1);

        return res;
    }

    private String getAliasName(String message) {
        String[] contents = message.split(" +-> +", 2);

        if(contents.length < 2)
            return "";
        else
            return contents[1].trim();
    }

    private String validateName(String name) {
        if(name.length() > 1500)
            return name.substring(0, 1500) + "...";
        else
            return name;
    }

    private List<String> accumulateFormName(List<Form> forms, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(forms.size(), config.searchLayout.chunkSize); i++) {
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

    private List<String> accumulateEnemyName(List<Enemy> enemies, SearchHolder.TextType textType) {
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

    private List<String> accumulateStageName(List<Stage> stages, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(stages.size(), config.searchLayout.chunkSize); i++) {
            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            if (st.id == null || stm.id == null)
                continue;

            String name = "";

            String fullName = "";

            if (mc != null) {
                String mcName = StaticStore.safeMultiLangGet(mc, lang);

                if (mcName == null || mcName.isBlank()) {
                    mcName = DataToString.getMapCode(mc);
                }

                fullName += mcName + " - ";
            } else {
                fullName += "Unknown - ";
            }

            String stmName = StaticStore.safeMultiLangGet(stm, lang);

            if (stmName == null || stmName.isBlank()) {
                stmName = Data.trio(stm.id.id);
            }

            fullName += stmName + " - ";

            String stName = StaticStore.safeMultiLangGet(st, lang);

            if (stName == null || stName.isBlank()) {
                stName = Data.trio(st.id.id);
            }

            fullName += stName;

            switch (textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
                        name = fullName;
                    } else {
                        name = "`" + DataToString.getStageCode(st) + "` " + fullName;
                    }
                }
                case LIST_DESCRIPTION -> name = DataToString.getStageCode(st);
                case LIST_LABEL -> name = fullName;
            }

            data.add(name);
        }

        return data;
    }
}
