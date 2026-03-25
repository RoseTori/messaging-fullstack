package com.example.messaging.user.api;

import com.example.messaging.auth.application.AuthenticatedUser;
import com.example.messaging.user.application.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return UserResponse.from(userService.getById(authenticatedUser.userId()));
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.getUsers();
    }
}
