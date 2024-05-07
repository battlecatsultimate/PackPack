package mandarin.packpack.commands;

import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TimedConstraintCommand extends Command {

    final String constRole;
    protected final long time;
    @Nullable
    protected final IDHolder holder;
    protected final String id;

    private boolean startTimer = true;

    public TimedConstraintCommand(ConstraintCommand.ROLE role, int lang, @Nullable IDHolder idHolder, long time, String id, boolean requireGuild) {
        super(lang, requireGuild);

        switch (role) {
            case MOD -> {
                if (idHolder != null) {
                    constRole = idHolder.MOD;
                } else {
                    constRole = null;
                }
            }
            case MEMBER -> {
                if (idHolder != null) {
                    constRole = idHolder.MEMBER;
                } else {
                    constRole = null;
                }
            }
            case MANDARIN -> constRole = "MANDARIN";
            case TRUSTED -> constRole = "TRUSTED";
            default -> throw new IllegalStateException("Invalid ROLE enum : " + role);
        }

        this.time = time;
        this.holder = idHolder;
        this.id = id;
    }

    @Override
    public void execute(GenericMessageEvent event) {
        new CommandLoader().load(event, loader -> {
            try {
                prepare();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/TimedConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

                return;
            }

            MessageChannel ch = loader.getChannel();

            Message msg = loader.getMessage();

            if(requireGuild && !(ch instanceof GuildChannel)) {
                replyToMessageSafely(ch, LangID.getStringByID("require_server", lang), msg, a -> a);

                return;
            }

            User u = msg.getAuthor();

            String memberID = u.getId();
            boolean hasRole;
            boolean canBypass = memberID.equals(StaticStore.MANDARIN_SMELL);

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

                if (hasRole && !u.getId().equals(StaticStore.MANDARIN_SMELL)) {
                    StaticStore.logger.uploadLog("User " + loader.getUser().getAsMention() + " called command : \n\n" + loader.getContent());
                }
            } else {
                Member me = loader.getMember();

                String role = StaticStore.rolesToString(me.getRoles());

                boolean isMod = holder != null && holder.MOD != null && role.contains(holder.MOD);

                hasRole = isMod || role.contains(constRole) || u.getId().equals(StaticStore.MANDARIN_SMELL);
            }

            if(ch instanceof GuildMessageChannel tc) {
                Guild g = loader.getGuild();

                if(!tc.canTalk()) {
                    String serverName = g.getName();
                    String channelName = ch.getName();

                    String content;

                    content = LangID.getStringByID("no_permch", lang).replace("_SSS_", serverName).replace("_CCC_", channelName);

                    u.openPrivateChannel()
                            .flatMap(pc -> pc.sendMessage(content))
                            .queue();

                    return;
                }

                List<Permission> missingPermission = getMissingPermissions((GuildChannel) ch, g.getSelfMember());

                if(!missingPermission.isEmpty()) {
                    u.openPrivateChannel()
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
                    if (ch instanceof GuildChannel) {
                        Guild g = loader.getGuild();

                        ch.sendMessage(LangID.getStringByID("const_role", lang).replace("_", StaticStore.roleNameFromID(g, constRole))).queue();
                    }
                }
            } else {
                try {
                    RecordableThread t = new RecordableThread(() -> {
                        doSomething(loader);

                        if(startTimer && !memberID.isBlank()) {
                            if(!StaticStore.timeLimit.containsKey(memberID)) {
                                Map<String, Long> memberLimit = new HashMap<>();

                                memberLimit.put(id, System.currentTimeMillis());

                                StaticStore.timeLimit.put(memberID, memberLimit);
                            } else {
                                StaticStore.timeLimit.get(memberID).put(id, System.currentTimeMillis());
                            }
                        }
                    }, e -> {
                        String data = "Command : " + loader.getContent() + "\n\n" +
                                "Member  : " + u.getId() + " (" + u.getId() + ")\n\n" +
                                "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                        if (ch instanceof GuildChannel) {
                            Guild g = loader.getGuild();

                            data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                        }

                        StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass()+"\n\n" + data);

                        if(e instanceof ErrorResponseException) {
                            onFail(loader, SERVER_ERROR);
                        } else {
                            onFail(loader, DEFAULT_ERROR);
                        }
                    }, loader);

                    t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + loader.getContent());
                    t.start();
                } catch (Exception e) {
                    String data = "Command : " + loader.getContent() + "\n\n" +
                            "Member  : " + u.getId() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                    if (ch instanceof GuildChannel) {
                        Guild g = loader.getGuild();

                        data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                    }

                    StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass()+"\n\n" + data);

                    if(e instanceof ErrorResponseException) {
                        onFail(loader, SERVER_ERROR);
                    } else {
                        onFail(loader, DEFAULT_ERROR);
                    }
                }

                try {
                    onSuccess(loader);
                } catch (Exception e) {
                    String data = "Command : " + loader.getContent() + "\n\n" +
                            "Member  : " + u.getId() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                    if (ch instanceof GuildChannel) {
                        Guild g = loader.getGuild();

                        data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                    }

                    StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass()+"\n\n" + data);
                }
            }
        });
    }

    private String getCooldown(long time) {
        return DataToString.df.format(time / 1000.0);
    }

    public void disableTimer() {
        startTimer = false;
    }
}
