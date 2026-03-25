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
@Table(name = "message_status")
public class MessageStatus {

    @Id
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "message_id", nullable = false, columnDefinition = "binary(16)")
    private UUID messageId;

    @Column(name = "user_id", nullable = false, columnDefinition = "binary(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageDeliveryStatus status;

    @Column(name = "status_at", nullable = false)
    private LocalDateTime statusAt;

    @PrePersist
    public void prePersist() {
        if (statusAt == null) {
            statusAt = LocalDateTime.now();
        }
    }
}
