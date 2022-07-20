package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.List;

public abstract class ConstraintCommand extends Command {
    public enum ROLE {
        MANDARIN,
        MOD,
        MEMBER,
        TRUSTED
    }

    final String constRole;
    protected final IDHolder holder;

    public ConstraintCommand(ROLE role, int lang, IDHolder id) {
        super(lang);

        switch (role) {
            case MOD:
                constRole = id.MOD;
                break;
            case MEMBER:
                constRole = id.MEMBER;
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

        this.holder = id;
    }

    @Override
    public void execute(GenericMessageEvent event) {
        try {
            prepare();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/ConstraintCommand::execute - Failed to prepare command : "+this.getClass().getName());

            return;
        }

        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        boolean hasRole;
        boolean isMod = false;

        Member m = getMember(event);

        if(m == null)
            return;

        SpamPrevent spam;

        if(StaticStore.spamData.containsKey(m.getId())) {
            spam = StaticStore.spamData.get(m.getId());

            if(spam.isPrevented(ch, lang, m.getId()))
                return;
        } else {
            spam = new SpamPrevent();

            StaticStore.spamData.put(m.getId(), spam);
        }

        String role = StaticStore.rolesToString(m.getRoles());

        if(constRole == null) {
            hasRole = true;
        } else if(constRole.equals("MANDARIN")) {
            hasRole = m.getId().equals(StaticStore.MANDARIN_SMELL);
        } else if(constRole.equals("TRUSTED")) {
            hasRole = StaticStore.contributors.contains(m.getId());
        } else {
            hasRole = role.contains(constRole) || m.getId().equals(StaticStore.MANDARIN_SMELL);

            if(!hasRole) {
                isMod = holder.MOD != null && role.contains(holder.MOD);
            }
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

        if(!hasRole && !isMod) {
            if(constRole.equals("MANDARIN")) {
                ch.sendMessage(LangID.getStringByID("const_man", lang)).queue();
            } else if(constRole.equals("TRUSTED")) {
                createMessageWithNoPings(ch, LangID.getStringByID("const_con", lang));
            } else {
                ch.sendMessage(LangID.getStringByID("const_role", lang).replace("_", StaticStore.roleNameFromID(g, constRole))).queue();
            }
        } else {
            StaticStore.executed++;

            try {
                new Thread(() -> {
                    try {
                        doSomething(event);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
                        onFail(event, DEFAULT_ERROR);
                    }
                }).start();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
                onFail(event, DEFAULT_ERROR);
            }

            try {
                onSuccess(event);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform onSuccess process");
            }
        }
    }
}
