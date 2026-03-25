package com.example.messaging.realtime.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TypingRequest(@NotNull UUID chatId, boolean typing) {
}
