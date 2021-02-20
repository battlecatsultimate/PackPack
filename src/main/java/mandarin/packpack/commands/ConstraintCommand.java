package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ConstraintCommand implements Command {
    public enum ROLE {
        MANDARIN,
        MOD,
        MEMBER,
        PRE_MEMBER
    }

    final String constRole;
    protected final int lang;
    protected final IDHolder holder;

    public ConstraintCommand(ROLE role, int lang, IDHolder id) {
        switch (role) {
            case MOD:
                constRole = id.MOD;
                break;
            case MEMBER:
                constRole = id.MEMBER;
                break;
            case PRE_MEMBER:
                constRole = id.PRE_MEMBER;
                break;
            case MANDARIN:
                constRole = "MANDARIN";
                break;
            default:
                throw new IllegalStateException("Invalid ROLE enum : "+role);
        }

        this.lang = lang;
        this.holder = id;
    }

    @Override
    public void execute(MessageCreateEvent event) {
        Message msg = event.getMessage();
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        AtomicReference<Boolean> hasRole = new AtomicReference<>(false);

        msg.getAuthorAsMember().subscribe(m -> {
            String role = StaticStore.rolesToString(m.getRoleIds());

            if(constRole == null) {
                hasRole.set(true);
            } else if(constRole.equals("MANDARIN")) {
                hasRole.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            } else {
                hasRole.set(role.contains(constRole) || m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            }

        }, e -> onFail(event, DEFAULT_ERROR), pause::resume);

        pause.pause(() -> onFail(event, DEFAULT_ERROR));

        if(!hasRole.get()) {
            if(constRole.equals("MANDARIN")) {
                ch.createMessage(LangID.getStringByID("const_man", lang)).subscribe();
            } else {
                String role = StaticStore.roleNameFromID(event, constRole);
                ch.createMessage(LangID.getStringByID("const_role", lang).replace("_", role)).subscribe();
            }
        } else {
            try {
                new Thread(() -> {
                    try {
                        doSomething(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                        onFail(event, DEFAULT_ERROR);
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
                onFail(event, DEFAULT_ERROR);
            }

            try {
                onSuccess(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
