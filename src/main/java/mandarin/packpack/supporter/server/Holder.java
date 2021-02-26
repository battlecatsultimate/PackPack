package mandarin.packpack.supporter.server;

import discord4j.core.event.domain.message.MessageCreateEvent;

public abstract class Holder {
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    public final long time = System.currentTimeMillis();

    public abstract int handleEvent(MessageCreateEvent event);
    public abstract void clean();
    public abstract void expire(String id);

    public boolean equals(Holder that) {
        return this.time == that.time;
    }
}
