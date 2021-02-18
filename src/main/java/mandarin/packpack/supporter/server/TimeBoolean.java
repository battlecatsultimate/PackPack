package mandarin.packpack.supporter.server;

public class TimeBoolean {
    public boolean canDo;
    public long time = System.currentTimeMillis();

    public TimeBoolean(boolean canDo) {
        this.canDo = canDo;
    }
}
