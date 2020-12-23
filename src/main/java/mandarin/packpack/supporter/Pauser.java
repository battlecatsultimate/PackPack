package mandarin.packpack.supporter;

public class Pauser {
    final Object obj = new Object();
    boolean finish = false;

    public void pause(Runnable handler) {
        while(!finish) {
            try {
                synchronized (obj) {
                    obj.wait();
                }
            } catch (InterruptedException e) {
                handler.run();
                e.printStackTrace();
            }
        }
    }

    public synchronized void resume() {
        finish = true;

        synchronized (obj) {
            obj.notifyAll();
        }
    }
}
