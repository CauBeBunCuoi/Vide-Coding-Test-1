package haonguyen.taskflow_be.exception;

public class AccessDeniedException extends RuntimeException {
    private final String errorCode;
    public AccessDeniedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
