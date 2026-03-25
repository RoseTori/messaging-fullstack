package com.example.messaging.user.api;

import com.example.messaging.user.domain.User;
import com.example.messaging.user.domain.UserStatus;
import java.util.UUID;

public record UserResponse(UUID id, String username, String displayName, UserStatus status) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getStatus());
    }
}
