package mandarin.packpack.supporter.server.holder;

import net.dv8tion.jda.api.events.Event;

import java.util.concurrent.TimeUnit;

public interface Holder<T extends Event> {
    int RESULT_FAIL = -1;
    int RESULT_STILL = 0;
    int RESULT_FINISH = 1;

    long FIVE_MIN = TimeUnit.MINUTES.toMillis(5);

    int handleEvent(T event);
    void clean();
    void expire(String id);
}
