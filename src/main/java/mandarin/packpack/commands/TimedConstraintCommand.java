package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.concurrent.atomic.AtomicReference;

public abstract class TimedConstraintCommand implements Command {
    public enum ROLE {
        MANDARIN,
        DEV,
        MOD,
        MEMBER,
        PRE_MEMBER
    }

    final String constRole;
    protected final int lang;
    protected final long time;

    public TimedConstraintCommand(ROLE role, int lang, IDHolder id, long time) {
        switch (role) {
            case DEV:
                constRole = id.DEV;
                break;
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
        this.time = time;
    }

    @Override
    public void execute(MessageCreateEvent event) {
        Message msg = event.getMessage();
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        AtomicReference<Boolean> canGo = new AtomicReference<>(true);

        event.getMember().ifPresent(m -> {
            String id = m.getId().asString();

            if(id.equals(StaticStore.MANDARIN_SMELL))
                return;

            if (StaticStore.timeLimit.containsKey(id)) {
                long oldTime = StaticStore.timeLimit.get(id);
                long currentTime = System.currentTimeMillis();

                if(currentTime-oldTime < time) {
                    ch.createMessage(LangID.getStringByID("command_timelimit", lang).replace("_", getCooldown(time - (currentTime-oldTime)))).subscribe();
                    canGo.set(false);
                } else {
                    StaticStore.timeLimit.put(id, currentTime);
                }
            } else {
                long currentTime = System.currentTimeMillis();
                StaticStore.timeLimit.put(id, currentTime);
            }
        });

        if(!canGo.get())
            return;

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
                ch.createMessage(LangID.getStringByID("const_man", lang)).subscribe();
            } else {
                String role = StaticStore.roleNameFromID(event, constRole);
                ch.createMessage(LangID.getStringByID("const_role", lang).replace("_", role)).subscribe();
            }
        } else {
            try {
                doSomething(event);
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

    protected void putTime(MessageCreateEvent event) {

    }

    private String getCooldown(long time) {
        return DataToString.df.format(time / 1000.0);
    }
}
