package mandarin.packpack.supporter.event;

public class EventTimeSection {
    public final EventTime start;
    public final EventTime end;

    public EventTimeSection(int start, int end) {
        if(end == 0)
            end = 2359;

        this.start = new EventTime(start);
        this.end = new EventTime(end);
    }

    public boolean equals(EventTimeSection thatSection) {
        return start.equals(thatSection.start) && end.equals(thatSection.end);
    }
}
