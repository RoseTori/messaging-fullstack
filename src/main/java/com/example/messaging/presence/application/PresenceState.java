package com.example.messaging.presence.application;

import java.time.Instant;

public record PresenceState(boolean online, Instant lastSeenAt) {
}
