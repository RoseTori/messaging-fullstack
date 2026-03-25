package com.example.messaging.chat.api;

import com.example.messaging.chat.domain.ChatType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatResponse(UUID id, ChatType type, String title, List<UUID> members, LocalDateTime createdAt) {
}
