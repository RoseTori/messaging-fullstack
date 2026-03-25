package com.example.messaging.user.application;

import com.example.messaging.common.application.NotFoundException;
import com.example.messaging.user.api.UserResponse;
import com.example.messaging.user.domain.User;
import com.example.messaging.user.domain.UserStatus;
import com.example.messaging.user.infrastructure.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public void updateStatus(UUID userId, UserStatus status) {
        User user = getById(userId);
        user.setStatus(status);
    }
}
