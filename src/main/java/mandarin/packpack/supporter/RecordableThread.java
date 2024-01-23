package mandarin.packpack.supporter;

import mandarin.packpack.supporter.server.CommandLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecordableThread extends Thread {
    private static final List<RecordableThread> executingThreads = new ArrayList<>();
    //3h
    private static final long expirationTime = 60 * 60 * 1000;

    public static void handleExpiration() {
        long currentTime = System.currentTimeMillis();

        executingThreads.removeIf(t -> {
            boolean expired = t.isExpired(currentTime);

            if (expired) {
                if (t.loader != null) {
                    String content = "I/RecordableThread::handleExpiration - Expired thread found : " + t.getName() + "\n" +
                            "\n" +
                            "Command : " + t.loader.getMessage().getContentRaw() + "\n" +
                            "Channel : " + t.loader.getChannel().getName() + " [" + t.loader.getChannel().getId() + "]\n" +
                            "User : " + t.loader.getUser().getEffectiveName() + " [" + t.loader.getUser().getId() + "]";

                    if (t.loader.hasGuild()) {
                        content += "\n" +
                                "Guild : " + t.loader.getGuild().getName() + " [" + t.loader.getGuild().getId() + "]";
                    }

                    StaticStore.logger.uploadLog(content);
                } else {
                    StaticStore.logger.uploadLog("I/RecordableThread::handleExpiration - Expired thread found : " + t.getName());
                }

                t.interrupt();
            }

            return expired;
        });
    }

    private final long createdTime = System.currentTimeMillis();

    private boolean manualExpired = false;

    private final Consumer<Exception> onError;
    private final CommandLoader loader;

    public RecordableThread(ThrowableRunnable executor, Consumer<Exception> onError) {
        super(executor);

        this.onError = onError;
        this.loader = null;
    }

    public RecordableThread(ThrowableRunnable executor, Consumer<Exception> onError, CommandLoader loader) {
        super(executor);

        this.onError = onError;
        this.loader = loader;
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
