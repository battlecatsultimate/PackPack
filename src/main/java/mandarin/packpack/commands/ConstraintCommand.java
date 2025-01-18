package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.SpamPrevent;
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

import java.util.List;

public abstract class ConstraintCommand extends Command {
    public enum ROLE {
        MANDARIN,
        MOD,
        MEMBER,
        TRUSTED
    }

    @Nullable
    final String constRole;
    protected final IDHolder holder;

    private final ConstraintCommand.ROLE role;

    public ConstraintCommand(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id, boolean requireGuild) {
        super(lang, requireGuild);

        this.role = role;

        switch (role) {
            case MOD -> {
                if (id != null) {
                    constRole = id.moderator;
                } else {
                    constRole = null;
                }
            }
            case MEMBER -> {
                if (id != null) {
                    constRole = id.member;
                } else {
                    constRole = null;
                }
            }
            case MANDARIN -> constRole = "MANDARIN";
            case TRUSTED -> constRole = "TRUSTED";
            default -> throw new IllegalStateException("Invalid ROLE enum : " + role);
        }

        this.holder = id;
    }

    @Override
    public void execute(GenericMessageEvent event) {
        new CommandLoader().load(event, this::onLoaded);
    }

    @Override
    public void execute(GenericInteractionCreateEvent event) {
        new CommandLoader().load(event, this::onLoaded);
    }

    private void onLoaded(CommandLoader loader) {
        try {
            prepare();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/ConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

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

        SpamPrevent spam;

        if(StaticStore.spamData.containsKey(u.getId()) && loader.fromMessage) {
            spam = StaticStore.spamData.get(u.getId());

            if(spam.isPrevented(ch, lang, u.getId()))
                return;
        } else if(!StaticStore.spamData.containsKey(u.getId())) {
            spam = new SpamPrevent();

            StaticStore.spamData.put(u.getId(), spam);
        }

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
            default -> throw new IllegalStateException("E/ConstraintCommand::execute - Unknown value : %s".formatted(role));
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

        StaticStore.executed++;

        try {
            RecordableThread t = new RecordableThread(() -> {
                if (StaticStore.logCommand) {
                    Logger.addLog(this.getClass() + " called : " + (loader.fromMessage ? loader.getContent() : loader.getInteractionEvent().getFullCommandName()));
                }

                doSomething(loader);
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

                StaticStore.logger.uploadErrorLog(e, "Failed to perform constraint command : "+this.getClass()+"\n\n" + data);

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

            StaticStore.logger.uploadErrorLog(e, "Failed to perform constraint command : "+this.getClass()+"\n\n" + data);

            if(e instanceof ErrorResponseException) {
                onFail(loader, SERVER_ERROR);
            } else {
                onFail(loader, DEFAULT_ERROR);
            }
        }

        try {
            onSuccess(loader);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to perform onSuccess process");
        }
    }
}
