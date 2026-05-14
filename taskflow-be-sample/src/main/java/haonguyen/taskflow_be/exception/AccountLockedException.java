package haonguyen.taskflow_be.exception;

import java.time.Instant;

public class AccountLockedException extends RuntimeException {
    private final Instant lockedUntil;
    public AccountLockedException(Instant lockedUntil) {
        super("Account is locked due to too many failed login attempts");
        this.lockedUntil = lockedUntil;
    }
    public Instant getLockedUntil() { return lockedUntil; }
}
