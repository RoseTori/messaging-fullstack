package com.example.messaging.presence.application;

import java.time.Instant;
import java.util.UUID;

public record TypingEvent(UUID chatId, UUID userId, String username, boolean typing, Instant expiresAt) {
}
