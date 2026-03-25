package com.example.messaging.moderation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "user_blocks")
public class UserBlock {

    @Id
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "blocker_id", nullable = false, columnDefinition = "binary(16)")
    private UUID blockerId;

    @Column(name = "blocked_id", nullable = false, columnDefinition = "binary(16)")
    private UUID blockedId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
