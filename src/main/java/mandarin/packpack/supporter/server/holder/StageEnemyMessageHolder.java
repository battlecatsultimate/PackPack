package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StageEnemyMessageHolder extends SearchHolder {
    private final List<List<Enemy>> enemySequences;
    private final List<Enemy> filterEnemy;
    private final StringBuilder enemyList;

    private List<Enemy> enemy;

    private final boolean isFrame;
    private final boolean isExtra;
    private final boolean isCompact;
    private final boolean orOperate;
    private final boolean hasBoss;

    private final int star;
    private final int background;
    private final int castle;
    private final int music;

    public StageEnemyMessageHolder(List<List<Enemy>> enemySequences, List<Enemy> filterEnemy, StringBuilder enemyList, Message author, Message msg, String channelID, boolean isFrame, boolean isExtra, boolean isCompact, boolean orOperate, boolean hasBoss, int star, int background, int castle, int music, int lang) {
        super(msg, author, channelID, lang);

        this.enemySequences = enemySequences;
        this.filterEnemy = filterEnemy;
        this.enemyList = enemyList;

        this.isFrame = isFrame;
        this.isExtra = isExtra;
        this.isCompact = isCompact;
        this.orOperate = orOperate;
        this.hasBoss = hasBoss;

        this.star = star;
        this.background = background;
        this.castle = castle;
        this.music = music;

        enemy = enemySequences.remove(0);

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        int oldConfig = CommonStatic.getConfig().lang;

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String ename = Data.trio(e.id.id) + " - ";

            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(e) != null)
                ename += MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(ename);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();
        Guild g = event.getGuild();
        Message author = getAuthorMessage();

        if(g == null || author == null)
            return;

        try {
            ArrayList<Stage> stages = EntityFilter.findStage(filterEnemy, music, background, castle, hasBoss, orOperate);

            if(stages.isEmpty()) {
                msg.delete().queue();

                ch.sendMessage(LangID.getStringByID("fstage_nost", lang)).queue();
            } else if(stages.size() == 1) {
                msg.delete().queue();

                Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, isExtra, isCompact, star, lang);

                if(result != null) {
                    if(StaticStore.timeLimit.containsKey(author.getAuthor().getId())) {
                        StaticStore.timeLimit.get(author.getAuthor().getId()).put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());
                    } else {
                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(author.getAuthor().getId(), memberLimit);
                    }

                    if(StaticStore.idHolder.containsKey(g.getId())) {
                        StaticStore.putHolder(author.getAuthor().getId(), new StageInfoButtonHolder(stages.get(0), author, result, channelID));
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("fstage_several", lang)).append("```md\n");

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > PAGE_CHUNK) {
                    int totalPage = stages.size() / PAGE_CHUNK;

                    if(stages.size() % PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = Command.registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), stages.size(), accumulateStage(stages, false), lang).complete();

                if(res != null) {
                    StaticStore.putHolder(author.getAuthor().getId(), new StageInfoMessageHolder(stages, author, res, ch.getId(), star, isFrame, isExtra, isCompact, lang));
                }

                msg.delete().queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        Enemy e = enemy.get(id);

        filterEnemy.add(e);

        String n = StaticStore.safeMultiLangGet(e, lang);

        if(n == null || n.isBlank())
            n = Data.trio(e.id.id);

        enemyList.append(n).append(", ");

        if(enemySequences.isEmpty()) {
            super.finish(event);
        } else {
            enemy = enemySequences.remove(0);
            page = 0;

            apply(event);
        }
    }

    @Override
    protected String getPage() {
        StringBuilder sb = new StringBuilder();

        if(enemyList.length() != 0) {
            sb.append(LangID.getStringByID("fstage_selected", lang).replace("_", enemyList.toString().replaceAll(", $", "")));
        }

        sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

        List<String> data = accumulateListData(false);

        for(int i = 0; i < data.size(); i++) {
            sb.append(page * PAGE_CHUNK + i + 1).append(". ").append(data.get(i)).append("\n");
        }

        if(enemy.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = enemy.size() / SearchHolder.PAGE_CHUNK;

            if(enemy.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            sb.append(LangID.getStringByID("formst_page", lang).replace("_", (page + 1) + "").replace("-", String.valueOf(totalPage))).append("\n");
        }

        sb.append("```");

        return sb.toString();
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }

    private List<String> accumulateStage(List<Stage> stages, boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < PAGE_CHUNK; i++) {
            if(i >= stages.size())
                break;

            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(onText) {
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
