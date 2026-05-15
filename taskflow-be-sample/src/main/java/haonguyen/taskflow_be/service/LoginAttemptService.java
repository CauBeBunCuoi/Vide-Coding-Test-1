package haonguyen.taskflow_be.service;

import haonguyen.taskflow_be.entity.LoginAttempt;
import haonguyen.taskflow_be.repository.LoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {
    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(String email, boolean success) {
        var attempt = new LoginAttempt();
        attempt.setEmail(email.toLowerCase());
        attempt.setSuccess(success);
        loginAttemptRepository.save(attempt);
    }
}
