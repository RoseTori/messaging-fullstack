package com.example.messaging.presence.application;

import java.time.Instant;
import java.util.UUID;

public record PresenceEvent(UUID userId, boolean online, Instant timestamp) {
}
