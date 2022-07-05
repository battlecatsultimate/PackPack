package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.SearchHolder;
import mandarin.packpack.supporter.server.holder.StageEnemyMessageHolder;
import mandarin.packpack.supporter.server.holder.StageInfoButtonHolder;
import mandarin.packpack.supporter.server.holder.StageInfoMessageHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.List;

public class FindStage extends TimedConstraintCommand {
    private static final int PARAM_SECOND = 2;
    private static final int PARAM_EXTRA = 4;

    private final ConfigHolder config;

    public FindStage(ConstraintCommand.ROLE role, int lang, IDHolder id, ConfigHolder config, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_FINDSTAGE_ID);

        this.config = config;
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String enemyName = getEnemyName(getContent(event));

        if(enemyName.isBlank()) {
            ch.sendMessage(LangID.getStringByID("eimg_more", lang)).queue();
            return;
        }

        int param = checkParameters(getContent(event));
        int star = getLevel(getContent(event));

        boolean isFrame = (param & PARAM_SECOND) == 0 && config.useFrame;
        boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;

        ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(enemyName, lang);

        if (enemies.isEmpty()) {
            createMessageWithNoPings(ch, LangID.getStringByID("enemyst_noenemy", lang).replace("_", enemyName));
            disableTimer();
        } else if(enemies.size() == 1) {
            ArrayList<Stage> stages = EntityFilter.findStageByEnemy(enemies.get(0));

            if(stages.isEmpty()) {
                String eName = StaticStore.safeMultiLangGet(enemies.get(0), lang);

                if(eName == null || eName.isBlank()) {
                    eName = enemies.get(0).names.toString();
                }

                if(eName.isBlank()) {
                    eName = Data.trio(enemies.get(0).id.id);
                }

                createMessageWithNoPings(ch, LangID.getStringByID("fstage_nost", lang).replace("_", eName));

                disableTimer();
            } else if(stages.size() == 1) {
                Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, isExtra, star, lang);

                Member m = getMember(event);

                if(m != null) {
                    Message msg = getMessage(event);

                    if(msg != null) {
                        StaticStore.putHolder(m.getId(), new StageInfoButtonHolder(stages.get(0), msg, result, ch.getId()));
                    }
                }
            } else {
                String eName = StaticStore.safeMultiLangGet(enemies.get(0), lang);

                if(eName == null || eName.isBlank())
                    eName = enemies.get(0).names.toString();

                if(eName.isBlank())
                    eName = Data.trio(enemies.get(0).id.id);

                StringBuilder sb = new StringBuilder(LangID.getStringByID("fstage_several", lang).replace("_", eName)).append("```md\n");
                
                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > SearchHolder.PAGE_CHUNK)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(stages.size()/SearchHolder.PAGE_CHUNK + 1))).append("\n");

                sb.append("```");

                Message res = registerSearchComponents(ch.sendMessage(sb.toString()).allowedMentions(new ArrayList<>()), stages.size(), accumulateStage(stages, false), lang).complete();

                if(res != null) {
                    Member m = getMember(event);

                    if(m != null) {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(m.getId(), new StageInfoMessageHolder(stages, msg, res, ch.getId(), star, isFrame, isExtra, lang));
                        }
                        disableTimer();
                    }
                }
            }
        } else {
            StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", enemyName));

            sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));
            
            List<String> data = accumulateEnemy(enemies);

            for(int i = 0; i < data.size(); i++) {
                sb.append(i+1).append(". ").append(data.get(i)).append("\n");
            }

            if(enemies.size() > SearchHolder.PAGE_CHUNK)
                sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(enemies.size() / SearchHolder.PAGE_CHUNK + 1))).append("\n");
            
            sb.append("```");

            Message res = registerSearchComponents(ch.sendMessage(sb.toString()).allowedMentions(new ArrayList<>()), enemies.size(), data, lang).complete();

            if(res != null) {
                Member m = getMember(event);

                if(m != null) {
                    Message msg = getMessage(event);

                    if(msg != null)
                        StaticStore.putHolder(m.getId(), new StageEnemyMessageHolder(enemies, msg, res, ch.getId(), isFrame, isExtra, star, lang));
                }
            }
        }
    }

    private String getEnemyName(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2)
            return "";

        StringBuilder result = new StringBuilder();

        boolean second = false;
        boolean level = false;

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
            } else if(contents[i].equals("-s") && !second) {
                second = true;
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

            for(String str : pureMessage) {
                if(str.equals("-s")) {
                    if((result & PARAM_SECOND) == 0) {
                        result |= PARAM_SECOND;
                    } else
                        break;
                } else if(str.equals("-e") || str.equals("-extra")) {
                    if((result & PARAM_EXTRA) == 0) {
                        result |= PARAM_EXTRA;
                    } else
                        break;
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
    
    private List<String> accumulateEnemy(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();
        
        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String ename = e.id == null ? "UNKNOWN " : Data.trio(e.id.id)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(e) != null)
                ename += MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(ename);
        }
        
        return data;
    }
    
    private List<String> accumulateStage(List<Stage> stages, boolean full) {
        List<String> data = new ArrayList<>();
        
        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= stages.size())
                break;

            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(full) {
                if(mc != null)
                    name = mc.getSID()+"/";
                else
                    name = "Unknown/";

                if(stm.id != null)
                    name += Data.trio(stm.id.id)+"/";
                else
                    name += "Unknown/";

                if(st.id != null)
                    name += Data.trio(st.id.id)+" | ";
                else
                    name += "Unknown | ";

                if(mc != null) {
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String mcn = MultiLangCont.get(mc);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            }

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String stmn = MultiLangCont.get(stm);

            CommonStatic.getConfig().lang = oldConfig;

            if(stm.id != null) {
                if(stmn == null || stmn.isBlank())
                    stmn = Data.trio(stm.id.id);
            } else {
                if(stmn == null || stmn.isBlank())
                    stmn = "Unknown";
            }

            name += stmn+" - ";

            CommonStatic.getConfig().lang = lang;

            String stn = MultiLangCont.get(st);

            CommonStatic.getConfig().lang = oldConfig;

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
}
