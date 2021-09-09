package mandarin.packpack.supporter.server.holder.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.holder.Holder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AliasStageHolder extends Holder<MessageCreateEvent> {
    private final ArrayList<Stage> stage;
    private final Message msg;
    private final String channelID;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    private final int lang;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public AliasStageHolder(ArrayList<Stage> stage, Message author, Message msg, String channelID, AliasHolder.MODE mode, int lang, @Nullable String aliasName) {
        super(MessageCreateEvent.class);

        this.stage = stage;
        this.msg = msg;
        this.channelID = channelID;
        this.mode = mode;
        this.aliasName = aliasName;

        this.lang = lang;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public int handleEvent(MessageCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID))
            return RESULT_STILL;

        String content = event.getMessage().getContent();

        if(content.equals("n")) {
            if(20 * (page + 1) >= stage.size()) {
                return RESULT_STILL;
            }

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
            int id = StaticStore.safeParseInt(content) - 1;

            if(id < 0 || id >= stage.size())
                return RESULT_STILL;

            msg.delete().subscribe();

            event.getMember().ifPresent(m -> {
                String mid = m.getId().asString();

                if(StaticStore.timeLimit.containsKey(mid)) {
                    StaticStore.timeLimit.get(mid).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
                } else {
                    Map<String, Long> memberLimit = new HashMap<>();

                    memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

                    StaticStore.timeLimit.put(mid, memberLimit);
                }
            });

            String stName = StaticStore.safeMultiLangGet(stage.get(id), lang);

            if(stName == null || stName.isBlank())
                stName = stage.get(id).name;

            if(stName == null || stName.isBlank())
                stName = stage.get(id).getCont().getSID() + "-" + Data.trio(stage.get(id).getCont().id.id) + "-" + Data.trio(stage.get(id).id.id);

            ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.STAGE, lang, stage.get(id));

            switch (mode) {
                case GET:
                    if(alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", stName));
                    } else {
                        StringBuilder result = new StringBuilder(LangID.getStringByID("alias_stalias", lang).replace("_SSS_", stName).replace("_NNN_", alias.size()+""));
                        result.append("\n\n");

                        for(int i = 0; i < alias.size(); i++) {
                            String temp = " - " + alias.get(i);

                            if(result.length() + temp.length() > 1900) {
                                result.append("\n")
                                        .append(LangID.getStringByID("alias_etc", lang));
                                break;
                            }

                            result.append(temp);

                            if(i < alias.size() - 1) {
                                result.append("\n");
                            }
                        }

                        createMessageWithNoPings(ch, result.toString());
                    }
                    break;
                case ADD:
                    if(alias == null)
                        alias = new ArrayList<>();

                    if(aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        break;
                    }

                    if(alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_contain", lang).replace("_", stName));
                        break;
                    }

                    alias.add(aliasName);

                    AliasHolder.SALIAS.put(AliasHolder.getLangCode(lang), stage.get(id), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_added", lang).replace("_DDD_", stName).replace("_AAA_", aliasName));
                    break;
                case REMOVE:
                    if(alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", stName));
                        break;
                    }

                    if(aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        break;
                    }

                    if(!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_nosuch", lang).replace("_", stName));
                        break;
                    }

                    alias.remove(aliasName);

                    AliasHolder.SALIAS.put(AliasHolder.getLangCode(lang), stage.get(id), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_removed", lang).replace("_DDD_", stName).replace("_AAA_", aliasName));
                    break;
            }

            expired = true;

            cleaner.add(event.getMessage());

            clean();

            return RESULT_STILL;
        } else if(content.equals("c")) {
            msg.edit(m -> {
                m.setContent(LangID.getStringByID("formst_cancel", lang));
                expired = true;
            }).subscribe();

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1]) -1;

                    if(p < 0 || p * 20 >= stage.size()) {
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

    private void showPage() {
        msg.edit(m -> {
            String check;

            if(stage.size() <= 20)
                check = "";
            else if(page == 0)
                check = LangID.getStringByID("formst_next", lang);
            else if((page + 1) * 20 >= stage.size())
                check = LangID.getStringByID("formst_pre", lang);
            else
                check = LangID.getStringByID("formst_nexpre", lang);

            StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

            for(int i = 20 * page; i < 20 * (page + 1); i++) {
                if(i >= stage.size())
                    break;

                Stage st = stage.get(i);
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

            if(stage.size() > 20)
                sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(stage.size()/20 + 1)));

            sb.append(LangID.getStringByID("formst_can", lang));
            sb.append("```");

            m.setContent(sb.toString());
        }).subscribe();
    }

    @Override
    public void clean() {
        for(Message m : cleaner) {
            if(m != null)
                m.delete().subscribe();
        }
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();
    }
}
