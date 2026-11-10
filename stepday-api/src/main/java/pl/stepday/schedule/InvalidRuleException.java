package pl.stepday.schedule;

public class InvalidRuleException extends IllegalArgumentException {
    public InvalidRuleException(String message) {
        super(message);
    }
}
