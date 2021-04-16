package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;
import mandarin.packpack.supporter.server.StageEnemyHolder;
import mandarin.packpack.supporter.server.StageInfoHolder;
import mandarin.packpack.supporter.server.StageReactionHolder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

public class FindStage extends TimedConstraintCommand {
    private static final int PARAM_SECOND = 2;

    public FindStage(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_FINDSTAGE_ID);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String enemyName = getEnemyName(getContent(event));

        if(enemyName.isBlank()) {
            ch.createMessage(LangID.getStringByID("eimg_more", lang)).subscribe();
            return;
        }

        int param = checkParameters(getContent(event));
        int star = getStar(getContent(event));

        boolean isFrame = (param & PARAM_SECOND) == 0;

        ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(enemyName, lang);

        if (enemies.isEmpty()) {
            ch.createMessage(LangID.getStringByID("enemyst_noenemy", lang)).subscribe();
            disableTimer();
        } else if(enemies.size() == 1) {
            ArrayList<Stage> stages = EntityFilter.findStageByEnemy(enemies.get(0));

            if(stages.isEmpty()) {
                ch.createMessage(LangID.getStringByID("fstage_nost", lang)).subscribe();
                disableTimer();
            } else if(stages.size() == 1) {
                Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, star, lang);
                Mono<Guild> mono = getGuild(event);

                if(mono != null) {
                    Guild g = mono.block();

                    getMember(event).ifPresent(m -> {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(m.getId().asString(), new StageReactionHolder(stages.get(0), msg, result, holder, lang, ch.getId().asString(), m.getId().asString()));
                        }
                    });
                }
            } else {
                String eName = StaticStore.safeMultiLangGet(enemies.get(0), lang);

                if(eName == null || eName.isBlank())
                    eName = enemies.get(0).name;

                if(eName == null || eName.isBlank())
                    eName = Data.trio(enemies.get(0).id.id);

                String check;

                if(stages.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                StringBuilder sb = new StringBuilder(LangID.getStringByID("fstage_several", lang).replace("_", eName)).append("```md\n").append(check);

                for(int i = 0; i < 20; i++) {
                    if(i >= stages.size())
                        break;

                    Stage st = stages.get(i);
                    StageMap stm = st.getCont();
                    MapColc mc = stm.getCont();

                    String name;

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

                    sb.append(i+1).append(". ").append(name).append("\n");
                }

                if(stages.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(stages.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                Message res = ch.createMessage(sb.toString()).block();

                if(res != null) {
                    getMember(event).ifPresent(member -> {
                        Message msg = getMessage(event);

                        if(msg != null) {
                            StaticStore.putHolder(member.getId().asString(), new StageInfoHolder(stages, msg, res, ch.getId().asString(), star, isFrame, lang));
                        }
                        disableTimer();
                    });
                }
            }
        } else {
            StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", enemyName));

            String check;

            if(enemies.size() <= 20)
                check = "";
            else
                check = LangID.getStringByID("formst_next", lang);

            sb.append("```md\n").append(check);

            for(int i = 0; i < 20; i++) {
                if(i >= enemies.size())
                    break;

                Enemy e = enemies.get(i);

                String ename = e.id == null ? "UNKNOWN " : Data.trio(e.id.id)+" ";

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                if(MultiLangCont.get(e) != null)
                    ename += MultiLangCont.get(e);

                CommonStatic.getConfig().lang = oldConfig;

                sb.append(i+1).append(". ").append(ename).append("\n");
            }

            if(enemies.size() > 20)
                sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(enemies.size() / 20 + 1)));

            sb.append(LangID.getStringByID("formst_can", lang));
            sb.append("```");

            Message res = ch.createMessage(sb.toString()).block();

            if(res != null) {
                getMember(event).ifPresent(m -> {
                    Message msg = getMessage(event);

                    if(msg != null)
                        StaticStore.putHolder(m.getId().asString(), new StageEnemyHolder(enemies, msg, res, ch.getId().asString(), isFrame, star, lang));
                });
            }
        }
    }

    private String getEnemyName(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2)
            return "";

        StringBuilder result = new StringBuilder();

        boolean second = false;
        boolean star = false;

        for(int i = 1; i < contents.length; i++) {
            if(contents[i].equals("-st") && !star) {
                if(i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    star = true;
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
                }
            }
        }

        return result;
    }

    private int getStar(String command) {
        int star = 0;

        if(command.contains("-st")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if(contents[i].equals("-st") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    star = StaticStore.safeParseInt(contents[i+1]);
                    break;
                }
            }
        }

        return star;
    }
}
