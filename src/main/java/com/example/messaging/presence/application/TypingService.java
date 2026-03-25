package com.example.messaging.presence.application;

import com.example.messaging.auth.application.AuthenticatedUser;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TypingService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.typing.ttl-seconds}")
    private long ttlSeconds;

    public void typing(UUID chatId, AuthenticatedUser authenticatedUser) {
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        redisTemplate.opsForValue().set(key(chatId, authenticatedUser.userId()), authenticatedUser.username(), ttl);
        messagingTemplate.convertAndSend("/topic/chat." + chatId + ".typing",
                new TypingEvent(chatId, authenticatedUser.userId(), authenticatedUser.username(), true, Instant.now().plus(ttl)));
    }

    public void stopTyping(UUID chatId, AuthenticatedUser authenticatedUser) {
        redisTemplate.delete(key(chatId, authenticatedUser.userId()));
        messagingTemplate.convertAndSend("/topic/chat." + chatId + ".typing",
                new TypingEvent(chatId, authenticatedUser.userId(), authenticatedUser.username(), false, Instant.now()));
    }

    private String key(UUID chatId, UUID userId) {
        return "typing:chat:" + chatId + ":user:" + userId;
    }
}
