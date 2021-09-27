package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StageInfoHolder extends Holder<MessageCreateEvent> {
    private final ArrayList<Stage> stage;
    private final Message msg;
    private final String channelID;

    private int page = 0;

    private final boolean isFrame;
    private final int star;
    private final int lang;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public StageInfoHolder(ArrayList<Stage> stage, Message author, Message msg, String channelID, int star, boolean isFrame, int lang) {
        super(MessageCreateEvent.class);

        this.stage = stage;
        this.msg = msg;
        this.channelID = channelID;

        this.star = star;
        this.isFrame = isFrame;
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

            try {
                Message msg = EntityHandler.showStageEmb(stage.get(id), ch, isFrame, star, lang);

                Guild g = event.getGuild().block();

                if(msg != null && g != null && StaticStore.idHolder.containsKey(g.getId().asString())) {
                    IDHolder holder = StaticStore.idHolder.get(g.getId().asString());

                    event.getMember().ifPresent(m -> {
                        StaticStore.removeHolder(m.getId().asString(), StageInfoHolder.this);
                        StaticStore.putHolder(m.getId().asString(), new StageReactionHolder(stage.get(id), event.getMessage(), msg, holder, lang, channelID, m.getId().asString()));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            expired = true;

            cleaner.add(event.getMessage());

            clean();

            return RESULT_STILL;
        } else if(content.equals("c")) {
            Command.editMessage(msg, m -> {
                m.content(wrap(LangID.getStringByID("formst_cancel", lang)));
                expired = true;
            });

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
        Command.editMessage(msg, m -> {
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

            m.content(wrap(sb.toString()));
        });
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

        Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID("formst_expire", lang))));
    }
}
