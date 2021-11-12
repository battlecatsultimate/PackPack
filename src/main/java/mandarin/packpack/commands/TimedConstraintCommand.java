package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TimedConstraintCommand extends Command {

    final String constRole;
    protected final long time;
    protected final IDHolder holder;
    protected final String id;

    private boolean startTimer = true;

    public TimedConstraintCommand(ConstraintCommand.ROLE role, int lang, IDHolder idHolder, long time, String id) {
        super(lang);

        switch (role) {
            case MOD:
                constRole = idHolder.MOD;
                break;
            case MEMBER:
                constRole = idHolder.MEMBER;
                break;
            case PRE_MEMBER:
                constRole = idHolder.PRE_MEMBER;
                break;
            case MANDARIN:
                constRole = "MANDARIN";
                break;
            default:
                throw new IllegalStateException("Invalid ROLE enum : "+role);
        }

        this.time = time;
        this.holder = idHolder;
        this.id = id;
    }

    @Override
    public void execute(MessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        AtomicReference<Boolean> canGo = new AtomicReference<>(true);
        AtomicReference<String> memberID = new AtomicReference<>("");
        AtomicReference<Boolean> hasRole = new AtomicReference<>(false);

        getMember(event).ifPresent(m -> {
            String mID = m.getId().asString();
            String role = StaticStore.rolesToString(m.getRoleIds());

            boolean isMod = holder.MOD != null && role.contains(holder.MOD);

            memberID.set(mID);

            if(mID.equals(StaticStore.MANDARIN_SMELL) || isMod) {
                hasRole.set(true);

                return;
            }

            if (StaticStore.timeLimit.containsKey(mID) && StaticStore.timeLimit.get(mID).containsKey(id)) {
                long oldTime = StaticStore.timeLimit.get(mID).get(id);
                long currentTime = System.currentTimeMillis();

                if(currentTime-oldTime < time) {
                    ch.createMessage(LangID.getStringByID("command_timelimit", lang).replace("_", getCooldown(time - (currentTime-oldTime)))).subscribe();
                    canGo.set(false);
                }
            }

            if(constRole == null) {
                hasRole.set(true);
            } else if(constRole.equals("MANDARIN")) {
                hasRole.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            } else {
                hasRole.set(role.contains(constRole) || m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            }
        });

        if(!canGo.get())
            return;

        if(!hasRole.get()) {
            if(constRole != null && constRole.equals("MANDARIN")) {
                ch.createMessage(LangID.getStringByID("const_man", lang)).subscribe();
            } else {
                Guild g = getGuild(event).block();

                String role = g == null ? "NONE" : StaticStore.roleNameFromID(g, constRole);
                ch.createMessage(LangID.getStringByID("const_role", lang).replace("_", role)).subscribe();
            }
        } else {
            try {
                new Thread(() -> {
                    try {
                        doSomething(event);

                        if(startTimer && !memberID.get().isBlank()) {
                            if(!StaticStore.timeLimit.containsKey(memberID.get())) {
                                Map<String, Long> memberLimit = new HashMap<>();

                                memberLimit.put(id, System.currentTimeMillis());

                                StaticStore.timeLimit.put(memberID.get(), memberLimit);
                            } else {
                                StaticStore.timeLimit.get(memberID.get()).put(id, System.currentTimeMillis());
                            }
                        }
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass());
                        e.printStackTrace();
                        onFail(event, DEFAULT_ERROR);
                    }
                }).start();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command : "+this.getClass());
                e.printStackTrace();
                onFail(event, DEFAULT_ERROR);
            }

            try {
                onSuccess(event);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "Failed to perform timed constraint command on success : "+this.getClass());
                e.printStackTrace();
            }
        }
    }

    private String getCooldown(long time) {
        return DataToString.df.format(time / 1000.0);
    }

    public void disableTimer() {
        startTimer = false;
    }
}
