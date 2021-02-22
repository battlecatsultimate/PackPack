package mandarin.packpack.supporter.event;

public class EventTimeSection {
    public final EventTime start;
    public final EventTime end;

    public EventTimeSection(int start, int end) {
        this.start = new EventTime(start);
        this.end = new EventTime(end);
    }
}
