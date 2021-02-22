package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;
import mandarin.packpack.supporter.server.TimeBoolean;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SingleContraintCommand implements Command {
    static String ABORT = "ABORT";

    final String constRole;
    protected final int lang;
    final String mainID;
    protected String optionalID = "";
    protected final ArrayList<String> aborts = new ArrayList<>();
    long time;

    protected final IDHolder holder;

    private boolean timerStart = true;

    public SingleContraintCommand(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID, long millis) {
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
        this.mainID = mainID;
        this.time = millis;
        this.holder = id;

        aborts.add(ABORT);
    }

    @Override
    public void execute(MessageCreateEvent event) {
        prepareAborts();

        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        AtomicReference<Boolean> hasRole = new AtomicReference<>(false);
        AtomicReference<Boolean> isMandarin = new AtomicReference<>(false);
        AtomicReference<Boolean> isMod = new AtomicReference<>(false);

        AtomicReference<Boolean> canGo = new AtomicReference<>(true);

        event.getMember().ifPresentOrElse(m -> {
            String role = StaticStore.rolesToString(m.getRoleIds());

            if(constRole == null) {
                System.out.println("Set to null");
                hasRole.set(true);
            } else if(constRole.equals("MANDARIN")) {
                System.out.println("Is Mandarin");
                hasRole.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            } else {
                System.out.println("Normal");
                hasRole.set(role.contains(constRole) || m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
            }

            if(!hasRole.get()) {
                isMod.set(holder.MOD != null && role.contains(holder.MOD));
            }

            isMandarin.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL));
        }, () -> canGo.set(false));

        if(!canGo.get())
            return;

        System.out.println(hasRole.get()+" | "+isMod.get()+" | "+constRole);

        if(!hasRole.get() && !isMod.get()) {
            if(constRole.equals("MANDARIN")) {
                ch.createMessage(LangID.getStringByID("const_man", lang)).subscribe();
            } else {
                String role = StaticStore.roleNameFromID(event, constRole);
                ch.createMessage(LangID.getStringByID("const_role", lang).replace("_", role)).subscribe();
            }
        } else {
            try {
                setOptionalID(event);

                String id = mainID+optionalID;

                if(!isMandarin.get() && StaticStore.canDo.containsKey(id) && !StaticStore.canDo.get(id).canDo) {
                    ch.createMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((30000 - (System.currentTimeMillis() - StaticStore.canDo.get(id).time)) / 1000.0))).subscribe();
                } else {

                    if(!aborts.contains(optionalID)) {
                        pause.reset();

                        System.out.println("Added process : "+id);

                        StaticStore.canDo.put(id, new TimeBoolean(false));

                        new Thread(() -> {
                            try {
                                doSomething(event);

                                System.out.println(timerStart);

                                if(timerStart) {
                                    Timer timer = new Timer();

                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            System.out.println("Remove Process : "+id);
                                            StaticStore.canDo.put(id, new TimeBoolean(true));
                                        }
                                    }, time);
                                } else {
                                    StaticStore.canDo.put(id, new TimeBoolean(true));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                onFail(event, DEFAULT_ERROR);
                            }
                        }).start();
                    } else {
                        onAbort(event);
                    }
                }
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

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {
        doThing(event);

        pause.resume();
    }

    protected void disableTimer() {
        timerStart = false;
    }

    protected void changeTime(long millis) {
        time = millis;
    }

    protected abstract void doThing(MessageCreateEvent event) throws Exception;

    protected abstract void setOptionalID(MessageCreateEvent event);

    protected abstract void prepareAborts();

    protected void onAbort(MessageCreateEvent event) {}
}
