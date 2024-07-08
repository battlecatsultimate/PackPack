package mandarin.packpack.supporter.server.holder.message.alias;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.holder.message.MessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AliasStageMessageHolder extends MessageHolder {
    private final ArrayList<Stage> stage;
    private final Message msg;
    private final String channelID;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    private final int lang;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public AliasStageMessageHolder(ArrayList<Stage> stage, Message author, Message msg, String channelID, AliasHolder.MODE mode, int lang, @Nullable String aliasName) {
        super(author, channelID, msg);

        this.stage = stage;
        this.msg = msg;
        this.channelID = channelID;
        this.mode = mode;
        this.aliasName = aliasName;

        this.lang = lang;

        registerAutoFinish(this, msg, lang, FIVE_MIN);
    }

    @Override
    public STATUS onReceivedEvent(MessageReceivedEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return STATUS.FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel();

        if(!ch.getId().equals(channelID))
            return STATUS.WAIT;

        String content = event.getMessage().getContentRaw();

        if(content.equals("n")) {
            if(20 * (page + 1) >= stage.size()) {
                return STATUS.WAIT;
            }

            page++;

            showPage();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return STATUS.WAIT;

            page--;

            showPage();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content) - 1;

            if(id < 0 || id >= stage.size())
                return STATUS.WAIT;

            msg.delete().queue();

            User u = event.getAuthor();

            String mid = u.getId();

            if(StaticStore.timeLimit.containsKey(mid)) {
                StaticStore.timeLimit.get(mid).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
            } else {
                Map<String, Long> memberLimit = new HashMap<>();

                memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

                StaticStore.timeLimit.put(mid, memberLimit);
            }

            String stName = StaticStore.safeMultiLangGet(stage.get(id), lang);

            if(stName == null || stName.isBlank())
                stName = stage.get(id).name;

            if(stName == null || stName.isBlank())
                stName = stage.get(id).getCont().getSID() + "-" + Data.trio(stage.get(id).getCont().id.id) + "-" + Data.trio(stage.get(id).id.id);

            ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.STAGE, lang, stage.get(id));

            switch (mode) {
                case GET -> {
                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", stName));
                    } else {
                        StringBuilder result = new StringBuilder(LangID.getStringByID("alias_stalias", lang).replace("_SSS_", stName).replace("_NNN_", String.valueOf(alias.size())));
                        result.append("\n\n");

                        for (int i = 0; i < alias.size(); i++) {
                            String temp = "- " + alias.get(i);

                            if (result.length() + temp.length() > 1900) {
                                result.append("\n")
                                        .append(LangID.getStringByID("alias_etc", lang));
                                break;
                            }

                            result.append(temp);

                            if (i < alias.size() - 1) {
                                result.append("\n");
                            }
                        }

                        createMessageWithNoPings(ch, result.toString());
                    }
                }
                case ADD -> {
                    if (alias == null)
                        alias = new ArrayList<>();
                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        break;
                    }
                    if (alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_contain", lang).replace("_", stName));
                        break;
                    }
                    alias.add(aliasName);
                    AliasHolder.SALIAS.put(AliasHolder.getLangCode(lang), stage.get(id), alias);
                    createMessageWithNoPings(ch, LangID.getStringByID("alias_added", lang).replace("_DDD_", stName).replace("_AAA_", aliasName));
                    StaticStore.logger.uploadLog("Alias added\n\nStage : " + stName + "\nAlias : " + aliasName + "\nBy : " + event.getAuthor().getAsMention());
                }
                case REMOVE -> {
                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", stName));
                        break;
                    }
                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        break;
                    }
                    if (!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_nosuch", lang).replace("_", stName));
                        break;
                    }
                    alias.remove(aliasName);
                    AliasHolder.SALIAS.put(AliasHolder.getLangCode(lang), stage.get(id), alias);
                    createMessageWithNoPings(ch, LangID.getStringByID("alias_removed", lang).replace("_DDD_", stName).replace("_AAA_", aliasName));
                    StaticStore.logger.uploadLog("Alias removed\n\nStage : " + stName + "\nAlias : " + aliasName + "\nBy : " + event.getAuthor().getAsMention());
                }
            }

            expired = true;

            cleaner.add(event.getMessage());

            clean();

            return STATUS.WAIT;
        } else if(content.equals("c")) {
            msg.editMessage(LangID.getStringByID("formst_cancel", lang)).queue();

            expired = true;

            cleaner.add(event.getMessage());

            return STATUS.FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1]) -1;

                    if(p < 0 || p * 20 >= stage.size()) {
                        return STATUS.WAIT;
                    }

                    page = p;

                    showPage();

                    cleaner.add(event.getMessage());
                }
            }
        }

        return STATUS.WAIT;
    }

    @Override
    public void clean() {
        for(Message m : cleaner) {
            if (m == null)
                continue;

            if (m.getChannel() instanceof PrivateChannel)
                return;

            m.delete().queue();
        }
    }

    @Override
    public void onExpire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.editMessage(LangID.getStringByID("formst_expire", lang))
                .mentionRepliedUser(false)
                .queue();
    }

    private void showPage() {
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
                String mcn = MultiLangCont.get(mc, lang);

                if(mcn == null || mcn.isBlank())
                    mcn = mc.getSID();

                name += mcn+" - ";
            } else {
                name += "Unknown - ";
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

            sb.append(i+1).append(". ").append(name).append("\n");
        }

        if(stage.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = stage.size() / SearchHolder.PAGE_CHUNK;

            if(stage.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            sb.append(LangID.getStringByID("formst_page", lang).formatted(page + 1, totalPage));
        }

        sb.append(LangID.getStringByID("formst_can", lang));
        sb.append("```");

        msg.editMessage(sb.toString()).queue();
    }
}
