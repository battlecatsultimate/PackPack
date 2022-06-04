package mandarin.packpack.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ConstraintCommand extends Command {
    public enum ROLE {
        MANDARIN,
        MOD,
        MEMBER,
        CONTRIBUTOR
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
            case CONTRIBUTOR:
                constRole = "CONTRIBUTOR";
                break;
            default:
                throw new IllegalStateException("Invalid ROLE enum : "+role);
        }

        this.holder = id;
    }

    @Override
    public void execute(MessageEvent event) {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event).block();

        if(ch == null || g == null)
            return;

        AtomicReference<Boolean> prevented = new AtomicReference<>(false);

        AtomicReference<Boolean> hasRole = new AtomicReference<>(false);
        AtomicReference<Boolean> isMod = new AtomicReference<>(false);

        AtomicReference<Boolean> canGo = new AtomicReference<>(true);

        getMember(event).ifPresentOrElse(m -> {
            SpamPrevent spam;

            if(StaticStore.spamData.containsKey(m.getId().asString())) {
                spam = StaticStore.spamData.get(m.getId().asString());

                prevented.set(spam.isPrevented(ch, lang, m.getId().asString()));
            } else {
                spam = new SpamPrevent();

                StaticStore.spamData.put(m.getId().asString(), spam);
            }

            if(prevented.get())
                return;

            String role = StaticStore.rolesToString(m.getRoleIds());

            if(constRole == null) {
                hasRole.set(true);
            } else if(constRole.equals("MANDARIN")) {
                hasRole.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            } else if(constRole.equals("CONTRIBUTOR")) {
                hasRole.set(StaticStore.contributors.contains(m.getId().asString()));
            } else {
                hasRole.set(role.contains(constRole) || m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            }

            if(!hasRole.get()) {
                isMod.set((constRole != null && !constRole.equals("MANDARIN") && !constRole.equals("CONTRIBUTOR")) && holder.MOD != null && role.contains(holder.MOD));
            }

        }, () -> canGo.set(false));

        if (prevented.get())
            return;

        if(!canGo.get())
            return;

        AtomicBoolean canTry = new AtomicBoolean(true);

        if(ch instanceof GuildChannel) {
            GuildChannel tc = ((GuildChannel) ch);

            Optional<PermissionSet> op = tc.getEffectivePermissions(Snowflake.of(StaticStore.PACKPACK)).blockOptional();

            op.ifPresent(permissions -> canTry.set(permissions.contains(Permission.SEND_MESSAGES)));
        }

        if(!canTry.get()) {
            getMember(event).ifPresent(m -> {
                String serverName = g.getName();
                String channelName;

                if(ch instanceof GuildChannel)
                    channelName = ((GuildChannel) ch).getName();
                else
                    channelName = null;

                String content;

                if(channelName == null) {
                    content = LangID.getStringByID("no_perm", lang).replace("_SSS_", serverName);
                } else {
                    content = LangID.getStringByID("no_permch", lang).replace("_SSS_", serverName).replace("_CCC_", channelName);
                }

                m.getPrivateChannel().subscribe(pc -> pc.createMessage(MessageCreateSpec.builder().content(content).build()).subscribe(null, e -> {}));
            });

            StaticStore.executed++;

            return;
        }

        if(!hasRole.get() && !isMod.get()) {
            if(constRole.equals("MANDARIN")) {
                ch.createMessage(LangID.getStringByID("const_man", lang)).subscribe();
            } else if(constRole.equals("CONTRIBUTOR")) {
                createMessageWithNoPings(ch, LangID.getStringByID("const_con", lang));
            } else {
                ch.createMessage(LangID.getStringByID("const_role", lang).replace("_", StaticStore.roleNameFromID(g, constRole))).subscribe();
            }
        } else {
            StaticStore.executed++;

            try {
                new Thread(() -> {
                    try {
                        doSomething(event);
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
                        e.printStackTrace();
                        onFail(event, DEFAULT_ERROR);
                    }
                }).start();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
                e.printStackTrace();
                onFail(event, DEFAULT_ERROR);
            }

            try {
                onSuccess(event);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform onSuccess process");
                e.printStackTrace();
            }
        }
    }
}
