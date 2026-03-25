package com.example.messaging.message.api;

import com.example.messaging.message.domain.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID chatId,
        @NotNull MessageType type,
        @NotBlank String cipherText,
        @NotBlank String encryptedKey,
        @NotBlank String nonce,
        @NotBlank String algorithm,
        String metadata,
        String clientMessageId
) {
}
