package mandarin.packpack.supporter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecordableThread extends Thread {
    private static final List<RecordableThread> executingThreads = new ArrayList<>();
    //24h
    private static final long expirationTime = 0;

    public static void handleExpiration() {
        long currentTime = System.currentTimeMillis();

        executingThreads.removeIf(t -> {
            boolean expired = t.isExpired(currentTime);

            System.out.println(t.getName() + " - " + expired);

            if (expired) {
                StaticStore.logger.uploadLog("I/RecordableThread::handleExpiration - Expired thread found : " + t.getName());

                t.interrupt();
            }

            return expired;
        });
    }

    private final long createdTime = System.currentTimeMillis();

    private boolean manualExpired = false;

    private final Consumer<Exception> onError;

    public RecordableThread(ThrowableRunnable executor, Consumer<Exception> onError) {
        super(executor);

        this.onError = onError;
    }

    @Override
    public void run() {
        executingThreads.add(this);

        try {
            super.run();
        } catch (Exception e) {
            if (!manualExpired) {
                onError.accept(e);
            }
        }

        endThread();
    }

    public synchronized boolean isExpired(long currentTime) {
        manualExpired = currentTime - createdTime > expirationTime;

        return manualExpired;
    }

    public synchronized void endThread() {
        executingThreads.remove(this);
    }
}
