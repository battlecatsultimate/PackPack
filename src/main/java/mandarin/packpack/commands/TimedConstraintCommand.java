package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TimedConstraintCommand extends Command {

    final String constRole;
    protected final long time;
    protected final IDHolder holder;
    protected final String id;

    private boolean startTimer = true;

    public TimedConstraintCommand(ConstraintCommand.ROLE role, int lang, IDHolder idHolder, long time, String id) {
        super(lang);

        switch (role) {
            case MOD:
                constRole = idHolder.MOD;
                break;
            case MEMBER:
                constRole = idHolder.MEMBER;
                break;
            case MANDARIN:
                constRole = "MANDARIN";
                break;
            case TRUSTED:
                constRole = "TRUSTED";
                break;
            default:
                throw new IllegalStateException("Invalid ROLE enum : "+role);
        }

        this.time = time;
        this.holder = idHolder;
        this.id = id;
    }

    @Override
    public void execute(GenericMessageEvent event) {
        try {
            prepare();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/TimedConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

            return;
        }

        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        Member m = getMember(event);

        if(m == null)
            return;

        String memberID = m.getId();
        boolean hasRole;
        boolean canBypass = memberID.equals(StaticStore.MANDARIN_SMELL);

        String role = StaticStore.rolesToString(m.getRoles());

        boolean isMod = holder.MOD != null && role.contains(holder.MOD);

        if (!canBypass && StaticStore.timeLimit.containsKey(memberID) && StaticStore.timeLimit.get(memberID).containsKey(id)) {
            long oldTime = StaticStore.timeLimit.get(memberID).get(id);
            long currentTime = System.currentTimeMillis();

            if(currentTime-oldTime < time) {
                ch.sendMessage(LangID.getStringByID("command_timelimit", lang).replace("_", getCooldown(time - (currentTime-oldTime)))).queue();

                return;
            }
        }

        if(constRole == null) {
            hasRole = true;
        } else if(constRole.equals("MANDARIN")) {
            hasRole = memberID.equals(StaticStore.MANDARIN_SMELL);
        } else if(constRole.equals("TRUSTED")) {
            hasRole = StaticStore.contributors.contains(memberID);
        } else {
            hasRole = isMod || role.contains(constRole) || m.getId().equals(StaticStore.MANDARIN_SMELL);
        }

        if(ch instanceof GuildMessageChannel) {
            GuildMessageChannel tc = ((GuildMessageChannel) ch);

            if(!tc.canTalk()) {
                String serverName = g.getName();
                String channelName = ch.getName();

                String content;

                content = LangID.getStringByID("no_permch", lang).replace("_SSS_", serverName).replace("_CCC_", channelName);

                m.getUser().openPrivateChannel()
                        .flatMap(pc -> pc.sendMessage(content))
                        .queue();

                return;
            }

            List<Permission> missingPermission = getMissingPermissions((GuildChannel) ch, g.getSelfMember());

            if(!missingPermission.isEmpty()) {
                m.getUser().openPrivateChannel()
                        .flatMap(pc -> pc.sendMessage(LangID.getStringByID("missing_permission", lang).replace("_PPP_", parsePermissionAsList(missingPermission)).replace("_SSS_", g.getName()).replace("_CCC_", ch.getName())))
                        .queue();

                return;
            }
        }

        if(!hasRole) {
            if(constRole.equals("MANDARIN")) {
                ch.sendMessage(LangID.getStringByID("const_man", lang)).queue();
            } else if(constRole.equals("TRUSTED")) {
                createMessageWithNoPings(ch, LangID.getStringByID("const_con", lang));
            } else {
                ch.sendMessage(LangID.getStringByID("const_role", lang).replace("_", StaticStore.roleNameFromID(g, constRole))).queue();
            }
        } else {
            try {
                new Thread(() -> {
                    try {
                        doSomething(event);

                        if(startTimer && !memberID.isBlank()) {
                            if(!StaticStore.timeLimit.containsKey(memberID)) {
                                Map<String, Long> memberLimit = new HashMap<>();

                                memberLimit.put(id, System.currentTimeMillis());

                                StaticStore.timeLimit.put(memberID, memberLimit);
                            } else {
                                StaticStore.timeLimit.get(memberID).put(id, System.currentTimeMillis());
                            }
                        }
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass()+"\n\nCommand : "+getContent(event));
                        e.printStackTrace();
                        onFail(event, DEFAULT_ERROR);
                    }
                }).start();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass()+"\n\nCommand : "+getContent(event));
                e.printStackTrace();
                onFail(event, DEFAULT_ERROR);
            }

            try {
                onSuccess(event);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass()+"\n\nCommand : "+getContent(event));
                e.printStackTrace();
            }
        }
    }

    private String getCooldown(long time) {
        return DataToString.df.format(time / 1000.0);
    }

    public void disableTimer() {
        startTimer = false;
    }
}
