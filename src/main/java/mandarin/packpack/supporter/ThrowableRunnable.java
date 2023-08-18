package mandarin.packpack.supporter;

@FunctionalInterface
public interface ThrowableRunnable extends Runnable {
    @Override
    default void run() {
        try {
            doRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void doRun() throws Exception;
}
