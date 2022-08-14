package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.SearchHolder;
import mandarin.packpack.supporter.server.holder.StageInfoButtonHolder;
import mandarin.packpack.supporter.server.holder.StageInfoMessageHolder;
import mandarin.packpack.supporter.server.holder.StageReactionSlashMessageHolder;
import mandarin.packpack.supporter.server.slash.SlashOption;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.ArrayList;
import java.util.List;

public class StageInfo extends TimedConstraintCommand {
    private static final int PARAM_SECOND = 2;
    private static final int PARAM_EXTRA = 4;
    private static final int PARAM_COMPACT = 8;

    public static void performInteraction(GenericCommandInteractionEvent event) {
        Interaction interaction = event.getInteraction();

        if(event.getOptions().isEmpty()) {
            StaticStore.logger.uploadLog("E/StageInfo::performInteraction - Options are absent in StageInfo!");

            return;
        }

        List<OptionMapping> options = event.getOptions();

        int lang = LangID.EN;

        IDHolder holder;

        if(interaction.getGuild() != null) {
            String gID = interaction.getGuild().getId();

            holder = StaticStore.idHolder.get(gID);

            if(holder == null) {
                event.deferReply().setContent("Bot couldn't get guild data").queue();

                return;
            }
        } else {
            event.deferReply().setContent("Please use this command in any server where PackPack is in!").queue();

            return;
        }

        if(interaction.getMember() != null) {
            Member member = interaction.getMember();

            if(StaticStore.config.containsKey(member.getId())) {
                lang = StaticStore.config.get(member.getId()).lang;

                if(lang == -1) {
                    if(interaction.getGuild() == null) {
                        lang = LangID.EN;
                    } else {
                        IDHolder idh = StaticStore.idHolder.get(interaction.getGuild().getId());

                        if(idh == null) {
                            lang = LangID.EN;
                        } else {
                            lang = idh.serverLocale;
                        }
                    }
                }
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
        boolean extra = SlashOption.getBooleanOption(options, "extra", false);
        int star = SlashOption.getIntOption(options, "level", 0);

        if(name[2].isBlank() || st == null) {
            event.deferReply().setContent(LangID.getStringByID("formst_specific", lang)).queue();
        } else {
            try {
                Message m = EntityHandler.performStageEmb(st, event, frame, extra, star, lang);

                if(m != null && interaction.getMember() != null && m.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE)) {
                    Member member = interaction.getMember();

                    StaticStore.putHolder(
                            member.getId(),
                            new StageReactionSlashMessageHolder(m, st, m.getId(), m.getChannel().getId(), member.getId(), holder, lang)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final ConfigHolder config;

    public StageInfo(ConstraintCommand.ROLE role, int lang, IDHolder id, ConfigHolder config, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_STAGEINFO_ID);

        this.config = config;
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getContent(event).split(" ", 2);

        String[] names = generateStageNameSeries(getContent(event));

        if(list.length == 1 || allNull(names)) {
            ch.sendMessage(LangID.getStringByID("stinfo_noname", lang)).queue();
        } else {
            ArrayList<Stage> stages = EntityFilter.findStageWithName(names, lang);

            if(stages.isEmpty() && names[0] == null && names[1] == null) {
                stages = EntityFilter.findStageWithMapName(names[2]);

                if(!stages.isEmpty()) {
                    ch.sendMessage(LangID.getStringByID("stinfo_smart", lang)).queue();
                }
            }

            if(stages.isEmpty()) {
                createMessageWithNoPings(ch, LangID.getStringByID("stinfo_nores", lang).replace("_", generateSearchName(names)));
            } else if(stages.size() == 1) {
                int param = checkParameters(getContent(event));
                int star = getLevel(getContent((event)));
                boolean isFrame = (param & PARAM_SECOND) == 0 && config.useFrame;
                boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
                boolean isCompact = (param & PARAM_COMPACT) > 0 || config.compact;

                CommonStatic.getConfig().lang = lang;

                Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, isExtra, isCompact, star, lang);

                Member m = getMember(event);

                if(m != null) {
                    Message author = getMessage(event);

                    if(author != null)
                        StaticStore.putHolder(m.getId(), new StageInfoButtonHolder(stages.get(0), author, result, ch.getId()));
                }
            } else {
                int param = checkParameters(getContent(event));
                int star = getLevel(getContent((event)));
                boolean isFrame = (param & PARAM_SECOND) == 0 && config.useFrame;
                boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
                boolean isCompact = (param & PARAM_COMPACT) > 0 || config.compact;

                StringBuilder sb = new StringBuilder(LangID.getStringByID("stinfo_several", lang).replace("_", generateSearchName(names)))
                        .append("```md\n")
                        .append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateData(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > SearchHolder.PAGE_CHUNK)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(stages.size()/SearchHolder.PAGE_CHUNK + 1))).append("\n");

                sb.append("```");

                Message res = registerSearchComponents(ch.sendMessage(sb.toString()).allowedMentions(new ArrayList<>()), stages.size(), accumulateData(stages, false), lang).complete();

                if(res != null) {
                    Member member = getMember(event);

                    if(member != null) {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(member.getId(), new StageInfoMessageHolder(stages, msg, res, ch.getId(), star, isFrame, isExtra, isCompact, lang));
                    }
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
                if(str.startsWith("-s") && !str.startsWith("-stm")) {
                    if((result & PARAM_SECOND) == 0) {
                        result |= PARAM_SECOND;
                    } else
                        break;
                } else if(str.equals("-e") || str.equals("-extra")) {
                    if((result & PARAM_EXTRA) == 0) {
                        result |= PARAM_EXTRA;
                    } else
                        break;
                } else if(str.equals("-c") || str.equals("-compact")) {
                    if((result & PARAM_COMPACT) == 0) {
                        result |= PARAM_COMPACT;
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
            else if(contents[i].equals("-lv") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1]))
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
            else if(contents[i].equals("-lv") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1]))
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
        boolean isExtra = false;
        boolean isCompact = false;

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
            } else if(contents[i].equals("-e") || contents[i].equals("-extra")) {
                if(!isExtra) {
                    isExtra = true;
                } else {
                    st.append(contents[i]).append(" ");
                }
            } else if(contents[i].equals("-c") || contents[i].equals("-compact")) {
                if(!isCompact) {
                    isCompact = true;
                } else {
                    st.append(contents[i]).append(" ");
                }
            } else if(contents[i].equals("-lv") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
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

    private List<String> accumulateData(List<Stage> stages, boolean full) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= stages.size())
                break;

            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name;

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
            } else {
                name = "";
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
