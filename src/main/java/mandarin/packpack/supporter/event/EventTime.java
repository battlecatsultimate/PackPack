package mandarin.packpack.supporter.event;

public class EventTime {
    public final int hour;
    public final int minute;

    public EventTime(int time) {
        hour = time / 100;
        minute = time % 100;
    }

    public boolean equals(EventTime thatTime) {
        return hour == thatTime.hour && minute == thatTime.minute;
    }

    public int compare(EventTime time) {
        int thatTime = time.hour * 100 + time.minute;
        int thisTime = hour * 100 + minute;

        return Integer.compare(thisTime, thatTime);
    }
}
