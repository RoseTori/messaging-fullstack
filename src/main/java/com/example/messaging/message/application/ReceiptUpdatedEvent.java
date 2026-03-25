package com.example.messaging.message.application;

import com.example.messaging.message.api.MessageStatusResponse;
import java.util.UUID;

public record ReceiptUpdatedEvent(UUID chatId, UUID messageId, MessageStatusResponse status) {
}
