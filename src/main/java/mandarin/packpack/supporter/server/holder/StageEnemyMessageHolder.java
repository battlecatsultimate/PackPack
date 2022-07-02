package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StageEnemyMessageHolder extends MessageHolder<MessageReceivedEvent> {
    private final ArrayList<Enemy> enemy;
    private final Message msg;
    private final String channelID;

    private final int lang;
    private final boolean isFrame;
    private final boolean isExtra;
    private final int star;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public StageEnemyMessageHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, boolean isFrame, boolean isExtra, int star, int lang) {
        super(MessageReceivedEvent.class);

        this.enemy = enemy;
        this.msg = msg;
        this.channelID = channelID;

        this.lang = lang;
        this.isFrame = isFrame;
        this.isExtra = isExtra;
        this.star = star;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public int handleEvent(MessageReceivedEvent event) {
        if(expired) {
            System.out.println("Expired at StageEnemyHolder!!!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel();

        if(!ch.getId().equals(channelID))
            return RESULT_STILL;

        String content = event.getMessage().getContentRaw();

        if(content.equals("n")) {
            if(20 * (page + 1) >= enemy.size())
                return RESULT_STILL;

            page++;

            showPage();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            showPage();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= enemy.size())
                return RESULT_STILL;

            try {
                Enemy e = enemy.get(id);

                ArrayList<Stage> stages = EntityFilter.findStageByEnemy(e);

                if(stages.isEmpty()) {
                    msg.delete().queue();

                    expired = true;

                    cleaner.add(event.getMessage());

                    ch.sendMessage(LangID.getStringByID("fstage_nost", lang)).queue();

                    return RESULT_FINISH;
                } else if(stages.size() == 1) {
                    msg.delete().queue();

                    Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, isExtra, star, lang);
                    Guild g = event.getGuild();

                    if(result != null) {
                        Member m = event.getMember();

                        if(m != null) {
                            if(StaticStore.timeLimit.containsKey(m.getId())) {
                                StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());
                            } else {
                                Map<String, Long> memberLimit = new HashMap<>();

                                memberLimit.put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());

                                StaticStore.timeLimit.put(m.getId(), memberLimit);
                            }

                            if(StaticStore.idHolder.containsKey(g.getId())) {
                                StaticStore.removeHolder(m.getId(), StageEnemyMessageHolder.this);
                                StaticStore.putHolder(m.getId(), new StageInfoButtonHolder(stages.get(0), event.getMessage(), result, channelID, m.getId()));
                            }
                        }
                    }

                    expired = true;

                    cleaner.add(event.getMessage());

                    clean();

                    return RESULT_STILL;
                } else {
                    String eName = StaticStore.safeMultiLangGet(e, lang);

                    if(eName == null || eName.isBlank())
                        eName = e.names.toString();

                    if(eName.isBlank())
                        eName = Data.trio(e.id.id);

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

                    Message res = ch.sendMessage(sb.toString())
                            .allowedMentions(new ArrayList<>())
                            .complete();

                    if(res != null) {
                        Member m = event.getMember();

                        if(m != null) {
                            Message msg = event.getMessage();

                            StaticStore.removeHolder(m.getId(), StageEnemyMessageHolder.this);
                            StaticStore.putHolder(m.getId(), new StageInfoMessageHolder(stages, msg, res, ch.getId(), star, isFrame, isExtra, lang));
                        }
                    }

                    msg.delete().queue();

                    expired = true;

                    cleaner.add(event.getMessage());

                    clean();

                    return RESULT_STILL;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(content.equals("c")) {
            msg.editMessage(LangID.getStringByID("formst_cancel", lang)).queue();

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1])-1;

                    if(p < 0 || p * 20 >= enemy.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    showPage();

                    cleaner.add(event.getMessage());
                }
            }
        }

        return RESULT_STILL;
    }

    @Override
    public void clean() {
        for(Message m : cleaner) {
            if(m != null) {
                m.delete().queue();
            }
        }

        cleaner.clear();
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.editMessage(LangID.getStringByID("formst_expire", lang)).queue();
    }

    private void showPage() {
        String check;

        if(enemy.size() <= 20)
            check = "";
        else if(page == 0)
            check = LangID.getStringByID("formst_next", lang);
        else if((page + 1) * 20 >= enemy.size())
            check = LangID.getStringByID("formst_pre", lang);
        else
            check = LangID.getStringByID("formst_nexpre", lang);

        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        for(int i = 20 * page; i < 20 * (page +1); i++) {
            if(i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String fname = Data.trio(e.id.id) + " - ";

            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(e) != null)
                fname += MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            sb.append(i+1).append(". ").append(fname).append("\n");
        }

        CommonStatic.getConfig().lang = oldConfig;

        if(enemy.size() > 20)
            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

        sb.append(LangID.getStringByID("formst_can", lang));
        sb.append("```");

        msg.editMessage(sb.toString()).queue();
    }
}
