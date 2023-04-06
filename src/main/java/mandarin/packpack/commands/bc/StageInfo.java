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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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

    private static final int STAGE = 0;
    private static final int MAP = 1;
    private static final int COLLECTION = 2;

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
            holder = null;
        }

        User u = interaction.getUser();

        if(StaticStore.config.containsKey(u.getId())) {
            lang = StaticStore.config.get(u.getId()).lang;

            if(lang == -1) {
                if(interaction.getGuild() == null) {
                    lang = LangID.EN;
                } else {
                    IDHolder idh = StaticStore.idHolder.get(interaction.getGuild().getId());

                    if(idh == null) {
                        lang = LangID.EN;
                    } else {
                        lang = idh.config.lang;
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

                if(m != null && (!(m.getChannel() instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE))) {
                    StaticStore.putHolder(
                            u.getId(),
                            new StageReactionSlashMessageHolder(m, st, m.getId(), m.getChannel().getId(), u.getId(), holder, lang)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final ConfigHolder config;

    public StageInfo(ConstraintCommand.ROLE role, int lang, IDHolder id, ConfigHolder config, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_STAGEINFO_ID, false);

        if(config == null)
            this.config = id == null ? StaticStore.defaultConfig : id.config;
        else
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

        String[] segments = getContent(event).split(" ");

        StringBuilder removeMistake = new StringBuilder();

        for(int i = 0; i < segments.length; i++) {
            if(segments[i].matches("-lv(l)?(\\d+(,)?)+")) {
                removeMistake.append("-lv ").append(segments[i].replace("-lvl", "").replace("-lv", ""));
            } else {
                removeMistake.append(segments[i]);
            }

            if(i < segments.length - 1)
                removeMistake.append(" ");
        }

        String command = removeMistake.toString();
        String[] list = command.split(" ", 2);

        String[] names = filterStageNames(command);

        if(list.length == 1 || allNull(names)) {
            replyToMessageSafely(ch, LangID.getStringByID("stinfo_noname", lang), getMessage(event), a -> a);

            disableTimer();
        } else {
            ArrayList<Stage> stages = EntityFilter.findStageWithName(names, lang);

            if(stages.isEmpty() && names[0] == null && names[1] == null) {
                stages = EntityFilter.findStageWithMapName(names[2]);

                if(!stages.isEmpty()) {
                    ch.sendMessage(LangID.getStringByID("stinfo_smart", lang)).queue();
                }
            }

            if(stages.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("stinfo_nores", lang).replace("_", generateSearchName(names)), getMessage(event), a -> a);

                disableTimer();
            } else if(stages.size() == 1) {
                int param = checkParameters(command);
                int star = getLevel(command);
                boolean isFrame = (param & PARAM_SECOND) == 0 && config.useFrame;
                boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
                boolean isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);

                CommonStatic.getConfig().lang = lang;

                Message result = EntityHandler.showStageEmb(stages.get(0), ch, getMessage(event), isFrame, isExtra, isCompact, star, calculateItFCrystal(getContent(event)), calculateCotCCrystal(getContent(event)), lang);

                User u = getUser(event);

                if(u != null) {
                    Message author = getMessage(event);

                    if(author != null)
                        StaticStore.putHolder(u.getId(), new StageInfoButtonHolder(stages.get(0), author, result, ch.getId(), isCompact));
                }
            } else {
                int param = checkParameters(command);
                int star = getLevel(command);
                boolean isFrame = (param & PARAM_SECOND) == 0 && config.useFrame;
                boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
                boolean isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);

                StringBuilder sb = new StringBuilder(LangID.getStringByID("stinfo_several", lang).replace("_", generateSearchName(names)))
                        .append("```md\n")
                        .append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateData(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = stages.size() / SearchHolder.PAGE_CHUNK;

                    if(stages.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                ArrayList<Stage> finalStages = stages;

                Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, finalStages.size(), accumulateData(finalStages, false), lang));

                if(res != null) {
                    User u = getUser(event);

                    if(u != null) {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(u.getId(), new StageInfoMessageHolder(stages, msg, res, ch.getId(), star, calculateItFCrystal(getContent(event)), calculateCotCCrystal(getContent(event)), isFrame, isExtra, isCompact, lang));
                    }
                }

                disableTimer();
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
                if((contents[i].equals("-lv") || contents[i].equals("-lvl")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    level = StaticStore.safeParseInt(contents[i+1]);
                    break;
                }
            }
        }

        return level;
    }

    private String[] filterStageNames(String command) {
        String[] contents = command.split(" ");

        String[] names = new String[3];

        StringBuilder stage = new StringBuilder();
        StringBuilder map = new StringBuilder();
        StringBuilder collection = new StringBuilder();

        int mode = 0;

        boolean second = false;
        boolean level = false;
        boolean stm = false;
        boolean mc = false;
        boolean extra = false;
        boolean compact = false;
        boolean itf = false;
        boolean cotc = false;

        for(int i = 1; i < contents.length; i++) {
            if(!second && contents[i].equals("-s")) {
                second = true;
            } else if(!level && contents[i].matches("-lv(l)?") && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                level = true;
                i++;
            } else if(!stm && contents[i].equals("-stm")) {
                stm = true;
                mode = MAP;
            } else if(!mc && contents[i].equals("-mc")) {
                mc = true;
                mode = COLLECTION;
            } else if(!extra && contents[i].matches("-e(xtra)?")) {
                extra = true;
            } else if(!compact && contents[i].equals("-c(ompact)?")) {
                compact = true;
            } else if(!itf && contents[i].matches("-i(tf)?\\d")) {
                itf = true;
            } else if(!cotc && contents[i].matches("-c(otc)?\\d")) {
                cotc = true;
            } else {
                switch (mode) {
                    case STAGE:
                        stage.append(contents[i]);

                        if(i < contents.length - 1) {
                            stage.append(" ");
                        }

                        break;
                    case MAP:
                        map.append(contents[i]);

                        if(i < contents.length - 1) {
                            map.append(" ");
                        }

                        break;
                    case COLLECTION:
                        collection.append(contents[i]);

                        if(i < contents.length - 1) {
                            collection.append(" ");
                        }

                        break;
                }
            }
        }

        String result = stage.toString().trim();

        if(!result.isBlank()) {
            names[2] = result;
        }

        result = map.toString().trim();

        if(!result.isBlank()) {
            names[1] = result;
        }

        result = collection.toString().trim();

        if(!result.isBlank()) {
            names[0] = result;
        }

        return names;
    }

    private String generateSearchName(String[] names) {
        String result = "";

        for(int i = 0; i < names.length; i++) {
            if(names[i] != null && names[i].length() > 500) {
                names[i] = names[i].substring(0, 500) + "...";
            }
        }

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

            String name = "";

            if(full) {
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

    private int calculateItFCrystal(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].matches("^-i(tf)?\\d$")) {
                int crystal = StaticStore.safeParseInt(contents[i].replaceAll("-i(tf)?", ""));

                if(crystal >= 3 || crystal < 0)
                    continue;

                return 7 - 2 * crystal;
            }
        }

        return 1;
    }

    private int calculateCotCCrystal(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].matches("^-c(otc)?\\d$")) {
                int crystal = StaticStore.safeParseInt(contents[i].replaceAll("-c(otc)?", ""));

                if(crystal >= 3 || crystal < 0)
                    continue;

                return 16 - 5 * crystal;
            }
        }

        return 1;
    }
}
