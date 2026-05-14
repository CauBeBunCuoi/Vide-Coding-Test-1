package haonguyen.taskflow_be.service;

import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.exception.ResourceNotFoundException;
import haonguyen.taskflow_be.mapper.UserMapper;
import haonguyen.taskflow_be.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getMe(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        return UserMapper.toResponse(user);
    }
}
