package com.example.messaging.auth.application;

import com.example.messaging.auth.api.AuthRequest;
import com.example.messaging.auth.api.AuthResponse;
import com.example.messaging.auth.api.RegisterRequest;
import com.example.messaging.common.application.RateLimitService;
import com.example.messaging.common.domain.UuidV7;
import com.example.messaging.user.domain.User;
import com.example.messaging.user.domain.UserStatus;
import com.example.messaging.user.infrastructure.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RateLimitService rateLimitService;

    @Value("${app.rate-limit.auth-per-minute}")
    private int authPerMinute;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        rateLimitService.consume("rate-limit:auth:register:" + request.username(), authPerMinute, Duration.ofMinutes(1));
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User();
        user.setId(UuidV7.randomUuid());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.OFFLINE);
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername()));
    }

    public AuthResponse login(AuthRequest request) {
        rateLimitService.consume("rate-limit:auth:login:" + request.username(), authPerMinute, Duration.ofMinutes(1));
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var user = userRepository.findByUsername(request.username()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername()));
    }
}
