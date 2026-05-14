package haonguyen.taskflow_be.exception;

public class BusinessRuleException extends RuntimeException {
    private final String errorCode;
    public BusinessRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
