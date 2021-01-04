package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ConstraintCommand implements Command {
    public enum ROLE {
        MANDARIN,
        DEV,
        MOD,
        MEMBER,
        PRE_MEMBER
    }

    final String constRole;

    public ConstraintCommand(ROLE role) {
        switch (role) {
            case DEV:
                constRole = StaticStore.DEV_ID;
                break;
            case MOD:
                constRole = StaticStore.MOD_ID;
                break;
            case MEMBER:
                constRole = StaticStore.MEMBER_ID;
                break;
            case PRE_MEMBER:
                constRole = StaticStore.PRE_MEMBER_ID;
                break;
            case MANDARIN:
                constRole = "MANDARIN";
                break;
            default:
                throw new IllegalStateException("Invalid ROLE enum : "+role);
        }
    }

    @Override
    public void execute(MessageCreateEvent event) {
        Message msg = event.getMessage();
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Pauser pause = new Pauser();

        AtomicReference<Boolean> isDev = new AtomicReference<>(false);

        msg.getAuthorAsMember().subscribe(m -> {
            String role = StaticStore.rolesToString(m.getRoleIds());

            if(constRole.equals("MANDARIN")) {
                isDev.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            } else {
                isDev.set(role.contains(constRole) || m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            }

        }, e -> onFail(event, DEFAULT_ERROR), pause::resume);

        pause.pause(() -> onFail(event, DEFAULT_ERROR));

        if(!isDev.get()) {
            if(constRole.equals("MANDARIN")) {
                ch.createMessage("This command can be only run by MandarinSmell!").subscribe();
            } else {
                String role = StaticStore.roleNameFromID(event, constRole);
                ch.createMessage("This command can be only run by "+role+"!").subscribe();
            }
        } else {
            try {
                doSomething(event);
            } catch (Exception e) {
                e.printStackTrace();
                onFail(event, DEFAULT_ERROR);
            }
        }
    }
}
