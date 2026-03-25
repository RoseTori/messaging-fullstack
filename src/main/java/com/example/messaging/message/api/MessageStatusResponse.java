package com.example.messaging.message.api;

import com.example.messaging.message.domain.MessageDeliveryStatus;
import com.example.messaging.message.domain.MessageStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record MessageStatusResponse(UUID messageId, UUID userId, MessageDeliveryStatus status, LocalDateTime statusAt) {
    public static MessageStatusResponse from(MessageStatus status) {
        return new MessageStatusResponse(status.getMessageId(), status.getUserId(), status.getStatus(), status.getStatusAt());
    }
}
