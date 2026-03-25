package com.example.messaging.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "chat_room_id", nullable = false, columnDefinition = "binary(16)")
    private UUID chatRoomId;

    @Column(name = "sender_id", nullable = false, columnDefinition = "binary(16)")
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    @Column(name = "cipher_text", nullable = false, columnDefinition = "longtext")
    private String cipherText;

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "longtext")
    private String encryptedKey;

    @Column(nullable = false, length = 255)
    private String nonce;

    @Column(nullable = false, length = 50)
    private String algorithm;

    @Column(columnDefinition = "json")
    private String metadata;

    @Column(name = "client_message_id", length = 100)
    private String clientMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
