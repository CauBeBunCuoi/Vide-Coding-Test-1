package haonguyen.taskflow_be.mapper;

import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.entity.User;

public class UserMapper {
    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }
}
