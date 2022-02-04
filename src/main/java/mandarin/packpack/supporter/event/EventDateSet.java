package mandarin.packpack.supporter.event;

import java.util.Calendar;

public class EventDateSet {
    public final EventDate dateStart, dateEnd;
    public final EventTimeSection section;

    public EventDateSet(int dateStart, int dateEnd, int startTime, int endTime) {
        section = new EventTimeSection(startTime, endTime);

        this.dateStart = new EventDate(dateStart, false, section, false);
        this.dateEnd = new EventDate(dateEnd, endTime == 0, section, true);
    }

    public boolean equals(EventDateSet thatSet) {
        return dateStart.equals(thatSet.dateStart) && dateEnd.equals(thatSet.dateEnd) && section.equals(thatSet.section);
    }

    public boolean inRange(EventDate current) {
        int c = dateStart.compare(current);

        if(c == 1)
            return false;
        else if(c == 0) {
            c = section.start.compare(current.section.start);

            if(c == 1)
                return false;
        }

        c = dateEnd.compare(current);

        if(c == -1)
            return false;
        else if(c == 0) {
            c = section.end.compare(current.section.end);

            return c == 1;
        }


        return true;
    }
}
