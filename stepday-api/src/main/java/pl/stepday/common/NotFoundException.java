package pl.stepday.common;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String what) {
        super("Nie znaleziono: " + what);
    }
}
