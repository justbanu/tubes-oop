package nangor.core;

public final class MapValidationException extends Exception {
    private static final long serialVersionUID = 1L;
    public MapValidationException(String message) { super(message); }
    public MapValidationException(String message, Throwable cause) { super(message, cause); }
}
