package pl.stepday.day;

public enum DayStatus {
    OPEN, FULL, PARTIAL, MISSED;

    /** FULL i PARTIAL liczą się jako dni zaliczone. */
    public boolean isCompleted() {
        return this == FULL || this == PARTIAL;
    }
}
