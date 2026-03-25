package com.example.messaging.message.api;

import com.example.messaging.message.domain.Message;
import com.example.messaging.message.domain.MessageType;
import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        UUID senderId,
        MessageType type,
        String cipherText,
        String encryptedKey,
        String nonce,
        String algorithm,
        String metadata,
        String clientMessageId,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getType(),
                message.getCipherText(),
                message.getEncryptedKey(),
                message.getNonce(),
                message.getAlgorithm(),
                message.getMetadata(),
                message.getClientMessageId(),
                message.getCreatedAt()
        );
    }
}
