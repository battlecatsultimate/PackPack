package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.TimeBoolean;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public abstract class GlobalTimedConstraintCommand extends Command {
    static String ABORT = "ABORT";

    final String constRole;
    final String mainID;
    protected String optionalID = "";
    protected final ArrayList<String> aborts = new ArrayList<>();
    long time;

    @Nullable
    protected final IDHolder holder;

    private boolean timerStart = true;

    public GlobalTimedConstraintCommand(ConstraintCommand.ROLE role, int lang, @Nullable IDHolder id, String mainID, long millis, boolean requireGuild) {
        super(lang, requireGuild);

        switch (role) {
            case MOD:
                if(id != null) {
                    constRole = id.MOD;
                } else {
                    constRole = null;
                }

                break;
            case MEMBER:
                if(id != null) {
                    constRole = id.MEMBER;
                } else {
                    constRole = null;
                }

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

        this.mainID = mainID;
        this.time = millis;
        this.holder = id;

        aborts.add(ABORT);
    }

    @Override
    public void execute(GenericMessageEvent event) {
        try {
            prepare();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/GlobalTimedConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

            return;
        }

        prepareAborts();

        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Message msg = getMessage(event);

        if(msg == null)
            return;

        if(requireGuild && !(ch instanceof GuildChannel)) {
            replyToMessageSafely(ch, LangID.getStringByID("require_server", lang), msg, a -> a);

            return;
        }

        User u = msg.getAuthor();

        SpamPrevent spam;

        if(StaticStore.spamData.containsKey(u.getId())) {
            spam = StaticStore.spamData.get(u.getId());

            if(spam.isPrevented(ch, lang, u.getId()))
                return;
        } else {
            spam = new SpamPrevent();

            StaticStore.spamData.put(u.getId(), spam);
        }

        boolean hasRole;
        boolean isMandarin = u.getId().equals(StaticStore.MANDARIN_SMELL);

        if(constRole == null) {
            hasRole = true;
        } else if(constRole.equals("MANDARIN")) {
            hasRole = u.getId().equals(StaticStore.MANDARIN_SMELL);
        } else if(constRole.equals("TRUSTED")) {
            hasRole = StaticStore.contributors.contains(u.getId());
        } else {
            Member me = getMember(event);

            if(me == null)
                return;

            String role = StaticStore.rolesToString(me.getRoles());

            boolean isMod = holder != null && holder.MOD != null && role.contains(holder.MOD);

            hasRole = isMod || role.contains(constRole) || u.getId().equals(StaticStore.MANDARIN_SMELL);
        }

        if(ch instanceof GuildMessageChannel) {
            Guild g = getGuild(event);

            if(g == null)
                return;

            GuildMessageChannel tc = ((GuildMessageChannel) ch);

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

        if(!hasRole && !isMandarin) {
            if(constRole.equals("MANDARIN")) {
                ch.sendMessage(LangID.getStringByID("const_man", lang)).queue();
            } else if(constRole.equals("TRUSTED")) {
                createMessageWithNoPings(ch, LangID.getStringByID("const_con", lang));
            } else {
                Guild g = getGuild(event);

                if(g == null)
                    return;

                ch.sendMessage(LangID.getStringByID("const_role", lang).replace("_", StaticStore.roleNameFromID(g, constRole))).queue();
            }
        } else {
            setOptionalID(event);

            String id = mainID+optionalID;

            try {
                TimeBoolean bool = StaticStore.canDo.get(id);

                if(!isMandarin && bool != null && !bool.canDo && System.currentTimeMillis() - bool.time < bool.totalTime) {
                    ch.sendMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((bool.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get(id).time)) / 1000.0))).queue();
                } else {
                    if(!aborts.contains(optionalID)) {
                        pause.reset();

                        System.out.println("Added process : "+id);

                        StaticStore.canDo.put(id, new TimeBoolean(false, time));

                        StaticStore.executed++;

                        new Thread(() -> {
                            try {
                                doSomething(event);

                                if(timerStart && time != 0) {
                                    Timer timer = new Timer();

                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            System.out.println("Remove Process : "+id+" | "+time);
                                            StaticStore.canDo.put(id, new TimeBoolean(true));
                                        }
                                    }, time);
                                } else {
                                    StaticStore.canDo.put(id, new TimeBoolean(true));
                                }
                            } catch (Exception e) {
                                String data = "Command : " + getContent(event) + "\n\n" +
                                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                                        "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                                Guild g = getGuild(event);

                                if(g != null) {
                                    data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                                }

                                StaticStore.logger.uploadErrorLog(e, "Failed to perform global timed constraint command : "+this.getClass()+"\n\n" + data);

                                if(e instanceof ErrorResponseException) {
                                    onFail(event, SERVER_ERROR);
                                } else {
                                    onFail(event, DEFAULT_ERROR);
                                }

                                StaticStore.canDo.put(id, new TimeBoolean(true));
                            }
                        }).start();
                    } else {
                        onAbort(event);
                    }
                }
            } catch (Exception e) {
                String data = "Command : " + getContent(event) + "\n\n" +
                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                Guild g = getGuild(event);

                if(g != null) {
                    data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                }

                StaticStore.logger.uploadErrorLog(e, "Failed to perform global timed constraint command : "+this.getClass()+"\n\n" + data);
                StaticStore.canDo.put(id, new TimeBoolean(true));

                if(e instanceof ErrorResponseException) {
                    onFail(event, SERVER_ERROR);
                } else {
                    onFail(event, DEFAULT_ERROR);
                }
            }

            try {
                onSuccess(event);
            } catch (Exception e) {
                String data = "Command : " + getContent(event) + "\n\n" +
                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                Guild g = getGuild(event);

                if(g != null) {
                    data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                }

                StaticStore.logger.uploadErrorLog(e, "Failed to perform global timed constraint command : "+this.getClass()+"\n\n" + data);
            }
        }
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        doThing(event);

        pause.resume();
    }

    protected void disableTimer() {
        timerStart = false;
    }

    protected void changeTime(long millis) {
        time = millis;

        TimeBoolean bool = StaticStore.canDo.get(mainID+optionalID);

        if(bool != null) {
            bool.totalTime = time;
        }
    }

    protected abstract void doThing(GenericMessageEvent event) throws Exception;

    protected abstract void setOptionalID(GenericMessageEvent event);

    protected abstract void prepareAborts();

    protected void onAbort(GenericMessageEvent event) {}
}
