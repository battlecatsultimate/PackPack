package mandarin.packpack.commands.bot;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.message.alias.AliasEnemyMessageHolder;
import mandarin.packpack.supporter.server.holder.message.alias.AliasFormMessageHolder;
import mandarin.packpack.supporter.server.holder.message.alias.AliasStageMessageHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;

public class AliasRemove extends ConstraintCommand {

    public AliasRemove(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        User u = getUser(event);

        if(ch == null || u == null)
            return;

        AliasHolder.TYPE type = getType(getContent(event));

        if(type == AliasHolder.TYPE.UNSPECIFIED) {
            ch.sendMessage(LangID.getStringByID("alias_specify", lang)).queue();
            return;
        }

        switch (type) {
            case FORM -> {
                String unitName = getName(getContent(event));

                if (unitName.isBlank()) {
                    createMessageWithNoPings(ch, LangID.getStringByID("alias_formnoname", lang));
                    return;
                }

                ArrayList<Form> forms = EntityFilter.findUnitWithName(unitName, false, lang);

                if (forms.isEmpty()) {
                    createMessageWithNoPings(ch, LangID.getStringByID("formst_nounit", lang).replace("_", validateName(unitName)));
                } else if (forms.size() == 1) {
                    String fname = StaticStore.safeMultiLangGet(forms.get(0), lang);

                    if (fname == null || fname.isBlank())
                        fname = forms.get(0).names.toString();

                    if (fname.isBlank())
                        fname = Data.trio(forms.get(0).unit.id.id) + "-" + Data.trio(forms.get(0).fid);

                    ArrayList<String> alias = AliasHolder.getAlias(type, lang, forms.get(0));

                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", fname));
                        return;
                    }

                    String aliasName = getAliasName(getContent(event));

                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        return;
                    }

                    if (!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_nosuch", lang));
                        return;
                    }

                    alias.remove(aliasName);

                    AliasHolder.FALIAS.put(AliasHolder.getLangCode(lang), forms.get(0), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_removed", lang).replace("_DDD_", fname).replace("_AAA_", aliasName));

                    User us = getUser(event);

                    StaticStore.logger.uploadLog("Alias removed\n\nUnit : " + fname + "\nAlias : " + aliasName + "\nBy : " + (us == null ? "Unknown" : u.getAsMention()));
                } else {
                    StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", validateName(unitName)));

                    String check;

                    if (forms.size() <= 20)
                        check = "";
                    else
                        check = LangID.getStringByID("formst_next", lang);

                    sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

                    for (int i = 0; i < 20; i++) {
                        if (i >= forms.size())
                            break;

                        Form f = forms.get(i);

                        String fname = Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + " ";

                        String n = StaticStore.safeMultiLangGet(f, lang);

                        if (n != null)
                            fname += n;

                        sb.append(i + 1).append(". ").append(fname).append("\n");
                    }

                    if (forms.size() > 20)
                        sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(forms.size() / 20 + 1)));

                    sb.append(LangID.getStringByID("formst_can", lang));
                    sb.append("```");

                    Message res = getMessageWithNoPings(ch, sb.toString());

                    if (res != null) {
                        Message msg = getMessage(event);

                        if (msg != null)
                            StaticStore.putHolder(u.getId(), new AliasFormMessageHolder(forms, msg, res, ch.getId(), AliasHolder.MODE.REMOVE, lang, getAliasName(getContent(event))));
                    }
                }
            }
            case ENEMY -> {
                String enemyName = getName(getContent(event));

                if (enemyName.isBlank()) {
                    createMessageWithNoPings(ch, LangID.getStringByID("alias_enemnoname", lang));
                    return;
                }

                ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(enemyName, lang);

                if (enemies.isEmpty()) {
                    createMessageWithNoPings(ch, LangID.getStringByID("enemyst_noenemy", lang).replace("_", validateName(enemyName)));
                } else if (enemies.size() == 1) {
                    String eName = StaticStore.safeMultiLangGet(enemies.get(0), lang);

                    if (eName == null || eName.isBlank())
                        eName = enemies.get(0).names.toString();

                    if (eName.isBlank())
                        eName = Data.trio(enemies.get(0).id.id);

                    ArrayList<String> alias = AliasHolder.getAlias(type, lang, enemies.get(0));

                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", eName));
                        return;
                    }

                    String aliasName = getAliasName(getContent(event));

                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        return;
                    }

                    if (!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_nosuch", lang).replace("_", eName));
                        return;
                    }

                    alias.remove(aliasName);

                    AliasHolder.EALIAS.put(AliasHolder.getLangCode(lang), enemies.get(0), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_removed", lang).replace("_DDD_", eName).replace("_AAA_", aliasName));

                    User us = getUser(event);

                    StaticStore.logger.uploadLog("Alias removed\n\nEnemy : " + eName + "\nAlias : " + aliasName + "\nBy : " + (us == null ? "Unknown" : u.getAsMention()));
                } else {
                    StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", validateName(enemyName)));

                    String check;

                    if (enemies.size() <= 20)
                        check = "";
                    else
                        check = LangID.getStringByID("formst_next", lang);

                    sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

                    for (int i = 0; i < 20; i++) {
                        if (i >= enemies.size())
                            break;

                        Enemy e = enemies.get(i);

                        String ename = e.id == null ? "UNKNOWN " : Data.trio(e.id.id) + " ";

                        if (MultiLangCont.get(e, lang) != null)
                            ename += MultiLangCont.get(e, lang);

                        sb.append(i + 1).append(". ").append(ename).append("\n");
                    }

                    if (enemies.size() > 20)
                        sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(enemies.size() / 20 + 1)));

                    sb.append(LangID.getStringByID("formst_can", lang));
                    sb.append("```");

                    Message res = getMessageWithNoPings(ch, sb.toString());

                    if (res != null) {
                        Message msg = getMessage(event);

                        if (msg != null)
                            StaticStore.putHolder(u.getId(), new AliasEnemyMessageHolder(enemies, msg, res, ch.getId(), AliasHolder.MODE.REMOVE, lang, getAliasName(getContent(event))));
                    }
                }
            }
            case STAGE -> {
                String[] names = generateStageNameSeries(getContent(event));

                if (names[0] == null && names[1] == null && names[2] == null) {
                    createMessageWithNoPings(ch, LangID.getStringByID("alias_stnoname", lang));
                    return;
                }

                ArrayList<Stage> stages = EntityFilter.findStageWithName(names, lang);

                if (stages.isEmpty() && names[0] == null && names[1] == null) {
                    stages = EntityFilter.findStageWithMapName(names[2]);

                    if (!stages.isEmpty()) {
                        ch.sendMessage(LangID.getStringByID("stinfo_smart", lang)).queue();
                    }
                }

                if (stages.isEmpty()) {
                    createMessageWithNoPings(ch, LangID.getStringByID("stinfo_nores", lang).replace("_", generateSearchName(names)));
                } else if (stages.size() == 1) {
                    String stName = StaticStore.safeMultiLangGet(stages.get(0), lang);

                    if (stName == null || stName.isBlank())
                        stName = stages.get(0).name;

                    if (stName == null || stName.isBlank())
                        stName = stages.get(0).getCont().getSID() + "-" + Data.trio(stages.get(0).getCont().id.id) + "-" + Data.trio(stages.get(0).id.id);

                    ArrayList<String> alias = AliasHolder.getAlias(type, lang, stages.get(0));

                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", stName));
                        return;
                    }

                    String aliasName = getAliasName(getContent(event));

                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        return;
                    }

                    if (!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_nosuch", lang).replace("_", stName));
                        return;
                    }

                    alias.remove(aliasName);

                    AliasHolder.SALIAS.put(AliasHolder.getLangCode(lang), stages.get(0), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_removed", lang).replace("_DDD_", stName).replace("_AAA_", aliasName));

                    User us = getUser(event);

                    StaticStore.logger.uploadLog("Alias removed\n\nStage : " + stName + "\nAlias : " + aliasName + "\nBy : " + (us == null ? "Unknown" : u.getAsMention()));
                } else {
                    String check;

                    if (stages.size() <= 20)
                        check = "";
                    else
                        check = LangID.getStringByID("formst_next", lang);

                    StringBuilder sb = new StringBuilder(LangID.getStringByID("stinfo_several", lang).replace("_", generateSearchName(names))).append("```md\n").append(check);

                    for (int i = 0; i < 20; i++) {
                        if (i >= stages.size())
                            break;

                        Stage st = stages.get(i);
                        StageMap stm = st.getCont();
                        MapColc mc = stm.getCont();

                        String stageName;

                        if (mc != null)
                            stageName = mc.getSID() + "/";
                        else
                            stageName = "Unknown/";

                        if (stm.id != null)
                            stageName += Data.trio(stm.id.id) + "/";
                        else
                            stageName += "Unknown/";

                        if (st.id != null)
                            stageName += Data.trio(st.id.id) + " | ";
                        else
                            stageName += "Unknown | ";

                        if (mc != null) {
                            String mcn = MultiLangCont.get(mc, lang);

                            if (mcn == null || mcn.isBlank())
                                mcn = mc.getSID();

                            stageName += mcn + " - ";
                        } else {
                            stageName += "Unknown - ";
                        }

                        String stmn = MultiLangCont.get(stm, lang);

                        if (stm.id != null) {
                            if (stmn == null || stmn.isBlank())
                                stmn = Data.trio(stm.id.id);
                        } else {
                            if (stmn == null || stmn.isBlank())
                                stmn = "Unknown";
                        }

                        stageName += stmn + " - ";

                        String stn = MultiLangCont.get(st, lang);

                        if (st.id != null) {
                            if (stn == null || stn.isBlank())
                                stn = Data.trio(st.id.id);
                        } else {
                            if (stn == null || stn.isBlank())
                                stn = "Unknown";
                        }

                        stageName += stn;

                        sb.append(i + 1).append(". ").append(stageName).append("\n");
                    }

                    if (stages.size() > 20)
                        sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(stages.size() / 20 + 1)));

                    sb.append(LangID.getStringByID("formst_can", lang));
                    sb.append("```");

                    Message res = getMessageWithNoPings(ch, sb.toString());

                    if (res != null) {
                        Message msg = getMessage(event);

                        if (msg != null)
                            StaticStore.putHolder(u.getId(), new AliasStageMessageHolder(stages, msg, res, ch.getId(), AliasHolder.MODE.REMOVE, lang, getAliasName(getContent(event))));
                    }
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
            result += LangID.getStringByID("stinfo_mc", lang).replace("_", names[0])+", ";
        }

        if(names[1] != null) {
            result += LangID.getStringByID("stinfo_stm", lang).replace("_", names[1])+", ";
        }

        if(names[2] != null) {
            result += LangID.getStringByID("stinfo_st", lang).replace("_", names[2]);
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
}
