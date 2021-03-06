package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.*;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.StageInfoHolder;
import mandarin.packpack.supporter.server.holder.StageReactionHolder;
import mandarin.packpack.supporter.server.holder.StageReactionSlashHolder;
import mandarin.packpack.supporter.server.slash.SlashBuilder;
import mandarin.packpack.supporter.server.slash.SlashOption;
import mandarin.packpack.supporter.server.slash.WebhookBuilder;

import java.util.ArrayList;
import java.util.List;

public class StageInfo extends TimedConstraintCommand {
    private static final int PARAM_SECOND = 2;

    public static WebhookBuilder getInteractionWebhook(InteractionCreateEvent event) {
        InteractionData interaction = event.getInteraction().getData();

        if(interaction.data().isAbsent()) {
            System.out.println("Data is absent in StageInfo!");
            return null;
        }

        ApplicationCommandInteractionData data = interaction.data().get();

        if(data.options().isAbsent()) {
            System.out.println("Options are absent in StageInfo!");
            return null;
        }

        List<ApplicationCommandInteractionOptionData> options = data.options().get();

        int lang = LangID.EN;

        IDHolder holder;

        if(!interaction.guildId().isAbsent()) {
            String gID = interaction.guildId().get();

            if(gID.equals(StaticStore.BCU_KR_SERVER))
                lang = LangID.KR;

            holder = StaticStore.idHolder.get(gID);

            if(holder == null) {
                return SlashBuilder.getWebhookRequest(w -> w.setContent("Bot couldn't get guild data"));
            }
        } else {
            return SlashBuilder.getWebhookRequest(w -> w.setContent("Please use this command in any server where PackPack is in!"));
        }

        if(!interaction.member().isAbsent()) {
            MemberData member = interaction.member().get();

            if(StaticStore.locales.containsKey(member.user().id().asString())) {
                lang = StaticStore.locales.get(member.user().id().asString());
            }
        }

        String[] name = {
                SlashOption.getStringOption(options, "map_collection", ""),
                SlashOption.getStringOption(options, "stage_map", ""),
                SlashOption.getStringOption(options, "name", "")
        };

        Stage st;

        if(name[2].isBlank()) {
            st = null;
        } else {
            st = EntityFilter.pickOneStage(name, lang);
        }

        boolean frame = SlashOption.getBooleanOption(options, "frame", true);
        int star = SlashOption.getIntOption(options, "star", 0);

        final int finalLang = lang;

        return SlashBuilder.getWebhookRequest(w -> {
            if(name[2].isBlank() || st == null) {
                w.setContent(LangID.getStringByID("formst_specific", finalLang));
            } else {
                try {
                    EntityHandler.showStageEmb(st, w, frame, star, finalLang);

                    w.addPostHandler((g, m) -> {
                        if(!interaction.member().isAbsent()) {
                            MemberData member = interaction.member().get();

                            StaticStore.putHolder(
                                    member.user().id().asString(),
                                    new StageReactionSlashHolder(g, st, m.id().asLong(), m.channelId().asLong(), member.user().id().asString(), holder, finalLang)
                            );
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public StageInfo(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_STAGEINFO_ID);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getContent(event).split(" ", 2);

        String[] names = generateStageNameSeries(getContent(event));

        if(list.length == 1 || allNull(names)) {
            ch.createMessage(LangID.getStringByID("stinfo_noname", lang)).subscribe();
        } else {
            ArrayList<Stage> stages = EntityFilter.findStageWithName(names, lang);

            if(stages.isEmpty()) {
                createMessageWithNoPings(ch, LangID.getStringByID("stinfo_nores", lang).replace("_", generateSearchName(names)));
            } else if(stages.size() == 1) {
                int param = checkParameters(getContent(event));
                int star = getStar(getContent((event)));
                boolean isFrame = (param & PARAM_SECOND) == 0;

                CommonStatic.getConfig().lang = lang;

                Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, star, lang);

                getMember(event).ifPresent(m -> {
                    Message author = getMessage(event);

                    if(author != null)
                        StaticStore.putHolder(m.getId().asString(), new StageReactionHolder(stages.get(0), author, result, holder, lang, ch.getId().asString(), m.getId().asString()));
                });
            } else {
                int param = checkParameters(getContent(event));
                int star = getStar(getContent((event)));
                boolean isFrame = (param & PARAM_SECOND) == 0;

                CommonStatic.getConfig().lang = lang;

                String check;

                if(stages.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                StringBuilder sb = new StringBuilder(LangID.getStringByID("stinfo_several", lang).replace("_", generateSearchName(names))).append("```md\n").append(check);

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

                Message res = getMessageWithNoPings(ch, sb.toString());

                if(res != null) {
                    getMember(event).ifPresent(member -> {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(member.getId().asString(), new StageInfoHolder(stages, msg, res, ch.getId().asString(), star, isFrame, lang));
                    });
                }
            }
        }
    }

    private boolean allNull(String[] name) {
        return name[0] == null && name[1] == null && name[2] == null;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.startsWith("-s") && !str.startsWith("-st") && !str.startsWith("-stm")) {
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
            else if(contents[i].equals("-st") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1]))
                i++;
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
            else if(contents[i].equals("-st") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1]))
                i++;
            else
                stm.append(contents[i]).append(" ");
        }

        String res = stm.toString();

        if(res.endsWith(" "))
            res = res.substring(0, res.length() - 1);

        return res;
    }

    private String getStageName(String[] contents) {
        boolean isSecond = false;

        StringBuilder st = new StringBuilder();

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-mc") || contents[i].equals("-stm"))
                break;
            else if(contents[i].equals("-s")) {
                if(!isSecond) {
                    isSecond = true;
                } else {
                    st.append(contents[i]).append(" ");
                }
            } else if(contents[i].equals("-st") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                i++;
            } else {
                st.append(contents[i]).append(" ");
            }
        }

        String res = st.toString();

        if(res.endsWith(" "))
            res = res.substring(0, res.length() - 1);

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
}
