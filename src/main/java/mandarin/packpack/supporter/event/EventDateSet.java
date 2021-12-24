package mandarin.packpack.supporter.event;

public class EventDateSet {
    public final EventDate dateStart, dateEnd;
    public final EventTimeSection section;

    public EventDateSet(int dateStart, int dateEnd, int startTime, int endTime) {
        this.dateStart = new EventDate(dateStart, false);
        this.dateEnd = new EventDate(dateEnd, endTime == 0);
        section = new EventTimeSection(startTime, endTime);
    }

    public boolean equals(EventDateSet thatSet) {
        return dateStart.equals(thatSet.dateStart) && dateEnd.equals(thatSet.dateEnd) && section.equals(thatSet.section);
    }
}
