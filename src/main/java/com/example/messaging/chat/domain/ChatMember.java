package com.example.messaging.chat.domain;

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
@Table(name = "chat_members")
public class ChatMember {

    @Id
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "chat_room_id", nullable = false, columnDefinition = "binary(16)")
    private UUID chatRoomId;

    @Column(name = "user_id", nullable = false, columnDefinition = "binary(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @PrePersist
    public void prePersist() {
        joinedAt = LocalDateTime.now();
    }
}
