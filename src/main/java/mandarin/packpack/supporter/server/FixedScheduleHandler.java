package mandarin.packpack.supporter.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FixedScheduleHandler {
    private final ScheduledExecutorService handler;

    public FixedScheduleHandler(int numberOfThreads) {
        handler = Executors.newScheduledThreadPool(numberOfThreads);
    }

    public void post(Runnable executor) {
        handler.schedule(executor, 0, TimeUnit.MILLISECONDS);
    }

    public void postDelayed(long delay, Runnable executor) {
        handler.schedule(executor, delay, TimeUnit.MILLISECONDS);
    }
}
