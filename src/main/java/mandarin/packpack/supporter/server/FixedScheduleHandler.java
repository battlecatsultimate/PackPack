package mandarin.packpack.supporter.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FixedScheduleHandler {
    private final ScheduledExecutorService handler;

    public FixedScheduleHandler(int numberOfThreads) {
        handler = Executors.newScheduledThreadPool(numberOfThreads);
    }

    public void post(Runnable executor) {
        handler.schedule(executor, 0, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> postDelayed(long delay, Runnable executor) {
        return handler.schedule(executor, delay, TimeUnit.MILLISECONDS);
    }
}
