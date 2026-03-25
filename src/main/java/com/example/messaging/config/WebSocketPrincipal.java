package com.example.messaging.config;

import java.security.Principal;
import java.util.UUID;

public record WebSocketPrincipal(UUID userId, String username) implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
