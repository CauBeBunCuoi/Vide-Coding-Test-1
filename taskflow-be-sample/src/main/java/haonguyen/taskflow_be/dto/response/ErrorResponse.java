package haonguyen.taskflow_be.dto.response;

public record ErrorResponse(String error, String message, Object details) {
    public record FieldError(String field, String message) {}
}
