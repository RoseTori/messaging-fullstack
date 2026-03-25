package com.example.messaging.message.application;

import com.example.messaging.message.api.MessageResponse;
import java.util.List;
import java.util.UUID;

public record MessageCreatedEvent(UUID chatId, MessageResponse message, List<UUID> recipientIds) {
}
