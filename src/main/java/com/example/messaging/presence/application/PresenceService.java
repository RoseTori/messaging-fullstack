package com.example.messaging.presence.application;

import com.example.messaging.message.application.MessageService;
import com.example.messaging.user.application.UserService;
import com.example.messaging.user.domain.UserStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final MessageService messageService;

    @Value("${app.presence.ttl-seconds}")
    private long ttlSeconds;

    public void registerSession(UUID userId, String sessionId) {
        Long count = redisTemplate.opsForSet().add(sessionsKey(userId), sessionId);
        redisTemplate.expire(sessionsKey(userId), Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForValue().set(lastSeenKey(userId), Instant.now().toString(), Duration.ofSeconds(ttlSeconds));
        if (count != null && count == 1L) {
            userService.updateStatus(userId, UserStatus.ONLINE);
            messageService.markDeliveredForUser(userId);
            messagingTemplate.convertAndSend("/topic/presence", new PresenceEvent(userId, true, Instant.now()));
        }
    }

    public void heartbeat(UUID userId) {
        redisTemplate.expire(sessionsKey(userId), Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForValue().set(lastSeenKey(userId), Instant.now().toString(), Duration.ofSeconds(ttlSeconds));
    }

    public void unregisterSession(UUID userId, String sessionId) {
        redisTemplate.opsForSet().remove(sessionsKey(userId), sessionId);
        Long remaining = redisTemplate.opsForSet().size(sessionsKey(userId));
        if (remaining == null || remaining == 0) {
            redisTemplate.delete(sessionsKey(userId));
            redisTemplate.delete(lastSeenKey(userId));
            userService.updateStatus(userId, UserStatus.OFFLINE);
            messagingTemplate.convertAndSend("/topic/presence", new PresenceEvent(userId, false, Instant.now()));
        }
    }

    public PresenceState get(UUID userId) {
        String value = redisTemplate.opsForValue().get(lastSeenKey(userId));
        return new PresenceState(value != null, value == null ? null : Instant.parse(value));
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanExpiredOnlineUsers() {
        Set<String> onlineUsers = redisTemplate.keys("presence:last-seen:*");
        if (onlineUsers == null) {
            return;
        }
        for (String key : onlineUsers) {
            String userId = key.substring(key.lastIndexOf(':') + 1);
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                userService.updateStatus(UUID.fromString(userId), UserStatus.OFFLINE);
            }
        }
    }

    private String sessionsKey(UUID userId) {
        return "presence:sessions:" + userId;
    }

    private String lastSeenKey(UUID userId) {
        return "presence:last-seen:" + userId;
    }
}
