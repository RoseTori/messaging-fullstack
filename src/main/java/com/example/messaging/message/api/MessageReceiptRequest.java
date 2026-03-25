package com.example.messaging.message.api;

import com.example.messaging.message.domain.MessageDeliveryStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MessageReceiptRequest(
        @NotNull UUID messageId,
        @NotNull MessageDeliveryStatus status
) {
}
