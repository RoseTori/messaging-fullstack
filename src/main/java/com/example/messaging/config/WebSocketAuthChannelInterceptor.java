package com.example.messaging.config;

import com.example.messaging.auth.application.JwtService;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new MessageDeliveryException(message, "Missing Authorization header");
            }

            String raw = authHeaders.get(0);
            String token = raw.startsWith("Bearer ") ? raw.substring(7) : raw;

            try {
                var claims = jwtService.parse(token);

                UUID userId = UUID.fromString(String.valueOf(claims.get("uid")));
                String username = claims.getSubject();

                WebSocketPrincipal principal = new WebSocketPrincipal(userId, username);

                accessor.setUser(principal);

            } catch (JwtException ex) {
                throw new MessageDeliveryException(message, "Invalid or expired token", ex);
            }
        }

        return message;
    }
}

