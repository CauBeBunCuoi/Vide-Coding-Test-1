package haonguyen.taskflow_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import haonguyen.taskflow_be.dto.response.TokenResponse;
import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.exception.BusinessRuleException;
import haonguyen.taskflow_be.exception.GlobalExceptionHandler;
import haonguyen.taskflow_be.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        var controller = new AuthController(authService, 604800000L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void register_returns201_onSuccess() throws Exception {
        var userResponse = new UserResponse(1L, "alice", "alice@example.com", Instant.now());
        when(authService.register(any())).thenReturn(userResponse);

        var body = Map.of("username", "alice", "email", "alice@example.com", "password", "Test1234!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void register_returns400_onMissingFields() throws Exception {
        var body = Map.of("email", "alice@example.com");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void login_returns200WithToken_onValidCredentials() throws Exception {
        var token = new TokenResponse("access-token-123", 900000L);
        when(authService.login(any())).thenReturn(new AuthService.AuthResult(token, "raw-refresh"));

        var body = Map.of("email", "alice@example.com", "password", "Test1234!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.expiresIn").value(900000));
    }

    @Test
    void login_returns400_onInvalidCredentials() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password"));

        var body = Map.of("email", "alice@example.com", "password", "wrong");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
