package mandarin.packpack.supporter.event;

public class EventDate {
    public final int year;
    public final int month;
    public final int day;

    public EventDate(int value) {
        year = value / 10000;

        value -= year * 10000;

        month = value / 100;

        value -= month * 100;

        day = value;
    }

    public int compare(EventDate date) {
        int thisDate = year * 10000 + month * 100 + day;
        int thatDate = date.year * 10000 + date.month * 100 + date.day;

        return Integer.compare(thisDate, thatDate);
    }

    public boolean equals(EventDate date) {
        return compare(date) == 0;
    }
}
