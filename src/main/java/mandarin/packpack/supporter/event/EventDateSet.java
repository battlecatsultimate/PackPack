package mandarin.packpack.supporter.event;

public class EventDateSet {
    public final EventDate dateStart, dateEnd;
    public final EventTimeSection section;

    public EventDateSet(int dateStart, int dateEnd, int startTime, int endTime) {
        this.dateStart = new EventDate(dateStart);
        this.dateEnd = new EventDate(dateEnd);
        section = new EventTimeSection(startTime, endTime);
    }
}
