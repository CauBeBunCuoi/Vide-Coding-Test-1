package haonguyen.taskflow_be.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,49}$",
        message = "Username must be 3–50 characters, letters/numbers/underscores, starting with a letter"
    )
    String username,

    @NotBlank
    @Email
    @Size(max = 100)
    String email,

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>?/~`\\\\]).{8,72}$",
        message = "Password must be 8–72 characters with uppercase, lowercase, number, and special character"
    )
    String password
) {}
