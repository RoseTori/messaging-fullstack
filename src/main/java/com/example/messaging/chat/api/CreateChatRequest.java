package com.example.messaging.chat.api;

import com.example.messaging.chat.domain.ChatType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record CreateChatRequest(
        @NotNull ChatType type,
        String title,
        @NotEmpty Set<UUID> memberIds
) {
}
