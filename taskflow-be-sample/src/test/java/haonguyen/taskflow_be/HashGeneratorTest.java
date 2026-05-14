package haonguyen.taskflow_be;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashGeneratorTest {

    @Test
    void printHashForSeedData() {
        var encoder = new BCryptPasswordEncoder(10);
        var hash = encoder.encode("Test1234!");
        System.out.println("=== BCrypt hash for Test1234! ===");
        System.out.println(hash);
        System.out.println("=================================");
    }
}
