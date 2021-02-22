package mandarin.packpack.supporter.event;

public class EventTime {
    public final int hour;
    public final int minute;

    public EventTime(int time) {
        hour = time / 100;
        minute = time % 100;
    }
}
