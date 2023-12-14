package mandarin.packpack.commands;

import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
            case MOD -> {
                if (id != null) {
                    constRole = id.MOD;
                } else {
                    constRole = null;
                }
            }
            case MEMBER -> {
                if (id != null) {
                    constRole = id.MEMBER;
                } else {
                    constRole = null;
                }
            }
            case MANDARIN -> constRole = "MANDARIN";
            case TRUSTED -> constRole = "TRUSTED";
            default -> throw new IllegalStateException("Invalid ROLE enum : " + role);
        }

        this.mainID = mainID;
        this.time = millis;
        this.holder = id;

        aborts.add(ABORT);
    }

    @Override
    public void execute(GenericMessageEvent event) {
        new CommandLoader().load(event, loader -> {
            try {
                prepare();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/GlobalTimedConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

                return;
            }

            prepareAborts();

            MessageChannel ch = loader.getChannel();

            Message msg = loader.getMessage();

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

            if(!hasRole && !isMandarin) {
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
                setOptionalID(loader);

                String id = mainID+optionalID;

                try {
                    TimeBoolean bool = StaticStore.canDo.get(id);

                    if(!isMandarin && bool != null && !bool.canDo && System.currentTimeMillis() - bool.time < bool.totalTime) {
                        ch.sendMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((bool.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get(id).time)) / 1000.0))).queue();
                    } else {
                        if(!aborts.contains(optionalID)) {
                            System.out.println("Added process : "+id);

                            StaticStore.canDo.put(id, new TimeBoolean(false, time));

                            StaticStore.executed++;

                            RecordableThread t = new RecordableThread(() -> {
                                doSomething(loader);

                                if(timerStart && time != 0) {
                                    StaticStore.executorHandler.postDelayed(time, () -> {
                                        System.out.println("Remove Process : "+id+" | "+time);

                                        StaticStore.canDo.put(id, new TimeBoolean(true));
                                    });
                                } else {
                                    StaticStore.canDo.put(id, new TimeBoolean(true));
                                }
                            }, e -> {
                                String data = "Command : " + loader.getContent() + "\n\n" +
                                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                                        "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                                if (ch instanceof GuildChannel) {
                                    Guild g = loader.getGuild();

                                    data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                                }

                                StaticStore.logger.uploadErrorLog(e, "Failed to perform global timed constraint command : "+this.getClass()+"\n\n" + data);

                                if(e instanceof ErrorResponseException) {
                                    onFail(loader, SERVER_ERROR);
                                } else {
                                    onFail(loader, DEFAULT_ERROR);
                                }

                                StaticStore.canDo.put(id, new TimeBoolean(true));
                            });

                            t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime());
                            t.start();
                        } else {
                            onAbort(loader);
                        }
                    }
                } catch (Exception e) {
                    String data = "Command : " + loader.getContent() + "\n\n" +
                            "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                    if (ch instanceof GuildChannel) {
                        Guild g = loader.getGuild();

                        data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                    }

                    StaticStore.logger.uploadErrorLog(e, "Failed to perform global timed constraint command : "+this.getClass()+"\n\n" + data);
                    StaticStore.canDo.put(id, new TimeBoolean(true));

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
                            "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

                    if (ch instanceof GuildChannel) {
                        Guild g = loader.getGuild();

                        data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                    }

                    StaticStore.logger.uploadErrorLog(e, "Failed to perform global timed constraint command : "+this.getClass()+"\n\n" + data);
                }
            }
        });
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        doThing(loader);
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

    protected abstract void doThing(CommandLoader loader) throws Exception;

    protected abstract void setOptionalID(CommandLoader loader);

    protected abstract void prepareAborts();

    protected void onAbort(CommandLoader loader) {}
}
