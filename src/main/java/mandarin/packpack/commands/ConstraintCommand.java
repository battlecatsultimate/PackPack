package mandarin.packpack.commands;

import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.SpamPrevent;
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

    public ConstraintCommand(ROLE role, int lang, @Nullable IDHolder id, boolean requireGuild) {
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

        this.holder = id;
    }

    @Override
    public void execute(GenericMessageEvent event) {
        new CommandLoader().load(event, loader -> {
            try {
                prepare();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/ConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

                return;
            }

            MessageChannel ch = loader.getChannel();
            Message msg = loader.getMessage();

            if(requireGuild && !(ch instanceof GuildChannel)) {
                replyToMessageSafely(ch, LangID.getStringByID("require_server", lang), msg, a -> a);

                return;
            }

            boolean hasRole;
            boolean isMod = false;

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

            if(constRole == null) {
                hasRole = true;
            } else if(constRole.equals("MANDARIN")) {
                hasRole = u.getId().equals(StaticStore.MANDARIN_SMELL);
            } else if(constRole.equals("TRUSTED")) {
                hasRole = StaticStore.contributors.contains(u.getId());

                if (hasRole && !u.getId().equals(StaticStore.MANDARIN_SMELL)) {
                    StaticStore.logger.uploadLog("User " + loader.getUser().getAsMention() + " called command : \n\n" + loader.getContent());
                }
            } else {
                Member me = loader.getMember();

                String role = StaticStore.rolesToString(me.getRoles());

                hasRole = role.contains(constRole) || u.getId().equals(StaticStore.MANDARIN_SMELL);

                if(!hasRole) {
                    isMod = holder != null && holder.MOD != null && role.contains(holder.MOD);
                }
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

            if(!hasRole && !isMod) {
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
                StaticStore.executed++;

                try {
                    RecordableThread t = new RecordableThread(() -> doSomething(loader), e -> {
                        String data = "Command : " + loader.getContent() + "\n\n" +
                                "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                                "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

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

                    t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + loader.getContent());
                    t.start();
                } catch (Exception e) {
                    String data = "Command : " + loader.getContent() + "\n\n" +
                            "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

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
        });
    }
}
