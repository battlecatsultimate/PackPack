package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
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

    private final ConstraintCommand.ROLE role;

    private boolean startTimer = true;

    public TimedConstraintCommand(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder idHolder, long time, String id, boolean requireGuild) {
        super(lang, requireGuild);

        this.role = role;

        switch (role) {
            case MOD -> {
                if (idHolder != null) {
                    constRole = idHolder.moderator;
                } else {
                    constRole = null;
                }
            }
            case MEMBER -> {
                if (idHolder != null) {
                    constRole = idHolder.member;
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
        new CommandLoader().load(event, this::onLoaded);
    }

    @Override
    public void execute(GenericInteractionCreateEvent event) {
        new CommandLoader().load(event, this::onLoaded);
    }

    private String getCooldown(long time) {
        return DataToString.df.format(time / 1000.0);
    }

    public void disableTimer() {
        startTimer = false;
    }

    private void onLoaded(CommandLoader loader) {
        try {
            prepare();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/TimedConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

            return;
        }

        MessageChannel ch = loader.getChannel();

        if(requireGuild && !(ch instanceof GuildChannel)) {
            if (loader.fromMessage) {
                replyToMessageSafely(ch, LangID.getStringByID("bot.sendFailure.reason.serverRequired", lang), loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("bot.sendFailure.reason.serverRequired", lang), a -> a);
            }

            return;
        }

        User u = loader.getUser();

        String memberID = u.getId();

        if(ch instanceof GuildMessageChannel tc) {
            Guild g = loader.getGuild();

            if(!tc.canTalk()) {
                String serverName = g.getName();
                String channelName = ch.getName();

                String content;

                content = LangID.getStringByID("bot.sendFailure.reason.noPermission.withChannel", lang).replace("_SSS_", serverName).replace("_CCC_", channelName);

                u.openPrivateChannel()
                        .flatMap(pc -> pc.sendMessage(content))
                        .queue();

                return;
            }

            List<Permission> missingPermission = getMissingPermissions((GuildChannel) ch, g.getSelfMember());

            if(!missingPermission.isEmpty()) {
                u.openPrivateChannel()
                        .flatMap(pc -> pc.sendMessage(LangID.getStringByID("bot.sendFailure.reason.missingPermission", lang).replace("_PPP_", parsePermissionAsList(missingPermission)).replace("_SSS_", g.getName()).replace("_CCC_", ch.getName())))
                        .queue();

                return;
            }
        }

        boolean canBypass = memberID.equals(StaticStore.MANDARIN_SMELL);

        if (!canBypass && StaticStore.timeLimit.containsKey(memberID) && StaticStore.timeLimit.get(memberID).containsKey(id)) {
            long oldTime = StaticStore.timeLimit.get(memberID).get(id);
            long currentTime = System.currentTimeMillis();

            if(currentTime-oldTime < time) {
                ch.sendMessage(LangID.getStringByID("bot.command.timeLimit", lang).replace("_", getCooldown(time - (currentTime-oldTime)))).queue();

                return;
            }
        }

        String denialMessage = null;
        boolean isMandarin = u.getId().equals(StaticStore.MANDARIN_SMELL);
        boolean hasRole;

        switch (role) {
            case MOD -> {
                Member m = loader.getMember();

                if (constRole != null) {
                    hasRole = m.getRoles().stream().anyMatch(r -> r.getId().equals(constRole)) || m.isOwner();

                    if (!hasRole) {
                        denialMessage = LangID.getStringByID("bot.denied.reason.noPermission.mod.withRole", lang).formatted(constRole);
                    }
                } else {
                    //Find if user has server manage permission
                    hasRole = m.getRoles().stream().anyMatch(r -> r.hasPermission(Permission.MANAGE_SERVER) || r.hasPermission(Permission.ADMINISTRATOR));

                    if (!hasRole) {
                        //Maybe role isn't existing, check if owner
                        hasRole = m.isOwner();
                    }

                    if (!hasRole) {
                        denialMessage = LangID.getStringByID("bot.denied.reason.noPermission.mod.noRole", lang);
                    }
                }
            }
            case MEMBER -> {
                if (constRole != null) {
                    Member m = loader.getMember();
                    List<Role> roles = m.getRoles();

                    boolean isModerator = false;

                    if (holder != null) {
                        String moderatorID = holder.moderator;

                        if (moderatorID != null) {
                            isModerator = roles.stream().anyMatch(r -> r.getId().equals(moderatorID)) || m.isOwner();
                        } else {
                            isModerator = m.getRoles().stream().anyMatch(r -> r.hasPermission(Permission.MANAGE_SERVER) || r.hasPermission(Permission.ADMINISTRATOR));

                            if (!isModerator) {
                                //Maybe role isn't existing, check if owner
                                isModerator = m.isOwner();
                            }
                        }
                    }

                    hasRole = isModerator || roles.stream().anyMatch(r -> r.getId().equals(constRole));

                    if (!hasRole) {
                        denialMessage = LangID.getStringByID("bot.denied.reason.noPermission.member", lang).formatted(constRole);
                    }
                } else {
                    hasRole = true;
                }
            }
            case TRUSTED -> {
                hasRole = StaticStore.contributors.contains(u.getId());

                if (!hasRole) {
                    denialMessage = LangID.getStringByID("bot.denied.reason.noPermission.trusted", lang).formatted(loader.getClient().getSelfUser().getId(), StaticStore.MANDARIN_SMELL);
                }
            }
            case MANDARIN -> {
                hasRole = isMandarin;

                if (!hasRole) {
                    denialMessage = LangID.getStringByID("bot.denied.reason.noPermission.developer", lang).formatted(StaticStore.MANDARIN_SMELL);
                }
            }
            default -> throw new IllegalStateException("E/TimedConstraintCommand::execute - Unknown value : %s".formatted(role));
        }

        if(!hasRole && !isMandarin) {
            if (denialMessage != null) {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, denialMessage, loader.getMessage(), a -> a);
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), denialMessage, a -> a);
                }
            }

            return;
        }

        try {
            RecordableThread t = new RecordableThread(() -> {
                if (StaticStore.logCommand) {
                    Logger.addLog(this.getClass() + " called : " + (loader.fromMessage ? loader.getContent() : loader.getInteractionEvent().getFullCommandName()));
                }

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
                String data;

                if (loader.fromMessage) {
                    data = "Command : " + loader.getContent() + "\n\n" +
                            "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
                } else {
                    data = "Command : " + loader.getInteractionEvent().getFullCommandName() + "\n\n" +
                            "Member : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
                }

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

            t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + (loader.fromMessage ? loader.getContent() : loader.getInteractionEvent().getFullCommandName()));
            t.start();
        } catch (Exception e) {
            String data;

            if (loader.fromMessage) {
                data = "Command : " + loader.getContent() + "\n\n" +
                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
            } else {
                data = "Command : " + loader.getInteractionEvent().getFullCommandName() + "\n\n" +
                        "Member : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
            }

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
            String data;

            if (loader.fromMessage) {
                data = "Command : " + loader.getContent() + "\n\n" +
                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
            } else {
                data = "Command : " + loader.getInteractionEvent().getFullCommandName() + "\n\n" +
                        "Member : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
            }

            if (ch instanceof GuildChannel) {
                Guild g = loader.getGuild();

                data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
            }

            StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass()+"\n\n" + data);
        }
    }
}
