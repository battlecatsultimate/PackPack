package mandarin.packpack.supporter.server.holder;

import discord4j.core.event.domain.message.MessageEvent;

public abstract class Holder<T extends MessageEvent> {
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    private final Class<?> cls;

    public Holder(Class<?> cls) {
        this.cls = cls;
    }

    public final long time = System.currentTimeMillis();

    public abstract int handleEvent(T event);
    public abstract void clean();
    public abstract void expire(String id);

    public boolean equals(Holder<T> that) {
        return this.time == that.time;
    }

    public boolean canCastTo(Class<?> cls) {
        return this.cls != null && this.cls == cls;
    }
}
