package haonguyen.taskflow_be.repository;

import haonguyen.taskflow_be.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT COUNT(a) FROM LoginAttempt a " +
           "WHERE LOWER(a.email) = LOWER(:email) " +
           "AND a.success = false " +
           "AND a.attemptedAt > :since")
    long countFailedAttempts(String email, Instant since);
}
