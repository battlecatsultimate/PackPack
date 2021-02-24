package mandarin.packpack.supporter.server;

public class TimeBoolean {
    public final boolean canDo;
    public final long time = System.currentTimeMillis();
    public long totalTime;

    public TimeBoolean(boolean canDo, long... time) {
        this.canDo = canDo;

        if(!canDo) {
            if(time.length != 1)
                throw new IllegalStateException("If canDo is false, you have to specify time!");
            else
                totalTime = time[0];
        } else {
            totalTime = 0;
        }
    }
}
